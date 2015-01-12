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
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.RuTubeVariant;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rutube.ru" }, urls = { "http://((www\\.)?rutube\\.ru/(tracks/\\d+\\.html|(play/|video/)?embed/\\d+|video/[a-f0-9]{32})|video\\.rutube.ru/[a-f0-9]{32})" }, flags = { 0 })
public class RuTubeRuDecrypter extends PluginForDecrypt {

    public RuTubeRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String uid       = null;
    private String vid       = null;
    private String parameter = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString();

        uid = new Regex(parameter, "/([a-f0-9]{32})").getMatch(0);
        vid = new Regex(parameter, "rutube\\.ru/(?:play/|video/)?embed/(\\d+)").getMatch(0);
        if (uid == null) {
            br.setFollowRedirects(true);
            if (vid != null) {
                // embed link, grab info since we are already on this page
                br.getPage("http://rutube.ru/play/embed/" + vid + "?wmode=opaque&autoStart=true");
                uid = br.getRegex("\"id\"\\s*:\\s*\"([a-f0-9]{32})\"").getMatch(0);
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
            DownloadLink link = createDownloadlink(uid);
            if (link != null) {
                decryptedLinks.add(link);
            }
        }

        return decryptedLinks;
    }

    /**
     * error message types on the embeded link url
     * 
     */
    private String getErrorMessage() {
        String msg = null;
        if (br.containsHTML("\"description\":\\s*\"The author made this video private\\.\"") || br.containsHTML("/video/private/([a-f0-9]{32})/")) {
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

        try {
            if (vid == null) {
                // since we know the embed url for player/embed link types no need todo this step
                br.getPage("http://rutube.ru/api/video/" + id);
                if (br.containsHTML("<root><detail>Not found</detail></root>")) {
                    return createOfflinelink(parameter);
                }
                vid = br.getRegex("/embed/(\\d+)").getMatch(0);
            }
            br.getPage("http://rutube.ru/play/embed/" + vid + "?wmode=opaque&autoStart=true");
            {
                // shouldn't be needed, non embeded links without 'vid' value should create offlinelink above.
                final String msg = getErrorMessage();
                if (msg != null) {
                    return createOfflinelink(parameter, vid + " - " + msg, msg);
                }
            }
            String videoBalancer = br.getRegex("(http\\:\\/\\/bl\\.rutube\\.ru[^\"]+)").getMatch(0);

            if (videoBalancer != null) {
                final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                final XPath xPath = XPathFactory.newInstance().newXPath();

                br.getPage(videoBalancer);

                Document d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
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
                logger.warning("Video not available in your country: " + "http://rutube.ru/api/video/" + vid);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

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