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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.IgnVariant;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class IgnCom extends PluginForDecrypt {
    public IgnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ign.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            final StringBuilder sb = new StringBuilder();
            final String domainPattern = buildHostsPatternPart(domains);
            sb.append("https?://(?:[a-z0-9]+)?\\." + domainPattern + "/(");
            sb.append("dor/objects/\\d+/[A-Za-z0-9_\\-]+/videos/.*?\\d+\\.html");
            sb.append("|[a-z0-9\\-]+/\\d+/video/[a-z0-9\\-]+");
            sb.append(")");
            sb.append("|https?://(?:www\\.)?" + domainPattern + "/videos/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+");
            ret.add(sb.toString());
        }
        return ret.toArray(new String[0]);
    }

    // private static final String TYPE_NEW = ".+ign\\.com/videos/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+";
    private final String TYPE_OLD   = "https?://(?:www\\.)?ign\\.com/videos/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+";
    private final String TYPE_EMBED = "https?://[^/]+/[a-z0-9\\-]+/\\d+/video/embed";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String fpName;
        String contenturl = param.getCryptedUrl();
        if (contenturl.matches(TYPE_EMBED)) {
            /* Embed URL: Redirects to extern video provider such as twitch.tv. */
            contenturl = contenturl.replaceFirst("(?i)http://", "https://");
            br.setFollowRedirects(false);
            br.getPage(contenturl);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ret.add(super.createDownloadlink(finallink));
        } else if (!contenturl.matches(TYPE_OLD)) {
            br.setFollowRedirects(true);
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            fpName = this.br.getRegex("data\\-video\\-title=\"([^<>\"]*?)\"").getMatch(0);
            if (fpName == null) {
                /* Fallback to url-name */
                fpName = new Regex(this.br.getURL(), "/([a-z0-9\\-]+)$").getMatch(0);
            }
            // final String json = br.getRegex("data-video=\\'(\\{.*?\\})\\'[\t\n\r ]+").getMatch(0);
            // final String json = br.getRegex("data-settings=\"(\\{.*?\\})\"[\t\n\r ]+").getMatch(0);
            // String json = br.getRegex("video&quot;:(\\{.*?\\})\"[\t\n\r ]+").getMatch(0);
            final String json_single_video = br.getRegex("<script type=\"application/ld\\+json\">([^<]*VideoObject[^>]*)</script>").getMatch(0);
            final String json = br.getRegex("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>").getMatch(0);
            if (json == null && json_single_video == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            Map<String, Object> entries = null;
            if (json_single_video != null) {
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json_single_video);
                final String embedURL = (String) entries.get("embedUrl");
                final String directurl = (String) entries.get("contentUrl");
                String fileTitle = (String) entries.get("name");
                if (StringUtils.isEmpty(directurl) && StringUtils.isEmpty(embedURL)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (StringUtils.isEmpty(fileTitle)) {
                    /* Fallback */
                    fileTitle = fpName;
                }
                if (!StringUtils.isEmpty(directurl)) {
                    final DownloadLink dlink = createDownloadlink("directhttp://" + directurl);
                    dlink.setFinalFileName(fileTitle + ".mp4");
                    ret.add(dlink);
                } else {
                    /* This will go into our crawler again and may e.g. redirect to external video sources such as twitch.tv. */
                    ret.add(super.createDownloadlink(embedURL));
                }
            } else {
                // json = Encoding.htmlDecode(json);
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps");
                final Object videoO = entries.get("video");
                if (videoO == null) {
                    /* 2019-08-28: E.g. readable article or livestream */
                    logger.info("Failed to find any downloadable content");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                entries = (Map<String, Object>) videoO;
                /* HLS */
                // final String hls = (String) entries.get("m3uUrl");
                final List<Object> renditions = (List) entries.get("assets");
                for (final Object rendition : renditions) {
                    entries = (Map<String, Object>) rendition;
                    final String finallink = (String) entries.get("url");
                    final String height = Long.toString(JavaScriptEngineFactory.toLong(entries.get("height"), -1));
                    if (finallink == null || height.equals("-1")) {
                        continue;
                    }
                    final DownloadLink dlink = createDownloadlink("directhttp://" + finallink);
                    dlink.setFinalFileName(fpName + "_" + height + ".mp4");
                    ret.add(dlink);
                }
            }
        } else {
            br.setFollowRedirects(true);
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404 || br.toString().length() <= 100) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            fpName = fpName.trim();
            String configUrl = br.getRegex("\"config_episodic\":\"(http:.*?)\"").getMatch(0);
            if (configUrl != null) {
                configUrl = configUrl.replace("\\", "");
                br.getPage(configUrl);
                boolean failed = true;
                String regexes[] = { "<downloadable src=\"(http://.*?)\"", "format\\-height=\"\\d+\" mime-type=\"video/mp4\" src=\"(http://.*?)\"" };
                for (String regex : regexes) {
                    String[] links = br.getRegex(regex).getColumn(0);
                    if (links != null && links.length != 0) {
                        failed = false;
                        for (String singleLink : links) {
                            final DownloadLink dlink = createDownloadlink(DirectHTTP.createURLForThisPlugin(singleLink));
                            dlink.setFinalFileName(fpName + singleLink.substring(singleLink.length() - 4, singleLink.length()));
                            ret.add(dlink);
                        }
                    }
                }
                if (failed) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                /* 2023-10-06 */
                final String[] links = br.getRegex("\"(http[^\"]+\\.mp4)").getColumn(0);
                if (links == null || links.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (String singleLink : links) {
                    final DownloadLink dlink = createDownloadlink(DirectHTTP.createURLForThisPlugin(singleLink));
                    // dlink.setFinalFileName(fpName + singleLink.substring(singleLink.length() - 4, singleLink.length()));
                    ret.add(dlink);
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.addLinks(ret);
        return ret;
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