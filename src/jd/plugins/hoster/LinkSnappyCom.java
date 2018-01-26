//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

/**
 * 24.11.15 Update by Bilal Ghouri:
 *
 * - Host has removed captcha and added attempts-account based system. API calls have been updated as well.<br />
 * - Cookies are valid for 30 days after last use. After that, Session Expired error will occur. In which case, Login() should be called to
 * get new cookies and store them for further use. <br />
 * - Better Error handling through exceptions.
 *
 * @author raztoki
 * @author psp
 * @author bilalghouri
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksnappy.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class LinkSnappyCom extends antiDDoSForHost {

    private static MultiHosterManagement mhm = new MultiHosterManagement("linksnappy.com");

    public LinkSnappyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://linksnappy.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://linksnappy.com/tos";
    }

    private static final String USE_API                = "USE_API";
    private static final String CLEAR_DOWNLOAD_HISTORY = "CLEAR_DOWNLOAD_HISTORY";
    private static final int    MAX_DOWNLOAD_ATTEMPTS  = 10;
    private int                 i                      = 1;
    private DownloadLink        currentLink            = null;
    private Account             currentAcc             = null;
    private boolean             resumes                = true;
    private int                 chunks                 = 0;
    private String              dllink                 = null;
    protected static Object     ACCLOCK                = new Object();

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        return api_fetchAccountInfo(false);
    }

    private AccountInfo api_fetchAccountInfo(final boolean force) throws Exception {
        synchronized (ACCLOCK) {
            br = new Browser();
            final AccountInfo ac = new AccountInfo();
            if (!force) {
                /** Load cookies */
                final Cookies cookies = currentAcc.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    getPage("https://linksnappy.com/api/USERDETAILS");
                    boolean error = "ERROR".equals(PluginJSonUtils.getJsonValue(br, "status"));
                    final String message = PluginJSonUtils.getJsonValue(br, "error");
                    // invalid username is shown when 2factorauth is required o_O.
                    if (isLoginSessionExpired(message)) {
                        login();
                    } else if (error) {
                        throw new AccountInvalidException(message);
                    } else {
                        // cached login successful
                    }
                } else {
                    login();
                }
            } else {
                login();
            }
            getPage("https://linksnappy.com/api/USERDETAILS");
            final String expire = PluginJSonUtils.getJsonValue(br, "expire");
            final String accPackage = PluginJSonUtils.getJsonValue(br, "package");
            if ("lifetime".equalsIgnoreCase(expire) || "lifetime".equalsIgnoreCase(accPackage)) {
                /* 2018-01-15: Lifetime accounts have an expire date near the max unix timestamp (thus we do not display it) */
                currentAcc.setType(AccountType.LIFETIME);
            } else if ("expired".equalsIgnoreCase(expire)) {
                /* Free account = also expired */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree accounts are not supported!\r\nIf your account is Premium contact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                ac.setValidUntil(Long.parseLong(expire) * 1000);
                currentAcc.setType(AccountType.PREMIUM);
            }
            /* Find traffic left */
            final String trafficLeft = PluginJSonUtils.getJsonValue(br, "trafficleft");
            final String maxtraffic = PluginJSonUtils.getJsonValue(br, "maxtraffic");
            if ("unlimited".equals(trafficLeft)) {
                ac.setUnlimitedTraffic();
            } else {
                /* Also check for negative traffic */
                if (trafficLeft.contains("-")) {
                    ac.setTrafficLeft(0);
                } else {
                    ac.setTrafficLeft(Long.parseLong(trafficLeft));
                }
                if (maxtraffic != null) {
                    ac.setTrafficMax(Long.parseLong(maxtraffic));
                }
            }
            /* now it's time to get all supported hosts */
            getPage("https://linksnappy.com/api/FILEHOSTS");
            if ("ERROR".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
                final String message = PluginJSonUtils.getJsonValue(br, "error");
                if ("Account has exceeded the daily quota".equals(message)) {
                    dailyLimitReached();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + message, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            final String hostText = br.getRegex("\\{\"status\":\"OK\",\"error\":false,\"return\":\\{(.*?\\})\\}").getMatch(0);
            if (hostText == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final ArrayList<String> supportedHosts = new ArrayList<String>();
            // connection info map
            final HashMap<String, HashMap<String, Object>> con = new HashMap<String, HashMap<String, Object>>();
            String[] hosts = new Regex(hostText, "([a-z0-9\\-]+\\.){1,}([a-z]{2,4})[^\\}]+\\}").getColumn(-1);
            for (final String hostInfo : hosts) {
                HashMap<String, Object> e = new HashMap<String, Object>();
                final String host = new Regex(hostInfo, "[^\"]+").getMatch(-1);
                if (hosts == null) {
                    continue;
                }
                final String status = PluginJSonUtils.getJsonValue(hostInfo, "Status");
                String quota = PluginJSonUtils.getJsonValue(hostInfo, "Quota");
                if (quota != null) {
                    if (quota.matches("\\d+")) {
                        e.put("quota", Long.parseLong(quota));
                    } else if ("unlimited".equalsIgnoreCase(quota)) {
                        e.put("quota", -1);
                    } else {
                        // this should not happen.
                        logger.warning("Possible plugin defect!");
                    }
                }
                String usage = PluginJSonUtils.getJsonValue(hostInfo, "Usage");
                if (usage != null) {
                    e.put("usage", Long.parseLong(usage));
                }
                final String resumes = PluginJSonUtils.getJsonValue(hostInfo, "resume");
                if (resumes != null) {
                    e.put("resumes", (resumes.matches("\\d+") && Integer.parseInt(resumes) == 1 ? true : false));
                }
                final String connlimit = PluginJSonUtils.getJsonValue(hostInfo, "connlimit");
                e.put("chunks", (connlimit != null && connlimit.matches("\\d+") ? Integer.parseInt(connlimit) : 0));
                if (!e.isEmpty()) {
                    con.put(host, e);
                }
                if (!"1".equals(status)) {
                    continue;
                } else if ((usage != null && quota != null && !"unlimited".equals(quota)) && (Long.parseLong(quota) - Long.parseLong(usage)) <= 0) {
                    continue;
                } else if (host != null) {
                    supportedHosts.add(host);
                }
            }
            currentAcc.setProperty("accountProperties", con);
            ac.setMultiHostSupport(this, supportedHosts);
            return ac;
        }
    }

    private boolean isLoginSessionExpired(String message) {
        if (br.containsHTML("Session Expired") || "Invalid Username".equals(message)) {
            return true;
        }
        return false;
    }

    private void dailyLimitReached() throws PluginException {
        final String host = br.getRegex("You have exceeded the daily ([a-z0-9\\-\\.]+) Download quota \\(").getMatch(0);
        if (host != null) {
            /* Daily specific host downloadlimit reached --> Disable host for some time */
            mhm.putError(currentAcc, currentLink, 10 * 60 * 1000l, "Daily limit '" + host + "'reached for this host");
        } else {
            /* Daily total downloadlimit for account is reached */
            final String lang = System.getProperty("user.language");
            logger.info("Daily limit reached");
            /* Workaround for account overview display bug so users see at least that there is no traffic left */
            currentAcc.getAccountInfo().setTrafficLeft(0);
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nTageslimit erreicht!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDaily limit reached!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
    }

    /** no override to keep plugin compatible to old stable */
    @SuppressWarnings("deprecation")
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);
        mhm.runCheck(currentAcc, currentLink);
        long tt = link.getLongProperty("filezize", -1);
        if (link.getView().getBytesLoaded() <= 0 || tt == -1) {
            long a = link.getView().getBytesTotalEstimated();
            if (a != -1) {
                link.setProperty("filezize", a);
                tt = a;
            }
        }
        // typically downloading generated links do not require login session!
        dllink = link.getStringProperty("linksnappycomdirectlink", null);
        if (dllink != null) {
            dllink = (attemptDownload() ? dllink : null);
        }
        if (dllink == null) {
            /** Load cookies */
            br = new Browser();
            boolean hasPerformedFetchAccountInfo = false;
            synchronized (ACCLOCK) {
                final Cookies cookies = currentAcc.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                } else {
                    currentAcc.setAccountInfo(api_fetchAccountInfo(true));
                    hasPerformedFetchAccountInfo = true;
                }
            }
            setDownloadConstants();
            /* Reset value because otherwise if attempts fail, JD will try again with the same broken dllink. */
            link.setProperty("linksnappycomdirectlink", Property.NULL);
            final String genLinks = "https://linksnappy.com/api/linkgen?genLinks=" + encode("{\"link\"+:+\"" + Encoding.urlEncode(link.getDownloadURL()) + "\"}");
            for (i = 1; i <= MAX_DOWNLOAD_ATTEMPTS; i++) {
                getPage(genLinks);
                final String message = PluginJSonUtils.getJsonValue(br, "error");
                if (isLoginSessionExpired(message)) {
                    if (hasPerformedFetchAccountInfo) {
                        // we already logged in seconds earlier... continuously re-logging in is pointless.
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Logged in multiple times in sucession, and session automatically expired. Please report to LinkSnappy.");
                    }
                    currentAcc.setAccountInfo(api_fetchAccountInfo(true));
                    hasPerformedFetchAccountInfo = true;
                    setDownloadConstants();
                    getPage(genLinks);
                }
                if (!attemptDownload()) {
                    // we should have short wait period between retries.
                    sleep(5000l, link, "Failed, will retry shortly");
                    continue;
                }
                break;
            }
            handleDownloadErrors();
        }
        link.setProperty("linksnappycomdirectlink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
            } else {
                /*
                 * Check if user wants JD to clear serverside download history in linksnappy account after each download - only possible via account - also
                 * make sure we get no exception as our download was successful NOTE: Even failed downloads will appear in the download history - but they
                 * will also be cleared once you have one successful download.
                 */
                if (PluginJsonConfig.get(LinkSnappyComConfig.class).isClearDownloadHistoryEnabled()) {
                    boolean history_deleted = false;
                    try {
                        getPage("https://linksnappy.com/api/DELETELINK?type=filehost&hash=all");
                        if ("OK".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "status"))) {
                            history_deleted = true;
                        }
                    } catch (final Throwable e) {
                        history_deleted = false;
                    }
                    try {
                        if (history_deleted) {
                            logger.warning("Delete history succeeded!");
                        } else {
                            logger.warning("Delete history failed");
                        }
                    } catch (final Throwable e2) {
                    }
                }
            }
        } catch (final PluginException e) {
            if (e.getMessage() != null && e.getMessage().contains("java.lang.ArrayIndexOutOfBoundsException")) {
                if ((tt / 10) > currentLink.getView().getBytesTotal()) {
                    // this is when linksnappy dls text as proper filename
                    System.out.print("bingo");
                    dl.getConnection().disconnect();
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Problem with multihoster");
                }
            }
            throw e;
        }
    }

    /**
     * We have already retried 10 times before this method is called, their is zero point to additional retries too soon. It should be minimum
     * of 5 minutes and above!
     *
     * @throws InterruptedException
     */
    private void handleDownloadErrors() throws PluginException, IOException, InterruptedException {
        if (dlResponseCode == 507) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Moving to new server", 5 * 60 * 1000l);
        }
        if (dlResponseCode == 504) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid response. Retrying", 5 * 60 * 1000l);
        }
        if (dlResponseCode == 503) {
            // Max 10 retries above link, 5 seconds waittime between = max 2 minutes trying -> Then deactivate host
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "503 error", 5 * 60 * 1000l);
        }
        if (dlResponseCode == 502) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Connection timeout from filehost", 5 * 60 * 1000l);
        }
        if (dlResponseCode == 429) {
            // what does ' max connection limit' error mean??, for user to that given hoster??, or user to that linksnappy finallink
            // server?? or linksnappy global (across all finallink servers) connections
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Max Connection limit reached", 5 * 60 * 1000l);
        }
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            if (dlResponseCode == 200) {
                // all but 200 is followed by handleAttemptResponseCode()
                br.followConnection();
            }
            logger.info("Unknown download error");
            mhm.handleErrorGeneric(currentAcc, currentLink, "unknowndlerror", 2, 5 * 60 * 1000l);
        }
    }

    private void setConstants(final Account account, final DownloadLink downloadLink) throws PluginException {
        if (downloadLink == null && account == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        currentLink = downloadLink;
        currentAcc = account;
    }

    private void setDownloadConstants() {
        final String dl_host = currentLink.getDefaultPlugin().getHost();
        final Object ret = currentAcc.getProperty("accountProperties", null);
        if (ret != null && ret instanceof HashMap) {
            @SuppressWarnings("unchecked")
            final HashMap<String, HashMap<String, Object>> ap = (HashMap<String, HashMap<String, Object>>) ret;
            final HashMap<String, Object> h = ap.get(dl_host);
            if (h == null) {
                // return defaults
                return;
            }
            final int c = h.containsKey("chunks") ? ((Number) h.get("chunks")).intValue() : chunks;
            chunks = (c > 1 ? -c : c);
            final Boolean r = (Boolean) (h.containsKey("resumes") ? h.get("resumes") : resumes);
            if (Boolean.FALSE.equals(r) && chunks == 1) {
                resumes = r;
            } else {
                resumes = true;
            }
        }
    }

    private int dlResponseCode = -1;

    private boolean attemptDownload() throws Exception {
        if (br != null && br.getHttpConnection() != null) {
            if ("FAILED".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "status"))) {
                final String err = PluginJSonUtils.getJsonValue(br, "error");
                if (err != null && !err.matches("\\s*") && !"".equalsIgnoreCase(err)) {
                    if ("ERROR Code: 087".equalsIgnoreCase(err)) {
                        // "status":"FAILED","error":"ERROR Code: 087"
                        // I assume offline (webui says his host is offline, but not the api host list.
                        mhm.putError(currentAcc, currentLink, 10 * 60 * 1000l, "hoster offline");
                    } else if (new Regex(err, "Invalid .*? link\\. Cannot find Filename\\.").matches()) {
                        logger.info("Error: Disabling current host");
                        mhm.putError(currentAcc, currentLink, 5 * 60 * 1000l, "Multihoster issue");
                    } else if (new Regex(err, "Invalid file URL format\\.").matches()) {
                        /*
                         * Update by Bilal Ghouri: Should not disable support for the entire host for this error. it means the host is online but the link format is
                         * not added on linksnappy.
                         */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unsupported URL format.");
                    } else if (new Regex(err, "File not found").matches()) {
                        if (i + 1 == MAX_DOWNLOAD_ATTEMPTS) {
                            // multihoster is not trusted source for offline...
                            logger.warning("Maybe the Hoster link is really offline! Confirm in browser!");
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        /* we just try again */
                        logger.info("Attempt failed: 'file not found' error");
                        return false;
                    }
                }
            }
            dllink = PluginJSonUtils.getJsonValue(br, "generated");
            if (dllink == null || "false".equals(dllink)) {
                logger.info("Direct downloadlink not found");
                mhm.handleErrorGeneric(currentAcc, currentLink, "dllinkmissing", 2, 5 * 60 * 1000l);
            }
        }
        dlResponseCode = -1;
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, currentLink, dllink, resumes, chunks);
            return handleAttemptResponseCode();
        } catch (final SocketTimeoutException e) {
            // if (currentLink.getHost() == "uploaded.to" || currentLink.getHost() == "rapidgator.net") {
            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Waiting for download", 30 * 1000l);
            // }
            final boolean timeoutedBefore = currentLink.getBooleanProperty("sockettimeout", false);
            if (timeoutedBefore) {
                currentLink.setProperty("sockettimeout", false);
                throw e;
            }
            currentLink.setProperty("sockettimeout", true);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } catch (final BrowserException ebr) {
            logger.info("Attempt failed: Got BrowserException for link: " + dllink);
            // this will happen when response codes are outside of allowable
            return handleAttemptResponseCode();
        }
    }

    private boolean handleAttemptResponseCode() throws IOException, PluginException {
        dlResponseCode = dl.getConnection().getResponseCode();
        if (dlResponseCode == 200) {
            return true;
        }
        br.followConnection();
        if (dlResponseCode == 509) {
            /* out of traffic should not retry! throw exception on first response! */
            dailyLimitReached();
        } else if (dlResponseCode == 401) {
            /*
             * claimed ip session changed mid session. not physically possible in JD... but user could have load balancing software or router or isps'
             * also can do this. a full retry should happen
             */
            throw new PluginException(LinkStatus.ERROR_RETRY, "Your ip has been changed. Please retry");
        }
        // generic, apparently can't be in a else statement...
        logger.info("Attempt failed: " + dlResponseCode + "  error for link: " + dllink);
        return false;
    }

    private void login() throws Exception {
        synchronized (ACCLOCK) {
            br = new Browser();
            br.setCookiesExclusive(true);
            getPage("https://linksnappy.com/api/AUTHENTICATE?" + "username=" + Encoding.urlEncode(currentAcc.getUser()) + "&password=" + Encoding.urlEncode(currentAcc.getPass()));
            if (br.getHttpConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 1000l);
            }
            final boolean error = "ERROR".equals(PluginJSonUtils.getJsonValue(br, "status"));
            final String message = PluginJSonUtils.getJsonValue(br, "error");
            if (error) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + message, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                // its not an error. it has to be OK!
                currentAcc.saveCookies(br.getCookies(this.getHost()), "");
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    private String encode(String value) {
        value = value.replace("\"", "%22");
        value = value.replace(":", "%3A");
        value = value.replace("{", "%7B");
        value = value.replace("}", "%7D");
        value = value.replace(",", "%2C");
        return value;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.getHeaders().put("User-Agent", "JDownloader " + getVersion());
            // linksnappy mentioned codes
            prepBr.addAllowedResponseCodes(new int[] { 429, 502, 503, 504, 507 });
            prepBr.setConnectTimeout(2 * 60 * 1000);
            prepBr.setReadTimeout(2 * 60 * 1000);
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return LinkSnappyComConfig.class;
    }

    public static interface LinkSnappyComConfig extends PluginConfigInterface {

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        public static class TRANSLATION {

            public String getClearDownloadHistory_label() {
                return "Clear download history after each successful download?";
            }
        }

        @DefaultBooleanValue(false)
        boolean isClearDownloadHistoryEnabled();

        void setClearDownloadHistoryEnabled(boolean b);
    }
}