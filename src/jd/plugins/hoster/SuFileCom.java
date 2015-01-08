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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sufile.com" }, urls = { "http://(www\\.)?sufile\\.com/(file|vip)/[a-z0-9]+\\.html" }, flags = { 2 })
public class SuFileCom extends PluginForHost {

    public SuFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.sufile.com/upgrade.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.sufile.com/terms.html";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    /* Further tests were impossible due to a lack of example links. */
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 10;

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/vip/", "/file/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("404 Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("class=\"title\">([^<>\"]*?)</h2>").getMatch(0);
        final String filesize = br.getRegex("文件大小：<b>([^<>\"]*?)</b>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Set final filename here because server filenames are bad. */
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String fid = getFID(downloadLink);
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            int wait = 30;
            final String waittime = br.getRegex("id=\"wait_input\" style=\"font-weight:bold;font-size:22px; color: green;\">(\\d+)</span>").getMatch(0);
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            this.sleep(wait * 1001l, downloadLink);
            br.getPage("/down/" + fid + ".html");
            final String code = getCaptchaCode("/downcode.php", downloadLink);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("/downcode.php", "action=yz&id=" + fid + "&code=" + Encoding.urlEncode(code));
            if (br.toString().equals("0")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            br.getPage("/dd.php?file_key=" + fid + "&p=1");
            dllink = br.getRegex("<a id=\"downs\" href=\"(http[^\"]+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("href=\"(http[^\"]+\\w+\\.sufile\\.net(?::\\d+)?/down/[^\"]+)").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().endsWith("/two.html")) {
                // this is when you're downloading too much or ip restriction (from google translate)
                // <div class="title">
                // <font color="#FF0000">普通用户只允许同时下载一个文件，请您先完成当前下载后，再尝试下载其他文件。</font>
                // </div>
                // <br />
                // <div class="content">
                // <div class="bottom"><br />
                // 若您当前并没有下载文件，仍然收到此提示，请通过以下两条进行检查：
                // <br />1. 如果您之前使用浏览器内置下载工具下载，我们建议您关闭并重新打开浏览器。
                // <br />
                // 2. 如果您之前使用迅雷等下载工具下载，我们建议您关闭并重新打开迅雷等下载工具。
                // <br /><br /><br />
                // <h1><a href="/pay_vip.php" target="_blank">升级为VIP会员将不受此限制</a></h1><br /><br /><br />
                // </div>
                // </div>
                // </div>
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 20 * 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]+)\\.html$").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://sufile.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getPage("http://www.sufile.com/login.html");
                br.postPage("http://www.sufile.com/post.php", "type=login&nick=" + Encoding.urlEncode(account.getUser()) + "&pwd=" + Encoding.urlEncode(account.getPass()));
                if (!br.toString().equals("1")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("/member/userinfo.php");
        ai.setUnlimitedTraffic();
        if (br.containsHTML("!VIP会员</span><span class=\"lgray\"")) {
            account.setProperty("free", true);
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            try {
                account.setType(AccountType.FREE);
                /* free accounts can still have captcha */
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Registered (free) user");
        } else {
            account.setProperty("free", false);
            final String expire = br.getRegex(">到期时间：</span><span class=\\\\mr15 w300 dib\">([^<>\"]*?)</span>").getMatch(0);
            if (expire == null) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.CHINA));
            }
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            try {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        if (account.getBooleanProperty("free", false)) {
            br.getPage(link.getDownloadURL());
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                br.setFollowRedirects(true);
                br.getPage(link.getDownloadURL());
                /* Example of a finallink: \"(https?://vip\\d+[a-z0-9\\-\\.]+\\.sufile.net:3660/down/filename?key=xxx */
                dllink = br.getRegex("\"(https?://vip\\d+[a-z0-9\\-\\.]+\\.sufile[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
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

}