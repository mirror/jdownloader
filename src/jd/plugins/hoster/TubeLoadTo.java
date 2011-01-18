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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tubeload.to" }, urls = { "http://[\\w\\.]*?tubeload\\.to/file(\\d+)?-.+" }, flags = { 0 })
public class TubeLoadTo extends PluginForHost {

    public TubeLoadTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tubeload.to/contact";
    }

    public String dllink = null;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setReadTimeout(120 * 1000);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(File kann nicht abgespielt werden|Sie werden weitergeleitet in )")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Sie schauen:(.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex(";font-weight:bold;text-indent:20px;color:#fff;line-height:33px\">(.*?)</div>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename);
        dllink = br.getRegex("video/divx\" src=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("src\" value=\"(http.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("'(http://tl[0-9]+\\.tubeload\\.to/file-[0-9]+/.*?/.*?)'").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("var url = '(http.*?)';").getMatch(0);
                }
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Finding the size takes time so once we got it we don't need to do it
        // again!
        if (downloadLink.getDownloadSize() == 0) {
            Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
