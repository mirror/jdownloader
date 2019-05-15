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
import java.util.LinkedHashMap;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vice.com" }, urls = { "https?://([A-Za-z0-9]+\\.)?vicedecrypted\\.com/.+" })
public class ViceCom extends PluginForHost {
    public ViceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("vicedecrypted.com/", "vice.com/"));
    }
    /* DEV NOTES */
    /* Tags: ooyala.com, ooyala Player, ooyala player API */
    // protocol: no https
    /*
     * other: 2nd way to get video urls: Access http://player.ooyala.com/player.js?embedCode=<video_id> --> Get the mobile js URL and access
     * it: http://player.ooyala.com/mobile_player.js?embedCodes=BmZ21sZTpx1K8pFKz_hat0Dq5zap77Xs&expires=1431993600&height=360&locale=de&
     * playerId
     * =ooyalaPlayer243936600_us5e93k235&rootItemEmbedCode=BmZ21sZTpx1K8pFKz_hat0Dq5zap77Xs&signature=VFus8riyCPBCsXeAuiXwFZElPFWpw0d%2
     * BSFIFMQtxD8s&video_pcode=JqcWY6ikg5nwtXilzVurvI-vU6Ik&width=480&device="+device+"&domain="
     *
     *
     * Additional thanks: https://github.com/rg3/youtube-dl/blob/e3216b82bf6ef54db63984f7fece4e95fbc3b981/youtube_dl/extractor/vice.py
     *
     * And: https://github.com/rg3/youtube-dl/blob/e3216b82bf6ef54db63984f7fece4e95fbc3b981/youtube_dl/extractor/ooyala.py
     */
    /*
     * Small documentation of the vice.com (NOT ooyala) API which we cannot really use at this point as the articleIDs seem not to be
     * available via desktop website.
     *
     * User-Agent: okhttp/2.2.0
     *
     * Get array of current articles: vice.com/de/api/getvicetoday/0
     *
     * Get information about an article: vice.com/de/api/article/<article_ID>
     */

    /*
     * Available HLS Qualities: 300KBs_480x270_64 KBs audio AAC LC, 600KBs_640x360_128 KBs audio AAC LC, 64KBs audio AAC LC audio ONLY
     */
    /* Available HTTP Qualities: 600KBs_640x360_128 KBs audio AAC LC */
    /**
     * TODO: If in the future, other plugins also need the ooyala player handling, make generic handling for it!
     */
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    public static final String   HTML_VIDEO_EXISTS = "class=\"video\\-wrapper\"";
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://vice.com/";
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        downloadLink.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        String ext = null;
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML(ViceCom.HTML_VIDEO_EXISTS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String playerid = br.getRegex("data-player-id=\"([^<>\"]*?)\"").getMatch(0);
        if (playerid == null) {
            playerid = br.getRegex("playerId=([^<>\"\\&]+)").getMatch(0);
        }
        String videoid = br.getRegex("data-video-id=\"([^<>\"]*?)\"").getMatch(0);
        if (videoid == null) {
            videoid = br.getRegex("vid=([^<>\"\\&]+)").getMatch(0);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        }
        if (filename == null || playerid == null || videoid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Possible for device: unknown, iphone, ipad, android_html, WIN%2017%2C0%2C0%2C169 */
        /* Possible for domain: motherboard.vice.com, www.ooyala.com */
        /* Possible and working for supportedFormats: mp4, m3u8 */
        /* Possible and NOT working for supportedFormats: wv_hls and wv_wvm (returns empty stream array if used) */
        /* Default for supportedFormats: mp4%2Cm3u8%2Cwv_hls%2Cwv_wvm%2Cwv_mp4 */
        /* Remove cookies/headers */
        this.br = new Browser();
        /* This UA is not necessarily needed. */
        this.br.getHeaders().put("User-Agent", "Dalvik/1.6.0 (Linux; U; Android 4.4.4; A0001 Build/KTU84Q)");
        br.getPage("http://player.ooyala.com/sas/player_api/v1/authorization/embed_code/" + playerid + "/" + videoid + "?device=android_html&domain=www.ooyala.com&supportedFormats=mp4");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final String walk_string = "authorization_data/" + videoid + "/streams/{0}/url/data";
        dllink = (String) JavaScriptEngineFactory.walkJson(entries, walk_string);
        if (dllink == null) {
            /* No HTTP url available --> must be HLS.only */
            br.getPage("http://player.ooyala.com/sas/player_api/v1/authorization/embed_code/" + playerid + "/" + videoid + "?device=android_html&domain=www.ooyala.com&supportedFormats=m3u8");
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            dllink = (String) JavaScriptEngineFactory.walkJson(entries, walk_string);
            if (dllink != null) {
                if (!dllink.startsWith("http")) {
                    dllink = Encoding.Base64Decode(dllink);
                }
                if (!dllink.startsWith("http")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage(dllink);
                dllink = br.getRegex("(https?://player\\.ooyala\\.com/player/iphone/[^<>\"]*?\\.m3u8)").getMatch(0);
            }
            ext = ".mp4";
        } else {
            /* HTTP url available --> Decrypt it */
            if (!dllink.startsWith("http")) {
                dllink = Encoding.Base64Decode(dllink);
            }
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        }
        if (dllink == null || !dllink.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        filename += "_AVC_640x360_600_AAC LC_128";
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        /* We cannot check the filesize for HLS urls */
        if (!dllink.endsWith(".m3u8")) {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = openConnection(br2, dllink);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        if (dllink.endsWith(".m3u8")) {
            /* HLS handling */
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            /* HTTP handling */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
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
