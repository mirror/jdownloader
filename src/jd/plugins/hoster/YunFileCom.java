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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yunfile.com" }, urls = { "http://(www|(p(?:age)?\\d|share)\\.)?(yunfile|filemarkets|yfdisk|needisk)\\.com/(file/(down/)?[a-z0-9]+/[a-z0-9]+|fs/[a-z0-9]+/?)" }, flags = { 2 })
public class YunFileCom extends PluginForHost {

    private static final String            MAINPAGE    = "http://yunfile.com/";
    private static final String            CAPTCHAPART = "/verifyimg/getPcv";
    private static Object                  LOCK        = new Object();
    private static AtomicReference<String> agent       = new AtomicReference<String>();
    private static AtomicInteger           maxPrem     = new AtomicInteger(1);

    // Works like HowFileCom
    public YunFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.yunfile.com/user/premiumMembership.html");
        // this.setStartIntervall(15 * 1000l);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("share.yunfile.com/", "yunfile.com/"));
    }

    @Override
    public String rewriteHost(String host) {
        if ("filemarkets.com".equals(getHost()) || "yfdisk.com".equals(getHost()) || "needisk.com".equals(getHost())) {
            if (host == null || "filemarkets.com".equals(host) || "yfdisk.com".equals(host) || "needisk.com".equals(host)) {
                return "yunfile.com";
            }
        }
        return super.rewriteHost(host);
    }

    private Browser prepBrowser(final Browser prepBr) {
        // // define custom browser headers and language settings.
        // if (agent.get() == null) {
        // /* we first have to load the plugin, before we can reference it */
        // agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        // }
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.111 Safari/537.36");
        prepBr.getHeaders().put("Accept-Language", "en-AU,en;q=0.8");
        prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.setCookie(MAINPAGE, "language", "en_au");
        prepBr.setReadTimeout(3 * 60 * 1000);
        prepBr.setConnectTimeout(3 * 60 * 1000);
        return prepBr;
    }

    private void checkErrors() throws NumberFormatException, PluginException {
        if (br.containsHTML(">You reached your hourly traffic limit")) {
            final String waitMins = br.getRegex("You can wait download for[^\r\n]+(\\d+)</span>\\s*-?minutes</span>").getMatch(0);
            if (waitMins != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waitMins) * 60 * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        } else if (br.containsHTML("class=\"gen\"> Too many connections for file service")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections - wait before starting new downloads");
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
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    // Works like MountFileCom and HowFileCom
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br = new Browser();
        // this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = br.openGetConnection(link.getDownloadURL());
        if (con.getResponseCode() == 503 || con.getResponseCode() == 404) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.followConnection();
        if (br.containsHTML("<title> - Yunfile\\.com - Free File Hosting and Sharing, Permanently Save </title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Access denied
        if (br.containsHTML("Access denied<|资源已被禁止访问</span>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Not found
        if (br.containsHTML("<span>(资源未找到|Not found)</span>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Wrong link */
        if (br.containsHTML(">Wrong</span>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null, filesize = null;
        // if (br.getURL().matches("http://page\\d+\\.yunfile.com/fs/[a-z0-9]+/")) ;
        filename = br.getRegex("Downloading:&nbsp;<a></a>&nbsp;([^<>]*) - [^<>]+<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) - Yunfile\\.com - Free File Hosting and Sharing, Permanently Save </title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<h2 class=\"title\">文件下载&nbsp;&nbsp;([^<>\"]*?)</h2>").getMatch(0);
        }
        filesize = br.getRegex("文件大小: <b>([^<>\"]*?)</b>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("File Size: <b>([^<>\"]*?)</b>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("Downloading:&nbsp;<a></a>&nbsp;[^<>]+ - (\\d*(\\.\\d*)? (K|M|G)?B)[\t\n\r ]*?<").getMatch(0);
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("您需要下载等待 <")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 1 * 60 * 1001l);
        }
        doFree(downloadLink);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String postData = null;
        String action = null;
        String userid = null;
        String fileid = null;
        // br.setCookie("http://yunfile.com/", "validCodeUrl",
        // "\"http://page1.yunfile.com:8880/view?module=service&action=queryValidCode\"");
        // br.setCookie("http://p1.yunfile.com/", "JSESSIONID", br.getCookie("http://yunfile.com/", "JSESSIONID"));
        // final String cssSkyBlue = "http://img.yfdisk.com/templates/yunfile/user/skyblue/css/skyblue.css?version=20150128";
        // this.simulateBrowser(cssSkyBlue);
        // this.simulateBrowser("http://img.yfdisk.com/templates/yunfile/js/jquery.js?version=20150128");
        // this.simulateBrowser("http://img.yfdisk.com/templates/yunfile/user/skyblue/js/skyblue.js?version=20150128");
        // // images, not sure
        // this.simulateBrowser("http://img.yunfile.com/templates/yunfile/images/blank.gif");
        // this.simulateBrowser("http://img.yfdisk.com/templates/default/images/social_skype2.png");
        // this.simulateBrowser("http://img.yunfile.com/images/no_icon.gif");
        // this.simulateBrowser("http://img.yunfile.com/images/yes_icon.gif");
        // this.simulateBrowser("http://img.yfdisk.com/templates/yunfile/images/pay_way03.jpg");
        // this.simulateBrowser("http://img.yfdisk.com/images/logo1.jpg");
        // this.simulateBrowser("http://img.yfdisk.com/templates/yunfile/user/skyblue/images/lang_list2.png");

        final int retry = 5;
        for (int i = 0; i <= retry; i++) {
            if (br.getHttpConnection().getResponseCode() == 404) {
                br.getPage(downloadLink.getDownloadURL());
            }
            if (br.containsHTML("您需要下载等待 <")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 1 * 60 * 1001l);
            }
            checkErrors();
            final Regex siteInfo = br.getRegex("<span style=\"font-weight:bold;\">&nbsp;&nbsp;<a href=\"(http://[a-z0-9]+\\.yunfile.com)/ls/([A-Za-z0-9\\-_]+)/\"");
            userid = siteInfo.getMatch(1);
            if (userid == null) {
                userid = new Regex(downloadLink.getDownloadURL(), "yunfile\\.com/file/(.*?)/").getMatch(0);
            }
            if (fileid == null) {
                fileid = br.getRegex("&fileId=([A-Za-z0-9]+)&").getMatch(0);
            }
            if (userid == null || fileid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String domain = siteInfo.getMatch(0);
            if (domain == null) {
                domain = new Regex(br.getURL(), "(http://.*?\\.?yunfile\\.com)").getMatch(0);
            }
            // if (!br.getURL().contains(domain)) {
            // logger.info("Domain mismatch, retrying...");
            // br.getPage(domain + "/file/" + userid + "/" + fileid + "/");
            // continue;
            // }
            String freelink = domain + "/file/down/" + userid + "/" + fileid + ".html";
            // Check if captcha needed
            if (br.containsHTML(CAPTCHAPART)) {
                String captchalink = br.getRegex("cvimgvip2\\.setAttribute\\(\"src\",(.*?)\\)").getMatch(0);
                if (captchalink == null) {
                    logger.warning("captchalink == null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                captchalink = captchalink.replace("\"", "");
                captchalink = captchalink.replace("+", "");
                final String code = getCaptchaCode(domain + captchalink, downloadLink);
                if ("".equals(code)) {
                    if (i + 1 > retry) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else {
                        // black images... needs another page get
                        br.getPage(br.getURL());
                        continue;
                    }
                }
                freelink = freelink.replace(".html", "/" + Encoding.urlEncode(code) + ".html");
            }
            // this has to be new browser! as shared cookies.... fks dl... //not needed
            // Browser n = prepBrowser(new Browser());
            // n.getHeaders().put("Referer", br.getURL());
            // n.setCookie("www.yunfile.com", "lastViewTime", br.getCookie("p1.yunfile.com", "lastViewTime"));
            // this.simulateBrowser(n, "http://www.yunfile.com//counter.jsp?userId=" + userid + "&fileId=" + fileid + "&dr=" + (i > 0 ?
            // Encoding.urlEncode(downloadLink.getDownloadURL()) : ""), null);
            //
            // this.simulateBrowser("http://img.yfdisk.com/templates/default/images/foot_social.png");
            // this.simulateBrowser(br, "http://img.yfdisk.com/templates/yunfile/user/skyblue/images/mastercard.png", cssSkyBlue);
            // this.simulateBrowser(br, "http://img.yfdisk.com/templates/yunfile/user/skyblue/images/visa.png", cssSkyBlue);
            // this.simulateBrowser("http://img.yunfile.com/templates/default/images/redbutton.jpg");

            int wait = 30;
            String shortWaittime = br.getRegex("\">(\\d+)</span> seconds</span> or").getMatch(0);
            if (shortWaittime != null) {
                wait = Integer.parseInt(shortWaittime);
            }
            sleep((wait * 1000l) + 10, downloadLink);
            //
            // br.setCookie(br.getURL(), "arp_scroll_position", "100"); // not needed
            br.getPage(freelink);
            // loose arp_scroll position cookie
            // br.getRequest().getCookies().remove("arp_scroll_position");
            if (br.containsHTML(CAPTCHAPART)) {
                if (i + 1 > retry) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                continue;
            }
            break;
        }
        /* Check here if the plugin is broken */
        final String savecdnurl = br.getRegex("saveCdnUrl=\"(http[^<>\"]*?)\"").getMatch(0);
        final String finalurl_pt2 = br.getRegex("form\\.setAttribute\\(\"action\",saveCdnUrl\\+\"(view[^<>\"]*?)\"\\);").getMatch(0);
        final String vid1 = br.getRegex("name=\"vid1\" value=\"([a-z0-9]+)\"").getMatch(0);
        final String vid = br.getRegex("var vericode = \"([a-z0-9]+)\";").getMatch(0);
        final String md5 = br.getRegex("name=\"md5\" value=\"([a-z0-9]{32})\"").getMatch(0);
        logger.info("vid = " + vid + " vid1 = " + vid1 + " action = " + action + " md5 = " + md5);
        if (vid1 == null || vid == null || md5 == null || savecdnurl == null || finalurl_pt2 == null) {
            if (br.containsHTML(CAPTCHAPART)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        action = savecdnurl + finalurl_pt2;
        br.setFollowRedirects(true);
        postData = "module=fileService&action=downfile&userId=" + userid + "&fileId=" + fileid + "&vid=" + vid + "&vid1=" + vid1 + "&md5=" + md5;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, action, postData, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors();
            br.followConnection();
            checkErrors();
            if (br.containsHTML(">Please wait")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 60 * 1000l);
        }
    }

    private String getFreelink() {
        String freelink = br.getRegex("id=\"downpage_link\" href=\"(/file/down/[^<>\"]*?)\"").getMatch(0);
        if (freelink == null) {
            freelink = br.getRegex("\"(/file/down/(?!guest)[^<>\"]*?)\"").getMatch(0);
        }
        return freelink;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false, link.getDownloadURL());
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("freeacc", false)) {
            doFree(link);
        } else {
            final String vid1 = br.getRegex("\"vid1\", \"([a-z0-9]+)\"").getMatch(0);
            String dllink = br.getRegex("\"(http://dl\\d+\\.yunfile\\.com/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<td align=center>[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
            }
            final String[] counter = br.getRegex("document.getElementById\\(\\'.*?\\'\\)\\.src = \"([^\"]+)").getColumn(0);
            if (counter != null && counter.length > 0) {
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
                handleServerErrors();
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private String getDomainFromLink(final String inputurl) {
        return new Regex(inputurl, "(http://[a-z0-9]+\\.yunfile\\.com)").getMatch(0);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true, "http://www.yunfile.com/explorer/list.html");
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (!br.getURL().contains("/user/edit.html")) {
            br.getPage("/user/edit.html");
        }
        final String space = br.getRegex("Used Space:.+?>(\\d+ (b|kb|mb|gb|tb)), Files").getMatch(0);
        if (space != null) {
            ai.setUsedSpace(space);
        }
        if (br.getCookie(MAINPAGE, "membership").equals("1")) {
            try {
                maxPrem.set(1);
                // free accounts can still have captcha.
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
            ai.setStatus("Registered (free) user");
            account.setProperty("freeacc", true);
        } else {
            final String expire = br.getRegex("Expire:(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
            }
            account.setValid(true);
            try {
                maxPrem.set(20);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
            ai.setStatus("Premium User");
            account.setProperty("freeacc", false);
        }
        return ai;
    }

    private void login(final Account account, final boolean force, final String returnPath) throws Exception {
        synchronized (LOCK) {
            // Load/Save cookies, if we do NOT do this parallel downloads fail
            prepBrowser(br);
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = account.getUser().matches(account.getStringProperty("name", account.getUser()));
            if (acmatch) {
                acmatch = account.getPass().matches(account.getStringProperty("pass", account.getPass()));
            }
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
            final String lang = System.getProperty("user.language");
            if (login == null) {
                logger.warning("Could not find login form");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            login.put("username", Encoding.urlEncode(account.getUser()));
            login.put("password", Encoding.urlEncode(account.getPass()));
            login.put("remember", "on");
            br.submitForm(login);
            if (br.getCookie(MAINPAGE, "jforumUserHash") == null || br.getCookie(MAINPAGE, "membership") == null) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
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

    private void simulateBrowser(final String url) {
        this.simulateBrowser(br, url, null);
    }

    private void simulateBrowser(final Browser br, final String url, final String referer) {
        if (br == null || url == null) {
            return;
        }
        Browser rb = br.cloneBrowser();
        // javascript
        if (url.contains(".js?") || url.contains(".jsp?")) {
            rb.getHeaders().put("Accept", "*/*");
        } else if (url.contains(".css?")) {
            rb.getHeaders().put("Accept", "*text/css,*/*;q=0.1");
        }
        if (referer != null) {
            rb.getHeaders().put("Referer", referer);
        }

        URLConnectionAdapter con = null;
        try {
            con = rb.openGetConnection(url);
        } catch (final Throwable e) {
        } finally {
            try {
                con.disconnect();
            } catch (final Exception e) {
            }
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