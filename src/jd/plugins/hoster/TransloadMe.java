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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "transload.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" })
public class TransloadMe extends PluginForHost {

    private static final String                            API_BASE                     = "http://transload.me/api/";
    private static final String                            NICE_HOST                    = "transload.me";
    private static final String                            NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NORESUME                     = NICE_HOSTproperty + "NORESUME";

    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    private final boolean                                  USE_API                      = true;

    private static Object                                  LOCK                         = new Object();
    private int                                            statuscode                   = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    public TransloadMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://en.transload.me/?p=register");
    }

    @Override
    public String getAGBLink() {
        return "http://en.transload.me/?p=login&redir=helpdesk";
    }

    private Browser newBrowser(final Browser br) {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
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
        newBrowser(this.br);
        setConstants(account, link);

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        login(account, false);
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null || !dllink.startsWith("http")) {
            dllink = generateDownloadlink(link);
            if (dllink == null) {
                /* Should never happen */
                handleErrorRetries("dllinknull", 30, 2 * 60 * 1000l);
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
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                updatestatuscode();
                handleErrors(this.br);
                handleErrorRetries("unknowndlerror", 5, 2 * 60 * 1000l);
            }
            this.dl.startDownload();
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
        login(account, true);
        final AccountInfo ai;
        if (this.USE_API) {
            ai = fetchAccountInfoAPI(account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
        return ai;
    }

    private String generateDownloadlink(final DownloadLink link) throws IOException, PluginException {
        final String dllink;
        if (this.USE_API) {
            dllink = generateDownloadlinkAPI(link);
        } else {
            dllink = generateDownloadlinkWebsite(link);
        }
        return dllink;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            if (this.USE_API) {
                loginAPI(account, force);
            } else {
                loginWebsite(account, false);
            }
        }
    }

    /* Please do not remove this function - future usage!! */
    private void updatestatuscode() {
        if (this.USE_API) {
            updatestatuscodeAPI();
        } else {
            updatestatuscodeWebsite();
        }
    }

    /* Please do not remove this function - future usage!! */
    private void handleErrors(final Browser br) throws PluginException {
        if (this.USE_API) {
            handleErrorsAPI(br);
        } else {
            handleErrorsWebsite(br);
        }
    }

    private String generateDownloadlinkAPI(final DownloadLink link) throws IOException, PluginException {
        String dllink = null;
        this.getPageSafe("action=getdirectlink&link=" + Encoding.urlEncode(link.getDownloadURL()));
        dllink = PluginJSonUtils.getJsonValue(this.br, "link");
        return dllink;
    }

    private String generateDownloadlinkWebsite(final DownloadLink link) throws IOException, PluginException {
        String dllink = null;
        this.getPageSafe("http://" + this.getHost() + "/download2.php?my_url=" + Encoding.urlEncode(link.getDownloadURL()));
        dllink = this.br.getRegex("<a href=\"(http[^<>\"]+)\"").getMatch(0);
        return dllink;
    }

    @SuppressWarnings("deprecation")
    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        setConstants(account, null);
        final AccountInfo ai = new AccountInfo();
        /* Balance left in USD */
        final String balance = PluginJSonUtils.getJsonValue(this.br, "balance");
        final String reg_date = PluginJSonUtils.getJsonValue(this.br, "reg_date");
        if (reg_date != null) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(reg_date, "yyyy-MM-dd", Locale.ENGLISH));
        }
        /*
         * Free users = They have no package (==no balance left) --> Accept them but set zero traffic left.
         */
        if (balance == null || Double.parseDouble(balance) <= 0) {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /* Free accounts have no traffic - set this so they will not be used (accidently) but still accept them. */
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium account balance " + balance + " USD");
            /*
             * Set unlimited traffic as each filehost costs a different amount of money per GB see:
             * http://en.transload.me/index.php?p=statistic
             */
            ai.setUnlimitedTraffic();
        }
        account.setValid(true);
        this.getPageSafe("action=getsupporthost");
        final String[] domains = this.br.getRegex("\"([^<>\"]+)\"").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(domains));
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    @SuppressWarnings("deprecation")
    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        setConstants(account, null);
        final AccountInfo ai = new AccountInfo();
        /* Balance left in USD */
        final String balance = this.br.getRegex("<div[^>]*?class=\"number\">([^<>\"]+)<sup><i[^>]*?class=\"icon\\-dollar\">").getMatch(0);
        /*
         * Free users = They have no package (==no balance left) --> Accept them but set zero traffic left.
         */
        if (balance == null || Double.parseDouble(balance) <= 0) {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /* Free accounts have no traffic - set this so they will not be used (accidently) but still accept them. */
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            ai.setStatus("Premium account balance " + balance + " USD");
            /*
             * Set unlimited traffic as each filehost costs a different amount of money per GB see:
             * http://en.transload.me/index.php?p=statistic
             */
            ai.setUnlimitedTraffic();
        }
        account.setValid(true);
        this.getPageSafe("/?p=download");
        final String[] domains = this.br.getRegex("<td[^>]*?><img src=\"/index/host/small/[A-Za-z0-9]+\\.png\"[^>]*?title=\"([^<>\"]+)\">").getColumn(0);
        final ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(domains));
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void loginAPI(final Account account, final boolean force) throws Exception {
        newBrowser(this.br);
        br.setFollowRedirects(true);
        this.getPage("action=getaccountdetails");
        handleErrors(this.br);
    }

    /*
     * 2016-10-21: Added this as an API workaround but the admin(s) fixed the API so this function is not needed and actually it does not
     * work (yet)!
     */
    private void loginWebsite(final Account account, final boolean force) throws Exception {
        try {
            newBrowser(this.br);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null && !force) {
                this.br.setCookies(this.getHost(), cookies);
                this.br.getPage("http://" + this.getHost());
                if (isLoggedInWebsite()) {
                    return;
                }
                newBrowser(new Browser());
            }
            // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0");
            final DownloadLink dlinkbefore = this.getDownloadLink();
            if (dlinkbefore == null) {
                this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "http://" + account.getHoster(), true));
            }

            // this.br.setCookie(this.getHost(), "auth", "true");
            this.br.getPage("http://transload.me/main/?p=main");
            this.br.getPage("/ru/?p=login");
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            if (dlinkbefore != null) {
                this.setDownloadLink(dlinkbefore);
            }
            this.br.postPage("/user.php", "action=login&redir=&rid=&action=login&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
            this.br.getPage("/main/?p=main");
            if (!isLoggedInWebsite()) {
                if (System.getProperty("user.language").equals("de")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(account.getHoster()), "");
            handleErrors(this.br);
        } catch (final PluginException e) {
            account.clearCookies("");
            throw e;
        }
    }

    private boolean isLoggedInWebsite() {
        return this.br.containsHTML("action=logout");
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void getPageSafe(final String parameters) throws IOException, PluginException {
        getPage(parameters);
        handleErrors(this.br);
    }

    private void getPage(final String input) throws IOException {
        String accesslink;
        if (this.USE_API) {
            accesslink = API_BASE + "?username=" + Encoding.urlEncode(this.currAcc.getUser()) + "&password=" + Encoding.urlEncode(this.currAcc.getPass());
            accesslink += "&" + input;
            accesslink += "&client_id=jdownloader";
        } else {
            accesslink = input;
        }
        br.getPage(accesslink);
        updatestatuscode();
    }

    private void updatestatuscodeAPI() {
        final String errorcode = PluginJSonUtils.getJsonValue(this.br, "error");
        if (errorcode != null && errorcode.matches("\\d+")) {
            statuscode = Integer.parseInt(errorcode);
        } else if (errorcode != null) {
            statuscode = 666;
        }
    }

    private void updatestatuscodeWebsite() {
        /* TODO */
    }

    /* Please do not remove this function - future usage!! */
    private void handleErrorsAPI(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 2:
                /* Host currently not supported --> deactivate it after some tries */
                statusMessage = "Unsupported host";
                handleErrorRetries("unsupported_host", 5, 5 * 60 * 1000l);
            case 3:
                statusMessage = "Temporary error occured";
                handleErrorRetries("temporary_error", 50, 2 * 60 * 1000l);
            case 4:
            case 5:
                if (System.getProperty("user.language").equals("de")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 6:
                statusMessage = "Invalid API request - this should never happen!";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 7:
                /*
                 * From API docs: You asked for too many links, wait a few minutes, or refill your account. (This occurs when the user has
                 * generated a lot of links, though lacking means for downloading, you need to wait a few minutes before clearing balance).
                 */
                handleErrorRetries("temporary_error", 50, 2 * 60 * 1000l);

            default:
                handleErrorRetries("unknown_error_state", 50, 2 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    private void handleErrorsWebsite(final Browser br) throws PluginException {
        /* TODO */
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final String error, final int maxRetries, final long waittime) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        this.currDownloadLink.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(waittime);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}