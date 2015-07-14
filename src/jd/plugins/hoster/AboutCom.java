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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "about.com" }, urls = { "http://(www\\.)?video\\.about\\.com/\\w+/[\\w\\-]+\\.htm" }, flags = { 32 })
public class AboutCom extends PluginForHost {

    private String DLLINK = null;

    public AboutCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.about.com/gi/pages/uagree.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /** Last revision containing the old AMF handling: 29950 */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        String dlink = link.getDownloadURL();
        br.getPage(dlink);
        if (br.containsHTML("404 Document Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String filename = br.getRegex("<meta itemprop=\"name\" content=\"([^\"]+)\"").getMatch(0);
        String playerKey = br.getRegex("\"playerKey\".value=\"([^\"]+)\"").getMatch(0);
        String videoPlayer = br.getRegex("\"@videoPlayer\".value=\"([^\"]+)\"").getMatch(0);
        String playerId = br.getRegex("\"playerID\".value=\"(\\d+)").getMatch(0);
        String publisherID = br.getRegex("name=\"publisherID\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (publisherID == null) {
            publisherID = "4013";
        }
        if (filename == null || playerKey == null || playerId == null || videoPlayer == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        final String brightcove_URL = "http://c.brightcove.com/services/viewer/htmlFederated?&width=340&height=192&flashID=&includeAPI=true&templateLoadHandler=templateLoaded&templateReadyHandler=playerReady&bgcolor=%23FFFFFF&htmlFallback=true&playerID=" + playerId + "&publisherID=" + publisherID + "&playerKey=" + Encoding.urlEncode(playerKey) + "&isVid=true&isUI=true&dynamicStreaming=true&optimizedContentLoad=true&wmode=transparent&%40videoPlayer=" + videoPlayer + "&allowScriptAccess=always";
        this.br.getPage(brightcove_URL);
        final jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveClipData bestBrightcoveVersion = jd.plugins.decrypter.BrightcoveDecrypter.findBestVideoHttpByFilesize(this.br);
        if (bestBrightcoveVersion == null || bestBrightcoveVersion.creationDate == -1 || filename == null || bestBrightcoveVersion.downloadurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = bestBrightcoveVersion.downloadurl;
        link.setFinalFileName(bestBrightcoveVersion.getStandardFilename());
        link.setDownloadSize(bestBrightcoveVersion.size);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (DLLINK.startsWith("rtmp")) {
            /* 2015-07-13 - rtmp is unused from now on */
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();

        } else {
            br.setFollowRedirects(true);
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (DLLINK.startsWith("mms")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (mms://) not supported!");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        String[] tmpRtmpUrl = DLLINK.split("@");
        rtmp.setUrl(tmpRtmpUrl[0] + tmpRtmpUrl[1]);
        rtmp.setApp(tmpRtmpUrl[1] + tmpRtmpUrl[3] + tmpRtmpUrl[4]);
        rtmp.setPlayPath(tmpRtmpUrl[2] + tmpRtmpUrl[3] + tmpRtmpUrl[4]);
        // rtmp.setConn("B:0");
        // rtmp.setConn("S:" + tmpRtmpUrl[2] + tmpRtmpUrl[3]);
        rtmp.setSwfVfy("http://admin.brightcove.com/viewer/us20121102.1044/federatedVideo/BrightcovePlayer.swf");
        rtmp.setResume(true);
        rtmp.setRealTime();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}