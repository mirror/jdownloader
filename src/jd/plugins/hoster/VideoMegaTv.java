//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videomega.tv" }, urls = { "http://(www\\.)?videomega\\.tv/\\?ref=[A-Za-z0-9]+" }, flags = { 0 })
public class VideoMegaTv extends PluginForHost {

    public VideoMegaTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://videomega.tv/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        br.getPage("http://videomega.tv/iframe.php?ref=" + fid + "&width=595&height=340");
        if (br.containsHTML(">VIDEO NOT FOUND")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setFinalFileName(fid + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String escaped = br.getRegex("document\\.write\\(unescape\\(\"([^<>\"]*?)\"").getMatch(0);
        if (escaped == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        escaped = Encoding.htmlDecode(escaped);
        String dllink = new Regex(escaped, "file: \"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(escaped, "\"(http://[a-z0-9]+\\.videomega\\.tv/vids/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+\\.mp4)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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