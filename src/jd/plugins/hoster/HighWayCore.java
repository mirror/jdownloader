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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.ConditionalSkipReasonException;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
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
    // private static final String API_BASE = "http://http.high-way.me/api.php";
    private static MultiHosterManagement          mhm                                 = new MultiHosterManagement("high-way.me");
    private static final String                   TYPE_TV                             = ".+high\\-way\\.me/onlinetv\\.php\\?id=.+";
    private static final String                   TYPE_DIRECT                         = ".+high\\-way\\.me/dlu/[a-z0-9]+/[^/]+";
    private static final int                      STATUSCODE_PASSWORD_NEEDED_OR_WRONG = 13;
    // private static final long trust_cookie_age = 300000l;
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

    public static interface HighWayMeConfigInterface extends UsenetAccountConfigInterface {
    };

    public HighWayCore(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* TODO */
    // @Override
    // public String getAGBLink() {
    // return "https://high-way.me/help/terms";
    // }

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
    private String getAPIBase() {
        return "https://" + this.getHost() + "/apiV2.php";
    }

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
            final String linkid = new Regex(dlink, "id=(\\d+)").getMatch(0);
            link.setName(linkid);
            link.setLinkID(this.getHost() + "://" + linkid);
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
                        if (this.looksLikeDownloadableContent(con)) {
                            filesize = con.getCompleteContentLength();
                            if (filesize <= 0) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            } else {
                                filename = getFileNameFromHeader(con);
                                link.setVerifiedFileSize(filesize);
                            }
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
                }
                if (con.getCompleteContentLength() <= 0) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                link.setFinalFileName(getFileNameFromHeader(con));
                link.setVerifiedFileSize(con.getCompleteContentLength());
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
                        throw new ConditionalSkipReasonException(new WaitingSkipReason(CAUSE.HOST_TEMP_UNAVAILABLE, 15 * 1000, null));
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
                    br.getPage("https://" + this.getHost() + "/load.php?json&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)) + "&pass=" + Encoding.urlEncode(passCode));
                    entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    statuscode = ((Number) entries.get("code")).intValue();
                    counter++;
                } while (statuscode == STATUSCODE_PASSWORD_NEEDED_OR_WRONG && counter <= 2);
                if (statuscode == STATUSCODE_PASSWORD_NEEDED_OR_WRONG) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else if (passCode != null) {
                    /* Password has been entered correctly or previously given password was correct --> Save it */
                    link.setDownloadPassword(passCode);
                }
                final String dllink = (String) entries.get("download");
                String hash = (String) entries.get("size");
                if (hash != null && hash.matches("md5:[a-f0-9]{32}")) {
                    hash = hash.substring(hash.lastIndexOf(":") + 1);
                    logger.info("Set hash given by multihost: " + hash);
                    link.setMD5Hash(hash);
                }
                if (dllink == null) {
                    logger.warning("Final downloadlink is null");
                    mhm.handleErrorGeneric(account, this.getDownloadLink(), "dllinknull", 50, 5 * 60 * 1000l);
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
    // private final String checkDirectLink(final DownloadLink link, final String property) {
    // String dllink = link.getStringProperty(property);
    // if (dllink != null) {
    // URLConnectionAdapter con = null;
    // try {
    // final Browser br2 = br.cloneBrowser();
    // br2.setFollowRedirects(true);
    // con = br2.openHeadConnection(dllink);
    // if (this.looksLikeDownloadableContent(con)) {
    // return dllink;
    // } else {
    // throw new IOException();
    // }
    // } catch (final Exception e) {
    // logger.log(e);
    // return null;
    // } finally {
    // if (con != null) {
    // con.disconnect();
    // }
    // }
    // }
    // return null;
    // }

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
        final long premium_traffic = ((Number) accountInfo.get("premium_traffic")).longValue();
        final long premium_traffic_max = ((Number) accountInfo.get("premium_max")).longValue();
        /* Set account type and account information */
        if (((Integer) accountInfo.get("premium")).intValue() == 1 || premiumUntil > 0 && premium_traffic_max > 0) {
            ai.setTrafficLeft(premium_traffic);
            ai.setTrafficMax(premium_traffic_max);
            ai.setValidUntil(premiumUntil * 1000, this.br);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
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
            ai.setStatus("Registered (free) account");
        }
        account.setConcurrentUsePossible(true);
        /* Set supported hosts, limits and account limits */
        account.setProperty("account_maxchunks", this.correctChunks(((Number) accountInfo.get("max_chunks")).intValue()));
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
            final List<Object> array_hoster = (List) entries.get("hoster");
            for (final Object hoster : array_hoster) {
                final Map<String, Object> hoster_map = (Map<String, Object>) hoster;
                final String domain = (String) hoster_map.get("name");
                final String active = (String) hoster_map.get("active");
                final int resume = Integer.parseInt((String) hoster_map.get("resume"));
                final int maxchunks = Integer.parseInt((String) hoster_map.get("chunks"));
                final int maxdls = Integer.parseInt((String) hoster_map.get("downloads"));
                final int rabatt = Integer.parseInt((String) hoster_map.get("rabatt"));
                /* Workaround to find the real domain. */
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
        }
        return null;
    }

    @Override
    protected String getUseNetPassword(final Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            return ai.getStringProperty("usenetP", null);
        }
        return null;
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
                    this.checkErrors(this.br, account);
                    logger.info("Cookie login successful");
                    // return true;
                } catch (final PluginException ignore) {
                    logger.info("Cookie login failed");
                }
            }
        }
        logger.info("Performing full login");
        br.postPage(this.getAPIBase() + "?login", "pass=" + Encoding.urlEncode(account.getPass()) + "&user=" + Encoding.urlEncode(account.getUser()));
        this.checkErrors(this.br, account);
        account.saveCookies(this.br.getCookies(this.br.getHost()), "");
        return true;
    }

    protected void exceptionAccountInvalid(final Account account) throws PluginException {
        /* TODO: Remove the note to disable 2FA: The new API can also be used while 2FA is enabled! */
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Falls du die 2-Faktor-Authentifizierung aktiviert hast, deaktiviere diese und versuche es erneut.\r\n3. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. If you have 2-factor-authentication enabled, disable it and try again.\r\n3. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    private void checkErrors(final Browser br, final Account account) throws PluginException, InterruptedException {
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if (entries.containsKey("retry_in_seconds")) {
            final String statustext = PluginJSonUtils.getJson(br, "for_jd");
            // final String waitHeader = br.getRequest().getResponseHeader("Retry-After");
            final String retry_in_seconds = PluginJSonUtils.getJson(br, "retry_in_seconds");
            if (retry_in_seconds != null && retry_in_seconds.matches("\\d+") && statustext != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File in queue: " + statustext, Long.parseLong(retry_in_seconds) * 1000l);
            } else {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File in queue", 60 * 1000l);
            }
        } else if (!entries.containsKey("error")) {
            /* No error -> We're good :) */
            return;
        }
        final int code = ((Number) entries.get("code")).intValue();
        String err = (String) entries.get("error");
        if (StringUtils.isEmpty(err)) {
            err = "Unknown error";
        }
        /* TODO: Implement list of errors accordingly */
        switch (code) {
        case 1:
            this.exceptionAccountInvalid(account);
        case 13:
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        default:
            throw new AccountUnavailableException("Unknown error: " + err, 5 * 60 * 1000l);
        }
        // if (err.equalsIgnoreCase("NotLoggedIn")) {
        // throw new AccountInvalidException(err);
        // } else if (err.equalsIgnoreCase("UserOrPassInvalid")) {
        // throw new AccountInvalidException(err);
        // } else {
        // /* Unknown error */
        // throw new AccountInvalidException(err);
        // }
        /* TODO: Add errorhandling */
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
            return 5;
        }
        return 1;
    }

    /** According to High-Way staff, Usenet SSL is unavailable since 2017-08-01 */
    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.high-way.me", false, 119));
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