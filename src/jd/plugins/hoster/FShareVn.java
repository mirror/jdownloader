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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fshare.vn", "mega.1280.com" }, urls = { "http://(www\\.)?(mega\\.1280\\.com|fshare\\.vn)/file/[0-9A-Z]+", "dgjediz65854twsdfbtzoi6UNUSED_REGEX" }, flags = { 2, 0 })
public class FShareVn extends PluginForHost {

    private static final String  SERVERERROR = "Tài nguyên bạn yêu cầu không tìm thấy";
    private static final String  IPBLOCKED   = "<li>Tài khoản của bạn thuộc GUEST nên chỉ tải xuống";
    private static Object        LOCK        = new Object();
    private static AtomicInteger maxPrem     = new AtomicInteger(1);

    public FShareVn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fshare.vn/buyacc.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("mega.1280.com", "fshare.vn"));
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            prepBrowser();
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once, maybre even more is possible */
                    if (index == urls.length || links.size() > 49) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("action=check_link&arrlinks=");
                for (final DownloadLink dl : links) {
                    sb.append(dl.getDownloadURL());
                    sb.append("%0A");
                }
                br.postPage("http://www.fshare.vn/check_link.php", sb.toString());
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                for (final DownloadLink dllink : links) {
                    final String fid = new Regex(dllink.getDownloadURL(), "([A-Z0-9]+)$").getMatch(0);
                    final String[][] fileInfo = br.getRegex("/file/" + fid + "</a></b></p>[a-z0-9]+<p>([^<>\"]*?)</p>ttrntt<p>([^<>\"]*?)</p>").getMatches();
                    if (fileInfo == null || fileInfo.length == 0) {
                        dllink.setAvailable(false);
                        logger.warning("Linkchecker broken for " + getHost() + " Example link: " + dllink.getDownloadURL());
                    } else if (br.containsHTML("/file/" + fid + "</a></b></p>[a-z0-9]+<p></p>[a-z0-9]+<p>0 KB</p>")) {
                        dllink.setAvailable(false);
                    } else {
                        dllink.setName(fileInfo[0][0]);
                        dllink.setAvailable(true);
                        dllink.setDownloadSize(SizeFormatter.getSize(fileInfo[0][1]));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>Fshare \\– Dịch vụ chia sẻ số 1 Việt Nam \\– Cần là có \\- </title>|b>Liên kết bạn chọn không tồn tại trên hệ thống Fshare</|<li>Liên kết không chính xác, hãy kiểm tra lại|<li>Liên kết bị xóa bởi người sở hữu\\.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<p><b>Tên file:</b> (.*?)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("<p><b>Tên tập tin:</b> (.*?)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\- Fshare \\- Dịch vụ chia sẻ, lưu trữ dữ liệu miễn phí tốt nhất </title>").getMatch(0);
        String filesize = br.getRegex("<p><b>Dung lượng: </b>(.*?)</p>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Server sometimes sends bad filenames
        link.setFinalFileName(Encoding.htmlDecode(filename));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
        br.setFollowRedirects(true);
        final String fid = br.getRegex("name=\"file_id\" value=\"(\\d+)\"").getMatch(0);
        if (fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(downloadLink.getDownloadURL(), "special=&action=download_file&file_id=" + fid);
        if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML("frm_download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String downloadURL = br.getRegex("window\\.location=\\'(.*?)\\'").getMatch(0);
        if (downloadURL == null) {
            downloadURL = br.getRegex("value=\"Download\" name=\"btn_download\" value=\"Download\"  onclick=\"window\\.location=\\'(http://.*?)\\'\"").getMatch(0);
            if (downloadURL == null) {
                // downloadURL = br.getRegex("\\'(http://download\\d+\\.fshare\\.vn/download/[A-Za-z0-9]+/.*?)\\'").getMatch(0);
                downloadURL = br.getRegex("<form action=\"(http://download\\d+\\.fshare\\.vn/download/[A-Za-z0-9]+/.*?)\"").getMatch(0);
            }
        }
        logger.info("downloadURL = " + downloadURL);
        // Waittime
        String wait = br.getRegex("var count = \"(\\d+)\";").getMatch(0);
        if (wait == null) wait = br.getRegex("var count = (\\d+);").getMatch(0);
        if (wait == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // No downloadlink shown, host is buggy
        if (downloadURL == null && "0".equals(wait)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        if (wait == null || downloadURL == null || !downloadURL.contains("fshare.vn")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(Long.parseLong(wait) * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadURL, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(SERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepBrowser() {
        // Sometime the page is extremely slow!
        br.setReadTimeout(120 * 1000);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.setCustomCharset("UTF-8");
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
            if (dllink == null) dllink = br.getRegex("\"(http://sdownload\\.fshare\\.vn/vip/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (dllink.contains("logout")) throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL premium error");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML(SERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
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
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("https://fshare.vn/", key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getPage("https://www.fshare.vn/login.php");
                br.postPage("https://www.fshare.vn/login.php", "login_useremail=" + Encoding.urlEncode(account.getUser()) + "&login_password=" + Encoding.urlEncode(account.getPass()) + "&url_refe=http%3A%2F%2Fwww.fshare.vn%2Flogin.php&auto_login=1");
                if (br.getCookie("https://www.fshare.vn", "fshare_userpass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("https://fshare.vn/");
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
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www.fshare.vn/index.php");
        String validUntil = br.getRegex(">Hạn dùng:<strong class=\"color_red\">\\&nbsp;(\\d+\\-\\d+\\-\\d+)</strong>").getMatch(0);
        if (validUntil != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd-MM-yyyy", Locale.ENGLISH));
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
                maxPrem.set(1);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
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

}