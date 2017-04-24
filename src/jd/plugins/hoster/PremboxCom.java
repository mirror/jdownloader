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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.simplejson.JSonUtils;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "prembox.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" })
public class PremboxCom extends PluginForHost {

    private static final String                            CLEAR_DOWNLOAD_HISTORY                    = "CLEAR_DOWNLOAD_HISTORY_COMPLETE";

    /* Properties */
    private static final String                            PROPERTY_DOWNLOADTYPE                     = "premboxdownloadtype";
    private static final String                            PROPERTY_DOWNLOADTYPE_instant             = "instant";
    private static final String                            PROPERTY_DOWNLOADTYPE_cloud               = "cloud";

    /* Other constants & properties */
    private static final String                            API_SERVER                                = "http://prembox.com/uapi";
    private static final String                            NICE_HOST                                 = "prembox.com";
    private static final String                            NICE_HOSTproperty                         = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NOCHUNKS                                  = NICE_HOSTproperty + "NOCHUNKS";
    private static final String                            NORESUME                                  = NICE_HOSTproperty + "NORESUME";

    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME                    = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS                 = 0;
    private static final int                               ACCOUNT_PREMIUM_MAXDOWNLOADS              = 10;
    /*
     * This is the interval in which the complete download history will be deleted from the account (if etting is checked by the user && JD
     * does check the account)
     */
    private static final long                              DELETE_COMPLETE_DOWNLOAD_HISTORY_INTERVAL = 1 * 60 * 60 * 1000l;

    private int                                            statuscode                                = 0;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap                        = new HashMap<Account, HashMap<String, Long>>();
    /* Contains <host><Boolean resume possible|impossible> */
    private static HashMap<String, Boolean>                hostResumeMap                             = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>                hostMaxchunksMap                          = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>                hostMaxdlsMap                             = new HashMap<String, Integer>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger>          hostRunningDlsNumMap                      = new HashMap<String, AtomicInteger>();
    /* List of hosts which are only available via cloud (queue) download system */
    public static ArrayList<String>                        cloudOnlyHosts                            = new ArrayList<String>();
    private Account                                        currAcc                                   = null;
    private DownloadLink                                   currDownloadLink                          = null;
    public static Object                                   ACCLOCK                                   = new Object();
    private static Object                                  CTRLLOCK                                  = new Object();
    private static AtomicInteger                           maxPrem                                   = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    public PremboxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://prembox.com/register.html");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://prembox.com/contact.html";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        /* Not necessarily needed as long as we use the API */
        br.setCookie(NICE_HOST, "lang", "en");
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
        /* Make sure that we do not start more than the allowed number of max simultan downloads for the current host. */
        synchronized (hostRunningDlsNumMap) {
            final String currentHost = correctHost(downloadLink.getHost());
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

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @SuppressWarnings("deprecation")
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

        /* Code unused as values are not given by API */
        /*
         * When JD is started the first time and the user starts downloads right away, a full login might not yet have happened but it is
         * needed to get the individual host limits.
         */
        synchronized (CTRLLOCK) {
            if (hostMaxchunksMap.isEmpty() || hostMaxdlsMap.isEmpty() || hostResumeMap.isEmpty()) {
                logger.info("Performing full login to set individual host limits");
                this.fetchAccountInfo(account);
            }
        }
        setConstants(account, link);

        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            if (cloudOnlyHosts.contains(link.getHost())) {
                /* Try max 10 minutes */
                int counter = 0;
                int count_max = 15;
                this.postAPISafe(API_SERVER + "/downloadLink", "directDownload=0&login=" + JSonUtils.escape(this.currAcc.getUser()) + "&pass=" + JSonUtils.escape(this.currAcc.getPass()) + "&url=" + Encoding.urlEncode(this.currDownloadLink.getDownloadURL()));
                /* 'downloadLink' value will be "fileNotReadyYet" at this stage. */
                do {
                    this.postAPISafe(API_SERVER + "/serverFileStatus", "login=" + JSonUtils.escape(this.currAcc.getUser()) + "&pass=" + JSonUtils.escape(this.currAcc.getPass()) + "&url=" + Encoding.urlEncode(this.currDownloadLink.getDownloadURL()));
                    dllink = PluginJSonUtils.getJsonValue(br, "downloadLink");
                    if (!inValidate(dllink)) {
                        break;
                    }
                    /* As long as the file is not downloaded to the prembox servers, 'downloadLink' value will be "" */
                    counter++;
                    /* Admin requested us to use 20 seconds (instead of e.g.5) to not to overload their servers. */
                    this.sleep(20000l, link);
                } while (counter <= count_max);
            } else {
                link.setProperty(PROPERTY_DOWNLOADTYPE, PROPERTY_DOWNLOADTYPE_instant);
                this.postAPISafe(API_SERVER + "/downloadLink", "directDownload=1&login=" + JSonUtils.escape(this.currAcc.getUser()) + "&pass=" + JSonUtils.escape(this.currAcc.getPass()) + "&url=" + Encoding.urlEncode(this.currDownloadLink.getDownloadURL()));
                // this.postAPISafe(API_SERVER + "/serverFileStatus", "directDownload=1&login=" + JSonUtils.escape(this.currAcc.getUser()) +
                // "&pass=" + JSonUtils.escape(this.currAcc.getPass()) + "&url=" +
                // Encoding.urlEncode(this.currDownloadLink.getDownloadURL()));
                dllink = PluginJSonUtils.getJsonValue(br, "downloadLink");
            }
            /* Check if the url starts with "http" --> Extra errorhandling for faulty API responses */
            if (inValidate(dllink) || !dllink.startsWith("http")) {
                /* Should never happen */
                handleErrorRetries("dllinknull", 50, 2 * 60 * 1000l);
            }
            dllink = dllink.replaceAll("\\\\/", "/");
        }
        handleDL(account, link, dllink);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        // final String requestID = link.getStringProperty("premboxrequestId", null);
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        /* First set hardcoded limit */
        int maxChunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        /* Then check if we got an individual limit. */
        if (hostMaxchunksMap != null) {
            final String thishost = link.getHost();
            synchronized (hostMaxchunksMap) {
                if (hostMaxchunksMap.containsKey(thishost)) {
                    maxChunks = hostMaxchunksMap.get(thishost);
                    resume = hostResumeMap.get(thishost);
                }
            }
        }
        if (link.getBooleanProperty(PremboxCom.NORESUME, false)) {
            resume = false;
            link.setProperty(PremboxCom.NORESUME, Boolean.valueOf(false));
        }
        if (!resume || link.getBooleanProperty(NICE_HOSTproperty + NOCHUNKS, false)) {
            maxChunks = 1;
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(PremboxCom.NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (dl.getConnection().getResponseCode() == 503 && link.getBooleanProperty(NICE_HOSTproperty + PremboxCom.NOCHUNKS, false) == false) {
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                link.setProperty(NICE_HOSTproperty + PremboxCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html") || contenttype.contains("json")) {
                br.followConnection();
                updatestatuscode();
                handleAPIErrors(this.br);
                handleErrorRetries("unknowndlerror", 50, 2 * 60 * 1000l);
            }
            try {
                controlSlot(+1);
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) {
                            return;
                        }
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(NICE_HOSTproperty + PremboxCom.NOCHUNKS, false) == false) {
                        link.setProperty(NICE_HOSTproperty + PremboxCom.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                e.printStackTrace();
                // New V2 chunk errorhandling
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(NICE_HOSTproperty + PremboxCom.NOCHUNKS, false) == false) {
                    link.setProperty(NICE_HOSTproperty + PremboxCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw e;
            } finally {
                // remove usedHost slot from hostMap
                // remove download slot
                controlSlot(-1);
            }
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

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        final long last_deleted_complete_download_history_time_ago = getLast_deleted_complete_download_history_time_ago();
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        login();
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("data");
        final LinkedHashMap<String, Object> traffic_standard = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "traffic/standard");
        final LinkedHashMap<String, Object> traffic_daily = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "traffic/daily");

        String status = null;
        long traffic_left_total = 0;
        long expire_timestamp_standard = JavaScriptEngineFactory.toLong(traffic_standard.get("expireTstamp"), -1);
        long expire_timestamp_daily = JavaScriptEngineFactory.toLong(traffic_daily.get("expireTstamp"), -1);
        long expire_timestamp;
        /* Accounts can also have negative traffic (=no traffic left) */
        long traffic_left_standard = JavaScriptEngineFactory.toLong(traffic_standard.get("left"), 0);
        long traffic_left_daily = JavaScriptEngineFactory.toLong(traffic_daily.get("left"), 0);
        final String accounttype = (String) entries.get("accountType");

        if (traffic_left_daily > 0 && traffic_left_standard > 0) {
            status = " account with daily & standard traffic";
            traffic_left_total = traffic_left_daily + traffic_left_standard;
        } else if (traffic_left_daily > 0) {
            status = " account with daily traffic";
            traffic_left_total = traffic_left_daily;
        } else if (traffic_left_standard > 0) {
            status = " account with standard traffic";
            traffic_left_total = traffic_left_standard;
        } else {
            status = " account without traffic";
            traffic_left_total = 0;
        }

        if (expire_timestamp_standard > expire_timestamp_daily) {
            expire_timestamp = expire_timestamp_standard;
        } else {
            expire_timestamp = expire_timestamp_daily;
        }
        expire_timestamp = expire_timestamp * 1000;

        if ("premium".equals(accounttype) && expire_timestamp > System.currentTimeMillis()) {
            account.setType(AccountType.PREMIUM);
            status = "Premium" + status;
            ai.setStatus("Premium account");
        } else {
            account.setType(AccountType.FREE);
            status = "Free" + status;
            ai.setStatus("Registered (free) account");
        }
        ai.setStatus(status);
        ai.setTrafficLeft(traffic_left_total);
        account.setValid(true);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        this.getAPISafe(API_SERVER + "/supportedHosts");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("data");
        /**
         * Explanation of their status-types: Healthy = working, Fragile = may work or not - if not will be fixed within the next 72 hours
         * (support also said it means that they currently have no accounts for this host), Limited = broken, will be fixed tomorrow, dead =
         * site offline or their plugin is completely broken, Limited = There are special daily limits for a host but it should work (even
         * though it is marked RED on the site)
         */
        cloudOnlyHosts.clear();
        for (final Object domaininfo_o : ressourcelist) {
            final LinkedHashMap<String, Object> domaininfo = (LinkedHashMap<String, Object>) domaininfo_o;
            /*
             * 2017-03-03: Admin asked why we do not show all hosts so we told them that we skip offline hosts. This is a possibility for
             * them to easily force JD to display certain hosts. So far it has not been used (not present in json).
             */
            final Object force_display_o = domaininfo.get("force_display");
            final boolean force_display = force_display_o != null ? ((Boolean) force_display_o).booleanValue() : false;
            final boolean canResume = ((Boolean) domaininfo.get("resumable")).booleanValue();
            final long isoffline = JavaScriptEngineFactory.toLong(domaininfo.get("tmpTurnedOff"), 0);
            final long cloudonly = JavaScriptEngineFactory.toLong(domaininfo.get("serverOnly"), 0);
            final int maxChunks = (int) JavaScriptEngineFactory.toLong(domaininfo.get("maxChunks"), 1);
            final int maxDownloads = (int) JavaScriptEngineFactory.toLong(domaininfo.get("maxDownloads"), 1);
            final String host = correctHost((String) domaininfo.get("host"));
            if (isoffline == 1 && !force_display) {
                /* Do not add hosts that do not work at the moment. */
                continue;
            }
            supportedHosts.add(host);
            if (cloudonly == 1) {
                cloudOnlyHosts.add(host);
            }
            hostMaxchunksMap.put(host, this.correctChunks(maxChunks));
            hostMaxdlsMap.put(host, this.correctMaxdls(maxDownloads));
            hostResumeMap.put(host, canResume);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        if (this.getPluginConfig().getBooleanProperty(CLEAR_DOWNLOAD_HISTORY, default_clear_download_history_complete) && (last_deleted_complete_download_history_time_ago >= DELETE_COMPLETE_DOWNLOAD_HISTORY_INTERVAL || last_deleted_complete_download_history_time_ago == 0)) {
            /*
             * Go in here if user wants to have it's history deleted && last deletion was before DELETE_COMPLETE_DOWNLOAD_HISTORY_INTERVAL
             * or never executed (0).
             */
            this.deleteCompleteDownloadHistory(PROPERTY_DOWNLOADTYPE_instant);
        }
        return ai;
    }

    /** Log into users' account and set login cookie */
    private void login() throws IOException, PluginException {
        postAPISafe(API_SERVER + "/login", "login=" + JSonUtils.escape(currAcc.getUser()) + "&pass=" + JSonUtils.escape(currAcc.getPass()));
    }

    /**
     * Deletes the complete download list / history.
     **/
    private void deleteCompleteDownloadHistory(final String downloadtype) throws Exception {
        boolean success = false;
        /* This moves the downloaded files/entries to the download history. */
        postAPISafe(API_SERVER + "/clearFileList", "login=" + JSonUtils.escape(currAcc.getUser()) + "&pass=" + JSonUtils.escape(currAcc.getPass()));
        success = Boolean.parseBoolean(PluginJSonUtils.getJsonValue(br, "success"));
        if (!success) {
            logger.warning("Failed to delete file list");
        }
        /* This deletes the download history. */
        postAPISafe(API_SERVER + "/clearHistory", "login=" + JSonUtils.escape(currAcc.getUser()) + "&pass=" + JSonUtils.escape(currAcc.getPass()));
        success = Boolean.parseBoolean(PluginJSonUtils.getJsonValue(br, "success"));
        if (!success) {
            logger.warning("Failed to delete download history");
        }
        this.currAcc.setProperty("last_time_deleted_history", System.currentTimeMillis());
    }

    // private String getDownloadType() {
    // String type;
    // if (PROPERTY_DOWNLOADTYPE_cloud.equals(this.currDownloadLink.getStringProperty(PROPERTY_DOWNLOADTYPE, null))) {
    // type = PROPERTY_DOWNLOADTYPE_cloud;
    // } else {
    // type = PROPERTY_DOWNLOADTYPE_instant;
    // }
    // return type;
    // }

    /* Returns the time difference between now and the last time the complete download history has been deleted. */
    private long getLast_deleted_complete_download_history_time_ago() {
        return System.currentTimeMillis() - this.currAcc.getLongProperty("last_time_deleted_history", System.currentTimeMillis());
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

    private String getAPISafe(final String accesslink) throws IOException, PluginException {
        br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
        return this.br.toString();
    }

    private String postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
        br.postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
        return this.br.toString();
    }

    /**
     * 0 = everything ok, 1-99 = "error"-errors, 100-199 = "not_available"-errors, 200-299 = Other (html) [download] errors, sometimes mixed
     * with the API errors., 666 = hell
     */
    private void updatestatuscode() {
        final String error = PluginJSonUtils.getJsonValue(br, "error");
        // final String errorDescr = getJson("errorDescr");
        if (error != null) {
            if (error.equals("loginFailed") || error.equals("invalidLoginOrPassword")) {
                statuscode = 1;
            } else if (error.equals("fileNotFound")) {
                statuscode = 2;
            } else if (error.equals("invalidAccount")) {
                statuscode = 3;
            } else if (error.equals("invalidUrl")) {
                statuscode = 4;
            } else if (error.equals("tooManyConcurrentDownloads")) {
                statuscode = 5;
            } else if (error.equals("notPossibleToDownload")) {
                statuscode = 6;
            } else if (error.equals("emptyUrl") || error.equals("tooLongUrl")) {
                statuscode = 7;
            } else {
                statuscode = 666;
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
                /* 2017-04-24: Do not trust their 'File not found' errormessage! */
                handleErrorRetries("api_dummy_file_not_found", 10, 2 * 60 * 1000l);
            case 3:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Cannot download this file with this account", 3 * 60 * 1000l);
            case 4:
                statusMessage = "Host unsupported ?!";
                tempUnavailableHoster(5 * 60 * 1000l);
            case 5:
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many concurrent downloads with this account", 1 * 60 * 1000l);
            case 6:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Cannot download this file with this account at the moment", 3 * 60 * 1000l);
            case 7:
                /* This one should never happen! */
                handleErrorRetries("api_error_fatal", 10, 2 * 60 * 1000l);
            default:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries("unknownAPIerror", 10, 2 * 60 * 1000l);
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

    /** Performs slight domain corrections. */
    private String correctHost(String host) {
        if (host.equals("uploaded.to") || host.equals("uploaded.net")) {
            host = "uploaded.net";
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
     */
    private void controlSlot(final int num) {
        synchronized (CTRLLOCK) {
            final String currentHost = this.currDownloadLink.getHost();
            int was = maxPrem.get();
            maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), ACCOUNT_PREMIUM_MAXDOWNLOADS));
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

    private final boolean default_clear_download_history_complete = false;

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEAR_DOWNLOAD_HISTORY, "Delete download history every 60 minutes (on each 2nd full account check)?").setDefaultValue(default_clear_download_history_complete));
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}