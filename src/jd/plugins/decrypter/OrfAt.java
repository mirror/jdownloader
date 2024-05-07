package jd.plugins.decrypter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.ORFMediathek;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class OrfAt extends PluginForDecrypt {
    public OrfAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void init() {
        super.init();
        this.cfg = SubConfiguration.getConfig("orf.at");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING, LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "orf.at" });
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
            String pattern = "https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/.+";
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    private final String                                      PATTERN_TYPE_OLD      = "(?i)https?://tvthek\\.orf\\.at/(?:index\\.php/)?(?:programs?|topic|profile)/.+";
    private final String                                      PATTERN_BROADCAST_OLD = "(?i)https?://([a-z0-9]+)\\.orf\\.at/(?:player|programm)/(\\d+)/([a-zA-Z0-9]+).*";
    private final String                                      PATTERN_BROADCAST_NEW = "(?i)https?://radiothek\\.orf\\.at/([a-z0-9]+)/(\\d+)/([a-zA-Z0-9]+).*";
    private final String                                      PATTERN_BROADCAST_2   = "(?i)https://[^/]+/radio/([a-z0-9]+)/sendung/(\\d+)/([\\w\\-]+).*";
    private final String                                      PATTERN_ARTICLE       = "(?i)https?://([a-z0-9]+)\\.orf\\.at/artikel/(\\d+)/([a-zA-Z0-9]+).*";
    private final String                                      PATTERN_PODCAST       = "(?i)https?://[^/]+/podcasts?/([a-z0-9]+)/([A-Za-z0-9\\-]+)(/([a-z0-9\\-]+))?.*";
    private final String                                      PATTERN_COLLECTION    = "(?i)^(https?://.*(?:/collection|podcast/highlights))/(\\d+)(/(\\d+)(/[a-z0-9\\-]+)?)?.*";
    private final String                                      PATTERN_VIDEO         = "(?i)^https?://[^/]+/program/(\\w+)/(\\w+)\\.html.*";
    private final String                                      API_BASE              = "https://audioapi.orf.at";
    private final String                                      PROPERTY_SLUG         = "slug";
    /* E.g. https://radiothek.orf.at/ooe --> "ooe" --> Channel == "oe2o" */
    private static LinkedHashMap<String, Map<String, Object>> CHANNEL_CACHE         = new LinkedHashMap<String, Map<String, Object>>() {
                                                                                        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                                                                                            return size() > 50;
                                                                                        };
                                                                                    };
    public SubConfiguration                                   cfg                   = null;

    /** Wrapper for podcast URLs containing md5 file-hashes inside URL. */
    protected DownloadLink createPodcastDownloadlink(final String directurl) throws MalformedURLException {
        final DownloadLink link = this.createDownloadlink(directurl, true);
        final UrlQuery query = UrlQuery.parse(directurl);
        final String md5Hash = query.get("etag");
        if (md5Hash != null) {
            link.setMD5Hash(md5Hash);
        }
        return link;
    }

    @Override
    protected DownloadLink createDownloadlink(final String directurl) {
        final DownloadLink link = super.createDownloadlink(directurl);
        link.setProperty(DirectHTTP.PROPERTY_REQUEST_TYPE, "GET");
        return link;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        Regex broadcastOld = null;
        Regex broadcastNew = null;
        Regex broadcast2 = null;
        if (param.getCryptedUrl().matches(PATTERN_TYPE_OLD)) {
            return this.crawlOrfmediathekOld(param);
        } else if (param.getCryptedUrl().matches("(?i)https?://on\\.orf\\.at/.+")) {
            return this.crawlOrfmediathekNew(param, param.getCryptedUrl());
        } else if (param.getCryptedUrl().matches(PATTERN_ARTICLE)) {
            return this.crawlArticle(param);
        } else if (param.getCryptedUrl().matches(PATTERN_COLLECTION)) {
            return this.crawlCollection(param);
        } else if (param.getCryptedUrl().matches(PATTERN_PODCAST)) {
            return this.crawlPodcast(param);
        } else if ((broadcastOld = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_OLD)).patternFind()) {
            final String broadCastID = broadcastOld.getMatch(2);
            final String broadcastDay = broadcastOld.getMatch(1);
            final String domainID = broadcastOld.getMatch(0);
            return crawlProgramm(domainID, broadCastID, broadcastDay);
        } else if ((broadcastNew = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_NEW)).patternFind()) {
            final String broadCastID = broadcastNew.getMatch(2);
            final String broadcastDay = broadcastNew.getMatch(1);
            final String domainID = broadcastNew.getMatch(0);
            return crawlProgramm(domainID, broadCastID, broadcastDay);
        } else if ((broadcast2 = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_2)).patternFind()) {
            final String domainID = broadcast2.getMatch(0);
            final String broadcastID = broadcast2.getMatch(1);
            return crawlBroadcast(domainID, broadcastID);
        } else if (param.getCryptedUrl().matches(PATTERN_VIDEO)) {
            return crawlVideo(param);
        } else {
            /* Unsupported URL -> Developer mistake */
            logger.info("Unsupported URL: " + param);
            return new ArrayList<DownloadLink>(0);
        }
    }

    private ArrayList<DownloadLink> crawlOrfmediathekNewEncryptedID(final String encryptedID, String sourceurl) throws Exception {
        if (StringUtils.isEmpty(encryptedID)) {
            throw new IllegalArgumentException();
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage("https://api-tvthek.orf.at/api/v4.3/public/episode/encrypted/" + Encoding.urlEncode(encryptedID));
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* E.g. {"error":{"code":404,"message":"Not Found"}} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> _embedded = (Map<String, Object>) entries.get("_embedded");
        final String contentIDSlashPlaylistIDSlashVideoID = entries.get("id").toString();
        final List<Map<String, Object>> segments = (List<Map<String, Object>>) _embedded.get("segments");
        final String dateStr = entries.get("date").toString();
        final String dateWithoutTime = new Regex(dateStr, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String mainVideoTitle = entries.get("title").toString();
        final String description = (String) entries.get("description");
        // final Boolean is_drm_protected = (Boolean) entries.get("is_drm_protected");
        // if (Boolean.TRUE.equals(is_drm_protected)) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "DRM protected");
        // }
        if (sourceurl == null) {
            sourceurl = (String) entries.get("share_body");
            if (StringUtils.isEmpty(sourceurl)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(dateWithoutTime + " - " + mainVideoTitle);
        if (description != null) {
            fp.setComment(description);
        }
        fp.setPackageKey("orfmediathek://video/" + contentIDSlashPlaylistIDSlashVideoID);
        final boolean has_active_youth_protection = ((Boolean) entries.get("has_active_youth_protection")).booleanValue();
        final SubConfiguration cfg = SubConfiguration.getConfig("orf.at");
        final ORFMediathek hosterplugin = (ORFMediathek) this.getNewPluginForHostInstance("orf.at");
        final Map<String, Long> qualityIdentifierToFilesizeMapGLOBAL = new HashMap<String, Long>();
        final List<String> selectedQualities = new ArrayList<String>();
        /* Collect user desired video qualities. */
        if (cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_VERYLOW, ORFMediathek.Q_VERYLOW_default)) {
            selectedQualities.add("VERYLOW");
        }
        if (cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_LOW, ORFMediathek.Q_LOW_default)) {
            selectedQualities.add("LOW");
        }
        if (cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_MEDIUM, ORFMediathek.Q_MEDIUM_default)) {
            selectedQualities.add("MEDIUM");
        }
        if (cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_HIGH, ORFMediathek.Q_HIGH_default)) {
            selectedQualities.add("HIGH");
        }
        if (cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_VERYHIGH, ORFMediathek.Q_VERYHIGH_default)) {
            selectedQualities.add("VERYHIGH");
        }
        final String subtitle_ext;
        final String subtitle_key_name;
        final int subtitleFormatSettingInt = cfg.getIntegerProperty(ORFMediathek.SETTING_SELECTED_SUBTITLE_FORMAT, ORFMediathek.SETTING_SELECTED_SUBTITLE_FORMAT_default);
        if (subtitleFormatSettingInt == 0) {
            subtitle_ext = ".smi";
            subtitle_key_name = "sami_url";
        } else if (subtitleFormatSettingInt == 1) {
            subtitle_ext = ".srt";
            subtitle_key_name = "srt_url";
        } else if (subtitleFormatSettingInt == 2) {
            subtitle_ext = ".ttml";
            subtitle_key_name = "ttml_url";
        } else if (subtitleFormatSettingInt == 3) {
            subtitle_ext = ".vtt";
            subtitle_key_name = "vtt_url";
        } else {
            subtitle_ext = ".xml";
            subtitle_key_name = "xml_url";
        }
        final Map<String, Object> gapless_subtitlemap = (Map<String, Object>) _embedded.get("subtitle");
        final String gapless_subtitleurl = gapless_subtitlemap != null ? (String) gapless_subtitlemap.get(subtitle_key_name) : null;
        String thumbnailurlFromFirstSegment = null;
        final boolean settingPreferBestVideo = cfg != null ? cfg.getBooleanProperty(ORFMediathek.Q_BEST, ORFMediathek.Q_BEST_default) : false;
        final boolean settingEnableFastCrawl = cfg != null ? cfg.getBooleanProperty(ORFMediathek.SETTING_ENABLE_FAST_CRAWL, ORFMediathek.SETTING_ENABLE_FAST_CRAWL_default) : true;
        boolean isProgressiveStreamAvailable = false;
        if (segments != null) {
            final List<String> selectedDeliveryTypes = new ArrayList<String>();
            final boolean enforceProgressive = true;
            final boolean allowProgressive = cfg != null ? cfg.getBooleanProperty(ORFMediathek.PROGRESSIVE_STREAM, ORFMediathek.PROGRESSIVE_STREAM_default) : true;
            final boolean hdsServersideBroken = true; // 2024-02-20: https://board.jdownloader.org/showthread.php?t=95259
            final boolean allow_HDS = cfg != null ? cfg.getBooleanProperty(ORFMediathek.HDS_STREAM, ORFMediathek.HDS_STREAM_default) : true;
            final boolean allow_HLS = cfg != null ? cfg.getBooleanProperty(ORFMediathek.HLS_STREAM, ORFMediathek.HLS_STREAM_default) : true;
            if (allow_HDS && !hdsServersideBroken) {
                selectedDeliveryTypes.add("hds");
            }
            if (allow_HLS) {
                selectedDeliveryTypes.add("hls");
            }
            if (allowProgressive || enforceProgressive) {
                selectedDeliveryTypes.add("progressive");
            }
            int videoPosition = 0;
            for (final Map<String, Object> segment : segments) {
                videoPosition++;
                final String segmentID = segment.get("id").toString();
                String thumbnailurl = null;
                final Map<String, Object> thisEmbedded = (Map<String, Object>) segment.get("_embedded");
                final Map<String, Object> thisplaylist = (Map<String, Object>) thisEmbedded.get("playlist");
                thumbnailurl = (String) JavaScriptEngineFactory.walkJson(segment, "_embedded/image/public_urls/highlight_teaser/url");
                if (thumbnailurlFromFirstSegment == null) {
                    thumbnailurlFromFirstSegment = thumbnailurl;
                }
                final List<Map<String, Object>> sources = (List<Map<String, Object>>) thisplaylist.get("sources");
                final List<Map<String, Object>> subtitlemaps = (List<Map<String, Object>>) thisplaylist.get("subtitles");
                String subtitleurl = null;
                if (subtitlemaps != null && subtitlemaps.size() > 0) {
                    /* Look for subtitle in user preferred format */
                    for (final Map<String, Object> subtitlemap : subtitlemaps) {
                        final String url = subtitlemap.get("src").toString();
                        final String type = subtitlemap.get("type").toString();
                        if (StringUtils.endsWithCaseInsensitive(url, subtitle_ext) || type.equalsIgnoreCase(subtitle_ext.replace(".", ""))) {
                            subtitleurl = url;
                            break;
                        }
                    }
                    if (subtitleurl == null) {
                        logger.warning("Selected subtitle format hasn't been found -> Looks like the assumption that all formats are always available is wrong");
                    }
                }
                if (subtitleurl == null && segments.size() == 1 && gapless_subtitleurl != null) {
                    /* Only one segment -> We can use gapless subtitle as fallback */
                    subtitleurl = gapless_subtitleurl;
                }
                final String titlethis = thisplaylist.get("title").toString();
                int numberofSkippedDRMItems = 0;
                final Map<String, Long> qualityIdentifierToFilesizeMap = new HashMap<String, Long>();
                final List<DownloadLink> videoresults = new ArrayList<DownloadLink>();
                final HashSet<String> allAvailableQualitiesAsHumanReadableIdentifiers = new HashSet<String>();
                for (final Map<String, Object> source : sources) {
                    final String url_directlink_video = (String) source.get("src");
                    final String fmt = (String) source.get("quality");
                    final String protocol = (String) source.get("protocol");
                    final String delivery = (String) source.get("delivery");
                    // final String subtitleUrl = (String) entry_source.get("SubTitleUrl");
                    if (StringUtils.equals(fmt, "QXADRM")) {
                        numberofSkippedDRMItems++;
                        continue;
                    }
                    /* possible protocols: http, rtmp, rtsp, hds, hls */
                    if (!"http".equals(protocol)) {
                        logger.info("skip protocol:" + protocol);
                        continue;
                    }
                    if ("dash".equals(delivery)) {
                        /* 2021-04-06 unsupported */
                        logger.info("skip delivery:" + delivery);
                        continue;
                    } else if ("rtmp".equals(delivery)) {
                        logger.info("skip delivery:" + delivery);
                        continue;
                    }
                    final boolean isProgressive = "progressive".equals(delivery);
                    if (isProgressive) {
                        isProgressiveStreamAvailable = true;
                    }
                    final String fmtHumanReadable = humanReadableQualityIdentifier(fmt.toUpperCase(Locale.ENGLISH).trim());
                    progressiveStreamFilesizeCheck: if (selectedQualities.contains(fmtHumanReadable) && isProgressive && !settingEnableFastCrawl && !qualityIdentifierToFilesizeMap.containsKey(fmtHumanReadable) && !has_active_youth_protection) {
                        logger.info("Checking progressive URL to find filesize: " + url_directlink_video);
                        URLConnectionAdapter con = null;
                        try {
                            final Browser brc = br.cloneBrowser();
                            con = brc.openHeadConnection(url_directlink_video);
                            if (!this.looksLikeDownloadableContent(con)) {
                                logger.info("Skipping broken progressive video quality: " + url_directlink_video);
                                continue;
                            } else if (ORFMediathek.isGeoBlocked(con.getURL().toExternalForm())) {
                                /* Item is GEO-blocked */
                                throw new DecrypterRetryException(RetryReason.GEO);
                            }
                            final long filesize = con.getCompleteContentLength();
                            if (filesize > 0) {
                                qualityIdentifierToFilesizeMap.put(fmtHumanReadable, filesize);
                                final Long filesizeSumForAllSegmentsOfThisQuality = qualityIdentifierToFilesizeMapGLOBAL.get(fmtHumanReadable);
                                if (filesizeSumForAllSegmentsOfThisQuality != null) {
                                    qualityIdentifierToFilesizeMapGLOBAL.put(fmtHumanReadable, filesizeSumForAllSegmentsOfThisQuality + filesize);
                                } else {
                                    qualityIdentifierToFilesizeMapGLOBAL.put(fmtHumanReadable, filesize);
                                }
                            }
                        } catch (final Exception e) {
                            if (e instanceof DecrypterRetryException) {
                                throw e;
                            } else {
                                /* Ignore Exception */
                                logger.log(e);
                            }
                        } finally {
                            try {
                                con.disconnect();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                    allAvailableQualitiesAsHumanReadableIdentifiers.add(fmtHumanReadable);
                    final DownloadLink video = super.createDownloadlink(url_directlink_video);
                    video.setDefaultPlugin(hosterplugin);
                    video.setHost(hosterplugin.getHost());
                    video.setContentUrl(sourceurl);
                    video.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_VIDEO);
                    video.setProperty(ORFMediathek.PROPERTY_DIRECTURL, url_directlink_video);
                    video.setProperty(ORFMediathek.PROPERTY_QUALITY_HUMAN_READABLE, fmtHumanReadable);
                    video.setProperty(ORFMediathek.PROPERTY_INTERNAL_QUALITY, fmt);
                    video.setProperty(ORFMediathek.PROPERTY_STREAMING_TYPE, protocol);
                    video.setProperty(ORFMediathek.PROPERTY_DELIVERY, delivery);
                    video.setAvailable(true);
                    videoresults.add(video);
                }
                /* Set additional properties */
                for (final DownloadLink videoresult : videoresults) {
                    /*
                     * Small trick: Update filesizes of all video items: If filesizes were found from progressive streams we can assume that
                     * the same file streamed via different streaming method will have a very similar size.
                     */
                    final String humanReadableQualityIdentifier = videoresult.getStringProperty(ORFMediathek.PROPERTY_QUALITY_HUMAN_READABLE);
                    final Long filesize = qualityIdentifierToFilesizeMap.get(humanReadableQualityIdentifier);
                    if (filesize != null) {
                        videoresult.setDownloadSize(filesize);
                    }
                }
                String bestAvailableQualityIdentifierHumanReadable = null;
                final String[] qualities = { "VERYHIGH", "HIGH", "MEDIUM", "LOW", "VERYLOW" };
                for (final String qual : qualities) {
                    if (allAvailableQualitiesAsHumanReadableIdentifiers.contains(qual)) {
                        bestAvailableQualityIdentifierHumanReadable = qual;
                        break;
                    }
                }
                final List<DownloadLink> selectedVideoQualities = new ArrayList<DownloadLink>();
                final List<DownloadLink> bestVideos = new ArrayList<DownloadLink>();
                for (final DownloadLink videoresult : videoresults) {
                    final String humanReadableQualityIdentifier = videoresult.getStringProperty(ORFMediathek.PROPERTY_QUALITY_HUMAN_READABLE);
                    final String delivery = videoresult.getStringProperty(ORFMediathek.PROPERTY_DELIVERY);
                    if (!selectedDeliveryTypes.contains(delivery)) {
                        /* Skip because user has de-selected this delivery type */
                        continue;
                    }
                    if (selectedQualities.contains(humanReadableQualityIdentifier)) {
                        selectedVideoQualities.add(videoresult);
                    }
                    if (StringUtils.equals(bestAvailableQualityIdentifierHumanReadable, humanReadableQualityIdentifier)) {
                        bestVideos.add(videoresult);
                    }
                }
                final List<DownloadLink> chosenVideoResults = new ArrayList<DownloadLink>();
                if (settingPreferBestVideo) {
                    /* Assume that we always find best-results. */
                    chosenVideoResults.addAll(bestVideos);
                } else {
                    if (selectedVideoQualities.size() > 0) {
                        chosenVideoResults.addAll(selectedVideoQualities);
                    } else {
                        /* Fallback */
                        logger.info("User selection would return no video results -> Returning all");
                        chosenVideoResults.addAll(videoresults);
                    }
                }
                final List<DownloadLink> finalresults = new ArrayList<DownloadLink>();
                finalresults.addAll(chosenVideoResults);
                /* Add a subtitle-result for each chosen video quality */
                if (cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_SUBTITLES, ORFMediathek.Q_SUBTITLES_default) && !StringUtils.isEmpty(subtitleurl)) {
                    for (final DownloadLink chosenVideoResult : chosenVideoResults) {
                        final DownloadLink subtitle = createDownloadlink(subtitleurl);
                        subtitle.setDefaultPlugin(hosterplugin);
                        subtitle.setHost(hosterplugin.getHost());
                        subtitle.setProperties(chosenVideoResult.getProperties());
                        subtitle.setProperty(ORFMediathek.PROPERTY_DIRECTURL, subtitleurl);
                        subtitle.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_SUBTITLE);
                        subtitle.setProperty(ORFMediathek.CONTENT_EXT_HINT, subtitle_ext);
                        subtitle.setAvailable(true);
                        subtitle.setContentUrl(sourceurl);
                        finalresults.add(subtitle);
                    }
                }
                if (cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_THUMBNAIL, ORFMediathek.Q_THUMBNAIL_default) && !StringUtils.isEmpty(thumbnailurl)) {
                    final DownloadLink thumbnail = this.createDownloadlink(thumbnailurl);
                    thumbnail.setDefaultPlugin(hosterplugin);
                    thumbnail.setHost(hosterplugin.getHost());
                    thumbnail.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_IMAGE);
                    thumbnail.setProperty(ORFMediathek.PROPERTY_DIRECTURL, thumbnailurl);
                    thumbnail.setAvailable(true);
                    finalresults.add(thumbnail);
                }
                // TODO: Check if this can still happen
                if (videoresults.isEmpty() && numberofSkippedDRMItems > 0) {
                    /* Seems like all available video streams are DRM protected. */
                    final DownloadLink dummy = this.createOfflinelink(br.getURL(), "DRM_" + titlethis, "This video is DRM protected and cannot be downloaded with JDownloader.");
                    finalresults.add(dummy);
                }
                /* Add more properties which are the same for all results of this segment */
                for (final DownloadLink result : finalresults) {
                    result.setProperty(ORFMediathek.PROPERTY_VIDEO_POSITION, videoPosition);
                    result.setProperty(ORFMediathek.PROPERTY_VIDEO_POSITION_MAX, segments.size());
                    result.setProperty(ORFMediathek.PROPERTY_TITLE, titlethis);
                    result.setProperty(ORFMediathek.PROPERTY_SEGMENT_ID, segmentID);
                    ret.add(result);
                }
            }
        }
        final boolean isCrawlGaplessAndVideoChapters;
        final boolean isCrawlGaplessOnly;
        if (cfg == null) {
            isCrawlGaplessAndVideoChapters = true;
            isCrawlGaplessOnly = false;
        } else {
            final int videoFormatSettingInt = cfg.getIntegerProperty(ORFMediathek.SETTING_SELECTED_VIDEO_FORMAT, ORFMediathek.SETTING_SELECTED_VIDEO_FORMAT_default);
            isCrawlGaplessAndVideoChapters = videoFormatSettingInt == 0;
            isCrawlGaplessOnly = videoFormatSettingInt == 2;
        }
        final boolean alreadyFoundGaplessProgressive = segments.size() == 1 && isProgressiveStreamAvailable;
        final boolean gaplessNeeded = isCrawlGaplessAndVideoChapters || isCrawlGaplessOnly;
        final boolean crawlGapless;
        if (ret.isEmpty()) {
            /* Found nothing -> Try to crawl gapless items */
            logger.info("Found nothing -> Trying to crawl gapless version as fallback");
            crawlGapless = true;
        } else if (gaplessNeeded && !alreadyFoundGaplessProgressive) {
            logger.info("User prefers gapless and already crawled items don't looke like gapless");
            crawlGapless = true;
        } else {
            /* Do not crawl gapless streams */
            crawlGapless = false;
        }
        gaplessHandling: if (crawlGapless) {
            /* Gapless video handling */
            logger.info("Crawling gapless video streams");
            // TODO: 2024-03-18: Add auto fallback to progressive video chapters if user prefers gapless && gapless is HLS split
            // audio/video.
            /* Check if any gapless sources are available. */
            final Map<String, Object> sourcesForGaplessVideo = (Map<String, Object>) entries.get("sources");
            if (sourcesForGaplessVideo == null || sourcesForGaplessVideo.isEmpty()) {
                logger.info("No gapless sources available -> Returning items we found so far");
                break gaplessHandling;
            }
            final List<Map<String, Object>> sources_hls = (List<Map<String, Object>>) sourcesForGaplessVideo.get("hls");
            if (sources_hls == null || sources_hls.isEmpty()) {
                logger.info("No gapless HLS sources available -> Returning items we found so far");
                break gaplessHandling;
            }
            final String segmentID = "gapless";
            final List<DownloadLink> videoresults = new ArrayList<DownloadLink>();
            DownloadLink best = null;
            for (final Map<String, Object> hlssource : sources_hls) {
                final String hlsMaster = hlssource.get("src").toString();
                br.getPage(hlsMaster);
                if (ORFMediathek.isGeoBlocked(br.getURL())) {
                    /* Item is GEO-blocked */
                    throw new DecrypterRetryException(RetryReason.GEO);
                }
                int heigthMax = 0;
                final List<HlsContainer> hlscontainers = HlsContainer.getHlsQualities(br);
                for (final HlsContainer hlscontainer : hlscontainers) {
                    // TODO: Maybe force-add unknown hls qualities
                    final String fmt = heightToOldQualityIdentifier(hlscontainer.getHeight());
                    final String fmtHumanReadable = heightToHumanReadableQualityIdentifier(hlscontainer.getHeight());
                    if (fmtHumanReadable == null) {
                        logger.info("Skipping unknown HLS quality: " + hlscontainer.getResolution());
                        continue;
                    }
                    final DownloadLink video = this.createDownloadlink(hlscontainer.getDownloadurl());
                    video.setDefaultPlugin(hosterplugin);
                    video.setHost(hosterplugin.getHost());
                    video.setContentUrl(sourceurl);
                    video.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_VIDEO);
                    video.setProperty(ORFMediathek.PROPERTY_DIRECTURL, hlscontainer.getDownloadurl());
                    video.setProperty(ORFMediathek.PROPERTY_QUALITY_HUMAN_READABLE, fmtHumanReadable);
                    video.setProperty(ORFMediathek.PROPERTY_INTERNAL_QUALITY, fmt);
                    video.setProperty(ORFMediathek.PROPERTY_STREAMING_TYPE, "http");
                    video.setProperty(ORFMediathek.PROPERTY_DELIVERY, "hls");
                    final Long guessedFilesize = qualityIdentifierToFilesizeMapGLOBAL.get(fmtHumanReadable);
                    if (guessedFilesize != null) {
                        video.setDownloadSize(guessedFilesize);
                    }
                    videoresults.add(video);
                    if (best == null || hlscontainer.getHeight() > heigthMax) {
                        heigthMax = hlscontainer.getHeight();
                        best = video;
                    }
                }
                /* Only add first item for now as the others look to be duplicates. */
                break;
            }
            if (videoresults.isEmpty()) {
                /**
                 * All possible results were skipped? -> This should never happen / very very rare case. </br>
                 * Either all available video resolutions are unsupported resolutions or GEO-blocked detection failed or something super
                 * unexpected happened.
                 */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Now decide which qualities we want to return. */
            final List<DownloadLink> selectedVideoQualities = new ArrayList<DownloadLink>();
            for (final DownloadLink videoresult : videoresults) {
                final String humanReadableQualityIdentifier = videoresult.getStringProperty(ORFMediathek.PROPERTY_QUALITY_HUMAN_READABLE);
                if (selectedQualities.contains(humanReadableQualityIdentifier)) {
                    selectedVideoQualities.add(videoresult);
                }
            }
            final List<DownloadLink> videoSelectedResults = new ArrayList<DownloadLink>();
            if (settingPreferBestVideo) {
                videoSelectedResults.add(best);
            } else {
                if (selectedVideoQualities.size() > 0) {
                    videoSelectedResults.addAll(selectedVideoQualities);
                } else {
                    logger.info("Users selection would return zero results -> Returning all instead");
                    videoSelectedResults.addAll(videoSelectedResults);
                }
            }
            final ArrayList<DownloadLink> thisFinalResults = new ArrayList<DownloadLink>();
            /* Sollect all chosen results and add subtitle if user wants to download subtitle. */
            for (final DownloadLink chosenVideoResult : videoSelectedResults) {
                thisFinalResults.add(chosenVideoResult);
                /* Add a subtitle-result for each chosen video quality */
                if ((cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_SUBTITLES, ORFMediathek.Q_SUBTITLES_default)) && !StringUtils.isEmpty(gapless_subtitleurl)) {
                    final DownloadLink subtitle = createDownloadlink(gapless_subtitleurl);
                    subtitle.setDefaultPlugin(hosterplugin);
                    subtitle.setHost(hosterplugin.getHost());
                    /* Inherit properties from video item */
                    subtitle.setProperties(chosenVideoResult.getProperties());
                    subtitle.setProperty(ORFMediathek.PROPERTY_DIRECTURL, gapless_subtitleurl);
                    subtitle.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_SUBTITLE);
                    subtitle.setProperty(ORFMediathek.CONTENT_EXT_HINT, subtitle_ext);
                    thisFinalResults.add(subtitle);
                }
            }
            /* Add teaser/thumbnail image */
            if ((cfg == null || cfg.getBooleanProperty(ORFMediathek.Q_THUMBNAIL, ORFMediathek.Q_THUMBNAIL_default)) && !StringUtils.isEmpty(thumbnailurlFromFirstSegment)) {
                final DownloadLink thumbnail = this.createDownloadlink(thumbnailurlFromFirstSegment);
                thumbnail.setDefaultPlugin(hosterplugin);
                thumbnail.setHost(hosterplugin.getHost());
                thumbnail.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_IMAGE);
                thumbnail.setProperty(ORFMediathek.PROPERTY_DIRECTURL, thumbnailurlFromFirstSegment);
                thisFinalResults.add(thumbnail);
            }
            if (isCrawlGaplessOnly) {
                /* Discard previously found results as user wants gapless items only. */
                ret.clear();
            }
            /* Add properties and add results to final list */
            for (final DownloadLink result : thisFinalResults) {
                result.setProperty(ORFMediathek.PROPERTY_TITLE, mainVideoTitle);
                result.setProperty(ORFMediathek.PROPERTY_TITLE, mainVideoTitle);
                result.setProperty(ORFMediathek.PROPERTY_SEGMENT_ID, segmentID);
                result.setProperty(ORFMediathek.PROPERTY_TITLE, mainVideoTitle);
                ret.add(result);
            }
        }
        /**
         * Add more properties which are the same for all results. </br>
         * It is important that all items run through this loop!
         */
        for (final DownloadLink result : ret) {
            if (!result.hasProperty(ORFMediathek.PROPERTY_CONTENT_TYPE)) {
                continue;
            }
            result.setContentUrl(sourceurl);
            result.setProperty(ORFMediathek.PROPERTY_VIDEO_ID, contentIDSlashPlaylistIDSlashVideoID);
            result.setProperty(ORFMediathek.PROPERTY_SOURCEURL, sourceurl);
            if (has_active_youth_protection) {
                result.setProperty(ORFMediathek.PROPERTY_AGE_RESTRICTED, true);
            }
            result.setFinalFileName(ORFMediathek.getFormattedVideoFilename(result));
            result.setAvailable(true);
        }
        fp.addLinks(ret);
        return ret;
    }

    private ArrayList<DownloadLink> crawlOrfmediathekNew(final CryptedLink param, final String contenturl) throws Exception {
        final Regex videourl = new Regex(contenturl, "(?i)https?://on\\.orf\\.at/video/(\\d+)(/([\\w\\-]+))?");
        if (!videourl.patternFind()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Unsupported URL");
        }
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String videoSource = br.getRegex("data-ssr=\"true\">(.+)</script>").getMatch(0);
        final String videoDurationStr = new Regex(videoSource, "duration=(\\d+)").getMatch(0);
        String encryptedID = null;
        if (videoDurationStr != null) {
            encryptedID = new Regex(videoSource, "\"([^\"]+)\"," + videoDurationStr).getMatch(0);
        }
        if (encryptedID == null) {
            /* Lazy attempt */
            encryptedID = new Regex(videoSource, "\"([A-Za-z0-9]{36})\"").getMatch(0);
        }
        if (encryptedID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return crawlOrfmediathekNewEncryptedID(encryptedID, contenturl);
    }

    @Deprecated
    /** TODO: Delete this once they've fully switched to on.orf.at. */
    private ArrayList<DownloadLink> crawlOrfmediathekOld(final CryptedLink param) throws Exception {
        final String contenturl = param.getCryptedUrl().replaceFirst("/index\\.php/", "/");
        final boolean rewriteUrlsToNewMediathek = true;
        final String videoIDFromURL = new Regex(param.getCryptedUrl(), "/profile/[^/]+/[^/]+/[^/]+/(\\d+)").getMatch(0);
        if (rewriteUrlsToNewMediathek && videoIDFromURL != null) {
            return this.crawlOrfmediathekNew(param, generateContenturlMediathek(videoIDFromURL, null));
        }
        br.setAllowedResponseCodes(500);
        br.setLoadLimit(this.br.getLoadLimit() * 4);
        br.getPage(contenturl);
        int status = br.getHttpConnection().getResponseCode();
        if (status != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("(404 \\- Seite nicht gefunden\\.|area_headline error_message\">Keine Sendung vorhanden<)") || !br.containsHTML("jsb_VideoPlaylist") || status == 404 || status == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String sourceurl = param.getCryptedUrl();
        String json = br.getRegex("class=\"jsb_ jsb_VideoPlaylist\" data\\-jsb=\"([^<>\"]+)\"").getMatch(0);
        /* jsonData --> HashMap */
        json = Encoding.htmlOnlyDecode(json);
        final Map<String, Object> root = restoreFromString(json, TypeRef.MAP);
        /* In this context a playlist is mostly a single video split into multiple parts/chapters. */
        final Map<String, Object> playlist = (Map<String, Object>) root.get("playlist");
        final String contentIDSlashPlaylistIDSlashVideoID = playlist.get("id").toString();
        final Map<String, Object> gapless_video = (Map<String, Object>) playlist.get("gapless_video");
        final List<Map<String, Object>> segments = (List<Map<String, Object>>) playlist.get("videos");
        final String titleOfPlaylistOrVideo = playlist.get("title").toString();
        if (rewriteUrlsToNewMediathek) {
            if (StringUtils.isEmpty(contentIDSlashPlaylistIDSlashVideoID)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return this.crawlOrfmediathekNew(param, generateContenturlMediathek(contentIDSlashPlaylistIDSlashVideoID, null));
        }
        final boolean isGaplessVideo;
        if (gapless_video != null && !gapless_video.isEmpty()) {
            /* Prefer single gapless video. */
            segments.clear();
            segments.add(gapless_video);
            isGaplessVideo = true;
        } else {
            isGaplessVideo = false;
        }
        /* playlist parameter is only given for old website tvthek.orf.at. */
        final SubConfiguration cfg = SubConfiguration.getConfig("orf.at");
        final List<String> selectedQualities = new ArrayList<String>();
        final List<String> selectedDeliveryTypes = new ArrayList<String>();
        boolean allow_HTTP = cfg.getBooleanProperty(ORFMediathek.PROGRESSIVE_STREAM, ORFMediathek.PROGRESSIVE_STREAM_default);
        final boolean allow_HDS = cfg.getBooleanProperty(ORFMediathek.HDS_STREAM, ORFMediathek.HDS_STREAM_default);
        final boolean allow_HLS = cfg.getBooleanProperty(ORFMediathek.HLS_STREAM, ORFMediathek.HLS_STREAM_default);
        if (allow_HDS) {
            selectedDeliveryTypes.add("hds");
        }
        if (allow_HLS) {
            selectedDeliveryTypes.add("hls");
        }
        allow_HTTP = true; // 2024-02-07: Enforce progressive
        if (allow_HTTP || true) {
            selectedDeliveryTypes.add("progressive");
        }
        if (cfg.getBooleanProperty(ORFMediathek.Q_VERYLOW, ORFMediathek.Q_VERYLOW_default)) {
            selectedQualities.add("VERYLOW");
        }
        if (cfg.getBooleanProperty(ORFMediathek.Q_LOW, ORFMediathek.Q_LOW_default)) {
            selectedQualities.add("LOW");
        }
        if (cfg.getBooleanProperty(ORFMediathek.Q_MEDIUM, ORFMediathek.Q_MEDIUM_default)) {
            selectedQualities.add("MEDIUM");
        }
        if (cfg.getBooleanProperty(ORFMediathek.Q_HIGH, ORFMediathek.Q_HIGH_default)) {
            selectedQualities.add("HIGH");
        }
        if (cfg.getBooleanProperty(ORFMediathek.Q_VERYHIGH, ORFMediathek.Q_VERYHIGH_default)) {
            selectedQualities.add("VERYHIGH");
        }
        final ORFMediathek hosterplugin = (ORFMediathek) this.getNewPluginForHostInstance("orf.at");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(playlist.get("title").toString());
        fp.setPackageKey("orfmediathek://video/" + contentIDSlashPlaylistIDSlashVideoID);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int videoPosition = 0;
        final String thumbnailurlGapless = playlist.get("preview_image_url").toString();
        for (final Map<String, Object> segment : segments) {
            videoPosition++;
            final Object segmentID_O = segment.get("id");
            final String segmentID;
            if (segmentID_O == null || segments.size() == 1) {
                segmentID = "gapless";
            } else {
                segmentID = segmentID_O.toString();
            }
            String thumbnailurl = (String) segment.get("preview_image_url");
            if (thumbnailurl == null) {
                thumbnailurl = thumbnailurlGapless;
            }
            final List<Map<String, Object>> sources = (List<Map<String, Object>>) segment.get("sources");
            final List<Map<String, Object>> subtitle_list = (List<Map<String, Object>>) segment.get("subtitles");
            String titlethis = null;
            if (isGaplessVideo) {
                titlethis = titleOfPlaylistOrVideo;
            } else {
                titlethis = (String) playlist.get("title");
            }
            String subtitleurl = null;
            int numberofSkippedDRMItems = 0;
            boolean progressiveSourcesLookGood = false;
            final Map<String, Long> qualityIdentifierToFilesizeMap = new HashMap<String, Long>();
            final List<DownloadLink> videoresults = new ArrayList<DownloadLink>();
            final HashSet<String> allAvailableQualitiesAsHumanReadableIdentifiers = new HashSet<String>();
            for (final Map<String, Object> source : sources) {
                subtitleurl = null;
                final String url_directlink_video = (String) source.get("src");
                final String fmt = (String) source.get("quality");
                final String protocol = (String) source.get("protocol");
                final String delivery = (String) source.get("delivery");
                // final String subtitleUrl = (String) entry_source.get("SubTitleUrl");
                if (StringUtils.equals(fmt, "QXADRM")) {
                    numberofSkippedDRMItems++;
                    continue;
                }
                if (subtitle_list != null && subtitle_list.size() > 0) {
                    /* [0] = .srt, [1] = WEBVTT .vtt */
                    if (subtitle_list.size() > 1) {
                        subtitleurl = (String) JavaScriptEngineFactory.walkJson(subtitle_list.get(1), "src");
                    } else if (subtitle_list.size() == 1) {
                        subtitleurl = (String) JavaScriptEngineFactory.walkJson(subtitle_list.get(0), "src");
                    } else {
                        subtitleurl = null;
                    }
                }
                /* possible protocols: http, rtmp, rtsp, hds, hls */
                if (!"http".equals(protocol)) {
                    logger.info("skip protocol:" + protocol);
                    continue;
                }
                if ("dash".equals(delivery)) {
                    /* 2021-04-06 unsupported */
                    logger.info("skip delivery: " + delivery);
                    continue;
                } else if ("rtmp".equals(delivery)) {
                    logger.info("skip delivery: " + delivery);
                    continue;
                }
                final String fmtHumanReadable = humanReadableQualityIdentifier(fmt.toUpperCase(Locale.ENGLISH).trim());
                if (selectedQualities.contains(fmtHumanReadable) && "progressive".equals(delivery) && !qualityIdentifierToFilesizeMap.containsKey(fmtHumanReadable)) {
                    /*
                     * VERYHIGH is always available but is not always REALLY available which means we have to check this here and skip it if
                     * needed! Filesize is also needed to find BEST quality.
                     */
                    URLConnectionAdapter con = null;
                    try {
                        final Browser brc = br.cloneBrowser();
                        brc.setFollowRedirects(true);
                        con = brc.openHeadConnection(url_directlink_video);
                        if (!this.looksLikeDownloadableContent(con)) {
                            logger.info("Skipping broken progressive video quality: " + url_directlink_video);
                            continue;
                        }
                        progressiveSourcesLookGood = true;
                        qualityIdentifierToFilesizeMap.put(fmtHumanReadable, con.getCompleteContentLength());
                    } catch (final Exception e) {
                        logger.log(e);
                    } finally {
                        try {
                            con.disconnect();
                        } catch (final Throwable e) {
                        }
                    }
                    if (!progressiveSourcesLookGood) {
                        continue;
                    }
                }
                allAvailableQualitiesAsHumanReadableIdentifiers.add(fmtHumanReadable);
                final DownloadLink link = super.createDownloadlink(url_directlink_video);
                link.setDefaultPlugin(hosterplugin);
                link.setHost(hosterplugin.getHost());
                link.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_VIDEO);
                link.setProperty(ORFMediathek.PROPERTY_TITLE, titleOfPlaylistOrVideo);
                link.setProperty(ORFMediathek.PROPERTY_VIDEO_POSITION, videoPosition);
                link.setProperty(ORFMediathek.PROPERTY_DIRECTURL, url_directlink_video);
                link.setProperty(ORFMediathek.PROPERTY_QUALITY_HUMAN_READABLE, fmtHumanReadable);
                link.setProperty(ORFMediathek.PROPERTY_INTERNAL_QUALITY, fmt);
                link.setProperty(ORFMediathek.PROPERTY_SOURCEURL, sourceurl);
                link.setProperty(ORFMediathek.PROPERTY_STREAMING_TYPE, protocol);
                link.setProperty(ORFMediathek.PROPERTY_DELIVERY, delivery);
                link.setAvailable(true);
                videoresults.add(link);
            }
            /* Collect thumbnail results */
            /* Set additional properties */
            for (final DownloadLink videoresult : videoresults) {
                videoresult.setProperty(ORFMediathek.PROPERTY_VIDEO_ID, contentIDSlashPlaylistIDSlashVideoID);
                videoresult.setProperty(ORFMediathek.PROPERTY_SEGMENT_ID, segmentID);
                videoresult.setProperty(ORFMediathek.PROPERTY_VIDEO_POSITION_MAX, segments.size());
                videoresult._setFilePackage(fp);
            }
            /*
             * Small trick: Update filesizes of all video items: If filesizes were found from progressive streams we can assume that the
             * same file streamed via different streaming method will have a very similar size.
             */
            for (final DownloadLink videoresult : videoresults) {
                final String humanReadableQualityIdentifier = videoresult.getStringProperty(ORFMediathek.PROPERTY_QUALITY_HUMAN_READABLE);
                final Long filesize = qualityIdentifierToFilesizeMap.get(humanReadableQualityIdentifier);
                if (filesize != null) {
                    videoresult.setDownloadSize(filesize);
                }
            }
            String bestAvailableQualityIdentifierHumanReadable = null;
            final String[] qualities = { "VERYHIGH", "HIGH", "MEDIUM", "LOW", "VERYLOW" };
            for (final String qual : qualities) {
                if (allAvailableQualitiesAsHumanReadableIdentifiers.contains(qual)) {
                    bestAvailableQualityIdentifierHumanReadable = qual;
                    break;
                }
            }
            final List<DownloadLink> selectedVideoQualities = new ArrayList<DownloadLink>();
            final List<DownloadLink> bestVideos = new ArrayList<DownloadLink>();
            for (final DownloadLink videoresult : videoresults) {
                final String humanReadableQualityIdentifier = videoresult.getStringProperty(ORFMediathek.PROPERTY_QUALITY_HUMAN_READABLE);
                final String delivery = videoresult.getStringProperty(ORFMediathek.PROPERTY_DELIVERY);
                if (!selectedDeliveryTypes.contains(delivery)) {
                    /* Skip because user has de-selected this delivery type */
                    continue;
                }
                if (selectedQualities.contains(humanReadableQualityIdentifier)) {
                    selectedVideoQualities.add(videoresult);
                }
                if (StringUtils.equals(bestAvailableQualityIdentifierHumanReadable, humanReadableQualityIdentifier)) {
                    bestVideos.add(videoresult);
                }
            }
            final List<DownloadLink> chosenVideoResults = new ArrayList<DownloadLink>();
            if (cfg.getBooleanProperty(ORFMediathek.Q_BEST, ORFMediathek.Q_BEST_default)) {
                /* Assume that we always find best-results. */
                chosenVideoResults.addAll(bestVideos);
            } else {
                if (selectedVideoQualities.size() > 0) {
                    chosenVideoResults.addAll(selectedVideoQualities);
                } else {
                    /* Fallback */
                    logger.info("User selection would return no video results -> Returning all");
                    chosenVideoResults.addAll(videoresults);
                }
            }
            final List<DownloadLink> finalresults = new ArrayList<DownloadLink>();
            finalresults.addAll(chosenVideoResults);
            /* Add a subtitle-result for each chosen video quality */
            if (cfg.getBooleanProperty(ORFMediathek.Q_SUBTITLES, ORFMediathek.Q_SUBTITLES_default) && !StringUtils.isEmpty(subtitleurl)) {
                for (final DownloadLink chosenVideoResult : chosenVideoResults) {
                    final DownloadLink subtitle = createDownloadlink(subtitleurl);
                    subtitle.setDefaultPlugin(hosterplugin);
                    subtitle.setHost(hosterplugin.getHost());
                    subtitle.setProperties(chosenVideoResult.getProperties());
                    subtitle.setProperty(ORFMediathek.PROPERTY_DIRECTURL, subtitleurl);
                    subtitle.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_SUBTITLE);
                    subtitle.setAvailable(true);
                    subtitle.setContentUrl(sourceurl);
                    finalresults.add(subtitle);
                }
            }
            if (cfg.getBooleanProperty(ORFMediathek.Q_THUMBNAIL, ORFMediathek.Q_THUMBNAIL_default) && !StringUtils.isEmpty(thumbnailurl)) {
                final DownloadLink thumbnail = this.createDownloadlink(thumbnailurl);
                thumbnail.setDefaultPlugin(hosterplugin);
                thumbnail.setHost(hosterplugin.getHost());
                thumbnail.setProperty(ORFMediathek.PROPERTY_CONTENT_TYPE, ORFMediathek.CONTENT_TYPE_IMAGE);
                thumbnail.setProperty(ORFMediathek.PROPERTY_VIDEO_ID, contentIDSlashPlaylistIDSlashVideoID);
                thumbnail.setProperty(ORFMediathek.PROPERTY_SEGMENT_ID, segmentID);
                thumbnail.setProperty(ORFMediathek.PROPERTY_TITLE, titleOfPlaylistOrVideo);
                thumbnail.setProperty(ORFMediathek.PROPERTY_VIDEO_POSITION, videoPosition);
                thumbnail.setProperty(ORFMediathek.PROPERTY_DIRECTURL, thumbnailurl);
                thumbnail.setProperty(ORFMediathek.PROPERTY_SOURCEURL, sourceurl);
                thumbnail.setAvailable(true);
                finalresults.add(thumbnail);
            }
            /* Set filenames for all video-related items */
            for (final DownloadLink finalresult : finalresults) {
                finalresult.setFinalFileName(ORFMediathek.getFormattedVideoFilename(finalresult));
            }
            // TODO: Check if this can still happen
            if (videoresults.isEmpty() && numberofSkippedDRMItems > 0) {
                /* Seems like all available video streams are DRM protected. */
                final DownloadLink dummy = this.createOfflinelink(br.getURL(), "DRM_" + titlethis, "This video is DRM protected and cannot be downloaded with JDownloader.");
                finalresults.add(dummy);
            }
            for (final DownloadLink finalresult : finalresults) {
                finalresult._setFilePackage(fp);
                ret.add(finalresult);
            }
        }
        return ret;
    }

    private static String generateContenturlMediathek(final String videoID, final String slug) {
        String url = "https://on.orf.at/video/" + videoID;
        if (slug != null) {
            url += "/" + slug;
        }
        return url;
    }

    private static String heightToHumanReadableQualityIdentifier(final int height) {
        if (height == 360) {
            return "MEDIUM";
        } else if (height == 540) {
            return "HIGH";
        } else if (height == 720) {
            return "VERYHIGH";
        } else {
            return null;
        }
    }

    private static String heightToOldQualityIdentifier(final int height) {
        if (height == 360) {
            return "Q4A";
        } else if (height == 540) {
            return "Q6A";
        } else if (height == 720) {
            return "Q8C";
        } else {
            return null;
        }
    }

    public static String humanReadableQualityIdentifier(String quality) {
        final String humanreabable;
        if ("Q0A".equals(quality)) {
            humanreabable = "VERYLOW";
        } else if ("Q1A".equals(quality)) {
            humanreabable = "LOW";
        } else if ("Q4A".equals(quality)) {
            humanreabable = "MEDIUM";
        } else if ("Q6A".equals(quality)) {
            humanreabable = "HIGH";
        } else if ("Q8C".equals(quality)) {
            humanreabable = "VERYHIGH";
        } else if ("QXA".equals(quality) || "QXB".equals(quality)) {
            humanreabable = "ADAPTIV";
        } else {
            humanreabable = quality;
        }
        return humanreabable;
    }

    private ArrayList<DownloadLink> crawlArticle(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String title = br.getRegex("\"og:title\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        final String oon_audioEntries[] = br.getRegex("<div class=\"oon-audio\"(.*?)\\s*</div>").getColumn(0);
        if (oon_audioEntries.length > 0) {
            for (final String oon_audioEntry : oon_audioEntries) {
                final String url = new Regex(oon_audioEntry, "data-url\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
                if (url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink link = createDownloadlink(url);
                link.setContentUrl(param.getCryptedUrl());
                String name = getFileNameFromURL(new URL(url));
                if (title != null) {
                    name = title + "-" + name;
                    fp.add(link);
                }
                if (name != null) {
                    link.setFinalFileName(name);
                }
                link.setProperty(DirectHTTP.FIXNAME, name);
                ret.add(link);
            }
            return ret;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /** Crawls all episodes of a podcast or only a specific episode. */
    private ArrayList<DownloadLink> crawlPodcast(final CryptedLink param) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), PATTERN_PODCAST);
        final String channelSlug = urlinfo.getMatch(0);
        final String podcastSeriesSlug = urlinfo.getMatch(1);
        final String podcastEpisodeTitleSlug = urlinfo.getMatch(3); // optional
        if (channelSlug == null || podcastSeriesSlug == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Old API call: (2022-09-27: Still working) */
        // br.getPage(API_BASE + "/radiothek/podcast/" + channelSlug + "/" + podcastSeriesSlug + ".json?_o=" + hostFromURL);
        br.getPage(API_BASE + "/radiothek/api/2.0/podcast/" + channelSlug + "/" + podcastSeriesSlug + "?episodes&_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* This should never happen but for sure users could modify added URLs and render them invalid. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> payload = (Map<String, Object>) entries.get("payload");
        final String station = payload.get("station").toString();
        final String podcastSlug = payload.get("slug").toString();
        final String podcastDescription = (String) payload.get("description");
        final String author = payload.get("author").toString();
        final String podcastTitle = payload.get("title").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(author + " - " + podcastTitle);
        if (!StringUtils.isEmpty(podcastDescription)) {
            fp.setComment(podcastDescription);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        boolean foundSpecificEpisode = false;
        final List<Map<String, Object>> episodes = (List<Map<String, Object>>) payload.get("episodes");
        for (final Map<String, Object> episode : episodes) {
            String directurl = null;
            final List<Map<String, Object>> enclosures = (List<Map<String, Object>>) episode.get("enclosures");
            for (final Map<String, Object> enclosure : enclosures) {
                if (enclosure.get("type").toString().equals("audio/mpeg")) {
                    directurl = enclosure.get("url").toString();
                    break;
                }
            }
            if (StringUtils.isEmpty(directurl)) {
                /* Most likely unsupported streaming type */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String episodeSlug = episode.get("slug").toString();
            final DownloadLink link = createPodcastDownloadlink(directurl);
            final String episodeDateStr = episode.get("published").toString();
            final String dateFormatted = new Regex(episodeDateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            final String filename = dateFormatted + "_" + author + " - " + episode.get("title").toString() + ".mp3";
            link.setFinalFileName(filename);
            link.setDownloadSize(calculateFilesize(((Number) episode.get("duration")).longValue()));
            link.setProperty(DirectHTTP.FIXNAME, filename);
            final String contentURL = (String) JavaScriptEngineFactory.walkJson(episode, "link/url");
            if (!StringUtils.isEmpty(contentURL)) {
                link.setContentUrl(contentURL);
            } else {
                /* ContentURLs are not always given in json e.g. https://sound.orf.at/podcast/tv/report-werkstatt */
                link.setContentUrl("https://sound.orf.at/podcast/" + station + "/" + podcastSlug + "/" + episodeSlug);
            }
            final String description = (String) episode.get("description");
            if (!StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
            link.setAvailable(true);
            link._setFilePackage(fp);
            if (podcastEpisodeTitleSlug != null && episodeSlug.equals(podcastEpisodeTitleSlug)) {
                /* User added link to single episode and we found it --> Clear previous findings and only return that single episode. */
                ret.clear();
                ret.add(link);
                foundSpecificEpisode = true;
                break;
            } else {
                ret.add(link);
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (podcastEpisodeTitleSlug != null && !foundSpecificEpisode) {
            /* Rare case */
            logger.warning("Failed to find specific episode --> Adding all instead");
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlProgramm(final String domainID, final String broadcastID, final String broadcastDay) throws Exception {
        if (broadcastID == null || broadcastDay == null || domainID == null || domainID.length() < 3) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* E.g. change "wien" to "wie": https://wien.orf.at/player/20220925/WTIT */
        final String domainIDCorrected = domainID.toLowerCase(Locale.ENGLISH).substring(0, 3);
        synchronized (CHANNEL_CACHE) {
            /* 2021-03-31 */
            if (!CHANNEL_CACHE.containsKey(domainIDCorrected)) {
                br.getPage("https://assets.orf.at/vue-storyserver/radiothek-item-player/js/app.js");
                final String channelInfoJs = br.getRegex("stations:(\\{.*?\\}\\}\\})\\},").getMatch(0);
                final Map<String, Map<String, Object>> channelInfo = (Map<String, Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(channelInfoJs);
                CHANNEL_CACHE.clear();
                CHANNEL_CACHE.putAll(channelInfo);
                // final String[][] shortnameToChannelNames = br.getRegex("/([^/]+)/json/" +
                // Regex.escape("4.0/broadcast{/programKey}{/broadcastDay}\")}") + ".*?,channel:\"([^\"]+)\"").getMatches();
                // for (final String[] channelInfo : shortnameToChannelNames) {
                // CHANNEL_CACHE.put(channelInfo[0], channelInfo[1]);
                // }
                if (!CHANNEL_CACHE.containsKey(domainIDCorrected)) {
                    /* Most likely invalid domainID. */
                    logger.info("Failed to find channel for: " + domainIDCorrected);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
        final Map<String, Object> channelInfo = CHANNEL_CACHE.get(domainIDCorrected);
        final Map<String, Object> loopstream = (Map<String, Object>) channelInfo.get("loopstream");
        br.setAllowedResponseCodes(410);
        br.getPage(API_BASE + "/" + domainIDCorrected + "/api/json/current/broadcast/" + broadcastID + "/" + broadcastDay + "?_s=" + System.currentTimeMillis());
        final Map<String, Object> response = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        if (br.getHttpConnection().getResponseCode() == 410 || "Broadcast is no longer available".equals(response.get("message"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String broadcastDescription = (String) response.get("description");
        final String broadcastStartISO = response.get("startISO").toString();
        final String broadcastDateFormatted = new Regex(broadcastStartISO, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        // final String broadCastDay = response.get("broadcastDay").toString();
        final String title = (String) response.get("title");
        final String programTitle = (String) response.get("programTitle");
        final List<Map<String, Object>> streams = (List<Map<String, Object>>) response.get("streams");
        String titleBase = response.get("station").toString();
        if (!StringUtils.isEmpty(programTitle)) {
            titleBase += " - " + programTitle + " - " + title;
        } else {
            titleBase += " - " + title;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setCleanupPackageName(false);
        fp.setName(broadcastDateFormatted + "_" + titleBase);
        if (!StringUtils.isEmpty(broadcastDescription)) {
            fp.setComment(broadcastDescription);
        }
        int position = 1;
        final String userid = UUID.randomUUID().toString();
        final int padLength = StringUtils.getPadLength(streams.size());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Map<String, Object> stream : streams) {
            final String loopStreamId = (String) stream.get("loopStreamId");
            if (loopStreamId == null) {
                continue;
            }
            final String startISO = stream.get("startISO").toString();
            final String dateFormatted = new Regex(startISO, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            final long startTimestamp = ((Number) stream.get("start")).longValue();
            final long endTimestamp = ((Number) stream.get("end")).longValue();
            final long runtimeMilliseconds = endTimestamp - startTimestamp;
            final long startOffset = ((Number) stream.get("startOffset")).longValue();
            final long endOffset = ((Number) stream.get("endOffset")).longValue();
            final long offset = startOffset - endOffset;
            final DownloadLink link = createDownloadlink("directhttp://https://" + loopstream.get("host") + "/?channel=" + loopstream.get("channel") + "&shoutcast=0&player=" + domainIDCorrected + "_v1&referer=" + domainIDCorrected + ".orf.at&_=" + System.currentTimeMillis() + "&userid=" + userid + "&id=" + loopStreamId + "&offset=" + offset + "&offsetende=" + runtimeMilliseconds);
            if (streams.size() > 1) {
                link.setFinalFileName(dateFormatted + "_" + titleBase + "_" + StringUtils.formatByPadLength(padLength, position) + ".mp3");
            } else {
                link.setFinalFileName(dateFormatted + "_" + titleBase + ".mp3");
            }
            link.setDownloadSize(this.calculateFilesize(runtimeMilliseconds));
            link.setAvailable(true);
            link.setLinkID(domainIDCorrected + ".orf.at://" + broadcastID + "/" + broadcastDay + "/" + position);
            ret.add(link);
            fp.add(link);
            position++;
        }
        return ret;
    }

    /** Crawls all items of a collection. A collection can contain single episode of various podcasts. */
    private ArrayList<DownloadLink> crawlCollection(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex collectionPatternRegex = new Regex(param.getCryptedUrl(), PATTERN_COLLECTION);
        if (!collectionPatternRegex.patternFind()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String baseURL = collectionPatternRegex.getMatch(0);
        final String collectionID = collectionPatternRegex.getMatch(1);
        // final String collectionTargetItemID = collectionPatternRegex.getMatch(3);
        final String collectionURL = baseURL + "/" + collectionID;
        br.getPage("https://collector.orf.at/api/frontend/collections/" + collectionID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> payload = (Map<String, Object>) entries.get("payload");
        final Map<String, Object> collectionContent = (Map<String, Object>) payload.get("content");
        final String collectionTitle = collectionContent.get("title").toString();
        final String collectionDescription = (String) collectionContent.get("description");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(collectionTitle);
        if (!StringUtils.isEmpty(collectionDescription)) {
            fp.setComment(collectionDescription);
        }
        final List<Map<String, Object>> items = (List<Map<String, Object>>) collectionContent.get("items");
        int progress = 0;
        int numberofOfflineItems = 0;
        for (final Map<String, Object> item : items) {
            progress++;
            final String collectionItemID = item.get("id").toString();
            logger.info("Crawling collection item " + progress + "/" + items.size() + " ID: " + collectionItemID);
            final Map<String, Object> collectionItemContent = (Map<String, Object>) item.get("content");
            final Map<String, Object> target = (Map<String, Object>) item.get("target");
            if ((Boolean) target.get("isGone")) {
                /* This should never happen?! */
                numberofOfflineItems++;
                continue;
            }
            final String collectionItemTitle = collectionItemContent.get("title").toString();
            final String collectionItemStation = collectionItemContent.get("station").toString();
            final String collectionItemContentURL = collectionURL + "/" + collectionItemID + "/" + toSlug(collectionItemTitle);
            final String targetType = target.get("type").toString();
            final Map<String, Object> params = (Map<String, Object>) target.get("params");
            final ArrayList<DownloadLink> thisresults = new ArrayList<DownloadLink>();
            /*
             * If offline exception happens for one item here this seems to mean that the whole collection is offline. Website will then
             * simply redirect to a random other collection.
             */
            if (targetType.equalsIgnoreCase("podcast-episode")) {
                final DownloadLink broadcast = crawlPodcastEpisodeByGUID(params.get("guid").toString());
                thisresults.add(broadcast);
            } else if (targetType.equalsIgnoreCase("broadcastitem")) {
                final DownloadLink podcast = this.crawlBroadcastItem(collectionItemStation, params.get("id").toString());
                thisresults.add(podcast);
            } else if (targetType.equalsIgnoreCase("broadcast")) {
                thisresults.addAll(this.crawlBroadcast(collectionItemStation, params.get("id").toString()));
            } else if (targetType.equalsIgnoreCase("upload")) {
                final DownloadLink upload = this.crawlUpload(params.get("id").toString());
                thisresults.add(upload);
            } else {
                logger.warning("Unsupported targetType " + targetType + " for collection item: " + collectionItemContentURL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /*
             * Item can be linked directly or as part of a collection. In this case we want to present it to the user as part of a
             * collection as this is what he will get via browser too.
             */
            for (final DownloadLink thisresult : thisresults) {
                thisresult.setContentUrl(collectionItemContentURL);
                thisresult.setContainerUrl(collectionURL);
                thisresult.setAvailable(true);
                thisresult._setFilePackage(fp);
                distribute(thisresult);
                ret.add(thisresult);
            }
        }
        if (ret.isEmpty()) {
            /* Rare case: All items are offline? */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (numberofOfflineItems > 0) {
            logger.info("Skippen offline items: " + numberofOfflineItems);
        }
        return ret;
    }

    private DownloadLink crawlPodcastEpisodeByGUID(final String podcastGUID) throws IOException, PluginException {
        br.getPage(API_BASE + "/radiothek/api/2.0/episode/" + podcastGUID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
        final Map<String, Object> podcast = (Map<String, Object>) payload.get("podcast");
        final String author = podcast.get("author").toString();
        final String podcastEpisodeTitle = podcast.get("title").toString();
        String directurl = null;
        final List<Map<String, Object>> enclosures = (List<Map<String, Object>>) payload.get("enclosures");
        for (final Map<String, Object> enclosure : enclosures) {
            if (enclosure.get("type").toString().equals("audio/mpeg")) {
                directurl = enclosure.get("url").toString();
                break;
            }
        }
        if (StringUtils.isEmpty(directurl)) {
            /* Most likely unsupported streaming type */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String contentURL = (String) JavaScriptEngineFactory.walkJson(payload, "link/url");
        final DownloadLink link = createPodcastDownloadlink(directurl);
        if (!StringUtils.isEmpty(contentURL)) {
            link.setContentUrl(contentURL);
        }
        link.setProperty(PROPERTY_SLUG, podcast.get("slug"));
        final String dateStr = payload.get("published").toString();
        final String dateFormatted = new Regex(dateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String filename = dateFormatted + "_" + author + " - " + podcastEpisodeTitle + " - " + payload.get("title").toString() + ".mp3";
        link.setFinalFileName(filename);
        link.setProperty(DirectHTTP.FIXNAME, filename);
        final String description = (String) payload.get("description");
        if (!StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        link.setDownloadSize(calculateFilesize(((Number) payload.get("duration")).longValue()));
        link.setAvailable(true);
        return link;
    }

    private DownloadLink crawlBroadcastItem(final String radioStation, final String broadcastID) throws IOException, PluginException {
        if (radioStation == null || broadcastID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(API_BASE + "/" + radioStation + "/api/json/5.0/broadcastitem/" + broadcastID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
        final Map<String, Object> broadcast = (Map<String, Object>) payload.get("broadcast");
        final ArrayList<Map<String, Object>> streams = new ArrayList<Map<String, Object>>();
        streams.add((Map<String, Object>) payload.get("stream"));
        final ArrayList<DownloadLink> results = crawlProcessBroadcastItems(broadcast, streams);
        return results.get(0);
    }

    private ArrayList<DownloadLink> crawlBroadcast(final String radioStation, final String broadcastID) throws IOException, PluginException {
        if (radioStation == null || broadcastID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(API_BASE + "/" + radioStation + "/api/json/5.0/broadcast/" + broadcastID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> broadcast = (Map<String, Object>) resp.get("payload");
        return crawlProcessBroadcastItems(broadcast, (List<Map<String, Object>>) broadcast.get("streams"));
    }

    private ArrayList<DownloadLink> crawlProcessBroadcastItems(final Map<String, Object> broadcast, final List<Map<String, Object>> streams) throws IOException, PluginException {
        final String broadcastTitle = broadcast.get("title").toString();
        // final String broadcastSubtitle = (String) broadcast.get("subtitle");
        final String broadcastDescription = (String) broadcast.get("description");
        final String broadcastStartDate = broadcast.get("start").toString();
        final String broadcastStartDateFormatted = new Regex(broadcastStartDate, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String station = broadcast.get("station").toString();
        final String titleBase = station + " - " + broadcastTitle;
        final FilePackage fp = FilePackage.getInstance();
        final String packagenameBase = broadcastStartDateFormatted + "_" + titleBase;
        fp.setName(packagenameBase);
        if (!StringUtils.isEmpty(broadcastDescription)) {
            fp.setComment(broadcastDescription);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final int padLength = StringUtils.getPadLength(streams.size());
        int position = 1;
        for (final Map<String, Object> stream : streams) {
            final String streamStartDate = stream.get("start").toString();
            final String streamStartDateFormatted = new Regex(streamStartDate, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            String filenameBase = streamStartDateFormatted + "_" + titleBase;
            if (streams.size() > 1) {
                filenameBase += " " + StringUtils.formatByPadLength(padLength, position);
            }
            filenameBase += ".mp3";
            final Map<String, Object> urls = (Map<String, Object>) stream.get("urls");
            // final String originalFilename = stream.get("loopStreamId").toString();
            String downloadurl = urls.get("progressive").toString();
            /* Remove placeholders inside URL which we're not filling in. */
            downloadurl = downloadurl.replaceAll("\\{\\&[^\\}]+\\}", "");
            final DownloadLink link = this.createDownloadlink("directhttp://" + downloadurl);
            link.setFinalFileName(filenameBase);
            link.setProperty(DirectHTTP.FIXNAME, filenameBase);
            link.setDownloadSize(calculateFilesize(((Number) stream.get("duration")).longValue()));
            link.setAvailable(true);
            ret.add(link);
            position++;
        }
        return ret;
    }

    private DownloadLink crawlUpload(final String uploadID) throws IOException, PluginException {
        br.getPage(API_BASE + "/radiothek/api/2.0/upload/" + uploadID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
        if ((Boolean) payload.get("isOnline") == Boolean.FALSE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String moderator = (String) payload.get("moderator");
        final String uploadTitle = payload.get("title").toString();
        String directurl = null;
        final List<Map<String, Object>> enclosures = (List<Map<String, Object>>) payload.get("enclosures");
        for (final Map<String, Object> enclosure : enclosures) {
            if (enclosure.get("type").toString().equals("audio/mpeg")) {
                directurl = enclosure.get("url").toString();
                break;
            }
        }
        if (StringUtils.isEmpty(directurl)) {
            /* Most likely unsupported streaming type */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink link = createPodcastDownloadlink(directurl);
        link.setProperty(PROPERTY_SLUG, payload.get("slug"));
        final String dateStr = payload.get("postDate").toString();
        final String dateFormatted = new Regex(dateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        String filename = dateFormatted + "_" + payload.get("station");
        if (!StringUtils.isEmpty(moderator)) {
            filename += "_ " + moderator;
        }
        filename += " - " + uploadTitle + " - " + payload.get("title").toString() + ".mp3";
        link.setFinalFileName(filename);
        link.setProperty(DirectHTTP.FIXNAME, filename);
        final String description = (String) payload.get("description");
        if (!StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        link.setDownloadSize(calculateFilesize(((Number) payload.get("duration")).longValue()));
        link.setAvailable(true);
        return link;
    }

    private ArrayList<DownloadLink> crawlVideo(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final HashSet<String> dupes = new HashSet<String>();
        final String[] programIDs = br.getRegex("data-ppid=\"([a-f0-9\\-]+)\"").getColumn(0);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final String programID : programIDs) {
            if (dupes.add(programID)) {
                ret.addAll(crawlVideoProgramID(programID));
            }
        }
        return ret;
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlVideoProgramID(final String programID) throws Exception {
        if (StringUtils.isEmpty(programID)) {
            throw new IllegalArgumentException();
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getHeaders().put("Origin", "https://tv.orf.at");
        br.getHeaders().put("Referer", "https://tv.orf.at/");
        br.getPage("https://api-tvthek.orf.at/api/v4.2/public/content-by-dds-programplanguid/" + programID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> episode = (Map<String, Object>) entries.get("episode");
        if (episode == null) {
            /*
             * Item does not exist anymore or hasn't aired yet. Website may show a preview-image and a description but no streamable video
             * content.
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String encrypted_id = (String) episode.get("encrypted_id");
        if (!StringUtils.isEmpty(encrypted_id)) {
            /* Use new handling */
            return this.crawlOrfmediathekNewEncryptedID(encrypted_id, null);
        }
        final SubConfiguration cfg = SubConfiguration.getConfig("orf.at");
        // if(Boolean.TRUE.equals(episode.get("is_drm_protected"))) {
        //
        // }
        final String title = episode.get("title").toString();
        final String description = episode.get("description").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.setComment(description);
        /* There are multiple HLS sources available. Looks like mirrors. */
        final Map<String, Object> hlsmap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(episode, "sources/hls/{0}");
        final String hlsMaster = hlsmap.get("src").toString();
        final Browser brc = br.cloneBrowser();
        brc.getPage(hlsMaster);
        final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(brc);
        for (final HlsContainer hlsContainer : hlsContainers) {
            final DownloadLink video = this.createDownloadlink(hlsContainer.getDownloadurl());
            video.setFinalFileName(title + "_" + hlsContainer.getHeight() + "p.mp4");
            ret.add(video);
        }
        final String thumbnailurl = (String) episode.get("related_audiodescription_episode_image_url");
        if (thumbnailurl != null && cfg.getBooleanProperty(ORFMediathek.Q_THUMBNAIL, ORFMediathek.Q_THUMBNAIL_default)) {
            final DownloadLink thumbnail = this.createDownloadlink(thumbnailurl);
            thumbnail.setFinalFileName(title + Plugin.getFileNameExtensionFromURL(thumbnailurl));
            thumbnail.setAvailable(true);
            ret.add(thumbnail);
        }
        final Map<String, Object> subtitlemap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(episode, "_embedded/subtitle");
        final String subtitleURL = subtitlemap != null ? (String) subtitlemap.get("srt_url") : null;
        if (!StringUtils.isEmpty(subtitleURL)) {
            final DownloadLink subtitle = this.createDownloadlink(subtitleURL);
            subtitle.setFinalFileName(title + ".srt");
            subtitle.setAvailable(true);
            ret.add(subtitle);
        }
        fp.addLinks(ret);
        return ret;
    }

    private String toSlug(final String str) {
        final String preparedSlug = str.toLowerCase(Locale.ENGLISH).replace("ü", "u").replace("ä", "a").replace("ö", "o");
        String slug = preparedSlug.replaceAll("[^a-z0-9]", "-");
        /* Remove double-minus */
        slug = slug.replaceAll("-{2,}", "-");
        /* Do not begin with minus */
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        /* Do not end with minus */
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug;
    }

    /** Calculates filesizes on the assumption that audio is delivered with quality 192kb/s. */
    private long calculateFilesize(final long durationMilliseconds) {
        final long durationSeconds = durationMilliseconds / 1000;
        return (durationSeconds * 192 * 1024) / 8;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
