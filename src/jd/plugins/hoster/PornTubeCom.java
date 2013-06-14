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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "porntube.com" }, urls = { "http://(www\\.)?porntube\\.com/videos/[a-z0-9\\-]+_\\d+" }, flags = { 0 })
public class PornTubeCom extends PluginForHost {

    private String DLLINK = null;

    public PornTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.porntube.com/info#terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(page-not-found\\.jpg\"|<title>Error 404 \\- Page not Found \\| PornTube\\.com</title>|alt=\"Page not Found\")")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>([^<>\"]*?) | PornTube \\&#174;</title>").getMatch(0);
        if (filename == null) filename = br.getRegex(">Videos</a> \\&gt; </strong>([^<>\"]*?)</h2>").getMatch(0);
        getDllink();
        String ext = "mp4";
        if (DLLINK.contains(".flv")) ext = "flv";
        filename = filename.endsWith(".") ? filename + ext : filename + "." + ext;
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
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

    private void getDllink() throws PluginException, IOException {
        DLLINK = br.getRegex("addVariable\\(\'config\', \'(/.*?)\'").getMatch(0);
        if (DLLINK == null) DLLINK = br.getRegex("\'(/xml/index\\?id=[0-9\\-]+)\'").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        br.getPage("http://www.porntube.com" + Encoding.htmlDecode(DLLINK));
        DLLINK = br.getRegex("<file>(http://.*?)</file>").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        final String[] qualities = { "720p", "480p", "360p", "240p" };
        for (final String quality : qualities) {
            DLLINK = getQuality(quality);
            if (DLLINK != null) break;
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
    }

    private String getQuality(final String quality) {
        return br.getRegex("label=\"" + quality + "\"( [a-z0-9]+=\"[a-z0-9]+\")?><file>(http://[^<>\"]*?)</file>").getMatch(1);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}