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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "music.163.com" }, urls = { "http://(www\\.)?music\\.163\\.com/(?:#/)?(?:song|mv)\\?id=\\d+|decrypted://music\\.163\\.comcover\\d+" }, flags = { 2 })
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
    private static final String           FAST_LINKCHECK    = "FAST_LINKCHECK";
    private static final String           GRAB_COVER        = "GRAB_COVER";

    private static final String           TYPE_MUSIC        = "http://(www\\.)?music\\.163\\.com/(?:#/)?song\\?id=\\d+";
    private static final String           TYPE_VIDEO        = "http://(www\\.)?music\\.163\\.com/(?:#/)?mv\\?id=\\d+";
    private static final String           TYPE_COVER        = "decrypted://music\\.163\\.comcover\\d+";

    private static final String           dlurl_format      = "http://m1.music.126.net/%s/%s.mp3";

    /* Qualities from highest to lowest in KB/s: 320, 160, 96 hMusic is officially only available for logged-in users! */
    public static final String[]          audio_qualities   = { "hMusic", "mMusic", "lMusic", "bMusic" };
    public static final String[]          video_qualities   = { "1080", "720", "360", "240" };
    public static final String            dateformat_en     = "yyyy-MM-dd";

    /* Connection stuff */
    private static final boolean          FREE_RESUME       = true;
    private static final int              FREE_MAXCHUNKS    = 0;
    private static final int              FREE_MAXDOWNLOADS = 20;

    private String                        DLLINK            = null;
    private LinkedHashMap<String, Object> entries           = null;

    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    /*
     * API documentation:
     * https://github.com/yanunon/NeteaseCloudMusic/wiki/%E7%BD%91%E6%98%93%E4%BA%91%E9%9F%B3%E4%B9%90API%E5%88%86%E6%9E%90
     */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* Tracknumber can only be given by decrypter */
        final String tracknumber = link.getStringProperty("trachnumber", null);
        /* Decrypters can also set final filenames */
        String filename = link.getStringProperty("directfilename", null);
        /*
         * First try to get publishedTimestamp from decrypter as an artist can have multiple albums with different timestamps but if the
         * user added an album we know which timestamp is definitly the correct one!
         */
        long publishedTimestamp = link.getLongProperty("publishedTimestamp", 0);
        String formattedDate = null;
        String artist = null;
        long filesize = 0;
        String ext = null;
        DLLINK = null;
        this.setBrowserExclusive();
        entries = null;
        prepareAPI(this.br);

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
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            entries = (LinkedHashMap<String, Object>) entries.get("data");
            filename = (String) entries.get("artistName");

            final String publishDate = (String) entries.get("publishTime");
            if (publishDate != null) {
                publishedTimestamp = TimeFormatter.getMilliSeconds(publishDate, "yyyy-MM-dd", Locale.ENGLISH);
                if (publishedTimestamp > 0) {
                    final SimpleDateFormat formatter = new SimpleDateFormat(jd.plugins.hoster.Music163Com.dateformat_en);
                    formattedDate = formatter.format(publishedTimestamp);
                }
            }
            entries = (LinkedHashMap<String, Object>) entries.get("brs");
            for (final String quality : video_qualities) {
                DLLINK = (String) entries.get(quality);
                if (DLLINK != null) {
                    break;
                }
            }
            if (filename == null || DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filesize = getFilesizeFromHeader(DLLINK);

            ext = "mp4";
            filename += "." + ext;
            if (formattedDate != null) {
                filename = formattedDate + "_" + filename;
            }
            if (tracknumber != null) {
                filename = tracknumber + "." + filename;
            }
        } else {
            link.setLinkID(new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
            /* Make sure that offline links also have nice filenames. */
            link.setName(link.getLinkID() + ".mp3");
            br.getPage("http://music.163.com/api/song/detail/?id=" + link.getLinkID() + "&ids=%5B" + link.getLinkID() + "%5D");
            if (br.getHttpConnection().getResponseCode() != 200) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final ArrayList<Object> songs = (ArrayList) entries.get("songs");
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
                    filesize = jd.plugins.hoster.DummyScriptEnginePlugin.toLong(musicmap.get("size"), -1);
                    break;
                }
            }
            /* Only set filename if user added links without decrypter. */
            if (filename == null) {
                artist = (String) artist_info.get("name");
                final String name_album = (String) album_info.get("name");
                final String songname = (String) entries.get("name");
                if (publishedTimestamp < 1) {
                    publishedTimestamp = jd.plugins.hoster.DummyScriptEnginePlugin.toLong(album_info.get("publishTime"), 0);
                }
                if (publishedTimestamp > 0) {
                    final SimpleDateFormat formatter = new SimpleDateFormat(jd.plugins.hoster.Music163Com.dateformat_en);
                    formattedDate = formatter.format(publishedTimestamp);
                }
                if (artist == null || name_album == null || songname == null || ext == null || filesize == -1) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename = artist + " - " + name_album + " - " + songname;
                filename += "." + ext;
                if (formattedDate != null) {
                    filename = formattedDate + "_" + filename;
                }
                if (tracknumber != null) {
                    filename = tracknumber + "." + filename;
                }
            }
        }
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
                LinkedHashMap<String, Object> entries_lyrics = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br2.toString());
                entries_lyrics = (LinkedHashMap<String, Object>) entries.get("lrc");
                final String lyrics = (String) entries_lyrics.get("lyric");
                downloadLink.setComment(lyrics);
                logger.info("Successfully set lyrics");
            } catch (final Throwable e) {
                logger.warning("Failed to get/set lyrics");
            }
            /* Now find the highest DOWNLOADABLE quality available */
            boolean trackDownloadable = false;
            for (final String quality : audio_qualities) {
                if (this.isAbort()) {
                    logger.info("User stopped downloads --> Stepping out of 'quality loop'");
                    /* Avoid 'freezing' when user stops downloads */
                    throw new PluginException(LinkStatus.ERROR_RETRY, "User aborted download");
                }
                final Object musicO = entries.get(quality);
                if (musicO != null) {
                    final LinkedHashMap<String, Object> musicmap = (LinkedHashMap<String, Object>) musicO;
                    final String dfsid = Long.toString(jd.plugins.hoster.DummyScriptEnginePlugin.toLong(musicmap.get("dfsId"), -1));
                    final String encrypted_dfsid = encrypt_dfsId(dfsid);
                    /*
                     * bMusic (and often/alyways) lMusic == mp3Url - in theory we don't have to generate the final downloadlink for these
                     * cases as it is already given.
                     */
                    // DLLINK = (String) entries.get("mp3Url");
                    DLLINK = String.format(dlurl_format, encrypted_dfsid, dfsid);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
                    /* Sometimes HQ versions of songs are officially available but directlinks return 404 on download attempt. */
                    if (dl.getConnection().getResponseCode() != 200 || dl.getConnection().getContentType().contains("html")) {
                        continue;
                    }
                    trackDownloadable = true;
                    break;
                }
            }
            if (!trackDownloadable) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - track is not downloadable at the moment", 30 * 60 * 1000l);
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
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
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
        final String b64 = new sun.misc.BASE64Encoder().encode(md5bytes);
        /*
         * In the above linked pyton examples it seems like they remove the last character of the base64 String but it seems like this is
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
        br.setCookie("http://music.163.com/", "appver", "1.7.3");
        br.setAllowedResponseCodes(400);
    }

    @Override
    public String getDescription() {
        return "JDownloader's music.163.com plugin helps downloading audio files from music.163.com.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, JDL.L("plugins.hoster.Music163Com.FastLinkcheck", "Enable fast linkcheck for cover-urls?\r\nNOTE: If enabled, before mentioned linktypes will appear faster but filesize won't be shown before downloadstart.")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_COVER, JDL.L("plugins.hoster.Music163Com.AlbumsGrabCover", "For albums & playlists: Grab cover?")).setDefaultValue(false));
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