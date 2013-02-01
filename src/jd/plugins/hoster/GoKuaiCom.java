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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gokuai.com" }, urls = { "https?://(www\\.)?gokuai\\.com/f/[A-Za-z0-9]+|gokuais?://(www\\.)?gokuai\\.com/a/[a-zA-Z0-9]{16}/[a-z0-9]{40}" }, flags = { 0 })
public class GoKuaiCom extends PluginForHost {

    public GoKuaiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.gokuai.com/agreement";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("gokuai://", "http://"));
        link.setUrlDownload(link.getDownloadURL().replace("gokuais://", "https://"));

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("class=\"error_wrp error_404\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2><i class=\"icon\\_[a-z0-9]+\"></i><span>(.*?)</span></h2>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("filename:\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\- 够快\\-网盘\\|云存储\\|网络硬盘\\|网络存储\\|我的网盘\\|免费网盘\\|数据备份</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex("<span class=\"filesize\">文件大小：<strong>(.*?)</strong></span>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<span class=\"filesize\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Server sends bad filenames */
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">发布人关闭了直接下载功能, 请先保存到网盘再下载<") || !br.containsHTML("download_now")) throw new PluginException(LinkStatus.ERROR_FATAL, "Download not possible!");
        String dllink = br.getRegex("class=\"download_now\" href=\"(https?://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(https?://\\d+\\.\\d+\\.\\d+\\.\\d+/d\\d+/[a-z0-9]+/[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -3);
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