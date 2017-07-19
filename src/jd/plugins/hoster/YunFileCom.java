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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.CookieStorable;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yunfile.com" }, urls = { "http://(www|(p(?:age)?\\d|share)\\.)?(?:yunfile|filemarkets|yfdisk|needisk|5xpan|dix3|dfpan)\\.com/(file/(down/)?[a-z0-9]+/[a-z0-9]+|fs/[a-z0-9]+/?)" })
public class YunFileCom extends PluginForHost {
    private static final String            MAINPAGE    = "http://www.yunfile.com/";
    private static final String            CAPTCHAPART = "/verifyimg/getPcv";
    private static Object                  LOCK        = new Object();
    private static AtomicReference<String> agent       = new AtomicReference<String>();
    private static final String            DOMAINS     = "(?:yunfile|filemarkets|yfdisk|needisk|5xpan|dix3|dfpan)\\.com";

    // Works like HowFileCom
    public YunFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "user/premiumMembership.html");
        // this.setStartIntervall(15 * 1000l);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        // to fix jiaz bad commit
        link.setUrlDownload(link.getDownloadURL().replaceAll("(?:share|p(?:age)?\\d*|www)\\.www\\.yunfile\\.com/", "yunfile.com/"));
        // standard
        link.setUrlDownload(link.getDownloadURL().replace("share.yunfile.com/", "yunfile.com/").replaceFirst("(?:filemarkets|yfdisk|needisk|5xpan|dix3|dfpan)\\.com/", "yunfile.com/"));
    }

    @Override
    public String rewriteHost(final String host) {
        if ("yunfile.com".equals(this.getHost())) {
            if (host == null || "filemarkets.com".equals(host) || "yfdisk.com".equals(host) || "needisk.com".equals(host) || "5xpan.com".equals(host) || "dix3.com".equals(host) || "dfpan.com".equals(host)) {
                return "yunfile.com";
            }
        }
        return super.rewriteHost(host);
    }

    private Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        if (agent.get() == null) {
            agent.set(UserAgents.stringUserAgent(BrowserName.Chrome));
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
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
        return MAINPAGE + "user/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // Works like MountFileCom and HowFileCom
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br = new Browser();
        // need to correct links that are added prior to fixing!
        correctDownloadLink(link);
        prepBrowser(br);
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = br.openGetConnection(link.getDownloadURL());
        if (con.getResponseCode() == 503 || con.getResponseCode() == 404) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.followConnection();
        if (br.containsHTML("<title>\\s*-\\s*(?:Yunfile|Dix3)[^<]*</title>")) {
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
            filename = br.getRegex("<title>(.*?)\\s*-\\s*(?:Yunfile|Dix3)[^<]*</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<h2 class=\"title\">文件下载&nbsp;&nbsp;([^<>\"]*?)</h2>").getMatch(0);
        }
        filesize = br.getRegex("文件大小: <b>([^<>\"]*?)</b>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("File Size: <b>([^<>\"]*?)</b>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("Downloading:&nbsp;<a></a>&nbsp;[^<>]+ - (\\d*(\\.\\d*)? (K|M|G)?B)[\t\n\r ]*?<").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("class=\"file_title\">\\&nbsp;Downloading:\\&nbsp;\\&nbsp;[^<>\"]*? \\- ([^<>\"]*?) </h2>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(decode("111", filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    private String decode(final String key, final String data) throws IOException {
        final String js1 = "function codeAndEncode(_key,_str) {\r\n\t var keyUnicodeSum = 0;\r\n\t var codedStr = \"\";\r\n\t for (j = 0; j < _key.length; j++) {\r\n\t\t  keyUnicodeSum += _key.charCodeAt(j);\r\n\t }\r\n\t for (i = 0; i < _str.length; i++) {\r\n\t\t  var _strXOR = _str.charCodeAt(i) ^ keyUnicodeSum;\r\n\t\t  codedStr += String.fromCharCode(_strXOR);\r\n\t }\r\n\t return codedStr;\r\n}";
        final ScriptEngineManager manager = org.jdownloader.scripting.JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        String result = null;
        try {
            engine.eval(js1);
            result = (String) inv.invokeFunction("codeAndEncode", key, data);
        } catch (final Throwable e) {
            // e.printStackTrace();
        }
        return result;
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
        final int retry = 5;
        for (int i = 0; i <= retry; i++) {
            if (br.getHttpConnection().getResponseCode() == 404) {
                br.getPage(downloadLink.getDownloadURL());
            }
            if (br.containsHTML("您需要下载等待 <")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 1 * 60 * 1001l);
            }
            checkErrors();
            final Regex siteInfo = br.getRegex("<span style=\"font-weight:bold;\">&nbsp;&nbsp;<a href=\"(https?://[a-z0-9]+\\." + DOMAINS + "/ls/([A-Za-z0-9\\-_]+)/)\"");
            userid = siteInfo.getMatch(1);
            if (userid == null) {
                userid = br.getRegex("\\&userId=([A-Za-z0-9]+)").getMatch(0);
            }
            if (fileid == null) {
                fileid = br.getRegex("\\&fileId=([A-Za-z0-9]+)").getMatch(0);
            }
            String freelink = this.br.getRegex("var url\\s*?=\\s*?\"(/file/down/[^<>\"\\']+\\.html)\";").getMatch(0);
            if (freelink == null && userid != null && fileid != null) {
                freelink = Request.getLocation("/file/down/" + userid + "/" + fileid + ".html", br.getRequest());
            }
            if (freelink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // Check if captcha needed
            if (br.containsHTML(CAPTCHAPART)) {
                String captchalink = br.getRegex("cvimgvip2\\.setAttribute\\(\"src\",(.*?)\\)").getMatch(0);
                if (captchalink == null) {
                    logger.warning("captchalink == null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                captchalink = captchalink.replace("\"", "");
                captchalink = captchalink.replace("+", "");
                final String code = getCaptchaCode(captchalink, downloadLink);
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
            int wait = 30;
            String shortWaittime = br.getRegex("\">(\\d+)</span> seconds</span> or").getMatch(0);
            if (shortWaittime != null) {
                wait = Integer.parseInt(shortWaittime);
            }
            sleep((wait * 1000l) + 10, downloadLink);
            br.getPage(freelink);
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
        postData = "module=fileService&userId=" + userid + "&fileId=" + fileid + "&vid=" + vid + "&vid1=" + vid1 + "&md5=" + md5;
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

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(true);
        if (AccountType.FREE.equals(account.getType())) {
            br.getPage(link.getDownloadURL());
            doFree(link);
        } else {
            final Map<String, String> dllinkVidMap = new HashMap<String, String>();
            // get download links, sites with different language have different links (servers)
            for (String language : new String[] { "zh_cn", "en_au" }) {
                br.setCookie(br.getURL(), "language", language);
                br.getPage(br.getURL());
                final String vid1 = br.getRegex("\"vid1\", \"([a-z0-9]+)\"").getMatch(0);
                for (String dllink : br.getRegex("\"(https?://dl\\d+\\." + DOMAINS + "/downfile/[^<>\"]*?)\"").getColumn(0)) {
                    dllinkVidMap.put(dllink, vid1);
                }
                if (dllinkVidMap.size() == 0) { // try to login if not found
                    login(account, true);
                }
                // dllink = br.getRegex("<td align=center>[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
            }
            final String[] counter = br.getRegex("document.getElementById\\('.*?'\\)\\.src = \"([^\"]+)").getColumn(0);
            if (counter != null && counter.length > 0) {
                for (String count : counter) {
                    // need the cookies to update after each response!
                    Browser br = this.br.cloneBrowser();
                    br.getHeaders().put("Accept", "*/*");
                    br.getHeaders().put("Cache-Control", null);
                    br.getHeaders().put("Accept-Language", null);
                    try {
                        br.cloneBrowser().getPage(count);
                    } catch (Throwable e) {
                    }
                }
            }
            if (dllinkVidMap.size() == 0) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final java.util.List<String> dllinks = new ArrayList<String>(dllinkVidMap.keySet());
            java.util.Collections.shuffle(dllinks); // Shuffle for load balancing
            final Iterator<String> it = dllinks.iterator();
            while (it.hasNext()) {
                final String dllink = it.next();
                br.setCookie(dllink, "vid1", dllinkVidMap.get(dllink));
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
                if (dl.getConnection().getResponseCode() == 503 || dl.getConnection().getResponseCode() == 404) {
                    logger.warning("server is busy, try next one");
                    if (it.hasNext()) {
                        continue;
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error " + dl.getConnection().getResponseCode(), 1 * 60 * 1001l);
                } else if (dl.getConnection().getContentType().contains("html")) {
                    handleServerErrors();
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    if (it.hasNext()) {
                        continue;
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    // success
                    dl.startDownload();
                    return;
                }
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
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
            // free accounts can still have captcha.
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
            account.setType(AccountType.FREE);
        } else {
            final String expire = br.getRegex("Expire:(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
            }
            account.setValid(true);
            account.setMaxSimultanDownloads(20);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
        }
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean ifr = br.isFollowingRedirects();
            try {
                if (!force) {
                    try {
                        final HashMap<String, List<CookieStorable>> cookies = account.restoreObject("cookies", new TypeRef<HashMap<String, List<CookieStorable>>>() {
                        });
                        if (importBrowserCookies(br, cookies)) {
                            return;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                prepBrowser(br);
                br.setFollowRedirects(true);
                if (br.getURL() == null) {
                    // only load page if page hasn't already been requested
                    br.getPage(MAINPAGE);
                }
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
                login.put("returnPath", Encoding.urlEncode(br.getURL()));
                br.submitForm(login);
                if (br.getCookie(MAINPAGE, "jforumUserHash") == null || br.getCookie(MAINPAGE, "membership") == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies - ALL OF THEM! they will save cookies to multiple domains! JD stores them in separate keys within hashmap.
                account.storeObject("cookies", exportBrowserCookies(br));
                account.setProperty("name", account.getUser());
                account.setProperty("pass", account.getPass());
            } finally {
                br.setFollowRedirects(ifr);
            }
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

    private boolean importBrowserCookies(Browser br, Map<String, List<CookieStorable>> cookies) {
        if (cookies != null && !cookies.isEmpty()) {
            for (final Map.Entry<String, List<CookieStorable>> cookieEntry : cookies.entrySet()) {
                final String host = cookieEntry.getKey();
                final List<CookieStorable> cs = cookieEntry.getValue();
                final Cookies ret = new Cookies();
                for (final CookieStorable storable : cs) {
                    final Cookie cookie = storable._restore();
                    if (!cookie.isExpired()) {
                        ret.add(cookie);
                    }
                }
                br.setCookies(host, ret);
            }
            return true;
        }
        return false;
    }

    private Map<String, List<CookieStorable>> exportBrowserCookies(Browser br) {
        final HashMap<String, List<CookieStorable>> cookies = new HashMap<String, List<CookieStorable>>();
        /*
         * do not cache antiddos cookies, this is job of the antiddos module, otherwise it can and will cause conflicts!
         */
        final String antiddosCookies = org.jdownloader.plugins.components.antiDDoSForHost.antiDDoSCookiePattern;
        final java.util.Iterator<Entry<String, Cookies>> it = br.getCookies().entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, Cookies> entry = it.next();
            final ArrayList<CookieStorable> cs = new ArrayList<CookieStorable>();
            final String domain = entry.getKey();
            final Cookies ckies = entry.getValue();
            for (final Cookie cookie : ckies.getCookies()) {
                if (cookie.getKey() != null && !cookie.getKey().matches(antiddosCookies) && !cookie.isExpired()) {
                    cs.add(new CookieStorable(cookie));
                }
            }
            cookies.put(domain, cs);
        }
        return cookies;
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