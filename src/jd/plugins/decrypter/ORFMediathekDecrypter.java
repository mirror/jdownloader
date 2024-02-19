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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ORFMediathek;

// http://tvthek,orf.at/live/... --> HDS
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tvthek.orf.at" }, urls = { "https?://tvthek22\\.orf\\.at/(?:index\\.php/)?(?:programs?|topic|profile)/.+" })
public class ORFMediathekDecrypter extends PluginForDecrypt {
    private static final String TYPE_TOPIC    = "https?://tvthek\\.orf\\.at/topic/.+";
    private static final String TYPE_PROGRAMM = "https?://tvthek\\.orf\\.at/programs?/.+";

    public ORFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.getCryptedUrl().replace("/index.php/", "/");
        this.br.setAllowedResponseCodes(500);
        this.br.setLoadLimit(this.br.getLoadLimit() * 4);
        br.getPage(parameter);
        int status = br.getHttpConnection().getResponseCode();
        if (status == 301 || status == 302) {
            br.setFollowRedirects(true);
            if (br.getRedirectLocation() != null) {
                parameter = br.getRedirectLocation();
                br.getPage(parameter);
            }
        } else if (status != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("(404 \\- Seite nicht gefunden\\.|area_headline error_message\">Keine Sendung vorhanden<)") || !br.containsHTML("jsb_VideoPlaylist") || status == 404 || status == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        ret.addAll(getDownloadLinks(param));
        return ret;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private ArrayList<DownloadLink> getDownloadLinks(final CryptedLink param) throws Exception {
        final String sourceurl = param.getCryptedUrl();
        String json = this.br.getRegex("class=\"jsb_ jsb_VideoPlaylist\" data\\-jsb=\"([^<>\"]+)\"").getMatch(0);
        /* jsonData --> HashMap */
        json = Encoding.htmlOnlyDecode(json);
        final Map<String, Object> root = restoreFromString(json, TypeRef.MAP);
        /* In this context a playlist is mostly a single video split into multiple parts/chapters. */
        final Map<String, Object> playlist = (Map<String, Object>) root.get("playlist");
        final String contentIDSlashPlaylistIDSlashVideoID = playlist.get("id").toString();
        final Map<String, Object> gapless_video = (Map<String, Object>) playlist.get("gapless_video");
        final List<Map<String, Object>> segments = (List<Map<String, Object>>) playlist.get("videos");
        final String titleOfPlaylistOrVideo = playlist.get("title").toString();
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
        boolean allow_HTTP = cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.HTTP_STREAM, true);
        final boolean allow_HDS = cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.HDS_STREAM, true);
        final boolean allow_HLS = cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.HLS_STREAM, true);
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
        if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_VERYLOW, true)) {
            selectedQualities.add("VERYLOW");
        }
        if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_LOW, true)) {
            selectedQualities.add("LOW");
        }
        if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_MEDIUM, true)) {
            selectedQualities.add("MEDIUM");
        }
        if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_HIGH, true)) {
            selectedQualities.add("HIGH");
        }
        if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_VERYHIGH, true)) {
            selectedQualities.add("VERYHIGH");
        }
        final ORFMediathek hosterplugin = (ORFMediathek) this.getNewPluginForHostInstance("orf.at");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(playlist.get("title").toString());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int videoPosition = 0;
        final String thumbnailurlGapless = playlist.get("preview_image_url").toString();
        for (final Map<String, Object> segment : segments) {
            videoPosition++;
            final Object segmentID_O = segment.get("id");
            final String segmentID;
            if (segmentID_O != null) {
                segmentID = segmentID_O.toString();
            } else {
                /* Gapless videos only have one segment thus that segment may not have an ID. */
                segmentID = "gapless";
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
                link.setContentUrl(sourceurl);
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
            /* Collrect thumbnail results */
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
            if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_BEST, true)) {
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
            if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_SUBTITLES, false) && !StringUtils.isEmpty(subtitleurl)) {
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
            if (cfg.getBooleanProperty(jd.plugins.hoster.ORFMediathek.Q_THUMBNAIL, false) && !StringUtils.isEmpty(thumbnailurl)) {
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

    private String humanReadableQualityIdentifier(final String quality) {
        return OrfAt.humanReadableQualityIdentifier(quality);
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}