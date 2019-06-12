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
package jd.plugins.hoster;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "music.163.com" }, urls = { "http://(www\\.)?music\\.163\\.com/(?:#/)?(?:song|mv)\\?id=\\d+|decrypted://music\\.163\\.comcover\\d+" })
public class Music163Com extends PluginForHost {
    @SuppressWarnings("deprecation")
    public Music163Com(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://music.163.com/html/web2/service.html";
    }

    /** Settings stuff */
    private static final String           FAST_LINKCHECK          = "FAST_LINKCHECK";
    private static final String           GRAB_COVER              = "GRAB_COVER";
    private static final String           SETTING_CUSTOM_FILENAME = "SETTING_CUSTOM_FILENAME_2";
    private static final String           SETTING_CUSTOM_DATE     = "SETTING_CUSTOM_DATE";
    private static final String           TYPE_MUSIC              = "http://(www\\.)?music\\.163\\.com/(?:#/)?song\\?id=\\d+";
    private static final String           TYPE_VIDEO              = "http://(www\\.)?music\\.163\\.com/(?:#/)?mv\\?id=\\d+";
    private static final String           TYPE_COVER              = "decrypted://music\\.163\\.comcover\\d+";
    /** TODO 2016-02-??: server seem to have changed from m5.music.126.net to ?.music.126.net or linkformat has changed or encryption. */
    /* 2016-02-15 testing m9 server */
    private static final String           dlurl_format            = "http://m9.music.126.net/%s/%s.mp3";
    /* Qualities from highest to lowest in KB/s: 320, 160, 96. 'hMusic' is officially only available for logged-in users! */
    public static final String[]          audio_qualities         = { "hMusic", "mMusic", "lMusic", "bMusic" };
    public static final String[]          video_qualities         = { "1080", "720", "360", "240" };
    public static final String            dateformat_en           = "yyyy-MM-dd";
    /* Connection stuff */
    private static final boolean          FREE_RESUME             = true;
    private static final int              FREE_MAXCHUNKS          = 0;
    private static final int              FREE_MAXDOWNLOADS       = 20;
    private String                        DLLINK                  = null;
    private LinkedHashMap<String, Object> entries                 = null;

    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);
    /*
     * API documentation:
     * https://github.com/yanunon/NeteaseCloudMusic/wiki/%E7%BD%91%E6%98%93%E4%BA%91%E9%9F%B3%E4%B9%90API%E5%88%86%E6%9E%90
     */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        String content_title = null;
        String filename = null;
        /*
         * First try to get publishedTimestamp from decrypter as an artist can have multiple albums with different timestamps but if the
         * user added an album we know which timestamp is definitly the correct one!
         */
        long publishedTimestamp = link.getLongProperty("publishedTimestamp", 0);
        String contentid = link.getStringProperty("contentid", null);
        String artist = null;
        String ext = null;
        String name_album = null;
        DLLINK = null;
        entries = null;
        long filesize = 0;
        this.setBrowserExclusive();
        prepareAPI(this.br);
        if (contentid == null) {
            contentid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        }
        if (link.getDownloadURL().matches(TYPE_COVER)) {
            DLLINK = link.getStringProperty("directlink", null);
            filesize = getFilesizeFromHeader(DLLINK);
        } else if (link.getDownloadURL().matches(TYPE_VIDEO)) {
            link.setLinkID(new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
            /* Make sure that offline links also have nice filenames. */
            link.setName(link.getLinkID() + ".mp4");
            br.getPage("http://music.163.com/api/mv/detail?id=" + link.getLinkID() + "&type=mp4");
            if (br.getHttpConnection().getResponseCode() != 200) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            entries = (LinkedHashMap<String, Object>) entries.get("data");
            artist = (String) entries.get("artistName");
            content_title = (String) entries.get("name");
            final String publishDate = (String) entries.get("publishTime");
            if (publishDate != null) {
                publishedTimestamp = TimeFormatter.getMilliSeconds(publishDate, "yyyy-MM-dd", Locale.ENGLISH);
            }
            entries = (LinkedHashMap<String, Object>) entries.get("brs");
            for (final String quality : video_qualities) {
                DLLINK = (String) entries.get(quality);
                if (DLLINK != null) {
                    break;
                }
            }
            if (artist == null || content_title == null || DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filesize = getFilesizeFromHeader(DLLINK);
            ext = "mp4";
        } else {
            link.setLinkID(new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
            /* Make sure that offline links also have nice filenames. */
            link.setName(link.getLinkID() + ".mp3");
            br.getPage("http://music.163.com/api/song/detail/?id=" + link.getLinkID() + "&ids=%5B" + link.getLinkID() + "%5D");
            if (br.getHttpConnection().getResponseCode() != 200) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> songs = (ArrayList) entries.get("songs");
            if (songs == null || songs.size() == 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = (LinkedHashMap<String, Object>) songs.get(0);
            final ArrayList<Object> artists = (ArrayList) entries.get("artists");
            final LinkedHashMap<String, Object> album_info = (LinkedHashMap<String, Object>) entries.get("album");
            final LinkedHashMap<String, Object> artist_info = (LinkedHashMap<String, Object>) artists.get(0);
            /* Now find the highest quality available (check later down below if it is actually downloadable) */
            for (final String quality : audio_qualities) {
                final Object musicO = entries.get(quality);
                if (musicO != null) {
                    final LinkedHashMap<String, Object> musicmap = (LinkedHashMap<String, Object>) musicO;
                    ext = (String) musicmap.get("extension");
                    filesize = JavaScriptEngineFactory.toLong(musicmap.get("size"), -1);
                    break;
                }
            }
            artist = (String) artist_info.get("name");
            name_album = (String) album_info.get("name");
            content_title = (String) entries.get("name");
            if (publishedTimestamp < 1) {
                publishedTimestamp = JavaScriptEngineFactory.toLong(album_info.get("publishTime"), 0);
            }
            if (artist == null || name_album == null || content_title == null || ext == null || filesize == -1) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* Set data - make sure not to overwrite decrypter-data! */
        link.setProperty("contentid", contentid);
        if (link != null) {
            link.setProperty("directtitle", content_title);
        }
        if (artist != null) {
            link.setProperty("directartist", artist);
        }
        if (ext != null) {
            link.setProperty("type", ext);
        }
        if (name_album != null) {
            link.setProperty("directalbum", name_album);
        }
        if (publishedTimestamp > 0) {
            link.setProperty("originaldate", publishedTimestamp);
        }
        filename = getFormattedFilename(link);
        link.setFinalFileName(filename);
        link.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (downloadLink.getDownloadURL().matches(TYPE_MUSIC)) {
            /* Get- and set lyrics if possible */
            try {
                final Browser br2 = br.cloneBrowser();
                br2.getPage("http://music.163.com/api/song/lyric?id=" + downloadLink.getLinkID() + "&lv=-1&tv=-1&csrf_token=");
                LinkedHashMap<String, Object> entries_lyrics = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br2.toString());
                entries_lyrics = (LinkedHashMap<String, Object>) entries.get("lrc");
                final String lyrics = (String) entries_lyrics.get("lyric");
                downloadLink.setComment(lyrics);
                logger.info("Successfully set lyrics");
            } catch (final Throwable e) {
                logger.warning("Failed to get/set lyrics");
            }
            /* Now find the highest DOWNLOADABLE quality available */
            boolean trackDownloadableViaEncryptedUrl = false;
            for (final String quality : audio_qualities) {
                if (this.isAbort()) {
                    logger.info("User stopped downloads --> Stepping out of 'quality loop'");
                    /* Avoid 'freezing' when user stops downloads */
                    throw new PluginException(LinkStatus.ERROR_RETRY, "User aborted download");
                }
                final Object musicO = entries.get(quality);
                if (musicO != null) {
                    final LinkedHashMap<String, Object> musicmap = (LinkedHashMap<String, Object>) musicO;
                    final String dfsid = Long.toString(JavaScriptEngineFactory.toLong(musicmap.get("dfsId"), -1));
                    final String encrypted_dfsid = encrypt_dfsId(dfsid);
                    /*
                     * bMusic (and often/alyways) lMusic == mp3Url - in theory we don't have to generate the final downloadlink for these
                     * cases as it is already given.
                     */
                    DLLINK = String.format(dlurl_format, encrypted_dfsid, dfsid);
                    try {
                        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
                    } catch (final BrowserException e) {
                        /*
                         * Avoid timeouts and other nasty things --> Then check availablestatus again here - if there are real connection /
                         * server issues it will just stop here
                         */
                        requestFileInformation(downloadLink);
                        continue;
                    }
                    /* Sometimes HQ versions of songs are officially available but directlinks return 404 on download attempt. */
                    if (dl.getConnection().getResponseCode() != 200 || dl.getConnection().getContentType().contains("html")) {
                        logger.info("Version " + quality + " is NOT downloadable");
                        continue;
                    }
                    logger.info("Version " + quality + " is downloadable --> Starting download");
                    trackDownloadableViaEncryptedUrl = true;
                    break;
                }
            }
            if (!trackDownloadableViaEncryptedUrl) {
                /* Last chance handling */
                DLLINK = (String) entries.get("mp3Url");
                if (DLLINK == null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - track is not downloadable at the moment", 30 * 60 * 1000l);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
            }
        } else {
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            /* We're using the API so nothing can really break */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
        }
        downloadLink.setProperty(directlinkproperty, DLLINK);
        dl.startDownload();
    }

    private long getFilesizeFromHeader(final String url) throws IOException, PluginException {
        URLConnectionAdapter con = null;
        long filesize = 0;
        try {
            con = br.openHeadConnection(url);
            if (!con.getContentType().contains("html")) {
                filesize = con.getLongContentLength();
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return filesize;
    }

    /*
     * Thx http://moonlib.com/606.html , https://github.com/sk1418/zhuaxia/blob/master/zhuaxia/netease.py and
     * https://github.com/PeterDing/iScript/blob/master/music.163.com.py Additional thanks to user "cryzed" :)
     */
    private String encrypt_dfsId(final String dfsid) throws NoSuchAlgorithmException {
        String result = "";
        byte[] byte1 = "3go8&$8*3*3h0k(2)2".getBytes();
        byte[] byte2 = dfsid.getBytes();
        final int byte1_len = byte1.length;
        for (int i = 0; i < byte2.length; i++) {
            byte2[i] = (byte) (byte2[i] ^ byte1[i % byte1_len]);
        }
        final byte[] md5bytes = MessageDigest.getInstance("MD5").digest(byte2);
        final String b64 = Base64.encodeToString(md5bytes);
        /*
         * In the above linked Python examples it seems like they remove the last character of the base64 String but it seems like this is
         * not necessary.
         */
        result = b64.replace("/", "_");
        result = result.replace("+", "-");
        return result;
    }

    public static void prepareAPI(final Browser br) {
        br.getHeaders().put("Referer", "http://music.163.com/");
        /* User-Agent no necessarily needed! */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36");
        /* Last updated: 2014-12-08 */
        br.setCookie("http://music.163.com/", "appver", "2.0.2");
        br.setAllowedResponseCodes(400);
        /* API is very slow sometimes! */
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        br.setLoadLimit(br.getLoadLimit() * 3);
    }

    @Override
    public String getDescription() {
        return "JDownloader's music.163.com plugin helps downloading audio files from music.163.com.";
    }

    /** Returns either the original server filename or one that is very similar to the original */
    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("music.163.com");
        final String ext = downloadLink.getStringProperty("type", null);
        final String artist = downloadLink.getStringProperty("directartist", "-");
        final String album = downloadLink.getStringProperty("directalbum", "-");
        final String title = downloadLink.getStringProperty("directtitle", "-");
        final String contentid = downloadLink.getStringProperty("contentid", null);
        final String tracknumber = downloadLink.getStringProperty("tracknumber", "-");
        /* Date: Maybe add this in the future, if requested by a user. */
        final long date = downloadLink.getLongProperty("originaldate", -1);
        String formattedDate = null;
        if (date == -1) {
            /* No date given */
            formattedDate = "-";
        } else {
            /* Get correctly formatted date */
            String dateFormat = cfg.getStringProperty(SETTING_CUSTOM_DATE, defaultCustomDate);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            Date theDate = new Date(date);
            try {
                formatter = new SimpleDateFormat(dateFormat);
                formattedDate = formatter.format(theDate);
            } catch (Exception e) {
                /* prevent user error killing plugin */
                formattedDate = "-";
            }
        }
        String formattedFilename = cfg.getStringProperty(SETTING_CUSTOM_FILENAME, defaultCustomFilename);
        if (!formattedFilename.contains("*title*") && !formattedFilename.contains("*contentid*") && !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename;
        }
        formattedFilename = formattedFilename.replace("*contentid*", contentid);
        formattedFilename = formattedFilename.replace("*tracknumber*", tracknumber);
        formattedFilename = formattedFilename.replace("*date*", formattedDate);
        formattedFilename = formattedFilename.replace("*ext*", "." + ext);
        if (artist != null) {
            formattedFilename = formattedFilename.replace("*artist*", artist);
        }
        if (album != null) {
            formattedFilename = formattedFilename.replace("*album*", album);
        }
        if (title != null) {
            formattedFilename = formattedFilename.replace("*title*", title);
        }
        return formattedFilename;
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("FAST_LINKCHECK", "Enable fast linkcheck for cover-urls?\r\nNOTE: If enabled, before mentioned linktypes will appear faster but filesize won't be shown before downloadstart.");
                                                      put("GRAB_COVER", "For albums & playlists: Grab cover?");
                                                      put("CUSTOM_DATE", "Enter your custom date:");
                                                      put("SETTING_TAGS", "Explanation of the available tags:\r\n*date* = Release date of the content (appears in the user defined format above)\r\n*artist* = Name of the artist\r\n*album* = Name of the album (not always available)\r\n*title* = Title of the content\r\n*tracknumber* = Position of a track (not always available)\r\n*contentid* = Internal id of the content e.g. '01485'\r\n*ext* = Extension of the file");
                                                      put("LABEL_FILENAME", "Define custom filename:");
                                                  }
                                              };
    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
                                                  {
                                                      put("FAST_LINKCHECK", "Aktiviere schnellen Linkcheck für cover-urls?\r\nWICHTIG: Falls aktiviert werden genannte Linktypen schneller im Linkgrabber erscheinen aber dafür ist deren Dateigröße erst beim Downloadstart sichtbar.");
                                                      put("GRAB_COVER", "Für Alben und Playlists: Cover auch herunterladen?");
                                                      put("CUSTOM_DATE", "Definiere dein gewünschtes Datumsformat:");
                                                      put("SETTING_TAGS", "Erklärung der verfügbaren Tags:\r\n*date* = Erscheinungsdatum (erscheint im oben definierten Format)\r\n*artist* = Name des Authors\r\n*album* = Name des Albums (nicht immer verfügbar)\r\n*title* = Titel des Inhaltes\r\n*tracknumber* = Position eines Songs (nicht immer verfügbar)\r\n*contentid* = Interne id des Inhaltes z.B. '01485'\r\n*ext* = Dateiendung");
                                                      put("LABEL_FILENAME", "Gib das Muster des benutzerdefinierten Dateinamens an:");
                                                  }
                                              };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    private static final String defaultCustomFilename = "*tracknumber*.*date*_*artist* - *album* - *title**ext*";
    private static final String defaultCustomDate     = "yyyy-MM-dd";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, getPhrase("FAST_LINKCHECK")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_COVER, getPhrase("GRAB_COVER")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CUSTOM_DATE, getPhrase("CUSTOM_DATE")).setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_CUSTOM_FILENAME, getPhrase("LABEL_FILENAME")).setDefaultValue(defaultCustomFilename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_TAGS")));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}