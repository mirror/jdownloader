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
import java.util.LinkedHashMap;
import java.util.Map;

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
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapidek.pl", "rapidekshare.com" }, urls = { "https?://rapidek\\.pl/file\\?id=([a-f0-9]{32})", "https?://rapidekshare\\.com/file\\?id=([a-f0-9]{32})" })
public class RapidekPl extends PluginForHost {
    private static final String          API_BASE                     = "https://rapidek.pl/api";
    private static MultiHosterManagement mhm                          = new MultiHosterManagement("rapidek.pl");
    private static final boolean         account_PREMIUM_resume       = true;
    /** 2020-03-21: phg: In my tests, it is OK for the chunkload with the value of 5 */
    private static final int             account_PREMIUM_maxchunks    = 0;
    private static final int             account_PREMIUM_maxdownloads = -1;

    @SuppressWarnings("deprecation")
    public RapidekPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://rapidek.pl/doladuj");
    }

    @Override
    public String getAGBLink() {
        return "https://rapidek.pl/";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        // return AvailableStatus.UNCHECKABLE;
        /*
         * 2020-06-25: Login required to check/download pre-generated directurls --> Return all as TRUE --> They will get checked in
         * handlePremium
         */
        final String fid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        link.setName(fid);
        link.setLinkID(this.getHost() + fid);
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account == null) {
            logger.info("Cannot check links without account");
            return AvailableStatus.TRUE;
        }
        this.loginAPI(account, false);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getPluginPatternMatcher());
            if (con.isContentDisposition() && con.isOK()) {
                link.setFinalFileName(getFileNameFromDispositionHeader(con));
                link.setDownloadSize(con.getCompleteContentLength());
            } else {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (br.getURL().contains("downloadRequestInvalidKey")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.getURL().contains("authDownloadRequest")) {
                    throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.loginAPI(account, false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), account_PREMIUM_resume, account_PREMIUM_maxchunks);
        if (!dl.getConnection().isContentDisposition()) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        }
        dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            this.loginAPI(account, false);
            br.postPageRaw(API_BASE + "/file-download/init", String.format("{\"Url\":\"%s\"}", link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            final String downloadID = PluginJSonUtils.getJson(br, "DownloadId");
            if (StringUtils.isEmpty(downloadID) || !downloadID.matches("\"?[a-f0-9\\-]+\"?")) {
                mhm.handleErrorGeneric(account, link, "Bad DownloadId", 20);
            }
            final boolean success = cacheDLChecker(downloadID);
            if (!success) {
                logger.info("Serverside download failed!");
                mhm.handleErrorGeneric(account, link, "Cloud download failure", 20);
            } else {
                dllink = PluginJSonUtils.getJson(br, "DownloadUrl");
                if (StringUtils.isEmpty(dllink)) {
                    mhm.handleErrorGeneric(account, link, "Failed o find final downloadurl", 20);
                }
            }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, account_PREMIUM_resume, account_PREMIUM_maxchunks);
        if (dl.getConnection().getContentType().contains("text")) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            mhm.handleErrorGeneric(account, link, "Unknown download error", 20);
        }
        this.dl.startDownload();
    }

    /* Stolen from LinkSnappyCom */
    private boolean cacheDLChecker(final String downloadID) throws Exception {
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
                return "File is downloaded to Server " + getHost();
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
        int lastProgress = -1;
        try {
            final int maxWaitSeconds = 300;
            final int waitSecondsPerLoop = 5;
            int waitSecondsLeft = maxWaitSeconds;
            Integer currentProgress = 0;
            do {
                logger.info(String.format("Waiting for file to get loaded onto server - seconds left %d / %d", waitSecondsLeft, maxWaitSeconds));
                this.getDownloadLink().addPluginProgress(waitProgress);
                waitProgress.updateValues(currentProgress.intValue(), 100);
                for (int sleepRound = 0; sleepRound < waitSecondsPerLoop; sleepRound++) {
                    if (isAbort()) {
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    } else {
                        Thread.sleep(1000);
                    }
                }
                if (currentProgress.intValue() != lastProgress) {
                    // lastProgressChange = System.currentTimeMillis();
                    lastProgress = currentProgress.intValue();
                }
                br.getPage(API_BASE + "/file-download/download-info?downloadId=" + Encoding.urlEncode(downloadID));
                /*
                 * Example response: {"DownloadId":"example","DownloadProgressPercentage":0,"FileSizeBytes":5253880,"Filename":
                 * "testuploadSampleVideo1280x7205mb.mp4","DownloadUrl":"","Status":"Failure"}
                 */
                /* Possible "Status" values: Initialization, Failure, Succeeded, (?? more?) */
                try {
                    final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    final String status = (String) entries.get("Status");
                    if ("Failure".equalsIgnoreCase(status)) {
                        /* Stop immediately on failure. */
                        break;
                    }
                    final int tmpCurrentProgress = (int) ((Number) entries.get("DownloadProgressPercentage")).longValue();
                    if (tmpCurrentProgress > currentProgress) {
                        /* Do not allow the progress to "go back". */
                        currentProgress = tmpCurrentProgress;
                    }
                } catch (final Throwable e) {
                    logger.info("Error parsing json response");
                    break;
                }
                waitSecondsLeft -= waitSecondsPerLoop;
            } while (waitSecondsLeft > 0 && currentProgress < 100);
            if (currentProgress >= 100) {
                return true;
            } else {
                return false;
            }
        } finally {
            this.getDownloadLink().removePluginProgress(waitProgress);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        loginAPI(account, true);
        if (br.getURL() == null || !br.getURL().contains("/account/info")) {
            br.getPage(API_BASE + "/account/info");
        }
        final String expireDate = PluginJSonUtils.getJson(br, "AccountExpiry");
        final String trafficLeft = PluginJSonUtils.getJson(br, "AvailableTransferBytes");
        if (expireDate == null && trafficLeft == null) {
            /* Free account --> Cannot download anything */
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        } else {
            /* Premium account */
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(account_PREMIUM_maxdownloads);
            if (expireDate != null && expireDate.matches("\\d+")) {
                ai.setValidUntil(Long.parseLong(expireDate));
            }
            /* 2020-06-05: So far, my test account is only traffic based without an expiredate given (psp) */
            if (trafficLeft != null && trafficLeft.matches("\\d+")) {
                ai.setTrafficLeft(trafficLeft);
            } else {
                ai.setUnlimitedTraffic();
            }
        }
        /*
         * 2020-06-05: API only returns the following this I've enabled this workaround for now:
         * {"Services":["premiumize.com","rapidu.net"]}
         */
        final boolean useForumWorkaround = false;
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        if (useForumWorkaround) {
            /* 2020-06-05: Possible workaround */
            br.getPage("https://rapidekforum.pl/all.php");
            final String[] hosts = br.getRegex("([^;]+);").getColumn(0);
            for (final String host : hosts) {
                /* Do not add domain of this multihost plugin */
                if (host.equalsIgnoreCase(this.getHost())) {
                    continue;
                }
                supportedHosts.add(host);
            }
        } else {
            br.getPage(API_BASE + "/services/list");
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final ArrayList<Object> hostsO = (ArrayList<Object>) entries.get("Services");
            for (final Object hostO : hostsO) {
                supportedHosts.add((String) hostO);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void loginAPI(final Account account, final boolean force) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                String token = account.getStringProperty("token");
                if (cookies != null && token != null) {
                    logger.info("Trying to login via token");
                    br.setCookies(API_BASE, cookies);
                    br.getHeaders().put("Authorization", "Bearer " + token);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l && !force) {
                        logger.info("Not checking token at all as it's still fresh");
                        return;
                    }
                    br.getPage(API_BASE + "/account/info");
                    if (br.getHttpConnection().getResponseCode() == 200) {
                        logger.info("Token login successful");
                        /* Save new cookie timestamp */
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Token login failed");
                        br.clearAll();
                    }
                }
                logger.info("Performing full login");
                br.getHeaders().put("accept", "application/json");
                br.postPageRaw(API_BASE + "/account/sign-in", String.format("{\"Username\":\"%s\",\"Password\":\"%s\"}", account.getUser(), account.getPass()));
                token = PluginJSonUtils.getJson(br, "Token");
                if (StringUtils.isEmpty(token)) {
                    /* 2020-06-05: E.g. "nullified" response: {"Result":"Unknown","AvailableTransferBytes":0} */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                account.setProperty("token", token);
                br.getHeaders().put("Authorization", "Bearer " + token);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() throws PluginException {
        return br.getCookie(br.getHost(), "auth", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}