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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videarn.com" }, urls = { "http://(www\\.)?videarndecrypted\\.com/video\\.php\\?id=\\d+" }, flags = { 32 })
public class VidearnCom extends PluginForHost {

    public VidearnCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        // Links come from a decrypter
        link.setUrlDownload(link.getDownloadURL().replace("videarndecrypted.com/", "videarn.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://videarn.com/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        final String playpath = br.getRegex("file:'(.*?)',").getMatch(0);
        final String url = br.getRegex("streamer:'(.*?)',").getMatch(0);
        if (playpath == null && url == null) {
            /* videarn now also supports download of the flv stream */
            String directURL = br.getRegex("player\\.swf.*?file: \"(http://.*?)\"").getMatch(0);
            if (directURL != null) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, directURL, true, 0);
                if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("text")) {
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
                return;
            }
        }
        if (playpath == null || url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (oldStyle()) { throw new PluginException(LinkStatus.ERROR_FATAL, "Not supported yet!"); }
        dl = new RTMPDownload(this, downloadLink, url + playpath);
        final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        final String host = url.substring(0, url.lastIndexOf("lb/"));
        final String app = "videarn";
        if (host == null || app == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        rtmp.setResume(false); // Must be set on false
        rtmp.setPlayPath(playpath);
        rtmp.setUrl(host + app);
        rtmp.setSwfUrl("http://videarn.com/player.swf");

        ((RTMPDownload) dl).startDownload();
    }

    private boolean oldStyle() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        int rev = Integer.parseInt(prev);
        if (rev < 14000) return true;
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        final String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (!br.containsHTML("\\w+")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<h3 class=\"page\\-title\"><strong>(.*?)</strong></h3>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Video \\- (.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = dllink.substring(dllink.lastIndexOf("/"));
            }
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim() + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}