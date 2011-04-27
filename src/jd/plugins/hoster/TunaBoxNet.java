//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tunabox.net" }, urls = { "http://(www\\.)?tunabox\\.net/files/[A-Za-z0-9]+\\.html" }, flags = { 2 })
public class TunaBoxNet extends PluginForHost {

    public TunaBoxNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/get-premium.php");
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/help/terms.php";
    }

    private static final String MAINPAGE      = "http://www.tunabox.net";
    private static final String GETLINKREGEX  = "disabled=\"disabled\" onclick=\"document\\.location=\\'(.*?)\\';\"";
    private static final String GETLINKREGEX2 = "\\'(" + "http://(www\\.)" + MAINPAGE.replaceAll("(http://|www\\.)", "") + "/get/[A-Za-z0-9]+/\\d+/.*?)\\'";
    private static final String PREMIUMLIMIT  = "out of 1000\\.00 GB</td>";
    private static final String FILENOTFOUND  = ">The file you have requested does not exist";
    private static final String APIKEY        = "QExPEQ47iXcpXx94RBqiAJlI0d7Q";

    // Using FreakshareScript 1.1 API Version
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Use API with JDownloader API Key
        br.getPage("http://tunabox.net/api/info.php?api_key=" + APIKEY + "&file_id=" + new Regex(link.getDownloadURL(), "/files/([A-Za-z0-9]+)\\.html").getMatch(0));
        if (br.containsHTML("file does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\\[file_name\\] => (.*?)(\n|\r)").getMatch(0);
        String filesize = br.getRegex("\\[file_size\\] => (\\d+)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set final filename here because hoster taggs files
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        // Check again, in case API tells us wrong information
        if (br.containsHTML(FILENOTFOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String getLink = br.getRegex(GETLINKREGEX).getMatch(0);
        if (getLink == null) getLink = br.getRegex(GETLINKREGEX2).getMatch(0);
        if (getLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // waittime
        String ttt = br.getRegex("var time = (\\d+);").getMatch(0);
        int tt = 20;
        if (ttt != null) tt = Integer.parseInt(ttt);
        if (tt > 240) {
            // 10 Minutes reconnect-waittime is not enough, let's wait one
            // hour
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        sleep(tt * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getLink, "task=download", false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("files per hour for free users\\.</div>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPage(MAINPAGE + "/login.php", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&submit=Login&task=dologin&return=.%2Fmembers%2Fmyfiles.php");
        if (!br.containsHTML(PREMIUMLIMIT)) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String hostedFiles = br.getRegex("<td>Files Hosted:</td>[\t\r\n ]+<td>(\\d+)</td>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Integer.parseInt(hostedFiles));
        String space = br.getRegex("<td>Spaced Used:</td>[\t\n\r ]+<td>(.*?) " + PREMIUMLIMIT).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        account.setValid(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        // Check again, in case API tells us wrong information
        if (br.containsHTML(FILENOTFOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String getLink = br.getRegex(GETLINKREGEX).getMatch(0);
        if (getLink == null) getLink = br.getRegex(GETLINKREGEX2).getMatch(0);
        if (getLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(getLink);
        // No multiple connections possible for premiumusers
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, getLink, "task=download", true, 1);
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}