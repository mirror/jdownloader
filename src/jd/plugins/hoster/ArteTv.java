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

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "arte.tv", "liveweb.arte.tv", "videos.arte.tv" }, urls = { "http://(www\\.)?arte\\.tv/[a-z]{2}/videos/.+", "http://liveweb\\.arte\\.tv/[a-z]{2}/videos?/.+", "decrypted://(videos|www).arte\\.tv/(guide/[a-z]{2}/[0-9\\-]+|[a-z]{2}/videos)/.+" }, flags = { 32, 32, 32 })
public class ArteTv extends PluginForHost {

    private String              CLIPURL     = null;
    private String              EXPIRED     = null;
    private String              FLASHPLAYER = null;
    private String              clipData;

    private Document            doc;

    private static final String Q_SUBTITLES = "Q_SUBTITLES";
    private static final String Q_BEST      = "Q_BEST";
    private static final String Q_LOW       = "Q_LOW";
    private static final String Q_HIGH      = "Q_HIGH";
    private static final String Q_VERYHIGH  = "Q_VERYHIGH";
    private static final String Q_HD        = "Q_HD";
    private static final String HBBTV       = "HBBTV";
    private static final String THUMBNAIL   = "THUMBNAIL";

    public ArteTv(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decrypted://", "http://"));
    }

    private boolean checkDateExpiration(String s) {
        if (s == null) return false;
        EXPIRED = s;
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
        try {
            Date date = null;
            try {
                date = df.parse(s);
            } catch (Throwable e) {
                df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                date = df.parse(s);
            }
            if (date.getTime() < System.currentTimeMillis()) return true;
            SimpleDateFormat dfto = new SimpleDateFormat("dd. MMM yyyy 'ab' HH:mm 'Uhr'");
            EXPIRED = dfto.format(date);
        } catch (Throwable e) {
            return false;
        }
        return false;
    }

    private void download(DownloadLink downloadLink) throws Exception {
        if (CLIPURL.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, CLIPURL);
            setupRTMPConnection(dl);
            if (!((RTMPDownload) dl).startDownload()) {
                if (downloadLink.getBooleanProperty("STREAMURLISEXPIRED", false)) {
                    downloadLink.setProperty("STREAMURLISEXPIRED", false);
                    refreshStreamingUrl(downloadLink.getStringProperty("tvguideUrl", null), downloadLink);
                }
            }
        } else if (CLIPURL.startsWith("http")) {
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, CLIPURL, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.arte.tv/de/Allgemeine-Nutzungsbedingungen/3664116.html";
    }

    private String getClipData(String tag) {
        return new Regex(clipData, "<" + tag + ">(.*?)</" + tag + ">").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void refreshStreamingUrl(String s, DownloadLink d) throws Exception {
        if (s == null) return;
        br.getPage(s);
        HashMap<String, HashMap<String, String>> streamValues = new HashMap<String, HashMap<String, String>>();
        HashMap<String, String> streamValue;
        String vsr = br.getRegex("\"VSR\":\\{(.*?\\})\\}").getMatch(0);
        if (vsr != null) {
            for (String[] ss : new Regex(vsr, "\"(.*?)\"\\s*:\\s*\\{(.*?)\\}").getMatches()) {
                streamValue = new HashMap<String, String>();
                for (String[] peng : new Regex(ss[1], "\"(.*?)\"\\s*:\\s*\"?(.*?)\"?,").getMatches()) {
                    streamValue.put(peng[0], peng[1]);
                }
                streamValues.put(ss[0], streamValue);
            }
            String streamingType = d.getStringProperty("streamingType", null);
            if (streamingType == null) return;
            if (streamValues.containsKey(streamingType)) {
                streamValue = new HashMap<String, String>(streamValues.get(streamingType));
                String url = streamValue.get("url");
                if (!url.startsWith("mp4:")) url = "mp4:" + url;
                d.setProperty("directURL", streamValue.get("streamer") + url);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        String link = downloadLink.getDownloadURL();

        if (downloadLink.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String lang = new Regex(link, "http://\\w+.arte.tv/(guide/)?(\\w+)/.+").getMatch(1);
        lang = lang != null && "de".equalsIgnoreCase(lang) ? "De" : lang;
        lang = lang != null && "fr".equalsIgnoreCase(lang) ? "Fr" : lang;

        String expiredBefore = null, expiredAfter = null, status = null, fileName = null, ext = "";
        clipData = br.getPage(link);
        if (br.getURL().contains("method=getHome")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">Diese Seite existiert leider nicht mehr")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        if (!"Error 404".equalsIgnoreCase(getClipData("title")) || lang == null) {
            HashMap<String, String> paras;

            if (link.startsWith("http://liveweb.arte.tv")) {
                paras = requestLivewebArte();
                expiredBefore = paras.get("dateEvent");
                expiredAfter = paras.get("dateExpiration");
                fileName = paras.get("name" + lang);
                CLIPURL = paras.get("urlHd");
                CLIPURL = CLIPURL == null ? paras.get("urlSd") : CLIPURL;
                if (CLIPURL == null) {
                    if (paras.get("liveUrl") != null) {
                        fileName += "__LIVE";
                        downloadLink.setProperty("ISLIVE", true);
                        CLIPURL = paras.get("liveUrl");
                    }
                }
            } else if (link.startsWith("http://www.arte.tv/guide/")) {
                expiredBefore = downloadLink.getStringProperty("VRA", null);
                expiredAfter = downloadLink.getStringProperty("VRU", null);
                fileName = downloadLink.getStringProperty("directName", null);
                CLIPURL = downloadLink.getStringProperty("directURL", null);
                FLASHPLAYER = downloadLink.getStringProperty("flashplayer", null);
            } else {
                paras = requestVideosArte();
                expiredBefore = paras.get("dateVideo");
                expiredAfter = paras.get("dateExpiration");
                fileName = paras.get("name");
                CLIPURL = paras.get("hd");
                CLIPURL = CLIPURL == null ? paras.get("sd") : CLIPURL;
            }
        }

        if (expiredBefore != null && !checkDateExpiration(expiredBefore)) {
            if ("de".equalsIgnoreCase(lang)) {
                status = "Dieses Video steht erst ab dem " + EXPIRED + " zur Verfügung!";
            } else {
                status = "Cette vidéo est disponible uniquement à partir de " + EXPIRED + "!";
            }
        }
        if (checkDateExpiration(expiredAfter)) {
            if ("de".equalsIgnoreCase(lang)) {
                status = "Dieses Video ist seit dem " + EXPIRED + " nicht mehr verfügbar!";
            } else {
                status = "Cette vidéo n'est plus disponible depuis " + EXPIRED + "!";
            }
        }
        if (fileName.matches("(Video nicht verfügbar\\.|vidéo indisponible\\.)")) {
            if ("de".equalsIgnoreCase(lang)) {
                status = "Dieses Video ist zur Zeit nicht verfügbar!";
            } else {
                status = "Cette vidéo n'est plus actuellement pas disponible!";
            }
        }
        if (status != null) {
            logger.warning(status);
            if (fileName != null) downloadLink.setName(fileName);
            return AvailableStatus.UNCHECKABLE;
        }

        if (fileName == null || CLIPURL == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        if (!link.startsWith("http://www.arte.tv/guide/")) {
            ext = CLIPURL.substring(CLIPURL.lastIndexOf("."), CLIPURL.length());
            if (ext.length() > 4) {
                ext = new Regex(ext, Pattern.compile("\\w/(mp4):", Pattern.CASE_INSENSITIVE)).getMatch(0);
            }
            ext = ext == null ? ".flv" : "." + ext;
        }

        if (fileName.endsWith(".")) {
            fileName = fileName.substring(0, fileName.length() - 1);
        }
        if ("HBBTV".equals(downloadLink.getStringProperty("streamingType", "RTMP"))) {
            URLConnectionAdapter con = null;
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            try {
                con = br2.openGetConnection(CLIPURL);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        downloadLink.setFinalFileName(fileName.trim() + ext);
        if (downloadLink.getBooleanProperty("ISLIVE", false)) {
            logger.info("arte.live.tv: Live videos not downloadable! --> " + link);
            return AvailableStatus.FALSE;
        }
        return AvailableStatus.TRUE;
    }

    private HashMap<String, String> requestLivewebArte() throws Exception {
        HashMap<String, String> paras = new HashMap<String, String>();
        String eventId = br.getRegex("eventId=(\\d+)").getMatch(0);
        FLASHPLAYER = br.getRegex("(http://[^\\?\"]+player.swf)").getMatch(0);

        XPath xPath = xmlParser("http://arte.vo.llnwd.net/o21/liveweb/events/event-" + eventId + ".xml?" + System.currentTimeMillis());
        NodeList modules = (NodeList) xPath.evaluate("//event[@id=" + eventId + "]/*|//event/video[@id]/*", doc, XPathConstants.NODESET);

        for (int i = 0; i < modules.getLength(); i++) {
            Node node = modules.item(i);
            if (node.getNodeName().matches("postrolls|categories|partner|live|chapters|video")) {
                if (node.getNodeName().matches("live")) {
                    NodeList t = node.getChildNodes();
                    for (int j = 0; j < t.getLength(); j++) {
                        Node g = t.item(j);
                        if ("#text".equals(g.getNodeName())) continue;
                        if ("liveUrl".equals(g.getNodeName())) {
                            paras.put(g.getNodeName(), g.getTextContent());
                        }
                    }
                }
                continue;
            }

            System.out.println(node.getNodeName() + ":" + node.getTextContent());
            paras.put(node.getNodeName(), node.getTextContent());
        }
        return paras;
    }

    private HashMap<String, String> requestVideosArte() throws Exception {
        HashMap<String, String> paras = new HashMap<String, String>();
        String tmpUrl = br.getRegex("ajaxUrl:\'([^<>]+view,)").getMatch(0);
        FLASHPLAYER = br.getRegex("<param name=\"movie\" value=\"(.*?)\\?").getMatch(0);
        if (FLASHPLAYER == null || tmpUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        XPath xPath = xmlParser("http://videos.arte.tv" + tmpUrl + "strmVideoAsPlayerXml.xml");
        NodeList modules = (NodeList) xPath.evaluate("/video/*|//urls/*", doc, XPathConstants.NODESET);

        for (int i = 0; i < modules.getLength(); i++) {
            Node node = modules.item(i);
            if (node.hasAttributes()) {
                paras.put(node.getAttributes().item(0).getTextContent(), node.getTextContent());
            } else {
                paras.put(node.getNodeName(), node.getTextContent());
            }
        }
        return paras;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        String type = new Regex(CLIPURL, "(?i)/(mp4:(liveweb|geo))").getMatch(0);
        String[] uri = CLIPURL.split("(?i)/" + type);
        if (uri != null && uri.length == 2) {
            rtmp.setPlayPath(type + uri[1]);
            rtmp.setUrl(uri[0]);
            rtmp.setApp(new Regex(uri[0], "rtmp.?://[^/]+/(.*?)$").getMatch(0));
        } else {
            rtmp.setUrl(CLIPURL);
        }
        rtmp.setSwfVfy(FLASHPLAYER);
        rtmp.setResume(true);
    }

    private XPath xmlParser(String linkurl) throws Exception {
        InputStream stream = null;
        try {
            URL url = new URL(linkurl);
            stream = url.openStream();
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            XPath xPath = XPathFactory.newInstance().newXPath();
            doc = parser.parse(stream);
            return xPath;
        } catch (Throwable e2) {
            return null;
        } finally {
            try {
                stream.close();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public String getDescription() {
        return "JDownloader's Arte Plugin helps downloading videoclips from arte.tv. Arte+7 provides different video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SUBTITLES, JDL.L("plugins.hoster.arte.subtitles", "Download subtitle whenever possible")).setEnabled(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.arte.best", "Load Best Version ONLY")).setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.arte.loadlow", "Load low version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, JDL.L("plugins.hoster.arte.loadhigh", "Load high version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_VERYHIGH, JDL.L("plugins.hoster.arte.loadveryhigh", "Load veryhigh version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.arte.loadhd", "Load HD version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), HBBTV, JDL.L("plugins.hoster.arte.usehbbtv", "Load only http streams (HbbTV)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), THUMBNAIL, JDL.L("plugins.hoster.arte.loadthumbnail", "Load thumbnail")).setDefaultValue(false));
    }

}