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
import java.util.Locale;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

/*Similar websites: bca-onlive.de, asscompact.de*/
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alphatv.gr" }, urls = { "https?://(www\\.)?alphatv\\.gr/shows/.+" })
public class AlphatvGr extends PluginForHost {

    public AlphatvGr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.alphatv.gr/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.containsHTML("jwplayer\\.flash\\.swf\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String date = br.getRegex("property=\"article:published_time\" content=\"([^<>\"]*?)\"").getMatch(0);
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null || date == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        date = date.trim();
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);

        final String date_formatted = formatDate(date);

        filename = date_formatted + "_alphatv_" + filename + ".mp4";
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String hls_master = this.br.getRegex("(?:\\'|\")(http://[^<>\"]*?\\.m3u8)(?:\\'|\")").getMatch(0);
        final String url_rtmp = this.br.getRegex("(?:\\'|\")(rtmp://[^<>\"]*?\\.mp4)(?:\\'|\")").getMatch(0);
        if (hls_master == null && url_rtmp == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_rtmp != null) {
            /* Prefer rtmp download */
            final Regex rtmp_regex = new Regex(url_rtmp, "(^rtmp://[^/]+/)([^/]+)/(mp4:.+)");
            String rtmp_host = rtmp_regex.getMatch(0);
            final String rtmp_app = rtmp_regex.getMatch(1);
            final String url_playpath = rtmp_regex.getMatch(2);
            if (url_playpath == null || rtmp_app == null || rtmp_host == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            rtmp_host += rtmp_app;

            dl = new RTMPDownload(this, downloadLink, url_rtmp);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setPlayPath(url_playpath);
            rtmp.setPageUrl(this.br.getURL());
            rtmp.setSwfVfy("http://static.adman.gr/jwplayer.flash.swf");
            // rtmp.setFlashVer("WIN 18,0,0,194");
            rtmp.setApp(rtmp_app);
            rtmp.setUrl(url_rtmp);
            rtmp.setResume(true);
        } else {
            /* If no rtmp url is available, download HLS */
            br.getPage(hls_master);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.downloadurl;
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
        }
        dl.startDownload();
    }

    private String formatDate(String input) {
        // 2015-06-28T15:00:00+03:00
        /* 2015-06-23T20:15:00+02:00 --> 2015-06-23T20:15:00+0200 */
        input = input.substring(0, input.lastIndexOf(":")) + "00";
        final long date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ssZ", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
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