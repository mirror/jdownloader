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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.ConditionalSkipReasonException;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 1, names = {}, urls = {})
public abstract class HighWayCore extends UseNet {
    private static final String                            PATTERN_TV                             = "(?i)https?://[^/]+/onlinetv\\.php\\?id=.+";
    private static final String                            PATTERN_DIRECT                         = "(?i)https?://[^/]+/dl(?:u|t)/(([a-z0-9]+)(?:/$|/.+))";
    private static final int                               STATUSCODE_PASSWORD_NEEDED_OR_WRONG    = 13;
    /* Contains <host><Boolean resume possible|impossible> */
    private static Map<String, Map<String, Boolean>>       hostResumeMap                          = new HashMap<String, Map<String, Boolean>>();
    /* Contains <host><number of max possible chunks per download> */
    private static Map<String, Map<String, Integer>>       hostMaxchunksMap                       = new HashMap<String, Map<String, Integer>>();
    /* Contains <host><number of max possible simultan downloads> */
    private static Map<String, Map<String, Integer>>       hostMaxdlsMap                          = new HashMap<String, Map<String, Integer>>();
    /* Contains <host><number of currently running simultan downloads> */
    private static Map<String, Map<String, AtomicInteger>> hostRunningDlsNumMap                   = new HashMap<String, Map<String, AtomicInteger>>();
    private static Map<String, Map<String, Integer>>       hostTrafficCalculationMap              = new HashMap<String, Map<String, Integer>>();
    private static Map<String, Object>                     mapLockMap                             = new HashMap<String, Object>();
    private final int                                      defaultMAXCHUNKS                       = -4;
    private final boolean                                  defaultRESUME                          = false;
    private final String                                   PROPERTY_ACCOUNT_MAXCHUNKS             = "maxchunks";
    private final String                                   PROPERTY_ACCOUNT_RESUME                = "resume";
    private final String                                   PROPERTY_ACCOUNT_MAX_DOWNLOADS_ACCOUNT = "max_downloads_account";
    private final String                                   PROPERTY_ACCOUNT_MAX_DOWNLOADS_USENET  = "max_downloads_usenet";
    private final String                                   PROPERTY_ACCOUNT_USENET_USERNAME       = "usenetU";
    private final String                                   PROPERTY_ACCOUNT_USENET_PASSWORD       = "usenetP";

    public static interface HighWayMeConfigInterface extends UsenetAccountConfigInterface {
    };

    protected abstract MultiHosterManagement getMultiHosterManagement();

    public HighWayCore(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        /* API returns errormessages in different languages depending on this header. */
        br.getHeaders().put("Accept-Language", System.getProperty("user.language"));
        br.setFollowRedirects(true);
        return br;
    }

    /** Returns true if an account is required to download the given item. */
    private boolean requiresAccount(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(PATTERN_DIRECT)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * API docs: https://high-way.me/threads/highway-api.201/ </br>
     * According to admin we can 'hammer' the API every 60 seconds
     */
    protected abstract String getAPIBase();

    protected abstract String getWebsiteBase();

    /** If enabled, apikey is used for login instead of username:password. */
    protected abstract boolean useApikeyLogin();

    protected Object getMapLock() {
        synchronized (mapLockMap) {
            Object ret = mapLockMap.get(getHost());
            if (ret == null) {
                ret = new Object();
                mapLockMap.put(getHost(), ret);
            }
            return ret;
        }
    }

    protected <T> Map<String, T> getMap(final Map<String, Map<String, T>> map) {
        synchronized (map) {
            Object ret = map.get(getHost());
            if (ret == null) {
                ret = new HashMap<String, Object>();
                map.put(getHost(), (Map<String, T>) ret);
            }
            return (Map<String, T>) ret;
        }
    }

    /** For some hosts this multihost calculates less/more traffic than the actual filesize --> Take this into account here */
    @Override
    public void update(final DownloadLink link, final Account account, long bytesTransfered) throws PluginException {
        synchronized (getMapLock()) {
            final Map<String, Integer> map = getMap(hostTrafficCalculationMap);
            final Integer trafficCalc = map.get(link.getHost());
            if (trafficCalc != null) {
                bytesTransfered = (bytesTransfered * trafficCalc) / 100;
            }
        }
        super.update(link, account, bytesTransfered);
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
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(PATTERN_TV)) {
            return new Regex(link.getPluginPatternMatcher(), "(?i)id=(\\d+)").getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_DIRECT).getMatch(0);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return this.requestFileInformation(link, account);
    }

    protected AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            return super.requestFileInformation(link);
        } else if (link.getPluginPatternMatcher().matches(PATTERN_TV)) {
            if (!link.isNameSet()) {
                link.setName(this.getFID(link) + ".mp4");
            }
            if (account == null) {
                link.getLinkStatus().setStatusText("Only downloadable via account!");
                return AvailableStatus.UNCHECKABLE;
            }
            br.setFollowRedirects(true);
            this.login(account, false);
            final boolean check_via_json = true;
            if (check_via_json) {
                final String json_url = link.getPluginPatternMatcher().replaceAll("(?i)stream=(?:0|1)", "") + "&json=1";
                br.getPage(json_url);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Number code = (Number) entries.get("code");
                if (code != null && code.intValue() != 0) {
                    /* E.g. {"code":"8","error":"ID nicht bekannt"} */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                this.checkErrors(br, link, account);
                link.setFinalFileName(entries.get("name").toString());
                final String filesizeBytesStr = entries.get("size").toString();
                if (filesizeBytesStr != null && filesizeBytesStr.matches("\\d+")) {
                    link.setVerifiedFileSize(Long.parseLong(filesizeBytesStr));
                }
            } else {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(Encoding.urlDecode(link.getPluginPatternMatcher(), true));
                    if (!this.looksLikeDownloadableContent(con) || con.getCompleteContentLength() <= 0) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        final String filename = getFileNameFromHeader(con);
                        if (filename != null) {
                            link.setFinalFileName(filename);
                        }
                        ;
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        } else {
            /* Direct-URL download. */
            if (!link.isNameSet()) {
                /* Set fallback name */
                link.setName(this.getFID(link));
            }
            if (account == null) {
                link.getLinkStatus().setStatusText("Only downloadable via account!");
                return AvailableStatus.UNCHECKABLE;
            }
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(link.getPluginPatternMatcher());
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    final String serverFilename = getFileNameFromHeader(con);
                    if (!StringUtils.isEmpty(serverFilename)) {
                        link.setFinalFileName(serverFilename);
                    } else {
                        String fallbackFilename = this.getFID(link);
                        final String realExtension = this.getExtensionFromMimeType(con.getContentType());
                        if (realExtension != null) {
                            fallbackFilename = applyFilenameExtension(fallbackFilename, "." + realExtension);
                        }
                        link.setFinalFileName(fallbackFilename);
                    }
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            /* Make sure that we do not start more than the allowed number of max simultaneous downloads for the current host. */
            synchronized (getMapLock()) {
                final Map<String, AtomicInteger> hostRunningDlsNumMap = getMap(HighWayCore.hostRunningDlsNumMap);
                final Map<String, Integer> hostMaxdlsMap = getMap(HighWayCore.hostMaxdlsMap);
                if (hostRunningDlsNumMap.containsKey(link.getHost()) && hostMaxdlsMap.containsKey(link.getHost())) {
                    final int maxDlsForCurrentHost = hostMaxdlsMap.get(link.getHost());
                    final AtomicInteger currentRunningDlsForCurrentHost = hostRunningDlsNumMap.get(link.getHost());
                    if (currentRunningDlsForCurrentHost.get() >= maxDlsForCurrentHost) {
                        /*
                         * Max downloads for specific host for this MOCH reached --> Avoid irritating/wrong 'Account missing' errormessage
                         * for this case - wait and retry!
                         */
                        final String msg;
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            msg = "Download von diesem Hoster aktuell nicht möglich: Zu viele gleichzeitige Downloads (max " + maxDlsForCurrentHost + ")";
                        } else {
                            msg = "Downloads of this host are currently not possible: Too many simultaneous downloads (max " + maxDlsForCurrentHost + ")";
                        }
                        throw new ConditionalSkipReasonException(new WaitingSkipReason(CAUSE.HOST_TEMP_UNAVAILABLE, 15 * 1000, msg));
                    }
                }
            }
            getMultiHosterManagement().runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.requestFileInformation(link, account);
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, this.getMaxChunks(link));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to file", 1 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.USENET };
    }

    /**
     * Block download slots for files which have to be downloaded to the multihost first? If this returns true, new downloads will not be
     * allowed as long as cacheDLChecker is running/waiting.
     */
    protected boolean blockDownloadSlotsForCloudDownloads(final Account account) {
        return true;
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        /*
         * When JD is started the first time and the user starts downloads right away, a full login might not yet have happened but it is
         * needed to get the individual host limits.
         */
        final boolean fetchAccountInfo;
        synchronized (getMapLock()) {
            if (getMap(HighWayCore.hostMaxchunksMap).isEmpty() || getMap(HighWayCore.hostMaxdlsMap).isEmpty()) {
                fetchAccountInfo = true;
            } else {
                fetchAccountInfo = false;
            }
        }
        if (fetchAccountInfo) {
            logger.info("Performing full login to set individual host limits");
            this.fetchAccountInfo(account);
        }
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            getMultiHosterManagement().runCheck(account, link);
            boolean resume = account.getBooleanProperty(PROPERTY_ACCOUNT_RESUME, defaultRESUME);
            int maxChunks = this.getMaxChunks(link);
            /* Look for host specific max chunks limit */
            final String thishost = link.getHost();
            synchronized (getMapLock()) {
                final Map<String, Integer> hostMaxchunksMap = getMap(HighWayCore.hostMaxchunksMap);
                if (hostMaxchunksMap.containsKey(thishost)) {
                    maxChunks = hostMaxchunksMap.get(thishost);
                }
                final Map<String, Boolean> hostResumeMap = getMap(HighWayCore.hostResumeMap);
                if (hostResumeMap.containsKey(thishost)) {
                    resume = hostResumeMap.get(thishost);
                }
            }
            if (!resume) {
                maxChunks = 1;
            }
            int statuscode;
            if (!this.attemptStoredDownloadurlDownload(link, this.getHost() + "directlink", resume, maxChunks)) {
                this.login(account, false);
                /* Request creation of downloadlink */
                br.setFollowRedirects(true);
                Map<String, Object> entries = null;
                String passCode = link.getDownloadPassword();
                int counter = 0;
                do {
                    if (counter > 0) {
                        passCode = getUserInput("Password?", link);
                    }
                    String getdata = "json&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
                    if (passCode != null) {
                        getdata += "&pass=" + Encoding.urlEncode(passCode);
                    }
                    br.getPage(getWebsiteBase() + "load.php?" + getdata);
                    entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    statuscode = ((Number) entries.get("code")).intValue();
                    if (statuscode != STATUSCODE_PASSWORD_NEEDED_OR_WRONG) {
                        break;
                    } else if (counter >= 2) {
                        logger.info("Password loop exceeded max retries");
                        break;
                    } else {
                        if (passCode != null) {
                            logger.info("Wrong password: " + passCode);
                        } else {
                            logger.info("Password required");
                        }
                        link.setPasswordProtected(true);
                        counter++;
                    }
                } while (true);
                if (statuscode == STATUSCODE_PASSWORD_NEEDED_OR_WRONG) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else if (passCode != null) {
                    /* Password has been entered correctly or previously given password was correct --> Save it */
                    link.setDownloadPassword(passCode);
                }
                this.checkErrors(entries, link, account);
                final Object infoMsgO = entries.get("info");
                if (infoMsgO instanceof String) {
                    /* Low traffic warning message: Usually something like "Less than 10% traffic remaining" */
                    BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
                        @Override
                        public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                            return new BasicNotify((String) infoMsgO, (String) infoMsgO, new AbstractIcon(IconKey.ICON_INFO, 32));
                        }
                    });
                }
                entries = this.cacheDLChecker(entries, this.br, link, account);
                String dllink = (String) entries.get("download");
                /* Validate URL */
                dllink = new URL(dllink).toString();
                String hash = (String) entries.get("hash");
                if (hash != null && !StringUtils.equalsIgnoreCase("hash", "null")) {
                    if (hash.matches("(?i)md5:[a-f0-9]{32}")) {
                        hash = hash.substring(hash.lastIndexOf(":") + 1);
                        logger.info("Set md5 hash given by multihost: " + hash);
                        link.setMD5Hash(hash);
                    } else if (hash.matches("(?i)sha1:[a-f0-9]{40}")) {
                        hash = hash.substring(hash.lastIndexOf(":") + 1);
                        logger.info("Set sha1 hash given by multihost: " + hash);
                        link.setSha1Hash(hash);
                    } else if (hash.matches("(?i)sha265:[a-f0-9]{40}")) {
                        hash = hash.substring(hash.lastIndexOf(":") + 1);
                        logger.info("Set sha265 hash given by multihost: " + hash);
                        link.setSha256Hash(hash);
                    } else {
                        logger.info("Unsupported file-hash string: " + hash);
                    }
                }
                br.setFollowRedirects(true);
                link.setProperty(this.getHost() + "directlink", dllink);
                br.setAllowedResponseCodes(new int[] { 503 });
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    this.checkErrors(this.br, link, account);
                    getMultiHosterManagement().handleErrorGeneric(account, null, "unknowndlerror", 1, 3 * 60 * 1000l);
                }
            }
            dl.setFilenameFix(true);
            controlSlot(+1);
            try {
                this.dl.startDownload();
            } finally {
                // remove usedHost slot from hostMap
                controlSlot(-1);
            }
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
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

    private Map<String, Object> cacheDLChecker(final Map<String, Object> loadDotPHPJson, final Browser br, final DownloadLink link, final Account account) throws Exception {
        final PluginProgress waitProgress = new PluginProgress(0, 100, null) {
            protected long lastCurrent    = -1;
            protected long lastTotal      = -1;
            protected long startTimeStamp = -1;

            @Override
            public PluginTaskID getID() {
                return PluginTaskID.WAIT;
            }

            @Override
            public String getMessage(Object requestor) {
                if (requestor instanceof ETAColumn) {
                    final long eta = getETA();
                    if (eta >= 0) {
                        return TimeFormatter.formatMilliSeconds(eta, 0);
                    }
                    return "";
                }
                // return status;
                return "Waiting for cache download...";
            }

            @Override
            public void updateValues(long current, long total) {
                super.updateValues(current, total);
                if (startTimeStamp == -1 || lastTotal == -1 || lastTotal != total || lastCurrent == -1 || lastCurrent > current) {
                    lastTotal = total;
                    lastCurrent = current;
                    startTimeStamp = System.currentTimeMillis();
                    // this.setETA(-1);
                    return;
                }
                long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
                if (currentTimeDifference <= 0) {
                    return;
                }
                final long speed = (current * 10000) / currentTimeDifference;
                if (speed == 0) {
                    return;
                }
                long eta = ((total - current) * 10000) / speed;
                this.setETA(eta);
            }
        };
        waitProgress.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
        waitProgress.setProgressSource(this);
        final String cachePollingURL = (String) loadDotPHPJson.get("cache");
        if (StringUtils.isEmpty(cachePollingURL)) {
            logger.info("Stepping out of cache handling because: No polling URL available -> No cache handling needed");
            return loadDotPHPJson;
        }
        Map<String, Object> entries = null;
        final int maxWaitSeconds = 90;
        int secondsWaited = 0;
        int retryInSecondsThisRound = 30;
        String textForJD = null;
        try {
            do {
                /**
                 * cacheStatus possible values and what they mean: </br>
                 * d = download </br>
                 * w = wait (retry) </br>
                 * q = in queue </br>
                 * qn = Download has been added to queue </br>
                 * i = direct download without cache </br>
                 * s = Cached download is ready for downloading
                 */
                br.getPage(cachePollingURL);
                entries = this.checkErrors(br, link, account);
                final Object cacheStatusO = entries.get("cacheStatus");
                if (cacheStatusO != null && cacheStatusO.toString().matches("(?i)i|s")) {
                    logger.info("Stepping out of cache handling because: Detected valid cacheStatus");
                    return entries;
                }
                retryInSecondsThisRound = ((Number) entries.get("retry_in_seconds")).intValue();
                textForJD = (String) entries.get("for_jd");
                if (!blockDownloadSlotsForCloudDownloads(account)) {
                    /**
                     * Throw exception right away so other download candidates will be tried. </br>
                     * This may speed up downloads significantly for some users.
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, textForJD, retryInSecondsThisRound * 1000l);
                }
                /* Wait and re-check */
                final int retryInSeconds = Math.min(retryInSecondsThisRound, maxWaitSeconds - secondsWaited);
                this.sleep(retryInSeconds * 1000l, link, textForJD);
                secondsWaited += retryInSeconds;
                final int currentProgress = ((Number) entries.get("percentage_Complete")).intValue();
                link.addPluginProgress(waitProgress);
                waitProgress.updateValues(currentProgress, 100);
                logger.info("Cache handling: Seconds waited " + secondsWaited + "/" + maxWaitSeconds);
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    throw new InterruptedException();
                } else if (secondsWaited >= maxWaitSeconds) {
                    logger.info("Stopping because: Exceeded max wait seconds " + maxWaitSeconds);
                    break;
                }
            } while (true);
        } finally {
            link.removePluginProgress(waitProgress);
        }
        logger.info("Cache handling: Timeout");
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, textForJD, retryInSecondsThisRound * 1000l);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.login(account, true);
        this.getPage(this.getAPIBase() + "?hoster&user");
        final Map<String, Object> entries = this.checkErrors(this.br, this.getDownloadLink(), account);
        final Map<String, Object> accountInfo = (Map<String, Object>) entries.get("user");
        final int accountResume = ((Number) accountInfo.get("resume")).intValue();
        final long premiumUntil = ((Number) accountInfo.get("premium_bis")).longValue();
        // final long premiumTraffic = ((Number) accountInfo.get("premium_traffic")).longValue();
        final long trafficLeftToday = ((Number) accountInfo.get("traffic_remain_today")).longValue();
        ai.setTrafficLeft(trafficLeftToday);
        /* Set account type and account information */
        if (Boolean.TRUE.equals(entries.get("premium"))) {
            ai.setTrafficMax(((Number) accountInfo.get("premium_max")).longValue());
            ai.setValidUntil(premiumUntil * 1000, this.br);
            account.setType(AccountType.PREMIUM);
        } else {
            final long free_traffic_max_daily = ((Number) accountInfo.get("free_traffic")).longValue();
            final long free_traffic_left = ((Number) accountInfo.get("remain_free_traffic")).longValue();
            if (free_traffic_left > free_traffic_max_daily) {
                /* User has more traffic than downloadable daily for free users --> Show max daily traffic. */
                ai.setTrafficLeft(free_traffic_max_daily);
            } else {
                /* User has less traffic (or equal) than downloadable daily for free users --> Show real traffic left. */
                ai.setTrafficLeft(free_traffic_left);
                ai.setTrafficMax(free_traffic_max_daily);
            }
            account.setType(AccountType.FREE);
        }
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            ai.setStatus(StringUtils.valueOfOrNull(accountInfo.get("type")) + " | Heute übrig: " + SIZEUNIT.formatValue((SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue(), trafficLeftToday));
        } else {
            ai.setStatus(StringUtils.valueOfOrNull(accountInfo.get("type")) + " | Remaining today: " + SIZEUNIT.formatValue((SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue(), trafficLeftToday));
        }
        final Map<String, Object> usenetLogins = (Map<String, Object>) accountInfo.get("usenet");
        if (this.useApikeyLogin()) {
            /* Try to set unique username as user could enter anything in the username field in this case */
            final String uniqueUsername = (String) usenetLogins.get("username");
            if (!StringUtils.isEmpty(uniqueUsername)) {
                account.setUser(uniqueUsername);
            }
        }
        account.setConcurrentUsePossible(true);
        /* Set supported hosts, host specific limits and account limits. */
        account.setProperty(PROPERTY_ACCOUNT_MAXCHUNKS, accountInfo.get("max_chunks"));
        account.setProperty(PROPERTY_ACCOUNT_MAX_DOWNLOADS_ACCOUNT, accountInfo.get("max_connection"));
        if (accountResume == 1) {
            account.setProperty(PROPERTY_ACCOUNT_RESUME, true);
        } else {
            account.setProperty(PROPERTY_ACCOUNT_RESUME, false);
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        synchronized (getMapLock()) {
            final Map<String, Integer> hostMaxchunksMap = getMap(HighWayCore.hostMaxchunksMap);
            final Map<String, Integer> hostTrafficCalculationMap = getMap(HighWayCore.hostTrafficCalculationMap);
            final Map<String, Integer> hostMaxdlsMap = getMap(HighWayCore.hostMaxdlsMap);
            final Map<String, Boolean> hostResumeMap = getMap(HighWayCore.hostResumeMap);
            hostMaxchunksMap.clear();
            hostTrafficCalculationMap.clear();
            hostMaxdlsMap.clear();
            /* Available hosts are returned by API depending on users' account type e.g. free users have much less supported hosts. */
            final List<Object> array_hoster = (List) entries.get("hoster");
            for (final Object hoster : array_hoster) {
                final Map<String, Object> hoster_map = (Map<String, Object>) hoster;
                final String domain = hoster_map.get("name").toString();
                /* Workaround to find the real domain which we need to assign the properties to later on! */
                final ArrayList<String> supportedHostsTmp = new ArrayList<String>();
                supportedHostsTmp.add(domain);
                ai.setMultiHostSupport(this, supportedHostsTmp);
                final List<String> realDomainList = ai.getMultiHostSupport();
                if (realDomainList == null || realDomainList.isEmpty()) {
                    /* Skip unsupported hosts or host plugins which don't allow multihost usage. */
                    logger.info("Skipping host not supported by JD or not multihoster-supported by JD: " + domain);
                    continue;
                }
                final String realDomain = realDomainList.get(0);
                // final String unlimited = (String) hoster_map.get("unlimited");
                if (((Number) hoster_map.get("active")).intValue() == 1) {
                    supportedHosts.add(realDomain);
                    hostTrafficCalculationMap.put(realDomain, ((Number) hoster_map.get("berechnung")).intValue());
                    hostMaxchunksMap.put(realDomain, correctChunks(((Number) hoster_map.get("chunks")).intValue()));
                    hostMaxdlsMap.put(realDomain, ((Number) hoster_map.get("downloads")).intValue());
                    if (((Number) hoster_map.get("resume")).intValue() == 1) {
                        hostResumeMap.put(realDomain, true);
                    } else {
                        hostResumeMap.put(realDomain, false);
                    }
                }
            }
        }
        /* Get- and store usenet logindata. These can differ from the logindata the user has added but may as well be equal to those. */
        if (usenetLogins != null) {
            ai.setProperty(PROPERTY_ACCOUNT_USENET_USERNAME, usenetLogins.get("username"));
            ai.setProperty(PROPERTY_ACCOUNT_USENET_PASSWORD, usenetLogins.get("pass"));
            account.setProperty(PROPERTY_ACCOUNT_MAX_DOWNLOADS_USENET, accountInfo.get("usenet_connection"));
        } else {
            supportedHosts.remove("usenet");
            supportedHosts.remove("Usenet");
            ai.removeProperty(PROPERTY_ACCOUNT_USENET_USERNAME);
            ai.removeProperty(PROPERTY_ACCOUNT_USENET_PASSWORD);
            account.removeProperty(PROPERTY_ACCOUNT_MAX_DOWNLOADS_USENET);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    @Override
    protected String getUseNetUsername(final Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            return ai.getStringProperty(PROPERTY_ACCOUNT_USENET_USERNAME);
        } else {
            return null;
        }
    }

    @Override
    protected String getUseNetPassword(final Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            return ai.getStringProperty(PROPERTY_ACCOUNT_USENET_PASSWORD);
        } else {
            return null;
        }
    }

    /**
     * Login without errorhandling
     *
     * @return true = cookies validated </br>
     *         false = cookies set but not validated
     *
     * @throws PluginException
     * @throws InterruptedException
     */
    public void login(final Account account, final boolean validateCookies) throws IOException, PluginException, InterruptedException {
        prepBR(this.br);
        if (this.useApikeyLogin()) {
            account.setPass(correctPassword(account.getPass()));
            if (!isAPIKey(account.getPass())) {
                throw new AccountInvalidException("Invalid API key format");
            }
        }
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            this.br.setCookies(this.getHost(), cookies);
            if (!validateCookies) {
                /* Do not validate cookies */
                return;
            } else {
                logger.info("Checking cookies");
                this.br.getPage(this.getAPIBase() + "?logincheck");
                /* Don't check for errors here as a failed login can trigger error dialogs which we don't want here! */
                // this.checkErrors(this.br, account);
                try {
                    this.checkErrors(br, null, account);
                    /* No exception --> Success */
                    logger.info("Cookie login successful");
                    account.saveCookies(this.br.getCookies(this.br.getHost()), "");
                    return;
                } catch (final PluginException ignore) {
                    logger.log(ignore);
                    logger.info("Cookie login failed");
                }
            }
        }
        logger.info("Performing full login");
        if (this.useApikeyLogin()) {
            br.postPage(getAPIBase() + "?login", "apikey=" + Encoding.urlEncode(account.getPass()));
        } else {
            br.postPage(getAPIBase() + "?login", "pass=" + Encoding.urlEncode(account.getPass()) + "&user=" + Encoding.urlEncode(account.getUser()));
        }
        this.checkErrors(this.br, this.getDownloadLink(), account);
        /* No Exception --> Assume that login was successful */
        account.saveCookies(this.br.getCookies(this.br.getHost()), "");
    }

    protected static boolean isAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[A-Za-z0-9]{32}")) {
            return true;
        } else {
            return false;
        }
    }

    public static String correctPassword(final String str) {
        return str.trim();
    }

    protected abstract void exceptionAccountInvalid(final Account account) throws PluginException;

    private int getMaxChunks(final DownloadLink link) {
        final int maxChunksStored = link.getIntegerProperty(PROPERTY_ACCOUNT_MAXCHUNKS, defaultMAXCHUNKS);
        return correctChunks(maxChunksStored);
    }

    /** Corrects input so that it fits what we use in our plugins. */
    private int correctChunks(final int maxchunks) {
        if (maxchunks > 1) {
            /* Minus maxChunksStored -> Up to X chunks */
            return -maxchunks;
        } else {
            return maxchunks;
        }
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
        synchronized (getMapLock()) {
            final Map<String, AtomicInteger> hostRunningDlsNumMap = getMap(HighWayCore.hostRunningDlsNumMap);
            AtomicInteger currentRunningDls = hostRunningDlsNumMap.get(this.getDownloadLink().getHost());
            if (currentRunningDls == null) {
                currentRunningDls = new AtomicInteger(0);
                hostRunningDlsNumMap.put(this.getDownloadLink().getHost(), currentRunningDls);
            }
            currentRunningDls.addAndGet(num);
        }
    }

    private Map<String, Object> checkErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        try {
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            this.checkErrors(entries, link, account);
            return entries;
        } catch (final JSonMapperException jse) {
            final String errormsg = "Invalid API response";
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormsg, 30 * 1000l);
            } else {
                throw new AccountUnavailableException(errormsg, 30 * 1000l);
            }
        }
    }

    private void checkErrors(final Map<String, Object> entries, final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        final Number code = (Number) entries.get("code");
        String msg = (String) entries.get("error");
        if (code == null && msg == null) {
            /* No error -> We're good :) */
            return;
        }
        int retrySeconds = 180;
        final Object retryInSecondsO = entries.get("retry_in_seconds");
        if (retryInSecondsO != null && (retryInSecondsO instanceof Number || retryInSecondsO.toString().matches("\\d+"))) {
            retrySeconds = Integer.parseInt(retryInSecondsO.toString());
        }
        if (code != null) {
            switch (code.intValue()) {
            case 0:
                /* No error */
                return;
            case 1:
                /* Invalid logindata */
                this.exceptionAccountInvalid(account);
            case 2:
                /* Session expired (this should never happen) */
                throw new AccountUnavailableException(msg, retrySeconds * 1000l);
            case 3:
                /* Not enough premium traffic available */
                throw new AccountUnavailableException(msg, retrySeconds * 1000l);
            case 4:
                /* User requested too many simultaneous downloads */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
            case 5:
                /* Premium package expired --> Temp. deactivate account so next account-check will set correct new account status */
                throw new AccountUnavailableException(msg, retrySeconds * 1000l);
            case 6:
                /* No- or no valid URL was provided (this should never happen) */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
            case 7:
                /* There is no case 7 (lol) */
            case 8:
                /* Temp. error try again later */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
            case 9:
                /* File not found --> Do not trust this error whenever a multihoster is answering with it */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
            case 10:
                /* Host is not supported or not supported for free account users */
                getMultiHosterManagement().putError(account, this.getDownloadLink(), retrySeconds * 1000l, msg);
            case 11:
                /**
                 * Host (not multihost) is currently under maintenance or offline --> Disable it for some time </br>
                 * 2021-11-08: Admin asked us not to disable host right away when this error happens as it seems like this error is more
                 * rleated to single files/fileservers -> Done accordingly.
                 */
                // mhm.putError(account, this.getDownloadLink(), retrySeconds * 1000l, msg);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
            case 12:
                /* Multihost itself is currently under maintenance --> Temp. disable account for some minutes */
                throw new AccountUnavailableException(msg, retrySeconds * 1000l);
            case 13:
                /* Password required or sent password was wrong --> This should never happen here as upper handling should handle this! */
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            case 14:
                /* Host specific (= account specific) download limit has been reached --> Try file later */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
            case 15:
                /*
                 * Host specific download request limit has been reahed. This is basically the protection of this multihost against users
                 * trying to start a lot of downloads of limited hosts at the same time, trying to exceed the multihosts daily host specific
                 * limits.
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
            case 16:
                /* Error, user is supposed to contact support of this multihost. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
            default:
                /* Unknown/other errorcodes */
                if (link != null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, retrySeconds * 1000l);
                } else {
                    throw new AccountUnavailableException("Unexpected error: " + msg, retrySeconds * 1000l);
                }
            }
        } else {
            /* Handle old style/old API errormessages */
            if (msg.equalsIgnoreCase("UserOrPassInvalid")) {
                throw new AccountInvalidException();
            } else {
                logger.warning("Unknown/Unhandled errormessage: " + msg);
            }
        }
    }

    @Override
    protected int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account != null && link != null) {
            if (isUsenetLink(link)) {
                return account.getIntegerProperty(PROPERTY_ACCOUNT_MAX_DOWNLOADS_USENET, 10);
            } else if (link.getPluginPatternMatcher().matches(PATTERN_DIRECT)) {
                return 5;
            } else {
                /* Look for host specific limit */
                synchronized (getMapLock()) {
                    final Map<String, Integer> hostMaxdlsMap = getMap(HighWayCore.hostMaxdlsMap);
                    if (hostMaxdlsMap.containsKey(link.getHost())) {
                        return hostMaxdlsMap.get(link.getHost());
                    }
                }
            }
        } else if (link != null && link.getPluginPatternMatcher().matches(PATTERN_DIRECT)) {
            /* Last checked: 2021-05-17 */
            return 5;
        } else if (account != null) {
            /* Return max simultan downloads per account. */
            // return account.getMaxSimultanDownloads();
            return account.getIntegerProperty(PROPERTY_ACCOUNT_MAX_DOWNLOADS_ACCOUNT, 10);
        }
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}