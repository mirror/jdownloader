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
import java.util.LinkedList;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filenium.com" }, urls = { "http://\\w+\\.filenium\\.com/get/\\w+/.+" }, flags = { 2 })
public class FileniumCom extends PluginForHost {

    /**
     * TODO: better error handling, as there exits none at the moment
     */
    private static final Object LOCK = new Object();

    public FileniumCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filenium.com/checkuserjd");
    }

    @Override
    public String getAGBLink() {
        return "http://filenium.com/legal";
    }

    public Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie("http://filenium.com", "langid", "1");
        br.getHeaders().put("Agent", "JDOWNLOADER");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            LinkedList<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            if (accs == null || accs.size() == 0) {
                logger.info("No account present, Please add a premium account.");
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
        if (AvailableStatus.FALSE == link.getAvailableStatus()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return link.getAvailableStatus();
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, false);
        handleDL(link, link.getDownloadURL(), false);
    }

    private void handleDL(DownloadLink link, String dllink, boolean liveLink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            if (liveLink == false && dl.getConnection().getResponseCode() == 404) {
                /* link is a pre-generated one and 404 = file offline */
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            br.followConnection();
        }

        /* temp disabled the host */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        login(acc, false);
        showMessage(link, "Task 1: Generating Link");
        String dllink = br.getPage("http://filenium.com/?filenium&filez=" + Encoding.urlEncode(link.getDownloadURL()));
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replaceAll("\\\\/", "/");
        showMessage(link, "Task 2: Download begins!");
        handleDL(link, dllink, true);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, true);
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        String expire = br.getRegex("(?i)<expiration\\-txt>([^<]+)").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy hh:mm:ss", null));
        }
        String acctype = br.getRegex("(?i)<type>(\\w+)</type>").getMatch(0);
        if (acctype != null) {
            ai.setStatus(acctype + " User");
        }
        String hostsSup = br.cloneBrowser().getPage("http://filenium.com/jddomains");
        String[] hosts = new Regex(hostsSup, "\"([^\"]+)\",").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
        ai.setProperty("multiHostSupport", supportedHosts);
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br = newBrowser();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("http://filenium.com", key, value);
                        }
                        return;
                    }
                }
                br.getPage("http://filenium.com/checkuserjd?user=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie("http://filenium.com", "secureid") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (!br.containsHTML("type>premium<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://filenium.com");
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}