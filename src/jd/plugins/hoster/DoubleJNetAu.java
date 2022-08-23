//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "doublej.net.au" }, urls = { "https?://(?:www\\.)?doublej\\.net\\.au/programs/[a-z0-9\\-]+/.+|https://(?:www\\.)?abc\\.net\\.au/doublej/programs/[a-z0-9\\-]+/[a-z0-9\\-]+/\\d+" })
public class DoubleJNetAu extends PluginForHost {
    // raztoki embed video player template.
    private Browser ajax = null;
    private Browser m3u  = null;

    /**
     * @author raztoki
     */
    public DoubleJNetAu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.doublej.net.au/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String dllinkHTTP = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Opera/9.80 (Windows NT 6.1; Win64; x64) Presto/2.12.388 Version/12.17");
        // first get
        br.getPage(link.getPluginPatternMatcher());
        final String aacURL = br.getRegex("\"url\":\"(https?://[^\"]+\\.aac)").getMatch(0);
        if (aacURL != null) {
            dllinkHTTP = aacURL;
            link.setFinalFileName(br._getURL().getPath() + ".aac");
        } else {
            String contentID = br.getRegex("-\\s*On-demand\\s*\\(([A-Za-z0-9]+)\\)</a></h2></div>").getMatch(0);
            if (contentID == null) {
                /* 2022-02-08 */
                contentID = br.getRegex("\"arid\": \"papi:([A-Za-z0-9]+)\"").getMatch(0);
            }
            if (contentID == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setLinkID(this.getHost() + "://" + contentID);
            // get associated json crapola
            // getAjax("http://program.abcradio.net.au/api/v1/on_demand/" + contentID + ".json");
            getAjax("https://program.abcradio.net.au/api/v1/programitems/" + contentID + ".json");
            final Map<String, Object> root = JSonStorage.restoreFromString(ajax.toString(), TypeRef.HASHMAP);
            dllinkHTTP = br.getRegex("(?i)Try to\\s*<a href=\"(https?://[^<>\"]+\\.mp3)\"").getMatch(0);
            link.setFinalFileName(root.get("title").toString() + ".m4a");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllinkHTTP != null) {
            /* Download http stream */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllinkHTTP, true, 1);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            /* Download HLS stream */
            checkFFmpeg(link, "Download a HLS Stream");
            final String hlsMaster = ajax.getRegex("\"(https?://[^\"]+\\.m3u8)").getMatch(0);
            getM3u(hlsMaster);
            final HlsContainer best = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(m3u));
            dl = new HLSDownloader(link, br, best.getDownloadurl());
            dl.startDownload();
        }
    }

    private void getAjax(final String url) throws IOException {
        // no cookie session
        ajax = new Browser();
        ajax.getHeaders().put("User-Agent", br.getHeaders().get("User-Agent"));
        ajax.getHeaders().put("Accept-Language", "en-AU,en;q=0.9");
        ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        ajax.getHeaders().put("Origin", "http://doublej.net.au");
        ajax.getHeaders().put("Accept-Charset", null);
        ajax.getHeaders().put("Cache-Control", null);
        ajax.getHeaders().put("Pragma", null);
        ajax.getPage(url);
    }

    private void getM3u(String url) throws IOException {
        // cookie _alid_ gets set on first request.
        if (m3u == null) {
            m3u = new Browser();
        }
        m3u.getHeaders().put("User-Agent", br.getHeaders().get("User-Agent"));
        m3u.getHeaders().put("Accept-Language", "en-AU,en;q=0.9");
        m3u.getHeaders().put("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/webp, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
        m3u.getHeaders().put("Referer", "http://www.abc.net.au/radio/player/beta/scripts/vendor/jwplayer/jwplayer.flash.swf");
        m3u.getHeaders().put("Accept-Charset", null);
        m3u.getHeaders().put("Cache-Control", null);
        m3u.getHeaders().put("Pragma", null);
        m3u.getPage(url);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}