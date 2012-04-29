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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import jd.utils.JDUtilities;

import org.appwork.utils.Hash;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision: 16318 $", interfaceVersion = 2, names = { "cokluindir.com" }, urls = { "http://\\w+\\.cokluindir\\.com/aio2\\.php/.+\\?id=[a-z0-9]{32}" }, flags = { 2 })
public class CokluindirCom extends PluginForHost {

    // DEV NOTES
    // supports last09 based on pre-generated links and jd2

    private static final String mName = "cokluindir.com";
    private static final String mProt = "http://";
    private static final Object LOCK  = new Object();

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
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(mProt + mName, "lang", "english");
        br.getHeaders().put("Agent", "JDOWNLOADER");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
    }

    public boolean checkLinks(DownloadLink[] urls) {
        prepBrowser();
        if (urls == null || urls.length == 0) { return false; }
        try {
            Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null || !aa.isValid()) {
                logger.info("No valid account present, Please add or enable a premium" + mName + "account.");
                return false;
            }
            login(aa, false);
            for (DownloadLink dl : urls) {
                URLConnectionAdapter con = br.openGetConnection(dl.getDownloadURL());
                dl.setFinalFileName(getFileNameFromHeader(con));
                dl.setDownloadSize(con.getLongContentLength());
                dl.setAvailable(true);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) {
        if (checkLinks(new DownloadLink[] { link }) == false) {
            link.setAvailableStatus(AvailableStatus.FALSE);
        } else if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.FALSE);
        }
        return link.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Download only works with a premium" + mName + "account.", PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            /* not existing in old stable */
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Download only works with a premium" + mName + "account.");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, false);
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        showMessage(link, "Task 2: Download begins!");
        handleDL(link, link.getDownloadURL());
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private void handleDL(DownloadLink link, String dllink) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from
             * premiumHosts list
             */
            br.followConnection();
            handleErrors();
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
            br.postPage(mProt + mName + "/indir10.php", "link=" + Encoding.urlEncode(link.getDownloadURL()) + "&password=" + Encoding.urlEncode(link.getStringProperty("pass", null)));
        } else {
            br.postPage(mProt + mName + "/indir10.php", "link=" + Encoding.urlEncode(link.getDownloadURL()) + "&password=");
        }
        handleErrors();
        String dllink = br.getRegex("(http[^\"]+)").getMatch(0);
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
            if (multiHostSupported()) {
                ai.setProperty("multiHostSupport", Property.NULL);
            }
            return ai;
        }
        account.setValid(true);
        try {
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(-1);
        } catch (final Throwable e) {
        }
        String expire = br.getRegex("(?i)<br>([\\d\\-]+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy", null));
        }
        ai.setStatus("Premium User");
        if (multiHostSupported()) {
            try {
                String hostsSup = br.cloneBrowser().getPage(mProt + mName + "/saglayicilar.php");
                String[] hosts = new Regex(hostsSup, "\"([^\"]+)\",").getColumn(0);
                ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
                ai.setProperty("multiHostSupport", supportedHosts);
            } catch (Throwable e) {
                account.setProperty("multiHostSupport", Property.NULL);
                logger.info("Could not fetch ServerList from Multishare: " + e.toString());
            }
        }
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
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
                prepBrowser();
                br.getPage(mProt + mName + "/giris.php?isim=" + Encoding.urlEncode(account.getUser()) + "&parola=" + Hash.getMD5(account.getPass()));
                if (br.containsHTML("102")) logger.warning("Non Premium accounts are not supported");
                if ((br.getCookie(mProt + mName, "cokluindir") == null) || !br.containsHTML("101")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    private boolean multiHostSupported() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 16116) return false;
        return true;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void handleErrors() throws PluginException {
        // begin the error handing..
        if (br.containsHTML("103")) {
            logger.warning("Apparently you're not logged in!");
            prepBrowser();
            try {
                Account aa = AccountController.getInstance().getValidAccount(this);
                if (aa == null || !aa.isValid()) {
                    logger.info("No valid account present, Please add or enable a premium" + mName + "account.");
                    return;
                }
                login(aa, false);
            } catch (Exception e) {
                return;
            }
        }
        if (br.containsHTML("110")) {
            logger.warning("Account has been banned! Please communicate this issue with " + mName);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.containsHTML("115")) {
            logger.warning("Account has been banned! Please communicate this issue with " + mName);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.containsHTML("130")) {
            logger.warning("Hoster isn't supported, links has been disabled. Removed from supported hoster list");
            // method to remove hoster from supported array HERE.
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        if (br.containsHTML("131")) {
            logger.warning("Invalid URL, it's been disabled. Please communicate this issue with " + mName);
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        if (br.containsHTML("140")) {
            logger.warning("Possible VPS/Dedicated Server in use. Please communicate this issue with " + mName);
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        if (br.containsHTML("141")) {
            logger.warning("Hoster isn't supported, links been disabled.");
            // ArrayList<String> supportedHosts = new
            // ArrayList<String>(ArraysList(ai.getProperty("multiHostSupport")));
            // supportedHosts.remove("host");
            // ai.setProperty("multiHostSupport", supportedHosts);
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}