//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "transload.me" }, urls = { "" })
public class TransloadMe extends PluginForHost {
    private static final String          API_BASE                     = "http://transload.me/api/";
    private static final String          WEB_BASE                     = "http://transload.me";
    private static final String          NICE_HOST                    = "transload.me";
    private static final String          NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String          NORESUME                     = NICE_HOSTproperty + "NORESUME";
    /* Connection limits */
    private static final boolean         ACCOUNT_PREMIUM_RESUME       = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int             ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    private static AtomicBoolean         useApi                       = new AtomicBoolean(true);
    private static Object                LOCK                         = new Object();
    private static MultiHosterManagement mhm                          = new MultiHosterManagement("transload.me");
    private Account                      currAcc                      = null;
    private DownloadLink                 currDownloadLink             = null;

    public TransloadMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://en.transload.me/?p=register");
    }

    @Override
    public String getAGBLink() {
        return "http://en.transload.me/?p=login&redir=helpdesk";
    }

    private Browser prepBrowser(final Browser br) {
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader " + getVersion());
        br.setCookie(getHost(), "lang", "en");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBrowser(br);
        setConstants(account, link);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            mhm.runCheck(currAcc, currDownloadLink);
            if (useApi.get()) {
                dllink = generateDownloadlinkAPI();
            } else {
                loginWebsite(false);
                dllink = generateDownloadlinkWebsite();
            }
            if (dllink == null) {
                /* Should never happen */
                mhm.handleErrorGeneric(currAcc, currDownloadLink, "dllinknull", 30, 2 * 60 * 1000l);
            }
        }
        handleDL(account, link, dllink);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        if (link.getBooleanProperty(NORESUME, false)) {
            resume = false;
            link.setProperty(NORESUME, Boolean.valueOf(false));
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                handleErrorsAPI(br);
                mhm.handleErrorGeneric(currAcc, currDownloadLink, "unknowndlerror", 5, 2 * 60 * 1000l);
            }
            dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw e;
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final AccountInfo ai;
        if (useApi.get()) {
            ai = fetchAccountInfoAPI();
        } else {
            loginWebsite(false);
            ai = fetchAccountInfoWebsite();
        }
        return ai;
    }

    private String generateDownloadlinkAPI() throws Exception {
        prepBrowser(br);
        getPage("action=getdirectlink&link=" + Encoding.urlEncode(currDownloadLink.getDownloadURL()));
        final String dllink = PluginJSonUtils.getJsonValue(br, "link");
        return dllink;
    }

    private String generateDownloadlinkWebsite() throws Exception {
        String dllink = null;
        getPage(WEB_BASE + "/download2.php?my_url=" + Encoding.urlEncode(currDownloadLink.getDownloadURL()));
        dllink = br.getRegex("<a href=\"(http[^<>\"]+)\"").getMatch(0);
        if (dllink == null) {
            handleErrorsWebsite(br);
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    public AccountInfo fetchAccountInfoAPI() throws Exception {
        final AccountInfo ai = new AccountInfo();
        prepBrowser(br);
        br.setFollowRedirects(true);
        getPage("action=getaccountdetails");
        /* Balance left in USD */
        final String balance = PluginJSonUtils.getJsonValue(br, "balance");
        final String reg_date = PluginJSonUtils.getJsonValue(br, "reg_date");
        if (reg_date != null) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(reg_date, "yyyy-MM-dd", Locale.ENGLISH));
        }
        if (balance == null || Double.parseDouble(balance) <= 0) {
            currAcc.setType(AccountType.FREE);
            throwZeroBalance();
        } else {
            currAcc.setType(AccountType.PREMIUM);
            currAcc.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium account balance " + balance + " USD");
            /*
             * Set unlimited traffic as each filehost costs a different amount of money per GB see:
             * http://en.transload.me/index.php?p=statistic
             */
            ai.setUnlimitedTraffic();
        }
        currAcc.setValid(true);
        getPage("action=getsupporthost");
        final String[] domains = br.getRegex("\"([^<>\"]+)\"").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(domains));
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    @SuppressWarnings("deprecation")
    public AccountInfo fetchAccountInfoWebsite() throws Exception {
        final AccountInfo ai = new AccountInfo();
        final boolean premium = br.containsHTML("<div[^>]*>\\s*Status\\s*(?:<[^>]+>\\s*)*Premium\\s*<");
        /* Balance left in USD */
        final String balance = br.getRegex("<div[^>]*?class=\"number\">([^<>\"]+)<sup><i[^>]*?class=\"icon\\-dollar\">").getMatch(0);
        if (!premium && (balance == null || Double.parseDouble(balance) <= 0)) {
            currAcc.setType(AccountType.FREE);
            throwZeroBalance();
        } else {
            currAcc.setType(AccountType.PREMIUM);
            if (balance == null || Double.parseDouble(balance) <= 0) {
                throwZeroBalance();
            }
            currAcc.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium Account - balance " + balance + " USD");
            /*
             * Set unlimited traffic as each filehost costs a different amount of money per GB see:
             * http://en.transload.me/index.php?p=statistic
             */
            ai.setUnlimitedTraffic();
        }
        currAcc.setValid(true);
        getPage("/en/?p=download");
        final String[] domains = br.getRegex("<td[^>]*?><img src=\"/index/host/small/[A-Za-z0-9]+\\.png\"[^>]*?title=\"([^<>\"]+)\">").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(domains));
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void throwZeroBalance() throws PluginException {
        synchronized (LOCK) {
            final AccountInfo ai = currAcc.getAccountInfo();
            ai.setTrafficLeft(0);
            ai.setProperty("multiHostSupport", Property.NULL);
            currAcc.setAccountInfo(ai);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account balance 0.00 USD!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private void loginWebsite(final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                final Cookies cookies = currAcc.loadCookies("");
                if (cookies != null && !force) {
                    prepBrowser(br);
                    br.setCookies(getHost(), cookies);
                    br.getPage(WEB_BASE + "/main/en");
                    if (isLoggedInWebsite()) {
                        return;
                    }
                }
                br = prepBrowser(new Browser());
                br.getPage(WEB_BASE + "/main/en/");
                br.getPage("/en/?p=login");
                final Form login = br.getFormbyProperty("id", "login-form");
                final DownloadLink dlinkbefore = getDownloadLink();
                if (dlinkbefore == null) {
                    setDownloadLink(new DownloadLink(this, "Account", getHost(), WEB_BASE, true));
                }
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                    @Override
                    public String getSiteKey() {
                        return getSiteKey(login.getHtmlCode());
                    };
                }.getToken();
                if (dlinkbefore != null) {
                    setDownloadLink(dlinkbefore);
                }
                login.put("login", Encoding.urlEncode(currAcc.getUser()));
                login.put("password", Encoding.urlEncode(currAcc.getPass()));
                login.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.submitForm(login);
                if (!isLoggedInWebsite()) {
                    if (System.getProperty("user.language").equals("de")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                currAcc.saveCookies(br.getCookies(currAcc.getHoster()), "");
                handleErrorsWebsite(br);
            } catch (final PluginException e) {
                currAcc.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedInWebsite() {
        return br.containsHTML("action=logout");
    }

    private void getPage(final String input) throws Exception {
        String accesslink;
        if (useApi.get()) {
            accesslink = API_BASE + "?username=" + Encoding.urlEncode(currAcc.getUser()) + "&password=" + Encoding.urlEncode(currAcc.getPass());
            accesslink += "&" + input;
            // accesslink += "&client_id=jdownloader";
            br.getPage(accesslink);
            handleErrorsAPI(br);
        } else {
            accesslink = input;
            br.getPage(accesslink);
            // call error handling within methods.
        }
    }

    private int updatestatuscodeAPI() {
        final String errorcode = PluginJSonUtils.getJsonValue(br, "error");
        if (errorcode != null && errorcode.matches("\\d+")) {
            return Integer.parseInt(errorcode);
        }
        return 0;
    }

    /* Please do not remove this function - future usage!! */
    private void handleErrorsAPI(final Browser br) throws Exception {
        final int statuscode = updatestatuscodeAPI();
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                // "error": "0" - the Request succeeds, if the code is 0, then you can process the result
                break;
            case 1:
                // "error": "1" - the File is not found or has been deleted.
                /* 2017-10-12: Do not trust this API response! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error:1", 60 * 60 * 1000l);
            case 2:
                // "error": "2" - file Sharing is not supported.
                // should be mh wide
                statusMessage = "Unsupported host";
                mhm.handleErrorGeneric(null, currDownloadLink, "unsupported_host", 5, 5 * 60 * 1000l);
            case 3:
                // "error": "3" - system error Occurred while processing, please try again later.
                statusMessage = "Temporary error occured";
                mhm.handleErrorGeneric(currAcc, currDownloadLink, "temporary_error", 5, 2 * 60 * 1000l);
            case 4:
                // "error": "4" - On account of insufficient funds, replenish your balance.
                // update account scraping
                currAcc.setAccountInfo(fetchAccountInfoAPI());
                mhm.handleErrorGeneric(currAcc, currDownloadLink, "credits", 2, 10 * 60 * 1000l);
            case 5:
                // "error": "5" is Used or password is incorrect.
                if (System.getProperty("user.language").equals("de")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 6:
                // "error": "6" - Invalid method request.
                statusMessage = "Invalid API request - this should never happen!";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 7:
                // "error": "7" - You asked for too many links, wait a few minutes, or refill your account. (This occurs when the user has
                // generated a lot of links, though lacking means for downloading, you need to wait a few minutes before clearing balance)
                // so no more links for the account?
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
            case 8:
                // disabled api, switch to webmode
                useApi.set(false);
                throw new AccountUnavailableException("API is disabled", 500l);
            default:
                mhm.handleErrorGeneric(currAcc, currDownloadLink, "unknown_error_state", 50, 2 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    private void handleErrorsWebsite(final Browser br) throws PluginException {
        // no balance
        if (br.containsHTML("On account of insufficient funds, replenish your balance\\.\\s*<")) {
            throwZeroBalance();
        }
        /* TODO */
        // the rest
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}