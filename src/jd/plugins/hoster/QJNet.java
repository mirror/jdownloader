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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dl.qj.net" }, urls = { "http://[\\w\\.]*?dl\\.qj\\.net/.+/\\w+/\\d+/fid/\\d+/catid/\\d+" }, flags = { 0 })
public class QJNet extends PluginForHost {

    public QJNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.qj.net/terms";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (!(br.containsHTML("<title>QuickJump Downloads- - </title>") || br.containsHTML("This file has been temporarily removed"))) {
            String filename = br.getRegex("File Name</b>.*?<b>(.*?)</b>").getMatch(0);
            String filesize = br.getRegex("File Size</td>.*?<td class=\"odd\">(.*?)</td>").getMatch(0);
            if (!(filename == null || filesize == null)) {
                downloadLink.setName(filename);
                downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String id = new Regex(downloadLink.getBrowserUrl(), "/(\\d+)/catid/").getMatch(0);
        String linkurl = "http://dl.qj.net/dl.php?fid=" + id;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
        String requestUrl = dl.getConnection().getURL().toString();
        if (!requestUrl.matches("http://\\w+.amazonaws.com/dlqjnet/.+\\?.+")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
