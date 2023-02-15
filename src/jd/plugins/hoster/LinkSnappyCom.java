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
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
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
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.components.MultiHosterManagement;

/**
 *
 * @author raztoki
 * @author psp
 * @author bilalghouri
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksnappy.com" }, urls = { "https?://(?:www\\.)?linksnappy\\.com/torrents/(\\d+)/download" })
public class LinkSnappyCom extends PluginForHost {
    private static MultiHosterManagement mhm = new MultiHosterManagement("linksnappy.com");

    public LinkSnappyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://linksnappy.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://linksnappy.com/tos";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private static final boolean resumes              = true;
    private static final int     chunks               = 0;
    /** Defines max. waittime for cached downloads after last serverside progress change. */
    private static final int     CACHE_WAIT_THRESHOLD = 10 * 60000;
    private static final String  PROPERTY_DIRECTURL   = "linksnappycomdirectlink";

    public void setBrowser(final Browser br) {
        super.setBrowser(br);
        br.getHeaders().put("User-Agent", "JDownloader " + getVersion());
        br.addAllowedResponseCodes(new int[] { 425, 429, 502, 503, 504, 507 });
        br.setConnectTimeout(2 * 60 * 1000);
        br.setReadTimeout(2 * 60 * 1000);
        br.setFollowRedirects(true);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, AccountController.getInstance().getValidAccount(this.getHost()));
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        if (account == null) {
            throw new AccountRequiredException();
        }
        loginAPI(account, false);
        URLConnectionAdapter con = null;
        try {
            /* Do NOT use HEAD request here as that will render our subsequent errorhandling useless. */
            con = br.openGetConnection(link.getPluginPatternMatcher());
            handleConnectionErrors(link, con);
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
            link.setFinalFileName(Plugin.getFileNameFromHeader(con));
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    /**
     * Only call this for download-requests of files, hosted on linksnappy!! Do not call this in any handling which is taking care of other
     * filehoster downloads! Do not use this in handleMultihost!
     */
    private void handleConnectionErrors(final DownloadLink link, final URLConnectionAdapter con) throws PluginException {
        if (!this.looksLikeDownloadableContent(con)) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getURL().contains(this.getFID(link))) {
                /* 404 not found page without 404 not found response-code */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* E.g. redirect to mainpage due to expired session/invalid cookies -> This should never happen */
                throw new AccountUnavailableException("Session expired?", 30 * 1000);
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.loginAPI(account, false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 0);
        handleConnectionErrors(link, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return api_fetchAccountInfo(account, true);
    }

    private AccountInfo api_fetchAccountInfo(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final AccountInfo ac = new AccountInfo();
            loginAPI(account, force);
            if (br.getURL() == null || !br.getURL().contains("/api/USERDETAILS")) {
                br.getPage("/api/USERDETAILS");
            }
            final Map<String, Object> userResponse = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            final Map<String, Object> usermap = (Map<String, Object>) userResponse.get("return");
            final Object expireTimestampO = usermap.get("expire");
            /*
             * final String accountType = (String) entries.get("accountType"); // "free" for free accounts and "elite" for premium AND
             * lifetime accounts
             */
            if ("lifetime".equalsIgnoreCase(expireTimestampO.toString()) || "2177388000".equals(expireTimestampO.toString())) {
                /* 2177388000 -> Valid until 2038 -> Lifetime account */
                account.setType(AccountType.LIFETIME);
            } else {
                long validUntil = -1;
                if (expireTimestampO instanceof Number) {
                    validUntil = ((Number) expireTimestampO).longValue() * 1000;
                } else if (expireTimestampO instanceof String) {
                    final String expireStr = expireTimestampO.toString();
                    if (expireStr.matches("\\d+")) {
                        validUntil = Long.parseLong(expireTimestampO.toString()) * 1000;
                    }
                }
                if (validUntil > System.currentTimeMillis()) {
                    ac.setValidUntil(validUntil);
                    account.setType(AccountType.PREMIUM);
                } else {
                    account.setType(AccountType.FREE);
                }
            }
            final Number usedSpace = (Number) usermap.get("usedspace");
            if (usedSpace != null) {
                ac.setUsedSpace(usedSpace.longValue());
            }
            final Number trafficUsedTodayBytes = (Number) usermap.get("trafficused");
            final Object trafficleftGlobalO = usermap.get("trafficleft"); // mostly "unlimited"
            String trafficMaxDailyHumanReadable = "N/A";
            final Number maxtrafficDailyBytesO = (Number) usermap.get("maxtraffic");
            if (maxtrafficDailyBytesO != null) {
                trafficMaxDailyHumanReadable = SizeFormatter.formatBytes(maxtrafficDailyBytesO.longValue());
            }
            if (trafficleftGlobalO instanceof String) {
                /* E.g. value is "unlimited" */
                // if (maxtrafficO instanceof Number && ((Long) maxtrafficO).longValue() > 0) {
                // ac.setTrafficLeft(((Long) maxtrafficO).longValue());
                // ac.setSpecialTraffic(true);
                // } else {
                // ac.setUnlimitedTraffic();
                // }
                ac.setUnlimitedTraffic();
            } else if (trafficleftGlobalO instanceof Number) {
                /* Also check for negative traffic */
                final long trafficleft = ((Number) trafficleftGlobalO).longValue();
                if (trafficleft <= 0) {
                    ac.setTrafficLeft(0);
                } else {
                    ac.setTrafficLeft(trafficleft);
                }
                if (maxtrafficDailyBytesO != null) {
                    ac.setTrafficMax(maxtrafficDailyBytesO.longValue());
                }
            }
            if (trafficUsedTodayBytes != null) {
                ac.setStatus(account.getType().getLabel() + " | Today's usage: ");
                ac.setStatus(String.format("%s | Today's daily usage: %s/%s", account.getType().getLabel(), SizeFormatter.formatBytes(trafficUsedTodayBytes.longValue()), trafficMaxDailyHumanReadable));
            }
            br.getPage("/api/FILEHOSTS");
            final String error = getError(br);
            if (error != null) {
                if (StringUtils.containsIgnoreCase(error, "Account has exceeded the daily quota")) {
                    errorDailyLimitReached(null, account);
                } else {
                    /* Permanently disable account --> Should not happen often. */
                    throw new AccountInvalidException(error);
                }
            }
            final ArrayList<String> supportedHosts = new ArrayList<String>();
            /* connection info map */
            final HashMap<String, HashMap<String, Object>> con = new HashMap<String, HashMap<String, Object>>();
            final Map<String, Object> hosterMapResponse = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
            final Map<String, Object> hosterMap = (Map<String, Object>) hosterMapResponse.get("return");
            final Iterator<Entry<String, Object>> it = hosterMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                final Map<String, Object> hosterInformation = (Map<String, Object>) entry.getValue();
                final String host = entry.getKey();
                if (StringUtils.isEmpty(host)) {
                    continue;
                }
                HashMap<String, Object> hostInfo = new HashMap<String, Object>();
                final long status = JavaScriptEngineFactory.toLong(hosterInformation.get("Status"), 0);
                final Object quotaO = hosterInformation.get("Quota");
                final long quota;
                boolean hostHasUnlimitedQuota;
                if (quotaO != null && quotaO instanceof String) {
                    if ("unlimited".equalsIgnoreCase((String) quotaO)) {
                        hostHasUnlimitedQuota = true;
                        hostInfo.put("quota", -1);
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
                hostInfo.put("usage", usage);
                if (resume == 1) {
                    hostInfo.put("resumes", true);
                } else {
                    hostInfo.put("resumes", false);
                }
                if (connlimit != null) {
                    hostInfo.put("chunks", JavaScriptEngineFactory.toLong(connlimit, 1));
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
                /* Workaround to find real host. */
                final ArrayList<String> tempList = new ArrayList<String>();
                tempList.add(host);
                final List<String> realHosts = ac.setMultiHostSupport(this, tempList);
                if (realHosts == null || realHosts.isEmpty()) {
                    logger.info("Skipping host because we don't have a plugin for it or the plugin we have doesn#t allow multihost usage: " + host);
                    continue;
                }
                final String realHost = realHosts.get(0);
                if (!hostInfo.isEmpty()) {
                    con.put(realHost, hostInfo);
                }
                supportedHosts.add(realHost);
            }
            account.setProperty("accountProperties", con);
            // final List<String> mapped = ac.setMultiHostSupport(this, supportedHosts);
            /* Free account information & downloading is only possible via website; not via API! */
            if (AccountType.FREE == account.getType()) {
                /* Try to find Free Account limits to display them properly */
                try {
                    br.getPage("/download");
                    /*
                     * 2019-09-05: Free Accounts must be verified via mail or they cannot download anything. E.g.
                     * "</i>Account Status : Unverified. Please verify your email OR purchase <a href="/myaccount/
                     * extend">an Elite account</a> in order to start download." OR
                     * ">Activation code has been blocked due to violation of our terms of service. Buy Elite membership in order to Download."
                     */
                    final Regex remainingURLS = br.getRegex("(?i)id\\s*=\\s*\"linkleft\">\\s*(\\d+)\\s*</span>\\s*out of (\\d+) premium link");
                    final String remainingDailyURLsStr = remainingURLS.getMatch(0);
                    final String maxDailyURLsStr = remainingURLS.getMatch(1);
                    final int remainingURLs = Integer.parseInt(remainingDailyURLsStr);
                    if (remainingURLs == 0) {
                        /* 0 links left for today --> ZERO trafficleft */
                        ac.setTrafficLeft(0);
                    } else {
                        ac.setUnlimitedTraffic();
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
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    private void errorDailyLimitReached(final DownloadLink link, final Account account) throws PluginException {
        if (link != null) {
            /* Daily specific host downloadlimit reached --> Disable host for some time */
            mhm.putError(account, this.getDownloadLink(), 10 * 60 * 1000l, "Reached daily limit for this host");
        } else {
            /* Daily total downloadlimit for account is reached */
            logger.info("Daily limit reached");
            /* Workaround for account overview display bug so users see at least that there is no traffic left */
            try {
                account.getAccountInfo().setTrafficLeft(0);
            } catch (final Throwable ignore) {
            }
            throw new AccountUnavailableException("Daily download limit reached", 1 * 60 * 1000);
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
                final Map<String, Object> data = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
                this.handleErrors(link, account, data);
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
                    link.addPluginProgress(waitProgress);
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
            link.removePluginProgress(waitProgress);
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
            for (int wrongPasswordAttempts = 0; wrongPasswordAttempts <= 3; wrongPasswordAttempts++) {
                final Map<String, Object> urlinfo = new HashMap<String, Object>();
                urlinfo.put("link", link.getDefaultPlugin().buildExternalDownloadURL(link, this));
                urlinfo.put("type", "");
                if (!StringUtils.isEmpty(passCode)) {
                    urlinfo.put("linkpass", passCode);
                }
                br.getPage(String.format(urlRaw, URLEncode.encodeURIComponent(JSonStorage.serializeToJson(urlinfo))));
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
                final List<Object> ressourcelist = (List<Object>) entries.get("links");
                entries = (Map<String, Object>) ressourcelist.get(0);
                final String message = this.getError(entries);
                if (this.isErrorDownloadPasswordRequiredOrWrong(message)) {
                    wrongPasswordAttempts += 1;
                    passCode = getUserInput("Password?", link);
                    /**
                     * Do not reset initial password.</br>
                     * Multihosters are prone to error - we do not want to remove the users' initial manually typed in PW!
                     */
                    // link.setDownloadPassword(null);
                    continue;
                } else {
                    /* User has entered correct password or none was needed. */
                    enteredCorrectPassword = true;
                    break;
                }
            }
            if (!enteredCorrectPassword) {
                /* Allow next candidate to try */
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (passCode != null) {
                link.setDownloadPassword(passCode);
            }
            handleErrors(link, account, entries);
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
            final String filename = (String) entries.get("filename");
            if (link.getFinalFileName() == null && filename != null && filename.contains("_")) {
                // fix server side broken filenames, unicode characters collapsed to _
                // remove all unicode characters and compare existing and server side filename
                final String existingName = link.getName();
                final String check = existingName.replaceAll("[^a-zA-Z0-9\\.]", "");
                final String check2 = URLEncode.decodeURIComponent(filename).replaceAll("[^a-zA-Z0-9\\.]", "");
                if (StringUtils.equalsIgnoreCase(check, check2)) {
                    // do not trust/use server side filename and keep existing name
                    link.setFinalFileName(existingName);
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                this.handleDownloadErrors(link, account);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content");
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
                        br.getPage("https://" + this.getHost() + "/api/DELETELINK?type=filehost&hash=all");
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
            errorDailyLimitReached(link, account);
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
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumes, chunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                link.removeProperty(PROPERTY_DIRECTURL);
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    private void handleErrors(final DownloadLink link, final Account account) throws PluginException, InterruptedException {
        Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final Object linksO = entries.get("links");
        if (linksO != null && linksO instanceof List) {
            /* Make sure we're working on the correct map! */
            final List<Object> ressourcelist = (List<Object>) linksO;
            entries = (Map<String, Object>) ressourcelist.get(0);
        }
        handleErrors(link, account, entries);
    }

    private void handleErrors(final DownloadLink link, final Account account, final Map<String, Object> entries) throws PluginException, InterruptedException {
        final String err = getError(entries);
        if (err != null) {
            if (new Regex(err, "(?i)No server available for this filehost, Please retry after few minutes").matches()) {
                // if no server available for the filehost
                mhm.putError(account, link, 5 * 60 * 1000l, "hoster offline");
            } else if (new Regex(err, "(?i)Couldn't (re-)?start download in system").matches()) {
                mhm.handleErrorGeneric(account, link, "Can't start cache. Possibly daily limit reached", 50);
            } else if (new Regex(err, "(?i)You have reached max download request").matches()) {
                mhm.putError(account, link, 5 * 60 * 1000l, "Too many requests. Please wait 5 minutes");
            } else if (new Regex(err, "(?i)You have reached max download limit of").matches()) {
                try {
                    account.getAccountInfo().setTrafficLeft(0);
                } catch (final Throwable e) {
                }
                throw new AccountUnavailableException("\r\nLimit Reached. Please purchase elite membership!", 1 * 60 * 1000);
            } else if (new Regex(err, "(?i)Invalid .*? link\\. Cannot find Filename").matches()) {
                logger.info("Error: Disabling current host");
                mhm.putError(account, link, 5 * 60 * 1000l, "Multihoster issue");
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
                throw new AccountUnavailableException("Account expired", 5 * 60 * 1000l);
            } else if (isErrorDownloadPasswordRequiredOrWrong(err)) {
                /** This error will usually be handled outside of here! */
                if (link != null) {
                    link.setDownloadPassword(null);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            } else if (new Regex(err, "(?i)Please upgrade to Elite membership").matches()) {
                /* 2019-09-05: Free Account daily downloadlimit reached */
                throw new AccountUnavailableException("Daily downloadlimit reached", 10 * 60 * 1000l);
            } else {
                logger.warning("Possible unknown API error occured: " + err);
                if (link == null) {
                    throw new AccountUnavailableException(err, 10 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, err, 5 * 60 * 1000l);
                }
            }
        }
    }

    private boolean isErrorDownloadPasswordRequiredOrWrong(final String msg) {
        return msg != null && msg.matches("(?i)This file requires password");
    }

    private String getError(final Browser br) {
        return getError(restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP));
    }

    private String getError(final Map<String, Object> map) {
        if (map == null) {
            return null;
        }
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

    private boolean loginAPI(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                br.setCookies(this.getHost(), cookies);
                if (!validateCookies) {
                    /* Do not validate cookies. */
                    return false;
                } else {
                    logger.info("Validating cookies");
                    br.getPage("https://" + this.getHost() + "/api/USERDETAILS");
                    final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
                    final String error = getError(entries);
                    // "Invalid username" is shown when 2Fa login is required o_O.
                    if (error == null) {
                        logger.info("Cached login successful");
                        /* Save new cookie timestamp */
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cached login failed:" + error);
                        br.clearCookies(null);
                    }
                }
            }
            /* Full login is required */
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/api/AUTHENTICATE?" + "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
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
                    throw new AccountInvalidException(error);
                }
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    @Deprecated
    private void loginWebsite(final Account account, final boolean verifyCookies) throws Exception {
        /* 2019-09-05: API cookies and website cookies are the same and can be used for both! */
        this.loginAPI(account, verifyCookies);
        if (verifyCookies) {
            br.getPage("https://" + this.getHost());
        }
    }

    /**
     * Checks login status by available cookies. </br>
     * Works for website- and API.
     */
    private boolean isLoggedin(final Browser br) {
        return br.getCookie(this.getHost(), "Auth", Cookies.NOTDELETEDPATTERN) != null && br.getCookie(this.getHost(), "username", Cookies.NOTDELETEDPATTERN) != null;
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        /* This should never get called. */
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
        final String                    text_ClearDownloadHistoryEnabled = "Clear download history after each successful download?";
        public static final TRANSLATION TRANSLATION                      = new TRANSLATION();

        public static class TRANSLATION {
            public String getClearDownloadHistoryEnabled_label() {
                return text_ClearDownloadHistoryEnabled;
            }
        }

        @AboutConfig
        @DefaultBooleanValue(false)
        @DescriptionForConfigEntry(text_ClearDownloadHistoryEnabled)
        boolean isClearDownloadHistoryEnabled();

        void setClearDownloadHistoryEnabled(boolean b);
    }
}