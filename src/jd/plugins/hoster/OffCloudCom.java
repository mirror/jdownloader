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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.simplejson.JSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "offcloud.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class OffCloudCom extends PluginForHost {

    /** Using API: https://github.com/offcloud/offcloud-api */
    private static final String                            NOCHUNKS                     = "NOCHUNKS";
    private static final String                            DOMAIN                       = "https://offcloud.com/api/";
    private static final String                            NICE_HOST                    = "offcloud.com";
    private static final String                            NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");

    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private int                                            statuscode                   = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    public OffCloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://offcloud.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://offcloud.com/legal";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
        final String logincookie = getLoginCookie();
        if (logincookie != null) {
            logger.info("logincookie SET");
            br.setCookie(NICE_HOST, "connect.sid", logincookie);
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
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

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        int maxChunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, maxChunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                handleErrorRetries(account, link, "unknowndlerror", 5);
            }
            try {
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(OffCloudCom.NOCHUNKS, false) == false) {
                        link.setProperty(OffCloudCom.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(OffCloudCom.NOCHUNKS, false) == false) {
                    link.setProperty(OffCloudCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw e;
            }
        } catch (final Throwable e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        setConstants(account, link);
        loginCheck();
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            this.postAPISafe(DOMAIN + "instant/download", "proxyId=&url=" + JSonUtils.escape(link.getDownloadURL()));
            final String status = getJson("status");
            if (!status.equals("created")) {
                logger.warning("WTF: " + link.getDownloadURL());
            }
            dllink = getJson("url");
            if (dllink == null) {
                handleErrorRetries(account, link, "dllinknull", 5);
            }
            dllink = dllink.replaceAll("\\\\/", "/");
        }
        handleDL(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        /* Only do a full login if either we have no login cookie at all or it is expired */
        if (getLoginCookie() != null) {
            this.loginCheck();
        } else {
            login();
        }
        /* TODO: Add account information - API call for that does not yet exist */
        account.setValid(true);
        /* Only add hosts which are listed as 'active' (working) */
        postAPISafe("https://offcloud.com/stats/sites", "");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String jsonlist = br.getRegex("\"fs\":[\t\n\r ]+\\[(.*?)\\][\t\nr ]+}").getMatch(0);
        final String[] hostDomainsInfo = jsonlist.split("\\},([\t\n\r ]+)?\\{");
        for (final String domaininfo : hostDomainsInfo) {
            final String status = getJson(domaininfo, "isActive");
            final String realhost = getJson(domaininfo, "displayName");
            if ("Active".equalsIgnoreCase(status) && realhost != null) {
                supportedHosts.add(realhost.toLowerCase());
            } else if (!"Active".equalsIgnoreCase(status) && realhost != null) {
                logger.info("NOT adding this host as it is inactive at the moment: " + realhost);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        /* All accounts are 'premium' but free accounts simply have ZERO traffic (points) */
        ai.setStatus("Premium account");
        return ai;
    }

    /** Log into users' account and set login cookie */
    private void login() throws IOException, PluginException {
        postAPISafe(DOMAIN + "login/classic", "username=" + JSonUtils.escape(currAcc.getUser()) + "&password=" + JSonUtils.escape(currAcc.getPass()));
        final String logincookie = br.getCookie(NICE_HOST, "connect.sid");
        if (logincookie == null) {
            /* This should never happen as we got errorhandling for invalid logindata */
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        currAcc.setProperty("offcloudlogincookie", logincookie);
    }

    /** Checks if we're logged in --> If not, re-login (errorhandling will throw error if something is not right with our account) */
    private void loginCheck() throws IOException, PluginException {
        this.postAPISafe("https://offcloud.com/api/login/check", "");
        if (this.getJson("loggedIn").equals("1")) {
            logger.info("loginCheck successful");
        } else {
            logger.info("loginCheck failed --> re-logging in");
            login();
        }
    }

    private String getLoginCookie() {
        return currAcc.getStringProperty("offcloudlogincookie", null);
    }

    private String getJson(final String parameter) {
        return getJson(this.br.toString(), parameter);
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki & psp
     * */
    private String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(?:[ ]+)(-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + key + "\":(?:[ ]+)\"([^\"]+)\"").getMatch(0);
        }
        if (result != null) {
            result = JSonUtils.unescape(result);
        }
        return result;
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
            /* wait 30 mins to retry this host */
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @SuppressWarnings("unused")
    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        br.postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void updatestatuscode() {
        String error = getJson("error");
        if (error == null) {
            error = getJson("not_available");
        }
        if (error == null) {
            statuscode = 0;
        } else {
            if (error.equals("Please enter a valid email address.")) {
                statuscode = 1;
            } else if (error.equals("NOPREMIUMACCOUNTS")) {
                statuscode = 2;
            } else if (error.equals("premium")) {
                statuscode = 100;
            }
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* Login or password invalid -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 2:
                /* No premiumaccounts available for used host --> Temporarily disable it */
                statusMessage = "There are currently no premium accounts available for this host";
                tempUnavailableHoster(30 * 60 * 1000l);
            case 100:
                /* Login or password invalid -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nFree Account Limits erreicht. Kaufe dir einen premium Account um weiter herunterladen zu können.";
                } else {
                    statusMessage = "\r\nFree account limits reached. Buy a premium account to continue downloading.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            default:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries(this.currAcc, this.currDownloadLink, "unknownAPIerror", 10);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final Account acc, final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            // tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
            /* TODO: Remove plugin defect once all known errors are correctly handled */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error);
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}