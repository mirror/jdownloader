//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zevera.com" }, urls = { "https?://\\w+\\.zevera\\.com/getFiles\\.as(p|h)x\\?ourl=.+" }, flags = { 2 })
public class ZeveraCom extends PluginForHost {

    // DEV NOTES
    // supports last09 based on pre-generated links and jd2

    private static final String                            mName              = "zevera.com";
    private static final String                            mProt              = "http://";
    private static final String                            mServ              = mProt + "api." + mName;
    private static Object                                  LOCK               = new Object();
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    private static final String                            NOCHUNKS           = "NOCHUNKS";

    public ZeveraCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(mProt + mName, "lang", "english");
        br.getHeaders().put("User-Agent", "JDOWNLOADER");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
    }

    public boolean checkLinks(DownloadLink[] urls) {
        prepBrowser();
        if (urls == null || urls.length == 0) { return false; }
        try {
            List<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                logger.info("No account present, Please add a premium" + mName + "account.");
                for (DownloadLink dl : urls) {
                    /* no check possible */
                    dl.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                }
                return false;
            }
            login(accs.get(0), false);
            br.setFollowRedirects(true);
            for (DownloadLink dl : urls) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(dl.getDownloadURL());
                    if (con.isContentDisposition()) {
                        dl.setFinalFileName(getFileNameFromHeader(con));
                        dl.setDownloadSize(con.getLongContentLength());
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                } finally {
                    try {
                        /* make sure we close connection */
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) return (AvailableStatus) ret;
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
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
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Download only works with a premium" + mName + "account.", PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, false);
        showMessage(link, "Phase 1/3: URL check for pre-generated links!");
        requestFileInformation(link);
        showMessage(link, "Pgase 2/3: Download ready!");
        handleDL(link, link.getDownloadURL());
    }

    private void handleDL(final DownloadLink link, String dllink) throws Exception {

        // Severa uses this redirect logic to wait for the actuall file in the backend. This means: follow the redirects undtil we get Data
        // after 10 redirects, severa shows an error page. We do not allow more than 10 redirects - so we probably never see this error page
        //
        // Besides redirects, the connections often run into socketexceptions. do the same on socket problems - retry
        // according to zevera, 20 retries should be enough

        br.setFollowRedirects(true);
        showMessage(link, "Phase 3/3: Check download!");
        int maxchunks = 0;
        if (link.getBooleanProperty(ZeveraCom.NOCHUNKS, false)) maxchunks = 1;
        try {
            logger.info("Connecting to " + new URL(dllink).getHost());
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxchunks);
        } catch (final PluginException e) {
            if ("Redirectloop".equals(e.getErrorMessage())) {
                logger.info("zevera.com: Download failed because of a Redirectloop -> This is caused by zevera and NOT a JD issue!");
                int timesFailed = link.getIntegerProperty("timesfailedzeveracom_redirectloop", 1);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 20) {
                    timesFailed++;
                    link.setProperty("timesfailedzeveracom_redirectloop", timesFailed);
                    logger.info("zevera.com: Download failed because of a Redirectloop -> This is caused by zevera and NOT a JD issue! -> Retrying!");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error (redirectloop)");
                } else {
                    link.setProperty("timesfailedzeveracom_redirectloop", Property.NULL);
                    logger.info("zevera.com: Download failed because of a Redirectloop -> This is caused by zevera and NOT a JD issue! -> Throwing temporarily unavailable server error");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error (redirectloop)", 5 * 60 * 1000l);
                }
            }
            /* unknown error, we disable multiple chunks */
            if (link.getBooleanProperty(ZeveraCom.NOCHUNKS, false) == false) {
                link.setProperty(ZeveraCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            logger.info("Zevera download failed because: " + e.getMessage());
            logger.info("Zevera.com: Name of the errorMessage: " + e.getErrorMessage());
            throw e;
        } catch (final SocketTimeoutException e) {
            logger.info("zevera.com: Download failed because of a timeout -> This is caused by zevera and NOT a JD issue!");
            int timesFailed = link.getIntegerProperty("timesfailedzeveracom_timeout", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 20) {
                timesFailed++;
                link.setProperty("timesfailedzeveracom_timeout", timesFailed);
                logger.info("zevera.com: Download failed because of a timeout -> This is caused by zevera and NOT a JD issue! -> Retrying!");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty("timesfailedzeveracom_timeout", Property.NULL);
                logger.info("zevera.com: Download failed because of a timeout -> This is caused by zevera and NOT a JD issue! -> Throwing exception!");
                throw e;
            }
        } catch (final SocketException e) {
            logger.info("Zevera download failed because of a timeout/connection problem -> This is probably caused by zevera and NOT a JD issue!");
            int timesFailed = link.getIntegerProperty("timesfailedzeveracom_timeout", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 20) {
                timesFailed++;
                link.setProperty("timesfailedzeveracom_timeout", timesFailed);
                logger.info("zevera.com: Download failed because of a timeout -> This is caused by zevera and NOT a JD issue! -> Retrying!");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                link.setProperty("timesfailedzeveracom_timeout", Property.NULL);
                logger.info("zevera.com: Download failed because of a timeout -> This is caused by zevera and NOT a JD issue! -> Throwing exception!");
                throw e;
            }
        } catch (final Exception e) {
            logger.info("Zevera download FATAL failed because: " + e.getMessage());
            throw e;
        }
        if (!dl.getConnection().getContentType().contains("html")) {
            /* contentdisposition, lets download it */
            try {
                if (!this.dl.startDownload()) {
                    try {
                        if (dl.externalDownloadStop()) return;
                    } catch (final Throwable e) {
                    }
                    /* unknown error, we disable multiple chunks */
                    if (link.getBooleanProperty(ZeveraCom.NOCHUNKS, false) == false) {
                        link.setProperty(ZeveraCom.NOCHUNKS, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            } catch (final PluginException e) {
                // New V2 errorhandling
                /* unknown error, we disable multiple chunks */
                if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(ZeveraCom.NOCHUNKS, false) == false) {
                    link.setProperty(ZeveraCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            if (dl.getConnection().getResponseCode() == 500) {
                logger.info("zevera.com: Download failed because of a server error 500 -> This is caused by zevera and NOT a JD issue!");
                int timesFailed = link.getIntegerProperty("timesfailedzeveracom_servererror500", 1);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 20) {
                    timesFailed++;
                    link.setProperty("timesfailedzeveracom_servererror500", timesFailed);
                    logger.info("zevera.com: Download failed because of a server error 500 -> This is caused by zevera and NOT a JD issue! -> Retrying");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Server error (500)");
                } else {
                    link.setProperty("timesfailedzeveracom_servererror500", Property.NULL);
                    logger.info("zevera.com: Download failed because of a server error 500 -> This is caused by zevera and NOT a JD issue! -> Throwing temporarily unavailable server error 500");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error (500)", 5 * 60 * 1000l);
                }
            }
            br.followConnection();
        }

        /* temp disabled the host */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        prepBrowser();
        login(acc, false);
        showMessage(link, "Task 1: Generating Link");
        /* request Download */
        if (link.getStringProperty("pass", null) != null) {
            br.getPage(mServ + "/getFiles.aspx?ourl=" + Encoding.urlEncode(link.getDownloadURL()) + "&FilePass=" + Encoding.urlEncode(link.getStringProperty("pass", null)));
        } else {
            br.getPage(mServ + "/getFiles.aspx?ourl=" + Encoding.urlEncode(link.getDownloadURL()));
        }
        // handleErrors();
        br.setFollowRedirects(false);
        final String continuePage = br.getRedirectLocation();
        br.getPage(continuePage);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.contains("/member/systemmessage.aspx")) {
            logger.info("zevera.com: unknown error");
            int timesFailed = link.getIntegerProperty("timesfailedzeveracom_unknown", 1);
            link.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 20) {
                timesFailed++;
                link.setProperty("timesfailedzeveracom_unknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                logger.info("zevera.com: Current host doesn't seem to work, deactivating it for 1 hour...");
                link.setProperty("timesfailedzeveracom_unknown", Property.NULL);
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
        }
        showMessage(link, "Task 2: Download begins!");
        handleDL(link, dllink);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            throw e;
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        // doesn't contain useful info....
        // br.getPage(mServ + "/jDownloader.ashx?cmd=accountinfo");
        // grab website page instead.
        br.getPage(mServ + "/Member/Dashboard.aspx");
        String expireTime = br.getRegex(">Expiration date:</label>.+?(\\d+/\\d+/\\d+ [\\d\\:]+ (AM|PM))<").getMatch(0);
        String serverTime = br.getRegex(">Server time:</label>.+?(\\d+/\\d+/\\d+ [\\d\\:]+ (AM|PM))<").getMatch(0);
        long eTime = 0, sTime = 0;
        if (expireTime != null) {
            eTime = TimeFormatter.getMilliSeconds(expireTime, "MM/dd/yyyy hh:mm:ss a", null);
            if (sTime == -1) sTime = TimeFormatter.getMilliSeconds(expireTime, "MM/dd/yyyy hh:mm a", null);
        }
        if (serverTime != null) {
            sTime = TimeFormatter.getMilliSeconds(serverTime, "MM/dd/yyyy hh:mm:ss a", null);
            if (sTime == -1) sTime = TimeFormatter.getMilliSeconds(serverTime, "MM/dd/yyyy hh:mm a", null);
        }
        if (eTime >= 0 && sTime >= 0) {
            // accounts for different time zones! Should always be correct assuming the user doesn't change time every five minutes. Adjust
            // expire time based on users current system time.
            ai.setValidUntil(System.currentTimeMillis() + (eTime - sTime));
        } else if (eTime >= 0) {
            // fail over..
            ai.setValidUntil(eTime);
        } else {
            // epic fail?
            logger.warning("Expire time could not be found/set. Please report to JDownloader Development Team");
        }
        ai.setStatus("Premium User");
        try {
            String hostsSup = br.cloneBrowser().getPage(mServ + "/jDownloader.ashx?cmd=gethosters");
            String[] hosts = new Regex(hostsSup, "([^,]+)").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            /*
             * set ArrayList<String> with all supported multiHosts of this service
             */
            // we used local cached links provided by our decrypter, locked to ip addresses. Can not use multihoster
            supportedHosts.remove("youtube.com");
            supportedHosts.remove("vimeo.com");
            supportedHosts.remove("megaupload.com"); // offline hoster
            ai.setProperty("multiHostSupport", supportedHosts);
        } catch (Throwable e) {
            account.setProperty("multiHostSupport", Property.NULL);
            logger.info("Could not fetch ServerList from Multishare: " + e.toString());
        }
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(mProt + mName, key, value);
                        }
                        return;
                    }
                }
                br.getPage(mServ);
                br.getPage(mServ + "/OfferLogin.aspx?login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(mProt + mName, ".ASPNETAUTH") == null) {
                    final String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(mProt + mName);
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

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}