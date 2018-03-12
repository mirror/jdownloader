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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tokyo-tube.com" }, urls = { "https?://(www\\.)?tokyo\\-tube\\.com/video/\\d+" })
public class TokyoTubeCom extends PluginForHost {
    /** DEVNOTES: this hoster has broken gzip, which breaks stable support, that's why we disable it */
    private String dllink = null;

    public TokyoTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tokyo-tube.com/static/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Encoding", "identity");
        br.postPage(downloadLink.getDownloadURL(), "language=en_US");
        if (br.getURL().contains("tokyo-tube.com/error/video_missing") || br.containsHTML("(>This video cannot be found|Are you sure you typed in the correct url\\?<|<title>無料アダルト動画 TokyoTube\\-Japanese Free Porn</title>)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<div class=\"left span\\-630\">[\t\n\r ]+<h2>(.*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>([^\"\\'<>]+)無料アダルト動画 TokyoTube\\-Japanese Free Porn</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^\"\\'<>]+)無料アダルト動画 TokyoTube\\-Japanese Free Porn</title>").getMatch(0);
            }
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)TokyoTube</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* 2017-01-05: Changed from: "http://www.tokyo-tube.com/media/player/config.php?vkey=" */
        br.getPage("http://www.tokyo-tube.com/media/videojs/mediainfo.php?vid=" + new Regex(downloadLink.getDownloadURL(), "tokyo\\-tube\\.com/video/(\\d+)").getMatch(0));
        dllink = PluginJSonUtils.getJsonValue(br, "src");
        if (dllink.isEmpty() && PluginJSonUtils.getJsonValue(br, "type").equals("private_video")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private video");
        }
        if (dllink == null || dllink.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // this.br.getPage(dllink);
        // final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        // final String url_hls = hlsbest.downloadurl;
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, dllink);
        dl.startDownload();
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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