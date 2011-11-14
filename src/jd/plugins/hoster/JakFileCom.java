//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.Random;

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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jakfile.com" }, urls = { "http://(www\\.)?jakfile\\.com/[a-z0-9]+" }, flags = { 2 })
public class JakFileCom extends PluginForHost {

    public JakFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.jakfile.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.jakfile.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>LINK NOT AVAILABLE<|Invalid link<br>|File deleted by owner<br>|The file has been deleted because it was violating our <a)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">File: </font><font style=\"font\\-size:16px\" color=\"#FFFFFF\" face=\"Arial\"><b>(.*?)</b>").getMatch(0);
        if (filename == null) filename = br.getRegex("<strong>Download Video:</strong>[\t\n\r ]+<a href=\"http://www\\d+\\.jakfile\\.com/get/[a-z0-9]+/[a-z0-9]+/(.*?)\"").getMatch(0);
        String filesize = br.getRegex(">Size: </font><font style=\"font\\-size:14px\" color=\"#FFFFFF\" face=\"Arial\">(.*?)</font>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String dllink = getDllink();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // br.getPage(MAINPAGE);
        br.postPage("http://www.jakfile.com/login.php", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&task=dologin&return=%2F");
        if (!br.containsHTML("You are:\n +<b>Premium Member</b>") && !br.containsHTML("Account Type:\n +<br>\n +<b>Premium Member</b>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.setFollowRedirects(false);
        br.getPage("http://www.jakfile.com/account");
        String space = br.getRegex(">Used Space:</td>[\t\n\r ]+<td>(.*?)</td>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        String uploadedFiles = br.getRegex(">Hosted Files:</td>[\t\n\r ]+<td>(.*?)</td>").getMatch(0);
        if (uploadedFiles != null) ai.setFilesNum(Integer.parseInt(uploadedFiles));
        ai.setUnlimitedTraffic();
        String expire = br.getRegex(">Premium End:</td>[\t\n\r ]+<td>(.*?)</td>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = getDllink();
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

    private String getDllink() throws IOException, PluginException {
        String nextPage = br.getRegex("\\' onclick=\"document\\.location=\\'(http://.*?)\\'").getMatch(0);
        if (nextPage == null) nextPage = br.getRegex("\\'(http://(www\\.)jakfile\\.com/get/[a-z0-9]+/[a-z0-9]+/.*?)\\'").getMatch(0);
        if (nextPage != null) br.getPage(nextPage);
        String dllink = br.getRegex("style=\"text\\-align: left;\">Download</h1>[\t\n\r ]+<form[\t\n\r ]+action=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.jakfile\\.com/get/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!dllink.contains("jakfile.com/")) dllink = "http://www.jakfile.com/" + dllink + "?task=download&x=" + Integer.toString(new Random().nextInt(100)) + "&y=" + Integer.toString(new Random().nextInt(100));
        return dllink;
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}