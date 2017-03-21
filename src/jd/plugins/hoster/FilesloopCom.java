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
import jd.controlling.AccountController;
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
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filesloop.com" }, urls = { "https?://(?:www\\.)?filesloop\\.com/myfiles/.+" })
public class FilesloopCom extends PluginForHost {

    /* Using similar API (and same owner): esoubory.cz, filesloop.com */

    private static final String                            DOMAIN               = "https://www.filesloop.com/api/";
    private static final String                            NICE_HOST            = "filesloop.com";
    private static final String                            NICE_HOSTproperty    = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NORESUME             = NICE_HOSTproperty + "NORESUME";
    private static final String                            PROPERTY_LOGINTOKEN  = "fileslooplogintoken";

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap   = new HashMap<Account, HashMap<String, Long>>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Boolean>                hostResumeMap        = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>                hostMaxchunksMap     = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>                hostMaxdlsMap        = new HashMap<String, Integer>();
    /* Contains <host><number of max possible filesize> */
    private static HashMap<String, Long>                   hostMaxfilesizeMap   = new HashMap<String, Long>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger>          hostRunningDlsNumMap = new HashMap<String, AtomicInteger>();

    /* Last updated: 31.03.15 */
    private static final int                               defaultMAXDOWNLOADS  = 10;
    private static final int                               defaultMAXCHUNKS     = 1;
    private static final boolean                           defaultRESUME        = true;

    private static Object                                  CTRLLOCK             = new Object();
    private int                                            statuscode           = 0;
    private static AtomicInteger                           maxPrem              = new AtomicInteger(1);
    private Account                                        currAcc              = null;
    private DownloadLink                                   currDownloadLink     = null;
    private static String                                  currLogintoken       = null;

    @SuppressWarnings("deprecation")
    public FilesloopCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.filesloop.com/premium/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.filesloop.com/terms-of-use/";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setReadTimeout(1 * 60 * 1000);
        br.setConnectTimeout(1 * 60 * 1000);
        return br;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
        if (currLogintoken == null) {
            currLogintoken = this.getLoginToken();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            return AvailableStatus.UNCHECKABLE;
        }
        try {
            login(false);
        } catch (final Throwable e) {
            return AvailableStatus.UNCHECKABLE;
        }
        this.getAPISafe(DOMAIN + "exists?token=" + currLogintoken + "&url=" + Encoding.urlEncode(link.getDownloadURL()));
        final String filename = PluginJSonUtils.getJsonValue(br, "filename");
        final String filesize = PluginJSonUtils.getJsonValue(br, "filesize");
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
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
        /* Make sure that the file we want to download is not too big. */
        synchronized (hostMaxfilesizeMap) {
            final long downloadfilesize = downloadLink.getDownloadSize();
            if (hostMaxfilesizeMap.containsKey(currentHost)) {
                final long filesizemax = hostMaxfilesizeMap.get(currentHost);
                if (downloadfilesize > filesizemax) {
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
            this.getAPISafe(DOMAIN + "exists?token=" + currLogintoken + "&url=" + Encoding.urlEncode(link.getDownloadURL()));
            /* Create downloadlink */
            this.getAPISafe(DOMAIN + "filelink?token=" + currLogintoken + "&url=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = PluginJSonUtils.getJsonValue(br, "link");
            if (dllink == null) {
                logger.warning("Final downloadlink is null");
                handleErrorRetries("dllinknull", 10, 60 * 60 * 1000l);
            }
        }
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
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
            link.setProperty(FilesloopCom.NORESUME, Boolean.valueOf(true));
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
        this.br = newBrowser();

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
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);

        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }

        login(true);
        this.getAPISafe(DOMAIN + "accountinfo?token=" + currLogintoken);

        final String accounttype = PluginJSonUtils.getJsonValue(br, "premium");
        final String validuntil = PluginJSonUtils.getJsonValue(br, "premium_to");
        long timestamp_validuntil = 0;
        if (validuntil != null) {
            timestamp_validuntil = Long.parseLong(validuntil) * 1000;
        }

        /* Expired premium == FREE but API will still say its premium so we have to identify the real account type via expire date. */
        /*
         * 2016-02-01: Free accounts ('Trail plan') can download one file up to 1 GB. On download attempt of multiple files (I was actually
         * able to download more than 1 file with a free account!) over 500 MB, server returned
         * '{"error":"error-downloading-file","data":[]}' so there is no way to handle this situation correctly until now. Once a free
         * account is completely out of traffic, this site will show "Your trail is over. Purchase premium account to download files.":
         * https://www.filesloop.com/account/dashboard/
         */
        if (accounttype.equals("1") && timestamp_validuntil > System.currentTimeMillis()) {
            ai.setValidUntil(timestamp_validuntil);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        }

        this.getAPISafe(DOMAIN + "list");
        ArrayList<String> supportedhostslist = new ArrayList();
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("data");
        for (final Object hostinfoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) hostinfoo;
            final Object max_filesizeo = entries.get("max_filesize");
            final int maxdownloads = this.correctMaxdls((int) JavaScriptEngineFactory.toLong(entries.get("max_download"), defaultMAXDOWNLOADS));
            final int maxchunks = this.correctChunks((int) JavaScriptEngineFactory.toLong(entries.get("max_connection"), defaultMAXCHUNKS));
            final String host = ((String) entries.get("domain")).toLowerCase();

            boolean resumable = defaultRESUME;
            final Object resumableo = entries.get("resumable");
            if (resumableo instanceof Boolean) {
                resumable = ((Boolean) resumableo).booleanValue();
            } else {
                resumable = Boolean.parseBoolean((String) resumableo);
            }

            /* WTF they put their own domain in there - skip that! */
            if (host.equals("filesloop.com")) {
                continue;
            }

            hostMaxchunksMap.put(host, maxchunks);
            hostMaxdlsMap.put(host, maxdownloads);
            hostResumeMap.put(host, resumable);
            if (max_filesizeo instanceof String) {
                final String max_filesize = (String) max_filesizeo;
                if (max_filesize.matches("\\d+")) {
                    hostMaxfilesizeMap.put(host, Long.parseLong(max_filesize));
                }
            }
            supportedhostslist.add(host);
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        ai.setUnlimitedTraffic();

        hostMaxchunksMap.clear();
        hostMaxdlsMap.clear();
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final boolean force) throws IOException, PluginException {
        if (currLogintoken != null) {
            this.getAPISafe(DOMAIN + "checktoken?token=" + currLogintoken);
            if (br.containsHTML("\"status\":\"invalid\"")) {
                logger.info("Current logintoken is invalid --> Performing full login");
            } else {
                logger.info("Current logintoken is still valid");
                return;
            }
        }
        this.br.getPage(DOMAIN + "login?email=" + Encoding.urlEncode(this.currAcc.getUser()) + "&password=" + Encoding.urlEncode(this.currAcc.getPass()));
        currLogintoken = PluginJSonUtils.getJsonValue(br, "token");
        if (currLogintoken == null) {
            /* Errorhandling should cover failed login already */
            updatestatuscode();
            handleAPIErrors(this.br);
            /* Should never happen - but we don't want an NPE to happen. */
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
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void getAPISafe(final String accesslink) throws IOException, PluginException {
        this.br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        this.br.postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

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
     * 0 = everything ok, 1-99 = official errorcodes, 100-199 = other errors, 666 = hell
     */
    private void updatestatuscode() {
        /* First look for errorcode */
        String error = PluginJSonUtils.getJsonValue(br, "error");
        if (inValidate(error)) {
            error = null;
        }
        if (error != null) {
            if (error.equalsIgnoreCase("invalid-email")) {
                statuscode = 1;
            } else if (error.equalsIgnoreCase("login-failed")) {
                statuscode = 2;
            } else if (error.equalsIgnoreCase("dl-token-invalid")) {
                statuscode = 3;
            } else if (error.equalsIgnoreCase("invalid-api-access")) {
                statuscode = 4;
            } else if (error.equalsIgnoreCase("invalid-file")) {
                statuscode = 5;
            } else if (error.equalsIgnoreCase("dl-error-too-many-logins")) {
                statuscode = 6;
            } else {
                statuscode = 666;
            }
        } else {
            final String exists = PluginJSonUtils.getJsonValue(br, "exists");
            if ("false".equals(exists)) {
                statuscode = 100;
            } else {
                /* Seems like everything is fine */
                statuscode = 0;
            }
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
                statusMessage = "Invalid account";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 2:
                statusMessage = "Invalid account";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 3:
                /* dl-token invalid --> Should never happen */
                statusMessage = "dl token invalid";
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_dl_token_invalid", 10, 5 * 60 * 1000l);
            case 4:
                /* Fatal API failure - should never happen */
                statusMessage = "Fatal API failure";
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nFataler API Fehler";
                } else {
                    statusMessage = "\r\nFatal API failure";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 5:
                /* "invalid-file" --> The name itself has no meaning - its just a general error so we should retry */
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_apierror_invalidfile", 20, 5 * 60 * 1000l);
            case 6:
                statusMessage = "Too many logins - try re-logging in later";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 100:
                /* File offline - don't trust their API! */
                // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_wrong_api_message_fileoffline", 20, 5 * 60 * 1000l);
            case 666:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknown_api_error", 20, 5 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    /** Corrects input so that it fits what we use in our plugins. */
    private int correctChunks(int maxchunks) {
        if (maxchunks < 1) {
            maxchunks = 1;
        } else if (maxchunks > 1) {
            maxchunks = -maxchunks;
        }
        /* Else maxchunks == 1 */
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