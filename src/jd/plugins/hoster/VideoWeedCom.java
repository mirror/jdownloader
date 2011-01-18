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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videoweed.com" }, urls = { "http://((www\\.)?videoweed\\.com/file/|embed\\.videoweed\\.com/embed\\.php\\?.*?v=)[a-z0-9]+" }, flags = { 0 })
public class VideoWeedCom extends PluginForHost {

    public VideoWeedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.videoweed.com/terms.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Make normal links out of embedded links
        String fileID = new Regex(link.getDownloadURL(), "v=([a-z0-9]+)").getMatch(0);
        if (fileID != null) link.setUrlDownload("http://www.videoweed.com/file/" + fileID);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>This file no longer exists on our servers\\.<|The video file was removed)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("name=\"title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("videoweed\\.com/file/[a-z0-9]+\\&title=(.*?)\\+-\\+VideoWeed\\.com\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<td><strong>Title: </strong>(.*?)</td>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<td width=\"580\">[\t\n\r ]+<div class=\"div_titlu\">(.*?) - <a").getMatch(0);
                    if (filename == null) filename = br.getRegex("colspan=\"2\"><strong>Title: </strong>(.*?)</td>").getMatch(0);
                }
            }
        }
        dllink = br.getRegex("flashvars\\.file=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://(www\\.)?videoweed\\.com/stream/.*?\\.flv)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(http://(www\\.)?n\\d+\\.(epornik|videoweed)\\.com/dl/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+\\.flv)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("addVariable\\(\"(file|streamer)\",\"(http://.*?)\"\\)").getMatch(1);
                    if (dllink == null) {
                        dllink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/dl/[a-z0-9]+/[a-z0-9]+/.*?\\.flv)\"").getMatch(0);
                    }
                }
            }
        }
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename.replace(filename.substring(filename.length() - 4, filename.length()), "") + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
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
