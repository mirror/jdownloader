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

import jd.PluginWrapper;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up.4share.vn" }, urls = { "http://(www\\.)?up\\.4share\\.vn/f/[a-z0-9]+/.+" }, flags = { 2 })
public class Up4ShareVn extends PluginForHost {

    private static final String MAINPAGE = "http://up.4share.vn/";

    public Up4ShareVn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://up.4share.vn/?act=gold");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (con.getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.containsHTML(">FID Không hợp lệ\\!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("Downloading: <strong>([^<>\"]*?)</strong>").getMatch(0);
        final String filesize = br.getRegex("Kích thước: <strong>([^<>\"]*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex("<br/>\\&#272;\\&#227; s\\&#7917; d\\&#7909;ng: <b>(.*?)  \\(").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("Ngày hết hạn.*?(\\d{2}-\\d{2}-\\d{4})").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://up.4share.vn/?act=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML("captcha1\\.html")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(false);
        String dllink = null;
        // Can be skipped
        int wait = 60;
        final String waittime = br.getRegex("var counter=(\\d+);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        for (int i = 0; i <= 3; i++) {
            final String code = getCaptchaCode("http://up.4share.vn/library/captcha1.html", downloadLink);
            br.postPage(downloadLink.getDownloadURL(), "&submit=DOWNLOAD&s=&security_code=" + code);
            dllink = br.getRedirectLocation();
            if (dllink == null && br.containsHTML("captcha1\\.html")) continue;
            break;
        }
        if (dllink == null && br.containsHTML("captcha1\\.html")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<div id=\"counter\">[\t\n\r ]+</div>[\t\n\r ]+</h3>[\t\n\r ]+<br><a href=\\'(http://.*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://s\\d+\\.4share\\.vn/\\d+/\\?i=[a-z0-9]+\\&f=.*?&l=[a-z0-9]+)\\'").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPage(MAINPAGE, "inputUserName=" + Encoding.urlEncode(account.getUser()) + "&inputPassword=" + Encoding.urlEncode(account.getPass()) + "&rememberlogin=on");
        if (br.getCookie(MAINPAGE, "userid") == null || br.getCookie(MAINPAGE, "passwd") == null || !br.containsHTML("<td> <b>VIP</b>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}