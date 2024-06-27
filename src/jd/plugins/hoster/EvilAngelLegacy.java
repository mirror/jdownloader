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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.EvilangelComConfig;
import org.jdownloader.plugins.components.config.EvilangelComConfig.Quality;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "evilangel.legacy" }, urls = { "" })
@Deprecated
// 2021-09-01: TODO: Rewrite all plugins which still make use of this to to use EvilangelCore!
public class EvilAngelLegacy extends antiDDoSForHost {
    public EvilAngelLegacy(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("http://www.evilangel.com/en/join");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (cookieLoginOnly) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            prepBr.setCookiesExclusive(true);
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setCookie(host, "enterSite", "en");
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
    }

    @Override
    public String getAGBLink() {
        return "http://www.evilangel.com/en/terms";
    }

    private String               dllink                     = null;
    private boolean              server_issues              = false;
    public static final long     trust_cookie_age           = 300000l;
    private static final String  HTML_LOGGEDIN              = "id=\"headerLinkLogout\"";
    public static final String   LOGIN_PAGE                 = "https://www.evilangel.com/en/login/";
    private static final String  URL_EVILANGEL_FILM         = "https?://members\\.evilangel.com/[a-z]{2}/([A-Za-z0-9\\-_]+)/film/(\\d+)";
    private static final String  URL_EVILANGEL_FREE_TRAILER = "https?://(?:www\\.)?evilangel\\.com/[a-z]{2}/video/([A-Za-z0-9\\-]+)/(\\d+)";
    private static final String  URL_EVILANGELNETWORK_VIDEO = "https?://members\\.[^/]+/[a-z]{2}/video/([A-Za-z0-9\\-_]+)(?:/[A-Za-z0-9\\-_]+)?/(\\d+)";
    private static final boolean cookieLoginOnly            = true;

    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    private String getURLTitle(final DownloadLink link) {
        final String fileID = getFID(link);
        String url_filename;
        if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FILM)) {
            url_filename = new Regex(link.getPluginPatternMatcher(), URL_EVILANGEL_FILM).getMatch(0);
        } else if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FREE_TRAILER)) {
            url_filename = new Regex(link.getPluginPatternMatcher(), URL_EVILANGEL_FREE_TRAILER).getMatch(0);
        } else {
            url_filename = new Regex(link.getPluginPatternMatcher(), URL_EVILANGELNETWORK_VIDEO).getMatch(0);
        }
        url_filename = fileID + "_" + url_filename;
        return url_filename;
    }

    /**
     * NOTE: While making the plugin, the testaccount was banned temporarily and we didn't get new password/username from the user->Plugin
     * isn't 100% done yet! http://svn.jdownloader.org/issues/6793
     */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return this.requestFileInformation(link, account);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getURLTitle(link) + ".mp4");
        }
        this.dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        final String fileID = this.getFID(link);
        String filename = null;
        final String host = Browser.getHost(link.getPluginPatternMatcher(), true);
        if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FREE_TRAILER) && !host.contains("members.")) {
            /* Free (trailer) download */
            getPage(link.getPluginPatternMatcher());
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String jsonPlayer = br.getRegex("ScenePlayerOptions = (\\{.*?\\});window\\.").getMatch(0);
            String server = null;
            if (jsonPlayer != null) {
                try {
                    Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(jsonPlayer);
                    entries = (Map<String, Object>) entries.get("playerOptions");
                    server = (String) entries.get("host");
                    final String sceneTitle = (String) entries.get("sceneTitle");
                    if (!StringUtils.isEmpty(sceneTitle)) {
                        filename = fileID + "_" + sceneTitle;
                    }
                } catch (final Throwable e) {
                }
            }
            if (StringUtils.isEmpty(filename)) {
                filename = getURLTitle(link);
            }
            if (StringUtils.isEmpty(server)) {
                server = "https://trailers-evilangel.gammacdn.com";
            }
            this.dllink = getDllinkTrailer(this.br);
            if (!StringUtils.isEmpty(this.dllink)) {
                this.dllink = server + this.dllink;
                final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
                if (quality == null) {
                    filename += ".mp4";
                } else {
                    filename = filename + "-" + quality + ".mp4";
                }
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = brc.openHeadConnection(dllink);
                    if (this.looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            if (con.isContentDecoded()) {
                                link.setDownloadSize(con.getCompleteContentLength());
                            } else {
                                link.setVerifiedFileSize(con.getCompleteContentLength());
                            }
                        }
                        if (StringUtils.isEmpty(filename)) {
                            /* Fallback if everything else fails */
                            filename = Encoding.htmlDecode(getFileNameFromHeader(con));
                        }
                        link.setFinalFileName(filename);
                    } else {
                        server_issues = true;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        } else if (account != null) {
            loginEvilAngelNetwork(account, false, LOGIN_PAGE, HTML_LOGGEDIN);
            if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FILM)) {
                getPage(link.getPluginPatternMatcher());
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = getVideoTitle();
                if (filename == null) {
                    /* Fallback */
                    filename = this.getURLTitle(link);
                }
                filename = Encoding.htmlDecode(filename.trim());
                dllink = getDllink(this.br);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = "http://members.evilangel.com" + dllink;
                final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
                if (quality == null) {
                    filename += ".mp4";
                } else {
                    filename = filename + "-" + quality + ".mp4";
                }
            } else if (link.getPluginPatternMatcher().matches(URL_EVILANGELNETWORK_VIDEO)) {
                getPage(link.getPluginPatternMatcher());
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = getVideoTitle();
                if (filename == null) {
                    /* Fallback */
                    filename = this.getURLTitle(link);
                }
                filename = Encoding.htmlDecode(filename.trim());
                dllink = getDllink(this.br);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = "https://" + this.br.getHost(true) + dllink;
                final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
                if (quality == null) {
                    filename += ".mp4";
                } else {
                    filename = filename + "-" + quality + ".mp4";
                }
            } else {
                dllink = link.getPluginPatternMatcher();
            }
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            link.getLinkStatus().setStatusText("Links can only be checked and downloaded via account!");
            return AvailableStatus.UNCHECKABLE;
        }
        return AvailableStatus.TRUE;
    }

    private String getUserPreferredqualityStr() {
        final Quality quality = PluginJsonConfig.get(EvilangelComConfig.class).getPreferredQuality();
        switch (quality) {
        case Q160:
            return "160p";
        case Q240:
            return "240p";
        case Q360:
            return "360p";
        case Q480:
            return "480p";
        case Q540:
            return "540p";
        case Q720:
            return "720p";
        case Q1080:
            return "1080p";
        case Q2160:
            return "2160p";
        default:
            /* E.g. BEST */
            return null;
        }
    }

    public boolean isFreeDownloadable(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(URL_EVILANGEL_FREE_TRAILER)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null && !isFreeDownloadable(link)) {
            return false;
        } else {
            return super.canHandle(link, account);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (!isFreeDownloadable(link)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        handleDownload(link);
    }

    private String getVideoTitle() {
        String title = br.getRegex("<h1 class=\"title\">([^<>\"]*?)</h1>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<h1 class=\"h1_title\">([^<>\"]*?)</h1>").getMatch(0);
            if (title == null) {
                title = br.getRegex("<h2 class=\"h2_title\">([^<>\"]*?)</h2>").getMatch(0);
            }
        }
        return title;
    }

    /** Find preferred quality downloadurl */
    private String getDllink(final Browser br) {
        final String preferredQualityStr = this.getUserPreferredqualityStr();
        String dllink = null;
        if (preferredQualityStr == null) {
            logger.info("User has selected BEST quality");
        } else {
            logger.info("User has selected quality: " + preferredQualityStr);
            dllink = this.findDesiredQuality(br, preferredQualityStr);
            if (dllink != null) {
                logger.info("Found user preferred quality");
                return dllink;
            } else {
                logger.info("Failed to find user selected quality --> Fallback to BEST handling");
            }
        }
        if (dllink == null) {
            final String[] qualities = { "2160p", "1080p", "720p", "540p", "480p", "240p", "160p" };
            for (final String qualityStr : qualities) {
                dllink = this.findDesiredQuality(br, qualityStr);
                if (dllink != null) {
                    return dllink;
                }
            }
        }
        return null;
    }

    private String findDesiredQuality(final Browser br, final String qualityStr) {
        return br.getRegex("\"(/[^\"]*/download/\\d+/" + qualityStr + "[^\"]*)\"").getMatch(0);
    }

    /** Find highest quality trailer downloadurl (sometimes higher quality than what website player is using) */
    public static String getDllinkTrailer(final Browser br) {
        String dllink = null;
        final String[] qualities = { "2160p", "1080p", "720p", "540p", "480p", "240p", "160p" };
        for (final String quality : qualities) {
            // dllink = br.getRegex("file=\"(/[^<>\"]*?/trailers/[^<>\"]+" + quality + "\\.mp4)\"").getMatch(0);
            dllink = br.getRegex("file=\"(/[^<>\"]*?/trailers/[^<>\"]+\\.mp4)\" size=\"" + quality).getMatch(0);
            if (dllink != null) {
                break;
            }
        }
        return dllink;
    }

    /** Function can be used for all evilangel type of networks/websites. */
    public void loginEvilAngelNetwork(final Account account, final boolean verifyCookies, String getpage, final String html_loggedin) throws Exception {
        synchronized (account) {
            try {
                final String host_account = account.getHoster();
                final String url_main = "http://" + host_account + "/";
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (cookieLoginOnly && userCookies == null) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                if (host_account.equals("evilangelnetwork.com")) {
                    getpage = "https://www.evilangelnetwork.com/en/login";
                } else if (host_account.equalsIgnoreCase("evilangel.com")) {
                    getpage = "https://www.evilangel.com/en/login";
                } else {
                    /* getpage must have already been set via parameter */
                }
                if (userCookies != null) {
                    br.setCookies(userCookies);
                    getPage(br, getpage);
                    if (this.isLoggedIn(html_loggedin)) {
                        /* Cookie login successful */
                        logger.info("User cookie login successful");
                        return;
                    } else {
                        br.clearAll();
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookies != null) {
                    br.setCookies(host_account, cookies);
                    getPage(br, getpage);
                    if (this.isLoggedIn(html_loggedin)) {
                        /* Cookie login successful */
                        logger.info("Cookie login successful");
                        /* Update cookies */
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        if (userCookies != null) {
                            /* No login password given */
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            br.clearAll();
                        }
                    }
                }
                br.setFollowRedirects(true);
                getPage(br, getpage);
                if (br.containsHTML("(?i)>We are experiencing some problems\\!<")) {
                    final AccountInfo ai = new AccountInfo();
                    ai.setStatus("Your IP is banned. Please re-connect to get a new IP to be able to log-in!");
                    account.setAccountInfo(ai);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final Form login = br.getFormbyProperty("id", "loginForm");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final boolean fillTimeFalues = true;
                {
                    if (fillTimeFalues) {
                        final Date d = new Date();
                        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
                        final String date = sd.format(d);
                        sd = new SimpleDateFormat("k:mm");
                        final String time = sd.format(d);
                        final String timedatestring = date + " " + time;
                        br.setCookie(url_main, "mDateTime", Encoding.urlEncode(timedatestring));
                        br.setCookie(url_main, "mOffset", "2");
                        br.setCookie(url_main, "origin", "promo");
                        br.setCookie(url_main, "timestamp", Long.toString(System.currentTimeMillis()));
                    } else {
                    }
                }
                // login.setAction("/en/login");
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                if (login.containsHTML("g-recaptcha")) {
                    // recaptchav2
                    final DownloadLink orig = this.getDownloadLink();
                    try {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account Login!", getHost(), getHost(), true);
                        this.setDownloadLink(dummyLink);
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                            @Override
                            public String getSiteKey() {
                                return getSiteKey(login.getHtmlCode());
                            }
                        }.getToken();
                        login.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    } finally {
                        this.setDownloadLink(orig);
                    }
                }
                login.remove("submit");
                login.remove("rememberme");
                login.put("rememberme", "1");
                submitForm(login);
                if (br.containsHTML("(?i)>\\s*Your account is deactivated for abuse")) {
                    final AccountInfo ai = new AccountInfo();
                    ai.setStatus("Your account is deactivated for abuse. Please re-activate it to use it in JDownloader.");
                    account.setAccountInfo(ai);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your account is deactivated for abuse. Please re-activate it to use it in JDownloader.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getURL().contains("/reactivate")) {
                }
                if (!isLoggedIn(html_loggedin)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(host_account), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIn(String html_loggedin) {
        final boolean loggedIN_html = br.containsHTML(">Wrong username or password provided. Please try again\\\\.<") && (html_loggedin == null || br.containsHTML(html_loggedin));
        final boolean loggedINCookie = br.getCookie(br.getHost(), "autologin_userid", Cookies.NOTDELETEDPATTERN) != null;
        return loggedIN_html || loggedINCookie;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = account.getAccountInfo() != null ? account.getAccountInfo() : new AccountInfo();
        try {
            /* Prevent direct login to prevent login captcha whenever possible */
            loginEvilAngelNetwork(account, true, LOGIN_PAGE, null);
        } catch (final PluginException e) {
            throw e;
        }
        final String subscriptionStatus = PluginJSonUtils.getJson(br, "subscriptionStatus");
        if (br.containsHTML("(?i)>\\s*Your membership has expired") || br.getURL().contains("/reactivate") || "expired".equalsIgnoreCase(subscriptionStatus)) {
            /* 2018-04-25 */
            ai.setExpired(true);
            ai.setTrafficLeft(0);
            account.setAccountInfo(ai);
            account.setType(AccountType.FREE);
        } else {
            ai.setUnlimitedTraffic();
            account.setType(AccountType.PREMIUM);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        handleDownload(link);
    }

    private void handleDownload(final DownloadLink link) throws Exception {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* No captchas at all */
        return false;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return EvilangelComConfig.class;
    }
}