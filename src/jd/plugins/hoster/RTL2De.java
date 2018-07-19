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

import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtl2.de" }, urls = { "http://(www\\.)?rtl2\\.de/sendung/[^/]+/video/\\d+.+" })
public class RTL2De extends PluginForHost {
    /* Tags: rtl-interactive.de */
    private String url_rtmp = null;
    private String url_hls  = null;

    public RTL2De(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public String getAGBLink() {
        return "http://www.rtl2.de/3733.html";
    }

    /* OLD SwfVfy url:http://www.rtl2.de/flashplayer/vipo_player.swf */
    /* NEW SwfVfy url:http://www.rtl2.de/sites/default/modules/rtl2/jwplayer/assets/jwplayer.flash.swf */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /* Offline links should also have nice filenames */
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "rtl2\\.de/(.+)").getMatch(0));
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        final String jsredirect = br.getRegex("window\\.location\\.href = \"(/[^<>\"]*?)\";</script>").getMatch(0);
        if (jsredirect != null) {
            br.getPage("http://rtl2now.rtl2.de" + jsredirect);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>RTL2 \\- Seite nicht gefunden \\(404\\)</title>") || br.getURL().equals("http://www.rtl2.de/video/") || br.getURL().equals("http://www.rtl2.de/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* No free download possible --> Show as offline */
        if (br.getURL().contains("productdetail=1")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String vico_id = br.getRegex("vico_id[\t\n\r ]*?:[\t\n\r ]*?(\\d+)").getMatch(0);
        if (vico_id == null) {
            vico_id = br.getRegex("vico_id=(\\d+)").getMatch(0);
        }
        if (vico_id == null) {
            vico_id = new Regex(downloadLink.getDownloadURL(), "rtl2\\.de/sendung/[^/]+/video/(\\d+)").getMatch(0);
        }
        String vivi_id = br.getRegex("vivi_id[\t\n\r ]*?:[\t\n\r ]*?(\\d+)").getMatch(0);
        if (vivi_id == null) {
            vivi_id = br.getRegex("vivi_id=(\\d+)").getMatch(0);
        }
        if (vivi_id == null) {
            vivi_id = br.getRegex("data\\-video=\"(\\d+)\"").getMatch(0);
        }
        if (vico_id == null || vivi_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* 2018-07-19: new url - old was: http://www.rtl2.de/video/php/get_video.php?vico_id=... */
        br.getPage("http://www.rtl2.de/sites/default/modules/rtl2/mediathek/php/get_video_jw.php?appmode=0&vico_id=" + vico_id + "&vivi_id=" + vivi_id);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("video");
        if (entries.isEmpty()) {
            return AvailableStatus.UNCHECKED;
        }
        final String description = (String) entries.get("beschreibung");
        downloadLink.setFinalFileName((String) entries.get("titel") + "__" + (String) entries.get("titel") + ".mp4");
        if (!StringUtils.isEmpty(description) && downloadLink.getComment() == null) {
            downloadLink.setComment(description);
        }
        url_rtmp = (String) entries.get("streamurl");
        url_hls = (String) entries.get("streamurl_hls");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    @SuppressWarnings("deprecation")
    private void download(final DownloadLink downloadLink) throws Exception {
        if (StringUtils.isEmpty(url_rtmp) && StringUtils.isEmpty(url_hls)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!StringUtils.isEmpty(url_hls)) {
            /* 2018-07-19: Prefer hls */
            this.br.getPage(this.url_hls);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        } else {
            /* Possible apps: vod/rtl2/flv/, ondemand/flv_dach/vipo/[seriesname]/ */
            final Regex info = new Regex(url_rtmp, "([a-z]+://[a-z0-9\\-\\.]+/)([^<>/]+/[^<>/]+/[^<>/]+/(?:[^<>/]+/)?)(.+)");
            dl = new RTMPDownload(this, downloadLink, url_rtmp);
            final String protocol_host = info.getMatch(0);
            final String app = info.getMatch(1);
            String playpath = info.getMatch(2);
            if (protocol_host == null || app == null || playpath == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url = protocol_host + app;
            /* Correct playpath */
            /* Notes: Usually we'll have the vod app in this case */
            if (playpath.endsWith(".flv")) {
                playpath = playpath.substring(0, playpath.lastIndexOf("."));
            } else {
                playpath = "mp4:" + playpath;
            }
            /* Setup rtmp connection */
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPlayPath(playpath);
            rtmp.setSwfVfy("http://www.rtl2.de/sites/default/modules/rtl2/jwplayer/assets/jwplayer.flash.swf");
            rtmp.setFlashVer("WIN 16,0,0,305");
            rtmp.setUrl(url);
            rtmp.setApp(app);
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setResume(false);
            /* Make sure we use the right protocol... */
            rtmp.setProtocol(0);
            // rtmp.setRealTime();
            rtmp.setTimeOut(-10);
            ((RTMPDownload) dl).startDownload();
        }
    }

    // private void setupRTMPConnection(final DownloadInterface dl) {
    // final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
    // rtmp.setPlayPath(url_rtmp.split("@")[1]);
    // rtmp.setSwfVfy("http://www.rtl2.de/flashplayer/vipo_player.swf");
    // rtmp.setFlashVer("WIN 16,0,0,305");
    // rtmp.setUrl(url_rtmp.split("@")[0]);
    // rtmp.setResume(true);
    // rtmp.setRealTime();
    // rtmp.setTimeOut(-10);
    // }
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