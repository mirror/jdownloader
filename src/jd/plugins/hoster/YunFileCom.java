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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yunfile.com" }, urls = { "http://(www\\.)?yunfile\\.com/file/[a-z0-9]+/[a-z0-9]+" }, flags = { 0 })
public class YunFileCom extends PluginForHost {
    // MountFileCom uses the same script
    public YunFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.yunfile.com/user/terms.html";
    }

    private static final String MAINPAGE = "http://yunfile.com/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        URLConnectionAdapter con = br.openGetConnection(link.getDownloadURL());
        if (con.getResponseCode() == 503) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.followConnection();
        if (br.containsHTML("(文件不存在或系统临时维护|h2 class=\"title\">文件下载\\&nbsp;\\&nbsp;</h2)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2 class=\"title\">文件下载\\&nbsp;\\&nbsp;(.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) - YunFile\\.com 网盘赚钱 - 最好的网赚网盘 赚钱网盘 </title>").getMatch(0);
        String filesize = br.getRegex("文件大小: <b>(.*?)</b><br>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("文件大小: <b>(.*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String waittime = br.getRegex("id=\"down_interval\" style=\"font-size: 28px; color: green;\">(\\d+)</span> 分钟</span>").getMatch(0);
        if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
        String userid = new Regex(downloadLink.getDownloadURL(), "yunfile\\.com/file/(.*?)/").getMatch(0);
        String fileid = new Regex(downloadLink.getDownloadURL(), "yunfile\\.com/file/.*?/(.+)").getMatch(0);
        if (userid == null || fileid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // sleep(35 * 1000l, downloadLink);
        br.getPage("http://yunfile.com/file/down/" + userid + "/" + fileid + ".html");
        String vid = br.getRegex("name=\"vid\" value=\"(.*?)\"").getMatch(0);
        String vid1 = br.getRegex("setCookie\\(\"vid1\", \"(.*?)\"").getMatch(0);
        String vid2 = br.getRegex("setCookie\\(\"vid2\", \"(.*?)\"").getMatch(0);
        String action = br.getRegex("id=\"down_from\" action=\"(http://.*?)\" method=\"post\"").getMatch(0);
        if (action == null) action = br.getRegex("\"(http://dl\\d+\\.yunfile\\.com/view)\"").getMatch(0);
        if (vid == null || vid1 == null || vid2 == null || action == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Those cookies are important, no downloadstart without them!
        br.setCookie(MAINPAGE, "vid1", vid1);
        br.setCookie(MAINPAGE, "vid2", vid2);
        String postData = "module=fileService&action=downfile&userId=" + userid + "&fileId=" + fileid + "&vid=" + vid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, action, postData, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("yunfile.com/file/" + userid + "/" + fileid)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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