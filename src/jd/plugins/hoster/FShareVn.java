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
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fshare.vn", "mega.1280.com" }, urls = { "https?://(www\\.)?(mega\\.1280\\.com|fshare\\.vn)/file/[0-9A-Z]+", "dgjediz65854twsdfbtzoi6UNUSED_REGEX" }, flags = { 2, 0 })
public class FShareVn extends PluginForHost {

    private static final String  SERVERERROR = "Tài nguyên bạn yêu cầu không tìm thấy";
    private static final String  IPBLOCKED   = "<li>Tài khoản của bạn thuộc GUEST nên chỉ tải xuống";
    private static Object        LOCK        = new Object();
    private static AtomicInteger maxPrem     = new AtomicInteger(1);
    private String               dllink      = null;

    public FShareVn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fshare.vn/buyacc.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("mega.1280.com", "fshare.vn"));
    }

    @Override
    public String rewriteHost(String host) {
        if ("mega.1280.com".equals(getHost())) {
            if (host == null || "mega.1280.com".equals(host)) {
                return "fshare.vn";
            }
        }
        return super.rewriteHost(host);
    }

    /** TODO: Find out of that thing still exists */
    // @Override
    // public boolean checkLinks(final DownloadLink[] urls) {
    // if (urls == null || urls.length == 0) {
    // return false;
    // }
    // try {
    // final Browser br = new Browser();
    // prepBrowser();
    // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    // br.setCookiesExclusive(true);
    // final StringBuilder sb = new StringBuilder();
    // final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
    // int index = 0;
    // while (true) {
    // links.clear();
    // while (true) {
    // /* we test 50 links at once, maybe even more is possible */
    // if (index == urls.length || links.size() > 49) {
    // break;
    // }
    // links.add(urls[index]);
    // index++;
    // }
    // sb.delete(0, sb.capacity());
    // sb.append("action=check_link&arrlinks=");
    // for (final DownloadLink dl : links) {
    // sb.append(dl.getDownloadURL());
    // sb.append("%0A");
    // }
    // br.postPage("http://www.fshare.vn/check_link.php", sb.toString());
    // br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
    // for (final DownloadLink dllink : links) {
    // final String fid = new Regex(dllink.getDownloadURL(), "([A-Z0-9]+)$").getMatch(0);
    // final String[][] fileInfo = br.getRegex("/file/" + fid +
    // "</a></b></p>[a-z0-9]+<p>([^<>\"]*?)</p>ttrntt<p>([^<>\"]*?)</p>").getMatches();
    // if (fileInfo == null || fileInfo.length == 0) {
    // dllink.setAvailable(false);
    // logger.warning("Linkchecker broken for " + getHost() + " Example link: " + dllink.getDownloadURL());
    // } else if (br.containsHTML("/file/" + fid + "</a></b></p>[a-z0-9]+<p></p>[a-z0-9]+<p>0 KB</p>")) {
    // dllink.setAvailable(false);
    // } else {
    // dllink.setName(fileInfo[0][0]);
    // dllink.setAvailable(true);
    // dllink.setDownloadSize(SizeFormatter.getSize(fileInfo[0][1]));
    // }
    // }
    // if (index == urls.length) {
    // break;
    // }
    // }
    // } catch (final Exception e) {
    // return false;
    // }
    // return true;
    // }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String redirect = br.getRedirectLocation();
        if (redirect != null) {
            final boolean follows_redirects = br.isFollowingRedirects();
            URLConnectionAdapter con = null;
            br.setFollowRedirects(true);
            try {
                if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
                    con = br.openHeadConnection(redirect);
                } else {
                    con = br.openGetConnection(redirect);
                }
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    br.followConnection();
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
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(<title>Fshare \\– Dịch vụ chia sẻ số 1 Việt Nam \\– Cần là có \\- </title>|b>Liên kết bạn chọn không tồn tại trên hệ thống Fshare</|<li>Liên kết không chính xác, hãy kiểm tra lại|<li>Liên kết bị xóa bởi người sở hữu\\.<)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("file\" title=\"(.*?)\">").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<p><b>Tên file:</b> (.*?)</p>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<p><b>Tên tập tin:</b> (.*?)</p>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) \\- Fshare \\- Dịch vụ chia sẻ, lưu trữ dữ liệu miễn phí tốt nhất </title>").getMatch(0);
        }
        String filesize = br.getRegex("<p><b>Dung lượng: </b>(.*?)</p>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("10px\">([\\d\\.]+ [K|M|G]B)<").getMatch(0);
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

    public void doFree(DownloadLink downloadLink) throws Exception {
        if (dllink != null) {
            // these are effectively premium links?
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (!dl.getConnection().getContentType().contains("html")) {
                dl.startDownload();
                return;
            } else {
                dllink = null;
            }
        }
        if (dllink == null) {
            if (br.containsHTML(IPBLOCKED)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
            }
            Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "*/*");
            ajax.getHeaders().put("x-requested-with", "XMLHttpRequest");
            ajax.getPage("/download/index");
            dllink = ajax.getRegex("(https?://download[^/]*fshare\\.vn/dl/.+)").getMatch(0);
            if (dllink != null && br.containsHTML(IPBLOCKED) || ajax.containsHTML(IPBLOCKED)) {
                final String nextDl = br.getRegex("LÆ°á»£t táº£i xuá»‘ng káº¿ tiáº¿p lÃ : ([^<>]+)<").getMatch(0);
                logger.info("Next download: " + nextDl);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
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
                dllink = br.getRegex("window\\.location=\\'(.*?)\\'").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("value=\"Download\" name=\"btn_download\" value=\"Download\"  onclick=\"window\\.location=\\'(http://.*?)\\'\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("<form action=\"(http://download[^\\.]+\\.fshare\\.vn/download/[^<>]+/.*?)\"").getMatch(0);
                    }
                }
            }
            logger.info("downloadURL = " + dllink);
            // Waittime
            String wait = br.getRegex("var count = \"(\\d+)\";").getMatch(0);
            if (wait == null) {
                wait = br.getRegex("var count = (\\d+);").getMatch(0);
            }
            if (wait == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(SERVERERROR)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepBrowser() {
        // Sometime the page is extremely slow!
        br.setReadTimeout(120 * 1000);
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
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        // requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        if (account.getStringProperty("acctype") != null) {
            doFree(link);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                dllink = br.getRegex("\"(http://[a-z0-9]+\\.fshare\\.vn/vip/[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.contains("logout")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL premium error");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
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

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
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
                            this.br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("https://www.fshare.vn/login");
                final String fs_csrf = br.getRegex("value=\"([a-z0-9]+)\" name=\"fs_csrf\"").getMatch(0);
                if (fs_csrf == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage("https://www.fshare.vn/login", "fs_csrf=" + fs_csrf + "&LoginForm%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&LoginForm%5BrememberMe%5D=0&LoginForm%5BrememberMe%5D=1&yt0=%C4%90%C4%83ng+nh%E1%BA%ADp");
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
                final Cookies add = this.br.getCookies(this.getHost());
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        String revsion = JDUtilities.getRevision();
        if (revsion == null || revsion.length() < 3) {
            revsion = "0";
        } else {
            revsion = revsion.replaceAll(",|\\.", "");
        }
        final int rev = Integer.parseInt(revsion);
        if (rev < 10000) {
            premiumWarning();
            account.setValid(false);
            return ai;
        }
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        login(account, true);
        if (!br.getURL().endsWith("/home")) {
            br.getPage("/home");
        }
        String validUntil = br.getRegex(">Hạn dùng:<strong[^>]+>\\&nbsp;(\\d+\\-\\d+\\-\\d+)</strong>").getMatch(0);
        if (validUntil == null) {
            validUntil = br.getRegex(">Hạn dùng:<strong>\\&nbsp;([^<>\"]*?)</strong>").getMatch(0);
        }
        if (validUntil == null) {
            validUntil = br.getRegex("<dt>Hạn dùng</dt>[\t\n\r ]+<dd><b>([^<>\"]*?)</b></dd>").getMatch(0);
        }
        if (validUntil == null) {
            validUntil = br.getRegex("Hạn dùng: ([^<>\"]*?)</p></li>").getMatch(0);
        }
        if (validUntil == null) {
            validUntil = br.getRegex("Expire: ([^<>\"]*?)</p></li>").getMatch(0);
        }
        if (br.containsHTML("title=\"Platium\">VIP </span>")) {
            ai.setStatus("Vip User");
            account.setProperty("acctype", Property.NULL);
        } else if (validUntil != null) {
            if (validUntil.contains("-")) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd-MM-yyyy", Locale.ENGLISH));
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd/MM/yyyy", Locale.ENGLISH));
            }
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            ai.setStatus("Premium User");
            account.setProperty("acctype", Property.NULL);
        } else {
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            ai.setStatus("Registered (free) User");
            account.setProperty("acctype", "free");
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
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    private static void premiumWarning() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        // if ("de".equalsIgnoreCase(lng)) {
                        // title = "fshare.vn Premium-Probleme";
                        // message = "\r\n";
                        // message += "\r\n";
                        // message += "";
                        // } else {
                        title = "fshare.vn Premium Issues";
                        message = "Hi, This version of JDownloader can not be used to login into fshare.vn, you will need to to install JDownloader 2.\r\n";
                        message += "More information can be found on our fourm, when selecting OK you will proceed to our forum in your default web browser.\r\n";
                        message += "http://board.jdownloader.org/showthread.php?t=37365\r\n";
                        // }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                            if (JOptionPane.OK_OPTION == result) {
                                CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
                            }
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }
}