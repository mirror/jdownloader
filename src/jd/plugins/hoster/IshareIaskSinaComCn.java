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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ishare.iask.sina.com.cn" }, urls = { "http://(www\\.)?ishare\\.iask\\.sina\\.com\\.cn/f/\\d+\\.html" }, flags = { 0 })
public class IshareIaskSinaComCn extends PluginForHost {

    public IshareIaskSinaComCn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://iask.com/help/mzsm.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>共享资料</title>|<br>5秒钟后跳转到首页</div>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) - 免费高速下载 - 共享资料</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("name=\"file_title\" id=\"file_des\" value=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("name=\"hiddenfile_title\" id=\"hiddenfile_title\" value=\"(.*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<h1 class=\"f14\" style=\"display:inline;\">(.*?)</h1></div>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<input type=\"hidden\" name=\"title\" value=\"(.*?)\">").getMatch(0);
                    }
                }
            }
        }
        String filesize = br.getRegex("class=\"f10\">0分<br>(.*?)</span></td>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.getPage("http://ishare.iask.sina.com.cn/download.php?fileid=" + new Regex(downloadLink.getDownloadURL(), "ishare\\.iask\\.sina\\.com\\.cn/f/(\\d+)\\.html").getMatch(0));
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}