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

import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videomore.ru" }, urls = { "https?://(?:www\\.)?videomore\\.ru/(?:video/tracks/\\d+|[^/]+/[^/]+/[^/]+)" })
public class VideomoreRu extends PluginForHost {
    public VideomoreRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://videomore.ru/";
    }

    private String hls_url = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String videoid = new Regex(link.getPluginPatternMatcher(), "/tracks/(\\d+)").getMatch(0);
        if (videoid == null) {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            videoid = br.getRegex("videomore\\.ru/video/tracks/(\\d+)\\.xml").getMatch(0);
            if (videoid == null) {
                /* Probably not a video */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        // final boolean use_oembed = true;
        // if (use_oembed) {
        // br.getPage("https://" + this.getHost() + "/video/oembed/track.xml?id=" + videoid);
        // if (br.getHttpConnection().getResponseCode() == 404) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        // title = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        // }
        link.setLinkID(videoid);
        br.getPage("https://player." + this.getHost() + "/?partner_id=97&track_id=" + videoid + "&autoplay=1&userToken=");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("var data_json\\s*?=\\s*?(\\{.*?\\});").getMatch(0);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/playlist/items/{0}");
        final String project_name = (String) entries.get("project_name");
        final String episode_name = (String) entries.get("episode_name");
        hls_url = (String) entries.get("hls_url");
        if (StringUtils.isEmpty(hls_url)) {
            /* Probably paid series OR we're GEO-blocked - we can only download the preview */
            hls_url = (String) JavaScriptEngineFactory.walkJson(entries, "previews_hls/{0}");
        }
        String filename = null;
        if (!StringUtils.isEmpty(project_name) && !StringUtils.isEmpty(episode_name)) {
            filename = project_name + " - " + episode_name;
        } else {
            filename = videoid;
        }
        filename += ".mp4";
        link.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (StringUtils.isEmpty(hls_url)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(hls_url);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, url_hls);
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
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}