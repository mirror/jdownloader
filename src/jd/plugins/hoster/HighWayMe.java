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
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "high-way.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 2 })
public class HighWayMe extends PluginForHost {

    /** General API information: According to admin we can 'hammer' the API every 60 seconds */
    private static final String                            NOCHUNKS                = "NOCHUNKS";

    private static final String                            DOMAIN                  = "https://high-way.me/api.php";
    private static final String                            NICE_HOST               = "high-way.me";
    private static final String                            NICE_HOSTproperty       = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final int                               ERRORHANDLING_MAXLOGINS = 2;

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap      = new HashMap<Account, HashMap<String, Long>>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Boolean>                hostResumeMap           = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>                hostMaxchunksMap        = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>                hostMaxdlsMap           = new HashMap<String, Integer>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger>          hostRunningDlsNumMap    = new HashMap<String, AtomicInteger>();

    /* Last updated: 31.03.15 */
    private static final int                               defaultMAXDOWNLOADS     = 10;
    private static final int                               defaultMAXCHUNKS        = -4;
    private static final boolean                           defaultRESUME           = false;

    private static Object                                  CTRLLOCK                = new Object();
    private int                                            statuscode              = 0;
    private static AtomicInteger                           maxPrem                 = new AtomicInteger(1);
    private Account                                        currAcc                 = null;
    private DownloadLink                                   currDownloadLink        = null;

    public HighWayMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://high-way.me/pages/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://high-way.me/help/terms";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        return br;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
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
        /* Make sure that we do not start more than the allowed number of max simultan downloads for the current host. */
        synchronized (hostRunningDlsNumMap) {
            final String currentHost = this.correctHost(downloadLink.getHost());
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
        boolean resume = defaultRESUME;
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
        /* Check if chunkload failed before. TODO: Remove this! */
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        if (!resume) {
            maxChunks = 1;
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // handleErrorRetries("unknowndlerror", 5, 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            controlSlot(+1);
            try {
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                }
            } catch (final PluginException e) {
                link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
                // New V2 errorhandling
                /* unknown error, we disable multiple chunks */
                if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(HighWayMe.NOCHUNKS, false) == false) {
                    link.setProperty(HighWayMe.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw e;
            }
        } finally {
            // remove usedHost slot from hostMap
            // remove download slot
            controlSlot(-1);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.setConstants(account, link);
        this.br = newBrowser();
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            /* request creation of downloadlink */
            br.setFollowRedirects(true);
            postAPISafe(DOMAIN + "?login", "pass=" + Encoding.urlEncode(account.getPass()) + "&user=" + Encoding.urlEncode(account.getUser()));
            this.getAPISafe("https://high-way.me/load.php?json&link=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = getJson("download");
            if (dllink == null) {
                // handleErrorRetries("dllinknull", 5, 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(dllink);
        }
        handleDL(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
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
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        this.postAPISafe(DOMAIN + "?login&user&hoster", "pass=" + Encoding.urlEncode(this.currAcc.getPass()) + "&user=" + Encoding.urlEncode(this.currAcc.getUser()));
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final LinkedHashMap<String, Object> info_account = (LinkedHashMap<String, Object>) entries.get("user");
        final ArrayList<Object> array_hoster = (ArrayList) entries.get("hoster");
        final int account_maxchunks = ((Number) info_account.get("max_chunks")).intValue();
        final int account_maxdls = ((Number) info_account.get("max_connection")).intValue();
        final long free_traffic = ((Number) info_account.get("free_traffic")).longValue();
        final long premium_bis = ((Number) info_account.get("premium_bis")).longValue();
        final long premium_traffic = ((Number) info_account.get("premium_traffic")).longValue();
        final long premium_traffic_max = ((Number) info_account.get("premium_max")).longValue();
        /* Set account type and related things */
        if (premium_bis > 0 && premium_traffic_max > 0) {
            ai.setTrafficLeft(premium_traffic);
            ai.setTrafficMax(premium_traffic_max);
            ai.setValidUntil(premium_bis * 1000);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
        } else {
            ai.setTrafficLeft(free_traffic);
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        /* Set supported hosts, limits and account limits */
        account.setProperty("account_maxchunks", this.correctChunks(account_maxchunks));
        account.setProperty("account_maxdls", this.correctMaxdls(account_maxdls));

        final ArrayList<String> supportedHosts = new ArrayList<String>();
        hostMaxchunksMap.clear();
        hostMaxdlsMap.clear();
        account.setMaxSimultanDownloads(20);
        for (final Object hoster : array_hoster) {
            final LinkedHashMap<String, Object> hoster_map = (LinkedHashMap<String, Object>) hoster;
            final String domain = correctHost((String) hoster_map.get("name"));
            final String active = (String) hoster_map.get("active");
            final int resume = Integer.parseInt((String) hoster_map.get("resume"));
            final int maxchunks = Integer.parseInt((String) hoster_map.get("chunks"));
            final int maxdls = Integer.parseInt((String) hoster_map.get("downloads"));
            // final String unlimited = (String) hoster_map.get("unlimited");
            if (active.equals("1")) {
                supportedHosts.add(domain);
                hostMaxchunksMap.put(domain, correctChunks(maxchunks));
                hostMaxdlsMap.put(domain, correctMaxdls(maxdls));
                if (resume == 0) {
                    hostResumeMap.put(domain, false);
                } else {
                    hostResumeMap.put(domain, true);
                }
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    /** Login without errorhandling */
    private void login() throws IOException {
        br.postPage(DOMAIN + "?login", "pass=" + Encoding.urlEncode(this.currAcc.getPass()) + "&user=" + Encoding.urlEncode(this.currAcc.getUser()));
    }

    /** Login + errorhandling */
    private void loginSafe() throws IOException, PluginException {
        login();
        updatestatuscode();
        handleAPIErrors(this.br);
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

    private String getXML(final String key) {
        return br.getRegex("<" + key + ">([^<>\"]*?)</" + key + ">").getMatch(0);
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

    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        int tries = 0;
        do {
            this.br.getPage(accesslink);
            handleLoginIssues();
            tries++;
        } while (tries <= ERRORHANDLING_MAXLOGINS && this.statuscode == 9);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        int tries = 0;
        do {
            this.br.postPage(accesslink, postdata);
            handleLoginIssues();
            tries++;
        } while (tries <= ERRORHANDLING_MAXLOGINS && this.statuscode == 9);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    /** Performs full logins on errorcode 9 up to ERRORHANDLING_MAXLOGINS-times, hopefully avoiding login/cookie problems. */
    private void handleLoginIssues() throws IOException {
        updatestatuscode();
        if (this.statuscode == 9) {
            this.login();
            updatestatuscode();
        }
    }

    /** Corrects input so that it fits what we use in our plugins. */
    private int correctChunks(int maxchunks) {
        if (maxchunks < 1) {
            maxchunks = 1;
        } else if (maxchunks > 20) {
            maxchunks = 20;
        } else if (maxchunks > 1) {
            maxchunks = -maxchunks;
        }
        /* Else maxchunks = 1 */
        return maxchunks;
    }

    /** Corrects input so that it fits what we use in our plugins. */
    private int correctMaxdls(int maxdls) {
        if (maxdls < 1) {
            maxdls = 1;
        } else if (maxdls > 20) {
            maxdls = 20;
        }
        /* Else we should have a valid value! */
        return maxdls;
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
            maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), this.currAcc.getIntegerProperty("account_maxdls", defaultMAXDOWNLOADS)));
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
     * 0 = everything ok, 1-99 = official errorcodes, 100-199 = login-errors, 666 = hell
     */
    private void updatestatuscode() {
        String error = this.getJson("error");
        if (error == null) {
            error = getJson("code");
        }
        if (error != null) {
            if (error.matches("\\d+")) {
                statuscode = Integer.parseInt(error);
            } else {
                if (error.equals("NotLoggedIn")) {
                    statuscode = 100;
                } else if (error.equals("UserOrPassInvalid")) {
                    statuscode = 100;
                }
            }
        } else {
            statuscode = 0;
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        final String lang = System.getProperty("user.language");
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* Login or password missing -> disable account */
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nDein Account wurde gesperrt!";
                    this.currAcc.setError(AccountError.INVALID, statusMessage);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nYour account was banned!";
                    this.currAcc.setError(AccountError.INVALID, statusMessage);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 2:
                statusMessage = "Not enough free traffic";
                this.currAcc.setError(AccountError.TEMP_DISABLED, statusMessage);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 3:
                statusMessage = "Not enough premium traffic";
                this.currAcc.setError(AccountError.TEMP_DISABLED, statusMessage);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 4:
                /* Too many simultaneous downloads */
                statusMessage = "Too many simultaneous downloads";
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, statusMessage);
            case 5:
                /* Login or password missing -> disable account */
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nCode 5: Login Fehler";
                    this.currAcc.setError(AccountError.INVALID, statusMessage);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nCode 5: Login failure";
                    this.currAcc.setError(AccountError.INVALID, statusMessage);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 6:
                /* Invalid link --> Disable host */
                statusMessage = "Invalid link";
                tempUnavailableHoster(30 * 60 * 1000l);
            case 7:
                statusMessage = "Undefined errorstate";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Undefined errorstate");
            case 8:
                /* Temp error, try again in some minutes */
                statusMessage = "Temporary error";
                tempUnavailableHoster(1 * 60 * 1000l);
            case 9:
                /* No account found -> Disable host for 30 minutes */
                statusMessage = "No account found";
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 10:
                /* Host offline or invalid url -> Disable for 5 minutes */
                statusMessage = "Unsupported host";
                tempUnavailableHoster(60 * 60 * 1000l);
            case 11:
                /* Host itself is currently unavailable (maintenance) -> Disable host */
                statusMessage = "Host itself is currently unavailable";
                tempUnavailableHoster(2 * 60 * 60 * 1000l);
            case 12:
                /* MOCH itself is under maintenance */
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nDieser Anbieter führt momentan Wartungsarbeiten durch!";
                    this.currAcc.setError(AccountError.TEMP_DISABLED, statusMessage);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    statusMessage = "\r\nThis service is doing maintenance work!";
                    this.currAcc.setError(AccountError.TEMP_DISABLED, statusMessage);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            case 100:
                /* Login or password missing -> disable account */
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 666:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                // handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknownapierror", 5, 30 * 60 * 1000l);
                /* TODO: Remove plugin defects once plugin is in a stable state */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
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