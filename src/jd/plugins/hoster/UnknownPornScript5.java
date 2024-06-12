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
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLSearch;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.storage.TypeRef;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "boyfriendtv.com", "ashemaletube.com", "pornoxo.com", "worldsex.com", "bigcamtube.com", "porneq.com" }, urls = { "https?://(?:\\w+\\.)?boyfriendtv\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:\\w+\\.)?ashemaletube\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:\\w+\\.)?pornoxo\\.com/videos/\\d+/[a-z0-9\\-]+/", "https?://(?:\\w+\\.)?worldsex\\.com/videos/[a-z0-9\\-]+\\-\\d+(?:\\.html|/)?", "https?://(?:\\w+\\.)?bigcamtube\\.com/videos/[a-z0-9\\-]+/", "https?://(?:\\w+\\.)?porneq\\.com/(?:video/\\d+/[a-z0-9\\-]+/?|wporn/porn-videos/[a-z0-9\\-]+/\\d+/)" })
public class UnknownPornScript5 extends PluginForHost {
    public UnknownPornScript5(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/registration/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* V0.1 */
    // other: Should work for all (porn) sites that use the "jwplayer" with http URLs: http://www.jwplayer.com/
    private static final String type_allow_title_as_filename = ".+FOR_WEBSITES_FOR_WHICH_HTML_TITLE_TAG_CONTAINS_GOOD_FILENAME.+";
    private static final String default_Extension            = ".mp4";
    /* Connection stuff */
    private static final int    free_maxdownloads            = -1;
    private boolean             resumes                      = true;
    private int                 chunks                       = 0;
    private String              dllink                       = null;

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, AccountController.getInstance().getValidAccount(this.getHost()), false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        if (!link.isNameSet()) {
            /* Now lets find the url_slug as a fallback in case we cannot find the filename inside the html code. */
            String url_slug = null;
            final String[] urlparts = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/[^/]+/(.+)").getMatch(0).split("/");
            String url_id = null;
            for (String urlpart : urlparts) {
                if (urlpart.matches("\\d+")) {
                    url_id = urlpart;
                } else {
                    url_slug = urlpart;
                    break;
                }
            }
            if (url_slug != null) {
                url_slug = url_slug.replace(".html", "");
                if (url_id == null) {
                    /* In case we have an ID, it might be in the url_filename --> Find it */
                    /* First check if we find it at the beginning. */
                    url_id = new Regex(url_slug, "^(\\d+\\-).+").getMatch(0);
                    if (url_id == null) {
                        /* Secondly check if we find it at the end. */
                        url_id = new Regex(url_slug, ".+(\\-\\d+)$").getMatch(0);
                    }
                }
                if (url_id != null) {
                    /* Remove url_id from url_filename */
                    url_slug = url_slug.replace(url_id, "");
                }
            } else {
                url_slug = url_id;
            }
            /* Make it look nicer! */
            url_slug = url_slug.replace("-", " ");
            link.setName(url_slug + default_Extension);
        }
        br.setAllowedResponseCodes(new int[] { 410 });
        br.setFollowRedirects(true);
        if (link.getDownloadURL().contains("bigcamtube.com")) {
            br.setCookie("www.bigcamtube.com", "age_verify", "1");
            br.addAllowedResponseCodes(500);
        }
        if (account != null) {
            this.login(account, false);
        }
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">Sorry, we couldn't find")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = regexStandardTitleWithHost(this.getHost());
        if (title == null) {
            title = HTMLSearch.searchMetaTag(br, "og:title");
        }
        if (title == null) {
            /* Works e.g. for: boyfriendtv.com, ashemaletube.com, pornoxo.com */
            title = br.getRegex("<div id=\"maincolumn2\">\\s*?<h1>([^<>]*?)</h1>").getMatch(0);
        }
        if (title == null && link.getDownloadURL().matches(type_allow_title_as_filename)) {
            title = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            link.setFinalFileName(title + default_Extension);
        }
        getDllink();
        if (!inValidateDllink(dllink)) {
            logger.info("dllink: " + dllink);
            if (dllink.contains(".m3u8")) { // bigcamtube.com
                br.getPage(dllink);
                // Get file size with checkFFProbe and StreamInfo fails with HTTP error 501 Not Implemented
                return AvailableStatus.TRUE;
            }
        }
        /* 2022-11-21: Disabled this as their servers will return wrong results when checking multiple items in a short time. */
        final boolean checkFilesize = false;
        if (!inValidateDllink(dllink) && checkFilesize && !isDownload) {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(this.dllink);
                handleConnectionErrors(br2, con);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                final String ext = getExtensionFromMimeType(con);
                if (ext != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + ext));
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

    private void getDllink() throws Exception {
        /* Find correct js-source, then find dllink inside of it. */
        String jwplayer_source = null;
        final String[] scripts = br.getRegex("<script[^>]*?>(.*?)</script>").getColumn(0);
        for (final String script : scripts) {
            if (script.contains("jwplayer")) {
                dllink = searchDllinkInsideJWPLAYERSource(script);
                if (dllink != null) {
                    dllink = dllink.replace("\\", "");
                    jwplayer_source = script;
                    break;
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("<(?:source|video)[^<>]*? src=(?:'|\")([^<>'\"]+)(?:'|\")").getMatch(0);
        }
        if (jwplayer_source == null && dllink == null && !requiresAccount(br)) {
            /*
             * No player found --> Chances are high that there is no playable content --> Video offline
             * 
             * This can also be seen as a "last chance offline" errorhandling for websites for which the above offline-errorhandling doesn't
             * work!
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static String searchDllinkInsideJWPLAYERSource(final String jwplayer_source) {
        /* Source #1 */
        String dllink = new Regex(jwplayer_source, "('|\")file\\1:\\s*('|\")(http.*?)\\2").getMatch(2);
        if (inValidateDllink(dllink)) {
            /* E.g. worldsex.com */
            dllink = new Regex(jwplayer_source, "file[\t\n\r ]*?:[\t\n\r ]*?('|\")(http.*?)\\1").getMatch(1);
        }
        if (inValidateDllink(dllink)) {
            /*
             * E.g. kt_player + jwplayer (can also be KernelVideoSharingCom), example: xfig.net[dedicated plugin], cliplips.com)<br />
             * Important: Do not pickup the slash at the end!
             */
            dllink = new Regex(jwplayer_source, "var videoFile=\"(http[^<>\"]*?)/?\";").getMatch(0);
        }
        if (inValidateDllink(dllink)) {
            /* Check for multiple videoqualities --> Find highest quality */
            int maxquality = 0;
            String sources_source = new Regex(jwplayer_source, "(?:\")?sources(?:\")?\\s*?:\\s*?\\[(.*?)\\]").getMatch(0);
            if (sources_source != null) {
                sources_source = sources_source.replace("\\", "");
                final String[] qualities = new Regex(sources_source, "(file: \".*?)\n").getColumn(0);
                for (final String quality_info : qualities) {
                    final String p = new Regex(quality_info, "label:\"(\\d+)p").getMatch(0);
                    int pint = 0;
                    if (p != null) {
                        pint = Integer.parseInt(p);
                    }
                    if (pint > maxquality) {
                        maxquality = pint;
                        dllink = new Regex(quality_info, "file[\t\n\r ]*?:[\t\n\r ]*?\"(http[^<>\"]*?)\"").getMatch(0);
                    }
                }
            }
        }
        return dllink;
    }

    public static boolean inValidateDllink(final String dllink) {
        if (dllink == null) {
            return true;
        } else if (dllink.endsWith(".vtt")) {
            /* We picked up the subtitle url instead of the video downloadurl! */
            return true;
        } else {
            return false;
        }
    }

    private boolean requiresAccount(final Browser br) {
        if (br.containsHTML("(?i)>\\s*To watch this video please")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (requiresAccount(br)) {
            throw new AccountRequiredException();
        } else if (inValidateDllink(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) { // bigcamtube.com
            /* hls download */
            /* Access hls master. */
            br.getPage(dllink);
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
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
            handleConnectionErrors(br, dl.getConnection());
            dl.startDownload();
        }
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Don't validate cookies */
                        return false;
                    }
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + this.getHost() + "/login.php");
                final Form loginform = br.getFormbyProperty("name", "loginForm");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("rememberMe", "1");
                final Request req = br.createFormRequest(loginform);
                req.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.getPage(req);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                if ((Boolean) entries.get("SUCCESS") != Boolean.TRUE) {
                    throw new AccountInvalidException();
                }
                /* Double-check */
                br.getPage(entries.get("URL").toString());
                if (!isLoggedin(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    private String regexStandardTitleWithHost(final String host) {
        final String[] hostparts = host.split("\\.");
        final String host_relevant_part = hostparts[0];
        String site_title = br.getRegex(Pattern.compile("<title>([^<>\"]*?) \\- " + Pattern.quote(host) + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (site_title == null) {
            site_title = br.getRegex(Pattern.compile("<title>([^<>\"]*?) at " + Pattern.quote(host) + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (site_title == null) {
            site_title = br.getRegex(Pattern.compile("<title>([^<>\"]*?) at " + host_relevant_part + "</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        return site_title;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript5;
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
