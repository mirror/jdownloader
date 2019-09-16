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

import java.text.DecimalFormat;
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tv.nrk.no" }, urls = { "https?://(?:www\\.)?tv\\.nrk\\.no/serie/[a-z0-9\\-]+(?:/[A-Z]{4}\\d{8})?/sesong(?:\\-|/)\\d+/episode(?:\\-|/)\\d+" })
public class TvNrkNo extends PluginForHost {
    public TvNrkNo(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags:
    // protocol: https possible (without API)
    // other: E.G. embed URL: http://www.nrk.no/embed/PS*234450
    // other2: In theory this could also handle their embedded videos which can be found e.g. here: http://p3.no/dokumentar/big-in-bangkok/
    // Example hds:
    // http://nordond27b-f.akamaihd.net/z/wo/open/f9/f90fe5e8dce1f147096b40bbb7354ecc3d09f420/b3a61e83-df5a-463a-bebe-420f24fb6455_,141,316,563,1266,2250,.mp4.csmil/manifest.f4m
    // Example hls:
    // http://nordond27b-f.akamaihd.net/i/wo/open/f9/f90fe5e8dce1f147096b40bbb7354ecc3d09f420/b3a61e83-df5a-463a-bebe-420f24fb6455_,141,316,563,1266,2250,.mp4.csmil/master.m3u8

    /* Connection stuff */
    private static final int              free_maxdownloads = -1;
    private boolean                       isAvailable       = true;
    private boolean                       newAPI            = false;
    private LinkedHashMap<String, Object> entries           = null;

    @Override
    public String getAGBLink() {
        return "http://www.nrk.no/kontakt/";
    }

    /**
     * Using API: http://v8.psapi.nrk.no/Help </br>
     * 2019-09-16: Also added support for new API: https://psapi-ne.nrk.no/documentation/
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        isAvailable = true;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), ".+/serie/([^<>\"/]+)(/([A-Z]{4}\\d{8}))?/sesong.(\\d+)/episode.(\\d+)");
        final String url_series_title = urlinfo.getMatch(0);
        String clip_id = urlinfo.getMatch(2);
        final String url_season = urlinfo.getMatch(3);
        final String url_episode = urlinfo.getMatch(4);
        String apikey = null;
        String new_api_base_url = null;
        if (clip_id == null) {
            /* 2019-09-16: New URLs do not contain the clip_id anymore */
            br.getPage(link.getPluginPatternMatcher());
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                /* 404 handling is enough to determine offline state (via API AND website)! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            clip_id = br.getRegex("data-program-id=\"([^\"]+)\"").getMatch(0);
            apikey = br.getRegex("data-key=\"([a-f0-9]{32})\"").getMatch(0);
            new_api_base_url = br.getRegex("data-psapi-base-url=\"(http[^\"]+)\"").getMatch(0);
        }
        /* 2019-09-16: Both ways are still working! */
        final boolean use_new_api = true;
        String series_title = null;
        String description = null;
        if (use_new_api) {
            /* 2019-09-16: New */
            newAPI = true;
            if (new_api_base_url == null) {
                /* 2019-09-16 */
                /* Also possible: https://psapi-we.nrk.no */
                new_api_base_url = "https://psapi.nrk.no";
            }
            if (apikey == null) {
                /* 2019-09-16 */
                apikey = "d1381d92278a47c09066460f2522a67d";
            }
            this.br.getPage(new_api_base_url + "/programs/" + clip_id + "?apiKey=" + apikey);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                /* 404 handling is enough to determine offline state (via API AND website)! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            series_title = (String) entries.get("seriesTitle");
            description = (String) entries.get("longDescription");
            if (StringUtils.isEmpty(description)) {
                description = (String) entries.get("shortDescription");
            }
            final String status = (String) JavaScriptEngineFactory.walkJson(entries, "availability/status");
            isAvailable = "available".equalsIgnoreCase(status);
        } else {
            /* Old but still working and does not require apikey */
            newAPI = false;
            this.br.getPage("http://v8.psapi.nrk.no/mediaelement/" + clip_id);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                /* 404 handling is enough to determine offline state (via API AND website)! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            isAvailable = ((Boolean) entries.get("isAvailable")).booleanValue();
            series_title = (String) entries.get("seriesTitle");
            description = (String) entries.get("description");
        }
        final DecimalFormat df = new DecimalFormat("00");
        final int url_season_i = Integer.parseInt(url_season);
        final int url_episode_i = Integer.parseInt(url_episode);
        if (series_title == null) {
            series_title = url_series_title.replace("-", " ");
        }
        String filename = series_title + "_S" + df.format(url_season_i) + "E" + df.format(url_episode_i) + ".mp4";
        if (description != null && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        if (!isAvailable) {
            link.getLinkStatus().setStatusText("Content is not downloadable or has not aired yet");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (!isAvailable) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Content is not downloadable or has not aired yet");
        }
        String hls_master = null;
        String url_hds = null;
        if (newAPI) {
            /*
             * 2019-09-16: New API has hls-master directly available - old API will only return hds URL which we will have to "convert" to
             * HLS-master manually.
             */
            try {
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "mediaAssetsOnDemand/{0}");
                url_hds = (String) entries.get("hdsUrl");
                hls_master = (String) entries.get("hlsUrl");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        } else {
            url_hds = PluginJSonUtils.getJsonValue(br, "mediaUrl");
        }
        /* 2019-09-16: html does not contain stream-URLs anymore! */
        // if (StringUtils.isEmpty(url_hds) || hls_master == null) {
        // this.br.getPage(link.getPluginPatternMatcher());
        // if (this.br.getHttpConnection().getResponseCode() == 404) {
        // /* 404 handling is enough to determine offline state (via API AND website)! */
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        // url_hds = this.br.getRegex("data\\-media=\"(http[^<>\"]*?)\"").getMatch(0);
        // hls_master = this.br.getRegex("data\\-hls\\-media=\"(http[^<>\"]*?)\"").getMatch(0);
        // }
        if (StringUtils.isEmpty(url_hds) && StringUtils.isEmpty(hls_master)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.isEmpty(hls_master) && !StringUtils.isEmpty(url_hds)) {
            /* This is a rare case - convert hds urls --> hls urls */
            hls_master = url_hds.replace("akamaihd.net/z/", "akamaihd.net/i/").replace("/manifest.f4m", "/master.m3u8");
        }
        this.br.getPage(hls_master);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, url_hls);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
