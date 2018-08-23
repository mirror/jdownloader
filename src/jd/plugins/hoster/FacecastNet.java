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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "facecast.net" }, urls = { "https?://(?:www\\.)?facecast\\.net/v/[A-Za-z0-9]+" })
public class FacecastNet extends PluginForHost {
    public FacecastNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.servustv.com/Nutzungsbedingungen";
    }

    /* Use this server as default until they change something or we find an easy way to find a working server. */
    // private static final String server_default = "http://edge-de-2.facecast.net";
    // TODO: Run this js to find fastest server: <script src="/v/player.min.js?c53b37b"></script>
    private static final String server_default = "http://edge-2.facecast.net";
    private long                date_start     = 0;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        this.br.getPage(server_default + "/eventdata?code=" + fid + "&ref=&_=" + System.currentTimeMillis());
        /* So far known errormessages: "Такого видео не существует" */
        final String error = PluginJSonUtils.getJsonValue(br, "error");
        if (br.getHttpConnection().getResponseCode() == 404 || error != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String date = PluginJSonUtils.getJsonValue(br, "date_plan_start_ts");
        if (date == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        date_start = Long.parseLong(date) * 1000;
        final String date_formatted = formatDate();
        link.setFinalFileName(date_formatted + "_facecast_.mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (this.date_start > System.currentTimeMillis()) {
            /* Seems like what the user wants to download hasn't aired yet --> Wait and retry later! */
            final long waitUntilStart = this.date_start - System.currentTimeMillis();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This video has not yet been broadcasted!", waitUntilStart);
        }
        final String videoid_intern = PluginJSonUtils.getJsonValue(br, "id");
        if (videoid_intern == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage("/public/" + videoid_intern + ".m3u8?_=" + System.currentTimeMillis());
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, url_hls);
        dl.startDownload();
    }

    private String formatDate() {
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date_start);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date_start);
        }
        return formattedDate;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}