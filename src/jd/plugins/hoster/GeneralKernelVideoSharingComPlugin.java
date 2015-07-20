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

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kernel-video-sharing.com", "befuck.com", "gayfall.com", "finevids.xxx", "freepornvs.com", "hclips.com", "mylust.com", "pornfun.com", "pornoid.com", "pornwhite.com", "sheshaft.com", "tryboobs.com", "tubepornclassic.com", "vikiporn.com", "fetishshrine.com", "katestube.com", "sleazyneasy.com", "yeswegays.com", "wetplace.com", "xbabe.com", "xfig.net", "hdzog.com", "sex3.com", "egbo.com", "bravoteens.com", "yoxhub.com", "xxxymovies.com", "bravotube.net", "upornia.com" }, urls = { "http://(?:www\\.)?kvs\\-demo\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?befuck\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?gayfall\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?finevids\\.xxx/videos/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?freepornvs\\.com/videos/\\d+/[a-z0-9\\-]+/",
        "http://(?:www\\.)?hclips\\.com/videos/[a-z0-9\\-]+", "http://(?:www\\.)?mylust\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?pornfun\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?pornoid\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?pornwhite\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?sheshaft\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?tryboobs\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?tubepornclassic\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?vikiporn\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?fetishshrine\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?katestube\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?sleazyneasy\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?yeswegays\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?wetplace\\.com/videos/\\d+/[a-z0-9\\-]+/",
        "http://(www\\.)?xbabe\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?xfig\\.net/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?hdzog\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(www\\.)?sex3\\.com/\\d+/", "http://(?:www\\.)?egbo\\.com/video/\\d+/?", "http://(?:www\\.)?bravoteens\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?yoxhub\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?xxxymovies\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?bravotube\\.net/videos/[a-z0-9\\-]+", "http://(?:www\\.)?upornia\\.com/videos/\\d+/[a-z0-9\\-]+/" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class GeneralKernelVideoSharingComPlugin extends PluginForHost {

    public GeneralKernelVideoSharingComPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Porn_get_file_/videos/_basic Version 0.5
    // Tags:
    // protocol: no https
    // other: URL to a live demo: http://www.kvs-demo.com/
    // other #2: Special websites that have their own plugins: alotporn.com,tubecup.com,voyeurhit.com,wankoz.com,tubewolf.com,alphaporno.com
    // other #3: Plugins with "high security" removed 2015-07-02: BravoTubeNet, BravoTeensCom REMAINING ONES: TubeWolfCom, AlphaPornoCom
    /**
     * specifications that have to be met for hosts to be added here:
     *
     * -404 error response on file not found
     *
     * -Possible filename inside URL
     *
     * -No serverside downloadlimits
     *
     * -No account support
     *
     * -Final downloadlink that fits the RegExes
     *
     * -Website should NOT link to external sources (needs decrypter)
     *
     * */

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    /* E.g. kernel-video-sharing.com e.g. special: alotporn.com */
    private static final String  type_normal       = "^https?://[^/]+/(videos/)?(?:\\d+/)?[a-z0-9\\-]+/?$";
    /* E.g. sex3.com, egbo.com */
    private static final String  type_only_numbers = "^https?://[^/]+/(?:video/)?\\d+/$";
    /* E.g. ??? */
    private static final String  type_embedded     = "NOT_IMPLEMENTED_YET";

    private String               DLLINK            = null;

    @Override
    public String getAGBLink() {
        return "http://www.kvs-demo.com/terms.php";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        DLLINK = null;
        String filename = null;
        this.setBrowserExclusive();
        this.br.setFollowRedirects(true);
        this.br.getPage(downloadLink.getDownloadURL());
        if (downloadLink.getDownloadURL().matches(type_only_numbers)) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        } else {
            filename = this.br.getRegex("class=\"block\\-title\">[\t\n\r ]*?<h\\d+>([^<>]*?)<").getMatch(0);
            if (filename == null) {
                filename = this.br.getRegex("<h\\d+ class=\"block_header\">([^<>]*?)<").getMatch(0);
            }
            if (filename == null) {
                filename = this.br.getRegex("<h\\d+ class=\"album_title\">([^<>]*?)<").getMatch(0);
            }
            if (filename == null) {
                filename = this.br.getRegex("var video_title[\t\n\r ]*?=[\t\n\r ]*?\"([^<>]*?)\";").getMatch(0);
            }
            if (filename == null) {
                filename = this.br.getRegex("itemprop=\"name\">([^<>]*?)<").getMatch(0);
            }
            if (filename == null) {
                filename = this.br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (filename == null) {
                filename = new Regex(downloadLink.getDownloadURL(), "videos/(?:\\d+/)?([a-z0-9\\-]+)/?$").getMatch(0);
                /* Make it look a bit better by using spaces instead of '-' which is always used inside their URLs. */
                filename = filename.replace("-", " ");
            }
        }
        /* Offline links should also have nice filenames */
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        downloadLink.setName(filename);
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().contains("/404.php")) {
            /* Definitly offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Most times this RegEx should do the job */
        DLLINK = this.br.getRegex("video_url[\t\n\r ]*?:[\t\n\r ]*?\\'(http[^<>\"]*?)\\'").getMatch(0);
        if (DLLINK == null && downloadLink.getDownloadURL().contains("xfig.net/")) {
            /* Small workaround */
            DLLINK = this.br.getRegex("var videoFile=\"(http[^<>\"]*?)/\"").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = this.br.getRegex("(http://[A-Za-z0-9\\.\\-]+/get_file/[^<>\"\\&]*?)(?:\\&|\\'|\")").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = this.br.getRegex("\\'(?:file|video)\\'[\t\n\r ]*?:[\t\n\r ]*?\\'(http[^<>\"]*?)\\'").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = this.br.getRegex("(?:file|url):[\t\n\r ]*?(?:\"|\\')(http[^<>\"]*?)(?:\"|\\')").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = this.br.getRegex("<source src=\"(https?://[^<>\"]*?)\" type=(?:\"|\\')video/(?:mp4|flv)(?:\"|\\')").getMatch(0);
        }
        if (DLLINK == null) {
            DLLINK = this.br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (DLLINK == null) {
            if (!this.br.containsHTML("license_code:") && !this.br.containsHTML("kt_player_[0-9\\.]+\\.swfx?")) {
                /* No licence key present in html and/or no player --> No video --> Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        // In case the link redirects to the finallink
        this.br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = this.br.openHeadConnection(DLLINK);
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    /* Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com */
                    final String redirect_url = this.br.getHttpConnection().getRequest().getUrl();
                    con = this.br.openHeadConnection(redirect_url);
                }
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setProperty("directlink", DLLINK);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (this.br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (this.br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, free_resume, free_maxchunks);
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

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "/");
        output = output.replace("\\", "");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
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
