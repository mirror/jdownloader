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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.ArteMediathekConfig;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.FilenameSchemeType;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.QualitySelectionFallbackMode;
import org.jdownloader.plugins.components.config.ArteMediathekConfig.QualitySelectionMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z]{2})/videos/(\\d+-\\d+-[ADF]+)/([a-z0-9\\-]+)/?");
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
        final String urlLanguage = urlinfo.getMatch(0);
        final String contentID = urlinfo.getMatch(1);
        prepBRAPI(br);
        br.getPage(API_BASE + "/programs/" + urlLanguage + "/" + contentID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // TODO: Add pagination
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object errorsO = entries.get("errors");
        if (errorsO != null) {
            /* TODO: Maybe display more detailed errormessage to user. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Map<String, Object>> programs = (List<Map<String, Object>>) entries.get("programs");
        for (final Map<String, Object> program : programs) {
            ret.addAll(this.crawlProgram(param, program));
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlProgram(final CryptedLink param, final Map<String, Object> program) throws IOException, PluginException {
        // TODO: Add support for multiple videos(?), implement plugin settings
        final Map<String, Object> vid = (Map<String, Object>) program.get("mainVideo");
        return crawlVideo(param, vid);
    }

    private ArrayList<DownloadLink> crawlVideo(final CryptedLink param, final Map<String, Object> vid) throws IOException, PluginException {
        final String title = vid.get("title").toString();
        final String subtitle = (String) vid.get("subtitle");
        final String dateFormatted = new Regex(vid.get("firstBroadcastDate").toString(), "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String fullDescription = (String) vid.get("fullDescription");
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
        String titleBase = dateFormatted + "_" + platform + "_" + titleAndSubtitle;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(titleBase);
        if (!StringUtils.isEmpty(fullDescription)) {
            fp.setComment(fullDescription);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Crawl thumbnail if wished by user */
        final ArteMediathekConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final QualitySelectionMode mode = cfg.getQualitySelectionMode();
        if (cfg.isCrawlThumbnail()) {
            final Map<String, Object> mainImage = (Map<String, Object>) vid.get("mainImage");
            final String imageCaption = (String) mainImage.get("caption");
            final DownloadLink thumbnail = this.createDownloadlink("directhttp://" + mainImage.get("url"));
            thumbnail.setFinalFileName(titleBase + "." + mainImage.get("extension"));
            if (imageCaption != null) {
                thumbnail.setComment(imageCaption);
            }
            thumbnail.setAvailable(true);
            ret.add(thumbnail);
            distribute(thumbnail);
        }
        /* Crawl video streams */
        /* Collect list of user desired/allowed qualities */
        final List<Integer> selectedQualitiesHeight = getSelectedHTTPQualities();
        // TODO: Implement pagination?
        final QualitySelectionFallbackMode qualitySelectionFallbackMode = cfg.getQualitySelectionFallbackMode();
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
        /*
         * Now filter by language, user selected qualities and so on. User can e.g. select multiple languages and best video quality of each
         * combined with subtitle preferences.
         */
        final ArrayList<DownloadLink> allResults = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> allBestResults = new ArrayList<DownloadLink>();
        for (final List<Map<String, Object>> videoStreamsByLanguage : languagePacks.values()) {
            final ArrayList<DownloadLink> userSelected = new ArrayList<DownloadLink>();
            long highestFilesize = 0;
            DownloadLink best = null;
            long highestFilesizeOfUserSelected = 0;
            DownloadLink bestOfUserSelected = null;
            for (final Map<String, Object> videoStream : videoStreamsByLanguage) {
                // final String language = videoStream.get("language").toString();
                final String protocol = videoStream.get("protocol").toString();
                if (!protocol.equalsIgnoreCase("https")) {
                    /* 2022-05-25: Only grab HTTP streams for now, skip all others */
                    continue;
                }
                // final int width = ((Number) videoStream.get("width")).intValue();
                final int height = ((Number) videoStream.get("height")).intValue();
                final int durationSeconds = ((Number) videoStream.get("durationSeconds")).intValue();
                final int bitrate = ((Number) videoStream.get("bitrate")).intValue();
                final String audioCode = videoStream.get("audioCode").toString(); // e.g. VF, VF-STA, VA, ...
                final DownloadLink link = this.createDownloadlink(videoStream.get("url").toString());
                link.setProperty(PROPERTY_VIDEO_ID, videoStream.get("programId").toString());
                link.setProperty(PROPERTY_TITLE, title);
                link.setProperty(PROPERTY_SUBTITLE, subtitle);
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
                final String filename = this.getAndSetFilename(link);
                /* Make sure that our directHTTP plugin will never change this filename. */
                link.setProperty(DirectHTTP.FIXNAME, filename);
                link.setAvailable(true);
                /* Calculate filesize in a very simple way */
                link.setDownloadSize(bitrate / 8 * 1024 * durationSeconds);
                link._setFilePackage(fp);
                allResults.add(link);
                /* Now check for skip conditions based on user settings */
                /* Skip subtitled versions if not wished by user */
                final List<Map<String, Object>> subtitles = (List<Map<String, Object>>) videoStream.get("subtitles");
                if (!subtitles.isEmpty() && !cfg.isCrawlSubtitledBurnedInVersions()) {
                    continue;
                }
                if (link.getView().getBytesTotal() > highestFilesize || best == null) {
                    best = link;
                    highestFilesize = link.getView().getBytesTotal();
                }
                if (selectedQualitiesHeight.contains(getHeightForQualitySelection(height))) {
                    userSelected.add(link);
                    if (link.getView().getBytesTotal() > highestFilesizeOfUserSelected) {
                        bestOfUserSelected = link;
                        highestFilesizeOfUserSelected = link.getView().getBytesTotal();
                    }
                }
            }
            allBestResults.add(best);
            if (mode == QualitySelectionMode.BEST) {
                userSelected.clear();
                /* Should never be null */
                userSelected.add(best);
            } else if (mode == QualitySelectionMode.BEST_OF_SELECTED) {
                userSelected.clear();
                /* Can be null if user has bad settings */
                if (bestOfUserSelected != null) {
                    userSelected.add(bestOfUserSelected);
                } else {
                    logger.warning("Failed to find bestOfUserSelected");
                }
            } else {
                // ALL_SELECTED
            }
            ret.addAll(userSelected);
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
        for (final DownloadLink result : ret) {
            distribute(result);
        }
        if (ret.isEmpty()) {
            /* This should never happen! */
            logger.warning("WTF Failed to find any results");
        }
        return ret;
    }

    private String getAndSetFilename(final DownloadLink link) {
        String filename;
        final ArteMediathekConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final FilenameSchemeType schemeType = cfg.getFilenameSchemeType();
        String customFilenameScheme = cfg.getFilenameScheme();
        if (schemeType == FilenameSchemeType.DEFAULT) {
            filename = "*date*_*platform*_*title*_*video_id*_*language*_*shortlanguage*_*resolution*_*bitrate**ext*";
        } else if (schemeType == FilenameSchemeType.LEGACY) {
            filename = "*date*_arte_*title*_*video_id*_*language*_*shortlanguage*_*resolution*_*bitrate**ext*";
        } else if (schemeType == FilenameSchemeType.CUSTOM && !StringUtils.isEmpty(customFilenameScheme)) {
            /* User customized filename scheme */
            /* Legacy compatibility for old arte config/crawler version 46182 */
            customFilenameScheme = customFilenameScheme.replace("*vpi*__*language*", "*vpi*_*language*"); // fix old mistake: one
                                                                                                          // underscore too much
            customFilenameScheme = customFilenameScheme.replace("*vpi*", "*video_id*"); // update changed tag name
            customFilenameScheme = customFilenameScheme.replace("*title*", "*title_and_subtitle*"); // update changed tag name
            filename = customFilenameScheme;
        } else {
            /* FilenameSchemeType.ORIGINAL and fallback */
            filename = "*original_filename*";
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
        filename = filename.replace("*subtitle*", link.getStringProperty(PROPERTY_SUBTITLE));
        filename = filename.replace("*title_and_subtitle*", link.getStringProperty(PROPERTY_TITLE_AND_SUBTITLE));
        link.setFinalFileName(filename);
        return filename;
    }

    private List<Integer> getSelectedHTTPQualities() {
        final ArteMediathekConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final List<Integer> selectedQualitiesHeight = new ArrayList<Integer>();
        if (cfg.isCrawlHTTP1080p()) {
            selectedQualitiesHeight.add(1080);
        }
        if (cfg.isCrawlHTTP720p()) {
            selectedQualitiesHeight.add(720);
        }
        if (cfg.isCrawlHTTP480p()) {
            selectedQualitiesHeight.add(480);
        }
        if (cfg.isCrawlHTTP360p()) {
            selectedQualitiesHeight.add(360);
        }
        if (cfg.isCrawlHTTP240p()) {
            selectedQualitiesHeight.add(240);
        }
        return selectedQualitiesHeight;
    }

    /*
     * Height of ARTE videos may vary but we only got a fixed quality selection for our users e.g. they can have 364x216 -> We'd consider
     * this 240p for quality selection handling.
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

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public Class<? extends ArteMediathekConfig> getConfigInterface() {
        return ArteMediathekConfig.class;
    }
}
