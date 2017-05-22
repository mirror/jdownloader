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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "arte.tv", "concert.arte.tv", "creative.arte.tv", "future.arte.tv", "cinema.arte.tv", "theoperaplatform.eu", "info.arte.tv" }, urls = { "https?://(?:www\\.)?arte\\.tv/.+", "https?://concert\\.arte\\.tv/.+", "https?://creative\\.arte\\.tv/(?:de|fr)/(?!scald_dmcloud_json).+", "https?://future\\.arte\\.tv/.+", "https?://cinema\\.arte\\.tv/.+", "https?://(?:www\\.)?theoperaplatform\\.eu/.+", "https?://info\\.arte\\.tv/.+" })
public class ArteMediathekDecrypter extends PluginForDecrypt {
    private static final String     EXCEPTION_LINKOFFLINE                       = "EXCEPTION_LINKOFFLINE";
    private static final String     TYPE_CONCERT                                = "https?://concert\\.arte\\.tv/(?:de|fr)/[a-z0-9\\-]+";
    private static final String     TYPE_CREATIVE                               = "https?://creative\\.arte\\.tv/(?:de|fr)/.+";
    private static final String     TYPE_FUTURE                                 = "https?://future\\.arte\\.tv/.+";
    private static final String     TYPE_ARTETV_GUIDE                           = "https?://(?:www\\.)?arte\\.tv/guide/[a-z]{2}/\\d+\\-\\d+(?:\\-[ADF])?/[a-z0-9\\-_]+.*?";
    private static final String     TYPE_ARTETV_EMBED                           = "https?://(?:www\\.)?arte\\.tv/guide/[A-Za-z]{2}/embed/.+";
    private static final String     TYPE_CINEMA                                 = "https?://cinema\\.arte\\.tv/.+";
    private static final String     TYPE_THEOPERAPLATFORM                       = "https?://(?:www\\.)?theoperaplatform\\.eu/.+";
    private static final String     API_TYPE_GUIDE                              = "^http://(www\\.)?arte\\.tv/papi/tvguide/videos/stream/player/[ADF]/.+\\.json$";
    private static final String     API_TYPE_CINEMA                             = "^https?://api\\.arte\\.tv/api/player/v1/config/[a-z]{2}/([A-Za-z0-9\\-]+)\\?vector=.+";
    private static final String     API_TYPE_OEMBED                             = "https://api.arte.tv/api/player/v1/oembed/[a-z]{2}/([A-Za-z0-9\\-]+)(\\?platform=.+)";
    private static final String     API_TYPE_OTHER                              = "https://api.arte.tv/api/player/v1/config/[a-z]{2}/([A-Za-z0-9\\-]+)(\\?.+)";
    /* ?autostart=0&lifeCycle=1 = get lower qualities too. */
    private static final String     API_HYBRID_URL_1                            = "https://api.arte.tv/api/player/v1/config/%s/%s?autostart=0&lifeCycle=1";
    private static final String     API_HYBRID_URL_2                            = "http://arte.tv/papi/tvguide/videos/stream/player/%s/%s/ALL/ALL.json";
    private static final String     API_HYBRID_URL_3                            = "https://api-preprod.arte.tv/api/player/v1/config/%s/%s?autostart=0&lifeCycle=1";
    private static final String     V_NORMAL                                    = "V_NORMAL";
    private static final String     V_SUBTITLED                                 = "V_SUBTITLED";
    private static final String     V_SUBTITLE_DISABLED_PEOPLE                  = "V_SUBTITLE_DISABLED_PEOPLE";
    private static final String     V_AUDIO_DESCRIPTION                         = "V_AUDIO_DESCRIPTION";
    private static final String     http_300                                    = "http_300";
    private static final String     http_800                                    = "http_800";
    private static final String     http_1500                                   = "http_1500";
    private static final String     http_2200                                   = "http_2200";
    private static final String     LOAD_LANGUAGE_URL                           = "LOAD_LANGUAGE_URL";
    private static final String     LOAD_LANGUAGE_GERMAN                        = "LOAD_LANGUAGE_GERMAN";
    private static final String     LOAD_LANGUAGE_FRENCH                        = "LOAD_LANGUAGE_FRENCH";
    private static final String     THUMBNAIL                                   = "THUMBNAIL";
    private static final String     FAST_LINKCHECK                              = "FAST_LINKCHECK";
    private static final short      format_intern_german                        = 1;
    private static final short      format_intern_french                        = 2;
    private static final short      format_intern_subtitled                     = 3;
    private static final short      format_intern_subtitled_for_disabled_people = 4;
    private static final short      format_intern_audio_description             = 5;
    private static final short      format_intern_unknown                       = 6;
    final String[]                  formats                                     = { http_300, http_800, http_1500, http_2200 };
    private static final String     LANG_DE                                     = "de";
    private static final String     LANG_FR                                     = "fr";
    private String                  parameter;
    private ArrayList<DownloadLink> decryptedLinks                              = new ArrayList<DownloadLink>();
    private String                  example_arte_vp_url                         = null;

    @SuppressWarnings("deprecation")
    public ArteMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    /** TODO: Re-Write parts of this - remove the bad language handling! */
    /*
     * E.g. smil (rtmp) url:
     * http://www.arte.tv/player/v2/webservices/smil.smil?json_url=http%3A%2F%2Farte.tv%2Fpapi%2Ftvguide%2Fvideos%2Fstream
     * %2Fplayer%2FD%2F045163-000_PLUS7-D%2FALL%2FALL.json&smil_entries=RTMP_SQ_1%2CRTMP_MQ_1%2CRTMP_LQ_1
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        int foundFormatsNum = 0;
        parameter = param.toString();
        this.example_arte_vp_url = null;
        final ArrayList<String> selectedLanguages = new ArrayList<String>();
        String title = getUrlFilename();
        String fid = null;
        String thumbnailUrl = null;
        final String plain_domain = new Regex(parameter, "https?://(?:www\\.)?([^/]+)/").getMatch(0);
        final String plain_domain_decrypter = plain_domain + ".artejd_decrypted_jd";
        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String hybridAPIUrl = null;
        String date_formatted = "-";
        final boolean fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(new int[] { 410, 503 });
        br.getPage(parameter);
        final boolean isGerman = "de".equalsIgnoreCase(TranslationFactory.getDesiredLanguage());
        final boolean isFrancais = "fr".equalsIgnoreCase(TranslationFactory.getDesiredLanguage());
        String videoid_base = null;
        String video_section = null;
        try {
            if (this.br.getHttpConnection().getResponseCode() != 200 && this.br.getHttpConnection().getResponseCode() != 301) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            /* First we need to have some basic data - this part is link-specific. */
            if (parameter.matches(TYPE_ARTETV_GUIDE) || parameter.matches(TYPE_ARTETV_EMBED)) {
                videoid_base = new Regex(this.parameter, "/guide/[A-Za-z]{2}/(\\d+\\-\\d+(?:\\-[ADF])?)").getMatch(0);
                int status = br.getHttpConnection().getResponseCode();
                if (br.getHttpConnection().getResponseCode() == 400 || br.containsHTML("<h1>Error 404</h1>") || (!parameter.contains("tv/guide/") && status == 200)) {
                    decryptedLinks.add(createofflineDownloadLink(parameter));
                    return decryptedLinks;
                }
                /* new arte+7 handling */
                if (status != 200) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                /* Make sure not to download trailers or announcements to movies by grabbing the whole section of the videoplayer! */
                video_section = br.getRegex("(<section class=\\'focus\\' data-action=.*?</section>)").getMatch(0);
                if (video_section == null) {
                    video_section = this.br.toString();
                }
                this.example_arte_vp_url = getArteVPUrl(video_section);
                if (this.example_arte_vp_url == null) {
                    /* We cannot be entirely sure but no videourl == we have no video == offline link */
                    decryptedLinks.add(createofflineDownloadLink(parameter));
                    return decryptedLinks;
                }
            } else {
                /* TODO: Find out when we can actually do this. */
                videoid_base = new Regex(this.parameter, "/(\\d+\\-\\d+(?:\\-[ADF])?)").getMatch(0);
                video_section = this.br.toString();
                scanForExternalUrls();
                if (decryptedLinks.size() > 0) {
                    return decryptedLinks;
                }
                Regex playerinfo = this.br.getRegex("\"(https?://[^/]+)/(?:[a-z]{2})/player/(\\d+)\"");
                final String player_host = playerinfo.getMatch(0);
                fid = playerinfo.getMatch(1);
                if (player_host != null && fid != null) {
                    hybridAPIUrl = player_host + "/%s/player/%s";
                } else {
                    /* Fallback - maybe they simply embed a normal ARTE TYPE_GUIDE video ... */
                    playerinfo = this.br.getRegex("api\\.arte\\.tv/api/player/v1/config/[a-z]{2}/([A-Za-z0-9\\-]+)(\\?[^<>\"\\']+)");
                    final String link_ending = playerinfo.getMatch(1);
                    fid = br.getRegex("api\\.arte\\.tv/api/player/v1/config/(?:de|fr)/([A-Za-z0-9\\-]+)").getMatch(0);
                    if (fid != null && link_ending != null) {
                        hybridAPIUrl = API_HYBRID_URL_1 + link_ending;
                    } else {
                        this.example_arte_vp_url = getArteVPUrl(video_section);
                        if (this.example_arte_vp_url == null) {
                            /* Seems like there is no content for us to download ... */
                            logger.info("Found no downloadable content");
                            return decryptedLinks;
                        }
                    }
                }
            }
            if (this.example_arte_vp_url != null || fid == null) {
                if (this.example_arte_vp_url.matches(API_TYPE_OTHER)) {
                    fid = videoid_base;
                    hybridAPIUrl = API_HYBRID_URL_1;
                } else if (this.example_arte_vp_url.matches(API_TYPE_OEMBED)) {
                    /*
                     * first "ALL" can e.g. be replaced with "HBBTV" to only get the HBBTV qualities. Also possible:
                     * https://api.arte.tv/api/player/v1/config/fr/051939-015-A?vector=CINEMA
                     */
                    final Regex info = new Regex(this.example_arte_vp_url, API_TYPE_OEMBED);
                    fid = info.getMatch(0);
                    final String link_ending = info.getMatch(1);
                    hybridAPIUrl = API_HYBRID_URL_1 + link_ending;
                } else {
                    fid = new Regex(example_arte_vp_url, "/stream/player/[A-Za-z]{1,5}/([^<>\"/]*?)/").getMatch(0);
                    /*
                     * first "ALL" can e.g. be replaced with "HBBTV" to only get the HBBTV qualities. Also possible:
                     * https://api.arte.tv/api/player/v1/config/fr/051939-015-A?vector=CINEMA
                     */
                    hybridAPIUrl = API_HYBRID_URL_2;
                }
                if (fid == null) {
                    /* Initially this complete errorhandling- was only for types: TYPE_ARTETV_GUIDE, TYPE_ARTETV_EMBED */
                    if (!br.containsHTML("arte_vp_config=")) {
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    /* Title is only available on DVD (buyable) */
                    if (video_section.contains("class='badge-vod'>VOD DVD</span>")) {
                        title = "only_available_on_DVD_" + title;
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    } else if (video_section.contains("class='badge-live'")) {
                        title = "livestreams_are_not_supported_" + title;
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    throw new DecrypterException("Decrypter broken: " + parameter);
                }
            }
            /*
             * Now let's check which languages the user wants. We'll do the quality selection later but we have to access webpages to get
             * the different languages so let's keep the load low by only grabbing what the user selected.
             */
            final boolean germanSelected = cfg.getBooleanProperty(LOAD_LANGUAGE_GERMAN, true);
            final boolean francaisSelected = cfg.getBooleanProperty(LOAD_LANGUAGE_FRENCH, true);
            if (cfg.getBooleanProperty(LOAD_LANGUAGE_URL, true)) {
                selectedLanguages.add(this.getUrlLang());
            } else {
                if (isGerman && germanSelected) {
                    selectedLanguages.add(LANG_DE);
                } else if (isFrancais && francaisSelected) {
                    selectedLanguages.add(LANG_FR);
                } else {
                    if (germanSelected) {
                        selectedLanguages.add(LANG_DE);
                    }
                    if (francaisSelected) {
                        selectedLanguages.add(LANG_FR);
                    }
                }
            }
            final HashSet<String> linkIDs = new HashSet<String>();
            /* Finally, grab all we can get (in the selected language(s)) */
            for (final String selectedLanguage : selectedLanguages) {
                final String apiurl = this.getAPIUrl(hybridAPIUrl, selectedLanguage, fid);
                br.getPage(apiurl);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    /* In most cases this simply means that one of the selected languages is not available so let's go on. */
                    logger.info("This language is not available: " + selectedLanguage);
                    continue;
                }
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                final LinkedHashMap<String, Object> videoJsonPlayer = (LinkedHashMap<String, Object>) entries.get("videoJsonPlayer");
                final Object error_info = videoJsonPlayer.get("custom_msg");
                if (error_info != null) {
                    final LinkedHashMap<String, Object> errorInfomap = (LinkedHashMap<String, Object>) error_info;
                    final String errmsg = (String) errorInfomap.get("msg");
                    final String type = (String) errorInfomap.get("type");
                    if ((type.equals("error") || type.equals("info")) && errmsg != null) {
                        title = errmsg + "_" + title;
                    } else {
                        title = "Unknown_error_" + title;
                        logger.warning("Unknown error");
                    }
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                // final String sourceURL = (String) videoJsonPlayer.get("VTR");
                /* Title is sometimes null e.g. for expired videos */
                final String json_title = (String) videoJsonPlayer.get("VTI");
                if (json_title != null) {
                    title = encodeUnicode(json_title);
                }
                String description = (String) videoJsonPlayer.get("VDE");
                if (description == null) {
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
                    date_formatted = formatDate(vra);
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
                final Object vsro = videoJsonPlayer.get("VSR");
                if (!(vsro instanceof LinkedHashMap)) {
                    /* No source available --> Video cannot be played --> Browser would says "Error code 2" then */
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                final Collection<Object> vsr_quals = ((LinkedHashMap<String, Object>) vsro).values();
                /* One packagename for every language */
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                for (final Object o : vsr_quals) {
                    foundFormatsNum++;
                    final LinkedHashMap<String, Object> qualitymap = (LinkedHashMap<String, Object>) o;
                    final String url = (String) qualitymap.get("url");
                    if (!url.startsWith("http") || url.contains(".m3u8")) {
                        continue;
                    }
                    final Object widtho = qualitymap.get("width");
                    final Object heighto = qualitymap.get("height");
                    String videoresolution = "";
                    String width = "";
                    String height = "";
                    final int videoBitrate = ((Number) qualitymap.get("bitrate")).intValue();
                    if (widtho != null && heighto != null) {
                        /* These parameters are available in 95+% of all cases! */
                        width = ((Number) qualitymap.get("width")).toString();
                        height = ((Number) qualitymap.get("height")).toString();
                        videoresolution = width + "x" + height;
                    }
                    final String quality_intern = "http_" + videoBitrate;
                    if (!cfg.getBooleanProperty(quality_intern, true)) {
                        /* User does not want this bitrate --> Skip it */
                        logger.info("Skipping " + quality_intern);
                        continue;
                    }
                    final String versionCode = (String) qualitymap.get("versionCode");
                    final String versionLibelle = (String) qualitymap.get("versionLibelle");
                    final String versionShortLibelle = (String) qualitymap.get("versionShortLibelle");
                    final VersionInfo versionInfo = parseVersionInfo(versionCode);
                    if (!cfg.getBooleanProperty(V_NORMAL, true) && !(SubtitleType.FULL.equals(versionInfo.getSubtitleType()) || SubtitleType.HEARING_IMPAIRED.equals(versionInfo.getSubtitleType()))) {
                        /* User does not want the non-subtitled version */
                        continue;
                    }
                    if (!cfg.getBooleanProperty(V_SUBTITLE_DISABLED_PEOPLE, true) && SubtitleType.HEARING_IMPAIRED.equals(versionInfo.getSubtitleType())) {
                        /* User does not want the subtitled-for-.disabled-people version */
                        continue;
                    }
                    if (!cfg.getBooleanProperty(V_SUBTITLED, true) && SubtitleType.FULL.equals(versionInfo.getSubtitleType())) {
                        /* User does not want the subtitled version */
                        continue;
                    }
                    if (!francaisSelected && SubtitleLanguage.FRANCAIS.equals(versionInfo.getSubtitleLanguage())) {
                        continue;
                    }
                    if (!germanSelected && SubtitleLanguage.GERMAN.equals(versionInfo.getSubtitleLanguage())) {
                        continue;
                    }
                    // TODO
                    // if (!cfg.getBooleanProperty(V_AUDIO_DESCRIPTION, true) && format_code == format_intern_audio_description) {
                    // /* User does not want the audio-description version */
                    // continue;
                    // }
                    final String linkID = getHost() + "://" + vpi + "/" + versionInfo.toString() + "/" + quality_intern;
                    if (linkIDs.add(linkID)) {
                        final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                        final String filename = date_formatted + "_arte_" + title + "_" + vpi + "_" + "_" + versionLibelle + "_" + versionShortLibelle + "_" + videoresolution + "_" + videoBitrate + ".mp4";
                        link.setFinalFileName(filename);
                        link.setContentUrl(parameter);
                        link._setFilePackage(fp);
                        link.setProperty("versionCode", versionCode);
                        link.setProperty("directURL", url);
                        link.setProperty("directName", filename);
                        link.setProperty("quality_intern", quality_intern);
                        link.setProperty("langShort", selectedLanguage);
                        link.setProperty("mainlink", parameter);
                        link.setProperty("apiurl", apiurl);
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
                        decryptedLinks.add(link);
                    }
                }
            }
            /* User did not activate all versions --> Show this info in filename so he can correct his mistake. */
            if (decryptedLinks.isEmpty() && foundFormatsNum > 0) {
                title = jd.plugins.hoster.ArteTv.getPhrase("ERROR_USER_NEEDS_TO_CHANGE_FORMAT_SELECTION") + title;
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            /* Check if user wants to download the thumbnail as well. */
            if (cfg.getBooleanProperty(THUMBNAIL, true) && thumbnailUrl != null) {
                final DownloadLink link = createDownloadlink("directhttp://" + thumbnailUrl);
                link.setFinalFileName(title + ".jpg");
                decryptedLinks.add(link);
            }
            if (decryptedLinks.size() > 1) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(date_formatted + "_arte_" + title);
                fp.addLinks(decryptedLinks);
            }
        } catch (final Exception e) {
            if (e instanceof DecrypterException && e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                final DownloadLink offline = createofflineDownloadLink(parameter);
                offline.setFinalFileName(title);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            throw e;
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private void scanForExternalUrls() {
        /* Return external links if existant */
        final String currentHost = new Regex(this.br.getURL(), "https?://([^/]*?)/.+").getMatch(0);
        final String[] externURLsRegexes = { "data\\-url=\"(http://creative\\.arte\\.tv/(de|fr)/scald_dmcloud_json/\\d+)", "(youtube\\.com/embed/[^<>\"]*?)\"", "(https?://(?:www\\.)?arte\\.tv/guide/[A-Za-z]{2}/embed/[^/\"]+/[^/\"]+)" };
        for (final String externURLRegex : externURLsRegexes) {
            final String[] externURLs = br.getRegex(externURLRegex).getColumn(0);
            if (externURLs != null && externURLs.length > 0) {
                for (String externURL : externURLs) {
                    if (externURL.matches("youtube\\.com/embed/.+")) {
                        externURL = "https://" + externURL;
                    } else if (!externURL.startsWith("http")) {
                        /* TODO: http://cinema.arte.tv/fr/magazine/court-circuit */
                        externURL = "http://" + currentHost + externURL;
                    }
                    final DownloadLink dl = createDownloadlink(externURL);
                    decryptedLinks.add(dl);
                }
            }
        }
    }

    private String getArteVPUrl(final String source) {
        String vp_url = new Regex(source, "arte_vp_url=(?:\"|\\')(http[^<>\"\\']*?)(?:\"|\\')").getMatch(0);
        if (vp_url == null) {
            vp_url = new Regex(source, "arte_vp_url_oembed=(?:\"|\\')(http[^<>\"\\']*?)(?:\"|\\')").getMatch(0);
        }
        if (vp_url == null) {
            /*
             * E.g.
             * https%3A%2F%2Fapi.arte.tv%2Fapi%2Fplayer%2Fv1%2Fconfig%2Fde%2F062222-000-A%3Fautostart%3D0%26lifeCycle%3D1&amp;lang=de_DE
             * &amp;config=arte_tvguide
             */
            /* We actually don't necessarily use these urls but the existance of them is an indicator for us on which API-url to use. */
            vp_url = new Regex(source, "<iframe[^>]*?src=\"https?://(?:www\\.)arte\\.tv/player/v\\d+/index\\.php\\?json_url=(http[^<>\"]+)\">[^>]*?</iframe>").getMatch(0);
            if (vp_url != null) {
                vp_url = Encoding.htmlDecode(vp_url);
            }
        }
        return vp_url;
    }

    private static enum VideoLanguage {
        FRANCAIS,
        GERMAN,
        OTHER;
        private static VideoLanguage parse(final String apiosCode) {
            final String originalVersion = new Regex(apiosCode, "^VO(F|A)($|-)").getMatch(0);
            if (originalVersion != null) {
                // original version
                if ("F".equals(originalVersion)) {
                    return FRANCAIS;
                } else if ("A".equals(originalVersion)) {
                    return GERMAN;
                } else {
                    return OTHER;
                }
            }
            final String nonOriginalVersion = new Regex(apiosCode, "^V(F|A)($|-)").getMatch(0);
            if (nonOriginalVersion != null) {
                // non-original version
                if ("F".equals(nonOriginalVersion)) {
                    return FRANCAIS;
                } else if ("A".equals(nonOriginalVersion)) {
                    return GERMAN;
                } else {
                    return OTHER;
                }
            }
            return OTHER;
        }
    }

    private static enum SubtitleLanguage {
        FRANCAIS,
        GERMAN,
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
            return OTHER;
        }
    }

    private static enum SubtitleType {
        NONE,
        FULL,
        PARTIAL,
        HEARING_IMPAIRED;
        private static SubtitleType parse(final String apiosCode) {
            if (StringUtils.equalsIgnoreCase(apiosCode, "VF-STF") || StringUtils.equalsIgnoreCase(apiosCode, "VA-STA") || StringUtils.equalsIgnoreCase(apiosCode, "VOF-STF") || StringUtils.equalsIgnoreCase(apiosCode, "VOA-STA")) {
                return PARTIAL;
            } else if (StringUtils.containsIgnoreCase(apiosCode, "-STM")) {
                return HEARING_IMPAIRED;
            } else if (StringUtils.containsIgnoreCase(apiosCode, "-ST")) {
                return FULL;
            } else {
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

    private static interface VersionInfo {
        VersionType getVersionType();

        VideoLanguage getVideoLanguage();

        SubtitleLanguage getSubtitleLanguage();

        SubtitleType getSubtitleType();

        boolean hasSubtitle();
    }

    private VersionInfo parseVersionInfo(final String apiosCode) {
        final SubtitleType subtitleType = SubtitleType.parse(apiosCode);
        final VideoLanguage videoLanguage = VideoLanguage.parse(apiosCode);
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
            public VideoLanguage getVideoLanguage() {
                return videoLanguage;
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
                sb.append(getVideoLanguage());
                if (hasSubtitle()) {
                    sb.append("_");
                    sb.append(getSubtitleType());
                    sb.append("_");
                    sb.append(getSubtitleLanguage());
                }
                return sb.toString();
            }

            @Override
            public boolean hasSubtitle() {
                return !SubtitleType.NONE.equals(getSubtitleType());
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
            /* Obviously this should never happen */
            return "WTF_PLUGIN_FAILED";
        }
    }

    private String getAPIUrl(final String hybridAPIlink, final String lang, final String id) {
        String apilink;
        if (example_arte_vp_url != null && this.example_arte_vp_url.matches(API_TYPE_GUIDE)) {
            final String api_language = this.artetv_api_language(lang);
            final String id_without_lang = id.substring(0, id.length() - 1);
            final String id_with_lang = id_without_lang + api_language;
            apilink = String.format(hybridAPIlink, api_language, id_with_lang);
        } else {
            apilink = String.format(hybridAPIlink, lang, id);
        }
        return apilink;
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

    @SuppressWarnings("unused")
    private String getURLFilename(final String parameter) {
        if (parameter.matches(TYPE_CONCERT)) {
            return new Regex(parameter, "concert\\.arte\\.tv/(de|fr)/(.+)").getMatch(1);
        } else {
            return new Regex(parameter, "arte\\.tv/guide/[a-z]{2}/(.+)").getMatch(0);
        }
    }

    private String getUrlLang() {
        final String lang = new Regex(parameter, "^https?://[^/]+(?:/guide)?/(\\w+)/.+").getMatch(0);
        if (lang != null && !lang.matches("(de|fr)")) {
            /* TODO: Maybe add support for "en" and/or others */
            return "de";
        }
        return lang;
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}