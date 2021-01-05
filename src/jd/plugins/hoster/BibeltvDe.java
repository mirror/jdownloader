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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bibeltv.de" }, urls = { "https?://(?:www\\.)?bibeltv\\.de/mediathek/(videos/crn/\\d+|videos/([a-z0-9\\-]+-\\d+|\\d+-[a-z0-9\\-]+))" })
public class BibeltvDe extends PluginForHost {
    public BibeltvDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: kaltura player, medianac, api.medianac.com */
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               mp4URL            = null;
    private String               hlsURL            = null;
    private boolean              tempunavailable   = false;

    @Override
    public String getAGBLink() {
        return "https://www.bibeltv.de/impressum/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return getFID(link.getPluginPatternMatcher());
    }

    private String getFID(final String url) {
        if (url == null) {
            return null;
        }
        final String fid;
        if (url.matches(TYPE_REDIRECT)) {
            fid = new Regex(url, TYPE_REDIRECT).getMatch(0);
        } else if (url.matches(TYPE_FID_AT_BEGINNING)) {
            fid = new Regex(url, TYPE_FID_AT_BEGINNING).getMatch(0);
        } else {
            /* TYPE_FID_AT_END */
            fid = new Regex(url, TYPE_FID_AT_END).getMatch(0);
        }
        return fid;
    }

    private static final String TYPE_REDIRECT         = "https?://[^/]+/mediathek/videos/crn/(\\d+)";
    private static final String TYPE_FID_AT_BEGINNING = "https?://[^/]+/mediathek/videos/(\\d{3,}).*";
    private static final String TYPE_FID_AT_END       = "https?://[^/]+/mediathek/videos/[a-z0-9\\-]+-(\\d{3,})$";
    private Map<String, Object> entries               = null;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* This website contains video content ONLY! */
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        final boolean useCRNURL;
        if (link.getPluginPatternMatcher().matches(TYPE_REDIRECT)) {
            /* ID inside URL will work fine for "crn" API request. */
            useCRNURL = true;
        } else if (link.getPluginPatternMatcher().matches(TYPE_FID_AT_BEGINNING)) {
            useCRNURL = true;
        } else {
            // TYPE_FID_AT_END
            useCRNURL = false;
            /*
             * 2020-09-18: We need to access the original URL once because the IDs in it may change. We need the ID inside the final URL to
             * use it as a video-ID for API access!
             */
            // br.getPage(link.getPluginPatternMatcher());
            // if (!new Regex(br.getURL(), this.getSupportedLinks()).matches()) {
            // logger.info("Redirect to unsupported URL --> Content is probably not downloadable");
            // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // }
            // fid = this.getFID(br.getURL());
            // if (new Regex(link.getPluginPatternMatcher(), TYPE_REDIRECT).matches()) {
            // /* Special handling for URLs which contain IDs that cannot be used via API! */
            // br.getPage(link.getPluginPatternMatcher());
            // if (br.getURL().matches(TYPE_REDIRECT) || !new Regex(br.getURL(), this.getSupportedLinks()).matches()) {
            // logger.info("Redirect to unsupported URL --> Content is probably not downloadable");
            // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // } else {
            // /* Set new URL which contains fileID which can be used via API. */
            // link.setPluginPatternMatcher(br.getURL());
            // }
            // }
        }
        final String fid = this.getFID(link);
        if (fid == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (useCRNURL) {
            br.getPage("https://www.bibeltv.de/mediathek/api/videodetails/videos?q=contains(crn,%22" + fid + "%22)&expand=");
        } else {
            br.getPage(String.format("https://www.bibeltv.de/mediathek/api/videodetails/videos?q=contains(api_id,%s)&expand=", fid));
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        try {
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        } catch (final Throwable e) {
            /* 2019-12-17: No parsable json --> Offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "items/{0}");
        String filename = (String) entries.get("name");
        final String description = (String) entries.get("descriptionLong");
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = getFID(link);
        }
        link.setFinalFileName(filename + ".mp4");
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        try {
            /* 2019-12-18: They provide HLS, DASH and http(highest quality only) */
            final List<Object> ressourcelist = (List) entries.get("urls");
            long max_width = 0;
            for (final Object videoo : ressourcelist) {
                final Map<String, Object> entry = (Map<String, Object>) videoo;
                final String type = (String) entry.get("type");
                final String url = (String) entry.get("url");
                if (StringUtils.equals("application/x-mpegURL", type)) {
                    if (false) {
                        // split audio/video, not yet supported
                        hlsURL = url;
                    }
                } else {
                    final long max_width_temp = JavaScriptEngineFactory.toLong(entry.get("width"), 0);
                    if (StringUtils.isEmpty(url) || max_width_temp == 0 || !"video/mp4".equals(type)) {
                        logger.info("Unsupported:" + url + "|" + type);
                        /* Skip invalid items and only grab http streams, ignore e.g. DASH streams. */
                        continue;
                    } else {
                        if (max_width_temp > max_width) {
                            mp4URL = url;
                            max_width = max_width_temp;
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            logger.warning("Failed to find downloadurl");
        }
        if (!StringUtils.isEmpty(mp4URL)) {
            mp4URL = Encoding.htmlDecode(mp4URL);
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(mp4URL);
                if (looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    }
                    return AvailableStatus.TRUE;
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        final Object drm = JavaScriptEngineFactory.walkJson(entries, "drm");
        if (StringUtils.isEmpty(mp4URL) && drm != null && StringUtils.equalsIgnoreCase("true", drm.toString())) {
            link.setFinalFileName("DRM protected_" + filename + ".mp4");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "DRM protected");
        } else if (StringUtils.isAllEmpty(hlsURL, mp4URL)) {
            String failureReason = (String) JavaScriptEngineFactory.walkJson(entries, "error/message");
            if (StringUtils.isEmpty(failureReason)) {
                failureReason = "Unknown error";
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, failureReason);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (tempunavailable) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video not available at the moment", 24 * 60 * 60 * 1000l);
        }
        if (mp4URL != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, mp4URL, free_resume, free_maxchunks);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } else if (hlsURL != null) {
            if (true) {
                // split audio/video
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            brc.getPage(hlsURL);
            final HlsContainer hlsBest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(brc));
            if (hlsBest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, hlsBest.getM3U8URL());
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KalturaVideoPlatform;
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
