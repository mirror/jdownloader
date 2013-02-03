//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDHexUtils;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tape.tv" }, urls = { "http://(www\\.)?tape\\.tv/(musikvideos/[\\w\\-]+(/[\\w\\-]+)?|vid/\\d+)" }, flags = { 32 })
public class TapeTv extends PluginForHost {

    private class ReplacerInputStream extends InputStream {

        private final byte[]      REPLACEMENT = "amp;".getBytes();
        private final byte[]      readBuf     = new byte[REPLACEMENT.length];
        private final Deque<Byte> backBuf     = new ArrayDeque<Byte>();
        private final InputStream in;

        /**
         * Replacing & to {@literal &amp;} in InputStreams
         * 
         * @author mhaller
         * @see <a
         *      href="http://stackoverflow.com/a/4588005">http://stackoverflow.com/a/4588005</a>
         */
        public ReplacerInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            if (!backBuf.isEmpty()) { return backBuf.pop(); }
            int first = in.read();
            if (first == '&') {
                peekAndReplace();
            }
            return first;
        }

        private void peekAndReplace() throws IOException {
            int read = super.read(readBuf, 0, REPLACEMENT.length);
            for (int i1 = read - 1; i1 >= 0; i1--) {
                backBuf.push(readBuf[i1]);
            }
            for (int i = 0; i < REPLACEMENT.length; i++) {
                if (read != REPLACEMENT.length || readBuf[i] != REPLACEMENT[i]) {
                    for (int j = REPLACEMENT.length - 1; j >= 0; j--) {
                        // In reverse order
                        backBuf.push(REPLACEMENT[j]);
                    }
                    return;
                }
            }
        }

    }

    private static class Decrypt {

        private static String                  keys[] = { "SceljyienIp2", "iquafZadvaf9", "EvadvouxUth5", "HuWutTotaf1" };
        private static HashMap<String, String> keyMap;

        public static String getPlainData(String sec, String enc, String secVersion) {

            if (keyMap == null) initKeyMap(sec);
            String key = keyMap.get(secVersion);
            enc = enc.replaceAll("\n", "");
            if (key != null) {
                byte t[] = getBlowFish(org.appwork.utils.encoding.Base64.decode(enc), JDHexUtils.getByteArray(JDHexUtils.getHexString(key)));
                return new String(t);
            }
            return null;
        }

        private static byte[] getBlowFish(byte[] enc, byte[] key) {
            try {
                Cipher c = Cipher.getInstance("Blowfish/CBC/PKCS5Padding");
                SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
                IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOf(enc, 8));
                c.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                return c.doFinal(enc, 8, enc.length - 8);
            } catch (Throwable e) {
                return null;
            }
        }

        private static void initKeyMap(String sec) {
            keyMap = new HashMap<String, String>();
            for (String k : keys) {
                keyMap.put(JDHash.getMD5(sec + "/" + k).toUpperCase(Locale.ENGLISH), k);
            }
        }

    }

    private Document            doc;

    private static final String Q_LOW    = "Q_LOW";
    private static final String Q_MEDIUM = "Q_MEDIUM";
    private static final String Q_HIGH   = "Q_HIGH";
    private static final String Q_SD     = "Q_SD";
    private static final String Q_HD     = "Q_HD";
    private static final String Q_BEST   = "Q_BEST";

    public TapeTv(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public ArrayList<DownloadLink> getDownloadLinks(String data, FilePackage fp) {
        ArrayList<DownloadLink> ret = super.getDownloadLinks(data, fp);

        try {
            if (ret != null && ret.size() > 0) {
                /*
                 * we make sure only one result is in ret, thats the case for
                 * svn/next major version
                 */
                final DownloadLink sourceLink = ret.get(0);

                Browser br = new Browser();
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.getPage(sourceLink.getDownloadURL());
                String currentUrl = br.getURL();

                // http://www.tape.tv/musikvideos/Artist/title --> hoster
                // handling
                // http://www.tape.tv/musikvideos/Artist --> decrypter handling
                String contentXML = "/tapeMVC/tape/channel/artist;?telly=tapetv&artistId=";
                if (new Regex(currentUrl, "http://(www\\.)?tape\\.tv/musikvideos/[\\w\\-]+/[\\w\\-]+").matches()) contentXML = "/tapeMVC/tape/channel/deeplink;?telly=tapetv&videoId=";

                if (br.getHttpConnection().getResponseCode() == 404) {
                    sourceLink.setAvailable(false);
                    ret.set(0, sourceLink);
                    return ret;
                }

                String ID = br.getRegex("(artistId|videoId)=(\\d+)").getMatch(1);

                if (ID != null) {
                    xmlParser("http://www.tape.tv" + contentXML + ID + "&rnd=" + System.currentTimeMillis());

                    ArrayList<DownloadLink> newTmpRet = new ArrayList<DownloadLink>();
                    ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                    HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                    HashMap<String, String> musicVideo = new HashMap<String, String>();
                    HashMap<String, String> quality = new HashMap<String, String>();
                    HashMap<String, HashMap<String, String>> qualitys = new HashMap<String, HashMap<String, String>>();

                    NodeList titleList = doc.getElementsByTagName("video");
                    String title = null;

                    for (int j = 0; j < titleList.getLength(); ++j) {
                        NodeList titleNode = titleList.item(j).getChildNodes();

                        for (int i = 0; i < titleNode.getLength(); ++i) {
                            Node n = titleNode.item(i);
                            NamedNodeMap nnm = n.getAttributes();
                            if (nnm.getLength() > 0) {
                                for (int d = 0; d < nnm.getLength(); ++d) {
                                    Node vb = nnm.item(d);
                                    if (vb.getFirstChild() != null && vb.getFirstChild().getNodeType() == Node.TEXT_NODE) {
                                        if ("url".equals(n.getNodeName()) || "streamToken".equals(n.getNodeName())) {
                                            quality.put(vb.getNodeName(), vb.getFirstChild().getNodeValue());
                                        }
                                    }
                                }
                            }
                            if (quality.size() > 0) {
                                String key = quality.get("qualityName");
                                if ("streamToken".equals(n.getNodeName())) key = "streamToken";
                                if (key != null) {
                                    quality.put(n.getNodeName(), n.getFirstChild().getNodeValue());
                                    HashMap<String, String> valueMap = new HashMap<String, String>(quality);
                                    qualitys.put(key, valueMap);
                                    quality.clear();
                                }
                            }
                            if (n.getFirstChild() != null && n.getFirstChild().getNodeType() != Node.TEXT_NODE) {
                                NodeList has = n.getChildNodes();
                                for (int k = 0; k < has.getLength(); ++k) {
                                    Node kk = has.item(k);
                                    if (kk.getFirstChild() != null && kk.getFirstChild().getNodeType() == Node.TEXT_NODE) {
                                        if ("artist".equals(n.getNodeName()) || "title".equals(n.getNodeName())) {
                                            musicVideo.put(kk.getNodeName(), kk.getFirstChild().getNodeValue());
                                            // System.out.println(kk.getNodeName()
                                            // + " == " +
                                            // kk.getFirstChild().getNodeValue());
                                        }
                                    }
                                }
                            } else {
                                if (n.getFirstChild() != null && n.getFirstChild().getNodeType() == Node.TEXT_NODE) {
                                    if ("artist".equals(n.getNodeName()) || "title".equals(n.getNodeName())) {
                                        musicVideo.put(n.getNodeName(), n.getFirstChild().getNodeValue());
                                    }
                                }
                            }
                        }

                        String streamToken = null;
                        boolean bestQuality = this.getPluginConfig().getBooleanProperty(Q_BEST, false);
                        title = musicVideo.get("artist") + " - " + musicVideo.get("title").replaceAll("\\(|\\)", "--");

                        for (String q : qualitys.keySet()) {
                            quality = new HashMap<String, String>(qualitys.get(q));
                            String actualQ = q.toLowerCase(Locale.ENGLISH);
                            if ("streamToken".equals(q)) {
                                if (streamToken == null) streamToken = Decrypt.getPlainData(quality.get("sec"), quality.get("streamToken"), quality.get("secVersion"));
                                continue;
                            }
                            /* best selection is done at the end */
                            if (actualQ.contains("low")) {
                                if (!(bestQuality || this.getPluginConfig().getBooleanProperty(Q_LOW, true))) {
                                    continue;
                                } else {
                                    actualQ = "low";
                                }
                            } else if (actualQ.contains("medium")) {
                                if (!(bestQuality || this.getPluginConfig().getBooleanProperty(Q_MEDIUM, true))) {
                                    continue;
                                } else {
                                    actualQ = "medium";
                                }
                            } else if (actualQ.contains("high")) {
                                if (!(bestQuality || this.getPluginConfig().getBooleanProperty(Q_HIGH, true))) {
                                    continue;
                                } else {
                                    actualQ = "high";
                                }
                            } else if (actualQ.contains("sd")) {
                                if (!(bestQuality || this.getPluginConfig().getBooleanProperty(Q_SD, true))) {
                                    continue;
                                } else {
                                    actualQ = "sd";
                                }
                            } else if (actualQ.contains("hd")) {
                                if (!(bestQuality || this.getPluginConfig().getBooleanProperty(Q_HD, true))) {
                                    continue;
                                } else {
                                    actualQ = "hd";
                                }
                            }

                            String streamUrl = Decrypt.getPlainData(quality.get("sec"), quality.get("url"), quality.get("secVersion"));
                            if (streamUrl == null) continue;
                            streamUrl = streamUrl.replace(".tape.tv/", ".tapetv/");
                            if (!streamUrl.contains(".tapetv/")) {
                                logger.warning("Decrypter broken for link \"" + sourceLink.getDownloadURL() + "\" and quality \"" + q + "\"");
                                continue;
                            }
                            streamUrl = "rtmpe://cp68509.edgefcs.net:1935/ondemand@ondemand?ovpfv=1.1&auth=" + streamToken + "&aifp=v001@mp4:" + streamUrl.substring(streamUrl.indexOf("tapetv/"));

                            String name = title + "@" + q + ".mp4";

                            if (musicVideo.get("title").startsWith("Anmoderation")) continue;

                            final DownloadLink link = new DownloadLink(this, name, getHost(), sourceLink.getDownloadURL(), true);
                            link.setAvailable(true);
                            link.setFinalFileName(name);
                            link.setBrowserUrl(sourceLink.getBrowserUrl());
                            link.setProperty("directURL", streamUrl);
                            link.setProperty("directName", name);
                            link.setProperty("LINKDUPEID", "tapetv" + ID + name + q);
                            DownloadLink best = bestMap.get(actualQ);
                            if (best == null) {
                                bestMap.put(actualQ, link);
                            }
                            newTmpRet.add(link);
                        }
                        if (bestQuality) {
                            /* only keep best quality */
                            DownloadLink keep = bestMap.get("hd");
                            if (keep == null) keep = bestMap.get("sd");
                            if (keep == null) keep = bestMap.get("high");
                            if (keep == null) keep = bestMap.get("medium");
                            if (keep == null) keep = bestMap.get("low");
                            if (keep != null) {
                                newTmpRet.clear();
                                bestMap.clear();
                                newRet.add(keep);
                            }
                        }
                        musicVideo.clear();
                        if (contentXML.contains("channel/deeplink")) break;
                    }

                    if (newRet.size() > 0) newTmpRet = newRet;

                    if (newTmpRet.size() > 0) {
                        /*
                         * only replace original found links by new ones, when
                         * we have some
                         */
                        if (fp != null) {
                            fp.addLinks(newTmpRet);
                            fp.remove(sourceLink);
                        } else if (title != null && newTmpRet.size() > 1) {
                            fp = FilePackage.getInstance();
                            fp.setName(title);
                            fp.addLinks(newTmpRet);
                        }
                        ret = newTmpRet;
                    }
                } else {
                    /*
                     * no other qualities
                     */
                }
            }
            /*
             * little pause needed so the next call does not return trash
             */
            // Thread.sleep(1000);
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return ret;
    }

    @Override
    public String getAGBLink() {
        return "http://www.tape.tv/agb.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath(stream[2]);
        rtmp.setUrl(stream[0]);
        rtmp.setApp(stream[1]);
        rtmp.setResume(true);
        rtmp.setSwfVfy("http://cachinga.tape.tv/static/main-14030.swf");
        rtmp.setRealTime();
        // rtmp.setTimeOut(-3);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getStringProperty("directURL", null) == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String stream[] = downloadLink.getStringProperty("directURL").split("@");
        if (stream[0].startsWith("rtmp")) {
            downloadLink.setProperty("FLVFIXER", true);
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);
            if (!((RTMPDownload) dl).startDownload()) {
                if (downloadLink.getLinkStatus().getStatus() != 513) {
                    // downloadLink.setProperty("directURL", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait a moment...", 5 * 60 * 1000l);
                }
            }

        } else {
            br.setFollowRedirects(true);
            final String dllink = stream[0];
            if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (dllink.startsWith("mms")) throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (mms://) not supported!");
            if (dllink.startsWith("rtsp")) throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (rtsp://) not supported!");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getStringProperty("directURL", null) == null) {
            /* fetch fresh directURL */
            setBrowserExclusive();
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            String mediaUrl = br.getRegex("Flashvars\\.mediaURL\\s+?=\\s+?\"([^\"]+)\"").getMatch(0);

            if (br.containsHTML("(>Dieser Beitrag ist leider.*?nicht \\(mehr\\) verf&uuml;gbar|>Das Video ist in diesem Format.*?aktuell leider nicht verf&uuml;gbar)") || mediaUrl == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.getPage(mediaUrl);
            String app = br.getRegex("<param name=\"app\" value=\"([^\"]+)\"").getMatch(0);
            String host = br.getRegex("<param name=\"host\" value=\"([^\"]+)\"").getMatch(0);

            if (app == null && host == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (!host.startsWith("rtmp://")) host = "rtmp://" + host;
            String q = downloadLink.getStringProperty("directQuality", "high");
            String newUrl[] = br.getRegex("<video dur=\"([\\d:]+)\" paramGroup=\"gl\\-vod\\-rtmp\" src=\"([^\"]+)\" system\\-bitrate=\"(\\d+)\">[^<]+<param name=\"quality\" value=\"" + q + "\"").getRow(0);
            if (newUrl == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setProperty("directURL", host + "@" + app + "@" + newUrl[1]);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void xmlParser(final String linkurl) throws Exception {
        try {
            final URL url = new URL(linkurl);
            final InputStream stream = url.openStream();
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            try {
                doc = parser.parse(new ReplacerInputStream(stream));
            } finally {
                try {
                    stream.close();
                } catch (final Throwable e) {
                }
            }
        } catch (final Throwable e2) {
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.tapetv.best", "Load Best Version ONLY")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.tapetv.loadlow", "Load Low Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MEDIUM, JDL.L("plugins.hoster.tapetv.loadhigh", "Load Medium Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, JDL.L("plugins.hoster.tapetv.loadveryhigh", "Load High Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SD, JDL.L("plugins.hoster.tapetv.loadhd", "Load SD Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.tapetv.loadhd", "Load HD Version")).setDefaultValue(true));

    }

}