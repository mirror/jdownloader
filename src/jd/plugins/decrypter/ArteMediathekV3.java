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
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.ArteMediathekConfig;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.FilenameSchemeType;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.LanguageSelectionMode;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.PackagenameSchemeType;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.QualitySelectionFallbackMode;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.QualitySelectionMode;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.ThumbnailFilenameMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ArteTv;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 4, names = {}, urls = {})
public class ArteMediathekV3 extends PluginForDecrypt {
    public ArteMediathekV3(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        prepBRAPI(br);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "arte.tv", "arte.fr" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z]{2}/videos/\\d+-\\d+-[A-Z]+(/([a-z0-9\\-]+)/?)?|embeds/[a-z]{2}/\\d+-\\d+-[A-Z]+)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String API_BASE                    = "https://api.arte.tv/api/opa/v3";
    private final String        PROPERTY_VIDEO_ID           = "video_id";
    private final String        PROPERTY_TITLE              = "title";
    private final String        PROPERTY_SUBTITLE           = "subtitle";
    private final String        PROPERTY_TITLE_AND_SUBTITLE = "title_and_subtitle";
    private final String        PROPERTY_DATE               = "date";
    private final String        PROPERTY_ORIGINAL_FILENAME  = "original_filename";
    private final String        PROPERTY_AUDIO_CODE         = "audio_code";                                                             // ex
                                                                                                                                        // versionCode
                                                                                                                                        // e.g.
                                                                                                                                        // VF,
                                                                                                                                        // VF-STA,
                                                                                                                                        // VA
    private final String        PROPERTY_AUDIO_SHORT_LABEL  = "audioShortLabel";                                                        // e.g.
                                                                                                                                        // DE,
                                                                                                                                        // FR
    private final String        PROPERTY_AUDIO_LABEL        = "audioLabel";                                                             // e.g.
                                                                                                                                        // Deutsch,
                                                                                                                                        // Französisch
    private final String        PROPERTY_WIDTH              = "width";
    private final String        PROPERTY_HEIGHT             = "height";
    private final String        PROPERTY_BITRATE            = "bitrate";
    private final String        PROPERTY_PLATFORM           = "platform";
    private final String        PROPERTY_TYPE               = "type";
    private final String        TYPE_NORMAL                 = "https?://[^/]+/([a-z]{2})/videos/(\\d+-\\d+-[A-Z]+)(/([a-z0-9\\-]+)/?)?";
    private final String        TYPE_EMBED                  = "https?://[^/]+/embeds/([a-z]{2})/(\\d+-\\d+-[A-Z]+)";

    private static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer Nzc1Yjc1ZjJkYjk1NWFhN2I2MWEwMmRlMzAzNjI5NmU3NWU3ODg4ODJjOWMxNTMxYzEzZGRjYjg2ZGE4MmIwOA");
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        return crawlPrograms(param);
    }

    private ArrayList<DownloadLink> crawlPrograms(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArteMediathekConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final List<Integer> selectedQualitiesHeight = getSelectedHTTPQualities();
        if (selectedQualitiesHeight.isEmpty() && cfg.getQualitySelectionFallbackMode() == QualitySelectionFallbackMode.NONE) {
            logger.info("User has deselected all qualities and set QualitySelectionFallbackMode to QualitySelectionFallbackMode.NONE --> Doing nothing");
            return ret;
        }
        final String urlLanguage = getUrlLanguage(param.getCryptedUrl());
        final String contentID = gerUrlContentID(param.getCryptedUrl());
        prepBRAPI(br);
        /* API will return all results in german or french depending on 'urlLanguage'! */
        br.getPage(API_BASE + "/programs/" + urlLanguage + "/" + contentID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object errorsO = entries.get("errors");
        if (errorsO != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Map<String, Object>> programs = (List<Map<String, Object>>) entries.get("programs");
        PluginException lastException = null;
        for (final Map<String, Object> program : programs) {
            try {
                ret.addAll(this.crawlProgram(param, program));
            } catch (final PluginException ex) {
                lastException = ex;
            }
        }
        if (ret.isEmpty() && lastException != null) {
            throw lastException;
        }
        return ret;
    }

    private String getUrlLanguage(final String url) {
        if (url.matches(TYPE_NORMAL)) {
            return new Regex(url, TYPE_NORMAL).getMatch(0);
        } else {
            return new Regex(url, TYPE_EMBED).getMatch(0);
        }
    }

    private String gerUrlContentID(final String url) {
        if (url.matches(TYPE_NORMAL)) {
            return new Regex(url, TYPE_NORMAL).getMatch(1);
        } else {
            return new Regex(url, TYPE_EMBED).getMatch(1);
        }
    }

    private ArrayList<DownloadLink> crawlProgram(final CryptedLink param, final Map<String, Object> program) throws IOException, PluginException {
        final Map<String, Object> availability = (Map<String, Object>) program.get("availability");
        if (availability == null) {
            /* Message in browser should be "No video available" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Boolean hasVideoStreams = (Boolean) availability.get("hasVideoStreams");
        // final Object broadcastBegin = availability.get("broadcastBegin");
        // broadcastBegin can be null even for available videos
        if (!Boolean.TRUE.equals(hasVideoStreams)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> vid = (Map<String, Object>) program.get("mainVideo");
        return crawlVideo(param, vid);
    }

    private ArrayList<DownloadLink> crawlVideo(final CryptedLink param, final Map<String, Object> vid) throws IOException, PluginException {
        final String kind = vid.get("kind").toString();
        if (!kind.matches("(PROGRAMM|SHOW)")) {
            if (kind.equals("LIVE")) {
                logger.info("Livestreams are not supported");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.info("Unknown kind Label: " + kind);
        }
        final ArteTv hosterplugin = (ArteTv) this.getNewPluginForHostInstance(this.getHost());
        final String programId = vid.get("programId").toString();
        final String title = vid.get("title").toString();
        final String subtitle = (String) vid.get("subtitle");
        final String date;
        final Object firstBroadcastDateO = vid.get("firstBroadcastDate");
        if (firstBroadcastDateO != null) {
            date = firstBroadcastDateO.toString();
        } else {
            date = vid.get("creationDate").toString();
        }
        final String language = vid.get("language").toString();
        final String dateFormatted = new Regex(date, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String shortDescription = (String) vid.get("shortDescription");
        final String platform = vid.get("platform").toString();
        String videoStreamsAPIURL = (String) JavaScriptEngineFactory.walkJson(vid, "links/videoStreams/web/href");
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            videoStreamsAPIURL = API_BASE + "/videoStreams?programId=" + programId + "&limit=100&language=" + language + "&protocol=HTTPS&kind=SHOW&reassembly=A";
        }
        String titleAndSubtitle = title;
        if (!StringUtils.isEmpty(subtitle)) {
            titleAndSubtitle += " - " + subtitle;
        }
        prepBRAPI(br);
        br.getPage(videoStreamsAPIURL);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final List<Map<String, Object>> videoStreams = (List<Map<String, Object>>) entries.get("videoStreams");
        if (videoStreams == null || videoStreams.isEmpty()) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* First put each language + audio version in separate lists. */
        final Map<String, List<Map<String, Object>>> languagePacks = new HashMap<String, List<Map<String, Object>>>();
        for (final Map<String, Object> videoStream : videoStreams) {
            final String audioCode = videoStream.get("audioCode").toString();
            if (!languagePacks.containsKey(audioCode)) {
                languagePacks.put(audioCode, new ArrayList<Map<String, Object>>());
            }
            languagePacks.get(audioCode).add(videoStream);
        }
        final ArteMediathekConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        /* Build package name */
        final PackagenameSchemeType schemeType = cfg.getPackagenameSchemeType();
        String customPackagenameScheme = cfg.getPackagenameScheme();
        String packageName;
        if (schemeType == PackagenameSchemeType.CUSTOM && !StringUtils.isEmpty(customPackagenameScheme)) {
            packageName = customPackagenameScheme;
        } else if (schemeType == PackagenameSchemeType.LEGACY) {
            packageName = "*date*_arte_*title_and_subtitle*";
        } else {
            /* PackagenameSchemeType.DEFAULT or fallback */
            packageName = "*title_and_subtitle*";
        }
        packageName = packageName.replace("*date*", dateFormatted);
        packageName = packageName.replace("*platform*", platform);
        packageName = packageName.replace("*video_id*", programId);
        packageName = packageName.replace("*title*", title);
        packageName = packageName.replace("*subtitle*", subtitle != null ? subtitle : "");
        packageName = packageName.replace("*title_and_subtitle*", titleAndSubtitle);
        final FilePackage fp = FilePackage.getInstance();
        fp.setCleanupPackageName(false);
        fp.setName(packageName);
        if (!StringUtils.isEmpty(shortDescription)) {
            fp.setComment(shortDescription);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final QualitySelectionMode mode = cfg.getQualitySelectionMode();
        /* Crawl video streams */
        /* Collect list of user desired/allowed qualities */
        final List<Integer> selectedQualitiesHeight = getSelectedHTTPQualities();
        final String langFromURLStr = getUrlLanguage(param.getCryptedUrl());
        if (langFromURLStr == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Language langFromURL = iso6391CodeToLanguageEnum(langFromURLStr);
        final List<Language> selectedLanguages = new ArrayList<Language>();
        if (cfg.getLanguageSelectionMode() == LanguageSelectionMode.ALL_SELECTED) {
            if (cfg.isCrawlLanguageEnglish()) {
                selectedLanguages.add(Language.ENGLISH);
            }
            if (cfg.isCrawlLanguageFrench()) {
                selectedLanguages.add(Language.FRENCH);
            }
            if (cfg.isCrawlLanguageGerman()) {
                selectedLanguages.add(Language.GERMAN);
            }
            if (cfg.isCrawlLanguageItalian()) {
                selectedLanguages.add(Language.ITALIAN);
            }
            if (cfg.isCrawlLanguagePolish()) {
                selectedLanguages.add(Language.POLISH);
            }
            if (cfg.isCrawlLanguageUnknown()) {
                selectedLanguages.add(Language.OTHER);
            }
        } else {
            /* Language by URL */
            selectedLanguages.add(langFromURL);
        }
        final HashSet<Language> existingLanguages = new HashSet<Language>();
        /* Look ahead and collect existing languages */
        for (final List<Map<String, Object>> videoStreamsByLanguage : languagePacks.values()) {
            for (final Map<String, Object> videoStream : videoStreamsByLanguage) {
                final String audioCode = videoStream.get("audioCode").toString();
                final VersionInfo versionInfo = parseVersionInfo(audioCode);
                existingLanguages.add(versionInfo.getAudioLanguage());
            }
        }
        final QualitySelectionFallbackMode qualitySelectionFallbackMode = cfg.getQualitySelectionFallbackMode();
        /*
         * Now filter by language, user selected qualities and so on. User can e.g. select multiple languages and best video quality of each
         * combined with subtitle preferences.
         */
        final ArrayList<DownloadLink> allResults = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> allBestResults = new ArrayList<DownloadLink>();
        for (final List<Map<String, Object>> videoStreamsByLanguage : languagePacks.values()) {
            final ArrayList<DownloadLink> userSelected = new ArrayList<DownloadLink>();
            long bestFilesize = 0;
            DownloadLink best = null;
            long bestAllowedFilesize = 0;
            DownloadLink bestAllowed = null;
            long bestOfUserSelectedFilesize = 0;
            DownloadLink bestOfUserSelected = null;
            for (final Map<String, Object> videoStream : videoStreamsByLanguage) {
                // final String language = videoStream.get("language").toString();
                final String protocol = videoStream.get("protocol").toString();
                if (!protocol.equalsIgnoreCase("https")) {
                    /* 2022-05-25: Only grab HTTP streams for now, skip all others */
                    continue;
                }
                final String videoStreamId = videoStream.get("videoStreamId").toString();
                final int height = ((Number) videoStream.get("height")).intValue();
                final int durationSeconds = ((Number) videoStream.get("durationSeconds")).intValue();
                final int bitrate = ((Number) videoStream.get("bitrate")).intValue();
                final String audioCode = videoStream.get("audioCode").toString(); // e.g. VF, VF-STA, VA, ...
                final DownloadLink link = new DownloadLink(hosterplugin, hosterplugin.getHost(), videoStream.get("url").toString(), true);
                link.setProperty(PROPERTY_TYPE, "video");
                /* Set properties which we later need for custom filenames. */
                link.setProperty(PROPERTY_VIDEO_ID, programId);
                link.setProperty(PROPERTY_TITLE, title);
                if (!StringUtils.isEmpty(PROPERTY_SUBTITLE)) {
                    link.setProperty(PROPERTY_SUBTITLE, subtitle);
                }
                link.setProperty(PROPERTY_TITLE_AND_SUBTITLE, titleAndSubtitle);
                link.setProperty(PROPERTY_DATE, dateFormatted);
                link.setProperty(PROPERTY_WIDTH, videoStream.get("width"));
                link.setProperty(PROPERTY_HEIGHT, height);
                link.setProperty(PROPERTY_BITRATE, bitrate);
                link.setProperty(PROPERTY_AUDIO_CODE, audioCode);
                link.setProperty(PROPERTY_AUDIO_SHORT_LABEL, videoStream.get("audioShortLabel"));
                link.setProperty(PROPERTY_AUDIO_LABEL, videoStream.get("audioLabel"));
                link.setProperty(PROPERTY_PLATFORM, platform);
                link.setProperty(PROPERTY_ORIGINAL_FILENAME, videoStream.get("filename"));
                final VersionInfo versionInfo = parseVersionInfo(audioCode);
                /* Do not modify those linkIDs to try to keep backward compatibility! remove the -[A-Z] to be same as old vpi */
                link.setContentUrl(param.getCryptedUrl());
                link.setProperty(DirectHTTP.PROPERTY_CUSTOM_HOST, getHost());
                link.setLinkID(getHost() + "://" + new Regex(programId, "(\\d+-\\d+)").getMatch(0) + "/" + versionInfo.toString() + "/" + "http_" + bitrate);
                /* Get filename according to users' settings. */
                final String filename = this.getAndSetFilename(link);
                /* Make sure that our directHTTP plugin will never change this filename. */
                link.setProperty(DirectHTTP.FIXNAME, filename);
                link.setAvailable(true);
                /* Calculate filesize in a very simple way */
                link.setDownloadSize(bitrate / 8 * 1024 * durationSeconds);
                link._setFilePackage(fp);
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    link.setComment(videoStreamId.toString());
                }
                allResults.add(link);
                /* Try to find the best version regardless of user settings. */
                if (link.getView().getBytesTotal() > bestFilesize || best == null) {
                    best = link;
                    bestFilesize = link.getView().getBytesTotal();
                }
                /* Now go through all other skip conditions based on user settings except video resolution */
                /*
                 * Skip subtitled versions if not wished by user. This needs to happen before BEST selection otherwise subtitled versions
                 * would still be incorperated in BEST selection which would be wrong.
                 */
                if (videoStreamsByLanguage.size() > 1) {
                    /* Only even allow to skip items if there are more than two versions */
                    if (versionInfo.hasAnySubtitle()) {
                        if (versionInfo.hasSubtitleAudioDescription() && !cfg.isCrawlSubtitledBurnedInVersionsAudioDescription()) {
                            continue;
                        } else if (versionInfo.hasSubtitleFull() && !cfg.isCrawlSubtitledBurnedInVersionsFull()) {
                            continue;
                        } else if (versionInfo.hasSubtitlePartial() && !cfg.isCrawlSubtitledBurnedInVersionsPartial()) {
                            continue;
                        } else if (versionInfo.hasSubtitleForHearingImpaired() && !cfg.isCrawlSubtitledBurnedInVersionsHearingImpaired()) {
                            continue;
                        }
                    }
                    if (cfg.getLanguageSelectionMode() == LanguageSelectionMode.ALL_SELECTED) {
                        /* Allow only selected languages, skip the rest */
                        if (!selectedLanguages.contains(versionInfo.getAudioLanguage())) {
                            /* Skip unwanted languages */
                            logger.info("Skipping videoStreamId because of unselected language: " + versionInfo.getAudioLanguage() + " | videoStreamId: " + videoStreamId);
                            continue;
                        }
                    } else {
                        /* Allow only language by URL and original version, skip the rest */
                        if (existingLanguages.size() > 1 && versionInfo.getAudioLanguage() != langFromURL) {
                            /* Multiple languages available --> Allow "stupid" filtering by language / skip unwanted languages */
                            logger.info("Skipping videoStreamId because of unselected language: " + versionInfo.getAudioLanguage() + " | videoStreamId: " + videoStreamId);
                            continue;
                        } else if (versionInfo.getAudioLanguage() != langFromURL && !versionInfo.isOriginalVersion()) {
                            /* Skip unwanted languages */
                            logger.info("Skipping videoStreamId: " + videoStreamId);
                            continue;
                        }
                    }
                }
                /* Collect best quality (regardless of user preferred video resolution config) */
                if (link.getView().getBytesTotal() > bestAllowedFilesize || bestAllowed == null) {
                    bestAllowed = link;
                    bestAllowedFilesize = link.getView().getBytesTotal();
                }
                /* Check if user wants this video resolution. */
                final int heightForQualitySelection = getHeightForQualitySelection(height);
                if (selectedQualitiesHeight.contains(heightForQualitySelection) || (!knownQualitiesHeight.contains(heightForQualitySelection) && cfg.isCrawlUnknownHTTPVideoQualities())) {
                    userSelected.add(link);
                    if (link.getView().getBytesTotal() > bestOfUserSelectedFilesize || bestOfUserSelected == null) {
                        bestOfUserSelected = link;
                        bestOfUserSelectedFilesize = link.getView().getBytesTotal();
                    }
                }
            }
            /* Collect current best result as we might need this for fallback later. */
            allBestResults.add(best);
            /* Decide what to add based on users' settings. */
            final ArrayList<DownloadLink> finalSelection = new ArrayList<DownloadLink>();
            if (mode == QualitySelectionMode.BEST) {
                if (bestAllowed != null) {
                    finalSelection.add(best);
                } else {
                    logger.info("Failed to find bestAllowed -> User might have bad plugin settings");
                }
            } else if (mode == QualitySelectionMode.BEST_OF_SELECTED) {
                /*
                 * Can be null if user has bad settings e.g. deselected all qualities or only selected qualities which are not available for
                 * currently processed video.
                 */
                if (bestOfUserSelected != null) {
                    finalSelection.add(bestOfUserSelected);
                } else {
                    logger.info("Failed to find bestOfUserSelected -> User might have bad plugin settings");
                }
            } else {
                // ALL_SELECTED
                finalSelection.addAll(userSelected);
            }
            ret.addAll(finalSelection);
        }
        if (ret.isEmpty()) {
            /* No results based on user selection --> Fallback */
            logger.info("Found no results based on user selection --> Using fallback");
            if (qualitySelectionFallbackMode == QualitySelectionFallbackMode.BEST && !allBestResults.isEmpty()) {
                ret.addAll(allBestResults);
            } else if (qualitySelectionFallbackMode == QualitySelectionFallbackMode.ALL) {
                ret.addAll(allResults);
            } else if (qualitySelectionFallbackMode == QualitySelectionFallbackMode.NONE) {
                // Return nothing
            } else {
                // Developer mistake
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        logger.info("Adding video results " + ret.size() + "/" + allResults.size());
        /* Crawl thumbnail if wished by user */
        if (cfg.isCrawlThumbnail()) {
            final Map<String, Object> mainImage = (Map<String, Object>) vid.get("mainImage");
            final String imageCaption = (String) mainImage.get("caption");
            final DownloadLink thumbnail = new DownloadLink(hosterplugin, hosterplugin.getHost(), mainImage.get("url").toString(), true);
            thumbnail.setProperty(PROPERTY_TYPE, "thumbnail");
            thumbnail.setContentUrl(param.getCryptedUrl());
            thumbnail.setProperty(DirectHTTP.PROPERTY_CUSTOM_HOST, getHost());
            final String extension = mainImage.get("extension").toString();
            final ThumbnailFilenameMode thumbnailFilenameMode = cfg.getThumbnailFilenameMode();
            if (ret.size() == 1 && thumbnailFilenameMode == ThumbnailFilenameMode.AUTO) {
                /* Only one video result --> Use same filename as that result for thumbnail. */
                final String filenameOfTheOnlyAddedVideo = ret.get(0).getFinalFileName();
                thumbnail.setFinalFileName(filenameOfTheOnlyAddedVideo.substring(0, filenameOfTheOnlyAddedVideo.lastIndexOf(".")) + "." + extension);
            } else {
                final FilenameSchemeType filenameSchemeType = cfg.getFilenameSchemeTypeV2();
                if (filenameSchemeType == FilenameSchemeType.ORIGINAL) {
                    thumbnail.setFinalFileName(mainImage.get("name").toString());
                } else {
                    thumbnail.setFinalFileName(packageName + "." + extension);
                }
                if (!StringUtils.isEmpty(imageCaption)) {
                    thumbnail.setComment(imageCaption);
                }
            }
            thumbnail.setAvailable(true);
            thumbnail._setFilePackage(fp);
            ret.add(thumbnail);
            distribute(thumbnail);
        }
        if (ret.isEmpty()) {
            /* This should never happen! */
            logger.warning("Failed to find any results");
        } else {
            for (final DownloadLink result : ret) {
                distribute(result);
            }
        }
        return ret;
    }

    private String getAndSetFilename(final DownloadLink link) {
        String filename;
        final ArteMediathekConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final FilenameSchemeType schemeType = cfg.getFilenameSchemeTypeV2();
        String customFilenameScheme = cfg.getFilenameScheme();
        if (schemeType == FilenameSchemeType.CUSTOM && !StringUtils.isEmpty(customFilenameScheme)) {
            /* User customized filename scheme */
            /* Legacy compatibility for old arte config/crawler version 46182 */
            customFilenameScheme = customFilenameScheme.replace("*vpi*__*language*", "*vpi*_*language*"); // fix old mistake: one
            // underscore too much
            customFilenameScheme = customFilenameScheme.replace("*vpi*", "*video_id*"); // update changed tag name
            customFilenameScheme = customFilenameScheme.replace("*title*", "*title_and_subtitle*"); // update changed tag name
            filename = customFilenameScheme;
        } else if (schemeType == FilenameSchemeType.ORIGINAL) {
            filename = "*original_filename*";
        } else if (schemeType == FilenameSchemeType.LEGACY) {
            filename = "*date*_arte_*title_and_subtitle*_*video_id*_*language*_*shortlanguage*_*resolution*_*bitrate**ext*";
        } else {
            /* FilenameSchemeType.DEFAULT and fallback */
            filename = "*date*_*platform*_*title_and_subtitle*_*video_id*_*language*_*shortlanguage*_*resolution*_*bitrate**ext*";
        }
        final String width = link.getStringProperty(PROPERTY_WIDTH);
        final String height = link.getStringProperty(PROPERTY_HEIGHT);
        filename = filename.replace("*video_id*", link.getStringProperty(PROPERTY_VIDEO_ID));
        filename = filename.replace("*platform*", link.getStringProperty(PROPERTY_PLATFORM));
        filename = filename.replace("*date*", link.getStringProperty(PROPERTY_DATE));
        filename = filename.replace("*shortlanguage*", link.getStringProperty(PROPERTY_AUDIO_SHORT_LABEL));
        filename = filename.replace("*language*", link.getStringProperty(PROPERTY_AUDIO_LABEL));
        filename = filename.replace("*width*", width);
        filename = filename.replace("*height*", height);
        filename = filename.replace("*resolution*", width + "x" + height);
        filename = filename.replace("*bitrate*", link.getStringProperty(PROPERTY_BITRATE));
        filename = filename.replace("*original_filename*", link.getStringProperty(PROPERTY_ORIGINAL_FILENAME));
        filename = filename.replace("*ext*", ".mp4");
        filename = filename.replace("*title*", link.getStringProperty(PROPERTY_TITLE));
        filename = filename.replace("*subtitle*", link.getStringProperty(PROPERTY_SUBTITLE, ""));
        filename = filename.replace("*title_and_subtitle*", link.getStringProperty(PROPERTY_TITLE_AND_SUBTITLE));
        link.setFinalFileName(filename);
        return filename;
    }

    private final List<Integer> knownQualitiesHeight = Arrays.asList(new Integer[] { 1080, 720, 480, 360, 240 });

    private List<Integer> getSelectedHTTPQualities() {
        final ArteMediathekConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final List<Integer> selectedQualitiesHeight = new ArrayList<Integer>();
        if (cfg.isCrawlHTTP1080p()) {
            selectedQualitiesHeight.add(knownQualitiesHeight.get(0));
        }
        if (cfg.isCrawlHTTP720p()) {
            selectedQualitiesHeight.add(knownQualitiesHeight.get(1));
        }
        if (cfg.isCrawlHTTP480p()) {
            selectedQualitiesHeight.add(knownQualitiesHeight.get(2));
        }
        if (cfg.isCrawlHTTP360p()) {
            selectedQualitiesHeight.add(knownQualitiesHeight.get(3));
        }
        if (cfg.isCrawlHTTP240p()) {
            selectedQualitiesHeight.add(knownQualitiesHeight.get(4));
        }
        return selectedQualitiesHeight;
    }

    /*
     * Height of ARTE videos may vary but we only got a fixed quality selection for our users e.g. they can have 364x216 -> We'd consider
     * this to be 240p for quality selection handling.
     */
    int getHeightForQualitySelection(final int height) {
        if (height > 180 && height <= 300) {
            return 240;
        } else if (height > 300 && height <= 400) {
            return 360;
        } else if (height > 400 && height <= 600) {
            return 480;
        } else if (height > 600 && height <= 900) {
            return 720;
        } else if (height > 900 && height <= 1200) {
            return 1080;
        } else {
            /* No fitting pre given height --> Return input */
            return height;
        }
    }

    private static Language iso6391CodeToLanguageEnum(final String iso6391Code) {
        if (iso6391Code.equalsIgnoreCase("en")) {
            return Language.ENGLISH;
        } else if (iso6391Code.equalsIgnoreCase("fr")) {
            return Language.FRENCH;
        } else if (iso6391Code.equalsIgnoreCase("de")) {
            return Language.GERMAN;
        } else if (iso6391Code.equalsIgnoreCase("it")) {
            return Language.ITALIAN;
        } else if (iso6391Code.equalsIgnoreCase("pl")) {
            return Language.POLISH;
        } else if (iso6391Code.equalsIgnoreCase("es")) {
            return Language.SPANISH;
        } else {
            /* Unknown/unsupported code */
            return Language.OTHER;
        }
    }

    private static enum Language {
        ENGLISH,
        FRENCH,
        GERMAN,
        ITALIAN,
        SPANISH,
        POLISH,
        OTHER;
    }

    private static Language parseAudioLanguage(final String apiosCode) {
        final String languageChar = new Regex(apiosCode, "^VO?([A-Z])").getMatch(0);
        if (languageChar != null) {
            if ("F".equals(languageChar)) {
                return Language.FRENCH;
            } else if ("A".equals(languageChar)) {
                return Language.GERMAN;
            } else {
                /**
                 * .g. VOEU -> Original version which usually comes with foreign subtitle -> It's okay to return Language.OTHER for such
                 * items!
                 */
                return Language.OTHER;
            }
        }
        return Language.OTHER;
    }

    private static Language parseSubtitleLanguage(final String apiosCode) {
        final String subtitleLanguage = new Regex(apiosCode, "-STM?(A|F)").getMatch(0);
        if (subtitleLanguage != null) {
            if ("F".equals(subtitleLanguage)) {
                return Language.FRENCH;
            } else if ("A".equals(subtitleLanguage)) {
                return Language.GERMAN;
            } else {
                return Language.OTHER;
            }
        } else if (apiosCode.endsWith("[ANG]")) {
            return Language.ENGLISH;
        } else if (apiosCode.endsWith("[ITA]")) {
            return Language.ITALIAN;
        } else if (apiosCode.endsWith("[POL]")) {
            return Language.POLISH;
        } else if (apiosCode.endsWith("[ESP]")) {
            return Language.SPANISH;
        } else {
            return Language.OTHER;
        }
    }

    private static enum SubtitleType {
        NONE,
        /**
         * Audio description is not a real subtitle by defintion(?) but since the audio is part of the video and it is used as "subtitle
         * replacement" it makes sense to put it here.
         */
        AUDIO_DESCRIPTION,
        FULL,
        PARTIAL,
        HEARING_IMPAIRED;

        private static SubtitleType parse(final String apiosCode) {
            if (apiosCode.matches(".*?AUD.*?")) {
                /* E.g. "VFAUD" or "VAAUD" */
                return AUDIO_DESCRIPTION;
            } else if (apiosCode.matches("[A-Z]+-ST(A|F)")) {
                /* Forced subtitles e.g. parts of original film got foreign language -> Those parts are subtitled */
                return PARTIAL;
            } else if (apiosCode.matches("[A-Z]+-STMA?.*?")) {
                /* Subtitle for hearing impaired ppl */
                return HEARING_IMPAIRED;
            } else if (apiosCode.matches("[A-Z]+-STE?.*?")) {
                /* Normal subtitles */
                return FULL;
            } else {
                /* No subtitles */
                return NONE;
            }
        }
    }

    private static enum VersionType {
        ORIGINAL,
        ORIGINAL_FRANCAIS,
        ORIGINAL_GERMAN,
        NON_ORIGINAL_FRANCAIS,
        NON_ORIGINAL_GERMAN,
        FOREIGN;

        private static VersionType parse(final String apiosCode) {
            if (StringUtils.startsWithCaseInsensitive(apiosCode, "VOF")) {
                return ORIGINAL_FRANCAIS;
            } else if (StringUtils.startsWithCaseInsensitive(apiosCode, "VOA")) {
                return ORIGINAL_GERMAN;
            } else if (StringUtils.startsWithCaseInsensitive(apiosCode, "VA-") || StringUtils.equalsIgnoreCase(apiosCode, "VA")) {
                return NON_ORIGINAL_GERMAN;
            } else if (StringUtils.startsWithCaseInsensitive(apiosCode, "VF-") || StringUtils.equalsIgnoreCase(apiosCode, "VF")) {
                return NON_ORIGINAL_FRANCAIS;
            } else if (StringUtils.startsWithCaseInsensitive(apiosCode, "VO-") || StringUtils.equalsIgnoreCase(apiosCode, "VO")) {
                return ORIGINAL;
            } else {
                return FOREIGN;
            }
        }
    }

    public static interface VersionInfo {
        VersionType getVersionType();

        Language getAudioLanguage();

        Language getAudioLanguageByVersionType();

        Language getSubtitleLanguage();

        SubtitleType getSubtitleType();

        boolean hasSubtitleAudioDescription();

        boolean hasAnySubtitle();

        boolean hasSubtitleFull();

        boolean hasSubtitlePartial();

        boolean hasSubtitleForHearingImpaired();

        boolean isOriginalVersion();
    }

    public static VersionInfo parseVersionInfo(final String apiosCode) {
        final SubtitleType subtitleType = SubtitleType.parse(apiosCode);
        final Language audioLanguage = parseAudioLanguage(apiosCode);
        final Language subtitleLanguage = parseSubtitleLanguage(apiosCode);
        final VersionType versionType = VersionType.parse(apiosCode);
        return new VersionInfo() {
            @Override
            public SubtitleType getSubtitleType() {
                return subtitleType;
            }

            @Override
            public Language getSubtitleLanguage() {
                return subtitleLanguage;
            }

            @Override
            public Language getAudioLanguage() {
                if (audioLanguage == Language.OTHER) {
                    /* Try to determine language by VersionType */
                    return getAudioLanguageByVersionType();
                } else {
                    return audioLanguage;
                }
            }

            @Override
            public Language getAudioLanguageByVersionType() {
                if (versionType == VersionType.ORIGINAL_FRANCAIS) {
                    return Language.FRENCH;
                } else if (versionType == VersionType.NON_ORIGINAL_FRANCAIS) {
                    return Language.FRENCH;
                } else if (versionType == VersionType.ORIGINAL_GERMAN) {
                    return Language.GERMAN;
                } else if (versionType == VersionType.NON_ORIGINAL_GERMAN) {
                    return Language.GERMAN;
                } else {
                    return Language.OTHER;
                }
            }

            @Override
            public VersionType getVersionType() {
                return versionType;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder();
                sb.append(getVersionType());
                sb.append("_");
                sb.append(getAudioLanguage());
                if (hasAnySubtitle()) {
                    sb.append("_");
                    sb.append(getSubtitleType());
                    sb.append("_");
                    sb.append(getSubtitleLanguage());
                }
                return sb.toString();
            }

            @Override
            public boolean hasAnySubtitle() {
                if (SubtitleType.NONE.equals(getSubtitleType())) {
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public boolean hasSubtitleAudioDescription() {
                if (SubtitleType.AUDIO_DESCRIPTION.equals(getSubtitleType())) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean hasSubtitleFull() {
                if (SubtitleType.FULL.equals(getSubtitleType())) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean hasSubtitlePartial() {
                if (SubtitleType.PARTIAL.equals(getSubtitleType())) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean hasSubtitleForHearingImpaired() {
                if (SubtitleType.HEARING_IMPAIRED.equals(getSubtitleType())) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean isOriginalVersion() {
                if (versionType == VersionType.ORIGINAL || versionType == VersionType.ORIGINAL_FRANCAIS || versionType == VersionType.ORIGINAL_GERMAN) {
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

    public boolean hasCaptcha(final CryptedLink link, final Account acc) {
        return false;
    }

    @Override
    public Class<? extends ArteMediathekConfig> getConfigInterface() {
        return ArteMediathekConfig.class;
    }
}
