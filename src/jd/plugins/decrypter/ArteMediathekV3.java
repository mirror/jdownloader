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

    private static final String API_BASE = "https://api.arte.tv/api/opa/v3";

    private static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("Authorization", "Bearer Nzc1Yjc1ZjJkYjk1NWFhN2I2MWEwMmRlMzAzNjI5NmU3NWU3ODg4ODJjOWMxNTMxYzEzZGRjYjg2ZGE4MmIwOA");
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        return crawlPrograms(param);
    }

    private ArrayList<DownloadLink> crawlPrograms(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
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

    private ArrayList<DownloadLink> crawlProgram(final CryptedLink param, final Map<String, Object> program) throws IOException {
        // TODO: Add support for multiple videos(?), implement plugin settings
        final Map<String, Object> vid = (Map<String, Object>) program.get("mainVideo");
        return crawlVideo(param, vid);
    }

    private ArrayList<DownloadLink> crawlVideo(final CryptedLink param, final Map<String, Object> vid) throws IOException {
        final String title = vid.get("title").toString();
        final String subtitle = (String) vid.get("subtitle");
        final String dateFormatted = new Regex(vid.get("firstBroadcastDate").toString(), "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String fullDescription = (String) vid.get("fullDescription");
        final String videoStreamsAPIURL = JavaScriptEngineFactory.walkJson(vid, "links/videoStreams/web/href").toString();
        prepBRAPI(br);
        // br.getPage(API_BASE + "/videoStreams?programId=" + Encoding.urlEncode(programId) +
        // "&reassembly=A&platform=ARTE_NEXT&channel=DE&kind=SHOW&protocol=%24in:HTTPS,HLS&quality=%24in:EQ,HQ,MQ,SQ,XQ&profileAmm=%24in:AMM-PTWEB,AMM-PTHLS,AMM-OPERA,AMM-CONCERT-NEXT,AMM-Tvguide&limit=100");
        br.getPage(videoStreamsAPIURL);
        String titleBase = dateFormatted + "_" + vid.get("platform") + "_" + title;
        if (!StringUtils.isEmpty(subtitle)) {
            titleBase += " - " + subtitle;
        }
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
        final List<Integer> selectedQualities = new ArrayList<Integer>();
        if (cfg.isCrawlHTTP1080p()) {
            selectedQualities.add(1080);
        }
        if (cfg.isCrawlHTTP720p()) {
            selectedQualities.add(720);
        }
        if (cfg.isCrawlHTTP480p()) {
            selectedQualities.add(480);
        }
        if (cfg.isCrawlHTTP360p()) {
            selectedQualities.add(360);
        }
        if (cfg.isCrawlHTTP240p()) {
            selectedQualities.add(240);
        }
        if (selectedQualities.isEmpty()) {
            logger.warning("User has deselected all qualities");
        }
        // TODO: Implement pagination?
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final List<Map<String, Object>> videoStreams = (List<Map<String, Object>>) entries.get("videoStreams");
        final ArrayList<DownloadLink> allResults = new ArrayList<DownloadLink>();
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
        for (final List<Map<String, Object>> videoStreamsByLanguage : languagePacks.values()) {
            /* TODO: Add BEST handling */
            final ArrayList<DownloadLink> userSelected = new ArrayList<DownloadLink>();
            long highestFilesize = 0;
            DownloadLink bestForThisPack = null;
            long highestFilesizeOfUserSelected = 0;
            DownloadLink bestOfUserSelected = null;
            for (final Map<String, Object> videoStream : videoStreamsByLanguage) {
                // final String language = videoStream.get("language").toString();
                final String protocol = videoStream.get("protocol").toString();
                if (!protocol.equalsIgnoreCase("https")) {
                    /* 2022-05-25: Only grab HTTP streams for now */
                    continue;
                }
                // final int width = ((Number) videoStream.get("width")).intValue();
                final int height = ((Number) videoStream.get("height")).intValue();
                final int durationSeconds = ((Number) videoStream.get("durationSeconds")).intValue();
                final int bitrate = ((Number) videoStream.get("bitrate")).intValue();
                final DownloadLink link = this.createDownloadlink(videoStream.get("url").toString());
                final String finalFilename = titleBase + "_" + videoStream.get("filename");
                link.setFinalFileName(finalFilename);
                link.setProperty(DirectHTTP.FIXNAME, finalFilename);
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
                if (link.getView().getBytesTotal() > highestFilesize) {
                    bestForThisPack = link;
                    highestFilesize = link.getView().getBytesTotal();
                }
                if (selectedQualities.contains(height)) {
                    userSelected.add(link);
                    if (link.getView().getBytesTotal() > highestFilesizeOfUserSelected) {
                        bestOfUserSelected = link;
                        highestFilesizeOfUserSelected = link.getView().getBytesTotal();
                    }
                }
            }
            if (mode == QualitySelectionMode.BEST) {
                userSelected.clear();
                /* Should never be null */
                if (bestForThisPack != null) {
                    userSelected.add(bestForThisPack);
                } else {
                    logger.warning("Failed to find bestForThisPack");
                }
            } else if (mode == QualitySelectionMode.BEST_OF_SELECTED) {
                userSelected.clear();
                /* Should never be null */
                if (bestOfUserSelected != null) {
                    userSelected.add(bestOfUserSelected);
                } else {
                    logger.warning("Failed to find bestOfUserSelected");
                }
            }
            for (final DownloadLink selected : userSelected) {
                distribute(selected);
                ret.add(selected);
            }
        }
        /*
         * TODO: Either add auto fallback to "Return all" or add setting to define behavior on
         * "no results found due to users' plugin settings"!
         */
        if (ret.isEmpty()) {
            logger.info("Returning zero results due to users' plugin settings!");
        }
        return ret;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public Class<? extends ArteMediathekConfig> getConfigInterface() {
        return ArteMediathekConfig.class;
    }
}
