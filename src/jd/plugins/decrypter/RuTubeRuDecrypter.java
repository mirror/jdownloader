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

import javax.swing.Icon;
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

import org.appwork.storage.Storable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rutube.ru" }, urls = { "http://((www\\.)?rutube\\.ru/(tracks/\\d+\\.html|(play/|video/)?embed/\\d+|video/[a-f0-9]{32})|video\\.rutube.ru/[a-f0-9]{32})" }, flags = { 0 })
public class RuTubeRuDecrypter extends PluginForDecrypt {

    public RuTubeRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String uid = null;
    private String vid = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        uid = new Regex(parameter, "/([a-f0-9]{32})").getMatch(0);
        vid = new Regex(parameter, "rutube\\.ru/(?:play/|video/)?embed/(\\d+)").getMatch(0);
        if (uid == null) {
            br.setFollowRedirects(true);
            if (vid != null) {
                // embed link, grab info since we are already on this page
                br.getPage("http://rutube.ru/play/embed/" + vid + "?wmode=opaque&autoStart=true");
                uid = br.getRegex("\"id\"\\s*:\\s*\"([a-f0-9]{32})\"").getMatch(0);
            } else {
                // tracks link
                br.getPage(parameter);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    final DownloadLink offline = super.createDownloadlink("directhttp://" + parameter);
                    offline.setAvailable(false);
                    offline.setProperty("offline", true);
                    decryptedLinks.add(offline);
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

    public static class RuTubeVariant implements LinkVariant, Storable {
        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof RuTubeVariant)) {
                return false;
            }
            return _getUniqueId().equals(((RuTubeVariant) obj)._getUniqueId());
        }

        private String width;

        public String getWidth() {
            return width;
        }

        public void setWidth(String width) {
            this.width = width;
        }

        public String getBitrate() {
            return bitrate;
        }

        public void setBitrate(String bitrate) {
            this.bitrate = bitrate;
        }

        public String getHeight() {
            return height;
        }

        public void setHeight(String height) {
            this.height = height;
        }

        public String getStreamID() {
            return streamID;
        }

        public void setStreamID(String streamID) {
            this.streamID = streamID;
        }

        private String       bitrate;
        private String       height;
        private String       streamID;
        private AbstractIcon icon;

        public RuTubeVariant(/* storable */) {

        }

        public RuTubeVariant(String width, String height, String bitrate, String streamID) {
            this.width = width;
            this.height = height;
            this.bitrate = bitrate;
            this.streamID = streamID;
            icon = new AbstractIcon(IconKey.ICON_VIDEO, 16);
        }

        @Override
        public String _getUniqueId() {
            return width + "x" + height + "_bitrate_" + streamID;
        }

        @Override
        public String _getName() {
            return height + "p";
        }

        @Override
        public Icon _getIcon() {
            return icon;
        }

        @Override
        public String _getExtendedName() {
            return height + "p (" + bitrate + "bps)";
        }

    }

    @Override
    protected DownloadLink createDownloadlink(String id) {
        DownloadLink ret = super.createDownloadlink("http://video.decryptedrutube.ru/" + id);

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
                ArrayList<RuTubeVariant> variantsVideo = new ArrayList<RuTubeRuDecrypter.RuTubeVariant>();
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