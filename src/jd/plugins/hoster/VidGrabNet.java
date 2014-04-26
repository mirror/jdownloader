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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vidgrab.net" }, urls = { "http://(www\\.)?vidgrab\\.net/\\d+/[^<>\"]*?\\.html" }, flags = { 0 })
public class VidGrabNet extends PluginForHost {

    public VidGrabNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://vidgrab.net/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">PAGE NOT FOUND\\!<") || br.getURL().contains("/?404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>([^<>\"]*?) \\- VIDGRAB</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<div class=\"singlepost\">[^<>\"]+<h2>([^<>\"]*?)</h2>").getMatch(0);
        DLLINK = br.getRegex("config=([^<>\"\\']*?)(\"|\\')").getMatch(0);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://vidgrab.net/95000.php?id=" + Encoding.htmlDecode(DLLINK));
        DLLINK = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
        /* Video does not load --> Probably down */
        if (DLLINK == null && br.containsHTML("<file>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if ((ext == null || ext.length() > 5) && DLLINK.contains("MP4")) ext = ".mp4";
        if (ext == null || ext.length() > 5) ext = ".flv";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
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
