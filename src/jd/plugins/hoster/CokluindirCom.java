//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.nutils.JDHash;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cokluindir.com", "i-debrid.com" }, urls = { "http://\\w+\\.(cokluindir|i-debrid)\\.com(:\\d+)?/aio2?\\.php/.+\\?id=[a-z0-9]{32}", "HAHAHAHAHAHAHA://thisisafakeregex" }, flags = { 2, 0 })
public class CokluindirCom extends PluginForHost {

    // DEV NOTES
    // supports last09 based on pre-generated links and jd2

    private static final String                            mName              = "cokluindir.com";
    private static final String                            mProt              = "http://";
    private static Object                                  LOCK               = new Object();
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();

    public CokluindirCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setCookie(mProt + mName, "lang", "english");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
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
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        showMessage(link, "Task 2: Download begins!");
        handleDL(link, link.getDownloadURL());
    }

    private void handleDL(DownloadLink link, String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /* download is not contentdisposition, so remove this host from premiumHosts list */
            br.followConnection();
            /* temp disabled the host */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        prepBrowser();
        login(acc, false);
        showMessage(link, "Task 1: Generating Link");
        /* request Download */
        if (link.getStringProperty("pass", null) != null) {
            br.postPage(mProt + mName + "/indir10.php", "link=" + Encoding.urlEncode(link.getDownloadURL()) + "&password=" + Encoding.urlEncode(link.getStringProperty("pass", null)));
        } else {
            br.postPage(mProt + mName + "/indir10.php", "link=" + Encoding.urlEncode(link.getDownloadURL()) + "&password=");
        }
        handleErrors(acc, link);
        String dllink = br.getRegex("href=\"(http[^\"]+)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        showMessage(link, "Task 2: Download begins!");
        // might need a sleep here hoster seems to have troubles with new links.
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
            return ai;
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        String expire = br.getRegex("<br>([\\d\\-]+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy", null));
        }
        ai.setStatus("Premium User");
        try {
            String hostsSup = br.cloneBrowser().getPage(mProt + mName + "/saglayicilar.php");
            String[] hosts = new Regex(hostsSup, "\"([^\", ]+)").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            ai.setProperty("multiHostSupport", supportedHosts);
        } catch (Throwable e) {
            account.setProperty("multiHostSupport", Property.NULL);
            logger.info("Could not fetch ServerList from " + mName + ": " + e.toString());
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
                br.getPage(mProt + mName + "/giris.php?isim=" + Encoding.urlEncode(account.getUser()) + "&parola=" + JDHash.getMD5(account.getPass()));
                handleErrors(account, null);
                if (br.getCookie(mProt + mName, "cokluindir") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 'long timeout' to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    private void handleErrors(Account account, DownloadLink downloadLink) throws PluginException {
        // begin the error handing..
        String error = null;
        String statusMessage = null;
        String err = br.getRegex("(.+)").getMatch(0);
        // errors are shown as numerical text response, but also shared at times with other data.
        if (err != null && !err.startsWith(new Regex(err, "(\\d+)").getMatch(0)))
            return;
        else if (err != null && err.startsWith(new Regex(err, "(\\d+)").getMatch(0))) error = new Regex(err, "(\\d+)").getMatch(0);
        try {
            int status = Integer.parseInt(error);
            switch (status) {
            case 100:
                // 100 - Login Failed
                if (statusMessage == null) statusMessage = "Login creditials are invalid!";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 101:
                // 101 - Successful Login as Premium
                if (statusMessage == null) statusMessage = "Sucessfully logged in!";
                break;
            case 102:
                // 102 - Successful Login as Normal
                // raz: free/expired account user, disable account!
                if (statusMessage == null) statusMessage = "You are a non Premium user, JDownloader does not support this.";
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 103:
                // 103 - You didn't log in (You must login)
                // raz: not possible JD wont allow dl unless they are logged in.
                break;
            case 110:
                // 110 - It can be either file is unavailable in hoster (deleted from file hoster) or ONLY this hoster is temporary
                // unavailable in Coklu Indir. We say 110 for both of them.
                if (statusMessage == null) statusMessage = "Tempory hoster issue, wait for retry" + mName;
                tempUnavailableHoster(account, downloadLink, 10 * 60 * 1000);
            case 115:
                // 115 - You were banned
                if (statusMessage == null) statusMessage = "Account has been banned! Please communicate this issue with " + mName;
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 120:
                // 120 - You have to buy a special membership for download from Depositfiles.com
                // raz: free account error, which we do not support.
                break;
            case 121:
                // 121 - You have to buy a special membership for download bigger than 100 MB
                // raz: free account error, which we do not support.
                break;
            case 122:
                // 122 - You have to buy a special membership for download bigger than 50 MB from Uploaded.to
                // raz: free account error, which we do not support.
                break;
            case 130:
                // 130 - This hoster isn't supported
                // raz: unsupported link/hoster
                if (statusMessage == null) statusMessage = "Hoster isn't supported by " + mName;
                throw new PluginException(LinkStatus.ERROR_FATAL);
            case 131:
                // 131 - You have to enter a link (You didn't enter any link)
                // raz: shouldn't be needed as JD wont send null links.
                if (statusMessage == null) statusMessage = "Invalid URL, trying normal hoster";
                throw new PluginException(LinkStatus.ERROR_FATAL);
            case 140:
                // 140 - Means dedicated or virtual server is detected. Your account is available in premium (you weren't banned), but you
                // cann't download any file in this IP.
                if (statusMessage == null) statusMessage = "Possible VPS/Dedicated Server in use. Please communicate this issue with " + mName;
                throw new PluginException(LinkStatus.ERROR_FATAL);
            case 141:
                // 141 - You can't enter more than 10 links in same time
                // raz: shouldn't happen in jd ?? not entirely sure what they mean by this..
                if (statusMessage == null) statusMessage = "Threshold reached";
                break;
            case 142:
                // 142 - You didn't enter any password.
                // raz: predl password not provided, yet required
                if (statusMessage == null) statusMessage = "This download password protected. Please provide a password in order to start the download process.";
                throw new PluginException(LinkStatus.ERROR_FATAL);
            default:
                // unknown error, do not try again with this multihoster
                if (statusMessage == null) statusMessage = "Unknown error code, please inform JDownloader Development Team";
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            // not all codes/messages are errors with exception, so finally is fine
            if (error != null) logger.info("Code: " + error + " Message: " + statusMessage);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}