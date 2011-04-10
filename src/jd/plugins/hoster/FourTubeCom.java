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
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4tube.com" }, urls = { "http://(www\\.)?4tube\\.com/videos/\\d+/?([\\w-]+)?" }, flags = { PluginWrapper.DEBUG_ONLY })
public class FourTubeCom extends PluginForHost {

    public FourTubeCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.4tube.com/legal/privacy";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String configUrl = br.getRegex("'flashvars','config=(.*?)'\\)").getMatch(0);
        if (configUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage("http://" + br.getHost() + configUrl);

        final String playpath = br.getRegex("<file>(.*?)</file>").getMatch(0);
        final String type = br.getRegex("<type>(.*?)</type>").getMatch(0);
        final String token = br.getRegex("<token>(.*?)</token>").getMatch(0);
        final String url = br.getRegex("<streamer>(.*?)</streamer>").getMatch(0);
        if (playpath == null || url == null || type == null || token == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        dl = new RTMPDownload(this, downloadLink, url + "/" + playpath);
        final RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        final String host = url.substring(0, url.lastIndexOf("/") + 1);
        final String app = url.replace(host, "");
        if (host == null || app == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        if (app.equals("vod/")) {
            rtmp.setLive(true);
        } else {
            rtmp.setResume(true);
        }
        rtmp.setToken(token);
        rtmp.setPlayPath(playpath);
        rtmp.setApp(app);
        rtmp.setUrl(host + app);
        rtmp.setSwfUrl("http://www.4tube.com/player2.swf");

        ((RTMPDownload) dl).startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        final String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (br.containsHTML("Page not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<meta name=\"description\".*?\\) (.*?)\\. ").getMatch(0);
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
