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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ctdisk.com" }, urls = { "http://(www\\.)?ctdisk\\.com/file/\\d+" }, flags = { 0 })
public class CtDiskCom extends PluginForHost {

    public CtDiskCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.ctdisk.com/help.php?item=service";
    }

    private static final String DLLINKREGEX2 = ">电信限速下载</a>[\t\n\r ]+<a href=\"(http://.*?)\"";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>提示：本文件已到期。成为VIP后才能下载所有文件|<title>成为VIP即可下载全部视频和文件</title>|>注意：高级VIP可下载所有文件|color=\"#FF0000\" face=\"黑体\">点击立即成为VIP</font>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"file_title\">(.*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\- 免费高速下载 \\- 城通网盘\\-CTDISK\\-城通国信\\-赚钱网盘\\-外链网盘\\-免费400GB网络硬盘</title>").getMatch(0);
        String filesize = br.getRegex("(<li>大小：\\-?|文件大小: <b>\\-)([0-9,\\.]+ (M|B|K))").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll("(,|\\-)", "").replace("M", "MB").replace("K", "KB")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String noCaptcha = br.getRegex("value=\"点击进入下载列表\"  style=\"width: 120px; height: 33px; font\\-weight: bold;\" onclick=\"window\\.location\\.href=\\'(/.*?)\\';\"").getMatch(0);
        if (noCaptcha == null) noCaptcha = br.getRegex("\\'(/downhtml/\\d+/\\d+/\\d+/[a-z0-9]+\\.html)\\'").getMatch(0);
        if (noCaptcha != null) {
            br.getPage("http://www.ctdisk.com" + noCaptcha);
        } else {
            if (!br.containsHTML("/randcode_guest\\.php\\?") || !br.containsHTML("/guest_login\\.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            long timeBefore = System.currentTimeMillis();
            String code = getCaptchaCode("http://www.ctdisk.com/randcode_guest.php?" + System.currentTimeMillis(), downloadLink);
            String waittime = br.getRegex("var maxtime_\\d+ = (\\d+);").getMatch(0);
            int wait = 6;
            if (waittime != null) wait = Integer.parseInt(waittime);
            int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
            wait -= passedTime;
            sleep(wait * 1001, downloadLink);
            br.postPage("http://www.ctdisk.com/guest_login.php", "file_id=" + new Regex(downloadLink.getDownloadURL(), "ctdisk\\.com/file/(\\d+)").getMatch(0) + "&randcode=" + code);
            if (br.getURL().contains("/guest_login.php")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRegex("\"(http://[a-z0-9]+\\.ctdisk\\.com/cache/.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex(DLLINKREGEX2).getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Check if link is working. If not try mirror. Limits can be
        // extended/skipped using this method :D
        boolean failed = false;
        URLConnectionAdapter con = null;
        Browser br2 = br.cloneBrowser();
        try {
            con = br2.openGetConnection(dllink);
        } catch (Exception e) {
            failed = true;
        }
        if (failed || con.getContentType().contains("html") || con.getResponseCode() == 503) {
            dllink = br.getRegex(DLLINKREGEX2).getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
        }
        if (con != null) con.disconnect();
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        } catch (Exception e) {

            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // Works best this way. Maximum that worked for me was 6
        return 5;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}