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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
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
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
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
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksnappy.com" }, urls = { "" })
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

    private static final int MAX_DOWNLOAD_ATTEMPTS = 10;
    private int              i                     = 1;
    private boolean          resumes               = true;
    private int              chunks                = 0;
    private String           dllink                = null;
    private String           linkHash              = null;
    private String           isCache               = null;
    private static final int CACHE_WAIT_THRESHOLD  = 10 * 60000;

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return api_fetchAccountInfo(account, false);
    }

    private AccountInfo api_fetchAccountInfo(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final AccountInfo ac = new AccountInfo();
            br = new Browser();
            loginAPI(account, force);
            if (br.getURL() == null || !br.getURL().contains("/api/USERDETAILS")) {
                getPage("https://" + this.getHost() + "/api/USERDETAILS");
            }
            final String expire = PluginJSonUtils.getJsonValue(br, "expire");
            final String accPackage = PluginJSonUtils.getJsonValue(br, "package");
            if ("lifetime".equalsIgnoreCase(expire) || "lifetime".equalsIgnoreCase(accPackage)) {
                /* 2018-01-15: Lifetime accounts have an expire date near the max unix timestamp (thus we do not display it) */
                account.setType(AccountType.LIFETIME);
            } else if ("expired".equalsIgnoreCase(expire)) {
                /* Free account which has never been premium = also "expired" */
                account.setType(AccountType.FREE);
                /* 2019-09-05: Free Accounts are supported from now on */
                // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree accounts are not supported!\r\nPlease make sure that your
                // account is a paid(premium) account.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                ac.setValidUntil(Long.parseLong(expire) * 1000);
                account.setType(AccountType.PREMIUM);
            }
            /* Find traffic left */
            final String trafficLeft = PluginJSonUtils.getJsonValue(br, "trafficleft");
            final String maxtraffic = PluginJSonUtils.getJsonValue(br, "maxtraffic");
            if ("unlimited".equalsIgnoreCase(trafficLeft)) {
                ac.setUnlimitedTraffic();
            } else if (!StringUtils.isEmpty(trafficLeft)) {
                /* Also check for negative traffic */
                if (trafficLeft.contains("-")) {
                    ac.setTrafficLeft(0);
                } else {
                    ac.setTrafficLeft(Long.parseLong(trafficLeft));
                }
                if (maxtraffic != null) {
                    ac.setTrafficMax(Long.parseLong(maxtraffic));
                }
            } else {
                logger.info("Failed to find trafficLeft");
            }
            /* now it's time to get all supported hosts */
            getPage("https://" + this.getHost() + "/api/FILEHOSTS");
            if ("ERROR".equals(PluginJSonUtils.getJsonValue(br, "status"))) {
                final String message = PluginJSonUtils.getJsonValue(br, "error");
                if ("Account has exceeded the daily quota".equals(message)) {
                    dailyLimitReached(account);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + message, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            final ArrayList<String> supportedHosts = new ArrayList<String>();
            /* connection info map */
            final HashMap<String, HashMap<String, Object>> con = new HashMap<String, HashMap<String, Object>>();
            LinkedHashMap<String, Object> hosterInformation;
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (LinkedHashMap<String, Object>) entries.get("return");
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                hosterInformation = (LinkedHashMap<String, Object>) entry.getValue();
                final String host = entry.getKey();
                if (StringUtils.isEmpty(host)) {
                    continue;
                }
                HashMap<String, Object> e = new HashMap<String, Object>();
                final long status = JavaScriptEngineFactory.toLong(hosterInformation.get("Status"), 0);
                final Object quotaO = hosterInformation.get("Quota");
                final long quota;
                boolean hostHasUnlimitedQuota;
                if (quotaO != null && quotaO instanceof String) {
                    if ("unlimited".equalsIgnoreCase((String) quotaO)) {
                        hostHasUnlimitedQuota = true;
                        e.put("quota", -1);
                    } else {
                        /* this should not happen */
                        hostHasUnlimitedQuota = false;
                        logger.warning("Possible plugin defect!");
                    }
                    quota = -1;
                } else {
                    hostHasUnlimitedQuota = false;
                    quota = JavaScriptEngineFactory.toLong(quotaO, 0);
                }
                // final long noretry = JavaScriptEngineFactory.toLong(hosterInformation.get("noretry"), 0);
                final long canDownload = JavaScriptEngineFactory.toLong(hosterInformation.get("canDownload"), 0);
                final long usage = JavaScriptEngineFactory.toLong(hosterInformation.get("Usage"), 0);
                final long resume = JavaScriptEngineFactory.toLong(hosterInformation.get("resume"), 0);
                final Object connlimit = hosterInformation.get("connlimit");
                e.put("usage", usage);
                if (resume == 1) {
                    e.put("resumes", true);
                } else {
                    e.put("resumes", false);
                }
                if (connlimit != null) {
                    e.put("chunks", JavaScriptEngineFactory.toLong(connlimit, 1));
                }
                if (canDownload != 1) {
                    logger.info("Skipping host as it is because API says download is not possible (canDownload!=1): " + host);
                    continue;
                } else if (status != 1) {
                    /* Host is currently not working or disabled for this MOCH --> Do not add it to the list of supported hosts */
                    logger.info("Skipping host as it is not available at the moment (status!=1): " + host);
                    continue;
                } else if (!hostHasUnlimitedQuota && quota - usage <= 0) {
                    /* User does not have any traffic left for this host */
                    logger.info("Skipping host as account has no quota left for it: " + host);
                    continue;
                }
                if (!e.isEmpty()) {
                    con.put(host, e);
                }
                supportedHosts.add(host);
            }
            account.setProperty("accountProperties", con);
            // final List<String> mapped = ac.setMultiHostSupport(this, supportedHosts);
            if (AccountType.FREE == account.getType()) {
                /* Try to find Free Account limits to display them properly */
                try {
                    this.getPage("/download");
                    /*
                     * 2019-09-05: Free Accounts must be verified via mail or they cannot download anything. E.g.
                     * "</i>Account Status : Unverified. Please verify your email OR purchase <a href="/myaccount/
                     * extend">an Elite account</a> in order to start download." OR
                     * ">Activation code has been blocked due to violation of our terms of service. Buy Elite membership in order to Download."
                     */
                    final Regex remainingURLS = br.getRegex("id=\"linkleft\">(\\d+)</span> out of (\\d+) premium link");
                    final String remainingDailyURLsStr = remainingURLS.getMatch(0);
                    final String maxDailyURLsStr = remainingURLS.getMatch(1);
                    final int remainingURLs = Integer.parseInt(remainingDailyURLsStr);
                    if (remainingURLs == 0) {
                        /* 0 links left for today --> ZERO trafficleft */
                        ac.setTrafficLeft(0);
                    }
                    ac.setStatus(String.format("Free Account [%s of %s daily links left]", remainingDailyURLsStr, maxDailyURLsStr));
                } catch (final Throwable e) {
                    logger.info("Failed to find free Account limits --> Setting ZERO trafficleft");
                    ac.setTrafficLeft(0);
                    ac.setStatus("Free Account [Failed to find number of URLs left]");
                }
            }
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

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private void dailyLimitReached(final Account account) throws PluginException {
        final String host = br.getRegex("You have exceeded the daily ([a-z0-9\\-\\.]+) Download quota \\(").getMatch(0);
        if (host != null) {
            /* Daily specific host downloadlimit reached --> Disable host for some time */
            mhm.putError(account, this.getDownloadLink(), 10 * 60 * 1000l, "Daily limit '" + host + "'reached for this host");
        } else {
            /* Daily total downloadlimit for account is reached */
            final String lang = System.getProperty("user.language");
            logger.info("Daily limit reached");
            /* Workaround for account overview display bug so users see at least that there is no traffic left */
            account.getAccountInfo().setTrafficLeft(0);
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nTageslimit erreicht!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDaily limit reached!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
    }

    /**
     * Check with linksnappy server if file needs to get downloaded first before the user can download it from there (2019-05-14: E.g.
     * rapidgator.net URLs).
     **/
    private void cacheDLChecker() throws Exception {
        if (isCache != null) {
            final String id = linkHash.toString();
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
                    return "Preparing your file";
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
                    long speed = (current * 10000) / currentTimeDifference;
                    if (speed == 0) {
                        return;
                    }
                    long eta = ((total - current) * 10000) / speed;
                    this.setETA(eta);
                }
            };
            waitProgress.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
            waitProgress.setProgressSource(this);
            try {
                long lastProgressChange = System.currentTimeMillis();
                int lastProgress = -1;
                while (System.currentTimeMillis() - lastProgressChange < CACHE_WAIT_THRESHOLD) {
                    if (isAbort()) {
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                    br.getPage("https://" + this.getHost() + "/api/CACHEDLSTATUS?id=" + Encoding.urlEncode(id));
                    final JSonObject dlNode = (JSonObject) new JSonFactory(br.toString().replaceAll("\\\\/", "/")).parse();
                    final JSonObject downloadNode = (JSonObject) dlNode.get("return");
                    final String status = dlNode.get("status").toString();
                    if ("ERROR".equalsIgnoreCase(status)) {
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                    final Integer currentProgress = Integer.parseInt(downloadNode.get("percent").toString());
                    // download complete?
                    if (currentProgress.intValue() == 100) {
                        // cache finished, lets go to download part
                        break;
                    } else {
                        this.getDownloadLink().addPluginProgress(waitProgress);
                        waitProgress.updateValues(currentProgress.intValue(), 100);
                        for (int sleepRound = 0; sleepRound < 10; sleepRound++) {
                            if (isAbort()) {
                                throw new PluginException(LinkStatus.ERROR_RETRY);
                            } else {
                                Thread.sleep(1000);
                            }
                        }
                        if (currentProgress.intValue() != lastProgress) {
                            lastProgressChange = System.currentTimeMillis();
                            lastProgress = currentProgress.intValue();
                        }
                    }
                }
            } finally {
                this.getDownloadLink().removePluginProgress(waitProgress);
            }
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        long tt = link.getLongProperty("filezize", -1);
        if (link.getView().getBytesLoaded() <= 0 || tt == -1) {
            long a = link.getView().getBytesTotalEstimated();
            if (a != -1) {
                link.setProperty("filezize", a);
                tt = a;
            }
        }
        /* typically downloading generated links do not require login session! */
        dllink = link.getStringProperty("linksnappycomdirectlink", null);
        if (dllink != null) {
            dllink = (attemptDownload(account) ? dllink : null);
        }
        if (dllink == null) {
            if (AccountType.FREE == account.getType()) {
                /* Free Account download - not possible via API! */
                loginWebsite(account);
                getPage("https://" + this.getHost() + "/api/linkfree?genLinks={%22link%22%20:%20%22" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)) + "%22,%20%22type%22%20:%20%22%22,%20%22linkpass%22%20:%20%22%22}");
                /* Returns same json as API which means we can use the same code to try to start the download! */
                attemptDownload(account);
            } else {
                /* Premium account download */
                boolean hasValidatedCookies = this.loginAPI(account, false);
                /* Reset value because otherwise if attempts fail, JD will try again with the same broken dllink. */
                link.setProperty("linksnappycomdirectlink", Property.NULL);
                final String genLinks = "https://" + this.getHost() + "/api/linkgen?genLinks=" + encode("{\"link\"+:+\"" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)) + "\"}");
                for (i = 1; i <= MAX_DOWNLOAD_ATTEMPTS; i++) {
                    getPage(genLinks);
                    final String message = PluginJSonUtils.getJsonValue(br, "error");
                    if (isLoginSessionExpired(message)) {
                        if (hasValidatedCookies) {
                            // we already logged in seconds earlier... continuously re-logging in is pointless.
                            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Logged in multiple times in sucession, and session automatically expired. Please report to LinkSnappy.");
                        }
                        this.loginAPI(account, true);
                        hasValidatedCookies = true;
                        getPage(genLinks);
                    }
                    if (!attemptDownload(account)) {
                        // we should have short wait period between retries.
                        sleep(5000l, link, "Failed, will retry shortly");
                        continue;
                    }
                    break;
                }
            }
            handleDownloadErrors(account);
        }
        link.setProperty("linksnappycomdirectlink", dllink);
        try {
            final String server_filename = getFileNameFromDispositionHeader(dl.getConnection());
            if (server_filename.contains("%")) {
                /* Fix html-encoded filename */
                link.setFinalFileName(Encoding.htmlDecode(server_filename));
            }
        } catch (final Throwable e) {
        }
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
                 * Check if user wants JD to clear serverside download history in linksnappy account after each download - only possible via
                 * account - also make sure we get no exception as our download was successful NOTE: Even failed downloads will appear in
                 * the download history - but they will also be cleared once you have one successful download.
                 */
                if (PluginJsonConfig.get(LinkSnappyComConfig.class).isClearDownloadHistoryEnabled()) {
                    boolean history_deleted = false;
                    try {
                        getPage("https://" + this.getHost() + "/api/DELETELINK?type=filehost&hash=all");
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
            logger.log(e);
            if (e.getMessage() != null && e.getMessage().contains("java.lang.ArrayIndexOutOfBoundsException")) {
                if ((tt / 10) > link.getView().getBytesTotal()) {
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
     * We have already retried 10 times before this method is called, their is zero point to additional retries too soon. It should be
     * minimum of 5 minutes and above!
     *
     * @throws InterruptedException
     */
    private void handleDownloadErrors(final Account account) throws PluginException, IOException, InterruptedException {
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
            mhm.handleErrorGeneric(account, this.getDownloadLink(), "unknowndlerror", 2, 5 * 60 * 1000l);
        }
    }

    // private void setDownloadConstants(final Account account, final DownloadLink link) {
    // final String dl_host = link.getDefaultPlugin().getHost();
    // final Object ret = account.getProperty("accountProperties", null);
    // if (ret != null && ret instanceof HashMap) {
    // @SuppressWarnings("unchecked")
    // final HashMap<String, HashMap<String, Object>> ap = (HashMap<String, HashMap<String, Object>>) ret;
    // final HashMap<String, Object> h = ap.get(dl_host);
    // if (h == null) {
    // // return defaults
    // return;
    // }
    // final int c = h.containsKey("chunks") ? ((Number) h.get("chunks")).intValue() : chunks;
    // chunks = (c > 1 ? -c : c);
    // final Boolean r = (Boolean) (h.containsKey("resumes") ? h.get("resumes") : resumes);
    // if (Boolean.FALSE.equals(r) && chunks == 1) {
    // resumes = r;
    // } else {
    // resumes = true;
    // }
    // }
    // }
    private int dlResponseCode = -1;

    private boolean attemptDownload(final Account account) throws Exception {
        if (br != null && br.getHttpConnection() != null) {
            final String status = PluginJSonUtils.getJsonValue(br, "status");
            if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
                final String err = PluginJSonUtils.getJsonValue(br, "error");
                if (!StringUtils.isEmpty(err)) {
                    if ("ERROR Code: 087".equalsIgnoreCase(err)) {
                        // "status":"FAILED","error":"ERROR Code: 087"
                        // I assume offline (webui says his host is offline, but not the api host list.
                        mhm.putError(account, this.getDownloadLink(), 10 * 60 * 1000l, "Filehost offline");
                    } else if ("No server available for this filehost, Please retry after few minutes".equalsIgnoreCase(err)) {
                        // if no server available for the filehost
                        mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, "hoster offline");
                    } else if (new Regex(err, "Couldn't (re-)?start download in system").matches()) {
                        mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, "Can't start cache. Possibly daily limit reached");
                    } else if (new Regex(err, "You have reached max download request").matches()) {
                        mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, "Too many requests. Please wait 5 minutes");
                    } else if (new Regex(err, "You have reached max download limit of").matches()) {
                        account.getAccountInfo().setTrafficLeft(0);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLimit Reached. Please purchase elite membership!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    } else if (new Regex(err, "Invalid .*? link\\. Cannot find Filename\\.").matches()) {
                        logger.info("Error: Disabling current host");
                        mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, "Multihoster issue");
                    } else if (new Regex(err, "Invalid file URL format\\.").matches()) {
                        /*
                         * Update by Bilal Ghouri: Should not disable support for the entire host for this error. it means the host is
                         * online but the link format is not added on linksnappy.
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
                    } else if (new Regex(err, "Your Account has Expired").matches()) {
                        /*
                         * 2019-09-03 "{"status": "ERROR", "error": "Your Account has Expired, Please <a
                         * href=\"https://linksnappy.com/myaccount/extend\">extend it</a>"}"
                         */
                        /*
                         * This message may also happens if you try to download with a free account with UN-confirmed E-Mail!! Browser will
                         * show a more precise errormessage in this case!
                         */
                        account.getAccountInfo().setExpired(true);
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account expired", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    } else if (new Regex(err, "Please upgrade to Elite membership").matches()) {
                        /* 2019-09-05: Free Account daily downloadlimit reached */
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Daily downloadlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    } else {
                        logger.warning("Possible unknown API error occured: " + err);
                    }
                }
            }
            dllink = PluginJSonUtils.getJsonValue(br, "generated");
            isCache = PluginJSonUtils.getJsonValue(br, "cacheDL");
            linkHash = PluginJSonUtils.getJsonValue(br, "hash");
            if (dllink == null || StringUtils.isEmpty(dllink) || "false".equals(dllink)) {
                logger.info("Direct downloadlink not found");
                mhm.handleErrorGeneric(account, this.getDownloadLink(), "dllinkmissing", 2, 5 * 60 * 1000l);
            }
            cacheDLChecker();
        }
        dlResponseCode = -1;
        try {
            br.setConnectTimeout(60 * 1000);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), dllink, resumes, chunks);
            return handleAttemptResponseCode(account, br, dl.getConnection());
        } catch (final SocketTimeoutException e) {
            // if (currentLink.getHost() == "uploaded.to" || currentLink.getHost() == "rapidgator.net") {
            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Waiting for download", 30 * 1000l);
            // }
            final boolean timeoutedBefore = this.getDownloadLink().getBooleanProperty("sockettimeout", false);
            if (timeoutedBefore) {
                this.getDownloadLink().setProperty("sockettimeout", false);
                throw e;
            }
            this.getDownloadLink().setProperty("sockettimeout", true);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } catch (final BrowserException ebr) {
            logger.log(ebr);
            logger.info("Attempt failed: Got BrowserException for link: " + dllink);
            // this will happen when response codes are outside of allowable
            return handleAttemptResponseCode(account, br, dl.getConnection());
        }
    }

    private boolean handleAttemptResponseCode(final Account account, final Browser br, final URLConnectionAdapter connection) throws IOException, PluginException {
        if (connection == null) {
            return false;
        }
        dlResponseCode = connection.getResponseCode();
        if ((dlResponseCode == 200 || dlResponseCode == 206) && (connection.isContentDisposition() || StringUtils.containsIgnoreCase(connection.getContentType(), "octet-stream"))) {
            return true;
        }
        connection.setAllowedResponseCodes(new int[] { connection.getResponseCode() });
        br.followConnection();
        if (dlResponseCode == 509) {
            /* out of traffic should not retry! throw exception on first response! */
            dailyLimitReached(account);
        } else if (dlResponseCode == 401) {
            /*
             * claimed ip session changed mid session. not physically possible in JD... but user could have load balancing software or
             * router or isps' also can do this. a full retry should happen
             */
            throw new PluginException(LinkStatus.ERROR_RETRY, "Your ip has been changed. Please retry");
        } else if (dlResponseCode == 425) {
            /*
             * This error code will occur only when the link is being cached in our system. You have to wait till its finished. Check
             * "/api/CACHEDLSTATUS" for current progress
             */
            throw new PluginException(LinkStatus.ERROR_RETRY, "Link is being cached. Please wait.");
        }
        // generic, apparently can't be in a else statement...
        logger.info("Attempt failed: " + dlResponseCode + "  error for link: " + dllink);
        return false;
    }

    /**
     * @param validateCookies
     *            true = Check whether stored cookies are still valid, if not, perform full login <br/>
     *            false = Set stored cookies and trust them if they're not older than 300000l
     *
     */
    private boolean loginAPI(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br = new Browser();
            br.setCookiesExclusive(true);
            boolean validatedCookies = false;
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(this.getHost(), cookies);
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !validateCookies) {
                    return false;
                }
                logger.info("Validating cookies");
                getPage("https://" + this.getHost() + "/api/USERDETAILS");
                boolean error = "ERROR".equals(PluginJSonUtils.getJsonValue(br, "status"));
                final String message = PluginJSonUtils.getJsonValue(br, "error");
                // invalid username is shown when 2factorauth is required o_O.
                if (!isLoginSessionExpired(message) && !error) {
                    logger.info("cached login successful");
                    validatedCookies = true;
                } else {
                    logger.info("cached login failed");
                    validatedCookies = false;
                }
            }
            if (!validatedCookies) {
                /* Full login is required */
                logger.info("Performing full login");
                getPage("https://" + this.getHost() + "/api/AUTHENTICATE?" + "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getHttpConnection().getResponseCode() == 503) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 1000l);
                }
                final boolean error = "ERROR".equals(PluginJSonUtils.getJsonValue(br, "status"));
                final String message = PluginJSonUtils.getJsonValue(br, "error");
                if (error) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + message, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // its not an error. it has to be OK!
                account.saveCookies(br.getCookies(this.getHost()), "");
                validatedCookies = true;
            }
            return validatedCookies;
        }
    }

    private boolean loginWebsite(final Account account) throws Exception {
        /* 2019-09-05: API cookies and website cookies are the same and can be used for both! */
        boolean validatedCookies = this.loginAPI(account, false);
        if (!validatedCookies) {
            br.getPage("https://" + this.getHost());
            validatedCookies = this.isLoggedinWebsite();
            if (!validatedCookies) {
                this.loginAPI(account, true);
            }
        }
        return validatedCookies;
    }

    private boolean isLoggedinWebsite() {
        return br.getCookie(this.getHost(), "Auth", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(this.getHost(), "username", Cookies.NOTDELETEDPATTERN) != null;
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
            prepBr.addAllowedResponseCodes(new int[] { 425, 429, 502, 503, 504, 507 });
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