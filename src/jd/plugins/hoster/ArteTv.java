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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "arte.tv", "concert.arte.tv", "creative.arte.tv", "future.arte.tv", "cinema.arte.tv", "theoperaplatform.eu", "info.arte.tv" }, urls = { "http://arte\\.tv\\.artejd_decrypted_jd/\\d+", "http://concert\\.arte\\.tv\\.artejd_decrypted_jd/\\d+", "http://creative\\.arte\\.tv\\.artejd_decrypted_jd/\\d+", "http://future\\.arte\\.tv\\.artejd_decrypted_jd/\\d+", "http://cinema\\.arte\\.tv\\.artejd_decrypted_jd/\\d+", "http://theoperaplatform\\.eu\\.artejd_decrypted_jd/\\d+", "http://info\\.arte\\.tv\\.artejd_decrypted_jd/\\d+" })
public class ArteTv extends PluginForHost {
    private static final String V_NORMAL                   = "V_NORMAL";
    private static final String V_SUBTITLED                = "V_SUBTITLED";
    private static final String V_SUBTITLE_DISABLED_PEOPLE = "V_SUBTITLE_DISABLED_PEOPLE";
    private static final String V_AUDIO_DESCRIPTION        = "V_AUDIO_DESCRIPTION";
    private static final String http_300                   = "http_300";
    private static final String http_800                   = "http_800";
    private static final String http_1500                  = "http_1500";
    private static final String http_2200                  = "http_2200";
    private static final String hls                        = "hls";
    /* creative.arte.tv extern qualities */
    private static final String http_extern_1000           = "http_extern_1000";
    private static final String hls_extern_250             = "hls_extern_250";
    private static final String hls_extern_500             = "hls_extern_500";
    private static final String hls_extern_1000            = "hls_extern_1000";
    private static final String hls_extern_2000            = "hls_extern_2000";
    private static final String hls_extern_4000            = "hls_extern_4000";
    private static final String LOAD_LANGUAGE_URL          = "LOAD_LANGUAGE_URL";
    private static final String LOAD_BEST                  = "LOAD_BEST";
    private static final String LOAD_LANGUAGE_GERMAN       = "LOAD_LANGUAGE_GERMAN";
    private static final String LOAD_LANGUAGE_FRENCH       = "LOAD_LANGUAGE_FRENCH";
    private static final String THUMBNAIL                  = "THUMBNAIL";
    private static final String FAST_LINKCHECK             = "FAST_LINKCHECK";
    private static final String TYPE_GUIDE                 = "http://www\\.arte\\.tv/guide/[a-z]{2}/.+";
    private static final String TYPE_CONCERT               = "http://(www\\.)?concert\\.arte\\.tv/.+";
    private String              dllink                     = null;
    private String              quality_intern             = null;

    @SuppressWarnings("deprecation")
    public ArteTv(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("jd_decrypted_jd", ""));
    }

    @Override
    public String getAGBLink() {
        return "http://www.arte.tv/de/Allgemeine-Nutzungsbedingungen/3664116.html";
    }

    /** Important information: RTMP player: http://www.arte.tv/player/v2//jwplayer6/mediaplayer.6.3.3242.swf */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        quality_intern = downloadLink.getStringProperty("quality_intern", null);
        br.setFollowRedirects(true);
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String apiurl = downloadLink.getStringProperty("apiurl", null);
        final String link = downloadLink.getStringProperty("mainlink", null);
        final String lang = downloadLink.getStringProperty("langShort", null);
        String expiredBefore = null, expiredAfter = null, status = null, fileName = null, ext = "";
        br.getPage(apiurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        expiredBefore = downloadLink.getStringProperty("VRA", null);
        expiredAfter = downloadLink.getStringProperty("VRU", null);
        fileName = downloadLink.getStringProperty("directName", null);
        dllink = downloadLink.getStringProperty("directURL", null);
        if (expiredBefore != null && expiredAfter != null) {
            status = getExpireMessage(lang, expiredBefore, expiredAfter);
            /* TODO: Improve this case! */
            if (status != null) {
                logger.warning(status);
                downloadLink.setName(status + "_" + fileName);
                return AvailableStatus.FALSE;
            }
        }
        if (fileName == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!link.matches(TYPE_GUIDE) && !link.matches(TYPE_CONCERT)) {
            ext = dllink.substring(dllink.lastIndexOf("."), dllink.length());
            if (ext.length() > 4) {
                ext = new Regex(ext, Pattern.compile("\\w/(mp4):", Pattern.CASE_INSENSITIVE)).getMatch(0);
            }
            ext = ext == null ? ".flv" : "." + ext;
        }
        /* We can only check the filesizes of http urls */
        if (quality_intern.contains("http_")) {
            URLConnectionAdapter con = null;
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            try {
                con = br2.openHeadConnection(dllink);
                final long contentLength = con.getLongContentLength();
                if (con.isOK() && !con.getContentType().contains("html")) {
                    if (contentLength > 1000) {
                        /* Only show filesize if we're sure that it definitly is a file. */
                        downloadLink.setDownloadSize(contentLength);
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else if (quality_intern.contains("hls_")) {
        }
        downloadLink.setFinalFileName(fileName);
        return AvailableStatus.TRUE;
    }

    public static String getExpireMessage(final String lang, final String expiredBefore, final String expiredAfter) {
        String expired_message = null;
        if (expiredBefore != null && !checkDateExpiration(expiredBefore)) {
            expired_message = String.format(getPhrase("ERROR_CONTENT_NOT_AVAILABLE_YET"), getNiceDate(expiredBefore));
        }
        if (checkDateExpiration(expiredAfter)) {
            expired_message = String.format(getPhrase("ERROR_CONTENT_NOT_AVAILABLE_ANYMORE_COPYRIGHTS_EXPIRED"), getNiceDate(expiredAfter));
        }
        return expired_message;
    }

    /** Checks if a date is expired or not yet passed. */
    public static boolean checkDateExpiration(String s) {
        if (s == null) {
            return false;
        }
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
        try {
            Date date = null;
            try {
                date = df.parse(s);
            } catch (Throwable e) {
                df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                date = df.parse(s);
            }
            if (date.getTime() < System.currentTimeMillis()) {
                return true;
            }
        } catch (Throwable e) {
            return false;
        }
        return false;
    }

    public static String getNiceDate(final String input) {
        String nicedate = null;
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
        SimpleDateFormat convdf;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            convdf = new SimpleDateFormat("dd.MMMM.yyyy '_' HH-mm 'Uhr'", Locale.GERMANY);
        } else {
            convdf = new SimpleDateFormat("MMMM.dd.yyyy '_' hh-mm 'o clock'", Locale.ENGLISH);
        }
        try {
            Date date = null;
            try {
                date = df.parse(input);
                nicedate = convdf.format(date);
            } catch (Throwable e) {
            }
        } catch (Throwable e) {
        }
        return nicedate;
    }

    public static String getNiceDate2(final String input) {
        String nicedate = null;
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss ZZZ", Locale.getDefault());
        SimpleDateFormat convdf;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            convdf = new SimpleDateFormat("dd.MMMM.yyyy '_' HH-mm 'Uhr'", Locale.GERMANY);
        } else {
            convdf = new SimpleDateFormat("MMMM.dd.yyyy '_' hh-mm 'o clock'", Locale.ENGLISH);
        }
        try {
            Date date = null;
            try {
                date = df.parse(input);
                nicedate = convdf.format(date);
            } catch (Throwable e) {
            }
        } catch (Throwable e) {
        }
        return nicedate;
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (quality_intern.startsWith("rtmp_")) {
            downloadRTMP(downloadLink);
        } else if (quality_intern.startsWith("http_")) {
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else if (quality_intern.startsWith("hls_")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    /* Currently not used! */
    private void downloadRTMP(final DownloadLink downloadLink) throws Exception {
        if (dllink.startsWith("rtmp")) {
            try {
                dl = new RTMPDownload(this, downloadLink, dllink);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            setupRTMPConnection(dl);
            if (!((RTMPDownload) dl).startDownload()) {
                if (downloadLink.getBooleanProperty("STREAMURLISEXPIRED", false)) {
                    downloadLink.setProperty("STREAMURLISEXPIRED", false);
                    refreshStreamingUrl(downloadLink.getStringProperty("tvguideUrl", null), downloadLink);
                }
            }
        }
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

    /* TODO: Fix! */
    private void refreshStreamingUrl(String s, DownloadLink d) throws Exception {
        if (s == null) {
            return;
        }
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
            if (streamingType == null) {
                return;
            }
            if (streamValues.containsKey(streamingType)) {
                streamValue = new HashMap<String, String>(streamValues.get(streamingType));
                String url = streamValue.get("url");
                if (!url.startsWith("mp4:")) {
                    url = "mp4:" + url;
                }
                d.setProperty("directURL", streamValue.get("streamer") + url);
            }
        }
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        setupRtmp(rtmp, dllink);
        rtmp.setResume(true);
    }

    public void setupRtmp(jd.network.rtmp.url.RtmpUrlConnection rtmp, String clipuri) {
        String app = new Regex(clipuri, "rtmp://[^\\/]+/(.*?)/").getMatch(0);
        String playPath = new Regex(clipuri, "rtmp://[^\\/]+/(.*?)/(.+)").getMatch(1);
        String tcUrl = new Regex(clipuri, "(rtmp://[^\\/]+/.*?/)").getMatch(0);
        String arteVpLang = br.getRegex("arte_vp_url=\'(.*?)\'").getMatch(0);
        String pageUrl = "http://www.arte.tv/player/v2/index.php?json_url=" + Encoding.urlTotalEncode(arteVpLang) + "&lang=de_DE&config=arte_tvguide&rendering_place=" + Encoding.urlTotalEncode(br.getURL());
        rtmp.setSwfVfy("http://www.arte.tv/arte_vp/jwplayer6/6.9.4867/jwplayer.flash.6.9.4867.swf");
        rtmp.setTcUrl(tcUrl);
        rtmp.setPageUrl(pageUrl);
        rtmp.setPlayPath(playPath);
        rtmp.setApp(app + "/");
        rtmp.setUrl(clipuri);
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

    @Override
    public String getDescription() {
        return "JDownloader's Arte Plugin helps downloading videoclips from arte.tv. Arte+7 provides different video qualities.";
    }

    public static HashMap<String, String> phrasesEN = new HashMap<String, String>(new HashMap<String, String>() {
        {
            put("ERROR_USER_NEEDS_TO_CHANGE_FORMAT_SELECTION", "Check_your_plugin_settings_activate_missing_formats_e_g_subtitled_versions_or_other_language_versions_");
            put("ERROR_CONTENT_NOT_AVAILABLE_ANYMORE_COPYRIGHTS_EXPIRED", "This video is not available anymore since %s!_");
            put("ERROR_CONTENT_NOT_AVAILABLE_YET", "This content is not available yet. It will be available from the %s!_");
        }
    });
    public static HashMap<String, String> phrasesDE = new HashMap<String, String>(new HashMap<String, String>() {
        {
            put("ERROR_USER_NEEDS_TO_CHANGE_FORMAT_SELECTION", "Überprüfe_deine_Plugineinstellungen_aktiviere_fehlende_Formate_z_B_Untertitelte_Version_oder_andere_Sprachversionen_");
            put("ERROR_CONTENT_NOT_AVAILABLE_ANYMORE_COPYRIGHTS_EXPIRED", "Dieses Video ist seit dem %s nicht mehr verfügbar!_");
            put("ERROR_CONTENT_NOT_AVAILABLE_YET", "Dieses Video ist noch nicht verfügbar. Es ist erst ab dem %s verfügbar!_");
        }
    });

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    public static String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), LOAD_BEST, JDL.L("plugins.hoster.arte.loadbest", "Nur die beste Qualität(aus der Auswahl) wählen?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Auswahl der Qualitätsstufen für externe creative.arte.tv Videos:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Auswahl der immer verfügbaren http Qualitätsstufen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), http_extern_1000, JDL.L("plugins.hoster.arte.http_extern_1000", "1000kBit/s 504x284")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Auswahl der immer verfügbaren hls Qualitätsstufen für spezielle externe creative.arte.tv Videos:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), hls_extern_250, JDL.L("plugins.hoster.arte.hls_extern_250", "250kBit/s 192x144 oder ähnlich")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), hls_extern_500, JDL.L("plugins.hoster.arte.hls_extern_500", "500kBit/s 308x232 oder ähnlich")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), hls_extern_1000, JDL.L("plugins.hoster.arte.hls_extern_1000", "1000kBit/s 496x372 oder ähnlich")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), hls_extern_2000, JDL.L("plugins.hoster.arte.hls_extern_2000", "2000kBit/s 720x408 oder ähnlich")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), hls_extern_4000, JDL.L("plugins.hoster.arte.hls_extern_4000", "4000kBit/s oder ähnlich")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Auswahl der Qualitätsstufen für normale creative.arte.tv/concert.arte.tv/arte.tv Videos:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), hls, JDL.L("plugins.hoster.arte.hls", "HLS")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Auswahl der manchmal verfügbaren Qualitätsstufen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), http_300, JDL.L("plugins.hoster.arte.http_300", "300kBit/s 384x216 (http)")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Auswahl der immer verfügbaren Qualitätsstufen:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), http_800, JDL.L("plugins.hoster.arte.http_800", "800kBit/s 720x406 (http)")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), http_1500, JDL.L("plugins.hoster.arte.http_1500", "1500kBit/s 720x406 (http)")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), http_2200, JDL.L("plugins.hoster.arte.http_2200", "2200kBit/s 1280x720 (http)")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Folgende Version(en) laden sofern verfügbar:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), V_NORMAL, JDL.L("plugins.hoster.arte.V_NORMAL", "Normale Version (ohne Untertitel)")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), V_SUBTITLED, JDL.L("plugins.hoster.arte.V_SUBTITLED", "Untertitelt")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), V_SUBTITLE_DISABLED_PEOPLE, JDL.L("plugins.hoster.arte.V_SUBTITLE_DISABLED_PEOPLE", "Untertitelt für Hörgeschädigte")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), V_AUDIO_DESCRIPTION, JDL.L("plugins.hoster.arte.V_AUDIO_DESCRIPTION", "Audio Deskription")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Auswahl der Sprachversionen:"));
        final ConfigEntry cfge = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), LOAD_LANGUAGE_URL, JDL.L("plugins.hoster.arte.LOAD_LANGUAGE_URL", "Sprachausgabe der URL laden (je nach dem, ob '/de/' oder '/fr/' im eingefügten Link steht)?\r\n<html><b>WICHTIG: Falls nur eine Sprachversion verfügbar ist, aber beide ausgewählt sind kann es passieren, dass diese doppelt (als deutsch und französisch gleicher Inhalt mit verschiedenen Dateinamen) im Linkgrabber landet!</b></html>")).setDefaultValue(true);
        getConfig().addEntry(cfge);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), LOAD_LANGUAGE_GERMAN, JDL.L("plugins.hoster.arte.LOAD_LANGUAGE_GERMAN", "Sprachausgabe Deutsch laden?")).setDefaultValue(false).setEnabledCondidtion(cfge, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), LOAD_LANGUAGE_FRENCH, JDL.L("plugins.hoster.arte.LOAD_LANGUAGE_FRENCH", "Sprachausgabe Französisch laden?")).setDefaultValue(false).setEnabledCondidtion(cfge, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Sonstiges:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), THUMBNAIL, JDL.L("plugins.hoster.arte.loadthumbnail", "Thumbnail laden?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, JDL.L("plugins.hoster.arte.fastlinkcheck", "Schnellen Linkcheck aktivieren?\r\n<html><b>WICHTIG: Dadurch erscheinen die Links schneller im Linkgrabber, aber die Dateigröße wird erst beim Downloadstart (oder manuellem Linkcheck) angezeigt.</b></html>")).setDefaultValue(true));
    }
}