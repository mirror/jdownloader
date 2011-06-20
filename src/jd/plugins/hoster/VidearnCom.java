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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videarn.com" }, urls = { "http://(www\\.)?videarn\\.com/video\\.php\\?id=\\d+" }, flags = { PluginWrapper.DEBUG_ONLY })
public class VidearnCom extends PluginForHost {

    public VidearnCom(final PluginWrapper wrapper) {
        super(wrapper);
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

        /**
         * NOT WORKING IN RTMPDUMP Version < 2.3
         */
        final String nw = "rtmpdump";
        if (nw.equals("rtmpdump")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Not supported yet!"); }

        final String playpath = br.getRegex("file:'(.*?)',").getMatch(0);
        final String url = br.getRegex("streamer:'(.*?)',").getMatch(0);
        if (playpath == null || url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        final String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (!br.containsHTML("\\w+")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<h2 class=\"page_title\"><br />(.*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = dllink.substring(dllink.lastIndexOf("/"));
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
