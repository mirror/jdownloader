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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bibeltv.de" }, urls = { "https?://(?:www\\.)?bibeltv\\.de/mediathek/(videos/crn/(\\d+)|videos/([a-z0-9\\-]+-(\\d+)|(\\d+)-[a-z0-9\\-]+))" })
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
    private String               dllink            = null;
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
        final Pattern pat = this.getSupportedLinks();
        final Matcher m = pat.matcher(url);
        if (!m.find()) {
            return null;
        }
        String fid = null;
        final int c = m.groupCount();
        for (int i = 0; i <= c; i++) {
            final String match = m.group(i);
            if (match == null) {
                continue;
            }
            if (match.matches("\\d+")) {
                fid = match;
                break;
            }
        }
        return fid;
    }

    private static final String TYPE_REDIRECT         = "https?://[^/]+/mediathek/videos/crn/(\\d+)";
    private static final String TYPE_FID_AT_BEGINNING = "https?://[^/]+/mediathek/videos/(\\d+).*";
    private static final String TYPE_FID_AT_END       = "https?://[^/]+/mediathek/videos/[a-z0-9\\-]+-(\\d+)$";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        /* This website contains video content ONLY! */
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        final String fid;
        final boolean useCRNURL;
        if (link.getPluginPatternMatcher().matches(TYPE_REDIRECT)) {
            /* ID inside URL will work fine for "crn" API request. */
            fid = this.getFID(link);
            useCRNURL = true;
        } else if (link.getPluginPatternMatcher().matches(TYPE_FID_AT_BEGINNING)) {
            useCRNURL = true;
            fid = this.getFID(link);
        } else {
            // TYPE_FID_AT_END
            useCRNURL = false;
            fid = this.getFID(link);
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
        if (useCRNURL) {
            br.getPage("https://www.bibeltv.de/mediathek/api/videodetails/videos?q=contains(crn,%22" + fid + "%22)&expand=");
        } else {
            br.getPage(String.format("https://www.bibeltv.de/mediathek/api/videodetails/videos?q=contains(api_id,%s)&expand=", fid));
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        LinkedHashMap<String, Object> entries = null;
        try {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        } catch (final Throwable e) {
            /* 2019-12-17: No parsable json --> Offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "items/{0}");
        String filename = (String) entries.get("name");
        final String description = (String) entries.get("descriptionLong");
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = getFID(link);
        }
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        /* 2019-12-18: They provide HLS, DASH and http(highest quality only) */
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("urls");
        long max_width = 0;
        long max_width_temp = 0;
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String dllink_tmp = (String) entries.get("url");
            max_width_temp = JavaScriptEngineFactory.toLong(entries.get("width"), 0);
            final String type = (String) entries.get("type");
            if (StringUtils.isEmpty(dllink_tmp) || max_width_temp == 0 || !"video/mp4".equals(type)) {
                /* Skip invalid items and only grab http streams, ignore e.g. DASH streams. */
                continue;
            }
            if (max_width_temp > max_width) {
                dllink = dllink_tmp;
                max_width = max_width_temp;
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (tempunavailable) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video not available at the moment", 24 * 60 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
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
