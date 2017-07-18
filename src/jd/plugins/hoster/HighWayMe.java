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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "high-way.me" }, urls = { "https?://high\\-way\\.me/onlinetv\\.php\\?id=\\d+[^/]+" })
public class HighWayMe extends UseNet {
    /** General API information: According to admin we can 'hammer' the API every 60 seconds */
    private static final String                            DOMAIN                              = "http://http.high-way.me/api.php";
    private static final String                            NICE_HOST                           = "high-way.me";
    private static final String                            NICE_HOSTproperty                   = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            NORESUME                            = NICE_HOSTproperty + "NORESUME";
    private static final int                               ERRORHANDLING_MAXLOGINS             = 2;
    private static final int                               STATUSCODE_PASSWORD_NEEDED_OR_WRONG = 13;
    private static final long                              trust_cookie_age                    = 300000l;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap                  = new HashMap<Account, HashMap<String, Long>>();
    /* Contains <host><Boolean resume possible|impossible> */
    private static HashMap<String, Boolean>                hostResumeMap                       = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>                hostMaxchunksMap                    = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>                hostMaxdlsMap                       = new HashMap<String, Integer>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger>          hostRunningDlsNumMap                = new HashMap<String, AtomicInteger>();
    private static HashMap<String, Integer>                hostRabattMap                       = new HashMap<String, Integer>();
    private static Object                                  UPDATELOCK                          = new Object();
    /* Last updated: 31.03.15 */
    private static final int                               defaultMAXDOWNLOADS                 = 10;
    private static final int                               defaultMAXCHUNKS                    = -4;
    private static final boolean                           defaultRESUME                       = false;
    private int                                            statuscode                          = 0;
    private Account                                        currAcc                             = null;
    private DownloadLink                                   currDownloadLink                    = null;
    private long                                           currentWaittimeOnFailue             = 0;

    public static interface HighWayMeConfigInterface extends UsenetAccountConfigInterface {
    };

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
    public void update(final DownloadLink downloadLink, final Account account, long bytesTransfered) throws PluginException {
        synchronized (UPDATELOCK) {
            final String currentHost = this.correctHost(downloadLink.getHost());
            final Integer rabatt = hostRabattMap.get(currentHost);
            if (rabatt != null) {
                bytesTransfered = (bytesTransfered * (100 - rabatt)) / 100;
            }
        }
        super.update(downloadLink, account, bytesTransfered);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (isUsenetLink(link)) {
            return super.requestFileInformation(link);
        } else {
            final boolean check_via_json = true;
            final String dlink = Encoding.urlDecode(link.getDownloadURL(), true);
            final String linkid = new Regex(dlink, "id=(\\d+)").getMatch(0);
            link.setName(linkid);
            link.setLinkID(linkid);
            link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            br.setFollowRedirects(true);
            ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                link.getLinkStatus().setStatusText("Only downlodable via account!");
                return AvailableStatus.UNCHECKABLE;
            }
            URLConnectionAdapter con = null;
            long filesize = -1;
            String filesize_str;
            String filename = null;
            for (Account acc : accs) {
                this.currAcc = acc;
                this.loginSafe(false);
                if (check_via_json) {
                    final String json_url = link.getDownloadURL().replaceAll("stream=(?:0|1)", "") + "&json=1";
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
                    if (StringUtils.isEmpty(filename) || StringUtils.isEmpty(filesize_str) || !filesize_str.matches("\\d+")) {
                        /* This should never happen at this stage! */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    filesize = Long.parseLong(filesize_str);
                    break;
                } else {
                    try {
                        con = br.openHeadConnection(dlink);
                        if (!con.getContentType().contains("html")) {
                            filesize = con.getLongContentLength();
                            if (filesize <= 0) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            filename = getFileNameFromHeader(con);
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                    }
                    break;
                }
            }
            if (filesize > -1) {
                link.setDownloadSize(filesize);
            }
            /* 2017-05-18: Even via json API, filenames are often html encoded --> Fix that */
            filename = Encoding.htmlDecode(filename);
            link.setFinalFileName(filename);
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        /* Make sure that we do not start more than the allowed number of max simultan downloads for the current host. */
        synchronized (UPDATELOCK) {
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
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.setConstants(account, link);
        this.loginSafe(false);
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, defaultMAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 30 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                this.br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 60 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        boolean resume = account.getBooleanProperty("resume", defaultRESUME);
        int maxChunks = account.getIntegerProperty("account_maxchunks", defaultMAXCHUNKS);
        final String thishost = link.getHost();
        synchronized (UPDATELOCK) {
            if (hostMaxchunksMap.containsKey(thishost)) {
                maxChunks = hostMaxchunksMap.get(thishost);
            }
            if (hostResumeMap.containsKey(thishost)) {
                resume = hostResumeMap.get(thishost);
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
            link.setProperty(HighWayMe.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
        return new FEATURE[] { FEATURE.MULTIHOST, FEATURE.USENET };
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
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
        this.setConstants(account, link);
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else {
            synchronized (UPDATELOCK) {
                final HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                if (unavailableMap != null) {
                    final Long lastUnavailable = unavailableMap.get(link.getHost());
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
            String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
            if (dllink == null) {
                /* request creation of downloadlink */
                br.setFollowRedirects(true);
                String passCode = Encoding.urlEncode(link.getStringProperty("pass", ""));
                postAPISafe(DOMAIN + "?login", "pass=" + Encoding.urlEncode(account.getPass()) + "&user=" + Encoding.urlEncode(account.getUser()));
                this.getAPISafe("http://http.high-way.me/load.php?json&link=" + Encoding.urlEncode(link.getDownloadURL()) + "&pass=" + Encoding.urlEncode(passCode));
                if (this.statuscode == STATUSCODE_PASSWORD_NEEDED_OR_WRONG) {
                    /* We alredy tried the saved password --> Ask for PW now */
                    logger.info("MOCH and download password ...");
                    passCode = Plugin.getUserInput("Password?", link);
                    this.getAPISafe("/load.php?json&link=" + Encoding.urlEncode(link.getDownloadURL()) + "&pass=" + Encoding.urlEncode(passCode));
                    if (this.statuscode == STATUSCODE_PASSWORD_NEEDED_OR_WRONG) {
                        link.setProperty("pass", Property.NULL);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                    }
                    /* Seems like the password is valid --> Save it */
                    link.setProperty("pass", passCode);
                }
                dllink = PluginJSonUtils.getJsonValue(br, "download");
                if (dllink == null) {
                    logger.warning("Final downloadlink is null");
                    handleErrorRetries("dllinknull", 10, 60 * 60 * 1000l);
                }
                dllink = Encoding.htmlDecode(dllink);
            }
            handleDL(account, link, dllink);
        }
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
        this.login(true);
        getAPISafe(DOMAIN + "?hoster&user");
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final LinkedHashMap<String, Object> info_account = (LinkedHashMap<String, Object>) entries.get("user");
        final ArrayList<Object> array_hoster = (ArrayList) entries.get("hoster");
        final int account_maxchunks = ((Number) info_account.get("max_chunks")).intValue();
        int account_maxdls = ((Number) info_account.get("max_connection")).intValue();
        account_maxdls = this.correctMaxdls(account_maxdls);
        final int account_resume = ((Number) info_account.get("resume")).intValue();
        /* TODO: Real traffic is missing. */
        final long free_traffic_max_daily = ((Number) info_account.get("free_traffic")).longValue();
        long free_traffic_left = ((Number) info_account.get("remain_free_traffic")).longValue();
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
            if (free_traffic_left > free_traffic_max_daily) {
                /* User has more traffic than downloadable daily for free users --> Show max daily traffic. */
                ai.setTrafficLeft(free_traffic_max_daily);
                ai.setTrafficMax(free_traffic_max_daily);
            } else {
                /* User has less traffic than downloadable daily for free users --> Show real traffic left. */
                ai.setTrafficLeft(free_traffic_left);
                ai.setTrafficMax(free_traffic_max_daily);
            }
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        /* Set supported hosts, limits and account limits */
        account.setProperty("account_maxchunks", this.correctChunks(account_maxchunks));
        account.setMaxSimultanDownloads(account_maxdls);
        if (account_resume == 1) {
            account.setProperty("resume", true);
        } else {
            account.setProperty("resume", false);
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        synchronized (UPDATELOCK) {
            hostMaxchunksMap.clear();
            hostRabattMap.clear();
            hostMaxdlsMap.clear();
            account.setMaxSimultanDownloads(account_maxdls);
            for (final Object hoster : array_hoster) {
                final LinkedHashMap<String, Object> hoster_map = (LinkedHashMap<String, Object>) hoster;
                final String domain = correctHost((String) hoster_map.get("name"));
                final String active = (String) hoster_map.get("active");
                final int resume = Integer.parseInt((String) hoster_map.get("resume"));
                final int maxchunks = Integer.parseInt((String) hoster_map.get("chunks"));
                final int maxdls = Integer.parseInt((String) hoster_map.get("downloads"));
                final int rabatt = Integer.parseInt((String) hoster_map.get("rabatt"));
                // final String unlimited = (String) hoster_map.get("unlimited");
                hostRabattMap.put(domain, rabatt);
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
        }
        final Map<String, Object> usenetLogins = (Map<String, Object>) info_account.get("usenet");
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
    protected String getUsername(Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            return ai.getStringProperty("usenetU", null);
        }
        return null;
    }

    @Override
    protected String getPassword(Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            return ai.getStringProperty("usenetP", null);
        }
        return null;
    }

    /**
     * Login without errorhandling
     *
     * @throws PluginException
     */
    private void login(final boolean force) throws IOException, PluginException {
        final Cookies cookies = this.currAcc.loadCookies("");
        if (cookies != null && !force) {
            this.br.setCookies(this.getHost(), cookies);
            if (System.currentTimeMillis() - this.currAcc.getCookiesTimeStamp("") <= trust_cookie_age) {
                /* We trust these cookies --> Do not check them */
                return;
            }
            this.br.getPage(DOMAIN + "?logincheck");
            if ("true".equals(PluginJSonUtils.getJsonValue(this.br, "loggedin"))) {
                /* Cookies valid? --> Save them again to renew the last-saved timestamp. */
                this.currAcc.saveCookies(this.br.getCookies(this.br.getHost()), "");
                return;
            }
            /* Cookies not valid anymore --> Perform full login */
        }
        br.postPage(DOMAIN + "?login", "pass=" + Encoding.urlEncode(this.currAcc.getPass()) + "&user=" + Encoding.urlEncode(this.currAcc.getUser()));
        if (br.getCookie(br.getURL(), "xf_user") == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        this.currAcc.saveCookies(this.br.getCookies(this.br.getHost()), "");
    }

    /** Login + errorhandling */
    private void loginSafe(final boolean force) throws IOException, PluginException {
        login(force);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private String getXML(final String key) {
        return br.getRegex("<" + key + ">([^<>\"]*?)</" + key + ">").getMatch(0);
    }

    private void tempUnavailableHoster(long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        if (this.currentWaittimeOnFailue > 0) {
            /* API timeout can override default timeout */
            timeout = this.currentWaittimeOnFailue;
        }
        synchronized (UPDATELOCK) {
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

    /**
     * Performs full logins on errorcode 9 up to ERRORHANDLING_MAXLOGINS-times, hopefully avoiding login/cookie problems.
     *
     * @throws PluginException
     */
    private void handleLoginIssues() throws IOException, PluginException {
        updatestatuscode();
        if (this.statuscode == 9) {
            this.login(true);
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
     */
    private void controlSlot(final int num) {
        synchronized (UPDATELOCK) {
            final String currentHost = correctHost(this.currDownloadLink.getHost());
            AtomicInteger currentRunningDls = new AtomicInteger(0);
            if (hostRunningDlsNumMap.containsKey(currentHost)) {
                currentRunningDls = hostRunningDlsNumMap.get(currentHost);
            }
            currentRunningDls.set(currentRunningDls.get() + num);
            hostRunningDlsNumMap.put(currentHost, currentRunningDls);
        }
    }

    /**
     * 0 = everything ok, 1-99 = official errorcodes, 100-199 = login-errors, 200-299 = info-states, 666 = hell
     */
    private void updatestatuscode() {
        final String waittime_on_failure = PluginJSonUtils.getJsonValue(br, "timeout");
        /* First look for errorcode */
        String error = PluginJSonUtils.getJsonValue(br, "code");
        if (error == null) {
            /* No errorcode? Look for errormessage (e.g. used in login function). */
            error = PluginJSonUtils.getJsonValue(br, "error");
        }
        final String info = PluginJSonUtils.getJsonValue(br, "info");
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
        } else if ("Traffic ist kleiner als 10%".equals(info)) {
            statuscode = 200;
        } else {
            statuscode = 0;
        }
        if (waittime_on_failure != null && waittime_on_failure.matches("\\d+")) {
            this.currentWaittimeOnFailue = Long.parseLong(waittime_on_failure);
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
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nYour account was banned!";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 2:
                statusMessage = "Not enough free traffic";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 3:
                statusMessage = "Not enough premium traffic";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 4:
                /* Too many simultaneous downloads */
                statusMessage = "Too many simultaneous downloads";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, statusMessage, 1 * 60 * 1000l);
            case 5:
                /* Login or password missing -> disable account */
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nCode 5: Login Fehler";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nCode 5: Login failure";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 6:
                /* Invalid link --> Disable host */
                statusMessage = "Invalid link";
                tempUnavailableHoster(5 * 60 * 1000l);
            case 7:
                statusMessage = "Undefined errorstate";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Undefined errorstate");
            case 8:
                /* Temp error, try again in some minutes */
                statusMessage = "Temporary error";
                tempUnavailableHoster(1 * 60 * 1000l);
            case 9:
                /* No account found -> Disable link for 10 minutes */
                statusMessage = "No account found";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
            case 10:
                /* Host offline or invalid url -> Remove host from array of supported hosts */
                statusMessage = "Invalid link --> Probably unsupported host";
                tempUnavailableHoster(10 * 60 * 1000l);
            case 11:
                /* Host itself is currently unavailable (maintenance) -> Disable host */
                statusMessage = "Host itself is currently unavailable";
                tempUnavailableHoster(10 * 60 * 1000l);
            case 12:
                /* MOCH itself is under maintenance */
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nDieser Anbieter führt momentan Wartungsarbeiten durch!";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    statusMessage = "\r\nThis service is doing maintenance work!";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            case 13:
                /* Download password for filehost needed - this should be handled via upper code - do not do anything here! */
                break;
            case 14:
                /*
                 * Host-specified traffic limit reached e.g. traffic for keep2share.cc is empty but account still has traffic left for other
                 * hosts.
                 */
                statusMessage = "Host specified traffic limit has been reached";
                tempUnavailableHoster(10 * 60 * 1000l);
            case 100:
                /* Login or password missing -> disable account */
                if ("de".equalsIgnoreCase(lang)) {
                    statusMessage = "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case 200:
                if (!org.appwork.utils.Application.isHeadless()) {
                    BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
                        @Override
                        public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                            return new BasicNotify("Weniger als 10% Traffic verbleibend", "Weniger als 10% Traffic verbleibend", new AbstractIcon(IconKey.ICON_INFO, 32));
                        }
                    });
                }
            case 666:
                /* Unknown error */
                statusMessage = "Unknown error";
                logger.info(NICE_HOST + ": Unknown API error");
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknown_api_error", 10, 5 * 60 * 1000l);
            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        if (account != null) {
            if (isUsenetLink(link)) {
                return 5;
            } else {
                if (link == null) {
                    return account.getMaxSimultanDownloads();
                } else {
                    final String currentHost = correctHost(link.getHost());
                    synchronized (UPDATELOCK) {
                        if (hostMaxdlsMap.containsKey(currentHost)) {
                            return hostMaxdlsMap.get(currentHost);
                        }
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.high-way.me", true, 563));
        return ret;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}