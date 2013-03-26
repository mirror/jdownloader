//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileswap.com" }, urls = { "https?://(www\\.)?fileswap\\.com/dl/[A-Za-z0-9]+/" }, flags = { 2 })
public class FileSwapCom extends PluginForHost {

    // DEV NOTES
    // non account: 4 * 20
    // free account: same as above (not tested)
    // premium account: 10 * 1
    // protocol: http + https
    // captchatype: null
    // other: no published info on expire time.

    private static Object        LOCK    = new Object();
    private static AtomicInteger maxPrem = new AtomicInteger(1);
    private static final String  HOST    = "http://www.fileswap.com";

    public FileSwapCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(HOST + "/premium/");
    }

    @Override
    public String getAGBLink() {
        return HOST + "/legal/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(HOST, "lang", "english");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("The file you requested has not been found|<title>FileSwap\\.com : File not found</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<legend>Share This File \\&#187; (.*?)</legend>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>FileSwap\\.com : (.*?) download free</title>").getMatch(0);
        String filesize = br.getRegex("Size:([^<>\"]*?)\\&nbsp;\\&nbsp;\\&nbsp;").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">The storage node this file is currently on is currently undergoing")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 2 * 60 * 1000l);
        String dllink = br.getRegex("<a id=\"share_index_dlslowbutton\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            int wait = 1;
            String waittime = br.getRegex("var time=(\\d+);").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            sleep(wait * 1001l, downloadLink);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("/ajax_requests.php", "id=" + new Regex(downloadLink.getDownloadURL(), "fileswap\\.com/dl/([A-Za-z0-9]+)/").getMatch(0));
            dllink = br.toString();
            /* remove newline */
            dllink = dllink.replaceAll("%0D%0A", "");
            dllink = dllink.trim();
            if (!dllink.startsWith("http") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink).replace("+", "%2B");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(There has been an error on the page you tried to access\\.|Support staff has been notified of this error\\.)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex("Storage Used:.+>([\\d\\.]+ (KB|MB|GB|TB))").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim() + " GiB");
        account.setValid(true);
        ai.setUnlimitedTraffic();
        if (account.getBooleanProperty("nopremium")) {
            ai.setStatus("Registered (free) User");
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
        } else {
            try {
                maxPrem.set(1);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            ai.setStatus("Premium User");
        }
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch && account.getPass() != null) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(HOST, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                if (account.getPass() != null) {
                    br.postPage(HOST.replace("http://", "https://") + "/account/login.php", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&rememberme=1");
                } else {
                    br.postPage(HOST.replace("http://", "https://") + "/account/login.php", "username=" + Encoding.urlEncode(account.getUser()) + "&password=&rememberme=1");
                }
                if (!br.getRegex("(Invalid username/password)").matches() && !br.getURL().matches(".+fileswap\\.com/index\\.php")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (!br.getRegex("Account Type:.+<span>(Premium)</span>").matches()) {
                    account.setProperty("nopremium", true);
                } else {
                    account.setProperty("nopremium", false);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(HOST);
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

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        if (account.getBooleanProperty("nopremium")) {
            br.getPage(link.getDownloadURL());
            handleFree(link);
        } else {
            requestFileInformation(link);
            login(account, false);
            br.setFollowRedirects(false);
            String dllink = null;
            if (dllink == null) {
                br.getPage(link.getDownloadURL());
                dllink = br.getRegex("<a id=\"textDownloadLink\" href=\"(https?://([\\w\\.]+)?fileswap\\.com/download/\\?id=[^\"]+)").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("(https?://([\\w\\.]+)?fileswap\\.com/download/\\?id=[^\"]+)").getMatch(0);
                    if (dllink == null) {
                        logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -10);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}