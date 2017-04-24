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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.IO;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.CountingOutputStream;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cocoleech.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32424" })
public class CocoleechCom extends PluginForHost {

    private static final String                            API_ENDPOINT         = "https://members.cocoleech.com/auth/api";
    private static final String                            NICE_HOST            = "cocoleech.com";
    private static final String                            NICE_HOSTproperty    = NICE_HOST.replaceAll("(\\.|\\-)", "");
    private static final String                            PROPERTY_LOGINTOKEN  = "cocoleechlogintoken";

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap   = new HashMap<Account, HashMap<String, Long>>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Boolean>                hostResumeMap        = new HashMap<String, Boolean>();
    /* Contains <host><number of max possible chunks per download> */
    private static HashMap<String, Integer>                hostMaxchunksMap     = new HashMap<String, Integer>();
    /* Contains <host><number of max possible simultan downloads> */
    private static HashMap<String, Integer>                hostMaxdlsMap        = new HashMap<String, Integer>();
    /* Contains <host><number of currently running simultan downloads> */
    private static HashMap<String, AtomicInteger>          hostRunningDlsNumMap = new HashMap<String, AtomicInteger>();

    /* Last updated: 2017-02-08 according to admin request. */
    private static final int                               defaultMAXDOWNLOADS  = 20;
    private static final int                               defaultMAXCHUNKS     = -4;
    private static final boolean                           defaultRESUME        = true;
    private final String                                   apikey               = "cdb5efc9c72196c1bd8b7a594b46b44f";

    private static Object                                  CTRLLOCK             = new Object();
    private int                                            statuscode           = 0;
    private static AtomicInteger                           maxPrem              = new AtomicInteger(1);
    private Account                                        currAcc              = null;
    private DownloadLink                                   currDownloadLink     = null;
    private static String                                  currLogintoken       = null;

    @SuppressWarnings("deprecation")
    public CocoleechCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://members.cocoleech.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://members.cocoleech.com/terms";
    }

    @Override
    public String rewriteHost(String host) {
        if ("vip.cocoleech.com".equals(getHost())) {
            if (host == null || "vip.cocoleech.com".equals(host)) {
                return "cocoleech.com";
            }
        }
        return super.rewriteHost(host);
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
        if (currLogintoken == null) {
            currLogintoken = this.getLoginToken();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        final String currentHost = this.correctHost(downloadLink.getHost());
        /* Make sure that we do not start more than the allowed number of max simultan downloads for the current host. */
        synchronized (hostRunningDlsNumMap) {
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
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        this.setConstants(account, link);
        handleDL(account, link);
    }

    @SuppressWarnings("deprecation")
    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        String maxchunksStr = null;
        if (dllink == null) {
            br.setFollowRedirects(true);
            /* request creation of downloadlink */
            /* Make sure that the file exists - unnecessary step in my opinion (psp) but admin wanted to have it implemented this way. */
            this.getAPISafe(API_ENDPOINT + "?username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            maxchunksStr = PluginJSonUtils.getJsonValue(this.br, "chunks");
            dllink = PluginJSonUtils.getJsonValue(br, "download");
            if (dllink == null || dllink.equals("")) {
                logger.warning("Final downloadlink is null");
                handleErrorRetries("dllinknull", 10, 60 * 60 * 1000l);
            }
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        boolean resume = account.getBooleanProperty("resume", defaultRESUME);
        int maxChunks = account.getIntegerProperty("account_maxchunks", defaultMAXCHUNKS);
        if (maxchunksStr != null && maxchunksStr.matches("\\d+")) {
            maxChunks = Integer.parseInt(maxchunksStr);
            if (maxChunks > 20) {
                maxChunks = 0;
            } else if (maxChunks > 1) {
                maxChunks = -maxChunks;
            } else {
                maxChunks = 1;
            }
        } else {
            /* Then check if we got an individual host limit. */
            if (hostMaxchunksMap != null) {
                final String thishost = link.getHost();
                synchronized (hostMaxchunksMap) {
                    if (hostMaxchunksMap.containsKey(thishost)) {
                        maxChunks = hostMaxchunksMap.get(thishost);
                    }
                }
            }

            if (hostResumeMap != null) {
                final String thishost = link.getHost();
                synchronized (hostResumeMap) {
                    if (hostResumeMap.containsKey(thishost)) {
                        resume = hostResumeMap.get(thishost);
                    }
                }
            }
        }
        if (!resume) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("json")) {
            br.followConnection();
            updatestatuscode();
            handleAPIErrors(this.br);
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
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
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

        /*
         * When JD is started the first time and the user starts downloads right away, a full login might not yet have happened but it is
         * needed to get the individual host limits.
         */
        synchronized (CTRLLOCK) {
            if (hostMaxchunksMap.isEmpty() || hostMaxdlsMap.isEmpty()) {
                logger.info("Performing full login to set individual host limits");
                this.fetchAccountInfo(account);
            }
        }
        this.setConstants(account, link);

        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (!con.isOK() || con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
                    downloadLink.removeProperty(property);
                } else {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    final CountingOutputStream cos = new CountingOutputStream(bos);
                    IO.readStreamToOutputStream(128 * 1024, con.getInputStream(), cos, false);
                    if (cos.transferedBytes() < 100) {
                        downloadLink.removeProperty(property);
                        logger.info(bos.toString("UTF-8"));
                    } else {
                        return dllink;
                    }
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.removeProperty(property);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
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
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();

        login(true);

        final String accounttype = PluginJSonUtils.getJsonValue(br, "type");
        final String trafficleft = PluginJSonUtils.getJsonValue(br, "traffic_left");
        final String validuntil = PluginJSonUtils.getJsonValue(br, "expire_date");
        long timestamp_validuntil = 0;
        if (validuntil != null) {
            timestamp_validuntil = TimeFormatter.getMilliSeconds(validuntil, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        if ("premium".equalsIgnoreCase(accounttype) && timestamp_validuntil > 0) {
            ai.setValidUntil(timestamp_validuntil);
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setConcurrentUsePossible(true);
            /*
             * 2017-02-08: Accounts do usually not have general traffic limits - however there are individual host traffic limits see
             * mainpage (when logged in) --> Right side "Daily Limit(s)"
             */
            if (trafficleft != null && !trafficleft.equalsIgnoreCase("unlimited")) {
                ai.setTrafficLeft(Long.parseLong(trafficleft));
            } else {
                ai.setUnlimitedTraffic();
            }
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
            /* 2016-05-05: According to admin, free accounts cannot download anything */
            account.setConcurrentUsePossible(false);
            ai.setTrafficLeft(0);
        }

        this.getAPISafe(API_ENDPOINT + "/hosts-status");
        ArrayList<String> supportedhostslist = new ArrayList();
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<Object> hosters = (ArrayList<Object>) entries.get("result");
        for (final Object hostero : hosters) {
            entries = (LinkedHashMap<String, Object>) hostero;
            String host = (String) entries.get("host");
            final String status = (String) entries.get("status");
            if (host != null && "online".equalsIgnoreCase(status)) {
                host = correctHost(host);
                final int maxdownloads = defaultMAXDOWNLOADS;
                final int maxchunks = defaultMAXCHUNKS;
                boolean resumable = defaultRESUME;

                hostMaxchunksMap.put(host, maxchunks);
                hostMaxdlsMap.put(host, maxdownloads);
                hostResumeMap.put(host, resumable);
                supportedhostslist.add(host);
            }
        }
        account.setValid(true);

        hostMaxchunksMap.clear();
        hostMaxdlsMap.clear();
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final boolean force) throws IOException, PluginException {
        this.getAPISafe(API_ENDPOINT + "/info?username=" + Encoding.urlEncode(this.currAcc.getUser()) + "&password=" + Encoding.urlEncode(this.currAcc.getPass()));
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        } else if (this.currDownloadLink.getHost().equals(this.getHost())) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        synchronized (hostUnavailableMap) {
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
        this.br.getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    // private void postAPISafe(final String accesslink, final String postdata) throws IOException, PluginException {
    // this.br.postPage(accesslink, postdata);
    // updatestatuscode();
    // handleAPIErrors(this.br);
    // }

    private String getLoginToken() {
        return currAcc.getStringProperty(PROPERTY_LOGINTOKEN, null);
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
        synchronized (CTRLLOCK) {
            final String currentHost = correctHost(this.currDownloadLink.getHost());
            int was = maxPrem.get();
            maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), defaultMAXDOWNLOADS));
            logger.info("maxPrem was = " + was + " && maxPrem now = " + maxPrem.get());
            AtomicInteger currentRunningDls = new AtomicInteger(0);
            if (hostRunningDlsNumMap.containsKey(currentHost)) {
                currentRunningDls = hostRunningDlsNumMap.get(currentHost);
            }
            currentRunningDls.set(currentRunningDls.get() + num);
            hostRunningDlsNumMap.put(currentHost, currentRunningDls);
        }
    }

    /**
     * 0 = everything ok, 666 = hell
     */
    private void updatestatuscode() {
        /* 2017-02-08: They do not have valid statuscodes at the moment - only errormessages. */
        statuscode = 0;
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        final String statusMessage = PluginJSonUtils.getJsonValue(br, "message");
        if (statusMessage != null) {
            if (statusMessage.equalsIgnoreCase("Incorrect log-in or password.")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (statusMessage.matches("Daily limit is reached\\. Hours left:\\s*?\\d+")) {
                tempUnavailableHoster(10 * 60 * 1000l);
            } else if (statusMessage.equalsIgnoreCase("Failed to generate link.")) {
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_failedtogeneratelink", 30, 10 * 60 * 1000l);
            } else {
                /* Unknown error */
                handleErrorRetries(NICE_HOSTproperty + "timesfailed_unknownerror", 50, 5 * 60 * 1000l);
            }
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}