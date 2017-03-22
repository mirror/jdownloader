//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.RuTubeVariant;

import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rutube.ru" }, urls = { "https?://((?:www\\.)?rutube\\.ru/(tracks/\\d+\\.html|(play/|video/)?embed/\\d+(.*?p=[A-Za-z0-9\\-_]+)?|video/[a-f0-9]{32})|video\\.rutube.ru/([a-f0-9]{32}|\\d+))" })
public class RuTubeRuDecrypter extends PluginForDecrypt {

    public RuTubeRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String uid          = null;
    private String vid          = null;
    private String privatevalue = null;
    private String parameter    = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br = new Browser();

        parameter = param.toString();

        uid = new Regex(parameter, "/([a-f0-9]{32})").getMatch(0);
        vid = new Regex(parameter, "rutube\\.ru/(?:play/|video/)?embed/(\\d+)").getMatch(0);
        privatevalue = new Regex(parameter, "p=([A-Za-z0-9\\-_]+)").getMatch(0);
        if (vid == null) {
            vid = new Regex(parameter, "video\\.rutube\\.ru/(\\d+)").getMatch(0);
        }
        if (uid == null) {
            br.setFollowRedirects(true);
            if (vid != null) {
                // embed link, grab info since we are already on this page
                getPage("http://rutube.ru/play/embed/" + vid + "?wmode=opaque&autoStart=true");

                final String b = HTMLEntities.unhtmlentities(HTMLEntities.unhtmlDoubleQuotes(br.toString()));
                uid = new Regex(b, "\"id\"\\s*:\\s*\"([a-f0-9]{32})\"").getMatch(0);
                if (uid == null) {
                    /* E.g. video/private/[a-f0-9]{32} */
                    uid = new Regex(b, "<link rel=\"canonical\" href=\"https?://rutube\\.ru/video[^<>\"\\']+([a-f0-9]{32})").getMatch(0);
                }
                if (uid == null) {
                    String msg = getErrorMessage();
                    if (msg != null) {
                        decryptedLinks.add(createOfflinelink(parameter, "Offline - " + vid + " - " + msg, msg));
                    } else {
                        decryptedLinks.add(createOfflinelink(parameter, "Offline - " + vid, null));
                    }
                    return decryptedLinks;
                }
            } else {
                // tracks link
                br.getPage(parameter);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    decryptedLinks.add(createOfflinelink(parameter));
                    return decryptedLinks;
                }
                br.getPage(br.getURL().replace("/video/", "/api/video/") + "?format=xml");
                uid = br.getRegex("<id>([a-f0-9]{32})</id>").getMatch(0);
            }
            if (uid == null) {
                throw new Exception("Unknown LinkType");
            }

        }
        if (uid != null && decryptedLinks.isEmpty()) {
            final DownloadLink link = createDownloadlink(uid);
            if (link == null) {
                return null;
            }
            decryptedLinks.add(link);
        }

        return decryptedLinks;
    }

    private void getPage(final String url) throws IOException {
        getPage(this.br, url);
    }

    private void getPage(final Browser br, String url) throws IOException {
        if (privatevalue != null) {
            if (!url.contains("?")) {
                url += "?";
            } else {
                url += "&";
            }
            url += "p=" + Encoding.urlEncode(privatevalue);
        }
        br.getPage(url);
    }

    /**
     * error message types on the embeded link url
     *
     */
    private String getErrorMessage() {
        String msg = null;
        /* 2016-09-16: Private videos with given "p" parameter can be watched by anyone who has the url! */
        if (br.containsHTML("\"description\":\\s*\"The author made this video private\\.\"")) {
            // not supported
            msg = "Private Video, Unsupported feature";
        } else if (br.containsHTML("\"description\":\\s*\"This video does not exist or URL link isn't correct\\.\"") || br.containsHTML("\"description\":\\s*\"Video removed at the request of the copyright holder\"")) {
            // wrong url or removed content
            msg = "Video does not exist";
        }
        return msg;
    }

    @Override
    protected DownloadLink createDownloadlink(String id) {
        DownloadLink ret = super.createDownloadlink("http://video.decryptedrutube.ru/" + id);
        if (privatevalue != null) {
            ret.setProperty("privatevalue", privatevalue);
        }

        try {
            if (vid == null) {
                final Browser br = this.br.cloneBrowser();
                // since we know the embed url for player/embed link types no need todo this step
                br.getPage("http://rutube.ru/api/video/" + id);
                if (br.containsHTML("<root><detail>Not found</detail></root>")) {
                    return createOfflinelink(parameter);
                }
                vid = br.getRegex("/embed/(\\d+)").getMatch(0);
            }
            getPage("http://rutube.ru/play/embed/" + vid + "?wmode=opaque&autoStart=true");
            {
                // shouldn't be needed, non embeded links without 'vid' value should create offlinelink above.
                final String msg = getErrorMessage();
                if (msg != null) {
                    return createOfflinelink(parameter, vid + " - " + msg, msg);
                }
            }
            Browser ajax = getAjaxBR(br);
            getPage(ajax, "/api/play/options/" + vid + "/?format=json&no_404=true&sqr4374_compat=1&referer=" + Encoding.urlEncode(br.getURL()) + "&_t=" + System.currentTimeMillis());
            final HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
            final String videoBalancer = (String) JavaScriptEngineFactory.walkJson(entries, "video_balancer/default");
            if (videoBalancer != null) {
                final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                final XPath xPath = XPathFactory.newInstance().newXPath();
                ajax = getAjaxBR(br);
                ajax.getPage(videoBalancer);

                Document d = parser.parse(new ByteArrayInputStream(ajax.toString().getBytes("UTF-8")));
                String baseUrl = xPath.evaluate("/manifest/baseURL", d).trim();

                NodeList f4mUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
                Node best = f4mUrls.item(f4mUrls.getLength() - 1);
                ArrayList<RuTubeVariant> variantsVideo = new ArrayList<RuTubeVariant>();
                RuTubeVariant bestVariant = null;
                for (int i = 0; i < f4mUrls.getLength(); i++) {
                    best = f4mUrls.item(i);
                    String width = getAttByNamedItem(best, "width");
                    String height = getAttByNamedItem(best, "height");
                    String bitrate = getAttByNamedItem(best, "bitrate");
                    String f4murl = getAttByNamedItem(best, "href");
                    ajax = getAjaxBR(br);
                    ajax.getPage(baseUrl + f4murl);

                    d = parser.parse(new ByteArrayInputStream(ajax.toString().getBytes("UTF-8")));
                    // baseUrl = xPath.evaluate("/manifest/baseURL", d).trim();
                    double duration = Double.parseDouble(xPath.evaluate("/manifest/duration", d));

                    NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
                    Node media;
                    for (int j = 0; j < mediaUrls.getLength(); j++) {
                        media = mediaUrls.item(j);
                        // System.out.println(new String(Base64.decode(xPath.evaluate("/manifest/media[" + (j + 1) + "]/metadata",
                        // d).trim())));

                        RuTubeVariant var = new RuTubeVariant(width, height, bitrate, getAttByNamedItem(media, "streamId"));
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
                logger.info("Video not available in your country: " + "http://rutube.ru/api/video/" + vid);
                ret.setAvailable(false);
                return ret;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Browser getAjaxBR(final Browser br) {
        final Browser ajax = br.cloneBrowser();
        // rv40.0 don't get "video_balancer".
        ajax.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0");
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("Content-Type", "application/json");
        /* 2017-03-22: This Header is not required anymore! */
        // ajax.getHeaders().put("X-Requested-With", "ShockwaveFlash/22.0.0.209");
        return ajax;
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}