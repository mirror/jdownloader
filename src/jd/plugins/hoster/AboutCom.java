//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.util.Arrays;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveEdgeContainer;
import jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveEdgeContainer.Protocol;

import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "about.com" }, urls = { "https?://[A-Za-z0-9\\-]+\\.about\\.com/video/[\\w\\-]+\\.htm" })
public class AboutCom extends PluginForHost {

    private String dllink = null;

    public AboutCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.about.com/legal.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /** Last revision containing the old AMF handling: 29950 */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;

        setBrowserExclusive();
        String dlink = link.getDownloadURL();
        br.getPage(dlink);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        final BrightcoveEdgeContainer bestQuality = jd.plugins.decrypter.BrightcoveDecrypter.findBESTBrightcoveEdgeContainerAuto(this.br, Arrays.asList(new Protocol[] { Protocol.HLS, Protocol.HTTP }));
        if (bestQuality == null) {
            /* We assume that the page does not contain any video-content --> Offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = bestQuality.getDownloadURL();
        bestQuality.setInformationOnDownloadLink(link);

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        if (dllink != null && dllink.contains(".m3u8")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    // private void setupRTMPConnection(DownloadInterface dl) {
    // jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
    // String[] tmpRtmpUrl = dllink.split("@");
    // rtmp.setUrl(tmpRtmpUrl[0] + tmpRtmpUrl[1]);
    // rtmp.setApp(tmpRtmpUrl[1] + tmpRtmpUrl[3] + tmpRtmpUrl[4]);
    // rtmp.setPlayPath(tmpRtmpUrl[2] + tmpRtmpUrl[3] + tmpRtmpUrl[4]);
    // // rtmp.setConn("B:0");
    // // rtmp.setConn("S:" + tmpRtmpUrl[2] + tmpRtmpUrl[3]);
    // rtmp.setSwfVfy("http://admin.brightcove.com/viewer/us20121102.1044/federatedVideo/BrightcovePlayer.swf");
    // rtmp.setResume(true);
    // rtmp.setRealTime();
    // }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}