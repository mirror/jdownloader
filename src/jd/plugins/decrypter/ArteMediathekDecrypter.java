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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ArteTv;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "arte.tv" }, urls = { "https?://(?:www\\.)?arte\\.tv/(guide/[a-z]{2}/|[a-z]{2}/videos/)\\d+-\\d+-[ADF]+/[^/]+" })
public class ArteMediathekDecrypter extends PluginForDecrypt {
    private static final String     PATTERN_SUPPORTED_LANGUAGES                 = "(?:de|fr|en|it|pl|es)";
    /* Current linktype */
    private static final String     TYPE_VIDEOS                                 = "https?://[^/]+/([a-z]{2})/videos/(\\d+-\\d+-[ADF]+)/([^/]+).*";
    /* Old linktype */
    private static final String     TYPE_GUIDE                                  = "https?://[^/]+/guide/([a-z]{2})/(\\d+-\\d+-[ADF])?/([^/]+).*";
    // private static final String API_TYPE_OEMBED_PATTERN = "https?://api.arte.tv/api/player/v\\d+/oembed/" + PATTERN_SUPPORTED_LANGUAGES +
    // "/([A-Za-z0-9\\-]+)(\\?platform=.+)";
    // private static final String API_TYPE_OTHER_PATTERN = "https?://api.arte.tv/api/player/v\\d+/config/" + PATTERN_SUPPORTED_LANGUAGES +
    // "/([A-Za-z0-9\\-]+)(\\?.+)?";
    private static final String     http_300                                    = "http_300";
    private static final String     http_800                                    = "http_800";
    private static final String     http_1500                                   = "http_1500";
    private static final String     http_2200                                   = "http_2200";
    private static final String     THUMBNAIL                                   = "THUMBNAIL";
    private static final String     FAST_LINKCHECK                              = "FAST_LINKCHECK";
    private static final short      format_intern_german                        = 1;
    private static final short      format_intern_french                        = 2;
    private static final short      format_intern_subtitled                     = 3;
    private static final short      format_intern_subtitled_for_disabled_people = 4;
    private static final short      format_intern_audio_description             = 5;
    private static final short      format_intern_unknown                       = 6;
    final String[]                  formats                                     = { http_300, http_800, http_1500, http_2200 };
    private static final String     LOAD_BEST                                   = "LOAD_BEST";
    private static final String     LANG_DE                                     = "de";
    private static final String     LANG_FR                                     = "fr";
    private static final String     LANG_EN                                     = "en";
    private static final String     LANG_ES                                     = "es";
    private static final String     LANG_PL                                     = "pl";
    private static final String     LANG_IT                                     = "it";
    private String                  parameter;
    private ArrayList<DownloadLink> decryptedLinks                              = new ArrayList<DownloadLink>();

    @SuppressWarnings("deprecation")
    public ArteMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        int foundFormatsNum = 0;
        parameter = param.toString();
        final ArrayList<String> selectedLanguages = new ArrayList<String>();
        String title = getUrlFilename();
        String videoID = null;
        String thumbnailUrl = null;
        final String plain_domain_decrypter = this.getHost() + ".artejd_decrypted_jd";
        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String dateFormatted = "-";
        final boolean fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(new int[] { 410, 500, 503 });
        final String desiredLanguage = TranslationFactory.getDesiredLanguage();
        final boolean isGerman = "de".equalsIgnoreCase(desiredLanguage);
        final boolean isFrancais = "fr".equalsIgnoreCase(desiredLanguage);
        final boolean isEnglish = "en".equalsIgnoreCase(desiredLanguage);
        final boolean isPolish = "pl".equalsIgnoreCase(desiredLanguage);
        final boolean isItalian = "it".equalsIgnoreCase(desiredLanguage);
        final boolean isSpanish = "es".equalsIgnoreCase(desiredLanguage);
        /* First we need to have some basic data - this part is link-specific. */
        if (parameter.matches(TYPE_VIDEOS)) {
            /* 2021-03-02: Testing if this will work for all URLs and completely without using their website. */
            final Regex urlinfo = new Regex(this.parameter, TYPE_VIDEOS);
            videoID = urlinfo.getMatch(1);
        } else if (parameter.matches(TYPE_GUIDE)) {
            final Regex urlinfo = new Regex(this.parameter, TYPE_GUIDE);
            videoID = urlinfo.getMatch(1);
        } else {
            logger.info("Unsupported linkformat: " + param.getCryptedUrl());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /*
         * Now let's check which languages the user wants. We'll do the quality selection later but we have to access webpages to get the
         * different languages so let's keep the load low by only grabbing what the user selected.
         */
        final boolean germanSelected = cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.LOAD_LANGUAGE_GERMAN, jd.plugins.hoster.ArteTv.default_LOAD_LANGUAGE_GERMAN);
        final boolean francaisSelected = cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.LOAD_LANGUAGE_FRENCH, jd.plugins.hoster.ArteTv.default_LOAD_LANGUAGE_FRENCH);
        final boolean polishSelected = cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.LOAD_LANGUAGE_POLISH, jd.plugins.hoster.ArteTv.default_LOAD_LANGUAGE_POLISH);
        final boolean italianSelected = cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.LOAD_LANGUAGE_ITALIAN, jd.plugins.hoster.ArteTv.default_LOAD_LANGUAGE_ITALIAN);
        final boolean englishSelected = cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.LOAD_LANGUAGE_ENGLISH, jd.plugins.hoster.ArteTv.default_LOAD_LANGUAGE_ENGLISH);
        final boolean spanishSelected = cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.LOAD_LANGUAGE_SPANISH, jd.plugins.hoster.ArteTv.default_LOAD_LANGUAGE_SPANISH);
        final boolean loadURLLanguage = cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.LOAD_LANGUAGE_URL, jd.plugins.hoster.ArteTv.default_LOAD_LANGUAGE_URL);
        final boolean loadBest = cfg.getBooleanProperty(LOAD_BEST, false);
        final String urlLanguage = this.getUrlLang();
        if (loadURLLanguage) {
            if (urlLanguage != null) {
                selectedLanguages.add(urlLanguage);
            } else {
                selectedLanguages.add(LANG_DE);
            }
        } else {
            if (germanSelected) {
                if (isGerman) {
                    selectedLanguages.add(0, LANG_DE);
                } else {
                    selectedLanguages.add(LANG_DE);
                }
            }
            if (francaisSelected) {
                if (isFrancais) {
                    selectedLanguages.add(0, LANG_FR);
                } else {
                    selectedLanguages.add(LANG_FR);
                }
            }
            if (englishSelected) {
                if (isEnglish) {
                    selectedLanguages.add(0, LANG_EN);
                } else {
                    selectedLanguages.add(LANG_EN);
                }
            }
            if (italianSelected) {
                if (isItalian) {
                    selectedLanguages.add(0, LANG_IT);
                } else {
                    selectedLanguages.add(LANG_IT);
                }
            }
            if (polishSelected) {
                if (isPolish) {
                    selectedLanguages.add(0, LANG_PL);
                } else {
                    selectedLanguages.add(LANG_PL);
                }
            }
            if (spanishSelected) {
                if (isSpanish) {
                    selectedLanguages.add(0, LANG_ES);
                } else {
                    selectedLanguages.add(LANG_ES);
                }
            }
            if (selectedLanguages.size() == 0) {
                /* Fallback - nothing selected --> Download everything */
                logger.info("User selected no language at all --> Downloading all languages");
                selectedLanguages.add(LANG_DE);
                selectedLanguages.add(LANG_FR);
                selectedLanguages.add(LANG_EN);
                selectedLanguages.add(LANG_IT);
                selectedLanguages.add(LANG_PL);
                selectedLanguages.add(LANG_ES);
            }
        }
        if (urlLanguage != null) {
            // put urlLanguage at first position
            selectedLanguages.remove(urlLanguage);
            selectedLanguages.add(0, urlLanguage);
        }
        final HashMap<String, DownloadLink> results = new HashMap<String, DownloadLink>();
        /* Finally, grab all we can get (in the selected language(s)) */
        for (final String selectedLanguage : selectedLanguages) {
            logger.info("Crawling language: " + selectedLanguage);
            /* ?autostart=0&lifeCycle=1 = get lower qualities too. */
            /* v2 requires bearer-authentication, we stick to v1 as long as it works */
            // final String apiurl = "https://api.arte.tv/api/player/v2/config/" + selectedLanguage + "/" + videoID;
            final String apiurl = "https://api.arte.tv/api/player/v1/config/" + selectedLanguage + "/" + videoID;
            ArteTv.requestAPIURL(br, apiurl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* In most cases this simply means that one of the selected languages is not available so let's go on. */
                logger.info("This language is not available");
                continue;
            }
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final Map<String, Object> videoJsonPlayer = (Map<String, Object>) entries.get("videoJsonPlayer");
            final Object error_info = videoJsonPlayer.get("custom_msg");
            if (error_info != null) {
                final Map<String, Object> errorInfomap = (Map<String, Object>) error_info;
                final String errmsg = (String) errorInfomap.get("msg");
                final String type = (String) errorInfomap.get("type");
                if ((type.equals("error") || type.equals("info")) && errmsg != null) {
                    title = errmsg + "_" + title;
                } else {
                    title = "Unknown_error_" + title;
                }
                logger.warning("Unknown error:msg='" + errmsg + "',type=" + type);
                continue;
            }
            final Object vsrO = videoJsonPlayer.get("VSR");
            if (!(vsrO instanceof Map)) {
                /* No source available --> Video cannot be played --> Browser would says "Error code 2" then */
                logger.info("This language is not available: " + selectedLanguage);
                continue;
            }
            // final String sourceURL = (String) videoJsonPlayer.get("VTR");
            /* Title is sometimes null e.g. for expired videos */
            final String json_title = (String) videoJsonPlayer.get("VTI");
            final String json_subtitle = (String) videoJsonPlayer.get("subtitle");
            if (json_title != null) {
                title = encodeUnicode(json_title);
                if (json_subtitle != null) {
                    title += " - " + encodeUnicode(json_subtitle);
                }
            }
            String description = (String) videoJsonPlayer.get("VDE");
            if (StringUtils.isEmpty(description)) {
                description = (String) videoJsonPlayer.get("V7T");
            }
            final String errormessage = (String) entries.get("msg");
            if (errormessage != null) {
                final DownloadLink offline = createofflineDownloadLink(parameter);
                offline.setFinalFileName(title + errormessage);
                offline.setComment(description);
                ret.add(offline);
                return ret;
            }
            if (thumbnailUrl == null) {
                thumbnailUrl = (String) videoJsonPlayer.get("programImage");
                if (thumbnailUrl == null) {
                    Object VTU = videoJsonPlayer.get("VTU");
                    if (VTU != null && VTU instanceof Map) {
                        thumbnailUrl = (String) ((Map) VTU).get("IUR");
                    }
                }
            }
            final String vru = (String) videoJsonPlayer.get("VRU");
            final String vra = (String) videoJsonPlayer.get("VRA");
            final String vdb = (String) videoJsonPlayer.get("VDB");
            final String vpi = (String) videoJsonPlayer.get("VPI");
            if ((vru != null && vra != null) || vdb != null) {
                dateFormatted = formatDate(vra);
                /*
                 * In this case the video is not yet released and there usually is a value "VDB" which contains the release-date of the
                 * video --> But we don't need that - right now, such videos are simply offline and will be added as offline.
                 */
                final String expired_message;
                if (vdb != null) {
                    expired_message = String.format(jd.plugins.hoster.ArteTv.getPhrase("ERROR_CONTENT_NOT_AVAILABLE_YET"), jd.plugins.hoster.ArteTv.getNiceDate2(vdb));
                } else {
                    expired_message = jd.plugins.hoster.ArteTv.getExpireMessage(selectedLanguage, convertDateFormat(vra), convertDateFormat(vru));
                }
                if (expired_message != null) {
                    final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                    link.setComment(description);
                    link.setProperty("offline", true);
                    link.setFinalFileName(expired_message + "_" + title);
                    decryptedLinks.add(link);
                    return decryptedLinks;
                }
            }
            final Collection<Object> vsr_quals = ((Map<String, Object>) vsrO).values();
            /* One packagename for every language */
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(getFormattedFilePackageName(dateFormatted, title));
            for (final Object o : vsr_quals) {
                foundFormatsNum++;
                final Map<String, Object> qualitymap = (Map<String, Object>) o;
                final String url = (String) qualitymap.get("url");
                if (!url.startsWith("http")) {
                    continue;
                }
                final String versionCode = (String) qualitymap.get("versionCode");
                final String versionLibelle = (String) qualitymap.get("versionLibelle");
                final String versionShortLibelle = (String) qualitymap.get("versionShortLibelle");
                final VersionInfo versionInfo = parseVersionInfo(versionCode);
                if (!cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.V_NORMAL, jd.plugins.hoster.ArteTv.default_V_NORMAL) && !(SubtitleType.FULL.equals(versionInfo.getSubtitleType()) || SubtitleType.HEARING_IMPAIRED.equals(versionInfo.getSubtitleType()))) {
                    /* User does not want the non-subtitled version */
                    continue;
                } else if (!cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.V_SUBTITLE_DISABLED_PEOPLE, jd.plugins.hoster.ArteTv.default_V_SUBTITLE_DISABLED_PEOPLE) && SubtitleType.HEARING_IMPAIRED.equals(versionInfo.getSubtitleType())) {
                    /* User does not want the subtitled-for-.disabled-people version */
                    continue;
                } else if (!cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.V_SUBTITLED, jd.plugins.hoster.ArteTv.default_V_SUBTITLED) && SubtitleType.FULL.equals(versionInfo.getSubtitleType())) {
                    /* User does not want the subtitled version */
                    continue;
                } else if (!francaisSelected && !loadURLLanguage && SubtitleLanguage.FRANCAIS.equals(versionInfo.getSubtitleLanguage())) {
                    continue;
                } else if (!germanSelected && !loadURLLanguage && SubtitleLanguage.GERMAN.equals(versionInfo.getSubtitleLanguage())) {
                    continue;
                } else if (!englishSelected && !loadURLLanguage && SubtitleLanguage.ENGLISH.equals(versionInfo.getSubtitleLanguage())) {
                    continue;
                } else if (!polishSelected && !loadURLLanguage && SubtitleLanguage.POLISH.equals(versionInfo.getSubtitleLanguage())) {
                    continue;
                } else if (!italianSelected && !loadURLLanguage && SubtitleLanguage.ITALIAN.equals(versionInfo.getSubtitleLanguage())) {
                    continue;
                } else if (!spanishSelected && !loadURLLanguage && SubtitleLanguage.SPANISH.equals(versionInfo.getSubtitleLanguage())) {
                    continue;
                }
                if (url.contains(".m3u8")) {
                    if (!cfg.getBooleanProperty(ArteTv.hls, false)) {
                        /* Skip if user doesn't want HLS qualities. */
                        continue;
                    }
                    final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(br.cloneBrowser(), url);
                    if (hlsContainers != null) {
                        for (HlsContainer container : hlsContainers) {
                            final int videoBitrate = container.getBandwidth();
                            final String quality_intern = "hls_" + videoBitrate;
                            Number width = null;
                            if (container.getWidth() > 0) {
                                width = container.getWidth();
                            }
                            Number height = null;
                            if (container.getHeight() > 0) {
                                height = container.getHeight();
                            }
                            String videoResolution = "";
                            if (height != null && width != null) {
                                videoResolution = width + "x" + height;
                            }
                            final String linkID = getHost() + "://" + vpi + "/" + versionInfo.toString() + "/" + quality_intern;
                            if (!results.containsKey(linkID)) {
                                final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + UniqueAlltimeID.next());
                                link.setContentUrl(parameter);
                                link._setFilePackage(fp);
                                link.setProperty("versionCode", versionCode);
                                link.setProperty("directURL", container.getDownloadurl());
                                link.setProperty("date", dateFormatted);
                                link.setProperty("title", title);
                                link.setProperty("vpi", vpi);
                                link.setProperty("versionLibelle", versionLibelle);
                                link.setProperty("versionShortLibelle", versionShortLibelle);
                                link.setProperty("quality_intern", quality_intern);
                                link.setProperty("langShort", selectedLanguage);
                                link.setProperty("mainlink", parameter);
                                link.setProperty("apiurl", apiurl);
                                link.setProperty("width", width);
                                link.setProperty("height", height);
                                link.setProperty("resolution", videoResolution);
                                link.setProperty("bitrate", videoBitrate);
                                link.setProperty("ext", "mp4");
                                final String filename = getFormattedFileName(link);
                                link.setProperty("directName", filename);
                                link.setFinalFileName(filename);
                                if (vra != null && vru != null) {
                                    link.setProperty("VRA", convertDateFormat(vra));
                                    link.setProperty("VRU", convertDateFormat(vru));
                                }
                                link.setComment(description);
                                link.setContentUrl(parameter);
                                link.setLinkID(linkID);
                                if (fastLinkcheck) {
                                    link.setAvailable(true);
                                }
                                results.put(linkID, link);
                            }
                        }
                    }
                } else {
                    final Object widtho = qualitymap.get("width");
                    final Object heighto = qualitymap.get("height");
                    String videoResolution = "";
                    Number width = null;
                    Number height = null;
                    final int videoBitrate = ((Number) qualitymap.get("bitrate")).intValue();
                    if (widtho != null && heighto != null) {
                        /* These parameters are available in 95+% of all cases! */
                        width = ((Number) qualitymap.get("width"));
                        height = ((Number) qualitymap.get("height"));
                        videoResolution = width + "x" + height;
                    }
                    final String quality_intern = "http_" + videoBitrate;
                    if (!cfg.getBooleanProperty(quality_intern, true)) {
                        /* User does not want this bitrate --> Skip it */
                        logger.info("Skipping " + quality_intern);
                        continue;
                    }
                    // TODO
                    // if (!cfg.getBooleanProperty(jd.plugins.hoster.ArteTv.V_AUDIO_DESCRIPTION,
                    // jd.plugins.hoster.ArteTv.default_V_AUDIO_DESCRIPTION) && format_code == format_intern_audio_description) {
                    // /* User does not want the audio-description version */
                    // continue;
                    // }
                    final String linkID = this.getHost() + "://" + vpi + "/" + versionInfo.toString() + "/" + quality_intern;
                    if (!results.containsKey(linkID)) {
                        final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + UniqueAlltimeID.next());
                        link.setContentUrl(parameter);
                        link._setFilePackage(fp);
                        link.setProperty("versionCode", versionCode);
                        link.setProperty("directURL", url);
                        link.setProperty("date", dateFormatted);
                        link.setProperty("title", title);
                        link.setProperty("vpi", vpi);
                        link.setProperty("versionLibelle", versionLibelle);
                        link.setProperty("versionShortLibelle", versionShortLibelle);
                        link.setProperty("quality_intern", quality_intern);
                        link.setProperty("langShort", selectedLanguage);
                        link.setProperty("mainlink", parameter);
                        link.setProperty("apiurl", apiurl);
                        link.setProperty("width", width);
                        link.setProperty("height", height);
                        link.setProperty("resolution", videoResolution);
                        link.setProperty("bitrate", videoBitrate);
                        link.setProperty("ext", "mp4");
                        final String filename = getFormattedFileName(link);
                        link.setProperty("directName", filename);
                        link.setFinalFileName(filename);
                        if (vra != null && vru != null) {
                            link.setProperty("VRA", convertDateFormat(vra));
                            link.setProperty("VRU", convertDateFormat(vru));
                        }
                        link.setComment(description);
                        link.setContentUrl(parameter);
                        link.setLinkID(linkID);
                        if (fastLinkcheck) {
                            link.setAvailable(true);
                        }
                        results.put(linkID, link);
                    }
                }
            }
            if (this.isAbort()) {
                break;
            }
        }
        if (loadBest) {
            final HashMap<String, DownloadLink> map = new HashMap<String, DownloadLink>();
            for (final DownloadLink link : results.values()) {
                final String versionCode = link.getStringProperty("versionCode");
                final String langShort = link.getStringProperty("langShort");
                final Number height = (Number) link.getProperty("height");
                final String id = versionCode + "_" + langShort;
                final DownloadLink best = map.get(id);
                if (best == null) {
                    map.put(id, link);
                } else {
                    final Number heightCompare = (Number) best.getProperty("height");
                    if (height != null && heightCompare != null) {
                        if (height.intValue() > heightCompare.intValue()) {
                            map.put(id, link);
                        }
                    }
                }
            }
            decryptedLinks.addAll(map.values());
        } else {
            decryptedLinks.addAll(results.values());
        }
        /* User did not activate all versions --> Show this info in filename so he can correct his mistake. */
        if (decryptedLinks.isEmpty() && foundFormatsNum > 0) {
            title = jd.plugins.hoster.ArteTv.getPhrase("ERROR_USER_NEEDS_TO_CHANGE_FORMAT_SELECTION") + title;
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Check if user wants to download the thumbnail as well. */
        if (cfg.getBooleanProperty(THUMBNAIL, true) && thumbnailUrl != null) {
            final DownloadLink link = createDownloadlink("directhttp://" + thumbnailUrl);
            link.setProperty("date", dateFormatted);
            link.setProperty("title", title);
            link.setProperty("ext", "jpg");
            final String filename = getFormattedThumbnailName(link);
            link.setProperty("directName", filename);
            link.setFinalFileName(filename);
            decryptedLinks.add(link);
        }
        if (decryptedLinks.size() > 0) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(getFormattedFilePackageName(dateFormatted, title));
            fp.addLinks(decryptedLinks);
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            /*
             * Most likely offline content e.g. meta-data is still available but no video streams ("depublizierter Inhalt") but at the same
             * time API doesn't return any kind of errormessage!
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return decryptedLinks;
    }

    private static enum AudioLanguage {
        ENGLISH,
        FRANCAIS,
        GERMAN,
        ITALIAN,
        OTHER;

        private static AudioLanguage parse(final String apiosCode) {
            final String languageChar = ArteMediathekV3.regexAudioLanguageChar(apiosCode);
            if (languageChar != null) {
                /* TODO: Add support for more languages */
                if ("F".equals(languageChar)) {
                    return FRANCAIS;
                } else if ("A".equals(languageChar)) {
                    return GERMAN;
                } else {
                    return OTHER;
                }
            }
            return OTHER;
        }
    }

    private static enum SubtitleLanguage {
        ENGLISH,
        FRANCAIS,
        GERMAN,
        ITALIAN,
        SPANISH,
        POLISH,
        OTHER;

        private static SubtitleLanguage parse(final String apiosCode) {
            final String subtitleLanguage = new Regex(apiosCode, "-STM?(A|F)").getMatch(0);
            if (subtitleLanguage != null) {
                if ("F".equals(subtitleLanguage)) {
                    return FRANCAIS;
                } else if ("A".equals(subtitleLanguage)) {
                    return GERMAN;
                } else {
                    return OTHER;
                }
            }
            if (apiosCode.endsWith("[ANG]")) {
                return ENGLISH;
            } else if (apiosCode.endsWith("[ITA]")) {
                return ITALIAN;
            } else if (apiosCode.endsWith("[POL]")) {
                return POLISH;
            } else if (apiosCode.endsWith("[ESP]")) {
                return SPANISH;
            } else {
                return OTHER;
            }
        }
    }

    private static enum SubtitleType {
        NONE,
        FULL,
        PARTIAL,
        HEARING_IMPAIRED;

        private static SubtitleType parse(final String apiosCode) {
            if (apiosCode.matches("[A-Z]+-ST(A|F)")) {
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

        AudioLanguage getAudioLanguage();

        SubtitleLanguage getSubtitleLanguage();

        SubtitleType getSubtitleType();

        boolean hasAnySubtitle();

        boolean hasSubtitleFull();

        boolean hasSubtitlePartial();

        boolean hasSubtitleForHearingImpaired();
    }

    public static VersionInfo parseVersionInfo(final String apiosCode) {
        final SubtitleType subtitleType = SubtitleType.parse(apiosCode);
        final AudioLanguage audioLanguage = AudioLanguage.parse(apiosCode);
        final SubtitleLanguage subtitleLanguage = SubtitleLanguage.parse(apiosCode);
        final VersionType versionType = VersionType.parse(apiosCode);
        return new VersionInfo() {
            @Override
            public SubtitleType getSubtitleType() {
                return subtitleType;
            }

            @Override
            public SubtitleLanguage getSubtitleLanguage() {
                return subtitleLanguage;
            }

            @Override
            public AudioLanguage getAudioLanguage() {
                return audioLanguage;
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
        };
    }

    /* 1 = No subtitle, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
    public static String get_user_format_from_format_code(final short version) {
        switch (version) {
        case format_intern_german:
            return "no_subtitle";
        case format_intern_french:
            return "no_subtitle";
        case format_intern_subtitled:
            return "subtitled";
        case format_intern_subtitled_for_disabled_people:
            return "subtitled_handicapped";
        case format_intern_audio_description:
            return "audio_description";
        case format_intern_unknown:
            return "no_subtitle";
        default:
            /* Developer mistake */
            return "WTF_PLUGIN_FAILED";
        }
    }

    private String artetv_api_language(final String lang) {
        if ("de".equals(lang)) {
            return "D";
        } else {
            return "F";
        }
    }

    private String getUrlFilename() {
        final String urlfilename = new Regex(parameter, "([A-Za-z0-9\\-]+)$").getMatch(0);
        return urlfilename;
    }

    private DownloadLink createofflineDownloadLink(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

    private String convertDateFormat(String s) {
        if (s == null) {
            return null;
        }
        if (s.matches("\\d+/\\d+/\\d+ \\d+:\\d+:\\d+ \\+\\d+")) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.getDefault());
            SimpleDateFormat convdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            try {
                Date date = null;
                try {
                    date = df.parse(s);
                    s = convdf.format(date);
                } catch (Throwable e) {
                    df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.ENGLISH);
                    date = df.parse(s);
                    s = convdf.format(date);
                }
            } catch (Throwable e) {
                return s;
            }
        }
        return s;
    }

    private String getUrlLang() {
        final String lang = new Regex(parameter, "^https?://[^/]+(?:/guide)?/(\\w+)/.+").getMatch(0);
        if (lang != null && !lang.matches("(de|fr|en|pl|it|es)")) {
            return null;
        } else {
            return lang;
        }
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "dd/MM/yyyy HH:mm:ss Z", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

    private String getFormattedFilePackageName(final String date, final String title) {
        @SuppressWarnings("deprecation")
        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        String formattedPackageName = cfg.getStringProperty(jd.plugins.hoster.ArteTv.CUSTOM_PACKAGE_NAME_PATTERN, jd.plugins.hoster.ArteTv.default_CUSTOM_PACKAGE_NAME_PATTERN);
        if (formattedPackageName == null || formattedPackageName.equals("")) {
            formattedPackageName = jd.plugins.hoster.ArteTv.default_CUSTOM_PACKAGE_NAME_PATTERN;
        }
        if (!formattedPackageName.contains("*title*")) {
            return "Custom filename pattern is missing *title*.";
        }
        if (formattedPackageName.contains("*date*")) {
            if (date != null) {
                formattedPackageName = formattedPackageName.replace("*date*", date);
            } else {
                formattedPackageName = formattedPackageName.replace("*date*", "");
            }
        }
        if (title != null) {
            formattedPackageName = formattedPackageName.replace("*title*", title);
        } else {
            formattedPackageName = formattedPackageName.replace("*title*", "missing_title");
        }
        return formattedPackageName;
    }

    private String getFormattedThumbnailName(final DownloadLink link) {
        @SuppressWarnings("deprecation")
        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        String formattedFileName = cfg.getStringProperty(jd.plugins.hoster.ArteTv.CUSTOM_THUMBNAIL_NAME_PATTERN, jd.plugins.hoster.ArteTv.default_CUSTOM_THUMBNAIL_NAME_PATTERN);
        if (formattedFileName == null || formattedFileName.equals("")) {
            formattedFileName = jd.plugins.hoster.ArteTv.default_CUSTOM_THUMBNAIL_NAME_PATTERN;
        }
        if (!formattedFileName.contains("*title*")) {
            return "Custom filename pattern for thumbnail is missing *title*.";
        }
        final String date = link.getStringProperty("date", null);
        final String title = link.getStringProperty("title", null);
        final String ext = link.getStringProperty("ext", null);
        if (formattedFileName.contains("*date*")) {
            if (date != null) {
                formattedFileName = formattedFileName.replace("*date*", date);
            } else {
                formattedFileName = formattedFileName.replace("*date*", "");
            }
        }
        // Insert title at the end to prevent errors with tags
        if (title != null) {
            formattedFileName = formattedFileName.replace("*title*", title);
        } else {
            formattedFileName = formattedFileName.replace("*title*", "missing_title");
        }
        // Extension will be either replaced according to tag or if not given, appended
        if (formattedFileName.contains("*ext*")) {
            if (ext != null) {
                formattedFileName = formattedFileName.replace("*ext*", ext);
            } else {
                formattedFileName = formattedFileName.replace("*ext*", "");
            }
        } else {
            if (ext != null) {
                formattedFileName = formattedFileName + "." + ext;
            }
        }
        return formattedFileName;
    }

    private String getFormattedFileName(final DownloadLink downloadLink) {
        @SuppressWarnings("deprecation")
        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        String formattedFileName = cfg.getStringProperty(jd.plugins.hoster.ArteTv.CUSTOM_FILE_NAME_PATTERN, jd.plugins.hoster.ArteTv.default_CUSTOM_FILE_NAME_PATTERN);
        if (formattedFileName == null || formattedFileName.equals("")) {
            formattedFileName = jd.plugins.hoster.ArteTv.default_CUSTOM_FILE_NAME_PATTERN;
        }
        if (!formattedFileName.contains("*title*")) {
            return "Custom filename pattern is missing *title*.";
        }
        final String date = downloadLink.getStringProperty("date", null);
        final String title = downloadLink.getStringProperty("title", null);
        final String vpi = downloadLink.getStringProperty("vpi", null);
        final String language = downloadLink.getStringProperty("versionLibelle", null);
        final String shortlanguage = downloadLink.getStringProperty("versionShortLibelle", null);
        final String resolution = downloadLink.getStringProperty("resolution", null);
        final String height = downloadLink.getStringProperty("height", null);
        final String width = downloadLink.getStringProperty("width", null);
        final String bitrate = downloadLink.getStringProperty("bitrate", null);
        final String ext = downloadLink.getStringProperty("ext", null);
        if (formattedFileName.contains("*date*")) {
            if (date != null) {
                formattedFileName = formattedFileName.replace("*date*", date);
            } else {
                formattedFileName = formattedFileName.replace("*date*", "");
            }
        }
        if (formattedFileName.contains("*vpi*")) {
            if (vpi != null) {
                formattedFileName = formattedFileName.replace("*vpi*", vpi);
            } else {
                formattedFileName = formattedFileName.replace("*vpi*", "");
            }
        }
        if (formattedFileName.contains("*language*")) {
            if (language != null) {
                formattedFileName = formattedFileName.replace("*language*", language);
            } else {
                formattedFileName = formattedFileName.replace("*language*", "");
            }
        }
        if (formattedFileName.contains("*shortlanguage*")) {
            if (shortlanguage != null) {
                formattedFileName = formattedFileName.replace("*shortlanguage*", shortlanguage);
            } else {
                formattedFileName = formattedFileName.replace("*shortlanguage*", "");
            }
        }
        if (formattedFileName.contains("*resolution*")) {
            if (resolution != null) {
                formattedFileName = formattedFileName.replace("*resolution*", resolution);
            } else {
                formattedFileName = formattedFileName.replace("*resolution*", "");
            }
        }
        if (formattedFileName.contains("*height*")) {
            if (height != null) {
                formattedFileName = formattedFileName.replace("*height*", height);
            } else {
                formattedFileName = formattedFileName.replace("*height*", "");
            }
        }
        if (formattedFileName.contains("*width*")) {
            if (width != null) {
                formattedFileName = formattedFileName.replace("*width*", width);
            } else {
                formattedFileName = formattedFileName.replace("*width*", "");
            }
        }
        if (formattedFileName.contains("*bitrate*")) {
            if (bitrate != null) {
                formattedFileName = formattedFileName.replace("*bitrate*", bitrate);
            } else {
                formattedFileName = formattedFileName.replace("*bitrate*", "");
            }
        }
        // Insert title at the end to prevent errors with tags
        if (title != null) {
            formattedFileName = formattedFileName.replace("*title*", title);
        } else {
            formattedFileName = formattedFileName.replace("*title*", "missing_title");
        }
        // Extension will be either replaced according to tag or if not given, appended
        if (formattedFileName.contains("*ext*")) {
            if (ext != null) {
                formattedFileName = formattedFileName.replace("*ext*", ext);
            } else {
                formattedFileName = formattedFileName.replace("*ext*", "");
            }
        } else {
            if (ext != null) {
                formattedFileName = formattedFileName + "." + ext;
            }
        }
        return formattedFileName;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}