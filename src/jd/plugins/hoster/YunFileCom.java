//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yunfile.com" }, urls = { "http://((www|(page\\d)\\.)?(yunfile|filemarkets|yfdisk)\\.com/file/(down/)?[a-z0-9]+/[a-z0-9]+|filemarkets\\.com/fs/[a-z0-9]+/)" }, flags = { 2 })
public class YunFileCom extends PluginForHost {

    private static final String    MAINPAGE = "http://yunfile.com/";
    private static Object          LOCK     = new Object();

    private static StringContainer agent    = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

    // Works like HowFileCom
    public YunFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.yunfile.com/user/premiumMembership.html");
        // this.setStartIntervall(15 * 1000l);
    }

    private Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        if (agent.string == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent.string);
        prepBr.getHeaders().put("Accept-Language", "de,en-us;q=0.7,en;q=0.3");
        prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        prepBr.setCookie("http://yunfile.com", "language", "en_us");
        // br.setCookie(this.getHost(), "language", "en_us");
        prepBr.setReadTimeout(3 * 60 * 1000);
        prepBr.setConnectTimeout(3 * 60 * 1000);
        return prepBr;
    }

    private void checkErrors() throws NumberFormatException, PluginException {
        if (br.containsHTML(">You reached your hourly traffic limit")) {
            final String waitMins = br.getRegex("style=\" color: green; font-size: 28px; \">(\\d+)</span> minutes</span>").getMatch(0);
            if (waitMins != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waitMins) * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.yunfile.com/user/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    // Works like MountFileCom and HowFileCom
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = br.openGetConnection(link.getDownloadURL());
        if (con.getResponseCode() == 503 || con.getResponseCode() == 404) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.followConnection();
        if (br.containsHTML("<title> \\- Yunfile\\.com \\- Free File Hosting and Sharing, Permanently Save </title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Access denied
        if (br.containsHTML("Access denied<|资源已被禁止访问</span>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Not found
        if (br.containsHTML("<span>资源未找到</span>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null, filesize = null;
        // if (br.getURL().matches("http://page\\d+\\.yunfile.com/fs/[a-z0-9]+/")) ;
        filename = br.getRegex("class=\"title\">Downloading:\\&nbsp;\\&nbsp;([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\- Yunfile\\.com \\- Free File Hosting and Sharing, Permanently Save </title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h2 class=\"title\">文件下载\\&nbsp;\\&nbsp;([^<>\"]*?)</h2>").getMatch(0);
        filesize = br.getRegex("文件大小: <b>([^<>\"]*?)</b>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("File Size: <b>([^<>\"]*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("您需要下载等待 <")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 1 * 60 * 1001l);
        checkErrors();
        final Regex siteInfo = br.getRegex("<span style=\"font\\-weight:bold;\">\\&nbsp;\\&nbsp;<a href=\"(http://[a-z0-9]+\\.yunfile.com)/ls/([A-Za-z0-9\\-_]+)/\"");
        String userid = siteInfo.getMatch(1);
        if (userid == null) userid = new Regex(downloadLink.getDownloadURL(), "yunfile\\.com/file/(.*?)/").getMatch(0);
        String fileid = new Regex(downloadLink.getDownloadURL(), "yunfile\\.com/file/.*?/([a-z0-9]+)").getMatch(0);
        if (fileid == null) fileid = new Regex(downloadLink.getDownloadURL(), "yunfile\\.com/file/([A-Za-z0-9]+)/$").getMatch(0);
        if (fileid == null) fileid = br.getRegex("\\&fileId=([A-Za-z0-9]+)\\&").getMatch(0);
        if (userid == null || fileid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String freeContinueLink = br.getRegex("\"(/file/down/[^<>\"]*?)\"").getMatch(0);
        String domain = siteInfo.getMatch(0);
        if (domain == null) domain = new Regex(br.getURL(), "(http://.*?\\.?yunfile\\.com)").getMatch(0);
        if (freeContinueLink != null) {
            freeContinueLink = domain + freeContinueLink;
        } else {
            freeContinueLink = domain + "/file/down/" + userid + "/" + fileid + ".html";
        }
        if (!br.getURL().contains(domain)) br.getPage(domain + "/file/" + fileid + "/");
        // Check if captcha needed
        if (br.containsHTML("verifyimg/getPcv\\.html")) {
            // final String code = getCaptchaCode("http://yunfile.com/verifyimg/getPcv.html", downloadLink);
            // freeContinueLink = freeContinueLink.replace(".html", "/" + Encoding.urlEncode(code) + ".html");
            throw new PluginException(LinkStatus.ERROR_FATAL, "Captcha handling is broken at the moment");
        }
        int wait = 30;
        String shortWaittime = br.getRegex("id=wait_span style=\"font\\-size: 28px; color: green;\">(\\d+)</span>").getMatch(0);
        if (shortWaittime != null) wait = Integer.parseInt(shortWaittime);
        sleep(wait * 1001l, downloadLink);
        br.getPage(freeContinueLink);

        /** Check here if the plugin is broken */
        final String vid1 = br.getRegex("name=\"vid1\" value=\"([a-z0-9]+)\"").getMatch(0);
        final String vid = br.getRegex("var vericode = \"([a-z0-9]+)\";").getMatch(0);
        final String action = br.getRegex("\"(http://dl\\d+\\.yunfile\\.com/view\\?fid=[a-z0-9]+)\"").getMatch(0);
        final String md5 = br.getRegex("name=\"md5\" value=\"([a-z0-9]{32})\"").getMatch(0);

        if (vid == null || action == null || md5 == null || vid1 == null) {
            // if (br.containsHTML("verifyimg/getPcv\\.html")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        String postData = "module=fileService&action=downfile&userId=" + userid + "&fileId=" + fileid + "&vid=" + vid + "&vid1=" + vid1 + "&md5=" + md5;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, action, postData, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            checkErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false, link.getDownloadURL());
        br.getPage(link.getDownloadURL());
        final String vid1 = br.getRegex("\"vid1\", \"([a-z0-9]+)\"").getMatch(0);
        String dllink = br.getRegex("\"(http://dl\\d+\\.yunfile\\.com/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<td align=center>[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        String[] counter = br.getRegex("document.getElementById\\(\\'.*?\\'\\)\\.src = \"([^\"]+)").getColumn(0);
        if (counter != null || counter.length < 0) {
            String referer = br.getURL();
            for (String count : counter) {
                // need the cookies to update after each response!
                br.getHeaders().put("Referer", referer);
                try {
                    br.getPage(count);
                } catch (Throwable e) {
                }
            }
        }
        if (dllink == null || vid1 == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setCookie(MAINPAGE, "vid1", vid1);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDomainFromLink(final String inputurl) {
        return new Regex(inputurl, "(http://[a-z0-9]+\\.yunfile\\.com)").getMatch(0);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true, "http://www.yunfile.com/explorer/list.html");
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!br.getURL().contains("/user/edit.html")) {
            br.getPage("/user/edit.html");
        }
        String expire = br.getRegex("Expire:(\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
        }
        String space = br.getRegex("Used Space:.+?>(\\d+ (b|kb|mb|gb|tb)), Files").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(space);
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    private void login(Account account, final boolean force, final String returnPath) throws Exception {
        synchronized (LOCK) {
            // Load/Save cookies, if we do NOT do this parallel downloads fail
            prepBrowser(br);
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = account.getUser().matches(account.getStringProperty("name", account.getUser()));
            if (acmatch) acmatch = account.getPass().matches(account.getStringProperty("pass", account.getPass()));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("jforumUserHash") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.getPage(MAINPAGE);
            Form login = br.getFormbyProperty("id", "login_form");
            if (login == null) {
                logger.warning("Could not find login form");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            login.put("username", Encoding.urlEncode(account.getUser()));
            login.put("password", Encoding.urlEncode(account.getPass()));
            login.put("remember", "on");
            br.submitForm(login);
            if (br.getCookie(MAINPAGE, "jforumUserHash") == null || br.getCookie(MAINPAGE, "membership") == null || !"2".equals(br.getCookie(MAINPAGE, "membership"))) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", account.getUser());
            account.setProperty("pass", account.getPass());
            account.setProperty("cookies", cookies);
        }
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