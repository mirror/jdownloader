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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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
import jd.plugins.hoster.K2SApi.JSonUtils;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vip.cocoleech.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32424" }, flags = { 2 })
public class VipCocoleechCom extends PluginForHost {

    private static final String                            API_ENDPOINT         = "http://vip.cocoleech.com/api";
    private static final String                            NICE_HOST            = "vip.cocoleech.com";
    private static final String                            NICE_HOSTproperty    = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NORESUME             = NICE_HOSTproperty + "NORESUME";
    private static final String                            PROPERTY_LOGINTOKEN  = "cocoleechlogintoken";

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap   = new HashMap<Account, HashMap<String, Long>>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Boolean>                hostResumeMap        = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>                hostMaxchunksMap     = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>                hostMaxdlsMap        = new HashMap<String, Integer>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger>          hostRunningDlsNumMap = new HashMap<String, AtomicInteger>();

    /* Last updated: 31.03.15 */
    private static final int                               defaultMAXDOWNLOADS  = 20;
    private static final int                               defaultMAXCHUNKS     = -5;
    private static final boolean                           defaultRESUME        = true;
    private final String                                   apikey               = "cdb5efc9c72196c1bd8b7a594b46b44f";

    private static Object                                  CTRLLOCK             = new Object();
    private int                                            statuscode           = 0;
    private static AtomicInteger                           maxPrem              = new AtomicInteger(1);
    private Account                                        currAcc              = null;
    private DownloadLink                                   currDownloadLink     = null;
    private static String                                  currLogintoken       = null;

    @SuppressWarnings("deprecation")
    public VipCocoleechCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://vip.cocoleech.com/pricing.php");
    }

    @Override
    public String getAGBLink() {
        return "http://vip.cocoleech.com/";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
        if (currLogintoken == null) {
            currLogintoken = this.getLoginToken();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        final String currentHost = this.correctHost(downloadLink.getHost());
        /* Make sure that we do not start more than the allowed number of max simultan downloads for the current host. */
        synchronized (hostRunningDlsNumMap) {
            if (hostRunningDlsNumMap.containsKey(currentHost) && hostMaxdlsMap.containsKey(currentHost)) {
                final int maxDlsForCurrentHost = hostMaxdlsMap.get(currentHost);
                final AtomicInteger currentRunningDlsForCurrentHost = hostRunningDlsNumMap.get(currentHost);
                if (currentRunningDlsForCurrentHost.get() >= maxDlsForCurrentHost) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        this.setConstants(account, link);
        handleDL(account, link);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            br.setFollowRedirects(true);
            /* request creation of downloadlink */
            /* Make sure that the file exists - unnecessary step in my opinion (psp) but admin wanted to have it implemented this way. */
            this.getAPISafe(API_ENDPOINT + "/download.php?url=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = JSonUtils.getJson(this.br, "download_url");
            if (dllink == null) {
                logger.warning("Final downloadlink is null");
                handleErrorRetries("dllinknull", 10, 60 * 60 * 1000l);
            }
        }

        boolean resume = account.getBooleanProperty("resume", defaultRESUME);
        int maxChunks = account.getIntegerProperty("account_maxchunks", defaultMAXCHUNKS);
        /* Then check if we got an individual host limit. */
        if (hostMaxchunksMap != null) {
            final String thishost = link.getHost();
            synchronized (hostMaxchunksMap) {
                if (hostMaxchunksMap.containsKey(thishost)) {
                    maxChunks = hostMaxchunksMap.get(thishost);
                }
            }
        }

        if (hostResumeMap != null) {
            final String thishost = link.getHost();
            synchronized (hostResumeMap) {
                if (hostResumeMap.containsKey(thishost)) {
                    resume = hostResumeMap.get(thishost);
                }
            }
        }
        if (link.getBooleanProperty(NORESUME, false)) {
            resume = false;
        }
        if (!resume) {
            maxChunks = 1;
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
        if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(VipCocoleechCom.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            updatestatuscode();
            handleAPIErrors(this.br);
            handleErrorRetries("unknowndlerror", 10, 5 * 60 * 1000l);
        }
        try {
            controlSlot(+1);
            this.dl.startDownload();
        } finally {
            // remove usedHost slot from hostMap
            // remove download slot
            controlSlot(-1);
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);

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

        /*
         * When JD is started the first time and the user starts downloads right away, a full login might not yet have happened but it is
         * needed to get the individual host limits.
         */
        synchronized (CTRLLOCK) {
            if (hostMaxchunksMap.isEmpty() || hostMaxdlsMap.isEmpty()) {
                logger.info("Performing full login to set individual host limits");
                this.fetchAccountInfo(account);
            }
        }
        this.setConstants(account, link);
        login(false);

        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
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
    private void handleErrorRetries(final String error, final int maxRetries, final long disableTime) throws PluginException {
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
            tempUnavailableHoster(disableTime);
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.setConstants(account, null);
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();

        login(false);
        /* As long as we always perform a full login, this call is never needed as full login will return account type and expire date too. */
        // accessUserInfo();

        final String premiumstatus = getJson("premium_status");
        final String validuntil = getJson("expire_date");
        long timestamp_validuntil = 0;
        if (validuntil != null) {
            timestamp_validuntil = Long.parseLong(validuntil) * 1000;
        }
        if ("active".equalsIgnoreCase(premiumstatus)) {
            ai.setValidUntil(timestamp_validuntil);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setConcurrentUsePossible(true);
            /*
             * 2016-05-05: Accounts do not have general traffic limits - however there are individual host traffic limits see mainpage -->
             * "Host Status": http://vip.cocoleech.com/
             */
            ai.setUnlimitedTraffic();
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /* 2016-05-05: According to admin, free accounts cannot download anything */
            account.setConcurrentUsePossible(false);
            ai.setTrafficLeft(0);
        }

        this.getAPISafe(API_ENDPOINT + "/status.php");
        ArrayList<String> supportedhostslist = new ArrayList();
        final String[] supportedHosts = this.br.getRegex("\"([A-Za-z0-9\\-]+\\.[A-Za-z0-9\\-\\.]+)\"").getColumn(0);
        for (String domain : supportedHosts) {
            domain = correctHost(domain);
            final int maxdownloads = defaultMAXDOWNLOADS;
            final int maxchunks = defaultMAXCHUNKS;
            boolean resumable = defaultRESUME;

            hostMaxchunksMap.put(domain, maxchunks);
            hostMaxdlsMap.put(domain, maxdownloads);
            hostResumeMap.put(domain, resumable);
            supportedhostslist.add(domain);
        }
        account.setValid(true);

        hostMaxchunksMap.clear();
        hostMaxdlsMap.clear();
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void accessUserInfo() throws IOException, PluginException {
        this.getAPISafe(API_ENDPOINT + "/info.php");
    }

    private void login(final boolean force) throws IOException, PluginException {
        if (currLogintoken != null && !force) {
            try {
                accessUserInfo();
                logger.info("Successfully re-used previous login token");
                return;
            } catch (final PluginException e) {
                logger.info("Failed to re-use previous login token - performing full login!");
                prepBR(new Browser());
            }
        }
        this.getAPISafe(API_ENDPOINT + "/login.php?username=" + Encoding.urlEncode(this.currAcc.getUser()) + "&password=" + Encoding.urlEncode(this.currAcc.getPass()));
        currLogintoken = getJson("token");
        if (currLogintoken == null) {
            /* Should never happen */
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        this.currAcc.setProperty(PROPERTY_LOGINTOKEN, currLogintoken);
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

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        } else if (this.currDownloadLink.getHost().equals(this.getHost())) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
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

    private void getAPISafe(String accesslink) throws IOException, PluginException {
        if (!accesslink.contains("?")) {
            accesslink += "?";
        } else {
            accesslink += "&";
        }
        accesslink += "key=" + Encoding.urlEncode(apikey);
        if (currLogintoken != null) {
            accesslink += "&token=" + currLogintoken;
        }
        this.br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    // private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
    // this.br.postPage(accesslink, postdata);
    // updatestatuscode();
    // handleAPIErrors(this.br);
    // }

    private String getLoginToken() {
        return currAcc.getStringProperty(PROPERTY_LOGINTOKEN, null);
    }

    /** Performs slight domain corrections. */
    private String correctHost(String host) {
        if (host.equals("uploaded.to") || host.equals("uploaded.net")) {
            host = "uploaded.to";
        }
        return host;
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlSlot
     *            (+1|-1)
     * */
    private void controlSlot(final int num) {
        synchronized (CTRLLOCK) {
            final String currentHost = correctHost(this.currDownloadLink.getHost());
            int was = maxPrem.get();
            maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), defaultMAXDOWNLOADS));
            logger.info("maxPrem was = " + was + " && maxPrem now = " + maxPrem.get());
            AtomicInteger currentRunningDls = new AtomicInteger(0);
            if (hostRunningDlsNumMap.containsKey(currentHost)) {
                currentRunningDls = hostRunningDlsNumMap.get(currentHost);
            }
            currentRunningDls.set(currentRunningDls.get() + num);
            hostRunningDlsNumMap.put(currentHost, currentRunningDls);
        }
    }

    /**
     * 0 = everything ok, 100-300 = official errorcodes, 666 = hell
     */
    private void updatestatuscode() {
        /* First look for errorcode */
        String code_str = this.getJson("code");
        if (inValidate(code_str)) {
            code_str = "0";
        }
        statuscode = Integer.parseInt(code_str);
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        final String lang = System.getProperty("user.language");
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 101:
                /* Everything ok - info.php/info.php successful */
                break;
            case 102:
                statusMessage = "Invalid account";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 103:
                /* Invalid apikey - this should never happen. We'll just treat this as invalid logindata ... */
                statusMessage = "Invalid apikey";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 104:
                /* Missing parameter - this should never happen. We'll just treat this as invalid logindata ... */
                statusMessage = "Missing parameter";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

            case 106:
                /* ??? We'll just treat this as invalid logindata ... */
                statusMessage = "Error getting user info";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 108:
                /* Token not found - this should never happen. We'll just treat this as invalid logindata ... */
                statusMessage = "Token not found";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 109:
                /* Empty token - this should never happen. We'll just treat this as invalid logindata ... */
                statusMessage = "Empty token";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

            case 201:
                /* Everything ok */
                break;
            case 202:
                /* I guess - this should never happen. */
                statusMessage = "Missing http in front of link";
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_missinghttp_should_never_happen", 5, 10 * 60 * 1000l);
            case 203:
                /* Individual host limit is reached --> Temp disable that */
                statusMessage = "Individual host limit reached";
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_filehost_individual_host", 3, 10 * 60 * 1000l);
            case 204:
                statusMessage = "Filehost is under maintenance";
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_filehost_maintenance", 5, 10 * 60 * 1000l);
            case 205:
                /* TODO: Find a solution for this - this should never happen! */
                statusMessage = "Token expired";
                throw new PluginException(LinkStatus.ERROR_FATAL, statusMessage);
            case 206:
                /* I guess this happens if there is an issue with a particular link but not thze filehost?? */
                statusMessage = "Error link";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, statusMessage);
            case 207:
                statusMessage = "Failed to generate link";
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_filehost_failed_to_generate_final_downloadlink", 30, 10 * 60 * 1000l);
            case 666:
                // /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknown_api_error", 20, 5 * 60 * 1000l);
            default:
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknown_api_error", 20, 5 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}