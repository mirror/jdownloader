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
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
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
import jd.plugins.download.DownloadLinkDownloadable;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "downloader.guru" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32424" })
public class DownloaderGuru extends PluginForHost {
    private static final String                            API_ENDPOINT         = "http://www.downloader.guru/";
    private static final String                            NICE_HOST            = "downloader.guru";
    private static final String                            NICE_HOSTproperty    = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NORESUME             = NICE_HOSTproperty + "NORESUME";
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
    private static final int                               defaultMAXCHUNKS     = 0;
    private static final boolean                           defaultRESUME        = true;
    private static Object                                  CTRLLOCK             = new Object();
    private int                                            statuscode           = 0;
    private Account                                        currAcc              = null;
    private DownloadLink                                   currDownloadLink     = null;

    @SuppressWarnings("deprecation")
    public DownloaderGuru(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.downloader.guru/Pricing.aspx");
    }

    @Override
    public String getAGBLink() {
        return "http://www.downloader.guru/Support.aspx";
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
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
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
            this.postRawAPISafe(API_ENDPOINT + "Transfers.ashx?sendlinks=1", link.getDownloadURL());
            /* Returns json map "transfers" which contains array with usually only 1 object --> In this map we can find the "GeneratedLink" */
            dllink = PluginJSonUtils.getJsonValue(this.br, "GeneratedLink");
            if (dllink == null || !dllink.startsWith("http")) {
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
        final DownloadLinkDownloadable downloadable;
        if (link.getName().matches(".+(rar|r\\d+)$")) {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            final URLConnectionAdapter con = brc.openGetConnection(dllink);
            try {
                if (con.isOK() && con.isContentDisposition() && con.getLongContentLength() > 0) {
                    dllink = con.getRequest().getUrl();
                    if (link.getVerifiedFileSize() != -1 && link.getVerifiedFileSize() != con.getLongContentLength()) {
                        logger.info("Workaround for size missmatch(rar padding?!)!");
                        link.setVerifiedFileSize(con.getLongContentLength());
                    }
                }
            } finally {
                con.disconnect();
            }
            downloadable = new DownloadLinkDownloadable(link) {
                @Override
                public boolean isHashCheckEnabled() {
                    return false;
                }
            };
        } else {
            downloadable = new DownloadLinkDownloadable(link);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadable, br.createGetRequest(dllink), resume, maxChunks);
        } catch (PluginException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "RedirectLoop")) {
                tempUnavailableHoster(10 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
        if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(DownloaderGuru.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            updatestatuscode();
            handleAPIErrors(this.br);
            handleErrorRetries("unknowndlerror", 10, 5 * 60 * 1000l);
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        this.dl.startDownload();
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
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        this.setConstants(account, null);
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        login(false);
        /* As long as we always perform a full login, this call is never needed as full login will return account type and expire date too. */
        // accessUserInfo();
        final ArrayList<String> supportedhostslist = new ArrayList();
        final ArrayList<String> supportedhostslistTables = new ArrayList();
        final String trafficleft = this.br.getRegex("Traffic: <span style=\"[^\"]+\">(\\d+[^<>\"]+)</span>").getMatch(0);
        if (trafficleft != null && !this.br.containsHTML("No Traffic")) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setConcurrentUsePossible(true);
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            /* Find- and add premium table */
            final String supportedhostslistTablePremium = this.br.getRegex("<table id=\"MainContent_DLPremiumHosters\"(.*?)</table>").getMatch(0);
            if (supportedhostslistTablePremium != null) {
                supportedhostslistTables.add(supportedhostslistTablePremium);
            }
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            account.setConcurrentUsePossible(false);
            /*
             * 2016-06-16: When logged in, top right corner says "No Traffic" while there is a list of "Free Trial Hosters". I've tested 4
             * of them but when sending the links to download, the error "You are out of Traffic!" will come up. I guess it's safe to say
             * that Free Accounts have no traffic.
             */
            ai.setTrafficLeft(0);
        }
        /* Find- and add free table - this is of course always available (free + premium). */
        final String supportedhostslistTableFree = this.br.getRegex("<table id=\"MainContent_DLFreeHosters\"(.*?)</table>").getMatch(0);
        if (supportedhostslistTableFree != null) {
            supportedhostslistTables.add(supportedhostslistTableFree);
        }
        for (final String supportedhostslistTable : supportedhostslistTables) {
            final String[] supportedHostsOfCurrentTable = new Regex(supportedhostslistTable, ">([^<>\"]+)(</b>\\s*)?</td>").getColumn(0);
            for (String domain : supportedHostsOfCurrentTable) {
                domain = Encoding.htmlDecode(domain).trim();
                domain = correctHost(domain);
                if (domain.length() <= 3) {
                    continue;
                }
                final int maxdownloads = defaultMAXDOWNLOADS;
                final int maxchunks = defaultMAXCHUNKS;
                boolean resumable = defaultRESUME;
                hostMaxchunksMap.put(domain, maxchunks);
                hostMaxdlsMap.put(domain, maxdownloads);
                hostResumeMap.put(domain, resumable);
                supportedhostslist.add(domain);
            }
        }
        account.setValid(true);
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final boolean force) throws Exception {
        final Cookies cookies = this.currAcc.loadCookies("");
        if (cookies != null && !force) {
            this.br.setCookies(this.getHost(), cookies);
            this.br.getPage(API_ENDPOINT + "/Download.aspx");
            if (br.containsHTML("Logout\\.aspx")) {
                /* Refresh cookie timestamp */
                this.br.setCookies(this.getHost(), cookies);
                return;
            }
            /* Perform full login */
            this.br = prepBR(new Browser());
        }
        try {
            getAPISafe(API_ENDPOINT + "Login.aspx");
            Form loginform = this.br.getFormbyAction("./Login.aspx");
            if (loginform == null) {
                loginform = this.br.getForm(0);
            }
            if (loginform == null) {
                /* Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!");
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "\r\nPlugin broken, please contact the JDownloader Support!");
                }
            }
            for (InputField inputField : loginform.getInputFields()) {
                if (StringUtils.containsIgnoreCase(inputField.getKey(), "txt1")) {
                    inputField.setValue(Encoding.urlEncode(this.currAcc.getUser()));
                } else if (StringUtils.containsIgnoreCase(inputField.getKey(), "txt2")) {
                    inputField.setValue(Encoding.urlEncode(this.currAcc.getPass()));
                } else if (StringUtils.containsIgnoreCase(inputField.getKey(), "chkRememberME")) {
                    inputField.setValue("on");
                }
            }
            postAPIFormSafe(loginform);
            if (this.br.getCookie(this.getHost(), "AuthCookie") == null) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            this.currAcc.saveCookies(this.br.getCookies(this.getHost()), "");
        } catch (final PluginException e) {
            this.currAcc.clearCookies("");
            throw e;
        }
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

    private void getAPISafe(final String url) throws IOException, PluginException {
        this.br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYou've been blocked from the API!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postRawAPISafe(final String url, final String postData) throws IOException, PluginException {
        this.br.postPageRaw(url, postData);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPIFormSafe(final Form form) throws Exception {
        this.br.submitForm(form);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    /** Performs slight domain corrections. */
    private String correctHost(String host) {
        if (host.equals("uploaded.to") || host.equals("uploaded.net")) {
            host = "uploaded.to";
        }
        return host;
    }

    /**
     * Errors 1-99 = JDErrors (by psp)
     */
    private void updatestatuscode() {
        final String errormessage = PluginJSonUtils.getJsonValue(this.br, "errormessage");
        if (errormessage != null) {
            if (errormessage.equalsIgnoreCase("You are out of Traffic!")) {
                statuscode = 1;
            } else {
                /* Hell - unknown errorcode! */
                statuscode = 666;
            }
        } else {
            statuscode = 0;
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
                /* No traffic left (e.g. free account) */
                this.currAcc.getAccountInfo().setTrafficLeft(0);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 666:
                // /* Unknown error */
                statusMessage = "Unknown error";
                logger.info("Unknown error");
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknown_api_error", 50, 5 * 60 * 1000l);
            default:
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknown_api_error", 20, 5 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}