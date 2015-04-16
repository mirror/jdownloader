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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "n24.de" }, urls = { "http://(www\\.)?n24\\.de/[^<>\"]*?(M|m)ediathek/[^/]+/d/\\d+/[a-z0-9\\-]+\\.html" }, flags = { 32 })
public class N24Mediathek extends PluginForHost {

    private String DLLINK = null;

    public N24Mediathek(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://image5-cdn.n24.de/blob/5354958/3/agb-produktion-n24-data.pdf";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("n24VideoCfg\\.")) {
            /* Not a video (offline or maybe picture gallery) */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String titleName = br.getRegex("title: \\'([^<>\"\\']*?)\\'").getMatch(0);
        if (titleName == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        titleName = Encoding.htmlDecode(titleName).trim();
        downloadLink.setFinalFileName(titleName + ".mp4");

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /*
         * Sometimes they also got HLS available but so far I saw it only once and also it was never enforced - only for html5
         * implementation!
         */
        final String rtmp_server = br.getRegex("_n24VideoCfg\\.flash\\.videoFlashconnectionUrl = \"(rtmp://[^<>\"]*?)\"").getMatch(0);
        final String rtmp_path = br.getRegex("_n24VideoCfg\\.flash\\.videoFlashSource = \"([^<>\"]*?)\"").getMatch(0);
        if (rtmp_server == null || rtmp_path == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new RTMPDownload(this, downloadLink, rtmp_server);
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(rtmp_server);
        rtmp.setPlayPath(rtmp_path);
        rtmp.setSwfVfy("http://www.n24.de/_swf/HomePlayer.swf?cachingVersion=2.74");
        rtmp.setPageUrl(this.br.getURL());
        rtmp.setResume(true);

        ((RTMPDownload) dl).startDownload();

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