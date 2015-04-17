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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "n24.de" }, urls = { "http://(www\\.|m\\.)?n24\\.de/[^<>\"]*?(M|m)ediathek/[^/]+/d/\\d+/[a-z0-9\\-]+\\.html" }, flags = { 32 })
public class N24Mediathek extends PluginForHost {

    private static final String html_videounavailable = "class=\"video_not_ready\"";

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
    public void correctDownloadLink(final DownloadLink link) {
        /* Correct mobile URLs --> Normal URLs */
        final String linkpart = new Regex(link.getDownloadURL(), "n24\\.de/(.+)").getMatch(0);
        link.setUrlDownload("http://www.n24.de/" + linkpart);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /* Offline links should also get nice filenames */
        downloadLink.setName(getFilenamefromURL(downloadLink.getDownloadURL()) + ".mp4");
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String titleName = br.getRegex("title: \\'([^<>\"\\']*?)\\'").getMatch(0);
        if (br.containsHTML(html_videounavailable)) {
            if (titleName == null) {
                titleName = getFilenamefromURL(downloadLink.getDownloadURL());
            }
            titleName = Encoding.htmlDecode(titleName.trim());
            titleName = encodeUnicode(titleName);
            downloadLink.setName(titleName + ".mp4");
            return AvailableStatus.TRUE;
        }
        /* Check offline here as temporary-unavailable videos also return 404 and browser doesn't contain player for them either! */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("n24VideoCfg\\.")) {
            /* Not a video (offline or maybe picture gallery) */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (titleName == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        titleName = Encoding.htmlDecode(titleName).trim();
        downloadLink.setFinalFileName(titleName + ".mp4");

        return AvailableStatus.TRUE;
    }

    private String getFilenamefromURL(final String url) {
        return new Regex(url, "/d/\\d+/([^<>\"]*?)\\.html").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(html_videounavailable)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Dieses Video ist aktuell nicht verfügbar", 1 * 60 * 60 * 1000l);
        }
        final String hlsurl = br.getRegex("_n24VideoCfg\\.html5.videoMp4Source = \"(http://n24[^<>\"]*?)\"").getMatch(0);
        if (hlsurl != null) {
            downloadHLS_HTTP(downloadLink, hlsurl);
        } else {
            /*
             * RTMP links are nearly always available but often don't work if the files are hosted on the n24 servers. Externally hosted
             * content works via rtmp more often but because we prefer hls, rtmp is barely used!
             */
            downloadRTMP(downloadLink);
        }

    }

    private void downloadRTMP(final DownloadLink downloadLink) throws Exception {
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

    private void downloadHLS_HTTP(final DownloadLink downloadLink, final String index) throws Exception {
        String finallink = null;
        String hlsBEST = null;
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        br.getPage(index);
        final String http_base = new Regex(index, "adaptive\\.level\\d+\\.net/(cm\\d{4}/[^<>\"]*?/)[a-f0-9]{32}").getMatch(0);
        if (br.containsHTML("#EXT-X-MEDIA-SEQUENCE:")) {
            /* We already got the finallink */
            finallink = index;
        } else {
            final String[] renditions = br.getRegex("#EXT-X-STREAM-INF(.*?\\.m3u8)").getColumn(0);
            long higestbandwidth = -1;
            for (final String linkinfo : renditions) {
                long bandwidthlong = 0;
                final String bandwidth = new Regex(linkinfo, "BANDWIDTH=(\\d+)").getMatch(0);
                final String url = new Regex(linkinfo, "\n(.+\\.m3u8)").getMatch(0);
                if (bandwidth == null || url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                bandwidthlong = Long.parseLong(bandwidth);
                if (bandwidthlong > higestbandwidth) {
                    higestbandwidth = bandwidthlong;
                    hlsBEST = url;
                }
            }
            final String hls_base = new Regex(index, "(http[^<>].+/)[^/]+").getMatch(0);
            finallink = hls_base + hlsBEST;
        }
        /*
         * Last checked 17.04.15: hls- and http links are the same qualities (minor differences, 0,XX MB). Whenever the files are hosted on
         * the n24 servers, http downloads are definitly possible!
         */
        if (http_base != null && hlsBEST != null) {
            /* Content is on the n24 servers --> HTTP download possible (usually only used for their mobile website) */
            final String http_final = "http://vcdn.n24.de/" + http_base + hlsBEST.replace(".m3u8", "");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, http_final, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            dl = new HLSDownloader(downloadLink, br, finallink);
            dl.startDownload();
        }
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
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