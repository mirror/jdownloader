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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "newgrounds.com" }, urls = { "https?://www\\.newgrounds\\.com/(portal/view/|audio/listen/)\\d+" })
public class NewgroundsCom extends antiDDoSForHost {
    public NewgroundsCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "Filename_by", "Choose file name + by?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "Filename_id", "Add id to file name?").setDefaultValue(true));
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    /* 2017-02-02: Only 1 official (audio) download possible every 60 seconds. Else we will get error 429 */
    private static final int     free_maxdownloads = 1;
    private String               dllink            = null;
    private boolean              server_issues     = false;
    private boolean              accountneeded     = false;

    @Override
    public String getAGBLink() {
        return "http://www.newgrounds.com/wiki/help-information/terms-of-use";
    }

    /* 2020-10-26: Not handled by hostplugin anymore! */
    private static final String ARTLINK = "https?://(?:\\w+\\.)?newgrounds\\.com/art/view/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 429) {
            errorRateLimited();
        }
        final boolean addID2Filename = getPluginConfig().getBooleanProperty("Filename_id", true);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">This entry was")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        // String artist = br.getRegex("<em>(?:Artist|Author|Programming) ?<[^<>]+>([^<>]*?)<").getMatch(0);
        String artist = br.getRegex("<h4.*?>\\s*<[^<>]+>([^<>]*?)</a>[^~]*?<em>(?:Artist|Author|Programming)").getMatch(0);
        // logger.info("artist:" + artist + "|");
        // final String username = br.getRegex("newgrounds\\.com/pm/send/([^<>\"]+)\"").getMatch(0);
        if (artist != null && getPluginConfig().getBooleanProperty("Filename_by", true)) {
            filename = filename + " by " + artist;
        }
        String ext = null;
        final boolean checkForFilesize;
        String url_filename = null;
        if (link.getDownloadURL().matches(ARTLINK)) {
            /* 2020-02-03: Such URLs are handled by the crawler from now on as they can lead to multiple pictures. */
            url_filename = new Regex(link.getDownloadURL(), "/view/(.+)").getMatch(0).replace("/", "_");
            checkForFilesize = true;
            dllink = br.getRegex("id=\"dim_the_lights\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"<img src=\\\\\"(https?:[^<>\"]*?)\\\\\"").getMatch(0);
                if (dllink != null) {
                    dllink = dllink.replace("\\", "");
                    final String id = new Regex(dllink, "images/\\d+/(\\d+)").getMatch(0);
                    if (addID2Filename && filename != null && id != null) {
                        filename = filename + "_" + id;
                    }
                }
            }
        } else {
            /* Audio & Video download */
            /*
             * 2017-02-02: Do not check for filesize as only 1 download per minute is possible --> Accessing directurls makes no sense here.
             */
            checkForFilesize = false;
            final String fid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
            url_filename = fid;
            // filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (link.getDownloadURL().contains("/audio/listen/")) {
                if (filename != null) {
                    filename = Encoding.htmlDecode(filename).trim();
                    if (addID2Filename) {
                        filename = filename + "_" + fid;
                    }
                }
                // var embed_controller = new
                // embedController([{"url":"https:\/\/audio.ngfiles.com\/843000\/843897_Starbarians-3-Suite.mp3?f1548006356"
                if (br.containsHTML("/audio/download/" + fid)) {
                    dllink = "https://www." + this.getHost() + "/audio/download/" + fid;
                } else {
                    final String embedController = br.getRegex("embedController\\s*\\(\\s*\\[\\s*\\{\\s*\"url\"\\s*:\\s*\"(https?:.*?)\"").getMatch(0);
                    if (embedController != null) {
                        dllink = embedController.replaceAll("\\\\", "");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                ext = ".mp3";
            } else {
                if (br.containsHTML("requires a Newgrounds account to play\\.\\s*<")) {
                    accountneeded = true;
                    return AvailableStatus.TRUE;
                }
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.getPage("https://www.newgrounds.com/portal/video/" + fid);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                filename = (String) entries.get("title");
                entries = (Map<String, Object>) entries.get("sources");
                final Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
                int qualityMax = 0;
                while (iterator.hasNext()) {
                    final Entry<String, Object> entry = iterator.next();
                    final String qualityStr = entry.getKey();
                    final ArrayList<Object> qualityInfoArray = (ArrayList<Object>) entry.getValue();
                    final Map<String, Object> qualityInfo = (Map<String, Object>) qualityInfoArray.get(0);
                    final String url = (String) qualityInfo.get("src");
                    if (!qualityStr.matches("\\d+p") || StringUtils.isEmpty(url)) {
                        /* Skip invalid items */
                        continue;
                    }
                    final int quality = Integer.parseInt(qualityStr.replace("p", ""));
                    if (quality > qualityMax) {
                        qualityMax = quality;
                        this.dllink = url;
                    }
                }
                if (addID2Filename && filename != null && fid != null) {
                    filename = filename + "_" + fid;
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
        link.setFinalFileName(filename);
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
                link.setFinalFileName(filename);
                if (this.looksLikeDownloadableContent(con)) {
                    link.setDownloadSize(con.getCompleteContentLength());
                } else {
                    server_issues = true;
                }
                link.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private void errorRateLimited() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 429 - wait before starting new downloads", 120 * 1000l);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (accountneeded) {
            throw new AccountRequiredException();
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        final int chunks;
        if (!link.getPluginPatternMatcher().matches(ARTLINK)) {
            // avoid 429, You're making too many requests. Wait a bit before trying again
            sleep(30 * 1000l, link);
            chunks = 1;
        } else {
            chunks = free_maxchunks;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, chunks);
        if (dl.getConnection().getResponseCode() == 429) {
            try {
                br.followConnection(true);
            } catch (final IOException ignore) {
                logger.log(ignore);
            }
            /* 2017-11-16: E.g. happens for audio files */
            errorRateLimited();
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException ignore) {
                logger.log(ignore);
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
            /* 2020-02-18: https://board.jdownloader.org/showthread.php?t=81805 */
            final boolean art_zip_download_broken_serverside = true;
            if (link.getName().contains(".zip") && art_zip_download_broken_serverside) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download broken serverside please wait for a fix, then contact us to fix our plugin");
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
