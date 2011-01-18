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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "u.115.com" }, urls = { "http://[\\w\\.]*?u\\.115\\.com/file/[a-z0-9]+" }, flags = { 0 })
public class U115Com extends PluginForHost {

    public U115Com(PluginWrapper wrapper) {
        super(wrapper);
        // 10 seconds waittime between the downloadstart of simultan DLs of this
        // host
        this.setStartIntervall(10000l);
    }

    @Override
    public String getAGBLink() {
        return "http://u.115.com/tos.html";
    }

    private static final String UNDERMAINTENANCEURL  = "http://u.115.com/weihu.html";
    private static final String UNDERMAINTENANCETEXT = "The servers are under maintenance";
    private static final String NOFREESLOTS          = "网络繁忙时段，非登陆用户其它下载地址暂时关闭。推荐您使用优蛋下载";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        br.setCustomCharset("utf-8");
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals(UNDERMAINTENANCEURL)) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.U115Com.undermaintenance", UNDERMAINTENANCETEXT));
                return AvailableStatus.UNCHECKABLE;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("id=\"pickcode_error\">很抱歉，文件不存在。</div>") || br.containsHTML("很抱歉，文件不存在。")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)\\|优盘|115优盘|115网络U盘-我的网盘|免费网络硬盘</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"Download\"></a><a id=\"Download(.*?)\"></a>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex(",file_name:\"(.*?)\"").getMatch(0);
            }
        }
        String filesize = br.getRegex("<li><strong>文件大小</strong>：(.*?)</li>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = filesize.replace(",", "");
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        String sh1 = br.getRegex("<li><strong>SHA1</strong>：(.*?)<a").getMatch(0);
        if (sh1 != null) link.setSha1Hash(sh1.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals(UNDERMAINTENANCEURL)) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.U115Com.undermaintenance", UNDERMAINTENANCETEXT));
        if (br.containsHTML(NOFREESLOTS)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available at the moment");
        String dllink = findLink();
        if (dllink == null) {
            logger.warning("dllink is null, seems like the regexes are defect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.urlDecode(dllink, true);
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(help_down.html|onclick=\"WaittingManager|action=submit_feedback|\"report_box\"|UploadErrorMsg)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public String findLink() throws Exception {
        String linkToDownload = br.getRegex("\"(http://\\d+\\.(cnc|tel|bak)\\.115cdn\\.com/pickdown/.*?)\"").getMatch(0);
        if (linkToDownload == null) linkToDownload = br.getRegex("</a>\\&nbsp;[\t\r\n ]+<a href=\"(http://.*?)\"").getMatch(0);
        return linkToDownload;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
