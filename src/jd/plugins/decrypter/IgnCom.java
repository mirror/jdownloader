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

import javax.swing.Icon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ign.com" }, urls = { "http://(www\\.)?pc\\.ign\\.com/dor/objects/\\d+/[A-Za-z0-9_\\-]+/videos/.*?\\d+\\.html|http://(www\\.)?ign\\.com/videos/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+" }, flags = { 0 })
public class IgnCom extends PluginForDecrypt {

    public IgnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_NEW = "http://(www\\.)?ign\\.com/videos/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName;
        if (parameter.matches(TYPE_NEW)) {
            if (br.getHttpConnection().getResponseCode() == 404) {
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            // final String finallink = br.getRegex("id=\"iPadVideoSource_0\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
            // if (finallink == null) {
            // logger.warning("Decrypter broken for link: " + parameter);
            // return null;
            // }
            // final DownloadLink fina = createDownloadlink("directhttp://" + finallink);
            // fina.setFinalFileName(Encoding.htmlDecode(fpName).trim() + ".mp4");
            // fina.setAvailable(true);
            // decryptedLinks.add(fina);

            // HDS

            final String p = "url\\s*\\+=\\s*\"\\?version=([\\d\\.]+)";
            String aa = br.getRegex("var videoUrl\\s*=\\s*\"([^\"]+)\"").getMatch(0);
            String bb = br.getRegex("var url\\s*=\\s*\"([^\"]+)\";\\s*if\\(flashvars.cacheBusting\\)\\s*\\{\\s*" + p).getMatch(0);
            String cc = br.getRegex(p).getMatch(0);
            if (aa == null || bb == null || cc == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            aa = unescape(aa);
            bb = unescape(bb);
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("Accept", "*/*");
            br2.getPage(aa + ".config?autoplay=true&windowLocation=" + Encoding.urlEncode(aa) + "&pagetype=mediavideoplayer&playerVersion=" + cc);
            String zz = br2.getRegex("\"playlist\":\\{(.*?)\\}\\s*,\\s*\"relatedContent\"").getMatch(0);

            // we want playlist > media

            String title = getJson(zz, "title");
            String media = getJson(zz, "url");
            Browser br3 = br.cloneBrowser();
            br3.getPage(media);
            String blah = br3.toString();
            String[][] ent = new Regex(blah, "<\\s*(bootstrapInfo|metadata)[^>]*>(.*?)</\\1").getMatches();
            for (String[] e : ent) {
                blah = blah.replace(e[1], (e[1]));
            }
            String blah2 = "";
        } else {
            if (br.containsHTML("No htmlCode read")) {
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
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
                        DownloadLink dlink = createDownloadlink("directhttp://" + singleLink);
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
        fp.setName("HAHAHA");
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean ut_pluginLoaded = false;

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (ut_pluginLoaded == false) {

            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) {
                throw new IllegalStateException("youtube plugin not found!");
            }
            ut_pluginLoaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    private String uid = null;
    private String vid = null;

    public static class IgnVariant implements LinkVariant, Storable {
        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof IgnVariant)) {
                return false;
            }
            return _getUniqueId().equals(((IgnVariant) obj)._getUniqueId());
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

        public IgnVariant(/* storable */) {

        }

        public IgnVariant(String width, String height, String bitrate, String streamID) {
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
                ArrayList<IgnVariant> variantsVideo = new ArrayList<IgnCom.IgnVariant>();
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

}