//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.IgnVariant;

import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ign.com" }, urls = { "http://(www\\.)?pc\\.ign\\.com/dor/objects/\\d+/[A-Za-z0-9_\\-]+/videos/.*?\\d+\\.html|http://(www\\.)?ign\\.com/videos/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+" })
public class IgnCom extends PluginForDecrypt {
    public IgnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_NEW = "http://(www\\.)?ign\\.com/videos/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName;
        if (parameter.matches(TYPE_NEW)) {
            if (br.getHttpConnection().getResponseCode() == 404) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            fpName = this.br.getRegex("data\\-video\\-title=\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) {
                /* Fallback to url-name */
                fpName = new Regex(parameter, "/([a-z0-9\\-]+)$").getMatch(0);
            }
            fpName = Encoding.htmlDecode(fpName).trim();
            // final String json = br.getRegex("data-video=\\'(\\{.*?\\})\\'[\t\n\r ]+").getMatch(0);
            // final String json = br.getRegex("data-settings=\"(\\{.*?\\})\"[\t\n\r ]+").getMatch(0);
            String json = br.getRegex("video&quot;:(\\{.*?\\})\"[\t\n\r ]+").getMatch(0);
            if (json == null) {
                return null;
            }
            json = Encoding.htmlDecode(json);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            /* HLS */
            // final String hls = (String) entries.get("m3uUrl");
            final ArrayList<Object> renditions = (ArrayList) entries.get("assets");
            for (final Object rendition : renditions) {
                entries = (LinkedHashMap<String, Object>) rendition;
                final String finallink = (String) entries.get("url");
                final String height = Long.toString(JavaScriptEngineFactory.toLong(entries.get("height"), -1));
                if (finallink == null || height.equals("-1")) {
                    continue;
                }
                final DownloadLink dlink = createDownloadlink("directhttp://" + finallink);
                dlink.setFinalFileName(fpName + "_" + height + ".mp4");
                decryptedLinks.add(dlink);
            }
        } else {
            if (br.containsHTML("No htmlCode read")) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            fpName = br.getRegex("<meta property=\"og:title\" content=\"(.*?) \\- IGN\"").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<h1 class=\"grid_16 container hdr\\-video\\-title\">(.*?)</h1>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("var disqus_title=\"(.*?)\";").getMatch(0);
                    if (fpName == null) {
                        fpName = br.getRegex("<title>(.*?) Video \\- ").getMatch(0);
                    }
                }
            }
            if (fpName == null) {
                return null;
            }
            fpName = fpName.trim();
            String configUrl = br.getRegex("\"config_episodic\":\"(http:.*?)\"").getMatch(0);
            if (configUrl == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            configUrl = configUrl.replace("\\", "");
            br.getPage(configUrl);
            boolean failed = true;
            String regexes[] = { "<downloadable src=\"(http://.*?)\"", "format\\-height=\"\\d+\" mime-type=\"video/mp4\" src=\"(http://.*?)\"" };
            for (String regex : regexes) {
                String[] links = br.getRegex(regex).getColumn(0);
                if (links != null && links.length != 0) {
                    failed = false;
                    for (String singleLink : links) {
                        final DownloadLink dlink = createDownloadlink("directhttp://" + singleLink);
                        dlink.setFinalFileName(fpName + singleLink.substring(singleLink.length() - 4, singleLink.length()));
                        decryptedLinks.add(dlink);
                    }
                }
            }
            if (failed) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    private String vid = null;

    @Override
    protected DownloadLink createDownloadlink(String id) {
        final DownloadLink ret;
        if (id.startsWith("directhttp://")) {
            ret = super.createDownloadlink(id);
        } else {
            /** RODO: Check if this is still needed - this code is probably very old! */
            ret = super.createDownloadlink("http://video.decryptedrutube.ru/" + id);
            try {
                if (vid == null) {
                    // since we know the embed url for player/embed link types no need todo this step
                    br.getPage("http://rutube.ru/api/video/" + id);
                    if (br.containsHTML("<root><detail>Not found</detail></root>")) {
                        return createOfflinelink(this.getCurrentLink().getURL());
                    }
                    vid = br.getRegex("/embed/(\\d+)").getMatch(0);
                }
                br.getPage("http://rutube.ru/play/embed/" + vid + "?wmode=opaque&autoStart=true");
                String videoBalancer = br.getRegex("(http\\:\\/\\/bl\\.rutube\\.ru[^\"]+)").getMatch(0);
                if (videoBalancer != null) {
                    final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    final XPath xPath = XPathFactory.newInstance().newXPath();
                    br.getPage(videoBalancer);
                    Document d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
                    String baseUrl = xPath.evaluate("/manifest/baseURL", d).trim();
                    NodeList f4mUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
                    Node best = f4mUrls.item(f4mUrls.getLength() - 1);
                    ArrayList<IgnVariant> variantsVideo = new ArrayList<IgnVariant>();
                    IgnVariant bestVariant = null;
                    for (int i = 0; i < f4mUrls.getLength(); i++) {
                        best = f4mUrls.item(i);
                        String width = getAttByNamedItem(best, "width");
                        String height = getAttByNamedItem(best, "height");
                        String bitrate = getAttByNamedItem(best, "bitrate");
                        String f4murl = getAttByNamedItem(best, "href");
                        br.getPage(baseUrl + f4murl);
                        d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
                        // baseUrl = xPath.evaluate("/manifest/baseURL", d).trim();
                        double duration = Double.parseDouble(xPath.evaluate("/manifest/duration", d));
                        NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
                        Node media;
                        for (int j = 0; j < mediaUrls.getLength(); j++) {
                            media = mediaUrls.item(j);
                            // System.out.println(new String(Base64.decode(xPath.evaluate("/manifest/media[" + (j + 1) + "]/metadata",
                            // d).trim())));
                            IgnVariant var = new IgnVariant(width, height, bitrate, getAttByNamedItem(media, "streamId"));
                            variantsVideo.add(var);
                            bestVariant = var;
                        }
                    }
                    if (variantsVideo.size() > 0) {
                        ret.setVariants(variantsVideo);
                        ret.setVariant(bestVariant);
                    }
                    return ret;
                } else {
                    logger.warning("Video not available in your country: " + "http://rutube.ru/api/video/" + vid);
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * lets try and prevent possible NPE from killing the progress.
     *
     * @author raztoki
     * @param n
     * @param item
     * @return
     */
    private String getAttByNamedItem(final Node n, final String item) {
        final String t = n.getAttributes().getNamedItem(item).getTextContent();
        return (t != null ? t.trim() : null);
    }
}