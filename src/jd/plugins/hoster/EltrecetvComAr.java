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

import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eltrecetv.com.ar" }, urls = { "https?://(?:www\\.)?eltrecetv\\.com\\.ar/.+\\d+/?$" })
public class EltrecetvComAr extends PluginForHost {
    public EltrecetvComAr(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/terminos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    private String hlsurl = null;

    protected String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)/?$").getMatch(0);
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        String fid = null;
        if (link != null && StringUtils.equals(getHost(), link.getHost()) && (fid = getFID(link)) != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String extDefault = ".mp4";
        final String fid = this.getFID(link);
        if (!link.isNameSet() && fid != null) {
            link.setName(fid + extDefault);
        }
        hlsurl = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String playerdata = br.getRegex("(playerId/[^/]+/contentId/\\d+)").getMatch(0);
        String title = null;
        String subtitle = br.getRegex("class=\"article__dropline\"[^>]*>([^<]+)").getMatch(0);
        if (playerdata != null) {
            br.getPage("https://api.vodgc.net/player/conf/" + playerdata);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            hlsurl = (String) entries.get("m3u8_url");
            if (StringUtils.isEmpty(hlsurl)) {
                hlsurl = (String) JavaScriptEngineFactory.walkJson(entries, "sources/{0}/src");
            }
            title = entries.get("video_name").toString();
        } else {
            /* 2021-09-15 */
            final String playerContentID = br.getRegex("data-content-id=\"(\\d+)\"").getMatch(0);
            if (playerContentID == null) {
                /* Probably no video content */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage("https://genoa-player-api.vodgc.net/content/" + playerContentID);
            /* Double-check */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            /* 2021-09-15: HTTP stream is also available but only in lower quality 480p. */
            this.hlsurl = JavaScriptEngineFactory.walkJson(entries, "sources/{0}/src").toString();
            title = entries.get("video_name").toString();
        }
        if (title != null && subtitle != null) {
            title = Encoding.htmlDecode(title).trim();
            subtitle = Encoding.htmlDecode(subtitle).trim();
            link.setFinalFileName(title + " - " + subtitle + extDefault);
        } else if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            link.setFinalFileName(title + extDefault);
        } else if (subtitle != null) {
            subtitle = Encoding.htmlDecode(subtitle).trim();
            link.setFinalFileName(subtitle + extDefault);
        } else {
            logger.warning("Failed to find videoTitle");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        download(link);
    }

    private void download(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(hlsurl)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(hlsurl);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
        dl.startDownload();
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