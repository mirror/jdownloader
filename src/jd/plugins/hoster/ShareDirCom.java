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
import java.util.Locale;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sharedir.com" }, urls = { "https?://dl\\.sharedir\\.com/\\d+/" })
public class ShareDirCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap                   = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS                             = "NOCHUNKS";

    private static final String                            API_BASE                             = "https://sharedir.com/sdapi.php";
    private static final String                            NICE_HOST                            = "sharedir.com";
    private static final String                            NICE_HOSTproperty                    = NICE_HOST.replaceAll("(\\.|\\-)", "");

    /** TODO: Get these 2 constants via API. */
    /* 500 MB, checked/set 28.12.14 */
    private static final long                              default_free_account_traffic_max     = 524288000L;
    /* Max. downloadable filesize for freeusers in MB, checked/set 2014-12-28 */
    private static final long                              default_free_account_filesize_max_mb = 150;

    public static final long                               trust_cookie_age                     = 300000l;

    private Account                                        currAcc                              = null;
    private DownloadLink                                   currDownloadLink                     = null;
    private int                                            STATUSCODE                           = 0;

    public ShareDirCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://sharedir.com/");
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public String getAGBLink() {
        return "https://sharedir.com/terms.html";
    }

    private Browser prepBr(final Browser br) {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setAllowedResponseCodes(new int[] { 500 });
        return br;
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        prepBr(this.br);
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)/$").getMatch(0));
        this.setBrowserExclusive();
        // Login required to check/download
        final Account aa = AccountController.getInstance().getValidAccount(this);
        // This shouldn't happen
        if (aa == null) {
            downloadLink.getLinkStatus().setStatusText("Only downlodable/checkable via account!");
            return AvailableStatus.UNCHECKABLE;
        }
        this.login(aa, false);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(downloadLink.getDownloadURL());
            if (!con.getContentType().contains("html")) {
                downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        prepBr(this.br);
        final AccountInfo ac = new AccountInfo();
        this.login(account, false);
        if (br.getURL() == null || !this.br.getURL().contains("get_acc_type")) {
            safeAPIRequest(API_BASE + "?get_acc_type", account, null);
        }
        final String acctype;
        if ("0".equals(this.br.toString().trim())) {
            ac.setTrafficMax(default_free_account_traffic_max);
            account.setType(AccountType.FREE);
            acctype = "Free Account";
        } else {
            account.setType(AccountType.PREMIUM);
            acctype = "Premium Account";
        }
        safeAPIRequest(API_BASE + "?get_traffic_left", account, null);
        ac.setTrafficLeft(Long.parseLong(br.toString().trim()) * 1024);
        safeAPIRequest(API_BASE + "?get_expire_date", account, null);
        if (!br.toString().trim().equals("0")) {
            ac.setValidUntil(TimeFormatter.getMilliSeconds(br.toString(), "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
        }

        ac.setProperty("multiHostSupport", Property.NULL);
        safeAPIRequest(API_BASE + "?get_dl_limit", account, null);
        int maxSim = Integer.parseInt(br.toString().trim());
        if (maxSim < 0) {
            maxSim = 1;
        }
        account.setMaxSimultanDownloads(maxSim);
        account.setConcurrentUsePossible(true);

        // this should done at the point of link generating (which this host doesn't do). As not every hoster would have the same value...
        safeAPIRequest(API_BASE + "?get_max_file_conn", account, null);
        int maxcon = Integer.parseInt(br.toString().trim());
        if (maxcon >= 20) {
            maxcon = 0;
        } else if (maxcon <= 1) {
            maxcon = 1;
        } else {
            maxcon = -maxcon;
        }
        account.setProperty("maxcon", maxcon);
        // now let's get a list of all supported hosts:
        safeAPIRequest(API_BASE + "?get_dl_hosts", account, null);
        String[] hosts = br.toString().split(",");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }
        ac.setStatus(acctype);
        ac.setMultiHostSupport(this, supportedHosts);
        return ac;
    }

    @SuppressWarnings("unused")
    private void apiRequest(final String requestUrl, final Account acc, final DownloadLink dl) throws PluginException, IOException {
        br.getPage(requestUrl);
        updatestatuscode();
    }

    private void safeAPIRequest(final String requestUrl, final Account acc, final DownloadLink dl) throws PluginException, IOException {
        this.br.getPage(requestUrl);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    @SuppressWarnings("unused")
    private void safeAPIPostRequest(final String requestUrl, final String postData, final Account acc, final DownloadLink dl) throws PluginException, IOException {
        br.postPage(requestUrl, "postData");
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBr(this.br);
        setConstants(account, link);
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

        login(account, false);
        final String dllink = "http://dl." + account.getHoster() + "/?i=" + Encoding.urlEncode(link.getDownloadURL());
        handleDl(link, account, dllink, false);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);
        requestFileInformation(link);
        login(account, false);
        handleDl(link, account, link.getDownloadURL(), true);
    }

    public void handleDl(final DownloadLink link, final Account account, final String dllink, final boolean hostlink) throws Exception {
        int maxChunks = (int) account.getLongProperty("maxcon", 1);
        if (link.getBooleanProperty(ShareDirCom.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        final URLConnectionAdapter con = dl.getConnection();
        final String content_type = con.getContentType();
        /* Maybe server sends a wrong file */
        if (con.getLongContentLength() <= 500 && "application/octet-stream".equalsIgnoreCase(content_type)) {
            handleErrorRetries("failedtimes_size_mismatch_errorpremium", 5, 5 * 60 * 1000l);
        }
        if (content_type != null && content_type.contains("html")) {
            br.followConnection();
            updatestatuscode();
            this.handleAPIErrors(this.br);
            handleErrorRetries("failedtimes_unknowndlerrorpremium", 5, 5 * 60 * 1000l);
        }
        link.setProperty(NICE_HOSTproperty + "finallink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(ShareDirCom.NOCHUNKS, false) == false) {
                    link.setProperty(ShareDirCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            /* This may happen if the downloads stops somewhere - a few retries usually help in this case - otherwise disable host */
            if (e.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                logger.info(NICE_HOST + ": DOWNLOAD_INCOMPLETE");
                handleErrorRetries("timesfailed_dl_incomplete", 5, 5 * 60 * 1000l);
            }
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(ShareDirCom.NOCHUNKS, false) == false) {
                link.setProperty(ShareDirCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                prepBr(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                    /* We trust these cookies --> Do not check them */
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                if (cookies != null) {
                    /* Check whether existing cookies are still valid or not. */
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage(API_BASE + "?get_acc_type");
                    this.updatestatuscode();
                    if (this.STATUSCODE != 500) {
                        /* Check for other errors, just in case ... */
                        this.handleAPIErrors(this.br);
                        /* Save cookies again to set new timestamp. */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    }
                    /* Perform full login ... */
                }
                final String lang = System.getProperty("user.language");
                br.postPage("https://" + account.getHoster() + "/login.html", "rem=1&login_submit=1&api=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("100\\s*?:\\s*?Invalid username/password combination")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.setType(AccountType.UNKNOWN);
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void updatestatuscode() {
        String statusCode = br.getRegex("ERR (\\d+)").getMatch(0);
        if (statusCode != null) {
            STATUSCODE = Integer.parseInt(statusCode);
        } else {
            if (br.containsHTML("You are not logged in\\!<br")) {
                STATUSCODE = 500;
            } else if (br.containsHTML(">Sorry, only Premium users can download files bigger than")) {
                STATUSCODE = 666;
            } else if (br.containsHTML(">Sorry, you don\\'t have enough traffic left to download this file directly")) {
                STATUSCODE = 667;
            } else if (br.containsHTML(">Sorry, due to excessive limitations, direct downloads from this host are only available to Premium users")) {
                STATUSCODE = 668;
            } else {
                STATUSCODE = 0;
            }
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (STATUSCODE) {
            case 0:
                // Everything ok
                break;
            case 100:
                /* Invalid username/password -> disable account */
                statusMessage = "Error 100 invalid username/password";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 200:
                /* Unknown server error -> disable account */
                statusMessage = "Error 200 unknown server error";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            case 500:
                /* Not logged in -> disable account */
                statusMessage = "Error 500";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 666:
                /*
                 * File too big for free accounts -> Use next download candidate. This should never happen as it is also covered in
                 * canHandle
                 */
                statusMessage = "Error: This file is only downloadable via premium account (filesize > 150 MB)";
                this.currDownloadLink.setProperty("sharedircom_" + this.currAcc.getUser() + "_downloadallowed", false);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "sharedir.com: This file is only downloadable via premium account (filesize > " + default_free_account_filesize_max_mb + " MB)", 1 * 60 * 1000l);
            case 667:
                /* No traffic available to download current file -> disable current host */
                tempUnavailableHoster(60 * 60 * 1000l);
            case 668:
                /*
                 * File is CURRENTLY too big for free accounts -> show errormessage [This is a very rare case]
                 */
                statusMessage = "File is currently too big for free accounts --> Unsupported host";
                tempUnavailableHoster(5 * 60 * 1000l);
            default:
                /* Unknown errorcode -> disable account */
                statusMessage = "Unknown errorcode";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);

            }
        } catch (final PluginException e) {
            logger.info(NICE_HOST + ": Exception: statusCode: " + STATUSCODE + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        // This should never happen
        if (this.currDownloadLink.getHost().equals("sharedir.com")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "FATAL Server error");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
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

    @SuppressWarnings("deprecation")
    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        /* Make sure that our account type allows downloading a link with this filesize. */
        long fsize = downloadLink.getVerifiedFileSize();
        if (fsize == -1) {
            fsize = downloadLink.getDownloadSize();
        }
        if (fsize >= default_free_account_filesize_max_mb * 1000l * 1000l && !Account.AccountType.PREMIUM.equals(account.getType())) {
            return false;
        }
        /* Failover for too big files - used in case a download attempt is made. */
        if (account != null && !downloadLink.getBooleanProperty("sharedircom_" + account.getUser() + "_downloadallowed", true) && account.getType() == AccountType.FREE) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}