//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upshare.me" }, urls = { "http://[\\w\\.]*?upshare\\.me/files/[A-Za-z0-9]+/.*?\\.html" }, flags = { 0 })
public class UpShareMe extends PluginForHost {

    public UpShareMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.upshare.me/help/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">The file you have requested does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("<h1 style=\"text-align:center;\">(.*?) \\(([0-9\\.]+ [A-Za-z]+)\\)</h1>");
        String filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set final filename here because hoster taggs files
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String getLink = br.getRegex("disabled=\"disabled\" onclick=\"document\\.location=\\'\\.-(/get/.*?)\\';\"").getMatch(0);
        if (getLink == null) getLink = br.getRegex("\\'\\.(/get/[A-Za-z0-9]+/\\d+/.*?)\\'").getMatch(0);
        if (getLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        getLink = "http://www.upshare.me" + getLink;
        // waittime
        String ttt = br.getRegex("var time = (\\d+);").getMatch(0);
        int tt = 10;
        if (ttt != null) tt = Integer.parseInt(ttt);
        if (tt > 181) {
            // 10 Minutes reconnect-waittime is not enough, let's wait one
            // hour
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        }
        sleep(tt * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getLink, "task=download", false, 1);
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