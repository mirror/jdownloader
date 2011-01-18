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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hyperfileshare.com" }, urls = { "http://[\\w\\.]*?hyperfileshare\\.com/(d/|download\\.php\\?code=)[a-fA-F0-9]+" }, flags = { 2 })
public class HyperFileShareCom extends PluginForHost {

    public HyperFileShareCom(PluginWrapper wrapper) {
        super(wrapper);
        // Actually we only got support for free accounts, not for premium!
        this.enablePremium("http://www.hyperfileshare.com/register.php");
    }

    @Override
    public String getAGBLink() {
        return "http://download.hyperfileshare.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        if (br.containsHTML("302 Found")) {
            String properlink = br.getRegex("The document has moved <a href=\"(.*?)\"").getMatch(0);
            br.getPage(properlink);
        }
        if (br.containsHTML("Download URL is incorrect") || br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Download (.*?)</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<span>Download(.*?)</span></div>").getMatch(0);
        String size = br.getRegex("File size:.*?strong>(.*?)</strong>").getMatch(0);
        if (filename == null || size == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(size + "B"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String url = br.getRegex("href=\"(download\\.php\\?code=[a-f0-9]+&sid=[a-f0-9]+&s=\\d)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        url = "http://download.hyperfileshare.com/" + url;
        br.getPage(url);
        url = null;
        url = br.getRegex("href=\"(download\\.php\\?code=[a-f0-9]+&sid=[a-f0-9]+&s=\\d)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        url = "http://download.hyperfileshare.com/" + url;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("You exceeded your download size limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getPage("http://www.hyperfileshare.com/index.php");
        br.postPage("http://www.hyperfileshare.com/index.php", "login=" + Encoding.urlEncode(account.getUser()) + "&psw=" + Encoding.urlEncode(account.getPass()) + "&rem_me=1");
        if (br.getCookie("http://www.hyperfileshare.com", "sid") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        account.setValid(true);
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String url = br.getRegex("href=\"(download\\.php\\?code=[a-f0-9]+&sid=[a-f0-9]+&s=\\d)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        url = "http://download.hyperfileshare.com/" + url;
        br.getPage(url);
        url = null;
        url = br.getRegex("href=\"(download\\.php\\?code=[a-f0-9]+&sid=[a-f0-9]+&s=\\d)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        url = "http://download.hyperfileshare.com/" + url;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("You exceeded your download size limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
