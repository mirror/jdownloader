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

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
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
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.1280.com" }, urls = { "http://[\\w\\.]*?mega\\.1280\\.com/file/[0-9|A-Z]+" }, flags = { 2 })
public class Mega1280Com extends PluginForHost {

    public Mega1280Com(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://psp.1280.com/register.php");
    }

    @Override
    public String getAGBLink() {
        return "http://mega.1280.com/terms.php";
    }

    private static final String SERVERERROR = "Tài nguyên bạn yêu cầu không tìm thấy";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Sometime the page is extremely slow!
        br.setReadTimeout(120 * 1000);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Liên kết bạn chọn không tồn tại trên hệ thống Mega1280<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<title>-- Mega 1280 -- (.*?) - </title>").getMatch(0));
        if (filename == null) filename = Encoding.htmlDecode(br.getRegex("Tên file: <span class=\"color_red\">(.*?)</span>").getMatch(0));
        String filesize = br.getRegex("<b>Dung lượng: </b><span>(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.setDebug(true);
        if (br.containsHTML("Vui lòng chờ cho lượt download kế tiếp")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.parse();
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, downloadLink);
        rc.setCode(c);
        if (br.containsHTML("frm_download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String downloadURL = br.getRegex("\\'(http://\\d+\\.\\d+\\.\\d+\\.\\d+/downloads/file/[a-z0-9]+/.*?)\\'").getMatch(0);
        if (downloadURL == null) downloadURL = br.getRegex("window\\.location=\\'(.*?)\\'").getMatch(0);
        // Waittime
        String wait = br.getRegex("var count = (\\d+);").getMatch(0);
        if (wait == null || downloadURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(Long.parseLong(wait) * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadURL, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(SERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.Mega1280Com.Servererror", "Servererror!"), 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        String mailPart1 = new Regex(account.getUser(), "(.+)@").getMatch(0);
        String mailPart2 = new Regex(account.getUser(), "(@.+)").getMatch(0);
        if (mailPart1 == null || mailPart2 == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        String postData = "user_email=" + Encoding.urlEncode(mailPart1) + " &lstdomain_mail=" + Encoding.urlEncode(mailPart2) + "&user_password=" + Encoding.urlEncode(account.getPass()) + "&auto_login=1&btnLogin=+&user_previous=index.php";
        br.postPage("http://psp.1280.com/login.php", postData);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://psp.1280.com/errors.php?error=active")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://psp.1280.com/", "1280_userpass") == null || br.getCookie("http://psp.1280.com/", "1280_userid") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://mega.1280.com/statistic.php");
        String days = br.getRegex("<b>Thời hạn dùng:</b></td>[\t\n\r ]+<td height=\"25\" align=\"left\" valign=\"top\">(\\d+)\\&nbsp;").getMatch(0);
        if (days != null) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(days) * 24 * 60 * 60 * 1000));
        } else {
            ai.setExpired(true);
            return ai;
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        Form dlForm = br.getFormbyProperty("name", "frm_download");
        if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlForm, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(SERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.Mega1280Com.Servererror", "Servererror!"), 60 * 60 * 1000l);
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
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
