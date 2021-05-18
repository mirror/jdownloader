//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kink.com" }, urls = { "https?://(?:www\\.)?kink.com/shoot/(\\d+)" })
public class KinkCom extends PluginForHost {
    public KinkCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.kink.com/join/kink");
    }
    /* DEV NOTES */
    // Tags: Porn plugin
    // protocol: no https
    // other:

    /* Connection stuff */
    private static final boolean resume        = true;
    private static final int     maxchunks     = 0;
    private static final int     maxdownloads  = -1;
    private String               dllink        = null;
    private boolean              server_issues = false;

    @Override
    public String getAGBLink() {
        return "https://kink.zendesk.com/hc/en-us/articles/360004660854-Kink-com-Terms-of-Use";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, acc);
    }

    private String getDirectlinkProperty(final DownloadLink link, final Account account) {
        if (account != null) {
            return "directlink_account";
        } else {
            return "directlink";
        }
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws IOException, PluginException {
        if (checkDirectLink(link, account) != null) {
            logger.info("Availablecheck via directurl complete");
            return AvailableStatus.TRUE;
        }
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        if (account != null) {
            try {
                this.login(account, false);
            } catch (final Throwable ignore) {
                /* This should never happen */
                logger.warning("Error in account login");
            }
        }
        dllink = null;
        server_issues = false;
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = this.getFID(link);
        } else {
            filename = this.getFID(link) + "_" + filename;
        }
        if (account != null) {
            /* Look for "official" downloadlinks --> Find highest quality */
            int qualityMax = 0;
            final String[][] dlinfos = br.getRegex("download=\"(https?://[^\"]+)\">\\s*(\\d+)\\s*<span").getMatches();
            for (final String[] dlinfo : dlinfos) {
                final int qualityTmp = Integer.parseInt(dlinfo[1]);
                if (qualityTmp > qualityMax) {
                    qualityMax = qualityTmp;
                    String url = dlinfo[0];
                    if (Encoding.isHtmlEntityCoded(url)) {
                        url = Encoding.htmlDecode(url);
                    }
                    this.dllink = url;
                }
            }
            logger.info("Chosen premium download quality: " + qualityMax);
        } else {
            /* Download trailer */
            dllink = br.getRegex("data\\-type=\"trailer\\-src\" data\\-url=\"(https?[^\"]+)\"").getMatch(0);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (!filename.endsWith(".mp4")) {
            filename += ".mp4";
        }
        link.setName(filename);
        if (!StringUtils.isEmpty(dllink)) {
            link.setProperty(this.getDirectlinkProperty(link, account), this.dllink);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
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

    private String checkDirectLink(final DownloadLink link, final Account account) {
        String dllink = link.getStringProperty(this.getDirectlinkProperty(link, account));
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (!this.attemptStoredDownloadurlDownload(link, account)) {
            requestFileInformation(link, account);
            if (server_issues) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            } else if (StringUtils.isEmpty(dllink)) {
                /* Display premiumonly message in this case */
                logger.info("Failed to download trailer");
                throw new AccountRequiredException();
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
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
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to file");
            }
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String url = link.getStringProperty(this.getDirectlinkProperty(link, account));
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private boolean login(final Account account, final boolean force) throws IOException, InterruptedException, PluginException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                br.setAllowedResponseCodes(new int[] { 401 });
                /* 2021-05-18: Website login doesn't work (yet) thus we only allow cookie login for now. */
                final boolean enforceCookieLogin = true;
                final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass());
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    if (checkAndSaveCookies(cookies, account)) {
                        return true;
                    }
                }
                if (userCookies != null) {
                    if (checkAndSaveCookies(userCookies, account)) {
                        return true;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (enforceCookieLogin) {
                    showCookieLoginInformation();
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login required", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                logger.info("Performing full login");
                br.setCookie(this.getHost(), "ktvc", "0");
                br.setCookie(this.getHost(), "privyOptIn", "false");
                br.setCookie(this.getHost(), "CookieControl", "DEVTEST_MAYBE_IRRELEVANT");
                br.getPage("https://www." + this.getHost() + "/login");
                final Form loginform = br.getFormbyProperty("name", "login");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* Add some browser fingerprinting magic */
                loginform.put("pf", Encoding.urlEncode(getPFValue(this.br, loginform)));
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                if (loginform.containsHTML("phone") && !loginform.hasInputFieldByName("phone")) {
                    loginform.put("phone", "");
                }
                /* Makes the cookies last for 30 days */
                loginform.put("remember", "on");
                if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(loginform)) {
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                br.submitForm(loginform);
                if (!isLoggedin()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    private boolean checkAndSaveCookies(final Cookies cookies, final Account account) throws IOException {
        logger.info("Attempting cookie login");
        this.br.setCookies(this.getHost(), cookies);
        br.getPage("https://www." + this.getHost() + "/my/billing");
        if (this.isLoggedin()) {
            logger.info("Cookie login successful");
            /* Refresh cookie timestamp */
            account.saveCookies(this.br.getCookies(this.getHost()), "");
            return true;
        } else {
            logger.info("Cookie login failed");
            return false;
        }
    }

    /** Returns special browser fingerprinting value required for first login. */
    private String getPFValue(final Browser br, final Form loginform) throws PluginException {
        final InputField csrf = loginform.getInputField("_csrf");
        if (csrf == null || csrf.getValue() == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String pf = "DEVTEST_DUMMY" + csrf + "%22%7D";
        return Encoding.Base64Encode(pf);
    }

    private boolean isLoggedin() {
        return br.containsHTML("/logout\"");
    }

    private Thread showCookieLoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String help_article_url = "https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions";
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Kink.com - Login";
                        message += "Hallo liebe(r) Kink.com NutzerIn\r\n";
                        message += "Um deinen Kink.com Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "Folge der Anleitung im Hilfe-Artikel:\r\n";
                        message += help_article_url;
                    } else {
                        title = "Kink.com - Login";
                        message += "Hello dear Kink.com user\r\n";
                        message += "In order to use an account of this service in JDownloader, you need to follow these instructions:\r\n";
                        message += help_article_url;
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(help_article_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!br.getURL().contains("/my/billing")) {
            /* E.g. first full website login */
            br.getPage("/my/billing");
        }
        long highestExpireTimestamp = 0;
        /* User can own multiple premium packages --> Use the expire date that is farthest away to set in JD! */
        final String[] possibleExpireDates = br.getRegex("([A-Z][a-z]{2} \\d{2}, \\d{4} \\d{2}:\\d{2} [A-Z]{3})").getColumn(0);
        for (final String possibleExpireDate : possibleExpireDates) {
            final long expireTimestampTmp = TimeFormatter.getMilliSeconds(possibleExpireDate, "MMM dd, yyyy HH:mm ZZ", Locale.ENGLISH);
            if (expireTimestampTmp > highestExpireTimestamp) {
                highestExpireTimestamp = expireTimestampTmp;
            }
        }
        /*
         * Always set expire-date. Free/expired premium accounts are unsupported and will get displayed as expired automatically --> Do NOT
         * accept those!
         */
        ai.setValidUntil(highestExpireTimestamp);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        /* Try to let user know when login session will expire */
        final Cookies allCookies = br.getCookies(br.getHost());
        final Cookie cookie = allCookies.get("kinky.sess");
        if (cookie != null) {
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            final String sessionExpireDateFormatted = formatter.format(new Date(cookie.getExpireDate() * 1000));
            ai.setStatus("Sess valid until: " + sessionExpireDateFormatted);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxdownloads;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* Only account login can have captchas */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxdownloads;
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
