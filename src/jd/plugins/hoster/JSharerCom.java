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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jsharer.com" }, urls = { "http://(www\\.)?jsharer\\.com/download/[a-z0-9]+\\.htm" }, flags = { 0 })
public class JSharerCom extends PluginForHost {

    public JSharerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.jsharer.com/help.htm#1_000";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("jsharer.com/pages/error.jsp") || br.containsHTML("(<title>jsharer \\- RE: 熔火核心计划</title>|>这是地图上不存在的页面<|中的地址是有误。如果您认为这个问题是由于熔火核心的暴走)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div id=dl\\-text>[\t\n\r ]+<p>([^\"\\'<>]+)<span").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>jsharer \\- RE: (.*?)</title>").getMatch(0);
        String filesize = br.getRegex("<span id=dl\\-stats><var>(.*?)</var>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        String sh1 = br.getRegex("id=\"dl\\-hashcode\">SHA-1: ([a-z0-9]+)</span>").getMatch(0);
        if (sh1 != null) link.setSha1Hash(sh1);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("由于目前下载人数过多，您暂时无法下载缺乏信仰的文件。")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
        String cph = br.getRegex("var cph = \\'([^\"\\']+)\\'").getMatch(0);
        String xph = br.getRegex("var xph = \\'([^\"\\']+)\\'").getMatch(0);
        String nph = br.getRegex("var nph = \\'([^\"\\']+)\\'").getMatch(0);
        String cpp = br.getRegex("var cpp = \\'([^\"\\']+)\\'").getMatch(0);
        String npu = br.getRegex("var npu = \\'([^\"\\']+)\\'").getMatch(0);
        String lastCrap = br.getRegex("\\+npu\\+\"/\"\\+\"([^\"\\']+)\"").getMatch(0);
        if (cph == null || nph == null || cpp == null || npu == null || lastCrap == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://www.jsharer.com/download/mc/" + cph + "/" + xph + "/" + nph + "/" + cpp + "/" + npu + "/" + lastCrap;
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 30;
        final String waittime = br.getRegex("var cpc = parseInt\\(\\'(\\d+)\\'\\)").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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