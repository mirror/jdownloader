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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hardsextube.com" }, urls = { "http://[\\w\\.]*?hardsextube\\.com/video/[0-9]+/" }, flags = { 0 })
public class HardSexTubeCom extends PluginForHost {

    public HardSexTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.hardsextube.com/register/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<title>(.*?)- HardSexTube").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div style='margin-top:-10px; height:15px'> \\&raquo; <b>(.*?)</b>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<div id='tabdetails' style=\" \">.*?<h1>(.*?)</h1>").getMatch(0);
            }
        }
        String videoID = new Regex(downloadLink.getDownloadURL(), "hardsextube\\.com/video/(\\d+)/").getMatch(0);
        Browser br2 = br.cloneBrowser();
        br2.getPage("http://vidii.hardsextube.com/video/" + videoID + "/configuj.xml");
        dllink = br2.getRegex("FLVPath\" Value=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br2.getRegex("\"(http://vs[0-9]+\\.hardsextube\\.com/content/.*?\\.flv)\"").getMatch(0);
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        URLConnectionAdapter con = br.openGetConnection(dllink);
        if (!con.getContentType().contains("html"))
            downloadLink.setDownloadSize(con.getLongContentLength());
        else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
