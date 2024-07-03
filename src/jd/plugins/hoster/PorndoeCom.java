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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.config.PorndoeComConfig;
import org.jdownloader.plugins.components.config.PorndoeComConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porndoe.com" }, urls = { "https?://(?:[a-z]{2}\\.)?porndoe\\.com/(?:video(?:/embed)?/(\\d+)|watch)(/[a-z0-9\\-]+)?" })
public class PorndoeCom extends PluginForHost {
    public PorndoeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://porndoe.com/terms-and-conditions";
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replace("/embed/", "/");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        br.getPage(getContentURL(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains("/video/") && !br.getURL().contains("/watch/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("/deleted-scenes/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(this.br.getURL(), "(?i)/(?:watch|video)/(.+)").getMatch(0);
        String filename = br.getRegex("<h1.*?>\\s*([^<>]+)\\s*</h1>").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = URLEncode.decodeURIComponent(url_filename);
        } else {
            filename = Encoding.htmlDecode(filename);
        }
        final String videoid = PluginJSonUtils.getJson(br, "id");
        if (!StringUtils.isEmpty(videoid)) {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Accept", "application/json, text/plain, */*");
            brc.getHeaders().put("Referer", br.getURL());
            brc.getPage("/service/index?device=desktop&page=video&id=" + videoid);
            /* Find highest quality */
            int quality_height_max = 0;
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> sources = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "payload/player/sources");
            if (sources != null) {
                final List<Map<String, Object>> mp4Sources = (List<Map<String, Object>>) sources.get("mp4");
                final String preferredQualityHeightStr = getPreferredStreamQuality();
                final Integer preferredQualityHeight = preferredQualityHeightStr != null ? Integer.parseInt(preferredQualityHeightStr) : null;
                for (final Map<String, Object> mp4Source : mp4Sources) {
                    final int qualityHeight = ((Number) mp4Source.get("height")).intValue();
                    final String dllinkTmp = (String) mp4Source.get("link");
                    if (StringUtils.isEmpty(dllinkTmp) || !dllinkTmp.contains(".mp4")) {
                        /* E.g. skip ad-URLs like: "/signup?utm_campaign=porndoe&utm_medium=desktop&utm_source=player_1080p" */
                        continue;
                    } else if (preferredQualityHeight != null && preferredQualityHeight == qualityHeight) {
                        logger.info("Found preferred quality: " + preferredQualityHeightStr);
                        this.dllink = dllinkTmp;
                        break;
                    } else {
                        if (qualityHeight > quality_height_max) {
                            quality_height_max = qualityHeight;
                            this.dllink = dllinkTmp;
                        }
                    }
                }
            } else {
                logger.warning("Failed to find video sources");
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.trim();
        final String ext;
        if (!StringUtils.isEmpty(dllink)) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, filename, ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private String getPreferredStreamQuality() {
        final PorndoeComConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        case BEST:
        default:
            return null;
        case Q1080P:
            return "1080";
        case Q720P:
            return "720";
        case Q480P:
            return "480";
        case Q360P: // Not available, get next higher
            return "480";
        case Q240P:
            return "240";
        }
    }

    @Override
    public Class<? extends PorndoeComConfig> getConfigInterface() {
        return PorndoeComConfig.class;
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
