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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashare.vn" }, urls = { "http://[\\w\\.]*?(megashare\\.vn/(download\\.php\\?uid=[0-9]+\\&id=[0-9]+|dl\\.php/\\d+)|share\\.megaplus\\.vn/dl\\.php/\\d+)" }, flags = { 0 })
public class MegaShareVn extends PluginForHost {

    public MegaShareVn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL();
        theLink = theLink.replace("download.php?id=", "dl.php/");
        String theID = new Regex(theLink, "megashare\\.vn/dl\\.php/(\\d+)").getMatch(0);
        if (theID != null) theLink = "http://share.megaplus.vn/dl.php/" + theID;
        link.setUrlDownload(theLink);
    }

    @Override
    public String getAGBLink() {
        return "http://megashare.vn/rule.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("(>DOWNLOAD NOT FOUND<|>File không tìm thấy hoặc đã bị xóa<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\">Tên file:</td>[\t\r\n ]+<td class=\"content_tx\">(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("\">Dung lượng:</td>[\r\t\n ]+<td class=\"content_tx\">(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        String waittime = br.getRegex("id=\\'timewait\\' value='\\d+'>(\\d+)</span>").getMatch(0);
        int waitThat = 20;
        if (waittime != null) waitThat = Integer.parseInt(waittime);
        sleep(waitThat * 1001l, link);
        br.getPage("http://share.megaplus.vn/getlink.php");
        String dllink = br.toString();
        if (dllink == null || !dllink.startsWith("http://") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html") && !new Regex(dllink, ".+html?$").matches()) {
            /* buggy server sends html content if filename ends on html */
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
