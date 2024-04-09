//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.config.YoupornConfig;
import org.jdownloader.plugins.components.config.YoupornConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class YouPornCom extends PluginForHost {
    /* DEV NOTES */
    /* Porn_plugin */
    String                      dllink        = null;
    private boolean             server_issues = false;
    private static final String TYPE_ALL      = "(?:https?://[^/]+)?/(?:watch|embed)/(\\d+)(/([a-z0-9\\-]+)/?)?";
    private static final String defaultEXT    = ".mp4";

    public YouPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public String getAGBLink() {
        return "http://youporn.com/terms";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "youporn.com", "youpornru.com", "youporngay.com" });
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
            ret.add("https?://(?:[a-z]{2}\\.|www\\.)?" + buildHostsPatternPart(domains) + "/(?:watch|embed)/\\d+(/([a-z0-9\\-]+)/?)?");
        }
        return ret.toArray(new String[0]);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_ALL).getMatch(0);
        }
    }

    private String getURLTitle(final DownloadLink link) {
        return getURLTitleCleaned(link.getPluginPatternMatcher());
    }

    private String getURLTitleCleaned(final String url) {
        String title = new Regex(url, TYPE_ALL).getMatch(2);
        if (title != null) {
            return title.replace("-", " ").trim();
        } else {
            return null;
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        final String urlTitle = getURLTitleCleaned(link.getPluginPatternMatcher());
        if (urlTitle != null) {
            return urlTitle + defaultEXT;
        } else {
            return this.getFID(link) + defaultEXT;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        if (!link.isNameSet()) {
            link.setName(this.getWeakFilename(link));
        }
        this.dllink = null;
        this.server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String host = Browser.getHost(link.getPluginPatternMatcher());
        br.setCookie(host, "age_verified", "1");
        br.setCookie(host, "yp-device", "1");
        br.setCookie(host, "language", "en");
        br.getPage(link.getPluginPatternMatcher());
        if (br.getURL().endsWith("/video-removed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().endsWith("/video-inactive")) {
            /* 2024-04-09: "This video has been deactivated" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("<div id=\"video-not-found-related\"|watchRemoved\"|class=\\'video-not-found\\'")) {
            /* Offline link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("404 \\- Page Not Found<|id=\"title_404\"") || this.br.getHttpConnection().getResponseCode() == 404) {
            /* Invalid link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)<div class\\s*=\\s*(\"|')geo-blocked-content(\"|')>\\s*This video has been disabled") || br.getURL().contains("/video-disabled")) {
            /* 2021-01-18 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Video has been disabled / flagged for review");
        } else if (this.br.containsHTML(">\\s*This video has been removed")) {
            /* 22024-04-09 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("(?i)<div class\\s*=\\s*(\"|')geo-blocked-content(\"|')>\\s*This page is not available in your location")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "This page is not available in your location", 60 * 60 * 1000l);
        } else if (this.br.containsHTML("(?i)<div class\\s*=\\s*(\"|')geo-blocked-content(\"|')>\\s*Video has been flagged for verification")) {
            /* 2021-01-18 */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video has been flagged for verification", 3 * 60 * 60 * 1000l);
        } else if (this.br.containsHTML("(?i)class\\s*=\\s*(\"|')geo-blocked-content(\"|')>")) {
            /* 2020-07-02: New: E.g. if you go to youpornru.com with a german IP and add specific URLs (not all content is GEO-blocked!). */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "GEO-blocked", 3 * 60 * 60 * 1000l);
        } else if (this.br.containsHTML("onload=\"go\\(\\)\"")) {
            /* 2017-07-26: TODO: Maybe follow that js redirect */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Temporarily blocked because of too many requests", 5 * 60 * 1000l);
        } else if (br.containsHTML("class=\"video-not-found-header\"")) {
            /*
             * 2021-01-07: Also applies for ">Video has been flagged for verification" --> I guess most- or all of such videos' status will
             * never change (?)
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2021-11-12: E.g. "Video is unavailable pending review." */
        final String otherReasonForTempUnavailable = br.getRegex("<div class=\"video-disabled-wrapper\">\\s*<h1>([^<>\"]+)</h1>").getMatch(0);
        if (otherReasonForTempUnavailable != null) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, otherReasonForTempUnavailable, 5 * 60 * 1000l);
        }
        String title = br.getRegex("<h1 class=\"videoTitle tm_videoTitle\"[^>]*>([^<]+)</h1>").getMatch(0);
        if (title == null) {
            title = br.getRegex("videoTitle\\s*:\\s*\'([^<>\']*?)\'").getMatch(0);
        }
        if (title == null || true) {
            title = HTMLSearch.searchMetaTag(br, "og:title");
        }
        if (title == null) {
            title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        if (title == null) {
            /* Final fallback */
            title = this.getURLTitleCleaned(br.getURL());
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim().replaceAll("   ", "-");
            title = title.replaceFirst("(?i) - Free Porn Videos.*$", "");
            title = title.replaceFirst("(?i) Video - Youporn\\.com$", "");
            link.setFinalFileName(title + defaultEXT);
        }
        final String channelname = br.getRegex("class=\"submitByLink\"[^>]*>\\s*<[^>]*href=\"/(?:channel|uservids)/([^\"/]+)").getMatch(0);
        if (channelname != null) {
            /* Packagizer property */
            link.setProperty("username", Encoding.htmlDecode(channelname).trim());
        }
        if (br.getURL().contains("/private/") || br.containsHTML("for=\"privateLogin_password\"")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not yet supported, contact our support!");
        }
        /* Find highest quality */
        int qualityMax = 0;
        /* 2020-07-02: Try to obey users' selected quality in this block only */
        final String mediaDefinition = br.getRegex("(?:video\\.)?mediaDefinition\\s*[=:]\\s*(\\[.*?\\]),\\n").getMatch(0);
        String fileSizeString = null;
        if (mediaDefinition != null) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) restoreFromString(mediaDefinition, TypeRef.OBJECT);
            for (Map<String, Object> entry : list) {
                final String videoUrl = (String) entry.get("videoUrl");
                final String format = (String) entry.get("format");
                if (StringUtils.isEmpty(videoUrl)) {
                    continue;
                } else if (StringUtils.equals("mp4", format)) {
                    final Browser brc = br.cloneBrowser();
                    brc.getPage(videoUrl);
                    list = (List<Map<String, Object>>) restoreFromString(brc.toString(), TypeRef.OBJECT);
                    break;
                }
            }
            final String userPreferredQuality = getPreferredStreamQuality();
            qualityMax = 0;
            if (list != null) {
                for (Object entry : list) {
                    Map<String, Object> video = (Map<String, Object>) entry;
                    final String videoUrl = (String) video.get("videoUrl");
                    final String format = (String) video.get("format");
                    if ("hls".equals(format)) {
                        // not yet supported
                        continue;
                    }
                    final Object quality = video.get("quality");
                    if (StringUtils.isEmpty(videoUrl)) {
                        continue;
                    } else if (quality == null) {
                        continue;
                    }
                    final Number fileSize = (Number) video.get("videoSize");
                    final String qualityTempStr = (String) quality;
                    if (StringUtils.equals(qualityTempStr, userPreferredQuality)) {
                        logger.info("Found user preferred quality: " + userPreferredQuality);
                        if (fileSize != null) {
                            fileSizeString = fileSize.toString();
                            link.setDownloadSize(fileSize.longValue());
                        }
                        dllink = videoUrl;
                        break;
                    }
                    final int qualityTemp = Integer.parseInt(qualityTempStr);
                    if (qualityTemp > qualityMax) {
                        if (fileSize != null) {
                            fileSizeString = fileSize.toString();
                            link.setDownloadSize(fileSize.longValue());
                        }
                        qualityMax = qualityTemp;
                        dllink = videoUrl;
                    }
                }
            }
        }
        /* Use fallback if needed - don't care about users' selected quality! */
        if (StringUtils.isEmpty(this.dllink)) {
            /* Old handling: Must not be present */
            final String[] htmls = br.getRegex("class='callBox downloadOption[^~]*?downloadVideoLink clearfix'([^~]*?)</span>").getColumn(0);
            for (final String html : htmls) {
                final String quality = new Regex(html, "(\\d+)p_\\d+k").getMatch(0);
                if (quality == null) {
                    continue;
                }
                final int qualityTemp = Integer.parseInt(quality);
                if (qualityTemp > qualityMax) {
                    qualityMax = qualityTemp;
                    this.dllink = new Regex(html, "(https?://[^'\"]+\\d+p[^'\"]+\\.mp4[^\\'\"\\|]+)").getMatch(0);
                    if (this.dllink != null) {
                        /* Only attempt to grab filesize if it corresponds to the current videoquality! */
                        fileSizeString = new Regex(html, "class=\\'downloadsize\\'>\\((\\d+[^<>\"]+)\\)").getMatch(0);
                    }
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://[^<>\"\\']+)\">MP4").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://videos\\-\\d+\\.youporn\\.com/[^<>\"\\'/]+/save/scene_h264[^<>\"\\']+)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://cdn[a-z0-9]+\\.public\\.youporn\\.phncdn\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<ul class=\"downloadList\">.*?href=\"(https?://[^\"]+)\">.*?</ul>").getMatch(0);
        }
        if (dllink == null) {
            /**
             * 2020-05-27: Workaround/Fallback for some users who seem to get a completely different pornhub page (???) RE:
             * https://svn.jdownloader.org/issues/88346 </br>
             * This source will be lower quality than their other sources!
             */
            dllink = br.getRegex("meta name=\"twitter:player:stream\" content=\"(http[^<>\"\\']+)\"").getMatch(0);
        }
        if (dllink != null) {
            /* Do NOT htmldecode! */
            dllink = dllink.replace("&amp;", "&");
        }
        if (fileSizeString != null) {
            link.setDownloadSize(SizeFormatter.getSize(fileSizeString));
        } else if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        if (con.isContentDecoded()) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        } else {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    }
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
        }
        dl.startDownload();
    }

    private String getPreferredStreamQuality() {
        final YoupornConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        case BEST:
        default:
            return null;
        case Q2160P:
            return "2160";
        case Q1080P:
            return "1080";
        case Q720P:
            return "720";
        case Q480P:
            return "480";
        case Q360P:
            return "360";
        case Q240P:
            return "240";
        }
    }

    @Override
    public Class<? extends YoupornConfig> getConfigInterface() {
        return YoupornConfig.class;
    }

    public void reset() {
    }

    public void resetDownloadlink(final DownloadLink link) {
    }
}