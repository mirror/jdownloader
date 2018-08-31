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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dramafever.com" }, urls = { "https?://(?:www\\.)?dramafever\\.com/[a-z]{2}/[a-z]+/\\d+/\\d+/[a-z0-9\\-]+/" })
public class DramafeverCom extends PluginForHost {
    public DramafeverCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.dramafever.com/company/about.html";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), "dramafever\\.com/[a-z]{2}/[a-z]+/(\\d+/\\d+)/").getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getSeriesID(final String url) {
        return new Regex(url, "dramafever\\.com/[a-z]{2}/[a-z]+/(\\d+)/\\d+/").getMatch(0);
    }

    private String getChapterNumber(final String url) {
        return new Regex(url, "dramafever\\.com/[a-z]{2}/[a-z]+/\\d+/(\\d+)/").getMatch(0);
    }

    private String getLanguage(final String url) {
        return new Regex(url, "dramafever\\.com/([a-z]{2})/").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final String chapter = getChapterNumber(link.getPluginPatternMatcher());
        /* Hardcoded value */
        br.getHeaders().put("x-consumer-key", "DA59dtVXYLxajktV");
        br.getPage("https://www.dramafever.com/api/5/series/" + getSeriesID(link.getPluginPatternMatcher()) + "/episodes/" + chapter + "/?trans=" + getLanguage(link.getPluginPatternMatcher()));
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String description = (String) entries.get("description");
        final String series_title = (String) entries.get("series_title");
        final String date = (String) entries.get("air_date");
        if (StringUtils.isEmpty(series_title) || StringUtils.isEmpty(date)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = date + "_" + series_title + "_cp_" + chapter + ".mp4";
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage("https://www.dramafever.com/api/5/video/" + getSeriesID(downloadLink.getPluginPatternMatcher()) + "." + getChapterNumber(downloadLink.getPluginPatternMatcher()) + "/stream/?cdn=cloudfront&subs=none&trans=" + getLanguage(downloadLink.getPluginPatternMatcher()));
        final String hls_master = PluginJSonUtils.getJson(this.br, "stream_url");
        if (StringUtils.isEmpty(hls_master)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(hls_master);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, hlsbest.getDownloadurl());
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
        return true;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}