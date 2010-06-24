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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "camwins.com" }, urls = { "http://[\\w\\.]*?camwins\\.com/video/\\d+" }, flags = { 0 })
public class CamWinsCom extends PluginForHost {

    public CamWinsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.camwins.com/static/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://www.camwins.com", "splash", "1");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("error/video_missing")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) filename = br.getRegex("<meta name=\"description\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String vidKey = new Regex(downloadLink.getDownloadURL(), "camwins\\.com/video/(\\d+)").getMatch(0);
        br.getPage("http://www.camwins.com/media/player/config.php?vkey=" + vidKey);
        dllink = br.getRegex("<src>(.*?)</src>").getMatch(0);
        if (dllink == null) dllink = br.getRegex("(http://media\\d+\\.camwins\\.com/media/videos/flv/\\d+\\.flv)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.trim();
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = br2.openGetConnection(dllink);
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
