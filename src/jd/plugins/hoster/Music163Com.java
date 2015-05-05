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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "music.163.com" }, urls = { "http://(www\\.)?music\\.163\\.com/(#/)?song\\?id=\\d+" }, flags = { 2 })
public class Music163Com extends PluginForHost {

    public Music163Com(PluginWrapper wrapper) {
        super(wrapper);
        // setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://music.163.com/html/web2/service.html";
    }

    /** Settings stuff */
    private static final String  FAST_LINKCHECK    = "FAST_LINKCHECK";

    private static final String  dlurl_format      = "http://m1.music.126.net/%s/%s.mp3";

    /* Qualities from highest to lowest in KB/s: 320, 160, 96 hMusic is officially only available for logged-in users! */
    public static final String[] qualities         = { "hMusic", "mMusic", "lMusic", "bMusic" };
    public static final String   dateformat_en     = "yyyy-MM-dd";

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    private String               DLLINK            = null;

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
        long publishedTimestamp = link.getLongProperty("publishedTimestamp", 0);
        String formattedDate = null;
        long filesize = 0;
        String ext = null;
        DLLINK = null;
        this.setBrowserExclusive();
        final String linkid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        link.setLinkID(linkid);
        prepareAPI(this.br);
        br.getPage("http://music.163.com/api/song/detail/?id=" + linkid + "&ids=%5B" + linkid + "%5D");
        /* Example for music videos: */
        // br.getPage("http://music.163.com/api/mv/detail?id=319104&type=mp4");
        if (br.getHttpConnection().getResponseCode() != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final ArrayList<Object> songs = (ArrayList) entries.get("songs");
        entries = (LinkedHashMap<String, Object>) songs.get(0);
        final ArrayList<Object> artists = (ArrayList) entries.get("artists");
        final LinkedHashMap<String, Object> album_info = (LinkedHashMap<String, Object>) entries.get("album");
        final LinkedHashMap<String, Object> artist_info = (LinkedHashMap<String, Object>) artists.get(0);
        /* Now find the highest quality available */
        for (final String quality : qualities) {
            final Object musicO = entries.get(quality);
            if (musicO != null) {
                final LinkedHashMap<String, Object> musicmap = (LinkedHashMap<String, Object>) musicO;
                ext = (String) musicmap.get("extension");
                final String dfsid = Long.toString(jd.plugins.hoster.DummyScriptEnginePlugin.toLong(musicmap.get("dfsId"), -1));
                final String encrypted_dfsid = encrypt_dfsId(dfsid);
                filesize = jd.plugins.hoster.DummyScriptEnginePlugin.toLong(musicmap.get("size"), -1);
                /*
                 * bMusic (and often/alyways) lMusic == mp3Url - in theory we don't have to generate the final downloadlink for these cases
                 * as it is already given.
                 */
                // DLLINK = (String) entries.get("mp3Url");
                DLLINK = String.format(dlurl_format, encrypted_dfsid, dfsid);
                break;
            }
        }

        final String name_artist = (String) artist_info.get("name");
        final String name_album = (String) album_info.get("name");
        final String songname = (String) entries.get("name");
        if (publishedTimestamp == 0) {
            publishedTimestamp = jd.plugins.hoster.DummyScriptEnginePlugin.toLong(album_info.get("publishTime"), 0);
        }
        if (publishedTimestamp > 0) {
            final SimpleDateFormat formatter = new SimpleDateFormat(jd.plugins.hoster.Music163Com.dateformat_en);
            formattedDate = formatter.format(publishedTimestamp);
        }
        if (name_artist == null || name_album == null || songname == null || ext == null || filesize == -1 || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = name_artist + " - " + name_album + " - " + songname + "." + ext;
        if (tracknumber != null) {
            filename = tracknumber + "." + filename;
        }
        if (formattedDate != null) {
            filename = formattedDate + "_" + filename;
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("unchecked")
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (downloadLink.getComment() == null) {
            /* Get- and set lyrics if possible */
            try {
                br.getPage("http://music.163.com/api/song/lyric?id=" + downloadLink.getLinkID() + "&lv=-1&tv=-1&csrf_token=");
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("lrc");
                final String lyrics = (String) entries.get("lyric");
                downloadLink.setComment(lyrics);
                logger.info("Successfully set lyrics");
            } catch (final Throwable e) {
                logger.warning("Failed to get/set lyrics");
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
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

    /*
     * Thx https://github.com/sk1418/zhuaxia/blob/master/zhuaxia/netease.py and
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, JDL.L("plugins.hoster.Music163Com.FastLinkcheck", "Enable fast linkcheck?\r\nNOTE: If enabled, links will appear faster but filesize won't be shown before downloadstart.")).setDefaultValue(false));
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