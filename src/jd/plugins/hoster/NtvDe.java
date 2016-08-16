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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "n-tv.de" }, urls = { "https?://(www\\.)?n\\-tv\\.de/mediathek/videos/[^/]+/[^/]+\\.html" }, flags = { 0 })
public class NtvDe extends PluginForHost {

    public NtvDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.n-tv.de/ntvintern/n-tv-Webseiten-article495196.html";
    }

    private static final String host_hls = "http://video.n-tv.de";

    // private static final String host_http = "http://video.n-tv.de";
    // private static final String host_rtmp = "rtmp://fms.n-tv.de/ntv/";

    /*
     * Possible rtmpdump parameters: rtmpdump -r "rtmp://fms.n-tv.de/ntv/" -W
     * "http://www.n-tv.de/resources/ts23917643-2/ver1-0/videoplayer/ntv_player.swf" -p "http://www.ntv.de/" -R -y
     * "mp4:2015/06/ContentPoolSchiesspolizist_1506251137.f4v" -o "ntvtest.mp4"
     */
    /* Possible http url (low quality for old mobile phones): http://video.n-tv.de/mobile/ContentPoolSchiesspolizist_1506251137.mp4 */
    /* Available streaming types (best to worst): hls, rtmpe, http */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String date = br.getRegex("publishedDateAsUnixTimeStamp:[\t\n\r ]*?\"(\\d+)\"").getMatch(0);
        String title = br.getRegex("ntv\\.pageInfo\\.title[\t\n\r ]*?=[\t\n\r ]*?\"([^<>]*?)\";\r").getMatch(0);
        if (title == null || date == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = title.replace("\\", "");
        title = Encoding.htmlDecode(title).trim();
        title = encodeUnicode(title);
        final String filename = formatDate(date) + "_n-tv_" + title + ".mp4";
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("videoM3u8:[\t\n\r ]*?\"(/apple/[^<>\"/]+\\.m3u8)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Failed to find HLS index");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Access index */
        br.getPage(host_hls + dllink);
        /* Grab highest quality possible - always located at the beginning of the index file (for this host). */
        dllink = br.getRegex("([^<>\"/\r\n\t]+\\.m3u8)\n").getMatch(0);
        if (dllink == null) {
            logger.warning("Failed to find HLS quality");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = host_hls + "/apple/" + dllink;
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, this.br, dllink);
        dl.startDownload();
    }

    public String formatDate(final String input) {
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        final Date theDate = new Date(Long.parseLong(input) * 1000);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
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