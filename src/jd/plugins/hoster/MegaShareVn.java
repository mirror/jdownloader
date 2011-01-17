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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashare.vn" }, urls = { "http://[\\w\\.]*?(megashare\\.vn/(download\\.php\\?uid=[0-9]+\\&id=[0-9]+|dl\\.php/\\d+)|share\\.megaplus\\.vn/dl\\.php/\\d+)" }, flags = { 2 })
public class MegaShareVn extends PluginForHost {

    public MegaShareVn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://share.megaplus.vn/vip.php");
    }

    private static final String PREMIUMHOST = "id.megaplus.vn";
    private static final String ACCESSURL   = "https://id.megaplus.vn/login?service=http%3A%2F%2Fshare.megaplus.vn%2Fmegavnnplus.php%3Fservice%3Dlogin&locale=en";

    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL();
        theLink = theLink.replace("download.php?id=", "dl.php/");
        String theID = new Regex(theLink, "megashare\\.vn/dl\\.php/(\\d+)").getMatch(0);
        if (theID != null) theLink = "http://share.megaplus.vn/dl.php/" + theID;
        link.setUrlDownload(theLink);
    }

    @Override
    public String getAGBLink() {
        return "http://megashare.vn/rule.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("(>DOWNLOAD NOT FOUND<|>File không tìm thấy hoặc đã bị xóa<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\">Tên file:</td>[\t\r\n ]+<td class=\"content_tx\">(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("\">Dung lượng:</td>[\r\t\n ]+<td class=\"content_tx\">(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        String waittime = br.getRegex("id=\\'timewait\\' value='\\d+'>(\\d+)</span>").getMatch(0);
        int waitThat = 20;
        if (waittime != null) waitThat = Integer.parseInt(waittime);
        sleep(waitThat * 1001l, link);
        br.getPage("http://share.megaplus.vn/getlink.php");
        String dllink = br.toString();
        if (dllink == null || !dllink.startsWith("http://") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html") && !new Regex(dllink, ".+html?$").matches()) {
            /* buggy server sends html content if filename ends on html */
            br.followConnection();
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        // Complicated login, needs time...
        br.setFollowRedirects(true);
        br.setCookie(PREMIUMHOST, "lang", "en");
        br.getPage(ACCESSURL);
        Form loginform = br.getFormbyProperty("id", "fm1");
        if (loginform == null) loginform = br.getForm(0);
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.setAction(ACCESSURL);
        loginform.put("username", Encoding.urlEncode(account.getUser()));
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginform);
        if (br.getCookie(PREMIUMHOST, "CASTGC") == null || br.getCookie(PREMIUMHOST, "CASPRIVACY") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://share.megaplus.vn/userinfo.php");
        if (!br.containsHTML("\">MegaShare VIP</a>")) {
            account.setValid(false);
            logger.info("This account is no premiumaccount!");
            return ai;
        }
        account.setValid(true);
        String availabletraffic = br.getRegex(">Thời hạn VIP còn lại:</td>[\t\n\r ]+<td colspan=\"2\" class=\"content_tx\">(.*?) ngày</td>").getMatch(0);
        if (availabletraffic != null) {
            ai.setTrafficLeft(Regex.getSize(availabletraffic + "GB"));
        } else {
            account.setValid(false);
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("height=\"40\" align=\"center\" valign=\"bottom\"><a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+/dl\\d+/\\d+/.*?)\"").getMatch(0);
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
