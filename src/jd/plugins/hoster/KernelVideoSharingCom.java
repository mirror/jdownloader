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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.components.kvs.Script;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "yourlust.com", "pornyeah.com", "javbangers.com", "videocelebs.net", "porngem.com", "webcamsbabe.com", "privat-zapisi.biz", "sexvid.xxx", "pornbimbo.com" }, urls = { "https?://(?:www\\.)?yourlust\\.com/videos/[a-z0-9\\-]+\\.html", "https?://(?:www\\.)?pornyeah\\.com/(?:videos/[a-z0-9\\-]+-\\d+\\.html|embed/\\d+)", "https?://(?:www\\.)?(?:javwhores|javbangers)\\.com/video/\\d+/[a-z0-9\\-]+/?", "https?://(?:www\\.)?videocelebs\\.net/[a-z0-9\\-]+\\.html", "https?://(?:www\\.)?porngem\\.com/(videos/[a-z0-9\\-]+-\\d+/?|embed/\\d+)", "https?://(?:www\\.)?webcamsbabe\\.com/videos/\\d+\\-[a-z0-9\\-]+\\.html", "https?://(?:www\\.)?privat-zapisi\\.biz/videos/\\d+\\-[a-z0-9\\-]+\\.html", "https?://(?:[a-z]+\\.)?sexvid\\.xxx/[a-z0-9\\-]+\\.html",
        "https?://(?:www\\.)?pornbimbo\\.com/(?:video/\\d+/[a-z0-9\\-]+/?|embed/\\d+)" })
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
    // other #2: Special websites that have their own plugins (examples): pornktube.com, alotporn.com, clipcake.com
    // other #3: Plugins with "high security" removed 2015-07-02: BravoTubeNet, BravoTeensCom
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
    private static final int    free_maxdownloads        = -1;
    /* E.g. normal kernel-video-sharing.com video urls */
    private static final String type_normal              = "^https?://[^/]+/(?:[a-z]{2}/)?(?:videos?/?)?(\\d+)/([a-z0-9\\-]+)(?:/?|\\.html)$";
    private static final String type_normal_fuid_at_end  = "^https?://[^/]+/videos/([a-z0-9\\-]+)-(\\d+)(?:/?|\\.html)$";
    /* Rare case. Example: xbabe.com */
    private static final String type_normal_without_fuid = ".+/videos/([a-z0-9\\-]+)/?$";
    private static final String type_normal_mobile       = "^https?://m\\.([^/]+/(videos/)?\\d+/[a-z0-9\\-]+/$)";
    /* E.g. sex3.com */
    public static final String  type_only_numbers        = "^https?://[^/]+/(?:video/)?(\\d+)/$";
    public static final String  type_embedded            = "^https?://[^/]+/embed/(\\d+)/?$";
    private String              dllink                   = null;
    private boolean             server_issues            = false;
    private boolean             private_video            = false;

    @Override
    public String getAGBLink() {
        return "http://www.kvs-demo.com/terms.php";
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(type_normal_mobile)) {
            /* Correct mobile urls --> Normal URLs */
            final Regex info = new Regex(link.getPluginPatternMatcher(), "^(https?://)m\\.([^/]+/(videos/)?\\d+/[a-z0-9\\-]+/$)");
            link.setPluginPatternMatcher(String.format("%swww.%s", info.getMatch(0), info.getMatch(1)));
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = regexFUIDAuto(null, link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    protected boolean hasFUIDAtEnd(final String url) {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean download_started) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        getPage(link.getPluginPatternMatcher());
        setSpecialFlags();
        final String filename_url = regexURLFilenameAuto(this.br, link);
        if (br.containsHTML("KernelTeamVideoSharingSystem\\.js|KernelTeamImageRotator_")) {
            /* <script src="/js/KernelTeamImageRotator_3.8.1.jsx?v=3"></script> */
            /* <script type="text/javascript" src="http://www.hclips.com/js/KernelTeamVideoSharingSystem.js?v=3.8.1"></script> */
        }
        /* TODO: Add fallback(s) for missing file title! (Content-Disposition and maybe filename from inside final downloadurl). */
        String filename = regexFilenameAuto(br, link);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("/404.php")) {
            /* Definitly offline - set url filename to avoid bad names! */
            if (!StringUtils.isEmpty(filename_url)) {
                link.setName(filename_url);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * 2019-07-25: Set fuid again here because for some hosts they're not given inside the URL which the user has added but rather
         * inside their embed URLs and also inside html
         */
        final String fuid = regexFUIDAuto(this.br, link);
        if (fuid != null) {
            link.setLinkID(this.getHost() + "://" + fuid);
            link.setName(fuid);
        }
        try {
            dllink = getDllink(br, this);
        } catch (final PluginException e) {
            if (this.private_video && e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                logger.info("ERROR_FILE_NOT_FOUND in getDllink but we have a private video so it is not offline ...");
            } else {
                throw e;
            }
        }
        final String ext;
        if (dllink != null && !dllink.contains(".m3u8")) {
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            /* Fallback */
            ext = ".mp4";
        }
        if (!StringUtils.isEmpty(filename) && !filename.endsWith(ext)) {
            filename += ext;
        }
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        // this prevents another check when download is about to happen! -raztoki
        if (download_started) {
            return AvailableStatus.TRUE;
        }
        if (dllink != null && !dllink.contains(".m3u8")) {
            URLConnectionAdapter con = null;
            try {
                // if you don't do this then referrer is fked for the download! -raztoki
                final Browser br = this.br.cloneBrowser();
                br.setAllowedResponseCodes(new int[] { 405 });
                // In case the link redirects to the finallink -
                br.setFollowRedirects(true);
                try {
                    // br.getHeaders().put("Accept-Encoding", "identity");
                    con = openAntiDDoSRequestConnection(br, br.createHeadRequest(dllink));
                    final String workaroundURL = getHttpServerErrorWorkaroundURL(con);
                    if (workaroundURL != null) {
                        con.disconnect();
                        con = openAntiDDoSRequestConnection(br, br.createHeadRequest(workaroundURL));
                    }
                } catch (final BrowserException e) {
                    logger.log(e);
                    server_issues = true;
                    return AvailableStatus.TRUE;
                }
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    }
                    final String redirect_url = br.getHttpConnection().getRequest().getUrl();
                    if (redirect_url != null) {
                        dllink = redirect_url;
                        logger.info("dllink: " + dllink);
                    }
                } else {
                    try {
                        br.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
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

    private void setSpecialFlags() {
        /* 2020-10-09: Tested for pornyeah.com, anyporn.com, camwhoreshd.com */
        if (br.containsHTML(">\\s*This video is a private video uploaded by |Only active members can watch private videos")) {
            this.private_video = true;
        } else {
            this.private_video = false;
        }
    }

    protected void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (false) {
            // untested, handlePremium untested
            if ((private_video || StringUtils.isEmpty(this.dllink)) && account != null) {
                server_issues = false;
                private_video = false;
                login(account, false);
                requestFileInformation(link, true);
            }
        }
        if (inValidate(dllink, this)) {
            if (private_video) {
                throw new AccountRequiredException("Private video");
            } else {
                /* 2016-12-02: At this stage we should have a working hls to http workaround so we should never get hls urls. */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (StringUtils.containsIgnoreCase(dllink, ".m3u8")) {
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
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            final int maxChunks = getMaxChunks(link);
            final boolean isResumeable = canResume(link);
            /* http download */
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, this.dllink, isResumeable, maxChunks);
            final String workaroundURL = getHttpServerErrorWorkaroundURL(dl.getConnection());
            if (workaroundURL != null) {
                dl.getConnection().disconnect();
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, workaroundURL, isResumeable, maxChunks);
            }
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 503) {
                    /* Should only happen in rare cases */
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 connection limit reached", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
                }
            }
            dl.startDownload();
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, true);
        this.handleDownload(link, account);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        handleDownload(link, null);
    }

    private int getMaxChunks(final DownloadLink link) {
        return 0;
    }

    private boolean canResume(final DownloadLink link) {
        return true;
    }

    /** TODO: Use this in future KVS plugins that need account support! See e.g. CamwhoresbayCom */
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    getPage("https://www." + this.getHost() + "/");
                    if (isLoggedIN()) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                br.clearCookies(this.getHost());
                getPage("https://www." + this.getHost() + "/login/");
                /*
                 * 2017-01-21: This request will usually return a json with some information about the account. Until now there are no
                 * premium accounts available at all.
                 */
                postPage("/login/", "remember_me=1&action=login&email_link=http%3A%2F%2Fwww." + this.getHost() + "%2Femail%2F&format=json&mode=async&username=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedIN()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.getCookie(br.getHost(), "kt_member", Cookies.NOTDELETEDPATTERN) != null;
    }

    public static String getHttpServerErrorWorkaroundURL(final URLConnectionAdapter con) {
        String workaroundURL = null;
        if (con.getResponseCode() == 403 || con.getResponseCode() == 404 || con.getResponseCode() == 405) {
            /*
             * Small workaround for buggy servers that redirect and fail if the Referer is wrong then or Cloudflare cookies were missing on
             * first attempt (e.g. clipcake.com). Examples: hdzog.com (404), txxx.com (403)
             */
            workaroundURL = con.getRequest().getUrl();
        }
        return workaroundURL;
    }

    public static String getDllink(final Browser br, final Plugin plugin) throws PluginException, IOException {
        /*
         * Newer KVS versions also support html5 --> RegEx for that as this is a reliable source for our final downloadurl.They can contain
         * the old "video_url" as well but it will lead to 404 --> Prefer this way.
         *
         * E.g. wankoz.com, pervclips.com, pornicom.com
         */
        // final String pc3_vars = br.getRegex("pC3\\s*:\\s*'([^<>\"\\']+)'").getMatch(0);
        // final String videoID = br.getRegex("video_id\\s*:\\s*(?:')?(\\d+)\\s*(?:')?").getMatch(0);
        // if (pc3_vars != null && videoID != null) {
        // /* 2019-11-26: TODO: Add support for this: Used by a lot of these hosts to hide their directurls */
        // br.postPage("/sn4diyux.php", "param=" + videoID + "," + pc3_vars);
        // String crypted_url = getDllinkCrypted(br);
        // }
        String dllink = null;
        final String json_playlist_source = br.getRegex("sources\\s*?:\\s*?(\\[.*?\\])").getMatch(0);
        String httpurl_temp = null;
        if (json_playlist_source != null) {
            /* 2017-03-16: E.g. txxx.com */
            /* TODO: Eventually improve this */
            // see if there are non hls streams first. since regex does this based on first in entry of source == =[ raztoki20170507
            dllink = new Regex(json_playlist_source, "'file'\\s*:\\s*'((?!.*\\.m3u8)http[^<>\"']*?(mp4|flv)[^<>\"']*?)'").getMatch(0);
            if (inValidate(dllink, plugin)) {
                dllink = new Regex(json_playlist_source, "'file'\\s*:\\s*'(http[^<>\"']*?(mp4|flv|m3u8)[^<>\"']*?)'").getMatch(0);
            }
        }
        if (inValidate(dllink, plugin)) {
            dllink = br.getRegex("flashvars\\['video_html5_url'\\]='(http[^<>\"]*?)'").getMatch(0);
        }
        if (inValidate(dllink, plugin)) {
            /* E.g. yourlust.com */
            dllink = br.getRegex("flashvars\\.video_html5_url\\s*=\\s*\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (inValidate(dllink, plugin)) {
            /* Try to find the highest quality possible --> Example website that has multiple qualities available: camwhoresbay.com */
            /*
             * This will NOT e.g. work for: video_url_text: 'High Definition' --> E.g. xxxymovies.com --> But this one only has one quality
             * available
             */
            /* Example website wi */
            int maxQuality = 0;
            final String[][] videoInfos = br.getRegex("([a-z0-9_]+_text)\\s*:\\s*'(\\d+)p'").getMatches();
            for (final String[] vidInfo : videoInfos) {
                final String varNameText = vidInfo[0];
                final String videoQualityStr = vidInfo[1];
                final int videoQuality = Integer.parseInt(videoQualityStr);
                if (videoQuality > maxQuality) {
                    final String varNameVideoURL = varNameText.replace("_text", "");
                    maxQuality = videoQuality;
                    dllink = br.getRegex(varNameVideoURL + "\\s*:\\s*'((?:http|/)[^<>\"']*?)'").getMatch(0);
                }
            }
        }
        if (inValidate(dllink, plugin)) {
            // E.g. xxxymovies.com, javbangers.com
            dllink = br.getRegex("video_url\\s*:\\s*'((?:http|/)[^<>\"']*?)'").getMatch(0);
        }
        /* 2020-08-05: TODO: The following 4 lines should not be needed anymore. Remove them on next cleanup. */
        // if (inValidate(dllink, plugin)) {
        // // HD javbangers.com
        // dllink = br.getRegex("video_alt_url\\s*:\\s*\\'((?:http|/)[^<>\"]*?)\\'").getMatch(0);
        // }
        if (inValidate(dllink, plugin)) {
            // function/0/http camwheres, pornyeah - find best quality
            final String functions[] = br.getRegex("(function/0/https?://[A-Za-z0-9\\.\\-]+/get_file/[^<>\"]*?)(?:\\&amp|'|\")").getColumn(0);
            final String crypted;
            if (functions != null && functions.length == 1) {
                crypted = functions[0];
            } else if (functions != null) {
                String best = null;
                for (String function : functions) {
                    if (best == null) {
                        best = function;
                    } else if (function.contains("_720p")) {
                        best = function;
                        break;
                    } else if (function.contains("_480p")) {
                        best = function;
                    }
                }
                crypted = best;
            } else {
                crypted = null;
            }
            if (crypted != null) {
                dllink = getDllinkCrypted(br, crypted);
            } else {
                if (br.containsHTML("function/0/http")) {
                    dllink = br.getRegex("(function/0/http[^']+)'").getMatch(0);
                    plugin.getLogger().info("function:" + dllink);
                }
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            /* 2019-07-24: E.g. pick best of multiple sources e.g. multiple sources available: xbabe.com */
            int qualityMax = 0;
            int qualityTmp = 0;
            final String[] sources = br.getRegex("<source[^>]*?src=\"(https?://[^<>\"]*?)\"[^>]*?type=(\"|')video/[a-z0-9]+\\2[^>]*?").getColumn(-1);
            for (final String source : sources) {
                final String dllinkTemp = new Regex(source, "src=\"(https?://[^<>\"]+)\"").getMatch(0);
                String qualityTempStr = new Regex(source, "title=\"(\\d+)p\"").getMatch(0);
                if (qualityTempStr == null) {
                    /* 2020-01-29: More open RegEx e.g. pornhat.com */
                    qualityTempStr = new Regex(source, "(\\d+)p").getMatch(0);
                }
                if (dllinkTemp == null || qualityTempStr == null) {
                    continue;
                }
                qualityTmp = Integer.parseInt(qualityTempStr);
                if (qualityTmp > qualityMax) {
                    qualityMax = qualityTmp;
                    dllink = dllinkTemp;
                }
            }
        }
        if (inValidate(dllink, plugin)) {
            dllink = br.getRegex("(https?://[A-Za-z0-9\\.\\-]+/get_file/[^<>\"]*?)(?:'|\")").getMatch(0); // 2018-06-20
            if (StringUtils.endsWithCaseInsensitive(dllink, "jpg/")) {
                dllink = null;
            }
        }
        if (inValidate(dllink, plugin)) {
            dllink = br.getRegex("(?:file|video)\\s*?:\\s*?(?:\"|')(http[^<>\"\\']*?\\.(?:m3u8|mp4|flv)[^<>\"]*?)(?:\"|')").getMatch(0);
        }
        if (inValidate(dllink, plugin)) {
            dllink = br.getRegex("(?:file|url):[\t\n\r ]*?(\"|')(http[^<>\"\\']*?\\.(?:m3u8|mp4|flv)[^<>\"]*?)\\1").getMatch(1);
        }
        if (inValidate(dllink, plugin)) { // tryboobs.com
            dllink = br.getRegex("<video src=\"(https?://[^<>\"]*?)\" controls").getMatch(0);
        }
        if (inValidate(dllink, plugin)) {
            dllink = br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (inValidate(dllink, plugin)) {
            /* 2016-11-01 - bravotube.net */
            dllink = br.getRegex("<source src=\"([^<>\"]*?)\"").getMatch(0);
        }
        if (inValidate(dllink, plugin)) {
            /* 2018-03-12 - wankoz.com */
            dllink = br.getRegex("source[^']+src:\\s*'([^']*?)'").getMatch(0);
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
        if (inValidate(dllink, plugin)) {
            String video_url = br.getRegex("var\\s+video_url\\s*=\\s*(\"|')(.*?)(\"|')\\s*;").getMatch(1);
            if (video_url == null) {
                video_url = br.getRegex("var\\s+video_url=Dpww3Dw64\\(\"([^\"]+)").getMatch(0);
            }
            // hdzog.com, hclips.com
            String video_url_append = br.getRegex("video_url\\s*\\+=\\s*(\"|')(.*?)(\"|')\\s*;").getMatch(1);
            if (video_url != null && video_url_append != null) {
                video_url += video_url_append;
            }
            if (video_url != null) {
                String[] videoUrlSplit = video_url.split("\\|\\|");
                video_url = videoUrlSplit[0];
                if (!video_url.startsWith("http")) {
                    final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    final Invocable inv = (Invocable) engine;
                    try {
                        engine.eval(IO.readURLToString(Script.class.getResource("script.js")));
                        final Object result = inv.invokeFunction("result", video_url);
                        if (result != null) {
                            dllink = result.toString();
                        }
                    } catch (final Throwable e) {
                        plugin.getLogger().log(e);
                    }
                } else {
                    dllink = video_url;
                }
                if (videoUrlSplit.length == 2) {
                    dllink = dllink.replaceFirst("/get_file/\\d+/[0-9a-z]{32}/", videoUrlSplit[1]);
                } else if (videoUrlSplit.length == 4) {
                    dllink = dllink.replaceFirst("/get_file/\\d+/[0-9a-z]{32}/", videoUrlSplit[1]);
                    dllink = dllink + "&lip=" + videoUrlSplit[2];
                    dllink = dllink + "&lt=" + videoUrlSplit[3];
                }
                return dllink;
            }
        }
        if (inValidate(dllink, plugin)) {
            if (!br.containsHTML("license_code:") && !br.containsHTML("kt_player_[0-9\\.]+\\.swfx?")) {
                /* No licence key present in html and/or no player --> No video --> Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // dllink = Encoding.htmlDecode(dllink); - Why?
        dllink = Encoding.urlDecode(dllink, true);
        if (dllink.contains("&amp;")) {
            dllink = Encoding.htmlDecode(dllink);
        }
        return dllink;
    }

    public static String getDllinkCrypted(final Browser br) {
        String videoUrl = br.getRegex("video_url\\s*:\\s*'(.+?)'").getMatch(0);
        if (videoUrl == null) {
            /* 2019-11-26: Nested in js brackets */
            videoUrl = PluginJSonUtils.getJson(br, "video_url");
        }
        if (videoUrl == null) {
            return null;
        }
        return getDllinkCrypted(br, videoUrl);
    }

    public static String getDllinkCrypted(final Browser br, final String videoUrl) {
        String dllink = null;
        // final String scriptUrl = br.getRegex("src=\"([^\"]+kt_player\\.js.*?)\"").getMatch(0);
        final String licenseCode = br.getRegex("license_code\\s*?:\\s*?\\'(.+?)\\'").getMatch(0);
        if (videoUrl.startsWith("function")) {
            if (videoUrl != null && licenseCode != null) {
                // final Browser cbr = br.cloneBrowser();
                // cbr.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                // cbr.getPage(scriptUrl);
                // final String hashRange = cbr.getRegex("(\\d+)px").getMatch(0);
                String hashRange = "16";
                dllink = decryptHash(videoUrl, licenseCode, hashRange);
            }
        } else {
            /* Return input */
            dllink = videoUrl;
        }
        return dllink;
    }

    private static String decryptHash(final String videoUrl, final String licenseCode, final String hashRange) {
        String result = null;
        List<String> videoUrlPart = new ArrayList<String>();
        Collections.addAll(videoUrlPart, videoUrl.split("/"));
        // hash
        String hash = videoUrlPart.get(7).substring(0, 2 * Integer.parseInt(hashRange));
        String nonConvertHash = videoUrlPart.get(7).substring(2 * Integer.parseInt(hashRange));
        String seed = calcSeed(licenseCode, hashRange);
        String[] seedArray = new String[seed.length()];
        for (int i = 0; i < seed.length(); i++) {
            seedArray[i] = seed.substring(i, i + 1);
        }
        if (seed != null && hash != null) {
            for (int k = hash.length() - 1; k >= 0; k--) {
                String[] hashArray = new String[hash.length()];
                for (int i = 0; i < hash.length(); i++) {
                    hashArray[i] = hash.substring(i, i + 1);
                }
                int l = k;
                for (int m = k; m < seedArray.length; m++) {
                    l += Integer.parseInt(seedArray[m]);
                }
                for (; l >= hashArray.length;) {
                    l -= hashArray.length;
                }
                StringBuffer n = new StringBuffer();
                for (int o = 0; o < hashArray.length; o++) {
                    n.append(o == k ? hashArray[l] : o == l ? hashArray[k] : hashArray[o]);
                }
                hash = n.toString();
            }
            videoUrlPart.set(7, hash + nonConvertHash);
            for (String string : videoUrlPart.subList(2, videoUrlPart.size())) {
                if (result == null) {
                    result = string;
                } else {
                    result = result + "/" + string;
                }
            }
        }
        return result;
    }

    private static String calcSeed(final String licenseCode, final String hashRange) {
        StringBuffer fb = new StringBuffer();
        String[] licenseCodeArray = new String[licenseCode.length()];
        for (int i = 0; i < licenseCode.length(); i++) {
            licenseCodeArray[i] = licenseCode.substring(i, i + 1);
        }
        for (String c : licenseCodeArray) {
            if (c.equals("$")) {
                continue;
            }
            int v = Integer.parseInt(c);
            fb.append(v != 0 ? c : "1");
        }
        String f = fb.toString();
        int j = f.length() / 2;
        int k = Integer.parseInt(f.substring(0, j + 1));
        int l = Integer.parseInt(f.substring(j));
        int g = l - k;
        g = Math.abs(g);
        int fi = g;
        g = k - l;
        g = Math.abs(g);
        fi += g;
        fi *= 2;
        String s = String.valueOf(fi);
        String[] fArray = new String[s.length()];
        for (int i = 0; i < s.length(); i++) {
            fArray[i] = s.substring(i, i + 1);
        }
        int i = Integer.parseInt(hashRange) / 2 + 2;
        StringBuffer m = new StringBuffer();
        for (int g2 = 0; g2 < j + 1; g2++) {
            for (int h = 1; h <= 4; h++) {
                int n = Integer.parseInt(licenseCodeArray[g2 + h]) + Integer.parseInt(fArray[g2]);
                if (n >= i) {
                    n -= i;
                }
                m.append(String.valueOf(n));
            }
        }
        return m.toString();
    }

    /**
     * Returns either current browser URL or PluginPatternMatcher depending on which is 'better' in terms of which information we may find
     * inside that URL.
     */
    public static String getURL_source(final Browser br, final DownloadLink dl) {
        if (br == null && dl == null) {
            return null;
        }
        final String url_source;
        final String current_browser_url = br != null ? br.getURL() : null;
        if (current_browser_url != null && current_browser_url.length() > dl.getPluginPatternMatcher().length() && current_browser_url.matches(type_normal)) {
            url_source = current_browser_url;
        } else {
            url_source = dl.getContentUrlOrPatternMatcher();
        }
        return url_source;
    }

    /**
     * Finds title inside a KVS URL. <br />
     * Automatically decides whether to use the current Browsers' URL or the original source URL added by the user.
     */
    protected String regexURLFilenameAuto(final Browser br, final DownloadLink dl) {
        if (br == null || dl == null) {
            return null;
        }
        final String url_source = getURL_source(br, dl);
        String url_filename = regexURLFilename(url_source);
        if (StringUtils.isEmpty(url_filename)) {
            url_filename = regexFUIDAuto(br, dl);
        }
        return url_filename;
    }

    /**
     * Finds title inside a given KVS URL. <br />
     */
    public static String regexURLFilename(final String url_source) {
        if (url_source == null) {
            return null;
        }
        String filename_url = regexURLFilenameSiteSpecific(url_source);
        if (StringUtils.isEmpty(filename_url)) {
            if (url_source.matches(type_normal_fuid_at_end)) {
                filename_url = new Regex(url_source, type_normal_fuid_at_end).getMatch(0);
            } else if (url_source.matches(type_normal)) {
                filename_url = new Regex(url_source, type_normal).getMatch(1);
            } else if (url_source.matches(type_normal_without_fuid)) {
                filename_url = new Regex(url_source, type_normal_without_fuid).getMatch(0);
            } else {
                /* We can only use fuid as filename */
                filename_url = null;
            }
        }
        if (!StringUtils.isEmpty(filename_url)) {
            /* Make the url-filenames look better by using spaces instead of '-'. */
            filename_url = filename_url.replace("-", " ");
        }
        return filename_url;
    }

    /**
     * Uses site-specific RegExes to find the URLFilename (if one is available for current website). <br />
     * 2017-07-27: So far there are no site-specific RegExes for URLFilenames.
     */
    public static String regexURLFilenameSiteSpecific(final String url) {
        if (url == null) {
            return null;
        }
        final String host = Browser.getHost(url);
        if (host == null) {
            return null;
        }
        String filename = null;
        if (host.equalsIgnoreCase("crockotube.com")) {
            filename = new Regex(url, "/watch/(.+)-[A-Z0-9]+$").getMatch(0);
        }
        return filename;
    }

    /**
     * Tries everything possible to find a nice filename for KVS websites. <br />
     * Returns url_filename as fallback.
     */
    protected String regexFilenameAuto(final Browser br, final DownloadLink dl) {
        String filename;
        final String filename_url = regexURLFilenameAuto(br, dl);
        final String url_source = getURL_source(br, dl);
        final String current_host = dl.getHost();
        /* Find 'real' filename and the one inside our URL. */
        /* Prefer website-specific filename */
        filename = regexFilenameSiteSpecific(br);
        if (filename == null) {
            if (url_source.matches(type_only_numbers)) {
                filename = br.getRegex("<title>\\s*([^<>\"]*?)\\s*</title>").getMatch(0);
            } else if (url_source.matches(type_embedded)) {
                /* Embed content usually won't have any useful filename/title available */
                filename = br.getRegex("<title>\\s*([^<>\"]*?)\\s*(/|-)\\s*Embed\\s*(Player|Video)</title>").getMatch(0);
                if (StringUtils.isEmpty(filename)) {
                    /* Fallback to fuid as filename */
                    filename = regexFUIDAuto(br, dl);
                }
            } else {
                // filename = regexFilenameSiteSpecific(br);
                filename = regexFilenameGeneral(br);
                if (StringUtils.isEmpty(filename)) {
                    filename = regexStandardTitleWithHost(br, br.getHost());
                }
            }
        }
        /* Now decide which filename we want to use */
        if (StringUtils.isEmpty(filename)) {
            filename = filename_url;
        } else {
            /* Remove html crap and spaces at the beginning and end. */
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = cleanupFilename(br, filename);
        }
        return filename;
    }

    /** Tries to find unique (video-)ID inside URL. It is not guaranteed to return anything but it should in most of all cases! */
    protected String regexFUIDAuto(final Browser br, final DownloadLink dl) {
        final String url_source = getURL_source(br, dl);
        if (url_source == null) {
            return null;
        } else {
            String fuid = null;
            if (url_source.matches(type_only_numbers)) {
                fuid = new Regex(url_source, type_only_numbers).getMatch(0);
            } else if (url_source.matches(type_embedded)) {
                fuid = new Regex(url_source, type_embedded).getMatch(0);
            } else if (url_source.matches(type_normal_fuid_at_end) && hasFUIDAtEnd(url_source)) {
                fuid = new Regex(url_source, type_normal_fuid_at_end).getMatch(1);
            } else if (url_source.matches(type_normal)) {
                fuid = new Regex(url_source, type_normal).getMatch(0);
            }
            if (fuid == null && br != null) {
                /* Rare case: Embed URL: No fuid given inside URL so we can try to find it via embed URL inside html. Example: xbabe.com */
                fuid = br.getRegex("\"https?://[^/]+/embed/(\\d+)/?\"").getMatch(0);
            }
            return fuid;
        }
    }

    /**
     * Removes parts of hostname from filename e.g. if host is "testhost.com", it will remove things such as " - TestHost", "testhost.com"
     * and so on.
     */
    private static String cleanupFilename(final Browser br, String filename_normal) {
        final String host = br.getHost();
        if (host == null) {
            return filename_normal;
        }
        final String host_without_tld = host.split("\\.")[0];
        String filename_clean = filename_normal.replace(" - " + host, "");
        filename_clean = filename_clean.replace("- " + host, "");
        filename_clean = filename_clean.replace(" " + host, "");
        filename_clean = filename_clean.replace(" - " + host, "");
        filename_clean = filename_clean.replace("- " + host, "");
        filename_clean = filename_clean.replace(" " + host, "");
        if (StringUtils.isEmpty(filename_clean)) {
            /* If e.g. filename only consisted of hostname, return original as fallback though this should never happen! */
            return filename_normal;
        }
        return filename_clean;
    }

    public static String regexFilenameGeneral(final Browser br) {
        /* Works e.g. for hdzog.com */
        String filename = br.getRegex("var video_title\\s*?=\\s*?\"([^<>]*?)\";").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            /* Newer KVS e.g. tubecup.com */
            filename = br.getRegex("title[\t\n\r ]*?:[\t\n\r ]*?\"([^<>\"]*?)\"").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("<h\\d+ class=\"album_title\">([^<>]*?)<").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"(?:Free Porn Videos \\| )?([^<>\"]*?)\"").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("\"media-title\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fails e.g. for alphaporno.com */
            filename = br.getRegex("<h\\d+ class=\"title\">([^<>\"]*?)<").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            /* Working e.g. for wankoz.com */
            filename = br.getRegex("<h\\d+ class=\"block_header\" id=\"desc_button\">([^<>\"]*?)</h\\d+>").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            /* Working e.g. for pervclips.com, pornicom.com */
            filename = br.getRegex("class=\"heading video-heading\">[\t\n\r ]+<(h\\d+)>([^<>\"]*?)</h\\1>").getMatch(1);
        }
        return filename;
    }

    /**
     * Uses site-specific RegExes to find the filename (if one is available for current website). <br />
     * Use this first to find the filename for KVS websites!
     */
    public static String regexFilenameSiteSpecific(final Browser br) {
        String filename;
        if (br.getHost().equalsIgnoreCase("yourlust.com")) {
            /* 2016-12-21 */
            filename = br.getRegex("<h\\d+ class=\"[^<>]+>([^<>]*?)<").getMatch(0);
        } else if (br.getHost().equalsIgnoreCase("theclassicporn.com")) {
            /* 2016-12-18 */
            filename = br.getRegex("class=\"link\\-blue link\\-no\\-border\">([^<>\"]*?)<").getMatch(0);
        } else if (br.getHost().equalsIgnoreCase("alphaporno.com")) {
            /* 2017-08-03 */
            filename = br.getRegex("<h1 class=\"title\" itemprop=\"name\">([^<>\"]+)</h1>").getMatch(0);
            if (StringUtils.isEmpty(filename)) {
                filename = br.getRegex("<title>([^<>]+)</title>").getMatch(0);
            }
        } else {
            filename = null;
        }
        return filename;
    }

    /** Many websites in general use this format - title plus their own hostname as ending. */
    public static String regexStandardTitleWithHost(final Browser br, final String host) {
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
    public static boolean inValidate(final String s, final Plugin plugin) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else if (("upornia.com".equals(plugin.getHost()) || "vjav.com".equals(plugin.getHost())) && s.contains("/player/timeline")) {
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
