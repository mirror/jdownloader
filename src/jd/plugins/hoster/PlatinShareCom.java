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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "platinshare.com" }, urls = { "http://(www\\.)?platinshare\\.com/files/[A-Za-z0-9]+" }, flags = { 2 })
public class PlatinShareCom extends PluginForHost {

    public PlatinShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.platinshare.com/premium/get");
    }

    @Override
    public String getAGBLink() {
        return "http://www.platinshare.com/help/terms";
    }

    private static final String FILEIDREGEX = "platinshare\\.com/files/(.+)";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>File not found|The file you were looking for could not be found, here are some possible reasons:|>The file was deleted by the uploader|>The file was deleted by PlatinShare because it did not comply with|>The file URL is incorrect|>The file expired because it was not downloaded)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("(<h1>|<title>PlatinShare\\.com \\- )([^\"\\']+) \\(([^\"\\']+)\\)<");
        String filename = fileInfo.getMatch(1);
        String filesize = fileInfo.getMatch(2);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        final String fileID = new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0);
        br.getPage("http://www.platinshare.com/get/" + fileID + ".html");
        String importantVar = br.getRegex("countDown\\(waitSecs, \\'(\\d+)\\'\\)").getMatch(0);
        if (importantVar == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://www.platinshare.com/get/" + fileID + "/" + importantVar + ".html", "task=download&time=" + importantVar, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">You can only download a max of 1 files per hour as a free user")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.postPage("http://www.platinshare.com/account/login", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&task=dologin&return=http%3A%2F%2Fwww.platinshare.com%2Fmembers%2Fmyfile");
        br.setFollowRedirects(true);
        br.getPage("http://platinshare.com/members/myfiles");
        if (!br.containsHTML("(\">Premium Expires:|>Account type: <strong>Premium Member</strong>)")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String space = br.getRegex(">Used Space: (.*?)</a>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        String hostedFiles = br.getRegex(">Total Files: (\\d+)</a>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Integer.parseInt(hostedFiles));
        ai.setUnlimitedTraffic();
        String expire = br.getRegex(">Premium Expires:(\\d{2}\\-\\d{2}\\-\\d{4} @ \\d{2}:\\d{2}:\\d{2})</a>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd-MM-yyyy @ hh:mm:ss", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
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
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}