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

@HostPlugin(revision = "$Revision: 16318 $", interfaceVersion = 2, names = { "real-debrid.com" }, urls = { "http://\\w+\\.real\\-debrid\\.com/dl/\\w+/.+" }, flags = { 2 })
public class RealDebridCom extends PluginForHost {

    // DEV NOTES
    // supports last09 based on pre-generated links and jd2

    private static final String mName = "real-debrid.com";
    private static final String mProt = "http://";
    private static final Object LOCK  = new Object();

    public RealDebridCom(PluginWrapper wrapper) {
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
                logger.info("No account present, Please add a premium" + mName + "account.");
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
            br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(link.getDownloadURL()) + "&password=" + Encoding.urlEncode(link.getStringProperty("pass", null)));
        } else {
            br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(link.getDownloadURL()));
        }
        // handleErrors();
        String dllink = br.getRegex("\"generated_links\":\\[\\[\"[^\"]+\",\"[^\\,]+,\"(http[^\"]+)\"\\]\\]").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replaceAll("\\\\/", "/");
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
        br.getPage(mProt + mName + "/api/account.php");
        String expire = br.getRegex("(?i)<expiration\\-txt>([^<]+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy hh:mm:ss", null));
        }
        String acctype = br.getRegex("(?i)<type>(\\w+)</type>").getMatch(0).toLowerCase();
        if (acctype.equals("premium")) {
            ai.setStatus("Premium User");
        } else {
            // unhandled account type here.
        }
        if (multiHostSupported()) {
            try {
                String hostsSup = br.cloneBrowser().getPage(mProt + mName + "/api/hosters.php");
                String[] hosts = new Regex(hostsSup, "\"([^\"]+)\",").getColumn(0);
                ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
                /*
                 * set ArrayList<String> with all supported multiHosts of this
                 * service
                 */
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
                br.getPage(mProt + mName + "/ajax/login.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Hash.getMD5(account.getPass()) + "&captcha_challenge=&captcha_answer=&time=" + System.currentTimeMillis());
                if (br.getCookie(mProt + mName, "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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

    private void handleErrors() {
        // begin the error handing..
        if (br.containsHTML("123")) logger.warning("123");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}