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
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "smtbe.com" }, urls = { "http://[\\w\\.]*?ashemaletube\\.com/.*?clipflv-[0-9]+.*?\\.html" }, flags = { 0 })
public class ASheMaleTubeCom extends PluginForHost {

    public ASheMaleTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.ashemaletube.com/dmca.txt";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)Shemale Tube Video").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("description\" content=\"You are watching(.*?)free shemale porn").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<strong>title: </strong>(.*?)</td>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<div class=\"full\" style=\"text-align:left\">(.*?)</div><").getMatch(0);
                }
            }
        }
        dllink = br.getRegex("file=(http.*?\\.flv)").getMatch(0);
        if (dllink == null) dllink = br.getRegex("(http://213\\.174\\.153\\.16/protect/[0-9]+/.*?/flvs/xxx/.*?\\.flv)").getMatch(0);
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
