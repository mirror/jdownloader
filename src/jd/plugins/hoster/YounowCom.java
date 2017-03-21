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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "younow.com" }, urls = { "https?://(?:www\\.)?younowdecrypted\\.com/[^/]+/\\d+" })
public class YounowCom extends PluginForHost {

    public YounowCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.younow.com/terms.php";
    }

    private String  server          = null;
    private String  stream          = null;
    /* 2016-02-28: hls only has one quality so it does not matter whether we use hls or rtmp. */
    private String  hls_master      = null;
    private boolean private_content = false;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("younowdecrypted.com/", "younow.com/"));
    }

    /**
     * List of API errorCodes and their meaning: <br />
     * 248 = "errorMsg":"Broadcast is not archived yet"<br />
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        private_content = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        br.getPage("https://cdn2.younow.com/php/api/broadcast/videoPath/broadcastId=" + fid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String name_url = fid + ".mp4";
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());

        final long errorcode = JavaScriptEngineFactory.toLong(entries.get("errorCode"), 0);
        if (errorcode == 247) {
            /* {"errorCode":247,"errorMsg":"Broadcast is private"} */
            link.setName(name_url);
            private_content = true;
            return AvailableStatus.TRUE;
        }
        if (errorcode > 0) {
            /* E.g. 263 = "errorMsg":"Replay no longer exists" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        final boolean videoAvailable = ((Boolean) entries.get("videoAvailable")).booleanValue();
        if (!videoAvailable) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        final String profileUrlString = (String) entries.get("profileUrlString");
        final String broadcastTitle = getbroadcastTitle(entries);
        String filename;
        if (profileUrlString != null && broadcastTitle != null) {
            filename = profileUrlString + "_" + fid + " - " + broadcastTitle;
        } else {
            filename = profileUrlString + "_" + fid + " - " + broadcastTitle;
        }
        server = (String) entries.get("server");
        stream = (String) entries.get("stream");
        hls_master = (String) entries.get("hls");
        filename = encodeUnicode(filename) + ".mp4";
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (private_content) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private broadcast");
        }
        if ((server == null || stream == null) && hls_master == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (hls_master != null && !hls_master.equals("")) {
            br.getPage(hls_master);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                /* At this stage the stream might have been deleted from the server. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final String url_hls;
            if (this.br.containsHTML("0.ts")) {
                /* Okay seems like there is no master so we already had the correct url */
                url_hls = hls_master;
            } else {
                /* Find BEST hls url containing .ts video segments */
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                if (hlsbest == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                url_hls = hlsbest.getDownloadurl();
            }
            checkFFmpeg(downloadLink, "Download a HLS Stream");

            final HLSDownloader downloader = new HLSDownloader(downloadLink, br, url_hls);

            final StreamInfo streamInfo = downloader.getProbe();
            if (downloader.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "HLS Server error 403 - stream might be offline", 60 * 60 * 1000l);
            } else if (streamInfo == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "HLS Server error - stream might be offline", 60 * 60 * 1000l);
            }

            dl = downloader;
            dl.startDownload();
        } else {
            try {
                dl = new RTMPDownload(this, downloadLink, server);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setUrl(server);
            rtmp.setPlayPath(stream);
            // rtmp.setApp(app);
            rtmp.setFlashVer("WIN 11,8,800,94");
            rtmp.setSwfUrl("http://cdn2.younow.com/js/jwplayer6/jwplayer.flash.swf");
            rtmp.setResume(false);
            rtmp.setLive(true);
            ((RTMPDownload) dl).startDownload();
        }
    }

    public static String getbroadcastTitle(final LinkedHashMap<String, Object> entries) {
        return (String) entries.get("broadcastTitle");
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}