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

import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dctp.tv" }, urls = { "https?://(?:www\\.)?dctp\\.tv/filme/([a-z0-9_\\-]+)/?" })
public class DctpTv extends PluginForHost {
    public DctpTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.dctp.tv/";
    }

    /* Tags: spiegel.tv, dctp.tv */
    private Map<String, Object> entries = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("class=\\'error\\'>Der gewünschte Film ist \\(zur Zeit\\) nicht") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String json = br.getRegex("window\\.__PRELOADED_STATE__ = (\\{.*?\\});").getMatch(0);
        if (json == null) {
            /* 2020-03-16 */
            json = br.getRegex("type=\"application/json\">(.*?)</script>").getMatch(0);
        }
        entries = JavaScriptEngineFactory.jsonToJavaMap(json);
        Object mediaInfo = JavaScriptEngineFactory.walkJson(entries, "ivms/media_items/{0}");
        if (mediaInfo == null) {
            /* 2020-03-16 */
            mediaInfo = JavaScriptEngineFactory.walkJson(entries, "props/pageProps/media");
        }
        entries = (Map<String, Object>) mediaInfo;
        // final List<Object> ressourcelist = (List<Object>) entries.get("");
        String title = (String) entries.get("title");
        String date = (String) entries.get("airdate");
        if (StringUtils.isEmpty(date)) {
            /* 2019-01-21: Fallback e.g. required for very old content! */
            date = (String) entries.get("web_airdate");
        }
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = "";
        if (!StringUtils.isEmpty(date)) {
            filename += date;
        }
        filename += "_dctpTV_" + title + ".mp4";
        link.setFinalFileName(filename);
        final String description = (String) entries.get("description");
        if (link.getComment() == null) {
            link.setComment(description);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final Object is_wide = entries.get("is_wide");
        String scalefactor;
        String scalefactor_new;
        if (Boolean.TRUE.equals(is_wide)) {
            scalefactor = "16x9";
            scalefactor_new = "720p";
        } else {
            scalefactor = "0500_4x3";
            scalefactor_new = "0500_4x3";
        }
        final String uuid = (String) entries.get("uuid");
        if (StringUtils.isEmpty(uuid)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String playpath = String.format("mp4:%s_dctp_0500_%s.m4v", uuid, scalefactor);
        final boolean hlsUseNewWay = true;
        String url_hls_master = null;
        if (hlsUseNewWay) {
            url_hls_master = String.format("https://cdn-segments.dctp.tv/%s_dctp_%s.m4v/playlist.m3u8", uuid, scalefactor_new);
        } else {
            final String hls_playpath_path = this.br.getRegex("(/[^<>\"\\']+/playlist\\.m3u8)").getMatch(0);
            String hls_stream_server = null;
            try {
                this.br.getPage("http://www.dctp.tv/elastic_streaming_client/get_streaming_server/");
                /* 2017-02-21: Usually '54.83.19.217' */
                hls_stream_server = PluginJSonUtils.getJsonValue(this.br, "server");
            } catch (final Throwable e) {
            }
            if (hls_stream_server != null && !hls_stream_server.equals("") && hls_playpath_path != null) {
                final boolean build_custom_url = true;
                if (build_custom_url) {
                    url_hls_master = String.format("http://%s/vods3/_definst/mp4:dctp_completed_media/%s_dctp_%s.m4v/playlist.m3u8", hls_stream_server, uuid, scalefactor);
                } else {
                    url_hls_master = String.format("http://%s%s", playpath, hls_playpath_path);
                }
                if (url_hls_master == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage(url_hls_master);
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                final String url_hls = hlsbest.getDownloadurl();
                if (url_hls == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                checkFFmpeg(link, "Download a HLS Stream");
                dl = new HLSDownloader(link, br, url_hls);
                dl.startDownload();
            }
        }
        final String url_hls;
        if (hlsUseNewWay) {
            url_hls = url_hls_master;
        } else {
            br.getPage(url_hls_master);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            url_hls = hlsbest.getDownloadurl();
        }
        if (url_hls == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, url_hls);
        dl.startDownload();
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