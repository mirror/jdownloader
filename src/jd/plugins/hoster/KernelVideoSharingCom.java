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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kernel-video-sharing.com", "hotmovs.com", "porndreamer.com", "cartoontube.xxx", "hotamateurs.xxx", "theclassicporn.com", "faplust.com", "alotporn.com", "alphaporno.com", "updatetube.com", "thenewporn.com", "pinkrod.com", "hotshame.com", "tubewolf.com", "voyeurhit.com", "yourlust.com", "pornicom.com", "pervclips.com", "wankoz.com", "tubecup.com", "myxvids.com", "hellporno.com", "h2porn.com", "gayfall.com", "finevids.xxx", "freepornvs.com", "mylust.com", "pornfun.com", "pornoid.com", "pornwhite.com", "sheshaft.com", "tryboobs.com", "tubepornclassic.com", "vikiporn.com", "fetishshrine.com", "katestube.com", "sleazyneasy.com", "yeswegays.com", "wetplace.com", "xbabe.com", "xfig.net", "hdzog.com", "sex3.com", "egbo.com", "bravoteens.com", "yoxhub.com", "xxxymovies.com", "bravotube.net", "upornia.com", "xcafe.com",
        "txxx.com", "pornpillow.com", "anon-v.com", "hclips.com", "camvideos.org" }, urls = { "http://(?:www\\.)?kvs\\-demo\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?hotmovs\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?porndreamer\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?cartoontube\\.xxx/video\\d+/[a-z0-9\\-]+/?", "http://(?:www\\.)?hotamateurs\\.xxx/pornvideos/\\d+\\-[a-z0-9\\-]+/", "http://(?:www\\.)?theclassicporn\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?faplust\\.com/watch/\\d+/", "http://(?:www\\.)?alotporn\\.com/(?:\\d+/[A-Za-z0-9\\-_]+/|(?:embed\\.php\\?id=|embed/)\\d+)|https?://m\\.alotporn\\.com/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?alphaporno\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?updatetube\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?thenewporn\\.com/videos/\\d+/[a-z0-9\\-]+/",
        "http://(?:www\\.)?pinkrod\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?hotshame\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?tubewolf\\.com/movies/[a-z0-9\\-]+", "http://(?:www\\.)?voyeurhit\\.com/videos/[a-z0-9\\-]+", "http://(?:www\\.)?yourlust\\.com/videos/[a-z0-9\\-]+\\.html", "https?://(?:www\\.)?pornicom\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?pervclips\\.com/tube/videos/[^<>\"/]+/", "https?://(?:www\\.|m\\.)?wankoz\\.com/videos/\\d+/[a-z0-9\\-_]+/", "http://(?:www\\.)?tubecup\\.com/(?:videos/\\d+/[a-z0-9\\-_]+/|embed/\\d+)", "http://(?:www\\.)?myxvids\\.com/(videos/\\d+/[a-z0-9\\-_]+/|embed/\\d+)", "http://(?:www\\.)?hellporno\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?h2porn\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?gayfall\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?finevids\\.xxx/videos/\\d+/[a-z0-9\\-]+",
        "http://(?:www\\.)?freepornvs\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?mylust\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?pornfun\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?pornoid\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?pornwhite\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?sheshaft\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?tryboobs\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?tubepornclassic\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?vikiporn\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?fetishshrine\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?katestube\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?sleazyneasy\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?yeswegays\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?wetplace\\.com/videos/\\d+/[a-z0-9\\-]+/",
        "http://(www\\.)?xbabe\\.com/videos/[a-z0-9\\-]+/", "http://(?:www\\.)?xfig\\.net/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?hdzog\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(www\\.)?sex3\\.com/\\d+/", "https?://(?:www\\.)?egbo\\.com/video/\\d+/?", "http://(?:www\\.)?bravoteens\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?yoxhub\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?xxxymovies\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://(?:www\\.)?bravotube\\.net/videos/[a-z0-9\\-]+", "http://(?:www\\.)?upornia\\.com/videos/\\d+/[a-z0-9\\-]+/", "http://xcafe\\.com/\\d+/", "http://(?:www\\.)?txxx\\.com/videos/\\d+/[a-z0-9\\-]+/|(https?://(?:www\\.)?txxx\\.com/embed/\\d+)", "https?://(?:www\\.)?pornpillow\\.com/\\d+/[^/]+\\.html", "https?://(?:www\\.)?anon\\-v\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:www\\.)?hclips\\.com/videos/[a-z0-9\\-]+/",
        "https?://(?:www\\.)?camvideos\\.org/embed/\\d+" })
public class KernelVideoSharingCom extends antiDDoSForHost {
    public KernelVideoSharingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    // Version 1.0
    // Tags:
    // protocol: no https
    // other: URL to a live demo: http://www.kvs-demo.com/
    // other #2: Special websites that have their own plugins: pornktube.com
    // other #3: Plugins with "high security" removed 2015-07-02: BravoTubeNet, BravoTeensCom
    // other #3: h2porn.com: Added without security stuff 2015-11-03 REV 29387
    // TODO: Check if it is possible to get nice filenames for embed-urls as well
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
     */
    /* Connection stuff */
    private static final boolean free_resume               = true;
    private static final int     free_maxchunks            = 0;
    private static final int     free_maxdownloads         = -1;
    /* E.g. normal kernel-video-sharing.com video urls */
    private static final String  type_normal               = "^https?://.+/(videos/)?(?:\\d+/)?[a-z0-9\\-]+(/?|\\.html)$";
    private static final String  type_mobile               = "^https?://m\\.[^/]+/\\d+/[a-z0-9\\-]+/$";
    /* E.g. sex3.com, egbo.com */
    private static final String  type_only_numbers         = "^https?://[^/]+/(?:video/)?\\d+/$";
    /* E.g. myxvids.com */
    private static final String  type_embedded             = "^https?://(?:www\\.)?[^/]+/embed/\\d+/?$";
    /* Special types */
    private static final String  type_special_alotporn_com = "^http://(?:www\\.)?alotporn\\.com/(?:\\d+/[A-Za-z0-9\\-_]+/|(?:embed\\.php\\?id=|embed/)\\d+)|https?://m\\.alotporn\\.com/\\d+/[a-z0-9\\-]+/";
    private String               dllink                    = null;
    private boolean              isDownload                = false;
    private boolean              server_issues             = false;                                                                                                                                          ;

    @Override
    public String getAGBLink() {
        return "http://www.kvs-demo.com/terms.php";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(type_mobile)) {
            /* Correct mobile urls --> Normal URLs */
            final Regex info = new Regex(link.getDownloadURL(), "^https?://m\\.([^/]+/\\d+/[a-z0-9\\-]+/$)");
            final String linkpart = info.getMatch(0);
            link.setUrlDownload("http://www." + linkpart);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        server_issues = false;
        String filename = null;
        final String host = downloadLink.getHost();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* Place for workarounds / special handling #1 */
        if (host.equals("camvideos.org")) {
            /* Without this Referer we cannot access their content plus they only have embed URLs! */
            br.getHeaders().put("Referer", "http://www.camwhores.tv/");
        }
        getPage(downloadLink.getDownloadURL());
        String filename_url = null;
        if (br.containsHTML("KernelTeamVideoSharingSystem\\.js|KernelTeamImageRotator_")) {
            /* <script src="/js/KernelTeamImageRotator_3.8.1.jsx?v=3"></script> */
            /* <script type="text/javascript" src="http://www.hclips.com/js/KernelTeamVideoSharingSystem.js?v=3.8.1"></script> */
        }
        /* Place for workarounds / special handling #2 */
        if (downloadLink.getDownloadURL().matches(type_special_alotporn_com)) {
            if (downloadLink.getDownloadURL().matches(type_embedded)) {
                /* Convert embed --> Normal */
                final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)/?$").getMatch(0);
                getPage("http://www.alotporn.com/" + fid + "/" + System.currentTimeMillis() + "/");
            }
            filename_url = new Regex(br.getURL(), "([a-z0-9\\-]+)/?$").getMatch(0);
            filename = br.getRegex("<div class=\"headline\">[\t\n\r ]*?<h1>([^<>\"]*?)</h1>").getMatch(0);
            if (inValidate(filename)) {
                filename = regexStandardTitleWithHost(host);
            }
        } else if (downloadLink.getDownloadURL().matches(type_only_numbers)) {
            filename_url = new Regex(downloadLink.getDownloadURL(), "(\\d+)/?").getMatch(0);
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        } else if (downloadLink.getDownloadURL().matches(type_embedded)) {
            filename_url = new Regex(downloadLink.getDownloadURL(), "(\\d+)/?").getMatch(0);
            filename = br.getRegex("<title>([^<>\"]*?) / Embed Player</title>").getMatch(0);
            if (inValidate(filename)) {
                /* Filename from decrypter */
                filename = downloadLink.getProperty("filename").toString();
            }
            if (inValidate(filename)) {
                filename = new Regex(downloadLink.getDownloadURL(), "(\\d+)/?$").getMatch(0);
            }
        } else {
            filename_url = new Regex(downloadLink.getDownloadURL(), "(?:videos|movies)/(?:\\d+/)?([a-z0-9\\-]+)(?:/?|\\.html)$").getMatch(0);
            /* Works e.g. for hdzog.com */
            filename = br.getRegex("var video_title[\t\n\r ]*?=[\t\n\r ]*?\"([^<>]*?)\";").getMatch(0);
            if (downloadLink.getDownloadURL().contains("yourlust.com")) {
                /* 2016-12-21 */
                filename = br.getRegex("<h\\d+ class=\"[^<>]+>([^<>]*?)<").getMatch(0);
            }
            if (downloadLink.getDownloadURL().contains("faplust.com")) {
                /* 2017-03-09 */
                filename = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
            }
            if (inValidate(filename)) {
                /* Newer KVS e.g. tubecup.com */
                filename = br.getRegex("title[\t\n\r ]*?:[\t\n\r ]*?\"([^<>\"]*?)\"").getMatch(0);
            }
            if (inValidate(filename)) {
                filename = br.getRegex("<h\\d+ class=\"album_title\">([^<>]*?)<").getMatch(0);
            }
            if (inValidate(filename)) {
                filename = br.getRegex("itemprop=\"name\">([^<>]*?)<").getMatch(0);
            }
            if (inValidate(filename)) {
                filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (inValidate(filename)) {
                /* 2016-12-18: theclassicporn.com */
                filename = br.getRegex("class=\"link-blue link-no-border\">([^<>\"]*?)<").getMatch(0); // theclassicporn.com
            }
            if (inValidate(filename)) {
                /* Fails e.g. for alphaporno.com */
                filename = br.getRegex("<h\\d+ class=\"title\">([^<>\"]*?)<").getMatch(0);
            }
            if (inValidate(filename)) {
                /* Working e.g. for wankoz.com */
                filename = br.getRegex("<h\\d+ class=\"block_header\" id=\"desc_button\">([^<>\"]*?)</h\\d+>").getMatch(0);
            }
            if (inValidate(filename)) {
                /* Working e.g. for pervclips.com, pornicom.com */
                filename = br.getRegex("class=\"heading video-heading\">[\t\n\r ]+<(h\\d+)>([^<>\"]*?)</h\\1>").getMatch(1);
            }
            if (inValidate(filename)) {
                /* Working e.g. for voyeurhit.com */
                filename = br.getRegex("<div class=\"info\\-player\">[\t\n\r ]+<h\\d+>([^<>\"]*?)</h\\d+>").getMatch(0);
            }
            // if (inValidate(filename)) {
            // /* This will e.g. fail for wankoz.com */
            // filename = br.getRegex("<h\\d+ class=\"block_header\">([^<>]*?)<").getMatch(0);
            // }
            // if (inValidate(filename)) {
            // /* This will e.g. fail for hdzog.com */
            // filename = br.getRegex("class=\"block\\-title\">[\t\n\r ]*?<h\\d+>([^<>]*?)<").getMatch(0);
            // }
            if (inValidate(filename)) {
                /* Many websites in general use this format - title plus their own hostname as ending. */
                filename = regexStandardTitleWithHost(host);
            }
            if (filename_url == null) {
                /* Last chance fallback: auto-url-filename */
                String url_part = new Regex(downloadLink.getDownloadURL(), "https?://[^/]+/(.+)").getMatch(0);
                url_part = url_part.replace(".html", "");
                url_part = url_part.replace(".htm", "");
                url_part = url_part.replace("/", "_");
                filename_url = url_part;
            }
        }
        /* Make the url-filenames look a bit better by using spaces instead of '-'. */
        filename_url = filename_url.replace("-", " ");
        if (inValidate(filename)) {
            filename = filename_url;
        }
        if (inValidate(filename_url)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Offline links should also have nice filenames */
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("/404.php")) {
            /* Definitly offline - set url filename to avoid bad names! */
            downloadLink.setName(filename_url);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(filename);
        dllink = getDllink(downloadLink, this.br);
        final String ext;
        if (dllink != null && !dllink.contains(".m3u8")) {
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            /* Fallback */
            ext = ".mp4";
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        // this prevents another check when download is about to happen! -raztoki
        if (isDownload) {
            return AvailableStatus.TRUE;
        }
        if (dllink != null && !dllink.contains(".m3u8")) {
            URLConnectionAdapter con = null;
            try {
                // if you don't do this then referrer is fked for the download! -raztoki
                final Browser br = this.br.cloneBrowser();
                // In case the link redirects to the finallink -
                br.setFollowRedirects(true);
                try {
                    con = br.openHeadConnection(dllink);
                    final String workaroundURL = getHttpServerErrorWorkaroundURL(br.getHttpConnection());
                    if (workaroundURL != null) {
                        con = br.openHeadConnection(workaroundURL);
                    }
                } catch (final BrowserException e) {
                    server_issues = true;
                    return AvailableStatus.TRUE;
                }
                final long filesize = con.getLongContentLength();
                if (!con.getContentType().contains("html") && filesize > 100000) {
                    downloadLink.setDownloadSize(filesize);
                    final String redirect_url = br.getHttpConnection().getRequest().getUrl();
                    if (redirect_url != null) {
                        dllink = redirect_url;
                        logger.info("dllink: " + dllink);
                    }
                    downloadLink.setProperty("directlink", dllink);
                } else {
                    server_issues = true;
                }
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
        isDownload = true;
        requestFileInformation(downloadLink);
        if (dllink == null) {
            /* 2016-12-02: At this stage we should have a working hls to http workaround so we should never get hls urls. */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (this.dllink.contains(".m3u8")) {
            /* hls download */
            /* Access hls master. */
            getPage(this.dllink);
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            /* http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
            final String workaroundURL = getHttpServerErrorWorkaroundURL(dl.getConnection());
            if (workaroundURL != null) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, workaroundURL, free_resume, free_maxchunks);
            }
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

    private String getHttpServerErrorWorkaroundURL(final URLConnectionAdapter con) {
        String workaroundURL = null;
        if (con.getResponseCode() == 403 || con.getResponseCode() == 404) {
            /*
             * Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com (404), txxx.com
             * (403)
             */
            workaroundURL = br.getHttpConnection().getRequest().getUrl();
        }
        return workaroundURL;
    }

    public static String getDllink(final DownloadLink dl, final Browser br) throws PluginException {
        /*
         * Newer KVS versions also support html5 --> RegEx for that as this is a reliable source for our final downloadurl.They can contain
         * the old "video_url" as well but it will lead to 404 --> Prefer this way.
         * 
         * 
         * E.g. wankoz.com, pervclips.com, pornicom.com
         */
        String dllink = null;
        final String json_playlist_source = br.getRegex("sources\\s*?:\\s*?(\\[.*?\\])").getMatch(0);
        String httpurl_temp = null;
        if (json_playlist_source != null) {
            /* 2017-03-16: E.g. txxx.com */
            /* TODO: Eventually improve this */
            // see if there are non hls streams first. since regex does this based on first in entry of source == =[ raztoki20170507
            dllink = new Regex(json_playlist_source, "'file'\\s*?:\\s*?'((?!.*\\.m3u8)http[^<>\"']*?(mp4|flv)[^<>\"']*?)'").getMatch(0);
            if (dllink == null) {
                dllink = new Regex(json_playlist_source, "'file'\\s*?:\\s*?'(http[^<>\"']*?(mp4|flv|m3u8)[^<>\"']*?)'").getMatch(0);
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("flashvars\\['video_html5_url'\\]='(http[^<>\"]*?)'").getMatch(0);
        }
        if (dllink == null) {
            /* E.g. yourlust.com */
            dllink = br.getRegex("flashvars\\.video_html5_url = \"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            /* RegEx for "older" KVS versions */
            dllink = br.getRegex("video_url[\t\n\r ]*?:[\t\n\r ]*?'(http[^<>\"]*?)'").getMatch(0);
        }
        if (dllink == null && dl.getDownloadURL().contains("xfig.net/")) {
            /* Small workaround - do not include the slash at the end. */
            dllink = br.getRegex("var videoFile=\"(http[^<>\"]*?)/?\"").getMatch(0);
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Accept-Encoding", "identity;q=1, *;q=0");
        }
        if (dllink == null) {
            dllink = br.getRegex("(http://[A-Za-z0-9\\.\\-]+/get_file/[^<>\"\\&]*?)(?:\\&|'|\")").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("(?:file|video)\\s*?:\\s*?(?:\"|')(http[^<>\"\\']*?(?:m3u8|mp4|flv)[^<>\"]*?)(?:\"|')").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("(?:file|url):[\t\n\r ]*?(\"|')(http[^<>\"\\']*?(?:m3u8|mp4|flv)[^<>\"]*?)\\1").getMatch(1);
        }
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"(https?://[^<>\"]*?)\" type=(\"|')video/(?:mp4|flv)\\2").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            /* 2016-11-01 - bravotube.net */
            dllink = br.getRegex("<source src=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (dllink != null && dllink.contains(".m3u8")) {
            /* 2016-12-02 - txxx.com, tubecup.com, hdzog.com */
            /* Prefer httpp over hls */
            try {
                /* First try to find highest quality */
                final String fallback_player_json = br.getRegex("\\.on\\(\\'setupError\\',function\\(\\)\\{[^>]*?jwsettings\\.playlist\\[0\\]\\.sources=(\\[.*?\\])").getMatch(0);
                final String[][] qualities = new Regex(fallback_player_json, "\\'label\\'\\s*?:\\s*?\\'(\\d+)p\\',\\s*?\\'file\\'\\s*?:\\s*?\\'(http[^<>\\']+)\\'").getMatches();
                int quality_max = 0;
                for (final String[] qualityInfo : qualities) {
                    final String quality_temp_str = qualityInfo[0];
                    final String quality_url_temp = qualityInfo[1];
                    final int quality_temp = Integer.parseInt(quality_temp_str);
                    if (quality_temp > quality_max) {
                        quality_max = quality_temp;
                        httpurl_temp = quality_url_temp;
                    }
                }
            } catch (final Throwable e) {
            }
            /* Last chance */
            if (httpurl_temp == null) {
                httpurl_temp = br.getRegex("\\.on\\(\\'setupError\\',function\\(\\)\\{[^>]*?\\'file\\'\\s*?:\\s*?\\'(http[^<>\"\\']*?\\.mp4[^<>\"\\']*?)\\'").getMatch(0);
            }
            if (httpurl_temp != null) {
                /* Prefer http over hls */
                dllink = httpurl_temp;
            }
        }
        if (dllink == null) {
            if (!br.containsHTML("license_code:") && !br.containsHTML("kt_player_[0-9\\.]+\\.swfx?")) {
                /* No licence key present in html and/or no player --> No video --> Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // dllink = Encoding.htmlDecode(dllink);
        dllink = Encoding.urlDecode(dllink, true);
        return dllink;
    }

    private String regexStandardTitleWithHost(final String host) {
        return br.getRegex(Pattern.compile("<title>([^<>\"]*?) \\- " + host + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
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
