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
        final LinkedHashMap<String, Object> bMusic = (LinkedHashMap<String, Object>) entries.get("bMusic");

        final String name_artist = (String) artist_info.get("name");
        final String name_album = (String) album_info.get("name");
        final String songname = (String) entries.get("name");
        final String ext = (String) bMusic.get("extension");
        final String filename = name_artist + " - " + name_album + " - " + songname + "." + ext;
        final long filesize = jd.plugins.hoster.DummyScriptEnginePlugin.toLong(bMusic.get("size"), -1);
        DLLINK = (String) entries.get("mp3Url");
        if (name_artist == null || name_album == null || songname == null || ext == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
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

    // private String checkDirectLink(final DownloadLink downloadLink, final String property) {
    // String dllink = downloadLink.getStringProperty(property);
    // if (dllink != null) {
    // URLConnectionAdapter con = null;
    // try {
    // final Browser br2 = br.cloneBrowser();
    // if (isJDStable()) {
    // con = br2.openGetConnection(dllink);
    // } else {
    // con = br2.openHeadConnection(dllink);
    // }
    // if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // }
    // } catch (final Exception e) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // } finally {
    // try {
    // con.disconnect();
    // } catch (final Throwable e) {
    // }
    // }
    // }
    // return dllink;
    // }

    // private boolean isJDStable() {
    // return System.getProperty("jd.revision.jdownloaderrevision") == null;
    // }

    public static void prepareAPI(final Browser br) {
        br.getHeaders().put("Referer", "http://music.163.com/");
        /* Last updated: 2014-07-13 */
        br.setCookie("http://music.163.com/", "appver", "1.5.0.75771");
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