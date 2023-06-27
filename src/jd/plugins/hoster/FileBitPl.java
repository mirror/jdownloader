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
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filebit.pl" }, urls = { "" })
public class FileBitPl extends PluginForHost {
    private static final String          APIKEY   = "YWI3Y2E2NWM3OWQxYmQzYWJmZWU3NTRiNzY0OTM1NGQ5ODI3ZjlhNmNkZWY3OGE1MjQ0ZjU4NmM5NTNiM2JjYw==";
    private static final String          API_BASE = "https://filebit.pl/api/index.php";
    /*
     * 2018-02-13: Their API is broken and only returns 404. Support did not respond which is why we now have website- and API support ...
     */
    private static final boolean         USE_API  = true;
    private static MultiHosterManagement mhm      = new MultiHosterManagement("filebit.pl");

    public FileBitPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://filebit.pl/oferta");
    }

    @Override
    public String getAGBLink() {
        return "https://filebit.pl/regulamin";
    }

    private Browser prepBrowserAPI(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setAllowedResponseCodes(new int[] { 401, 204, 403, 404, 497, 500, 503 });
        br.setFollowRedirects(true);
        return br;
    }

    private Browser prepBrowserWebsite(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* this should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        /*
         * 2019-08-17: It is especially important to try to re-use generated downloadurls in this case because they will charge the complete
         * traffic of a file once you add an URL to their download-queue.
         */
        String dllink = checkDirectLink(link, "filebitpldirectlink");
        if (StringUtils.isEmpty(dllink)) {
            /* request Download */
            dllink = getDllink(account, link);
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 50);
            }
        }
        br.setCurrentURL(null);
        int maxChunks = (int) account.getLongProperty("filebitpl_maxconnections", 1);
        if (maxChunks > 1) {
            maxChunks = -maxChunks;
        }
        link.setProperty("filebitpldirectlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), maxChunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                mhm.handleErrorGeneric(account, link, "403dlerror", 20);
            } else if (br.containsHTML("<title>FileBit\\.pl \\- Error</title>")) {
                mhm.handleErrorGeneric(account, link, "dlerror_known_but_unsure", 20);
            } else {
                mhm.handleErrorGeneric(account, link, "dlerror_unknown", 50);
            }
        }
        this.dl.startDownload();
    }

    private String getDllink(final Account account, final DownloadLink link) throws Exception {
        final String dllink;
        if (USE_API) {
            dllink = getDllinkAPI(account, link);
        } else {
            dllink = getDllinkWebsite(account, link);
        }
        return dllink;
    }

    private String getDllinkAPI(final Account account, final DownloadLink link) throws Exception {
        this.loginAPI(account);
        long total_numberof_waittime_loops_for_current_fileID = 0;
        final long max_total_numberof_waittime_loops_for_current_fileID = 200;
        String fileID = link.getStringProperty("filebitpl_fileid");
        final String sessionid = this.getSessionID(account);
        if (!StringUtils.isEmpty(fileID)) {
            /*
             * 2019-08-19: If a downloadlink has previously been created successfully with that fileID and upper handling fails to re-use
             * that generated directURL, re-using the old fileID to generate a new downloadurl will not use up any traffic of the users'
             * account either!!
             */
            logger.info("Found saved fileID: " + fileID);
            logger.info("--> We've tried to download this URL before --> Trying to re-use old fileID as an attempt not to waste any traffic (this multihoster has a bad traffic-counting-system)");
            total_numberof_waittime_loops_for_current_fileID = link.getLongProperty("filebitpl_fileid_total_numberof_waittime_loops", 0);
            logger.info("Stored fileID already went through serverside-download-loops in the past for x-times: " + total_numberof_waittime_loops_for_current_fileID);
        } else {
            /* This initiates a serverside download. The user will be charged for the full amount of traffic immediately! */
            logger.info("Failed to find any old fileID --> Initiating a new serverside queue-download");
            br.getPage(API_BASE + "?a=addNewFile&sessident=" + sessionid + "&url=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            final Map<String, Object> resp = handleAPIErrors(br, account, link);
            final Map<String, Object> data = (Map<String, Object>) resp.get("data");
            /* Serverside fileID which refers to the current serverside download-process. */
            fileID = data.get("fileId").toString();
            if (StringUtils.isEmpty(fileID)) {
                /* This should never happen */
                mhm.handleErrorGeneric(account, link, "Failed to find fileID", 20);
            }
            /* Save this ID so that in case of an error or aborted downloads we can re-use it without wasting the users' traffic. */
            link.setProperty("filebitpl_fileid", fileID);
        }
        int loop = 0;
        int lastProgressValue = 0;
        int timesWithoutProgressChange = 0;
        final int timesWithoutProgressChangeMax = 10;
        boolean serverDownloadFinished = false;
        Map<String, Object> resp = null;
        do {
            br.getPage(API_BASE + "?a=checkFileStatus&sessident=" + sessionid + "&fileId=" + fileID);
            try {
                resp = handleAPIErrors(br, account, link);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    link.removeProperty("filebitpl_fileid");
                    mhm.handleErrorGeneric(account, link, "Cached fileID is not available anymore", 20);
                } else {
                    throw e;
                }
            }
            final int status = ((Number) resp.get("status")).intValue();
            serverDownloadFinished = status == 3;
            if (status == 3) {
                serverDownloadFinished = true;
                break;
            } else {
                /* Progress is only available when status == 2 */
                int progressPercentTemp = 0;
                final Object progressO = resp.get("progress");
                String info = resp.get("info").toString();
                info += ": %s%%";
                final String progressHumanReadable;
                if (progressO != null && progressO.toString().matches("\\d+")) {
                    final String progressStr = progressO.toString();
                    progressPercentTemp = Integer.parseInt(progressStr);
                    progressHumanReadable = progressStr;
                } else {
                    progressHumanReadable = "?";
                }
                if (progressPercentTemp == lastProgressValue) {
                    timesWithoutProgressChange++;
                    logger.info("No progress change in loop number: " + timesWithoutProgressChange);
                } else {
                    lastProgressValue = progressPercentTemp;
                    /* Reset counter */
                    timesWithoutProgressChange = 0;
                }
                info = String.format(info, progressHumanReadable);
                sleep(5000l, link, info);
                loop++;
            }
            if (timesWithoutProgressChange >= timesWithoutProgressChangeMax) {
                logger.info("No progress change in " + timesWithoutProgressChangeMax + " loops: Giving up");
                break;
            }
            logger.info("total_numberof_waittime_loops_for_current_fileID: " + total_numberof_waittime_loops_for_current_fileID);
            if (total_numberof_waittime_loops_for_current_fileID >= max_total_numberof_waittime_loops_for_current_fileID) {
                /*
                 * Too many attempts for current fileID --> Next retry will create a new fileID which will charge the full amount of traffic
                 * for this file (again).
                 */
                logger.info("Reached max_total_numberof_waittime_loops_for_current_fileID --> Resetting fileid and filebitpl_fileid_total_numberof_waittime_loops for future retries");
                logger.info("Next attempt will charge traffic again");
                link.removeProperty("filebitpl_fileid");
                link.removeProperty("filebitpl_fileid_total_numberof_waittime_loops");
            }
            /*
             * Only display this logger and save filebitpl_fileid_total_numberof_waittime_loops if we did over 0 loops for this
             * download-attempt!
             */
            link.setProperty("filebitpl_fileid_total_numberof_waittime_loops", total_numberof_waittime_loops_for_current_fileID);
            total_numberof_waittime_loops_for_current_fileID++;
        } while (!serverDownloadFinished && loop <= 200);
        if (!serverDownloadFinished) {
            /* Rare case */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server transfer retry count exhausted", 5 * 60 * 1000l);
        }
        // final String expires = getJson("expires");
        link.setProperty("filebitpl_resumable", resp.get("resume"));
        link.setProperty("filebitpl_maxconnections", resp.get("chunks"));
        return resp.get("downloadurl").toString();
    }

    /**
     * 2019-08-19: Keep in mind: This may waste traffic as it is not (yet) able to re-use previously generated "filebit.pl fileIDs".</br>
     * DO NOT USE THIS AS LONG AS THEIR API IS WORKING FINE!!!
     */
    private String getDllinkWebsite(final Account account, final DownloadLink link) throws Exception {
        this.loginWebsite(account);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // do not change to new method, admin is working on new api
        br.postPage("/includes/ajax.php", "a=serverNewFile&url=" + Encoding.urlEncode(link.getDownloadURL()) + "&t=" + System.currentTimeMillis());
        final List<Object> ressourcelist = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        Map<String, Object> entries = (Map<String, Object>) ressourcelist.get(0);
        entries = (Map<String, Object>) entries.get("array");
        // final String downloadlink_expires = (String) entries.get("expire");
        // final String internal_id = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
        return (String) entries.get("download");
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return link.getBooleanProperty("filebitpl_resumable", false);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.prepBrowserAPI(br);
        if (USE_API) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginAPI(account);
        br.getPage(API_BASE + "?a=accountStatus&sessident=" + this.getSessionID(account));
        final Map<String, Object> resp = handleAPIErrors(br, account, null);
        final Map<String, Object> data = (Map<String, Object>) resp.get("data");
        final String accountDescription = data.get("acctype").toString();
        final String premium = data.get("premium").toString();
        final Number expires = (Number) data.get("expires");
        if (expires != null) {
            ai.setValidUntil(System.currentTimeMillis() + expires.longValue());
        }
        final Number transferLeftBytes = (Number) data.get("transferLeft");
        if (transferLeftBytes != null) {
            ai.setTrafficLeft(transferLeftBytes.longValue());
        } else {
            ai.setUnlimitedTraffic();
        }
        final Number maxSimultanDlsO = (Number) data.get("maxsin");
        int maxSimultanDls = maxSimultanDlsO != null ? maxSimultanDlsO.intValue() : -1;
        if (maxSimultanDls < 1) {
            maxSimultanDls = 1;
        } else if (maxSimultanDls > 20) {
            maxSimultanDls = 0;
        }
        account.setMaxSimultanDownloads(maxSimultanDls);
        long maxChunks = ((Number) data.get("maxcon")).intValue();
        if (maxChunks > 1) {
            maxChunks = -maxChunks;
        }
        account.setProperty("filebitpl_maxconnections", maxChunks);
        br.getPage(API_BASE + "?a=getHostList");
        final Map<String, Object> resp2 = handleAPIErrors(br, account, null);
        final List<Map<String, Object>> domaininfos = (List<Map<String, Object>>) resp2.get("data");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final Map<String, Object> domaininfo : domaininfos) {
            final List<String> domains = (List<String>) domaininfo.get("hostdomains");
            for (final String domain : domains) {
                supportedHosts.add(domain);
            }
        }
        if (!"1".equals(premium)) {
            account.setType(AccountType.FREE);
        } else {
            account.setType(AccountType.PREMIUM);
        }
        if (!StringUtils.isEmpty(accountDescription)) {
            ai.setStatus(accountDescription);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Deprecated
    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        prepBrowserWebsite(br);
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account);
        br.getPage("/wykaz");
        account.setConcurrentUsePossible(true);
        final boolean isPremium = br.containsHTML("(?i)KONTO\\s*<span>\\s*PREMIUM\\s*</span>");
        if (!isPremium) {
            throw new AccountInvalidException("Unsupported account type!");
        }
        long timeleftMilliseconds = 0;
        final Regex timeleftHoursMinutes = br.getRegex("class=\"name\">(\\d+) godzin, (\\d+) minut</p>");
        final String timeleftDays = br.getRegex("<span class=\"long\" style=\"[^<>\"]+\">(\\d+) DNI</span>").getMatch(0);
        final String timeleftHours = timeleftHoursMinutes.getMatch(0);
        final String timeleftMinutes = timeleftHoursMinutes.getMatch(1);
        if (timeleftDays != null) {
            timeleftMilliseconds += Long.parseLong(timeleftDays) * 24 * 60 * 60 * 1000;
        }
        if (timeleftHours != null) {
            timeleftMilliseconds += Long.parseLong(timeleftHours) * 60 * 60 * 1000;
        }
        if (timeleftMinutes != null) {
            timeleftMilliseconds += Long.parseLong(timeleftMinutes) * 60 * 1000;
        }
        if (timeleftMilliseconds > 0) {
            ai.setValidUntil(System.currentTimeMillis() + timeleftMilliseconds, this.br);
        }
        final Regex trafficRegex = br.getRegex("Pobrano dzisiaj:<br />\\s*?<strong style=\"[^\"]*?\">([^<>\"]+) z <span style=\"[^<>\"]*?\">([^<>\"]+)</span>");
        final String trafficUsedTodayStr = trafficRegex.getMatch(0);
        final String traffixMaxTodayStr = trafficRegex.getMatch(1);
        if (trafficUsedTodayStr != null && traffixMaxTodayStr != null) {
            final long trafficMaxToday = SizeFormatter.getSize(traffixMaxTodayStr);
            ai.setTrafficLeft(trafficMaxToday - SizeFormatter.getSize(trafficUsedTodayStr));
            ai.setTrafficMax(trafficMaxToday);
        } else {
            /* This is wrong but we do not want our plugin to break just because of some missing information. */
            ai.setUnlimitedTraffic();
        }
        if (!isPremium) {
            account.setType(AccountType.FREE);
        } else {
            account.setType(AccountType.PREMIUM);
        }
        final String hostTableText = br.getRegex("<ul class=\"wykazLista\">(.*?)</ul>").getMatch(0);
        if (hostTableText == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hostInfo = hostTableText.split("<li>");
        for (final String singleHostInfo : hostInfo) {
            String host = new Regex(singleHostInfo, "<b>([^<>\"]+)</b>").getMatch(0);
            final boolean isActive = singleHostInfo.contains("online.png");
            if (StringUtils.isEmpty(host) || !isActive) {
                continue;
            }
            host = host.toLowerCase();
            supportedHosts.add(host);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    // private void login(final Account account) throws IOException, PluginException, InterruptedException {
    // if (USE_API) {
    // loginAPI(account);
    // } else {
    // loginWebsite(account);
    // }
    // }
    private Map<String, Object> loginAPI(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                this.prepBrowserAPI(br);
                String sessionID = getSessionID(account);
                final long session_expire = account.getLongProperty("sessionexpire", 0);
                if (!StringUtils.isEmpty(sessionID) && System.currentTimeMillis() < session_expire) {
                    /* Trust session to be valid */
                    return null;
                }
                if (!StringUtils.isEmpty(sessionID)) {
                    logger.info("Checking sessionID");
                    try {
                        br.getPage(API_BASE + "?a=accountStatus&sessident=" + sessionID);
                        final Map<String, Object> userinfo = handleAPIErrors(br, account, null);
                        logger.info("Validated stored sessionID");
                        account.setProperty("sessionexpire", System.currentTimeMillis() + 40 * 60 * 60 * 1000);
                        return userinfo;
                    } catch (PluginException e) {
                        logger.log(e);
                    }
                }
                logger.info("Performing full login");
                /* 2023-06-26: They've switched from md5 pw to plaintext. */
                final boolean useMd5PW = false;
                final String pwvalue;
                if (useMd5PW) {
                    pwvalue = JDHash.getMD5(account.getPass());
                } else {
                    pwvalue = account.getPass();
                }
                final UrlQuery query = new UrlQuery();
                query.add("a", "login");
                query.add("apikey", Encoding.urlEncode(Encoding.Base64Decode(APIKEY)));
                query.add("login", Encoding.urlEncode(account.getUser()));
                query.add("password", Encoding.urlEncode(pwvalue));
                br.getPage(API_BASE + "?" + query.toString());
                final Map<String, Object> resp = handleAPIErrors(br, account, null);
                final Map<String, Object> data = (Map<String, Object>) resp.get("data");
                sessionID = (String) data.get("sessident");
                if (StringUtils.isEmpty(sessionID)) {
                    throw new AccountUnavailableException("Unknown error", 5 * 60 * 1000);
                }
                /* According to API documentation, sessionIDs are valid for 60 minutes */
                account.setProperty("sessionexpire", System.currentTimeMillis() + 40 * 60 * 60 * 1000);
                account.setProperty("sessionid", sessionID);
                return resp;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    dumpSessionID(account);
                }
                throw e;
            }
        }
    }

    private String getSessionID(final Account account) {
        return account.getStringProperty("sessionid");
    }

    private void dumpSessionID(final Account account) {
        account.removeProperty("sessionid");
    }

    private boolean isLoggedinHTMLWebsite() {
        return br.containsHTML("class=\"wyloguj\"");
    }

    private void loginWebsite(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedinViaCookies = false;
                if (cookies != null) {
                    /* Avoid full login whenever possible as it requires a captcha to be solved ... */
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://" + account.getHoster() + "/");
                    loggedinViaCookies = isLoggedinHTMLWebsite();
                }
                if (!loggedinViaCookies) {
                    br.getPage("https://" + account.getHoster() + "/");
                    Form loginform = null;
                    final Form[] forms = br.getForms();
                    for (final Form aForm : forms) {
                        if (aForm.containsHTML("/panel/login")) {
                            loginform = aForm;
                            break;
                        }
                    }
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("login", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    String reCaptchaKey = br.getRegex("\\'sitekey\\'\\s*?:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
                    if (reCaptchaKey == null) {
                        /* 2018-02-13: Fallback-key */
                        reCaptchaKey = "6Lcu5AcUAAAAAC9Hkb6eFqM2P_YLMbI39eYi7KUm";
                    }
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    br.submitForm(loginform);
                    final String redirect = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+;URL=(http[^<>\"]+)\" />").getMatch(0);
                    if (redirect != null) {
                        br.getPage(redirect);
                    }
                    final String sessionID = this.br.getCookie(this.br.getHost(), "PHPSESSID");
                    if (sessionID == null || !isLoggedinHTMLWebsite()) {
                        // This should never happen
                        throw new AccountInvalidException();
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private Map<String, Object> handleAPIErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object statuscodeO = entries.get("errno");
        if (statuscodeO == null) {
            /* No error */
            return entries;
        }
        final int statuscode;
        if (statuscodeO instanceof Number) {
            statuscode = ((Number) statuscodeO).intValue();
        } else {
            statuscode = Integer.parseInt(statuscodeO.toString());
        }
        String msg = (String) entries.get("error");
        try {
            /* Should never happen: {"result":false,"errno":99,"error":"function not found"} */
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 99:
                /* Function not found (should never happen) */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 100:
                dumpSessionID(account);
                throw new AccountUnavailableException("Session expired #1", 1 * 60 * 1000l);
            case 200:
                /* SessionID expired --> Refresh on next full login */
                msg = "Invalid sessionID";
                dumpSessionID(account);
                throw new AccountUnavailableException("Session expired #2", 1 * 60 * 1000l);
            case 201:
                /* MOCH server maintenance */
                msg = "Server maintenance";
                mhm.handleErrorGeneric(account, link, "server_maintenance", 10);
            case 202:
                /* Login/PW missing (should never happen) */
                throw new AccountInvalidException(msg);
            case 203:
                if (StringUtils.containsIgnoreCase(msg, "Twoje IP zostalo zablokowane")) {
                    // IP blocked
                    throw new AccountUnavailableException(msg, 1 * 60 * 60 * 1000l);
                } else {
                    /* Invalid API key (should never happen) */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            case 204:
                /* Login/PW wrong or account temporary blocked */
                throw new AccountInvalidException(msg);
            case 207:
                /* Custom API error */
                throw new AccountUnavailableException(msg, 5 * 60 * 1000l);
            case 210:
                /* Link offline */
                msg = "File offline";
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 211:
                /* Host not supported -> Remove it from hostList */
                msg = "Host not supported";
                mhm.handleErrorGeneric(account, link, "Hoster unsupported", 5);
            case 212:
                /* Host offline -> Disable for 5 minutes */
                msg = "Host offline";
                mhm.handleErrorGeneric(account, link, "Hoster offline", 5);
            case 213:
                /* one day hosting links count limit reached. */
                /* Przekroczyłeś dzienny limit ilości pobranych plików z tego hostingu. Spróbuj z linkami z innego hostingu. */
                msg = "Daily limit reached";
                mhm.handleErrorGeneric(account, link, "Daily limit reached", 1);
            default:
                /* unknown error, do not try again with this multihoster */
                msg = "Unknown API error code: " + statuscode;
                if (link == null) {
                    throw new AccountUnavailableException(msg, 5 * 60 * 1000l);
                } else {
                    mhm.handleErrorGeneric(account, link, msg, 20);
                }
            }
        } catch (final PluginException e) {
            logger.info("Exception: statusCode: " + statuscode + " statusMessage: " + msg);
            throw e;
        }
        return entries;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}