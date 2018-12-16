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

import java.util.List;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "snotr.com" }, urls = { "https?://(www\\.)?snotr\\.com/video/\\d+" })
public class SnotrCom extends PluginForHost {
    public SnotrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String       dllink       = null;
    private HlsContainer hlsContainer = null;

    @Override
    public String getAGBLink() {
        return "http://www.snotr.com/faq";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">This video does not exist<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)( - Snotr)?</title>").getMatch(0);
        }
        String hlsURL = br.getRegex("<source src=\"([^<>\"]*?)\"").getMatch(0);
        if (hlsURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            br.getPage(hlsURL);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                if (Thread.currentThread() instanceof SingleDownloadController) {
                    hlsContainer = hlsbest;
                }
                final List<M3U8Playlist> playLists = M3U8Playlist.loadM3U8(hlsbest.getDownloadurl(), br);
                long estimatedSize = -1;
                for (M3U8Playlist playList : playLists) {
                    if (hlsbest.getBandwidth() > 0) {
                        playList.setAverageBandwidth(hlsbest.getBandwidth());
                        estimatedSize += playList.getEstimatedSize();
                    }
                }
                if (estimatedSize > 0) {
                    downloadLink.setDownloadSize(estimatedSize);
                }
                filename = filename.trim() + ".mp4";
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String m3u8 = hlsContainer.getDownloadurl();
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        sleep(2000, downloadLink);
        dl = new HLSDownloader(downloadLink, br, m3u8);
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
