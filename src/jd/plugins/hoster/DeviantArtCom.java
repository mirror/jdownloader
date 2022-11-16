//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.DeviantArtComConfig;
import org.jdownloader.plugins.components.config.DeviantArtComConfig.DownloadMode;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/(?:[^/]+/)?art/[\\w\\-]+|https?://[\\w\\.\\-]*?\\.deviantart\\.com/status/\\d+|https?://[\\w\\.\\-]*?deviantartdecrypted\\.com/(?:[^/]+/)?journal/[\\w\\-]+" })
public class DeviantArtCom extends PluginForHost {
    private String       dllink                        = null;
    private final String INVALIDLINKS                  = "https?://[^/]+/(?:[^/]+/)?art/general";
    // private static final String DLLINK_REFRESH_NEEDED = "https?://(www\\.)?deviantart\\.com/download/.+";
    private final String TYPE_DOWNLOADALLOWED_HTML     = "(?i)class=\"text\">HTML download</span>";
    private final String TYPE_DOWNLOADFORBIDDEN_HTML   = "<div class=\"grf\\-indent\"";
    @Deprecated
    private final String TYPE_DOWNLOADFORBIDDEN_SWF    = "class=\"flashtime\"";
    private boolean      downloadHTML                  = false;
    private final String PATTERN_ART                   = "https?://[^/]+/(?:[^/]+/)?art/[^<>\"/]+";
    private final String LINKTYPE_JOURNAL              = "https?://[^/]+/(?:[^/]+/)?journal/[\\w\\-]+";
    @Deprecated
    private final String LINKTYPE_STATUS               = "https?://[^/]+/status/\\d+";
    private final String TYPE_BLOG_OFFLINE             = "https?://[^/]+/(?:[^/]+/)?blog/.+";
    private final String PROPERTY_OFFICIAL_DOWNLOADURL = "official_downloadurl";

    /**
     * @author raztoki
     */
    @SuppressWarnings("deprecation")
    public DeviantArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/join/");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/about/policy/service/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getPluginPatternMatcher().replace("deviantartdecrypted.com/", "deviantart.com/"));
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 1500);
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        this.dllink = null;
        if (link.getPluginPatternMatcher().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        prepBR(this.br);
        br.setFollowRedirects(true);
        if (account != null) {
            login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("/error\\-title\\-oops\\.png\\)") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(",target: \\'motionbooks/")) {
            logger.info("Motionbooks are not supported (yet)");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Redirects can lead to unsupported/offline links/linktypes */
        if (br.getURL().matches(TYPE_BLOG_OFFLINE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = HTMLSearch.searchMetaTag(br, "og:title");
        if (title != null) {
            title = title.replaceAll("(?i) on deviantart$", "");
        }
        if (link.getPluginPatternMatcher().matches(LINKTYPE_STATUS)) {
            final String fid = new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
            if (fid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            title = Encoding.htmlDecode(title.trim());
        }
        /* TODO: Test this with video- and animation content */
        String ext = null;
        boolean setOfficialDownloadFilesize = false;
        final String officialDownloadFilesize = br.getRegex("(?i)>\\s*Image size\\s*</div><div [^>]*>\\d+x\\d+px\\s*(\\d+[^>]+)</div>").getMatch(0);
        // final boolean accountNeededForOfficialDownload = br.containsHTML("(?i)Log in to download");
        String officialDownloadurl = br.getRegex("data-hook=\"download_button\"[^>]*href=\"(https?://[^\"]+)").getMatch(0);
        if (officialDownloadurl != null) {
            officialDownloadurl = officialDownloadurl.replace("&amp;", "&");
        }
        link.setProperty(PROPERTY_OFFICIAL_DOWNLOADURL, officialDownloadurl);
        final String displayedImageURL = HTMLSearch.searchMetaTag(br, "og:image");
        final DeviantArtComConfig cfg = PluginJsonConfig.get(DeviantArtComConfig.class);
        final DownloadMode mode = cfg.getDownloadMode();
        /* Check if either user wants to download the html code or if we have a linktype which needs this. */
        if (mode == DownloadMode.HTML || link.getPluginPatternMatcher().matches(LINKTYPE_JOURNAL) || link.getPluginPatternMatcher().matches(LINKTYPE_STATUS)) {
            downloadHTML = true;
            dllink = br.getURL();
            ext = ".html";
        } else if (officialDownloadurl != null) {
            this.dllink = officialDownloadurl;
            setOfficialDownloadFilesize = true;
        } else if (displayedImageURL != null) {
            this.dllink = displayedImageURL;
        } else if (br.containsHTML(TYPE_DOWNLOADFORBIDDEN_SWF)) {
            String url_swf_sandbox = br.getRegex("class=\"flashtime\" src=\"(https?://sandbox\\.deviantart\\.com[^<>\"]*?)\"").getMatch(0);
            if (url_swf_sandbox == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Fix that ... */
            url_swf_sandbox = url_swf_sandbox.replace("?", "/?");
            br.getPage(url_swf_sandbox);
            this.dllink = br.getRegex("<param name=\"movie\" value=\"(https?://[^<>\"]*?\\.swf)\"").getMatch(0);
            if (this.dllink == null) {
                this.dllink = br.getRegex("id=\"sandboxembed\" src=\"(http[^<>\"]+\\.swf)\"").getMatch(0);
            }
            ext = ".swf";
        } else if (br.containsHTML(TYPE_DOWNLOADALLOWED_HTML) || br.containsHTML(TYPE_DOWNLOADFORBIDDEN_HTML)) {
            downloadHTML = true;
            dllink = br.getURL();
            ext = ".html";
        } else if (isAccountRequiredUploaderDecision(br)) {
            /* Account needed to view/download */
            ext = ".html";
        } else {
            /* Workaround for invalid domain(s) e.g. "laur-.deviantart.com" */
            final String cookie = this.br.getCookie(null, "userinfo", Cookies.NOTDELETEDPATTERN);
            if (cookie != null) {
                br.setCookie(this.getHost(), "userinfo", cookie);
            }
        }
        link.setVerifiedFileSize(-1); // reset this every time as user can change settings
        if (downloadHTML) {
            try {
                link.setDownloadSize(br.getRequest().getHtmlCode().getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
        } else if (officialDownloadFilesize != null && (setOfficialDownloadFilesize || mode == DownloadMode.OFFICIAL_DOWNLOAD_ONLY)) {
            /*
             * Set filesize of official download if: User wants official download and it is available and/or if user wants official
             * downloads only (set filesize even if official downloadurl was not found).
             */
            link.setDownloadSize(SizeFormatter.getSize(officialDownloadFilesize.replace(",", "")));
        } else if (mode == DownloadMode.OFFICIAL_DOWNLOAD_ELSE_PREVIEW && cfg.isFastLinkcheckForSingleItems() == false && !isDownload && !StringUtils.isEmpty(this.dllink)) {
            final Browser br2 = br.cloneBrowser();
            /* Workaround for old downloadcore bug that can lead to incomplete files */
            br2.getHeaders().put("Accept-Encoding", "identity");
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(this.dllink);
                if (!looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String mimeTypeExt = getExtensionFromMimeType(con.getRequest().getResponseHeader("Content-Type"));
                if (mimeTypeExt != null) {
                    ext = "." + mimeTypeExt;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        if (ext == null) {
            ext = Plugin.getFileNameExtensionFromURL(this.dllink);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            title = this.correctOrApplyFileNameExtension(title, ext);
            link.setFinalFileName(title);
        } else if (!StringUtils.isEmpty(this.dllink)) {
            final String filenameFromURL = Plugin.getFileNameFromURL(new URL(this.dllink));
            if (filenameFromURL != null) {
                link.setName(filenameFromURL);
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean isAccountRequiredForOfficialDownload(final Browser br) {
        if (br.containsHTML("(?i)aria-label=\"Log in to download\"")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAccountRequired(final Browser br) {
        if (isAccountRequiredUploaderDecision(br) || isAccountRequiredMatureContent(br)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAccountRequiredUploaderDecision(final Browser br) {
        if (br.containsHTML("(?i)has limited the viewing of this artwork\\s*<")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAccountRequiredMatureContent(final Browser br) {
        if (br.containsHTML("(?i)>\\s*This content is intended for mature audiences")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        final String officialDownloadurl = link.getStringProperty(PROPERTY_OFFICIAL_DOWNLOADURL);
        final DeviantArtComConfig cfg = PluginJsonConfig.get(DeviantArtComConfig.class);
        final DownloadMode mode = cfg.getDownloadMode();
        if (mode == DownloadMode.OFFICIAL_DOWNLOAD_ONLY && officialDownloadurl == null) {
            /* User only wants to download items with official download option available but it is not available in this case. */
            if (!this.isAccountRequiredForOfficialDownload(br)) {
                /* Looks like official download is not available at all for this item */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Official download not available");
            } else if (account == null) {
                /* Account is required to be able to use official download option. */
                throw new AccountRequiredException();
            } else {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Official download broken or login issue");
            }
        }
        if (this.isAccountRequired(br)) {
            throw new AccountRequiredException();
        } else if (StringUtils.isEmpty(this.dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Workaround for old downloadcore bug that can lead to incomplete files */
        /* Disable chunks as we only download pictures or small files */
        br.getHeaders().put("Accept-Encoding", "identity");
        boolean resume = true;
        if (downloadHTML) {
            link.setVerifiedFileSize(-1);
            resume = false;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, this.dllink, resume, 1);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            handleServerErrors(dl.getConnection());
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter con) {
        if (super.looksLikeDownloadableContent(con)) {
            return true;
        } else if (downloadHTML && con.getContentType().contains("html")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    private void handleServerErrors(final URLConnectionAdapter con) throws PluginException {
        /* Happens sometimes - download should work fine later though. */
        if (con.getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 1 * 60 * 1000l);
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        account.setType(AccountType.FREE);
        /**
         * Try to get unique username even if users use cookie login as they can theoretically enter whatever they want into username field.
         */
        final Cookies userCookies = account.loadUserCookies();
        String realUsername = getUsernameFromCookies(br);
        if (userCookies != null && !userCookies.isEmpty()) {
            if (!StringUtils.isEmpty(realUsername)) {
                account.setUser(realUsername);
            } else {
                logger.warning("Failed to find real username inside cookies");
            }
        }
        return ai;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                /* 2022-10-31: Normal login process won't work due to their anti DDoS protection -> Only cookie login is possible */
                final boolean allowCookieLoginOnly = true;
                if (userCookies == null && allowCookieLoginOnly) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                if (userCookies != null) {
                    br.setCookies(this.getHost(), userCookies);
                    if (!force) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage("https://www." + this.getHost());
                    if (this.isLoggedIN(br)) {
                        logger.info("User cookie login successful");
                        return;
                    } else {
                        logger.info("User cookie login failed");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                } else if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Do not validate cookies */
                        return;
                    }
                    br.getPage("https://www. " + this.getHost());
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                br.getHeaders().put("Referer", "https://www." + this.getHost());
                br.getPage("https://www." + this.getHost() + "/users/login"); // Not allowed to go directly to /users/login/
                if (br.containsHTML("Please confirm you are human")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                final boolean allowCaptcha = false;
                if (allowCaptcha && (br.containsHTML("Please confirm you are human") || (br.containsHTML("px-blocked") && br.containsHTML("g-recaptcha")))) {
                    // disabled because perimeterx code is incomplete
                    final DownloadLink dummyLink = new DownloadLink(this, "Account Login", getHost(), getHost(), true);
                    final DownloadLink odl = this.getDownloadLink();
                    this.setDownloadLink(dummyLink);
                    final CaptchaHelperHostPluginRecaptchaV2 captcha = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Lcj-R8TAAAAABs3FrRPuQhLMbp5QrHsHufzLf7b");
                    if (odl != null) {
                        this.setDownloadLink(odl);
                    }
                    final String uuid = new Regex(br.getURL(), "uuid=(.*?)($|&)").getMatch(0);
                    String vid = new Regex(br.getURL(), "vid=(.*?)($|&)").getMatch(0);
                    if (StringUtils.isEmpty(vid)) {
                        vid = "null";
                    }
                    br.setCookie(getHost(), "_pxCaptcha", URLEncoder.encode(captcha.getToken(), "UTF-8") + ":" + uuid + ":" + vid);
                    br.getPage("https://www.deviantart.com/users/login");
                }
                final Form loginform = br.getFormbyActionRegex("(?i).*do/signin");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "on");
                br.submitForm(loginform);
                if (!isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final AccountInvalidException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        final String username = getUsernameFromCookies(br);
        if (!StringUtils.isEmpty(username) && br.containsHTML("data-hook=\"user_link\" data-username=\"" + Pattern.quote(username))) {
            return true;
        } else if (br.containsHTML("/users/logout")) {
            return true;
        } else {
            return false;
        }
    }

    private String getUsernameFromCookies(final Browser br) {
        String userinfoCookie = br.getCookie(br.getHost(), "userinfo", Cookies.NOTDELETEDPATTERN);
        if (userinfoCookie != null) {
            userinfoCookie = Encoding.htmlDecode(userinfoCookie);
            return PluginJSonUtils.getJson(userinfoCookie, "username");
        }
        return null;
    }

    public static Browser prepBR(final Browser br) {
        /* Needed to view mature content */
        br.setCookie("deviantart.com", "agegate_state", "1");
        return br;
    }

    @Override
    public String getDescription() {
        return "JDownloader's Deviantart Plugin helps downloading data from deviantart.com.";
    }

    @Override
    public Class<? extends DeviantArtComConfig> getConfigInterface() {
        return DeviantArtComConfig.class;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}