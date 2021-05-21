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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.ConditionalSkipReasonException;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 1, names = {}, urls = {})
public abstract class HighWayCore extends UseNet {
    /** General API information: According to admin we can 'hammer' the API every 60 seconds */
    private static MultiHosterManagement          mhm                                 = null;
    private static final String                   TYPE_TV                             = "https?://[^/]+/onlinetv\\.php\\?id=.+";
    private static final String                   TYPE_DIRECT                         = "https?://[^/]+/dlu/[a-z0-9]+/[^/]+";
    private static final int                      STATUSCODE_PASSWORD_NEEDED_OR_WRONG = 13;
    /* Contains <host><Boolean resume possible|impossible> */
    private static HashMap<String, Boolean>       hostResumeMap                       = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>       hostMaxchunksMap                    = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>       hostMaxdlsMap                       = new HashMap<String, Integer>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger> hostRunningDlsNumMap                = new HashMap<String, AtomicInteger>();
    private static HashMap<String, Integer>       hostRabattMap                       = new HashMap<String, Integer>();
    private static Object                         UPDATELOCK                          = new Object();
    private static final int                      defaultMAXCHUNKS                    = -4;
    private static final boolean                  defaultRESUME                       = false;
    protected static final String                 PROPERTY_ACCOUNT_MAXCHUNKS          = "maxchunks";
    protected static final String                 PROPERTY_ACCOUNT_RESUME             = "resume";

    public static interface HighWayMeConfigInterface extends UsenetAccountConfigInterface {
    };

    public HighWayCore(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void init() {
        mhm = new MultiHosterManagement(this.getHost());
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

    /**
     * API docs: https://high-way.me/threads/highway-api.201/ </br>
     * According to admin we can 'hammer' the API every 60 seconds
     */
    protected abstract String getAPIBase();

    protected abstract String getWebsiteBase();

    /** If enabled, apikey is used for login instead of username:password. */
    protected abstract boolean useApikeyLogin();

    @Override
    public void update(final DownloadLink link, final Account account, long bytesTransfered) throws PluginException {
        synchronized (UPDATELOCK) {
            if (hostRabattMap.containsKey(link.getHost())) {
                final Integer rabatt = hostRabattMap.get(link.getHost());
                if (rabatt != null) {
                    bytesTransfered = (bytesTransfered * (100 - rabatt)) / 100;
                }
            }
        }
        super.update(link, account, bytesTransfered);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (isUsenetLink(link)) {
            return super.requestFileInformation(link);
        } else if (link.getPluginPatternMatcher().matches(TYPE_TV)) {
            final boolean check_via_json = true;
            final String dlink = Encoding.urlDecode(link.getPluginPatternMatcher(), true);
            final String fid = new Regex(dlink, "id=(\\d+)").getMatch(0);
            link.setLinkID(this.getHost() + "://" + fid);
            if (!link.isNameSet()) {
                link.setName(fid);
            }
            /* We know that all added content has to be video content. */
            link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            br.setFollowRedirects(true);
            ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                link.getLinkStatus().setStatusText("Only downloadable via account!");
                return AvailableStatus.UNCHECKABLE;
            }
            URLConnectionAdapter con = null;
            long filesize = -1;
            String filesize_str;
            String filename = null;
            /* Use first existant account */
            for (final Account acc : accs) {
                this.login(acc, false);
                if (check_via_json) {
                    final String json_url = link.getPluginPatternMatcher().replaceAll("stream=(?:0|1)", "") + "&json=1";
                    this.br.getPage(json_url);
                    final String code = PluginJSonUtils.getJsonValue(this.br, "code");
                    filename = PluginJSonUtils.getJsonValue(this.br, "name");
                    filesize_str = PluginJSonUtils.getJsonValue(this.br, "size");
                    if ("5".equals(code)) {
                        /* Login issue */
                        return AvailableStatus.UNCHECKABLE;
                    } else if (!"0".equals(code)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (StringUtils.isEmpty(filesize_str) || !filesize_str.matches("\\d+")) {
                        /* This should never happen at this stage! */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    filesize = Long.parseLong(filesize_str);
                    link.setDownloadSize(filesize);
                } else {
                    try {
                        con = br.openHeadConnection(dlink);
                        if (!this.looksLikeDownloadableContent(con) || con.getCompleteContentLength() <= 0) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        } else {
                            filename = getFileNameFromHeader(con);
                            link.setVerifiedFileSize(filesize);
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                    }
                }
                break;
            }
            if (!StringUtils.isEmpty(filename)) {
                link.setFinalFileName(filename);
            }
        } else {
            /* Direct URLs (e.g. cloud stored Usenet downloads) - downloadable even without account. */
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(link.getPluginPatternMatcher());
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (con.getCompleteContentLength() <= 0) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    link.setFinalFileName(getFileNameFromHeader(con));
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
        if (account != null && link.getPluginPatternMatcher().matches(TYPE_DIRECT)) {
            /* This is the only linktype which is downloadable via account */
            return true;
        } else if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            /* Make sure that we do not start more than the allowed number of max simultan downloads for the current host. */
            synchronized (UPDATELOCK) {
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
                            msg = "Download von diesem Hoster aktuell nicht möglich: Zu viele gleichzeitige Downloads";
                        } else {
                            msg = "Downloads of this host are currently not possible: Too many simultaneous downloads";
                        }
                        throw new ConditionalSkipReasonException(new WaitingSkipReason(CAUSE.HOST_TEMP_UNAVAILABLE, 15 * 1000, msg));
                    }
                }
            }
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleSelfhostedDownload(link);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            this.login(account, false);
            handleSelfhostedDownload(link);
        }
    }

    public void handleSelfhostedDownload(final DownloadLink link) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, defaultMAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to file", 1 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST, FEATURE.USENET };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        /*
         * When JD is started the first time and the user starts downloads right away, a full login might not yet have happened but it is
         * needed to get the individual host limits.
         */
        synchronized (UPDATELOCK) {
            if (hostMaxchunksMap.isEmpty() || hostMaxdlsMap.isEmpty()) {
                logger.info("Performing full login to set individual host limits");
                this.fetchAccountInfo(account);
            }
        }
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            mhm.runCheck(account, link);
            boolean resume = account.getBooleanProperty(PROPERTY_ACCOUNT_RESUME, defaultRESUME);
            int maxChunks = account.getIntegerProperty(PROPERTY_ACCOUNT_MAXCHUNKS, defaultMAXCHUNKS);
            final String thishost = link.getHost();
            synchronized (UPDATELOCK) {
                if (hostMaxchunksMap.containsKey(thishost)) {
                    maxChunks = hostMaxchunksMap.get(thishost);
                }
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
                String passCode = Encoding.urlEncode(link.getDownloadPassword());
                int counter = 0;
                do {
                    if (counter > 0) {
                        passCode = getUserInput("Password?", link);
                    }
                    br.getPage(getWebsiteBase() + "load.php?json&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)) + "&pass=" + Encoding.urlEncode(passCode));
                    entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    statuscode = ((Number) entries.get("code")).intValue();
                    if (counter >= 2) {
                        break;
                    } else if (statuscode != STATUSCODE_PASSWORD_NEEDED_OR_WRONG) {
                        break;
                    } else {
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
                this.checkErrors(this.br, account);
                if (entries.containsKey("retry_in_seconds")) {
                    /* File needs to be downloaded to multihost server first --> Retry later and display status */
                    final String statustext = (String) entries.get("for_jd");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, statustext, ((Integer) entries.get("retry_in_seconds")).intValue() * 1000l);
                }
                String dllink = (String) entries.get("download");
                /* Validate URL */
                dllink = new URL(dllink).toString();
                String hash = (String) entries.get("hash");
                if (hash != null) {
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
                        logger.info("Unsupported hash String: " + hash);
                    }
                }
                br.setFollowRedirects(true);
                link.setProperty(this.getHost() + "directlink", dllink);
                br.setAllowedResponseCodes(new int[] { 503 });
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    this.checkErrors(this.br, account);
                    mhm.handleErrorGeneric(account, this.getDownloadLink(), "unknowndlerror", 10, 5 * 60 * 1000l);
                }
            }
            dl.setFilenameFix(true);
            try {
                controlSlot(+1);
                this.dl.startDownload();
            } finally {
                // remove usedHost slot from hostMap
                // remove download slot
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.login(account, true);
        this.getPage(this.getAPIBase() + "?hoster&user");
        this.checkErrors(this.br, account);
        final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final Map<String, Object> accountInfo = (Map<String, Object>) entries.get("user");
        int account_maxdls = ((Number) accountInfo.get("max_connection")).intValue();
        account_maxdls = this.correctMaxdls(account_maxdls);
        final int account_resume = ((Number) accountInfo.get("resume")).intValue();
        final long premiumUntil = ((Number) accountInfo.get("premium_bis")).longValue();
        final long premiumTraffic = ((Number) accountInfo.get("premium_traffic")).longValue();
        final long premiumTrafficMax = ((Number) accountInfo.get("premium_max")).longValue();
        final long trafficLeftToday = ((Long) accountInfo.get("traffic_remain_today")).longValue();
        /* Set account type and account information */
        if (((Boolean) entries.get("premium")).booleanValue() || premiumUntil > 0 && premiumTrafficMax > 0) {
            ai.setTrafficLeft(premiumTraffic);
            ai.setTrafficMax(premiumTrafficMax);
            ai.setValidUntil(premiumUntil * 1000, this.br);
            account.setType(AccountType.PREMIUM);
        } else {
            final long free_traffic_max_daily = ((Number) accountInfo.get("free_traffic")).longValue();
            final long free_traffic_left = ((Number) accountInfo.get("remain_free_traffic")).longValue();
            if (free_traffic_left > free_traffic_max_daily) {
                /* User has more traffic than downloadable daily for free users --> Show max daily traffic. */
                ai.setTrafficLeft(free_traffic_max_daily);
                ai.setTrafficMax(free_traffic_max_daily);
            } else {
                /* User has less traffic (or equal) than downloadable daily for free users --> Show real traffic left. */
                ai.setTrafficLeft(free_traffic_left);
                ai.setTrafficMax(free_traffic_max_daily);
            }
            account.setType(AccountType.FREE);
        }
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            ai.setStatus(account.getType() + " | Traffic übrig heute: " + SizeFormatter.formatBytes(trafficLeftToday));
        } else {
            ai.setStatus(account.getType() + " | Traffic left today: " + SizeFormatter.formatBytes(trafficLeftToday));
        }
        account.setConcurrentUsePossible(true);
        /* Set supported hosts, host specific limits and account limits. */
        account.setProperty(PROPERTY_ACCOUNT_MAXCHUNKS, this.correctChunks(((Number) accountInfo.get("max_chunks")).intValue()));
        account.setMaxSimultanDownloads(account_maxdls);
        if (account_resume == 1) {
            account.setProperty(PROPERTY_ACCOUNT_RESUME, true);
        } else {
            account.setProperty(PROPERTY_ACCOUNT_RESUME, false);
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        synchronized (UPDATELOCK) {
            hostMaxchunksMap.clear();
            hostRabattMap.clear();
            hostMaxdlsMap.clear();
            account.setMaxSimultanDownloads(account_maxdls);
            /* Available hosts are returned by API depending on users' account type e.g. free users have much less supported hosts. */
            final List<Object> array_hoster = (List) entries.get("hoster");
            for (final Object hoster : array_hoster) {
                final Map<String, Object> hoster_map = (Map<String, Object>) hoster;
                final String domain = (String) hoster_map.get("name");
                final String active = (String) hoster_map.get("active");
                final int resume = Integer.parseInt((String) hoster_map.get("resume"));
                final int maxchunks = Integer.parseInt((String) hoster_map.get("chunks"));
                final int maxdls = Integer.parseInt((String) hoster_map.get("downloads"));
                final int rabatt = Integer.parseInt((String) hoster_map.get("rabatt"));
                /* Workaround to find the real domain which we need to assign the properties to later on! */
                final ArrayList<String> supportedHostsTmp = new ArrayList<String>();
                supportedHostsTmp.add(domain);
                ai.setMultiHostSupport(this, supportedHostsTmp);
                final List<String> realDomainList = ai.getMultiHostSupport();
                if (realDomainList == null || realDomainList.isEmpty()) {
                    /* Skip unsupported hosts or host plugins which don't allow multihost usage. */
                    continue;
                }
                final String realDomain = realDomainList.get(0);
                // final String unlimited = (String) hoster_map.get("unlimited");
                hostRabattMap.put(realDomain, rabatt);
                if (active.equals("1")) {
                    supportedHosts.add(realDomain);
                    hostMaxchunksMap.put(realDomain, correctChunks(maxchunks));
                    hostMaxdlsMap.put(realDomain, correctMaxdls(maxdls));
                    if (resume == 0) {
                        hostResumeMap.put(realDomain, false);
                    } else {
                        hostResumeMap.put(realDomain, true);
                    }
                }
            }
        }
        /* Get- and store usenet logindata. These can differ from the logindata the user has added but may as well be equal to those. */
        final Map<String, Object> usenetLogins = (Map<String, Object>) accountInfo.get("usenet");
        if (usenetLogins != null) {
            final String usenetUsername = (String) usenetLogins.get("username");
            final String usenetPassword = (String) usenetLogins.get("pass");
            ai.setProperty("usenetU", usenetUsername);
            ai.setProperty("usenetP", usenetPassword);
        } else {
            supportedHosts.remove("usenet");
            supportedHosts.remove("Usenet");
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    @Override
    protected String getUseNetUsername(final Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            return ai.getStringProperty("usenetU", null);
        } else {
            return null;
        }
    }

    @Override
    protected String getUseNetPassword(final Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            return ai.getStringProperty("usenetP", null);
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
    private boolean login(final Account account, final boolean validateCookies) throws IOException, PluginException, InterruptedException {
        prepBR(this.br);
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            this.br.setCookies(this.getHost(), cookies);
            if (!validateCookies) {
                /* We trust these (new) cookies --> Do not check them */
                logger.info("Trust cookies without check");
                return false;
            } else {
                logger.info("Checking cookies");
                this.br.getPage(this.getAPIBase() + "?logincheck");
                try {
                    /* Don't check for errors here as a failed login can trigger error dialogs which we don't want here! */
                    // this.checkErrors(this.br, account);
                    final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    if (getAPIErrorcode(entries) == -1) {
                        logger.info("Cookie login successful");
                        return true;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCookie login failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } catch (final Exception ignore) {
                    logger.log(ignore);
                    logger.info("Cookie login failed");
                }
            }
        }
        logger.info("Performing full login");
        if (this.useApikeyLogin()) {
            br.postPage(this.getAPIBase() + "?login", "apikey=" + Encoding.urlEncode(account.getPass()));
        } else {
            br.postPage(this.getAPIBase() + "?login", "pass=" + Encoding.urlEncode(account.getPass()) + "&user=" + Encoding.urlEncode(account.getUser()));
        }
        this.checkErrors(this.br, account);
        account.saveCookies(this.br.getCookies(this.br.getHost()), "");
        return true;
    }

    protected abstract void exceptionAccountInvalid(final Account account) throws PluginException;

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
        synchronized (UPDATELOCK) {
            AtomicInteger currentRunningDls = new AtomicInteger(0);
            if (hostRunningDlsNumMap.containsKey(this.getDownloadLink().getHost())) {
                currentRunningDls = hostRunningDlsNumMap.get(this.getDownloadLink().getHost());
            }
            currentRunningDls.set(currentRunningDls.get() + num);
            hostRunningDlsNumMap.put(this.getDownloadLink().getHost(), currentRunningDls);
        }
    }

    private int getAPIErrorcode(final Map<String, Object> entries) {
        if (!entries.containsKey("error")) {
            /* No error */
            return -1;
        } else {
            return ((Number) entries.get("code")).intValue();
        }
    }

    private void checkErrors(final Browser br, final Account account) throws PluginException, InterruptedException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final int code = getAPIErrorcode(entries);
        if (code == -1) {
            /* No error -> We're good :) */
            return;
        }
        String msg = (String) entries.get("error");
        if (StringUtils.isEmpty(msg)) {
            msg = "Unknown error";
        }
        switch (code) {
        case 1:
            /* Invalid logindata */
            this.exceptionAccountInvalid(account);
        case 2:
            /* Session expired (this should never happen) */
            throw new AccountUnavailableException(msg, 3 * 60 * 1000l);
        case 3:
            /* Not enough premium traffic available */
            throw new AccountUnavailableException(msg, 3 * 60 * 1000l);
        case 4:
            /* User requested too many simultaneous downloads */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 30 * 1000l);
        case 5:
            /* Premium package expired --> Temp. deactivate account so next account-check will set correct new account status */
            throw new AccountUnavailableException(msg, 30 * 60 * 1000l);
        case 6:
            /* No- or no valid URL was provided (this should never happen) */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 3 * 60 * 1000l);
        case 7:
            /* There is no case 7 (lol) */
        case 8:
            /* Temp. error try again later */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 1 * 60 * 1000l);
        case 9:
            /* File not found --> Do not trust this error whenever a multihoster is answering with it */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 1 * 60 * 1000l);
        case 10:
            /* Host is not supported or not supported for free account users */
            mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, msg);
        case 11:
            /* Host (not multihost) is currently under maintenance or offline --> Disable it for some time */
            mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, msg);
        case 12:
            /* Multihost itself is currently under maintenance --> Temp. disable account for some minutes */
            throw new AccountUnavailableException(msg, 5 * 60 * 1000l);
        case 13:
            /* Password required or sent password was wrong --> This should never happen here as upper handling should handle this! */
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        case 14:
            /* Host specific (= account specific) download limit has been reached --> Disable that particular host for some time */
            mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, msg);
        case 15:
            /*
             * Host specific download request limit has been reahed. This is basically the protection of this multihost against users trying
             * to start a lot of downloads of limited hosts at the same time, trying to exceed the multihosts daily host specific limits.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 3 * 60 * 1000l);
        case 16:
            /* Error, user is supposed to contact support of this multihost. */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 3 * 60 * 1000l);
        default:
            /* Treat unknown/other erors as account errors */
            throw new AccountUnavailableException("Unexpected error: " + msg, 5 * 60 * 1000l);
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account != null) {
            if (isUsenetLink(link)) {
                return 5;
            } else {
                if (link == null) {
                    return account.getMaxSimultanDownloads();
                } else if (link.getPluginPatternMatcher().matches(TYPE_DIRECT)) {
                    return 5;
                } else {
                    synchronized (UPDATELOCK) {
                        if (hostMaxdlsMap.containsKey(link.getHost())) {
                            return hostMaxdlsMap.get(link.getHost());
                        }
                    }
                }
            }
        } else if (link != null && link.getPluginPatternMatcher().matches(TYPE_DIRECT)) {
            /* Last checked: 2021-05-17 */
            return 5;
        }
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}