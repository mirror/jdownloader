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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
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
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
import jd.plugins.components.MultiHosterManagement;

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

    private static final boolean resumes              = true;
    private static final int     chunks               = 0;
    /** Defines max. waittime for cached downloads after last serverside progress change. */
    private static final int     CACHE_WAIT_THRESHOLD = 10 * 60000;
    private static final String  PROPERTY_DIRECTURL   = "linksnappycomdirectlink";

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return api_fetchAccountInfo(account, true);
    }

    private AccountInfo api_fetchAccountInfo(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final AccountInfo ac = new AccountInfo();
            loginAPI(account, force);
            if (br.getURL() == null || !br.getURL().contains("/api/USERDETAILS")) {
                getPage("https://" + this.getHost() + "/api/USERDETAILS");
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) entries.get("return");
            final Object expireO = entries.get("expire");
            logger.info("expire:" + expireO);
            // final String accountType = (String) entries.get("accountType"); // "free" for free accounts and "elite" for premium accounts
            if ("lifetime".equalsIgnoreCase(expireO.toString())) {
                account.setType(AccountType.PREMIUM);
            } else if ("expired".equalsIgnoreCase(expireO.toString())) {
                /* Free account which has never been premium = also "expired" */
                account.setType(AccountType.FREE);
            } else {
                Long validUntil = null;
                if (expireO instanceof Number) {
                    validUntil = ((Number) expireO).longValue() * 1000;
                } else if (expireO instanceof String) {
                    try {
                        validUntil = Long.parseLong(expireO.toString()) * 1000;
                    } catch (final NumberFormatException e) {
                        logger.exception("expire:" + expireO, e);
                    }
                }
                if (validUntil != null) {
                    ac.setValidUntil(validUntil, this.br);
                    if (!ac.isExpired()) {
                        account.setType(AccountType.PREMIUM);
                    } else {
                        ac.setValidUntil(-1);
                        account.setType(AccountType.FREE);
                    }
                } else {
                    account.setType(AccountType.FREE);
                }
            }
            /* Find traffic left */
            final Object trafficleftO = entries.get("trafficleft");
            logger.info("trafficLeft:" + trafficleftO);
            // final Object maxtrafficO = entries.get("maxtraffic");
            if (trafficleftO instanceof String) {
                /* E.g. value is "unlimited" */
                // if (maxtrafficO instanceof Number && ((Long) maxtrafficO).longValue() > 0) {
                // ac.setTrafficLeft(((Long) maxtrafficO).longValue());
                // ac.setSpecialTraffic(true);
                // } else {
                // ac.setUnlimitedTraffic();
                // }
                ac.setUnlimitedTraffic();
            } else if (trafficleftO instanceof Number) {
                /* Also check for negative traffic */
                final long trafficleft = ((Number) trafficleftO).longValue();
                if (trafficleft <= 0) {
                    ac.setTrafficLeft(0);
                } else {
                    ac.setTrafficLeft(trafficleft);
                }
                if (entries.get("maxtraffic") instanceof Number) {
                    ac.setTrafficMax(((Number) entries.get("maxtraffic")).longValue());
                }
            }
            /* now it's time to get all supported hosts */
            getPage("https://" + this.getHost() + "/api/FILEHOSTS");
            final String error = getError(br);
            if (error != null) {
                if (StringUtils.containsIgnoreCase(error, "Account has exceeded the daily quota")) {
                    dailyLimitReached(account);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + error, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            final ArrayList<String> supportedHosts = new ArrayList<String>();
            /* connection info map */
            final HashMap<String, HashMap<String, Object>> con = new HashMap<String, HashMap<String, Object>>();
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (Map<String, Object>) entries.get("return");
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                final Map<String, Object> hosterInformation = (Map<String, Object>) entry.getValue();
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
                        logger.warning("Failed to find individual quota for host: " + host);
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
            /* Free account information & downloading is only possible via website; not via API! */
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
                    final Regex remainingURLS = br.getRegex("id\\s*=\\s*\"linkleft\">\\s*(\\d+)\\s*</span>\\s*out of (\\d+) premium link");
                    final String remainingDailyURLsStr = remainingURLS.getMatch(0);
                    final String maxDailyURLsStr = remainingURLS.getMatch(1);
                    final int remainingURLs = Integer.parseInt(remainingDailyURLsStr);
                    if (remainingURLs == 0) {
                        /* 0 links left for today --> ZERO trafficleft */
                        ac.setTrafficLeft(0);
                    }
                    ac.setStatus(String.format("Free Account [%s of %s daily links left]", remainingDailyURLsStr, maxDailyURLsStr));
                } catch (final Throwable e) {
                    logger.exception("Failed to find free Account limits --> Setting ZERO trafficleft", e);
                    ac.setTrafficLeft(0);
                    ac.setStatus("Free Account [Failed to find number of URLs left]");
                }
            }
            ac.setMultiHostSupport(this, supportedHosts);
            return ac;
        }
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
            logger.info("Daily limit reached");
            /* Workaround for account overview display bug so users see at least that there is no traffic left */
            try {
                account.getAccountInfo().setTrafficLeft(0);
            } catch (final Throwable e) {
            }
            throw new AccountUnavailableException("Daily download limit reached", 5 * 60 * 1000);
        }
    }

    /**
     * Check with linksnappy server if file needs to get downloaded first before the user can download it from there (2019-05-14: E.g.
     * rapidgator.net URLs).
     **/
    private void cacheDLChecker(final DownloadLink link, final Account account, final String id) throws Exception {
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
                final Map<String, Object> data = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                this.handleErrors(this.getDownloadLink(), account);
                if (data.get("return") == null) {
                    logger.warning("Bad cache state/answer");
                    break;
                }
                final Map<String, Object> cacheReturnStatus = (Map<String, Object>) data.get("return");
                final Integer currentProgress = (int) JavaScriptEngineFactory.toLong(cacheReturnStatus.get("percent"), 0);
                // download complete?
                if (currentProgress.intValue() == 100) {
                    // cache finished, lets go to download part
                    return;
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
        mhm.handleErrorGeneric(account, link, "Cache handling timeout", 10);
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
        /* Typically downloading generated links do not require login session! */
        if (attemptStoredDownloadurlDownload(link)) {
            logger.info("Using previously generated final downloadurl");
        } else {
            logger.info("Generating new downloadurl");
            final String urlRaw;
            if (AccountType.FREE == account.getType()) {
                urlRaw = "https://" + this.getHost() + "/api/linkfree?genLinks=%s";
                /* Free Account download - not possible via API! */
                loginWebsite(account, false);
            } else {
                urlRaw = "https://" + this.getHost() + "/api/linkgen?genLinks=%s";
                this.loginAPI(account, false);
            }
            Map<String, Object> entries = null;
            String passCode = link.getDownloadPassword();
            String dllink = null;
            boolean enteredCorrectPassword = false;
            for (int wrongPasswordAttempts = 0; wrongPasswordAttempts <= wrongPasswordAttempts; wrongPasswordAttempts++) {
                final Map<String, Object> urlinfo = new HashMap<String, Object>();
                urlinfo.put("link", link.getDefaultPlugin().buildExternalDownloadURL(link, this));
                urlinfo.put("type", "");
                if (!StringUtils.isEmpty(passCode)) {
                    urlinfo.put("linkpass", passCode);
                }
                getPage(String.format(urlRaw, URLEncode.encodeURIComponent(JSonStorage.serializeToJson(urlinfo))));
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final List<Object> ressourcelist = (List<Object>) entries.get("links");
                entries = (Map<String, Object>) ressourcelist.get(0);
                final String message = this.getError(entries);
                if (this.isErrorPasswordRequiredOrWrong(message)) {
                    wrongPasswordAttempts += 1;
                    passCode = getUserInput("Password?", link);
                    /*
                     * Do not reset initial password. Multihosters are prone to error - we do not want to remove the users' initial manually
                     * typed in PW!
                     */
                    // link.setDownloadPassword(null);
                    continue;
                } else {
                    enteredCorrectPassword = true;
                    break;
                }
            }
            if (!enteredCorrectPassword) {
                /* Allow next candidate to try */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wrong password entered");
            } else if (passCode != null) {
                link.setDownloadPassword(passCode);
            }
            handleErrors(link, account);
            /* 2021-02-18: Downloadurl will always be returned even if file hasn't been downloaded successfully serverside yet! */
            dllink = (String) entries.get("generated");
            if (StringUtils.isEmpty(dllink)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
            }
            if (entries.containsKey("cacheDL")) {
                logger.info("Checking caching file...");
                cacheDLChecker(link, account, (String) entries.get("hash"));
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), dllink, resumes, chunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                this.handleDownloadErrors(link, account);
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
            }
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        }
        dl.setFilenameFix(true);
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
                        if (this.getError(this.br) == null) {
                            history_deleted = true;
                        }
                    } catch (final Throwable e) {
                        history_deleted = false;
                    }
                    if (history_deleted) {
                        logger.warning("Delete history succeeded!");
                    } else {
                        logger.warning("Delete history failed");
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
                    mhm.putError(account, link, 5 * 60 * 1000l, "Cache download failure");
                }
            }
            throw e;
        }
    }

    /**
     * We have already retried X times before this method is called, their is zero point to additional retries too soon.</br>
     * It should be minimum of 5 minutes and above!
     *
     * @throws InterruptedException
     */
    private void handleDownloadErrors(final DownloadLink link, final Account account) throws PluginException, IOException, InterruptedException {
        final int dlResponseCode = dl.getConnection().getResponseCode();
        if (dlResponseCode == 401) {
            /*
             * claimed ip session changed mid session. not physically possible in JD... but user could have load balancing software or
             * router or isps' also can do this. a full retry should happen
             */
            throw new PluginException(LinkStatus.ERROR_RETRY, "Your ip has been changed. Please retry");
        } else if (dlResponseCode == 425) {
            /*
             * This error code will occur only when the link is being cached in LS system. You have to wait till its finished. Check
             * "/api/CACHEDLSTATUS" for current progress.
             */
            throw new PluginException(LinkStatus.ERROR_RETRY, "Link is being cached. Please wait.");
        } else if (dlResponseCode == 502) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Connection timeout from filehost", 5 * 60 * 1000l);
        } else if (dlResponseCode == 503) {
            // Max 10 retries above link, 5 seconds waittime between = max 2 minutes trying -> Then deactivate host
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "503 error", 5 * 60 * 1000l);
        } else if (dlResponseCode == 504) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid response. Retrying", 5 * 60 * 1000l);
        } else if (dlResponseCode == 507) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Moving to new server", 5 * 60 * 1000l);
        } else if (dlResponseCode == 509) {
            /* out of traffic should not retry! throw exception on first response! */
            dailyLimitReached(account);
        } else if (dlResponseCode == 429) {
            // what does ' max connection limit' error mean??, for user to that given hoster??, or user to that linksnappy finallink
            // server?? or linksnappy global (across all finallink servers) connections
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Max Connection limit reached", 5 * 60 * 1000l);
        }
        if (dl.getConnection() == null || !this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dlResponseCode == 200) {
                // all but 200 is followed by handleAttemptResponseCode()
                br.followConnection(true);
            }
            logger.info("Unknown download error");
            mhm.handleErrorGeneric(account, link, "unknowndlerror", 2, 5 * 60 * 1000l);
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (url == null) {
            return false;
        }
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), url, resumes, chunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                dl.getConnection().disconnect();
                return false;
            }
        } catch (final Throwable e) {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e2) {
            }
        }
        return false;
    }

    private void handleErrors(final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object linksO = entries.get("links");
        if (linksO != null && linksO instanceof List) {
            /* Make sure we're working on the correct map! */
            final List<Object> ressourcelist = (List<Object>) linksO;
            entries = (Map<String, Object>) ressourcelist.get(0);
        }
        final String err = getError(entries);
        if (err != null) {
            if (new Regex(err, "(?i)No server available for this filehost, Please retry after few minutes").matches()) {
                // if no server available for the filehost
                mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, "hoster offline");
            } else if (new Regex(err, "(?i)Couldn't (re-)?start download in system").matches()) {
                mhm.handleErrorGeneric(account, link, "Can't start cache. Possibly daily limit reached", 50);
            } else if (new Regex(err, "(?i)You have reached max download request").matches()) {
                mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, "Too many requests. Please wait 5 minutes");
            } else if (new Regex(err, "(?i)You have reached max download limit of").matches()) {
                try {
                    account.getAccountInfo().setTrafficLeft(0);
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nLimit Reached. Please purchase elite membership!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (new Regex(err, "(?i)Invalid .*? link\\. Cannot find Filename").matches()) {
                logger.info("Error: Disabling current host");
                mhm.putError(account, this.getDownloadLink(), 5 * 60 * 1000l, "Multihoster issue");
            } else if (new Regex(err, "(?i)Invalid file URL format\\.").matches()) {
                /*
                 * Update by Bilal Ghouri: Should not disable support for the entire host for this error. it means the host is online but
                 * the link format is not added on linksnappy.
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unsupported URL format");
            } else if (new Regex(err, "(?i)File not found").matches()) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Untrusted error 'file not found'");
            } else if (new Regex(err, "(?i)Your Account has Expired").matches()) {
                /*
                 * 2019-09-03 "{"status": "ERROR", "error": "Your Account has Expired, Please <a
                 * href=\"https://linksnappy.com/myaccount/extend\">extend it</a>"}"
                 */
                /*
                 * This message may also happens if you try to download with a free account with UN-confirmed E-Mail!! Browser will show a
                 * more precise errormessage in this case!
                 */
                try {
                    account.getAccountInfo().setExpired(true);
                } catch (final Throwable e) {
                }
                throw new AccountUnavailableException("Account expired", 3 * 60 * 60 * 1000l);
            } else if (isErrorPasswordRequiredOrWrong(err)) {
                /** This error will usually be handled outside of here! */
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            } else if (new Regex(err, "(?i)Please upgrade to Elite membership").matches()) {
                /* 2019-09-05: Free Account daily downloadlimit reached */
                throw new AccountUnavailableException("Daily downloadlimit reached", 10 * 60 * 1000l);
            } else {
                logger.warning("Possible unknown API error occured: " + err);
                if (this.getDownloadLink() == null) {
                    throw new AccountUnavailableException(err, 10 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, err, 5 * 60 * 1000l);
                }
            }
        }
    }

    private boolean isErrorPasswordRequiredOrWrong(final String msg) {
        return msg != null && msg.matches("(?i)This file requires password");
    }

    private String getError(final Browser br) {
        return getError(JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP));
    }

    private String getError(Map<String, Object> map) {
        final Object status = map.get("status");
        final Object error = map.get("error");
        if (status != null) {
            if ("OK".equalsIgnoreCase(status.toString())) {
                return null;
            } else {
                if (error instanceof String) {
                    return error.toString();
                } else {
                    return "unknown/" + status + "/" + error;
                }
            }
        } else if (error != null) {
            if (error instanceof String) {
                return error.toString();
            } else {
                return "unknown/" + status + "/" + error;
            }
        } else {
            return null;
        }
    }

    /**
     * @param validateCookies
     *            true = Check whether stored cookies are still valid, if not, perform full login <br/>
     *            false = Set stored cookies and trust them if they're not older than 300000l
     *
     */
    private boolean loginAPI(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                br.setCookies(this.getHost(), cookies);
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l && !validateCookies) {
                    logger.info("Trust cookies as they're not that old");
                    return false;
                }
                logger.info("Validating cookies");
                getPage("https://" + this.getHost() + "/api/USERDETAILS");
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final String error = getError(entries);
                // invalid username is shown when 2factorauth is required o_O.
                if (error == null) {
                    logger.info("Cached login successful");
                    /* Save new cookie timestamp */
                    account.saveCookies(br.getCookies(this.getHost()), "");
                    return true;
                } else {
                    logger.info("Cached login failed:" + error);
                    br.clearCookies(br.getHost());
                }
            }
            /* Full login is required */
            logger.info("Performing full login");
            getPage("https://" + this.getHost() + "/api/AUTHENTICATE?" + "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final String error = getError(entries);
            if (error != null) {
                final String redirect = (String) entries.get("redirect");
                if (StringUtils.containsIgnoreCase(error, "Two-Factor Verification Required") && !StringUtils.isEmpty(redirect)) {
                    /* 2021-02-16: Rare case: User needs to open this URL and confirm log. */
                    /**
                     * {"status":"ERROR","error":"Two-Factor Verification Required. Please check your
                     * Email","return":null,"redirect":"\/validate\/xxxxxxxxxx"}
                     */
                    final String fullURL = br.getURL(redirect).toString();
                    throw new AccountUnavailableException("\r\n" + error + "\r\n" + fullURL, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + error, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(this.getHost()), "");
            return true;
        }
    }

    private boolean loginWebsite(final Account account, final boolean verifyCookies) throws Exception {
        /* 2019-09-05: API cookies and website cookies are the same and can be used for both! */
        boolean validatedCookies = this.loginAPI(account, verifyCookies);
        if (verifyCookies) {
            br.getPage("https://" + this.getHost());
            return this.isLoggedinWebsite();
        } else {
            return validatedCookies;
        }
    }

    private boolean isLoggedinWebsite() {
        return br.getCookie(this.getHost(), "Auth", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(this.getHost(), "username", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            return true;
        }
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
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
            public String getClearDownloadHistoryEnabled_label() {
                return "Clear download history after each successful download?";
            }
        }

        @DefaultBooleanValue(false)
        boolean isClearDownloadHistoryEnabled();

        void setClearDownloadHistoryEnabled(boolean b);
    }
}