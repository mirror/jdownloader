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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.config.SrfChConfig;
import org.jdownloader.plugins.components.config.SrfChConfig.Quality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SrfCh extends PluginForHost {
    @SuppressWarnings("deprecation")
    public SrfCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "srf.ch" });
        ret.add(new String[] { "rts.ch" });
        ret.add(new String[] { "rsi.ch" });
        ret.add(new String[] { "rtr.ch" });
        ret.add(new String[] { "swissinfo.ch" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:audio|play)/.+\\?(?:id=[A-Za-z0-9\\-]+|urn=[a-z0-9\\-:]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "http://www.srf.ch/allgemeines/impressum";
    }

    private final String OFFICIAL_DOWNLOADURL         = "official_downloadurl";
    private final String PROPERTY_LAST_CHOSEN_QUALITY = "last_chosen_quality";
    private final String PROPERTY_URN                 = "urn";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private void setUrnAsLinkID(final DownloadLink link, final String urn) {
        link.setLinkID(this.getHost() + "://" + urn);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String json = null;
        String urn = getURN(link, false);
        if (urn != null) {
            setUrnAsLinkID(link, urn);
        } else {
            this.br.setAllowedResponseCodes(new int[] { 410 });
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            urn = getURN(this.br);
            json = br.getRegex(">\\s*window\\.__SSR_VIDEO_DATA__ = (\\{.*?\\})</script>").getMatch(0);
        }
        String officialDownloadurl = null;
        final boolean allowLegacyWebsiteHandling = false;
        if (json != null && allowLegacyWebsiteHandling) {
            /* Website/old handling */
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final Map<String, Object> videoDetail = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "initialData/videoDetail");
            String title = (String) videoDetail.get("title");
            String dateFormatted = null;
            final String date = (String) videoDetail.get("publishDate");
            if (!StringUtils.isEmpty(date)) {
                dateFormatted = new Regex(date, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            }
            if (!StringUtils.isEmpty(title)) {
                title = Encoding.htmlDecode(title.trim());
                if (dateFormatted != null) {
                    link.setFinalFileName(dateFormatted + "_" + title + ".mp4");
                } else {
                    link.setFinalFileName(title + ".mp4");
                }
            }
            final String description = (String) videoDetail.get("description");
            if (StringUtils.isEmpty(link.getComment()) && !StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
            officialDownloadurl = (String) videoDetail.get("downloadUrl");
        } else {
            /* E.g. audio items */
            if (urn == null) {
                /* Assume that content is offline or is no media content. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            setUrnAsLinkID(link, urn);
            this.accessAPI(urn);
            final Map<String, Object> root = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final List<Object> chapterList = (List<Object>) root.get("chapterList");
            if (chapterList.size() != 1) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Map<String, Object> mediaInfo = (Map<String, Object>) chapterList.get(0);
            final String title = (String) mediaInfo.get("title");
            if (title == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String dateFormatted = new Regex(mediaInfo.get("date"), "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            final String ext;
            final String mediaType = (String) mediaInfo.get("mediaType");
            if (mediaType.equalsIgnoreCase("AUDIO")) {
                ext = ".mp3";
            } else if (mediaType.equalsIgnoreCase("VIDEO")) {
                ext = ".mp4";
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported mediaType:" + mediaType);
            }
            link.setFinalFileName(dateFormatted + "_" + mediaInfo.get("vendor") + "_" + title + ext);
            String description = (String) mediaInfo.get("description");
            if (description == null) {
                description = (String) JavaScriptEngineFactory.walkJson(root, "show/description");
            }
            if (StringUtils.isEmpty(link.getComment()) && !StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
            officialDownloadurl = (String) mediaInfo.get("podcastHdUrl");
        }
        /* urn was not contained in given URL but we found it in html code --> Save it as property */
        if (getURNFromURL(link.getPluginPatternMatcher()) == null) {
            link.setProperty(PROPERTY_URN, urn);
        }
        if (!StringUtils.isEmpty(officialDownloadurl)) {
            link.setProperty(OFFICIAL_DOWNLOADURL, officialDownloadurl);
            /* Only check for filesize if user wants BEST quality anyways. */
            if (!isDownload && getUserPreferredqualityHeight(link) == null) {
                URLConnectionAdapter con = null;
                try {
                    /* 2022-05-09: HEAD request is not possible. */
                    con = br.openGetConnection(officialDownloadurl);
                    if (!this.looksLikeDownloadableContent(con)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Media file broken?");
                    } else {
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
        }
        return AvailableStatus.TRUE;
    }

    private String getURN(final DownloadLink link, final boolean allowFallback) throws MalformedURLException {
        if (link.hasProperty(PROPERTY_URN)) {
            return link.getStringProperty(PROPERTY_URN);
        } else {
            final String domainpart = new Regex(link.getPluginPatternMatcher(), "https?://(?:www\\.)?([A-Za-z0-9\\.]+)\\.ch/").getMatch(0);
            final String videoid = new Regex(link.getPluginPatternMatcher(), "\\?id=([A-Za-z0-9\\-]+)").getMatch(0);
            final String channelname = convertDomainPartToShortChannelName(domainpart);
            /* First check if urn is located in URL added by user. */
            String urn = getURNFromURL(link.getPluginPatternMatcher());
            if (StringUtils.isEmpty(urn) && allowFallback) {
                /* Final fallback e.g. for older URLs --> We have to generate that parameter on our own. */
                if (link.getPluginPatternMatcher().matches("https?://[^/]+/audio/.+")) {
                    urn = "urn:" + channelname + ":audio:" + videoid;
                } else {
                    urn = "urn:" + channelname + ":video:" + videoid;
                }
            }
            return urn;
        }
    }

    private String getURN(final Browser br) {
        return br.getRegex("data-assetid=\"(urn:[^\"]+)\"").getMatch(0);
    }

    private String getURNFromURL(final String url) throws MalformedURLException {
        if (url == null) {
            return null;
        } else {
            return UrlQuery.parse(url).get("urn");
        }
    }

    private void accessAPI(final String urn) throws IOException, PluginException {
        /* xml also possible: http://il.srgssr.ch/integrationlayer/1.0/<channelname>/srf/video/play/<videoid>.xml */
        this.br.getPage("https://il.srgssr.ch/integrationlayer/2.0/mediaComposition/byUrn/" + urn + ".json?onlyChapters=false&vector=portalplay");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String urn = this.getURN(link, true);
        if (urn == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Integer preferredQualityHeight = getUserPreferredqualityHeight(link);
        /* 2020-07-29: E.g. "blockReason" : "ENDDATE" or "blockReason" : "GEOBLOCK" */
        String blockReason = br.getRegex("geoblock\\s*?:\\s*?\\{\\s*?(?:audio|video)\\:\\s*?\"([^\"]+)\"").getMatch(0);
        if (!br.getURL().contains("/byUrn/")) {
            accessAPI(urn);
        }
        String url_http_download = null;
        String url_hls_master = null;
        if (preferredQualityHeight == null && link.hasProperty(OFFICIAL_DOWNLOADURL)) {
            /* User prefers BEST quality && official downloadurl is available --> Use that */
            url_http_download = link.getStringProperty(OFFICIAL_DOWNLOADURL);
        } else {
            final Map<String, Object> root = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            try {
                final Map<String, String> hlsMap = new HashMap<String, String>();
                final Map<String, String> httpDownloadsMap = new HashMap<String, String>();
                final List<Object> chapterList = (List<Object>) root.get("chapterList");
                if (chapterList.size() > 1) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Map<String, Object> mediaInfo = (Map<String, Object>) chapterList.get(0);
                if (StringUtils.isEmpty(blockReason)) {
                    /* 2020-07-29: New */
                    blockReason = (String) mediaInfo.get("blockReason");
                }
                // final String id = (String) entries.get("id");
                final List<Object> ressourcelist = (List<Object>) mediaInfo.get("resourceList");
                for (final Object ressourceO : ressourcelist) {
                    final Map<String, Object> resourceInfo = (Map<String, Object>) ressourceO;
                    final String protocol = (String) resourceInfo.get("protocol");
                    if (protocol.equals("HTTP") || protocol.equals("HTTPS")) {
                        final String url = (String) resourceInfo.get("url");
                        final String quality = (String) resourceInfo.get("quality");
                        if (StringUtils.isNotEmpty(url)) {
                            httpDownloadsMap.put(quality, url);
                        }
                    } else if (protocol.equals("HLS")) {
                        final String url = (String) resourceInfo.get("url");
                        final String quality = (String) resourceInfo.get("quality");
                        if (StringUtils.isNotEmpty(url)) {
                            hlsMap.put(quality, url);
                        }
                    } else {
                        /* Skip unsupported protocol */
                        logger.info("Skipping protocol: " + resourceInfo);
                        continue;
                    }
                }
                if (hlsMap.size() > 0) {
                    if (hlsMap.containsKey("HD")) {
                        url_hls_master = hlsMap.get("HD");
                    } else if (hlsMap.containsKey("SD")) {
                        url_hls_master = hlsMap.get("SD");
                    } else {
                        logger.info("unknown qualities(hls):" + hlsMap);
                        url_hls_master = hlsMap.entrySet().iterator().next().getValue();
                    }
                }
                if (httpDownloadsMap.size() > 0) {
                    if (httpDownloadsMap.containsKey("HD")) {
                        url_http_download = httpDownloadsMap.get("HD");
                    } else if (hlsMap.containsKey("SD")) {
                        url_http_download = httpDownloadsMap.get("SD");
                    } else {
                        logger.info("unknown qualities(mp4):" + httpDownloadsMap);
                        url_http_download = httpDownloadsMap.entrySet().iterator().next().getValue();
                    }
                }
                if (!StringUtils.isEmpty(url_hls_master)) {
                    /* Sign URL */
                    String acl = new Regex(url_hls_master, "https?://[^/]+(/.+\\.csmil)").getMatch(0);
                    if (acl == null) {
                        logger.warning("Failed to find acl");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    acl += "/*";
                    br.getPage("https://player.rts.ch/akahd/token?acl=" + acl);
                    final Map<String, Object> signInfoRoot = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                    final Map<String, Object> authMap = (Map<String, Object>) signInfoRoot.get("token");
                    String authparams = (String) authMap.get("authparams");
                    if (StringUtils.isEmpty(authparams)) {
                        logger.warning("Failed to find authparams");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    authparams = new Regex(authparams, "hdnts=(.+)").getMatch(0);
                    authparams = URLEncode.encodeURIComponent(authparams);
                    authparams = authparams.replace("*", "%2A");
                    url_hls_master += "&hdnts=" + authparams;
                    final String param_caption = new Regex(url_hls_master, "caption=([^\\&]+)").getMatch(0);
                    if (param_caption != null) {
                        String param_caption_new = param_caption;
                        param_caption_new = Encoding.htmlDecode(param_caption_new);
                        param_caption_new = URLEncode.encodeURIComponent(param_caption_new);
                        param_caption_new = param_caption_new.replace("%3D", "=");
                        url_hls_master = url_hls_master.replace(param_caption, param_caption_new);
                    }
                }
            } catch (final Exception e) {
                if (!StringUtils.isEmpty(blockReason)) {
                    contentBlocked(blockReason);
                } else {
                    throw e;
                }
            }
        }
        if (url_http_download == null && url_hls_master == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_http_download != null) {
            /* Prefer http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url_http_download, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (br.getHttpConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 3 * 60 * 1000l);
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
            this.dl.startDownload();
        } else {
            /* HLS download */
            /* 2019-08-09: These headers are not necessarily needed */
            logger.info("Downloading hls");
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Origin", "https://player.rts.ch");
            br.getHeaders().put("Sec-Fetch-Site", "cross-site");
            this.br.getPage(url_hls_master);
            /* 2019-08-09: This will also return error 403 for wrongly signed URLs! */
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                contentBlocked(blockReason);
            }
            // try {
            // this.br.cloneBrowser().getPage("http://il.srgssr.ch/integrationlayer/1.0/ue/" + channelname + "/video/" + videoid +
            // "/clicked.xml");
            // } catch (final Throwable e) {
            // }
            /* Pick user preferred quality */
            final List<HlsContainer> qualities = HlsContainer.getHlsQualities(this.br);
            if (qualities == null || qualities.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            HlsContainer chosenQuality = null;
            if (preferredQualityHeight == null) {
                chosenQuality = HlsContainer.findBestVideoByBandwidth(qualities);
                logger.info("Using best quality: " + chosenQuality.getHeight() + "p");
                ;
            } else {
                for (final HlsContainer quality : qualities) {
                    if (quality.getHeight() == preferredQualityHeight.intValue()) {
                        logger.info("Using user preferred quality: " + quality.getHeight() + "p");
                        chosenQuality = quality;
                        break;
                    }
                }
                if (chosenQuality == null) {
                    chosenQuality = HlsContainer.findBestVideoByBandwidth(qualities);
                    logger.info("Using best quality as fallback: " + chosenQuality.getHeight() + "p");
                }
            }
            link.setProperty(PROPERTY_LAST_CHOSEN_QUALITY, chosenQuality.getHeight());
            final String url_hls = chosenQuality.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, url_hls);
            dl.startDownload();
        }
    }

    private void contentBlocked(final String blockReason) throws PluginException {
        final String userReadableBlockedReason;
        if (StringUtils.isEmpty(blockReason)) {
            userReadableBlockedReason = "Unknown";
        } else if (blockReason.equalsIgnoreCase("ENDDATE")) {
            userReadableBlockedReason = "Content is not available anymore (expired)";
        } else if (blockReason.equalsIgnoreCase("GEOBLOCK")) {
            userReadableBlockedReason = "GEO-blocked";
        } else {
            userReadableBlockedReason = "Unknown reason:" + blockReason;
        }
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Content not downloadable because " + userReadableBlockedReason);
    }

    private Integer getUserPreferredqualityHeight(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_LAST_CHOSEN_QUALITY)) {
            return link.getIntegerProperty(PROPERTY_LAST_CHOSEN_QUALITY);
        } else {
            final Quality quality = PluginJsonConfig.get(SrfChConfig.class).getPreferredQuality();
            switch (quality) {
            case Q180:
                return 180;
            case Q270:
                return 270;
            case Q360:
                return 360;
            case Q480:
                return 480;
            case Q720:
                return 720;
            case Q1080:
                return 1080;
            default:
                /* Best quality */
                return null;
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String findBestQualityForStreamtype(Map<String, Object> temp) {
        final String[] possibleQualities = { "HD", "HQ", "MQ", "SQ", "SD" };
        String best_quality = null;
        List<Object> ressourcelist2 = (List) temp.get("url");
        for (final String possible_quality : possibleQualities) {
            for (final Object hlso : ressourcelist2) {
                temp = (Map<String, Object>) hlso;
                final String quality = (String) temp.get("@quality");
                if (quality == null) {
                    continue;
                }
                if (quality.equals(possible_quality)) {
                    /* We found the best available quality */
                    best_quality = (String) temp.get("text");
                    return best_quality;
                }
            }
        }
        return best_quality;
    }

    private String convertDomainPartToShortChannelName(final String input) {
        final String output;
        if (input.equals("play.swissinfo")) {
            output = "swi";
        } else {
            output = input;
        }
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(PROPERTY_LAST_CHOSEN_QUALITY);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return SrfChConfig.class;
    }
}