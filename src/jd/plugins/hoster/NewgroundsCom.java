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

import java.util.List;
import java.util.Map;

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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "newgrounds.com" }, urls = { "https?://www\\.newgrounds\\.com/((portal/view/|audio/listen/)\\d+|art/view/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+)" })
public class NewgroundsCom extends antiDDoSForHost {
    public NewgroundsCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "Filename_by", "Choose file name + by?").setDefaultValue(true));
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    /* 2017-02-02: Only 1 official (audio) download possible every 60 seconds. */
    private static final int     free_maxdownloads = 1;
    private String               dllink            = null;
    private boolean              server_issues     = false;
    private boolean              accountneeded     = false;

    @Override
    public String getAGBLink() {
        return "http://www.newgrounds.com/wiki/help-information/terms-of-use";
    }

    private static final String ARTLINK = "https?://(?:www\\.)?newgrounds\\.com/art/view/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">This entry was")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        String artist = br.getRegex("<em>(?:Artist|Author|Programming) ?<[^<>]+>([^<>]*?)<").getMatch(0);
        // final String username = br.getRegex("newgrounds\\.com/pm/send/([^<>\"]+)\"").getMatch(0);
        final String username_inside_artist = new Regex(artist, "\\((.+)\\)").getMatch(0);
        if (username_inside_artist != null) {
            /* User request: Use the username as artist name if available */
            artist = username_inside_artist;
        }
        if (artist != null && getPluginConfig().getBooleanProperty("Filename_by", true)) {
            filename = filename + " by " + artist;
        }
        String ext = null;
        final boolean checkForFilesize;
        String url_filename = null;
        if (downloadLink.getDownloadURL().matches(ARTLINK)) {
            url_filename = new Regex(downloadLink.getDownloadURL(), "/view/(.+)").getMatch(0).replace("/", "_");
            checkForFilesize = true;
            dllink = br.getRegex("id=\"dim_the_lights\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"<img src=\\\\\"(https?:[^<>\"]*?)\\\\\"").getMatch(0);
                if (dllink != null) {
                    dllink = dllink.replace("\\", "");
                }
            }
        } else {
            /* Audio & Video download */
            /*
             * 2017-02-02: Do not check for filesize as only 1 download per minute is possible --> Accessing directurls makes no sense here.
             */
            checkForFilesize = false;
            final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
            url_filename = fid;
            // filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (downloadLink.getDownloadURL().contains("/audio/listen/")) {
                if (filename != null) {
                    filename = Encoding.htmlDecode(filename).trim();// + "_" + fid;
                }
                dllink = "http://www." + this.getHost() + "/audio/download/" + fid;
                ext = ".mp3";
            } else {
                if (br.containsHTML("requires a Newgrounds account to play\\.<")) {
                    accountneeded = true;
                    return AvailableStatus.TRUE;
                }
                final String videoPlayer = br.getRegex("iframe\\s*src\\s*=\\s*\\\\\"([^\"]*/videoplayer[^\"]*)\\\\\"").getMatch(0);
                if (videoPlayer != null) {
                    final Browser brc = br.cloneBrowser();
                    brc.getPage(videoPlayer.replace("\\", ""));
                    final String playerSrc = brc.getRegex("player\\.updateSrc\\((.*?)\\)").getMatch(0);
                    final List<Object> items = JSonStorage.restoreFromString(playerSrc, TypeRef.LIST);
                    Map<String, Object> best = null;
                    for (final Object item : items) {
                        if (item instanceof Map) {
                            final Map<String, Object> map = (Map<String, Object>) item;
                            if (best == null || ((Number) map.get("res")).longValue() > ((Number) best.get("res")).longValue()) {
                                best = map;
                            }
                        }
                    }
                    if (best != null) {
                        dllink = (String) best.get("src");
                        if (filename != null) {
                            filename += "_" + best.get("res");
                        }
                    }
                }
                if (dllink == null) {
                    dllink = br.getRegex("\"src\":[\t\n\r ]+\"(https?:[^<>\"]*?)\"").getMatch(0);
                    // Maybe video or .swf
                    if (dllink == null) {
                        dllink = br.getRegex("\"url\":\"(https?:[^<>\"]*?)\"").getMatch(0);
                    }
                    if (dllink != null) {
                        dllink = dllink.replace("\\", "");
                        dllink = Encoding.htmlDecode(dllink);
                    }
                }
            }
        }
        if (filename == null) {
            /* Fallback */
            filename = url_filename;
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (ext == null) {
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        if (dllink != null && checkForFilesize) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (filename == null) {
                    filename = getFileNameFromHeader(con);
                }
                filename = Encoding.htmlDecode(filename);
                filename = filename.trim();
                filename = encodeUnicode(filename);
                if (!filename.endsWith(ext)) {
                    filename += ext;
                }
                downloadLink.setFinalFileName(filename);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
                downloadLink.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (accountneeded) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getResponseCode() == 429) {
            /* 2017-11-16: E.g. happens for audio files */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 429 - wait before starting new downloads", 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            try {
                br.followConnection();
            } catch (final Throwable ignore) {
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503 - wait before starting new downloads", 3 * 60 * 1000l);
            }
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
