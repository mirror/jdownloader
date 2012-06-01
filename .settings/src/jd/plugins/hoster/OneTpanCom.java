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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 15557 $", interfaceVersion = 2, names = { "1tpan.com" }, urls = { "http://(www\\.)?d\\.1tpan\\.com/tp\\d+" }, flags = { 0 })
public class OneTpanCom extends PluginForHost {

    public OneTpanCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        /** Not AGBlink found */
        return "http://1tpan.com/";
    }

    private static final String SIZEREGEX = "\"size\":(\\d+)";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/link/notfound.shtml") || br.containsHTML("(哦~见鬼了，文件不存在|1\\.文件主人取消了分享|>您见到本页可能是如下原因：</span><br)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"left dlink\" network=\"default\" filename=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\\{\"name\":\"(.*?)\"").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>(.*?)_金山T盘</title>").getMatch(0);
        }
        String filesize = br.getRegex(SIZEREGEX).getMatch(0);
        if (filesize == null) filesize = br.getRegex("<span id=\"size\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Filename is displayed like from an API, just take that */
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        final String sha1 = br.getRegex("\"sha1\":\"([a-z0-9]+)\"").getMatch(0);
        if (sha1 != null) link.setSha1Hash(sha1);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String filesize = br.getRegex(SIZEREGEX).getMatch(0);
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String finallink = "http://d.1tpan.com/tfs3/servlet/link?cmd=readlink&extraCode=" + new Regex(downloadLink.getDownloadURL(), "1tpan\\.com/(tp.+)").getMatch(0) + "&network=t&filename=" + downloadLink.getFinalFileName() + "&size=" + filesize;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, "", true, -2);
        if (dl.getConnection().getLongContentLength() == 0) throw new PluginException(LinkStatus.ERROR_FATAL, "Download not possible");
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