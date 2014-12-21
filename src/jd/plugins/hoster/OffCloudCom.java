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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.storage.simplejson.JSonUtils;
import org.appwork.uio.UIOManager;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "offcloud.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class OffCloudCom extends PluginForHost {

    /** Using API: https://github.com/offcloud/offcloud-api */
    private static final String                            CLEAR_DOWNLOAD_HISTORY       = "CLEAR_DOWNLOAD_HISTORY";
    private static final String                            DOMAIN                       = "https://offcloud.com/api/";
    private static final String                            NICE_HOST                    = "offcloud.com";
    private static final String                            NICE_HOSTproperty            = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NOCHUNKS                     = NICE_HOSTproperty + "NOCHUNKS";
    private static final String                            NORESUME                     = NICE_HOSTproperty + "NORESUME";

    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private int                                            statuscode                   = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap           = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currAcc                      = null;
    private DownloadLink                                   currDownloadLink             = null;

    public OffCloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://offcloud.com/");
        this.setConfigElements();
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

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        setConstants(account, link);
        loginCheck();
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            this.postAPISafe(DOMAIN + "instant/download", "proxyId=&url=" + JSonUtils.escape(link.getDownloadURL()));
            final String requestId = getJson("requestId");
            if (requestId == null) {
                /* Should never happen */
                handleErrorRetries("requestIdnull", 5);
            }
            link.setProperty("offcloudrequestId", requestId);
            dllink = getJson("url");
            if (dllink == null) {
                /* Should never happen */
                handleErrorRetries("dllinknull", 5);
            }
            dllink = dllink.replaceAll("\\\\/", "/");
        }
        handleDL(account, link, dllink);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        final String requestID = link.getStringProperty("offcloudrequestId", null);
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        int maxChunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        if (link.getBooleanProperty(OffCloudCom.NORESUME, false)) {
            resume = false;
            link.setProperty(OffCloudCom.NORESUME, Boolean.valueOf(false));
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(OffCloudCom.NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html") || contenttype.contains("json")) {
                br.followConnection();
                updatestatuscode();
                handleAPIErrors(this.br);
                handleErrorRetries("unknowndlerror", 5);
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
                } else if (this.getPluginConfig().getBooleanProperty(CLEAR_DOWNLOAD_HISTORY, default_clear_download_history)) {
                    try {
                        boolean success = false;
                        try {
                            logger.info("Trying to delete downloaded link from history");
                            br.getPage("https://offcloud.com/instant/remove/" + requestID);
                            if (getJson("success").equals("true")) {
                                success = true;
                            }
                        } catch (final Throwable e) {
                            success = false;
                        }
                        if (success) {
                            logger.info("Succeeded to clear download history");
                        } else {
                            logger.warning("Failed to clear download history");
                        }
                    } catch (final Throwable ex) {
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
        if (!account.getUser().matches(".+@.+\\..+")) {
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
        br.postPage("https://offcloud.com/stats/usage-left", "");
        String remaininglinksnum = getJson("links");
        postAPISafe("https://offcloud.com/stats/addons", "");
        /*
         * Basically, at the moment we got 3 account types: Premium, Free account with generate-links feature, Free Account without
         * generate-links feature (used free account, ZERO traffic)
         */
        String userpackage = null;
        final String jsonarraypackages = br.getRegex("\"data\": \\[(.*?)\\]").getMatch(0);
        final String[] packages = jsonarraypackages.split("\\},([\t\r\n ]+)?\\{");
        if (packages.length == 1) {
            userpackage = packages[0];
        } else {
            for (final String singlepackage : packages) {
                final String type = getJson(singlepackage, "type");
                if (type.contains("link-unlimited")) {
                    userpackage = singlepackage;
                    break;
                }
            }
        }
        final String packagetype = getJson(userpackage, "type");
        if ((userpackage == null && br.containsHTML("-increase")) || userpackage.equals("") || packagetype.equals("premium-link-increase")) {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /* Important: If we found our package, get the remaining links count from there as the other one might be wrong! */
            if ("premium-link-increase".equals(packagetype)) {
                remaininglinksnum = getJson(userpackage, "remainingLinksCount");
            }
            account.setProperty("accinfo_linksleft", remaininglinksnum);
            if (remaininglinksnum.equals("0")) {
                /* No links downloadable anymore --> No traffic left --> Free account limit reached */
                ai.setTrafficLeft(0);
            }
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            ai.setUnlimitedTraffic();
            String expiredate = getJson(userpackage, "activeTill");
            expiredate = expiredate.replaceAll("Z$", "+0000");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate, "yyyy-MM-dd'T'HH:mm:ss.S", Locale.ENGLISH));
            account.setProperty("accinfo_linksleft", remaininglinksnum);
        }
        account.setValid(true);
        /* Only add hosts which are listed as 'active' (working) */
        postAPISafe("https://offcloud.com/stats/sites", "");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String jsonlist = br.getRegex("\"fs\":[\t\n\r ]+\\[(.*?)\\][\t\nr ]+}").getMatch(0);
        final String[] hostDomainsInfo = jsonlist.split("\\},([\t\n\r ]+)?\\{");
        for (final String domaininfo : hostDomainsInfo) {
            final String status = getJson(domaininfo, "isActive");
            final String realhost = getJson(domaininfo, "displayName");
            boolean active = Arrays.asList("Active", "Healthy", "Fragile").contains(status);
            if (active && "180upload.com".equals(realhost)) {
                logger.info("NOT adding 180upload.com to the list of supported hosts as it will require captcha via multihost also");
            } else if (active && realhost != null) {
                supportedHosts.add(realhost.toLowerCase());
            } else if (!active && realhost != null) {
                logger.info("NOT adding this host as it is inactive at the moment: " + realhost);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
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

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(ibr.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response provided String source.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value given JSon Array of Key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJsonArray(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonArray(br.toString(), key);
    }

    /**
     * Wrapper<br/>
     * Tries to return String[] value from provided JSon Array
     *
     * @author raztoki
     * @param source
     * @return
     */
    private String[] getJsonResultsFromArray(final String source) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJsonResultsFromArray(source);
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

    /**
     * 0 = everything ok, 1-99 = "error"-errors, 100-199 = "not_available"-errors, 200-299 = Other (html) [download] errors, sometimes mixed
     * with the API errors.
     */
    private void updatestatuscode() {
        String error = getJson("error");
        if (error == null) {
            error = getJson("not_available");
        }
        if (error != null) {
            if (error.equals("Please enter a valid email address.")) {
                statuscode = 1;
            } else if (error.equals("NOPREMIUMACCOUNTS")) {
                statuscode = 2;
            } else if (error.equals("User not authorized")) {
                statuscode = 3;
            } else if (error.equals("Purchase a premium downloading addon to continue with this operation.")) {
                statuscode = 4;
            } else if (error.equals("The credentials entered are wrong.")) {
                statuscode = 5;
            } else if (error.equals("File will not be downloaded due to an error.")) {
                statuscode = 6;
            } else if (error.equals("The supported site is temporarily disabled. We are working to resolve the problem quickly. Please try again later. Sorry for the inconvenience.") || error.contains("The supported site is temporarily disabled")) {
                statuscode = 7;
            } else if (error.equals("User is not allowed this operation.")) {
                statuscode = 8;
            } else if (error.equals("IP address needs to be registered. Check your email for further information.")) {
                statuscode = 9;
            } else if (error.equals("There is a networking issue. Please contact our support to get some help.")) {
                statuscode = 10;
            } else if (error.equals("premium")) {
                statuscode = 100;
            } else {
                /* TODO: Enable code below once all known errors are correctly handled */
                // statuscode = 666;
            }
        } else {
            if (br.containsHTML("We\\'re sorry but your download ticket couldn\\'t have been found, please repeat the download process\\.")) {
                statuscode = 200;
            } else {
                /* No way to tell that something unpredictable happened here --> status should be fine. */
                statuscode = 0;
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
                /* No email entered --> Should never happen as we validate user-input before -> permanently disable account */
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
            case 3:
                /* Free account limits reached and an additional download-try failed -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nFree Account Limits erreicht. Kaufe dir einen premium Account um weiter herunterladen zu können.";
                } else {
                    statusMessage = "\r\nFree account limits reached. Buy a premium account to continue downloading.";
                }
                this.currAcc.getAccountInfo().setTrafficLeft(0);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 4:
                freeAccountLimitReached();
            case 5:
                /* Username or password invalid -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 6:
                /* "File will not be downloaded due to an error." --> WTF */
                statusMessage = "WTF";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, statusMessage);
            case 7:
                /* Host is temporarily disabled --> Temporarily disable it */
                statusMessage = "Host is temporarily disabled";
                tempUnavailableHoster(30 * 60 * 1000l);
            case 8:
                /*
                 * 'User is not allowed this operation.' --> Basically the meaning is unknown but in this case, the host does not work
                 * either --> Disable it for a short period of time.
                 */
                statusMessage = "'User is not allowed this operation.' --> Host is temporarily disabled";
                tempUnavailableHoster(15 * 60 * 1000l);
            case 9:
                /* Free account limits reached -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nBitte bestätige deine aktuelle IP Adresse über den Bestätigungslink per E-Mail um den Account wieder nutzen zu können.";
                } else {
                    statusMessage = "\r\nPlease confirm your current IP adress via the activation link you got per mail to continue using this account.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 10:
                /* Networking issues --> Serverside problems --> Temporarily disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nServerseitige Netzwerkprobleme - bitte den offcloud Support kontaktieren.";
                } else {
                    statusMessage = "\r\nServerside networking problems - please contact the Offcloud support.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 100:
                /* Free account limits reached -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nFree Account Limits erreicht. Kaufe dir einen premium Account um weiter herunterladen zu können.";
                } else {
                    statusMessage = "\r\nFree account limits reached. Buy a premium account to continue downloading.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 200:
                /* Free account limits reached -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nDownloadticket defekt --> Neuversuch";
                } else {
                    statusMessage = "\r\nDownload ticket broken --> Retry link";
                }
                handleErrorRetries("downloadticketBroken", 10);
            default:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries("unknownAPIerror", 10);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    private void freeAccountLimitReached() throws PluginException {
        /*
         * Free account limits reached and an additional download-try failed or account cookie is invalid -> permanently disable account
         */
        String statusMessage;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            statusMessage = "\r\nFree Account Limits erreicht. Kaufe dir einen premium Account um weiter herunterladen zu können.";
        } else {
            statusMessage = "\r\nFree account limits reached. Buy a premium account to continue downloading.";
        }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    private void handleErrorRetries(final String error, final int maxRetries) throws PluginException {
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
            // tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
            /* TODO: Remove plugin defect once all known errors are correctly handled */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, error);
        }
    }

    private final boolean default_clear_download_history = false;

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEAR_DOWNLOAD_HISTORY, JDL.L("plugins.hoster.offcloudcom.clear_serverside_download_history", getPhrase("SETTING_CLEAR_DOWNLOAD_HISTORY"))).setDefaultValue(default_clear_download_history));
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
        {
            put("SETTING_CLEAR_DOWNLOAD_HISTORY", "Delete downloaded links from the offcloud download history after successful download?");
            put("ACCOUNT_USERNAME", "Username:");
            put("ACCOUNT_LINKSLEFT", "Instant download inputs left:");
            put("ACCOUNT_TYPE", "Account type:");
            put("ACCOUNT_SIMULTANDLS", "Max. simultaneous downloads:");
            put("ACCOUNT_CHUNKS", "Max number of chunks per file:");
            put("ACCOUNT_RESUME", "Resume of stopped downloads:");
            put("ACCOUNT_YES", "Yes");
            put("ACCOUNT_NO", "No");
            put("DETAILS_TITEL", "Account information");
            put("LANG_GENERAL_UNLIMITED", "Unlimited");
            put("LANG_GENERAL_CLOSE", "Close");
        }
    };

    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
        {
            put("SETTING_CLEAR_DOWNLOAD_HISTORY", "Lösche heruntergeladene links nach jedem erfolgreichen Download aus der offcloud Download-Historie?");
            put("ACCOUNT_USERNAME", "Account Name:");
            put("ACCOUNT_LINKSLEFT", "Verbleibende Anzahl von Instant-Download Links:");
            put("ACCOUNT_TYPE", "Account Typ:");
            put("ACCOUNT_SIMULTANDLS", "Max. Anzahl gleichzeitiger Downloads:");
            put("ACCOUNT_CHUNKS", "Max. Anzahl Verbindungen pro Datei (Chunks):");
            put("ACCOUNT_RESUME", "Abgebrochene Downloads fortsetzbar:");
            put("ACCOUNT_YES", "Ja");
            put("ACCOUNT_NO", "Nein");
            put("DETAILS_TITEL", "Additional account information");
            put("LANG_GENERAL_UNLIMITED", "Unlimitiert");
            put("LANG_GENERAL_CLOSE", "Schließen");
        }
    };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    public void showAccountDetailsDialog(final Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            final String windowTitleLangText = getPhrase("DETAILS_TITEL");
            final String accType = account.getStringProperty("acc_type", "Premium Account");
            final String accUsername = account.getUser();
            String linksleft = account.getStringProperty("accinfo_linksleft", "?");
            if (linksleft.equals("-1")) {
                linksleft = getPhrase("LANG_GENERAL_UNLIMITED");
            }

            /* it manages new panel */
            final PanelGenerator panelGenerator = new PanelGenerator();

            JLabel hostLabel = new JLabel("<html><b>" + account.getHoster() + "</b></html>");
            hostLabel.setIcon(DomainInfo.getInstance(account.getHoster()).getFavIcon());
            panelGenerator.addLabel(hostLabel);

            String revision = "$Revision$";
            try {
                String[] revisions = revision.split(":");
                revision = revisions[1].replace('$', ' ').trim();
            } catch (final Exception e) {
                logger.info(this.getHost() + " revision number error: " + e);
            }

            panelGenerator.addCategory("Account");
            panelGenerator.addEntry(getPhrase("ACCOUNT_USERNAME"), accUsername);
            panelGenerator.addEntry(getPhrase("ACCOUNT_TYPE"), accType);

            panelGenerator.addCategory("Download");
            panelGenerator.addEntry(getPhrase("ACCOUNT_LINKSLEFT"), linksleft);
            panelGenerator.addEntry(getPhrase("ACCOUNT_SIMULTANDLS"), "20");
            panelGenerator.addEntry(getPhrase("ACCOUNT_CHUNKS"), "2");
            panelGenerator.addEntry(getPhrase("ACCOUNT_RESUME"), getPhrase("ACCOUNT_YES"));

            panelGenerator.addEntry("Plugin Revision:", revision);

            ContainerDialog dialog = new ContainerDialog(UIOManager.BUTTONS_HIDE_CANCEL + UIOManager.LOGIC_COUNTDOWN, windowTitleLangText, panelGenerator.getPanel(), null, getPhrase("LANG_GENERAL_CLOSE"), "");
            try {
                Dialog.getInstance().showDialog(dialog);
            } catch (DialogNoAnswerException e) {
            }
        }

    }

    public class PanelGenerator {
        private JPanel panel = new JPanel();
        private int    y     = 0;

        public PanelGenerator() {
            panel.setLayout(new GridBagLayout());
            panel.setMinimumSize(new Dimension(270, 200));
        }

        public void addLabel(JLabel label) {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y;
            c.insets = new Insets(0, 5, 0, 5);
            panel.add(label, c);
            y++;
        }

        public void addCategory(String categoryName) {
            JLabel category = new JLabel("<html><u><b>" + categoryName + "</b></u></html>");

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y;
            c.insets = new Insets(10, 5, 0, 5);
            panel.add(category, c);
            y++;
        }

        public void addEntry(String key, String value) {
            GridBagConstraints c = new GridBagConstraints();
            JLabel keyLabel = new JLabel(key);
            // keyLabel.setFont(keyLabel.getFont().deriveFont(Font.BOLD));
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0.9;
            c.gridx = 0;
            c.gridy = y;
            c.insets = new Insets(0, 5, 0, 5);
            panel.add(keyLabel, c);

            JLabel valueLabel = new JLabel(value);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            panel.add(valueLabel, c);

            y++;
        }

        public void addTextField(JTextArea textfield) {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y;
            c.insets = new Insets(0, 5, 0, 5);
            panel.add(textfield, c);
            y++;
        }

        public JPanel getPanel() {
            return panel;
        }

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}