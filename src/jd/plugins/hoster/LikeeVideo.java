//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class LikeeVideo extends PluginForHost {
    public LikeeVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "likee.video" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            String regex = "https?://l\\." + buildHostsPatternPart(domains) + "/v/[A-Za-z0-9]+";
            regex += "|https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(@[^/]+/video/\\d+|trending\\?postId=\\d+.*)";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean       free_resume       = true;
    private final int           free_maxchunks    = 0;
    private final int           free_maxdownloads = -1;
    private String              dllink            = null;
    private static final String PROPERTY_VIDEO_ID = "videoid";
    private static final String PROPERTY_DATE     = "date";
    private static final String PROPERTY_TITLE    = "title";
    private static String       PROPERTY_USERNAME = "username";
    private final String        TYPE_1            = "https://l\\.[^/]+/v/([A-Za-z0-9]+)";
    private final String        TYPE_2            = "https?://[^/]+/@([^/]+)/video/(\\d+)";
    private final String        TYPE_3            = "https?://[^/]+/trending\\?postId=(\\d+).*";

    @Override
    public String getAGBLink() {
        return "https://likee.com/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getVideoID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getVideoID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.hasProperty(PROPERTY_VIDEO_ID)) {
            return link.getStringProperty(PROPERTY_VIDEO_ID);
        } else if (link.getPluginPatternMatcher().matches(TYPE_2)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_2).getMatch(1);
        } else if (link.getPluginPatternMatcher().matches(TYPE_3)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_3).getMatch(0);
        } else {
            /* Unknown pattern */
            return null;
        }
    }

    /** Website similar to tiktok.com */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        String videoID = this.getVideoID(link);
        if (!link.isNameSet() && videoID != null) {
            /* Fallback */
            link.setName(this.getVideoID(link) + ".mp4");
        }
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (videoID == null) {
            /* E.g. shortlinks like l.likee.video/v/blabla */
            br.getPage(link.getPluginPatternMatcher());
            videoID = UrlQuery.parse(br.getURL()).get("postId");
            if (videoID == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Save so next time we can skip this step. */
            link.setProperty(PROPERTY_VIDEO_ID, videoID);
        }
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Accept", "application/json, text/plain, */*");
        brc.getHeaders().put("Content-Type", "application/json");
        brc.getHeaders().put("Origin", "https://" + this.getHost());
        brc.getHeaders().put("Referer", "https://" + this.getHost() + "/");
        brc.postPageRaw("https://api.like-video.com/likee-activity-flow-micro/videoApi/getVideoInfo", "{\"postIds\":\"" + videoID + "\"}");
        // br.getPage(link.getPluginPatternMatcher());
        if (brc.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        final Map<String, Object> videoInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/videoList/{0}");
        link.setProperty(PROPERTY_TITLE, videoInfo.get("title"));
        link.setProperty(PROPERTY_VIDEO_ID, videoInfo.get("postId"));
        link.setProperty(PROPERTY_USERNAME, videoInfo.get("nickname"));
        link.setProperty(PROPERTY_DATE, new SimpleDateFormat("yyyy-dd-MM").format(new Date(((Number) videoInfo.get("postTime")).longValue() * 1000)));
        this.dllink = videoInfo.get("videoUrl").toString();
        /* We want to have the video without watermark */
        this.dllink = this.dllink.replaceFirst("_4.mp4", ".mp4");
        setFilename(link);
        if (!StringUtils.isEmpty(dllink) && !link.isSizeSet()) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private static void setFilename(final DownloadLink link) {
        String filename = link.getStringProperty(PROPERTY_DATE) + "_@" + link.getStringProperty(PROPERTY_USERNAME) + " - " + link.getStringProperty(PROPERTY_TITLE);
        // String dateFormatted = getDateFormatted(link);
        // if (dateFormatted != null) {
        // filename = dateFormatted;
        // }
        filename += ".mp4";
        link.setFinalFileName(filename);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws Exception {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
            }
        }
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
