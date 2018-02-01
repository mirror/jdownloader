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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fshare.vn" }, urls = { "https?://(?:www\\.)?(?:mega\\.1280\\.com|fshare\\.vn)/file/([0-9A-Z]+)" })
public class FShareVn extends PluginForHost {
    private final String         SERVERERROR                  = "Tài nguyên bạn yêu cầu không tìm thấy";
    private final String         IPBLOCKED                    = "<li>Tài khoản của bạn thuộc GUEST nên chỉ tải xuống";
    private static Object        LOCK                         = new Object();
    private String               dllink                       = null;
    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    // private static final boolean ACCOUNT_FREE_RESUME = false;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -3;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    public FShareVn(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l);
        this.enablePremium("https://www.fshare.vn/payment/package/?type=vip");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("mega.1280.com", "fshare.vn"));
        if (link.getSetLinkID() == null) {
            final String uid = getUID(link);
            if (uid != null) {
                link.setLinkID(this.getHost() + "://" + uid);
            }
        }
    }

    private String getUID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public String rewriteHost(String host) {
        if ("mega.1280.com".equals(host)) {
            return "fshare.vn";
        }
        return super.rewriteHost(host);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br = new Browser();
        this.setBrowserExclusive();
        correctDownloadLink(link);
        prepBrowser();
        br.setFollowRedirects(false);
        // enforce english
        br.getHeaders().put("Referer", link.getDownloadURL());
        br.getPage("https://www.fshare.vn/site/location?lang=vi"); // en - English version is having problems in version 3
        String redirect = br.getRedirectLocation();
        if (redirect != null) {
            final boolean follows_redirects = br.isFollowingRedirects();
            URLConnectionAdapter con = null;
            br.setFollowRedirects(true);
            try {
                con = br.openHeadConnection(redirect);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    br.followConnection();
                    if (con.getRequestMethod() == RequestMethod.HEAD) {
                        br.getPage(redirect);
                    }
                } else {
                    link.setName(getFileNameFromHeader(con));
                    try {
                        // @since JD2
                        link.setVerifiedFileSize(con.getLongContentLength());
                    } catch (final Throwable t) {
                        link.setDownloadSize(con.getLongContentLength());
                    }
                    // lets also set dllink
                    dllink = br.getURL();
                    return AvailableStatus.TRUE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
                br.setFollowRedirects(follows_redirects);
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("content=\"Error 404\"|<title>Not Found - Fshare<|<title>Fshare \\– Dịch vụ chia sẻ số 1 Việt Nam \\– Cần là có \\- </title>|b>Liên kết bạn chọn không tồn tại trên hệ thống Fshare</|<li>Liên kết không chính xác, hãy kiểm tra lại|<li>Liên kết bị xóa bởi người sở hữu\\.<|>\\s*Your requested file does not existed\\.\\s*<|>The file has been deleted by the user\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("file\" title=\"(.*?)\">").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<p><b>Tên file:</b> (.*?)</p>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<i class=\"fa fa\\-file[^\"]*?\"></i>\\s*(.*?)\\s*</div>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>(?:Fshare - )?(.*?)(?: - Fshare)?</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<i class=\"fa fa\\-file\\-o\"></i>\\s*(.*?)\\s*</div>").getMatch(0);
        }
        String filesize = br.getRegex("<i class=\"fa fa-hdd-o\"></i>\\s*(.*?)\\s*</div>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex(">\\s*([\\d\\.]+ [K|M|G]B)\\s*<").getMatch(0);
        }
        if (filename == null) {
            logger.info("filename = " + filename + ", filesize = " + filesize);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Server sometimes sends bad filenames
        link.setFinalFileName(Encoding.htmlDecode(filename));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink downloadLink, final Account acc) throws Exception {
        if (dllink != null) {
            // these are effectively premium links?
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (!dl.getConnection().getContentType().contains("html")) {
                dl.startDownload();
                return;
            } else {
                br.followConnection();
                dllink = null;
            }
        }
        final String directlinkproperty;
        if (acc != null) {
            directlinkproperty = "account_free_directlink";
        } else {
            directlinkproperty = "directlink";
        }
        dllink = this.checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            simulateBrowser();
            if (dllink == null) {
                if (br.containsHTML(IPBLOCKED)) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
                }
                // we want _csrf token
                final String csrf = br.getRegex("_csrf-app\" value=\"([^<>\"]+)\"").getMatch(0);
                final Browser ajax = br.cloneBrowser();
                ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                ajax.getHeaders().put("x-requested-with", "XMLHttpRequest");
                ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                // final String postdata = "_csrf-app=" + csrf + "&DownloadForm%5Bpwd%5D=&DownloadForm%5Blinkcode%5D=" +
                // getUID(downloadLink) + "&ajax=download-form&undefined=undefined";
                // ajax.postPage("/download/get", postdata);
                Form form = br.getFormbyAction("/download/get");
                for (int i = 1; i < 3; i++) {
                    ajax.submitForm(form);
                    if (ajax.containsHTML("url")) {
                        break;
                    }
                    if (ajax.containsHTML("Too many download sessions") || ajax.containsHTML("Quá nhiều phiên tải")) {
                        sleep(10 * 1001l, downloadLink);
                    }
                }
                if (ajax.containsHTML("Too many download sessions") || ajax.containsHTML("Quá nhiều phiên tải")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many download sessions", 3 * 60 * 1000l);
                } else if (ajax.containsHTML("\"errors\":")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, ajax.toString(), 3 * 60 * 1000l);
                }
                if (StringUtils.containsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "msg"), "Server error") && StringUtils.containsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "msg"), "please try again later")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                }
                dllink = PluginJSonUtils.getJsonValue(ajax, "url");
                if (dllink != null && br.containsHTML(IPBLOCKED) || ajax.containsHTML(IPBLOCKED)) {
                    final String nextDl = br.getRegex("LÆ°á»£t táº£i xuá»‘ng káº¿ tiáº¿p lÃ : ([^<>]+)<").getMatch(0);
                    logger.info("Next download: " + nextDl);
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
                }
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    rc.setCode(c);
                    if (br.containsHTML("frm_download")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                if (dllink == null) {
                    dllink = br.getRegex("window\\.location='(.*?)'").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("value=\"Download\" name=\"btn_download\" value=\"Download\"  onclick=\"window\\.location='(http://.*?)'\"").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("<form action=\"(http://download[^\\.]+\\.fshare\\.vn/download/[^<>]+/.*?)\"").getMatch(0);
                        }
                    }
                }
                logger.info("downloadURL = " + dllink);
                // Waittime
                String wait = PluginJSonUtils.getJsonValue(ajax, "wait_time");
                if (wait == null) {
                    br.getRegex("var count = \"(\\d+)\";").getMatch(0);
                    if (wait == null) {
                        wait = br.getRegex("var count = (\\d+);").getMatch(0);
                        if (wait == null) {
                            wait = "35";
                            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
                // No downloadlink shown, host is buggy
                if (dllink == null && "0".equals(wait)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
                }
                if (wait == null || dllink == null || !dllink.contains("fshare.vn")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                sleep(Long.parseLong(wait) * 1001l, downloadLink);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(SERVERERROR)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private void prepBrowser() {
        // Sometime the page is extremely slow!
        br.setReadTimeout(120 * 1000);
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.setCustomCharset("utf-8");
    }

    @Override
    public String getAGBLink() {
        return "http://www.fshare.vn/policy.php?action=sudung";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        // this should set English here...
        requestFileInformation(link);
        if (account.getType() == AccountType.FREE) {
            // premium link wont need user to login!
            if (dllink == null) {
                login(account, true);
                br.getPage(link.getDownloadURL());
                dllink = br.getRedirectLocation();
            }
            doFree(link, account);
        } else {
            final String directlinkproperty = "directlink_account";
            // English is also set here && cache login causes problems, premium pages sometimes not returned without fresh login.
            login(account, true);
            dllink = this.checkDirectLink(link, directlinkproperty);
            if (dllink == null) {
                // we get page again, because we do not take directlink from requestfileinfo.
                br.getPage(link.getDownloadURL());
                dllink = br.getRedirectLocation();
                final String uid = getUID(link);
                if (dllink != null && dllink.endsWith("/file/" + uid)) {
                    br.getPage(dllink);
                    if (br.containsHTML("Your account is being used from another device")) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Account is being used in another device");
                    }
                    dllink = br.getRedirectLocation();
                }
                if (dllink == null) {
                    if (br.containsHTML(">\\s*Fshare suspect this account has been stolen or is being used by other people\\.|Please press “confirm” to get a verification code, it’s sent to your email address\\.<")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account determined as stolen or shared...", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    dllink = br.getRegex("\"(https?://[a-z0-9]+\\.fshare\\.vn/(vip|dl)/[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        String page = getDllink(); // <---
                        for (int i = 1; i < 3; i++) {
                            if (page.contains("url")) {
                                break;
                            }
                            if (page.contains("Too many download sessions") || page.contains("Quá nhiều phiên tải")) {
                                sleep(10 * 1001l, link);
                                page = getDllink(); // <---
                            }
                        }
                        if (page.contains("Too many download sessions") || page.contains("Quá nhiều phiên tải")) {
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many download sessions", 3 * 60 * 1000l);
                        } else if (page.contains("\"errors\":")) {
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, page, 3 * 60 * 1000l);
                        }
                        dllink = PluginJSonUtils.getJsonValue(page, "url");
                        if (dllink == null) {
                            final String msg = PluginJSonUtils.getJsonValue(page, "msg");
                            if (StringUtils.containsIgnoreCase(msg, "try again")) {
                                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 5 * 60 * 1000l);
                            }
                        } else {
                            dllink = dllink.replace("\\", "");
                        }
                    }
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (StringUtils.containsIgnoreCase(dllink, "Server error") && StringUtils.containsIgnoreCase(dllink, "please try again later")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                } else if (dllink.contains("logout")) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL premium error");
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML(SERVERERROR)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    public String getDllink() throws Exception {
        final Form dlfast = br.getFormbyAction("/download/get");
        if (dlfast != null) {
            // Fix form
            if (!dlfast.hasInputFieldByName("ajax")) {
                dlfast.put("ajax", "download-form");
            }
            if (!dlfast.hasInputFieldByName("undefined")) {
                dlfast.put("undefined", "undefined");
            }
            dlfast.remove("DownloadForm%5Bpwd%5D");
            dlfast.put("DownloadForm[pwd]", "");
            // button base download here,
            final Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            ajax.getHeaders().put("x-requested-with", "XMLHttpRequest");
            ajax.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            ajax.submitForm(dlfast);
            return ajax.toString();
        }
        return null;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBrowser();
                /** Load cookies */
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
                            br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                final boolean isFollowingRedirects = br.isFollowingRedirects();
                br.setFollowRedirects(true);
                // enforce English
                br.getHeaders().put("Referer", "https://www.fshare.vn/site/login");
                br.getPage("https://www.fshare.vn"); // 503 with /site/location?lang=en");
                final String csrf = br.getRegex("name=\"_csrf-app\" value=\"([^<>\"]+)\"").getMatch(0);
                if (csrf == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.setFollowRedirects(false);
                br.postPage("/site/login", "_csrf-app=" + csrf + "&LoginForm%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&LoginForm%5BrememberMe%5D=0&LoginForm%5BrememberMe%5D=1");
                if (br.containsHTML("class=\"errorMessage\"")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.getURL().contains("/resend")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDein Account ist noch nicht aktiviert. Bestätige die Aktivierungsmail um ihn verwenden zu können..", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account is not activated yet. Confirm the activation mail to use it.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                br.setFollowRedirects(isFollowingRedirects);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("/account/profile");
        String validUntil = br.getRegex(">Hạn dùng:<strong[^>]+>\\&nbsp;(\\d+\\-\\d+\\-\\d+)</strong>").getMatch(0);
        if (validUntil == null) {
            validUntil = br.getRegex(">Hạn dùng:<strong>\\&nbsp;([^<>\"]*?)</strong>").getMatch(0);
            if (validUntil == null) {
                validUntil = br.getRegex("<dt>Hạn dùng</dt>[\t\n\r ]+<dd><b>([^<>\"]*?)</b></dd>").getMatch(0);
                if (validUntil == null) {
                    validUntil = br.getRegex("Hạn dùng:\\s*([^<>\"]*?)(?:</a>)?</p></li>").getMatch(0);
                    if (validUntil == null) {
                        // validUntil = br.getRegex("Expire:\\s*([^<>\"]*?)(?:</a>)?</p></li>").getMatch(0);
                        validUntil = br.getRegex("(?:Expire|Hạn dùng):</a>\\s*<span.*?>([^<>]*?)</span>").getMatch(0); // Version 3 (2018)
                    }
                }
            }
        }
        String accountType = br.getRegex("<dt>Account type</dt>.*?member\">\\s*(.*?)\\s*<").getMatch(0);
        if (accountType != null) {
            accountType = br.getRegex("<dt>>Loại tài khoản</dt>.*?member\">\\s*(.*?)\\s*<").getMatch(0);
        }
        if (StringUtils.equalsIgnoreCase(accountType, "VIP")) {
            ai.setStatus("VIP Account");
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            if (validUntil != null) {
                long validuntil = 0;
                if (validUntil.contains("-")) {
                    validuntil = TimeFormatter.getMilliSeconds(validUntil, "dd-MM-yyyy", Locale.ENGLISH);
                } else {
                    validuntil = TimeFormatter.getMilliSeconds(validUntil, "dd/MM/yyyy", Locale.ENGLISH);
                }
                if (validuntil > 0) {
                    validuntil += 24 * 60 * 60 * 1000l;
                }
                ai.setValidUntil(validuntil, br, "EEE, dd MMM yyyy HH:mm:ss z");
            }
        } else if (validUntil != null) {
            long validuntil = 0;
            if (validUntil.contains("-")) {
                validuntil = TimeFormatter.getMilliSeconds(validUntil, "dd-MM-yyyy", Locale.ENGLISH);
            } else {
                validuntil = TimeFormatter.getMilliSeconds(validUntil, "dd/MM/yyyy", Locale.ENGLISH);
            }
            if (validuntil > 0) {
                validuntil += 24 * 60 * 60 * 1000l;
            }
            ai.setValidUntil(validuntil, br, "EEE, dd MMM yyyy HH:mm:ss z");
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
        } else if (br.containsHTML(">BUNDLE</a>")) {
            /* This is a kind of account that they give to their ADSL2+/FTTH service users. It works like VIP. */
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Bundle Account");
            account.setType(AccountType.PREMIUM);
        } else {
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Free Account");
            account.setType(AccountType.FREE);
        }
        account.setValid(true);
        return ai;
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
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    private LinkedHashSet<String> dupe = new LinkedHashSet<String>();

    /**
     * @author raztoki
     */
    private void simulateBrowser() throws InterruptedException {
        final AtomicInteger requestQ = new AtomicInteger(0);
        final AtomicInteger requestS = new AtomicInteger(0);
        final ArrayList<String> links = new ArrayList<String>();
        String[] l1 = br.getRegex("\\s+(?:src|href)=(\"|')(.*?)\\1").getColumn(1);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        l1 = br.getRegex("\\s+(?:src|href)=(?!\"|')([^\\s]+)").getColumn(0);
        if (l1 != null) {
            links.addAll(Arrays.asList(l1));
        }
        for (final String link : links) {
            // lets only add links related to this hoster.
            final String correctedLink = Request.getLocation(link, br.getRequest());
            if (!"fshare.vn".equalsIgnoreCase(Browser.getHost(correctedLink))) {
                continue;
            }
            if (!correctedLink.contains(".png") && !correctedLink.contains(".js") && !correctedLink.contains(".css")) {
                continue;
            }
            if (dupe.add(correctedLink)) {
                final Thread simulate = new Thread("SimulateBrowser") {
                    public void run() {
                        final Browser rb = br.cloneBrowser();
                        rb.getHeaders().put("Cache-Control", null);
                        // open get connection for images, need to confirm
                        if (correctedLink.matches(".+\\.png.*")) {
                            rb.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
                        }
                        if (correctedLink.matches(".+\\.js.*")) {
                            rb.getHeaders().put("Accept", "*/*");
                        } else if (correctedLink.matches(".+\\.css.*")) {
                            rb.getHeaders().put("Accept", "text/css,*/*;q=0.1");
                        }
                        URLConnectionAdapter con = null;
                        try {
                            requestQ.getAndIncrement();
                            con = rb.openGetConnection(correctedLink);
                        } catch (final Exception e) {
                        } finally {
                            try {
                                con.disconnect();
                            } catch (final Exception e) {
                            }
                            requestS.getAndIncrement();
                        }
                    }
                };
                simulate.start();
                Thread.sleep(100);
            }
        }
        while (requestQ.get() != requestS.get()) {
            Thread.sleep(1000);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
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
}