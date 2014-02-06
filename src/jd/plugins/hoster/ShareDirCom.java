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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sharedir.com" }, urls = { "http://dl\\.sharedir\\.com/\\d+/" }, flags = { 2 })
public class ShareDirCom extends PluginForHost {

    // Based on API: http://easyfiles.pl/api_dokumentacja.php?api_en=1
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(20);
    private static final String                            NOCHUNKS           = "NOCHUNKS";

    private static final String                            NICE_HOST          = "sharedir.com";
    private static final String                            API_HTTP           = "http://";
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private int                                            STATUSCODE         = 0;

    public ShareDirCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sharedir.com/");
    }

    @Override
    public String getAGBLink() {
        return "http://sharedir.com/terms.html";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    private Browser prepBr(final Browser br) {
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // Login required to check/download
        final Account aa = AccountController.getInstance().getValidAccount(this);
        // This shouldn't happen
        if (aa == null) {
            downloadLink.getLinkStatus().setStatusText("Only downlodable/checkable via account!");
            return AvailableStatus.UNCHECKABLE;
        }
        this.login(aa, false);
        br.setFollowRedirects(true);
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
        prepBr(this.br);
        final AccountInfo ac = new AccountInfo();
        this.login(account, false);
        safeAPIRequest("http://sharedir.com/sdapi.php?get_acc_type", account, null);
        String acctype = null;
        if (br.containsHTML("0")) {
            acctype = "Registered (free) user";
        } else {
            acctype = "Premium Account";
        }
        safeAPIRequest("http://sharedir.com/sdapi.php?get_traffic_left", account, null);
        ac.setTrafficLeft(Long.parseLong(br.toString().trim()) * 1024);
        safeAPIRequest("http://sharedir.com/sdapi.php?get_expire_date", account, null);
        if (!br.toString().trim().equals("0")) ac.setValidUntil(TimeFormatter.getMilliSeconds(br.toString(), "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));

        ac.setProperty("multiHostSupport", Property.NULL);
        safeAPIRequest("http://sharedir.com/sdapi.php?get_dl_limit", account, null);
        try {
            int maxSim = Integer.parseInt(br.toString().trim());
            if (maxSim > 20)
                maxSim = 20;
            else if (maxSim < 0) maxSim = 1;
            maxPrem.set(maxSim);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }
        safeAPIRequest("http://sharedir.com/sdapi.php?get_max_file_conn", account, null);
        int maxcon = Integer.parseInt(br.toString().trim());
        if (maxcon >= 20)
            maxcon = 0;
        else if (maxcon <= 1)
            maxcon = 1;
        else
            maxcon = -maxcon;
        account.setProperty("maxcon", maxcon);
        // now let's get a list of all supported hosts:
        safeAPIRequest("http://sharedir.com/sdapi.php?get_dl_hosts", account, null);
        String[] hosts = br.toString().split(",");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }
        if (supportedHosts.contains("uploaded.net") || supportedHosts.contains("ul.to") || supportedHosts.contains("uploaded.to")) {
            if (!supportedHosts.contains("uploaded.net")) {
                supportedHosts.add("uploaded.net");
            }
            if (!supportedHosts.contains("ul.to")) {
                supportedHosts.add("ul.to");
            }
            if (!supportedHosts.contains("uploaded.to")) {
                supportedHosts.add("uploaded.to");
            }
        }

        if (supportedHosts.size() == 0) {
            ac.setStatus(acctype + " valid: 0 Hosts via " + NICE_HOST + " available");
        } else {
            ac.setStatus(acctype + " valid: " + supportedHosts.size() + " Hosts via " + NICE_HOST + " available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    private void apiRequest(final String requestUrl, final Account acc, final DownloadLink dl) throws PluginException, IOException {
        br.getPage(requestUrl);
        updatestatuscode();
    }

    private void safeAPIRequest(final String requestUrl, final Account acc, final DownloadLink dl) throws PluginException, IOException {
        br.getPage(requestUrl);
        updatestatuscode();
        handleAPIErrors(this.br, acc, dl);
    }

    private void safeAPIPostRequest(final String requestUrl, final String postData, final Account acc, final DownloadLink dl) throws PluginException, IOException {
        br.postPage(requestUrl, "postData");
        updatestatuscode();
        handleAPIErrors(this.br, acc, dl);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        prepBr(this.br);
        login(acc, false);
        final String dllink = "http://dl.sharedir.com/?i=" + Encoding.urlEncode(link.getDownloadURL());
        handleDl(link, acc, dllink, false);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
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
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            updatestatuscode();
            this.handleAPIErrors(this.br, account, link);
            int timesFailed = link.getIntegerProperty(NICE_HOSTproperty + "failedtimes_unknowndlerrorpremium", 0);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                logger.info(NICE_HOST + ": Unknown download error -> Retrying");
                timesFailed++;
                link.setProperty(NICE_HOSTproperty + "failedtimes_unknowndlerrorpremium", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown download error");
            } else {
                logger.info(NICE_HOST + ": Unknown download error -> Plugin is broken");
                link.setProperty(NICE_HOSTproperty + "failedtimes_unknowndlerrorpremium", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(NICE_HOSTproperty + "finallink", dllink);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(ShareDirCom.NOCHUNKS, false) == false) {
                    link.setProperty(ShareDirCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(ShareDirCom.NOCHUNKS, false) == false) {
                link.setProperty(ShareDirCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private static Object       LOCK        = new Object();
    private static final String COOKIE_HOST = "http://sharedir.com";

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBr(this.br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                final String lang = System.getProperty("user.language");
                br.postPage("http://sharedir.com/login.html", "rem=1&login_submit=1&api=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("100: Invalid username/password combination")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
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
            } else {
                STATUSCODE = 0;
            }
        }
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink downloadLink) throws PluginException {
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
                /* File too big for free accounts -> show errormessage / try without account (or use other available account(s)) */
                statusMessage = "Error: This file is only downloadable via premium account (filesize > 150 MB)";
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only downloadable via premium account (filesize > 150 MB)");
            case 667:
                /* No traffic available to download current file -> disable current host */
                tempUnavailableHoster(account, downloadLink, 60 * 60 * 1000l);
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

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        // This should never happen
        if (downloadLink.getHost().equals("sharedir.com")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "FATAL Server error"); }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
                }
            }
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