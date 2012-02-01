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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidgator.net" }, urls = { "http://(www\\.)?rapidgator\\.net/file/\\d+/[^/<>]+\\.html" }, flags = { 2 })
public class RapidGatorNet extends PluginForHost {

    public RapidGatorNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rapidgator.net/article/premium");
    }

    private static final String MAINPAGE = "http://rapidgator.net/";
    private static final Object LOCK     = new Object();

    @Override
    public String getAGBLink() {
        return "http://rapidgator.net/?content=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        int wait = 30;
        final String waittime = br.getRegex("var secs = (\\d+);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getPage("http://rapidgator.net/download/AjaxStartTimer?fid=" + new Regex(downloadLink.getDownloadURL(), "rapidgator\\.net/file/(\\d+)/").getMatch(0));
        final String sid = br2.getRegex("\"sid\":\"(.*?)\"").getMatch(0);
        if (sid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(wait * 1001l, downloadLink);
        br2.getPage("http://rapidgator.net/download/AjaxGetDownloadLink?sid=" + sid);
        if (!br2.containsHTML("\"state\":\"done\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://rapidgator.net/download/captcha");
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        for (int i = 0; i <= 5; i++) {
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.getForm().put("DownloadCaptchaForm%5Bcaptcha%5D", "");
            rc.setCode(c);
            if (br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/)")) continue;
            break;
        }
        if (br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("location\\.href = \\'(http://.*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://pr_srv\\.rapidgator\\.net//\\?r=download/index\\&session_id=[A-Za-z0-9]+)\\'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setCookie("http://rapidgator.net/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Downloading:[\t\n\r ]+</strong>([^<>\"]+)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download file ([^<>\"]+)</title>").getMatch(0);
        String filesize = br.getRegex("File size:[\t\n\r ]+<strong>([^<>\"]+)</strong>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(false);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).matches(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).matches(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(true);
            br.postPage("http://rapidgator.net/auth/login", "LoginForm%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&LoginForm%5BrememberMe%5D=1");
            if (br.getCookie(MAINPAGE, "user__") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("Premium till (\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("var premium_download_link = \\'(http://[^<>\"\\']+)\\';").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://pr_srv\\.rapidgator\\.net//\\?r=download/index\\&session_id=[A-Za-z0-9]+)\\'").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}