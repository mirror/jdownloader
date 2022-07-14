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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
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
import jd.plugins.decrypter.ArteMediathekDecrypter.VersionInfo;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 4, names = {}, urls = {})
public class ArteMediathekV3 extends PluginForDecrypt {
    public ArteMediathekV3(PluginWrapper wrapper) {
        super(wrapper);
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z]{2})/videos/(\\d+-\\d+-[ADF]+)(/([a-z0-9\\-]+)/?)?");
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
    private final String        PROPERTY_AUDIO_CODE         = "audio_code";                    // ex versionCode e.g. VF, VF-STA, VA
    private final String        PROPERTY_AUDIO_SHORT_LABEL  = "audioShortLabel";               // e.g. DE, FR
    private final String        PROPERTY_AUDIO_LABEL        = "audioLabel";                    // e.g. Deutsch, Französisch
    private final String        PROPERTY_WIDTH              = "width";
    private final String        PROPERTY_HEIGHT             = "height";
    private final String        PROPERTY_BITRATE            = "bitrate";
    private final String        PROPERTY_PLATFORM           = "platform";

    private static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("Authorization", "Bearer Nzc1Yjc1ZjJkYjk1NWFhN2I2MWEwMmRlMzAzNjI5NmU3NWU3ODg4ODJjOWMxNTMxYzEzZGRjYjg2ZGE4MmIwOA");
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
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String urlLanguage = urlinfo.getMatch(0); // de or fr
        final String contentID = urlinfo.getMatch(1);
        prepBRAPI(br);
        /* API will return all results in german or french depending on 'urlLanguage'! */
        br.getPage(API_BASE + "/programs/" + urlLanguage + "/" + contentID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object errorsO = entries.get("errors");
        if (errorsO != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Map<String, Object>> programs = (List<Map<String, Object>>) entries.get("programs");
        for (final Map<String, Object> program : programs) {
            ret.addAll(this.crawlProgram(param, program));
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlProgram(final CryptedLink param, final Map<String, Object> program) throws IOException, PluginException {
        final Map<String, Object> vid = (Map<String, Object>) program.get("mainVideo");
        return crawlVideo(param, vid);
    }

    private ArrayList<DownloadLink> crawlVideo(final CryptedLink param, final Map<String, Object> vid) throws IOException, PluginException {
        final String kindLabel = vid.get("kindLabel").toString();
        if (kindLabel.equals("LIVE")) {
            logger.info("Livestreams are not supported");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!kindLabel.equals("PROGRAMM")) {
            logger.info("Unknown kindLabel: " + kindLabel);
        }
        final String videoID = vid.get("programId").toString();
        final String title = vid.get("title").toString();
        final String subtitle = (String) vid.get("subtitle");
        final String dateFormatted = new Regex(vid.get("firstBroadcastDate").toString(), "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String fullDescription = (String) vid.get("shortDescription");
        final String platform = vid.get("platform").toString();
        final String videoStreamsAPIURL = JavaScriptEngineFactory.walkJson(vid, "links/videoStreams/web/href").toString();
        String titleAndSubtitle = title;
        if (!StringUtils.isEmpty(subtitle)) {
            titleAndSubtitle += " - " + subtitle;
        }
        prepBRAPI(br);
        // br.getPage(API_BASE + "/videoStreams?programId=" + Encoding.urlEncode(programId) +
        // "&reassembly=A&platform=ARTE_NEXT&channel=DE&kind=SHOW&protocol=%24in:HTTPS,HLS&quality=%24in:EQ,HQ,MQ,SQ,XQ&profileAmm=%24in:AMM-PTWEB,AMM-PTHLS,AMM-OPERA,AMM-CONCERT-NEXT,AMM-Tvguide&limit=100");
        br.getPage(videoStreamsAPIURL);
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
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
        packageName = packageName.replace("*video_id*", videoID);
        packageName = packageName.replace("*title*", title);
        packageName = packageName.replace("*subtitle*", subtitle != null ? subtitle : "");
        packageName = packageName.replace("*title_and_subtitle*", titleAndSubtitle);
        final FilePackage fp = FilePackage.getInstance();
        fp.setCleanupPackageName(false);
        fp.setName(packageName);
        if (!StringUtils.isEmpty(fullDescription)) {
            fp.setComment(fullDescription);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Crawl thumbnail if wished by user */
        final QualitySelectionMode mode = cfg.getQualitySelectionMode();
        /* Crawl video streams */
        /* Collect list of user desired/allowed qualities */
        final List<Integer> selectedQualitiesHeight = getSelectedHTTPQualities();
        final List<String> selectedLanguages = getSelectedLanguages(param.getCryptedUrl());
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
                final int height = ((Number) videoStream.get("height")).intValue();
                final int durationSeconds = ((Number) videoStream.get("durationSeconds")).intValue();
                final int bitrate = ((Number) videoStream.get("bitrate")).intValue();
                final String audioCode = videoStream.get("audioCode").toString(); // e.g. VF, VF-STA, VA, ...
                final String audioChar = regexAudioLanguageChar(audioCode);
                final DownloadLink link = this.createDownloadlink(videoStream.get("url").toString());
                /* Set properties which we later need for custom filenames. */
                link.setProperty(PROPERTY_VIDEO_ID, videoID);
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
                final VersionInfo versionInfo = ArteMediathekDecrypter.parseVersionInfo(audioCode);
                /* Do not modify those linkIDs to try to keep backward compatibility! remove the -[ADF] to be same as old vpi */
                final String linkID = getHost() + "://" + new Regex(videoID, "(\\d+-\\d+)").getMatch(0) + "/" + versionInfo.toString() + "/" + "http_" + bitrate;
                link.setContentUrl(param.getCryptedUrl());
                link.setProperty(DirectHTTP.PROPERTY_CUSTOM_HOST, getHost());
                link.setLinkID(linkID);
                /* Get filename according to users' settings. */
                final String filename = this.getAndSetFilename(link);
                /* Make sure that our directHTTP plugin will never change this filename. */
                link.setProperty(DirectHTTP.FIXNAME, filename);
                link.setAvailable(true);
                /* Calculate filesize in a very simple way */
                link.setDownloadSize(bitrate / 8 * 1024 * durationSeconds);
                link._setFilePackage(fp);
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    link.setComment(vid.get("id").toString());
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
                if (versionInfo.hasAnySubtitle()) {
                    if (versionInfo.hasSubtitleFull() && !cfg.isCrawlSubtitledBurnedInVersionsFull()) {
                        continue;
                    } else if (versionInfo.hasSubtitleForHearingImpaired() && !cfg.isCrawlSubtitledBurnedInVersionsPartial()) {
                        continue;
                    }
                    if (versionInfo.hasSubtitleForHearingImpaired() && !cfg.isCrawlSubtitledBurnedInVersionsHearingImpaired()) {
                        continue;
                    }
                }
                if (this.knownLanguages.contains(audioChar)) {
                    if (!selectedLanguages.contains(audioChar)) {
                        /* Skip unwanted languages */
                        continue;
                    }
                } else {
                    if (!cfg.isCrawlLanguageUnknown()) {
                        /* Skip this unknown language */
                        continue;
                    }
                    logger.info("Adding unknown language allowed candidate: " + audioChar);
                }
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
            /* Decide what to do based on users' settings. */
            final ArrayList<DownloadLink> finalSelection = new ArrayList<DownloadLink>();
            if (mode == QualitySelectionMode.BEST) {
                if (bestAllowed != null) {
                    finalSelection.add(best);
                } else {
                    logger.warning("Failed to find bestAllowed -> User must have bad plugin settings");
                }
            } else if (mode == QualitySelectionMode.BEST_OF_SELECTED) {
                /*
                 * Can be null if user has bad settings e.g. deselected all qualities or only selected qualities which are not available for
                 * currently processed video.
                 */
                if (bestOfUserSelected != null) {
                    finalSelection.add(bestOfUserSelected);
                } else {
                    logger.warning("Failed to find bestOfUserSelected -> User must have bad plugin settings");
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
        if (cfg.isCrawlThumbnail()) {
            final Map<String, Object> mainImage = (Map<String, Object>) vid.get("mainImage");
            final String imageCaption = (String) mainImage.get("caption");
            final DownloadLink thumbnail = this.createDownloadlink("directhttp://" + mainImage.get("url"));
            thumbnail.setContentUrl(param.getCryptedUrl());
            thumbnail.setProperty(DirectHTTP.PROPERTY_CUSTOM_HOST, getHost());
            final String extension = mainImage.get("extension").toString();
            if (ret.size() == 1) {
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
                if (imageCaption != null) {
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
            logger.warning("WTF Failed to find any results");
        } else {
            for (final DownloadLink result : ret) {
                distribute(result);
            }
        }
        return ret;
    }

    public static String regexAudioLanguageChar(final String audioCode) {
        return new Regex(audioCode, "^VO?([A-Z])").getMatch(0);
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
    private final List<String>  knownLanguages       = Arrays.asList(new String[] { "english_todo", "F", "A", "italian_todo", "polish_todo", "spanish_todo" });

    private List<String> getSelectedLanguages(final String url) throws PluginException {
        final ArteMediathekConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final List<String> selectedLanguages = new ArrayList<String>();
        if (cfg.getLanguageSelectionMode() == LanguageSelectionMode.ALL_SELECTED) {
            if (cfg.isCrawlLanguageEnglish()) {
                // TODO
                selectedLanguages.add("english_todo");
            }
            if (cfg.isCrawlLanguageFrench()) {
                selectedLanguages.add("F");
            }
            if (cfg.isCrawlLanguageGerman()) {
                selectedLanguages.add("A");
            }
            if (cfg.isCrawlLanguageItalian()) {
                // TODO
                selectedLanguages.add("italian_todo");
            }
            if (cfg.isCrawlLanguagePolish()) {
                // TODO
                selectedLanguages.add("polish_todo");
            }
        } else {
            /* Language by URL */
            final String langFromURL = new Regex(url, "https?://[^/]+/([a-z]{2})").getMatch(0);
            if (langFromURL == null) {
                /* Developer mistake */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            selectedLanguages.add(iso6391CodeToArteAudioChar(langFromURL));
        }
        return selectedLanguages;
    }

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

    /** E.g. "de" --> "A" */
    private static String iso6391CodeToArteAudioChar(final String iso6391Code) {
        if (iso6391Code.equalsIgnoreCase("en")) {
            return "english_todo";
        } else if (iso6391Code.equalsIgnoreCase("fr")) {
            return "F";
        } else if (iso6391Code.equalsIgnoreCase("de")) {
            return "A";
        } else if (iso6391Code.equalsIgnoreCase("it")) {
            return "italian_todo";
        } else if (iso6391Code.equalsIgnoreCase("pl")) {
            return "polish_todo";
        } else if (iso6391Code.equalsIgnoreCase("es")) {
            return "spanish_todo";
        } else {
            /* Unknown/unsupported code */
            return null;
        }
    }

    public boolean hasCaptcha(final CryptedLink link, final Account acc) {
        return false;
    }

    @Override
    public Class<? extends ArteMediathekConfig> getConfigInterface() {
        return ArteMediathekConfig.class;
    }
}
