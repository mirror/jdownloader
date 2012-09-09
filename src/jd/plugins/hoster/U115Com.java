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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "u.115.com" }, urls = { "http://(www\\.)?(u\\.)?115\\.com/file/[a-z0-9]+" }, flags = { 2 })
public class U115Com extends PluginForHost {

    private final String        ua                    = RandomUserAgent.generate();
    private static final String MAINPAGE              = "http://115.com/";
    private static final String UNDERMAINTENANCEURL   = "http://u.115.com/weihu.html";
    private static final String UNDERMAINTENANCETEXT  = "The servers are under maintenance";
    private static final String NOFREESLOTS           = "网络繁忙时段，非登陆用户其它下载地址暂时关闭。推荐您使用优蛋下载";
    private static final String ACCOUNTNEEDED         = ">115网盘已关闭大众分享功能，可以到发布资源者的<";
    private static final String ACCOUNTNEEDEDUSERTEXT = "Account is needed to download this link";
    private static final String EXACTLINKREGEX        = "\"(http://[a-z0-9]+\\.115\\.com/[a-z0-9_\\-]+\\d+/[^<>\"\\'/]*?/[^<>\"\\'/]*?/[^<>\"\\']*?)\"";
    private static final Object LOCK                  = new Object();

    public U115Com(PluginWrapper wrapper) {
        super(wrapper);
        /**
         * 10 seconds waittime between the downloadstart of simultan DLs of this
         * host
         */
        this.setStartIntervall(10000l);
        this.enablePremium();
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("u.115.com/file/", "115.com/file/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals(UNDERMAINTENANCEURL)) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.U115Com.undermaintenance", UNDERMAINTENANCETEXT));
                return AvailableStatus.UNCHECKABLE;
            }
            br.getPage(br.getRedirectLocation());
        }
        if (br.containsHTML("(id=\"pickcode_error\">很抱歉，文件不存在。</div>|很抱歉，文件不存在。|>很抱歉，该文件提取码不存在。<|<title>115网盘\\|网盘\\|115,我的网盘\\|免费网络硬盘 \\- 爱分享，云生活</title>|/resource\\?r=404|>视听类文件暂时不支持分享，给您带来的不便深表歉意。<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)网盘下载\\|115网盘|网盘|115网络U盘-我的网盘|免费网络硬盘</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"Download\"></a><a id=\"Download(.*?)\"></a>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("file_name: \\'(.*?)\\',").getMatch(0);
            }
        }
        String filesize = br.getRegex("文件大小：(.*?)<div class=\"share-url\"").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("u6587\\\\u4ef6\\\\u5927\\\\u5c0f\\\\uff1a(.*?)\\\\r\\\\n\\\\").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("file_size: \\'(.*?)\\'").getMatch(0);
                if (filesize == null) filesize = br.getRegex("<li>文件大小：(.*?)</li>").getMatch(0);
            }
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = filesize.replace(",", "");
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        parseSHA1(link, br);
        if (br.containsHTML(ACCOUNTNEEDED)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.u115com.only4registered", ACCOUNTNEEDEDUSERTEXT));
        return AvailableStatus.TRUE;
    }

    public String findLink(DownloadLink link) throws Exception {
        String linkToDownload = br.getRegex(EXACTLINKREGEX).getMatch(0);
        if (linkToDownload == null) {
            linkToDownload = br.getRegex("<div class=\"btn\\-wrap\">[\t\n\r ]+<a href=\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/[^\"\\'<>]+)\"").getMatch(0);
            if (linkToDownload == null) {
                /** First way: For freeusers */
                String pickLink = br.getRegex("\"(/\\?ct=pickcode\\&.*?)\"").getMatch(0);
                if (pickLink != null) {
                    String waittime = br.getRegex("id=\"js_get_download_second\">(\\d+)</b>").getMatch(0);
                    if (waittime == null) waittime = br.getRegex("var second = (\\d+);").getMatch(0);
                    if (waittime != null) sleep(Integer.parseInt(waittime) * 1001l, link);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getPage("http://115.com" + pickLink);
                    linkToDownload = findWorkingLink(br.toString().replace("\\", ""));
                }
                /** Second way: For logged in freeusers */
                pickLink = br.getRegex("GetMyDownloadAddress\\(\\'(h=[^<>\"\\']+)\\'").getMatch(0);
                if (pickLink != null) {
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getPage("http://115.com/?ct=download&ac=get&" + pickLink);
                    linkToDownload = findWorkingLink(br.toString().replace("\\", ""));
                }
            }
        }
        return linkToDownload;
    }

    /**
     * Here we got mirrors, sometimes a mirror does not work so we'll check
     * until we find a working one here
     * 
     * @throws PluginException
     */
    private String findWorkingLink(String correctedBR) throws IOException, PluginException {
        if (correctedBR.contains("\"msg_code\":50027")) throw new PluginException(LinkStatus.ERROR_FATAL, ACCOUNTNEEDEDUSERTEXT);
        String linksToDownload[] = new Regex(correctedBR, "\"(url|data)\":\"(http:.*?)\"").getColumn(1);
        if (linksToDownload == null || linksToDownload.length == 0) linksToDownload = new Regex(correctedBR, EXACTLINKREGEX).getColumn(0);
        if (linksToDownload == null || linksToDownload.length == 0) return null;
        String finallink = null;
        Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        for (String mirror : linksToDownload) {
            try {
                con = br2.openGetConnection(mirror);
                if (!con.getContentType().contains("html")) {
                    finallink = mirror;
                    con.disconnect();
                    break;
                } else {
                    con.disconnect();
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return finallink;
    }

    @Override
    public String getAGBLink() {
        return "http://u.115.com/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void parseSHA1(DownloadLink link, Browser br) {
        String sh1 = br.getRegex("<li>SHA1：(.*?) <a href=\"").getMatch(0);
        if (sh1 == null) sh1 = br.getRegex("sha1: \"(.*?)\",").getMatch(0);
        if (sh1 != null && sh1.matches(("^[a-fA-F0-9]+$"))) {
            link.setSha1Hash(sh1.trim());
        } else {
            link.setSha1Hash(null);
        }
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) { return; }
            br.setReadTimeout(2 * 60 * 1000);
            br.setCookie("http://u.115.com/", "lang", "en");
            br.getHeaders().put("User-Agent", ua);
            br.setCustomCharset("utf-8");
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
        } catch (Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        doFree(link);
    }

    public void doFree(DownloadLink link) throws Exception {
        if (UNDERMAINTENANCEURL.equals(br.getRedirectLocation())) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.U115Com.undermaintenance", UNDERMAINTENANCETEXT));
        if (br.containsHTML(NOFREESLOTS)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available at the moment");
        if (br.containsHTML(ACCOUNTNEEDED)) throw new PluginException(LinkStatus.ERROR_FATAL, ACCOUNTNEEDEDUSERTEXT);
        final String dllink = findLink(link);
        if (dllink == null) {
            logger.warning("dllink is null, seems like the regexes are defect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        parseSHA1(link, br);
        /** Don't do html decode, it can make the dllink invalid */
        // dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(help_down.html|onclick=\"WaittingManager|action=submit_feedback|\"report_box\"|UploadErrorMsg)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
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
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(true);
            br.getPage("https://passport.115.com");
            Form login = br.getForm(0);
            if (login == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            login.put("login%5Baccount%5D", Encoding.urlEncode(account.getUser()));
            login.put("login%5Bpasswd%5D", Encoding.urlEncode(account.getPass()));
            br.submitForm(login);
            if (br.getCookie(MAINPAGE, "OOFL") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setValid(true);
        /**
         * Didn't ever have a premium login, plugin is only designed for free
         * accounts!
         */
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        doFree(link);
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