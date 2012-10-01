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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

//All links come from a decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pan.baidu.com" }, urls = { "http://(www\\.)?pan\\.baidudecrypted\\.com/\\d+" }, flags = { 0 })
public class PanBaiduCom extends PluginForHost {

    public PanBaiduCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://pan.baidu.com/";
    }

    private String         DLLINK       = null;
    private static boolean pluginloaded = false;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String dirName = link.getStringProperty("dirname");
        final String mainlink = link.getStringProperty("mainlink");
        String plainfilename = link.getStringProperty("plainfilename");
        br.getPage(mainlink);
        if (br.containsHTML(">很抱歉，您要访问的页面不存在。<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String size = br.getRegex("\\\\\",\\\\\"size\\\\\":(\\\\\")?(\\d+).*?\\\\\"md5\\\\\":\\\\\"" + link.getMD5Hash()).getMatch(1);
        if (size == null) {
            final String shareid = new Regex(mainlink, "shareid=(\\d+)").getMatch(0);
            final String uk = new Regex(mainlink, "uk=(\\d+)").getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String listPage = "http://pan.baidu.com/share/list?channel=chunlei&clienttype=0&web=1&num=100&t=" + System.currentTimeMillis() + "&page=1&dir=%2F" + dirName + "&t=0." + +System.currentTimeMillis() + "&uk=" + uk + "&shareid=" + shareid + "&_=" + System.currentTimeMillis();
            br.getPage(listPage);
            size = br.getRegex("\\\\\",\\\\\"size\\\\\":(\\\\\")?(\\d+).*?\\\\\"md5\\\\\":\\\\\"" + link.getMD5Hash()).getMatch(1);
            if (size == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(unescape(plainfilename)));
        link.setDownloadSize(SizeFormatter.getSize(size + "b"));
        DLLINK = br.getRegex("\\\\\"md5\\\\\":\\\\\"" + link.getMD5Hash() + "\\\\\".*?,\\\\\"dlink\\\\\":\\\\\"(http:[^<>\"]*?)\\\\\"").getMatch(0);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = DLLINK.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
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