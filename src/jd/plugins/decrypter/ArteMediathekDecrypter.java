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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "arte.tv", "concert.arte.tv", "creative.arte.tv" }, urls = { "http://www\\.arte\\.tv/guide/(de|fr)/\\d+\\-\\d+/[a-z0-9\\-_]+", "http://concert\\.arte\\.tv/(de|fr)/[a-z0-9\\-]+", "http://creative\\.arte\\.tv/(de|fr)/(?!scald_dmcloud_json)[a-z0-9\\-]+(/[a-z0-9\\-]+)?" }, flags = { 0, 0, 0 })
public class ArteMediathekDecrypter extends PluginForDecrypt {

    private static final String EXCEPTION_LINKOFFLINE      = "EXCEPTION_LINKOFFLINE";

    private static final String TYPE_CONCERT               = "http://(www\\.)?concert\\.arte\\.tv/(de|fr)/[a-z0-9\\-]+";
    private static final String TYPE_CREATIVE              = "http://(www\\.)?creative\\.arte\\.tv/(de|fr)/[a-z0-9\\-]+(/[a-z0-9\\-]+)?";
    private static final String TYPE_GUIDE                 = "http://www\\.arte\\.tv/guide/(de|fr)/\\d+\\-\\d+/[a-z0-9\\-_]+";

    private static final String V_NORMAL                   = "V_NORMAL";
    private static final String V_SUBTITLED                = "V_SUBTITLED";
    private static final String V_SUBTITLE_DISABLED_PEOPLE = "V_SUBTITLE_DISABLED_PEOPLE";
    private static final String V_AUDIO_DESCRIPTION        = "V_AUDIO_DESCRIPTION";
    private static final String http_300                   = "http_300";
    private static final String http_800                   = "http_800";
    private static final String http_1500                  = "http_1500";
    private static final String http_2200                  = "http_2200";
    private static final String LOAD_LANGUAGE_URL          = "LOAD_LANGUAGE_URL";
    private static final String LOAD_LANGUAGE_GERMAN       = "LOAD_LANGUAGE_GERMAN";
    private static final String LOAD_LANGUAGE_FRENCH       = "LOAD_LANGUAGE_FRENCH";
    private static final String THUMBNAIL                  = "THUMBNAIL";
    private static final String FAST_LINKCHECK             = "FAST_LINKCHECK";

    final String[]              formats                    = { http_300, http_800, http_1500, http_2200 };

    private int                 languageVersion            = 1;
    private String              parameter;

    @SuppressWarnings("deprecation")
    public ArteMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    /*
     * E.g. smil (rtmp) url:
     * http://www.arte.tv/player/v2/webservices/smil.smil?json_url=http%3A%2F%2Farte.tv%2Fpapi%2Ftvguide%2Fvideos%2Fstream
     * %2Fplayer%2FD%2F045163-000_PLUS7-D%2FALL%2FALL.json&smil_entries=RTMP_SQ_1%2CRTMP_MQ_1%2CRTMP_LQ_1
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        /* Load host plugin to access some static methods later */
        JDUtilities.getPluginForHost("arte.tv");
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString();
        ArrayList<String> selectedFormats = new ArrayList<String>();
        ArrayList<String> selectedLanguages = new ArrayList<String>();
        HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
        String title = getUrlFilename();
        String fid;
        String thumbnailUrl = null;
        final String plain_domain = new Regex(parameter, "([a-z]+\\.arte\\.tv)").getMatch(0);
        final String plain_domain_decrypter = plain_domain.replace("arte.tv", "artejd_decrypted_jd.tv");
        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String hybridAPIUrl = null;
        final boolean fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);

        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        try {
            /* First we need to have some basic data - this part is link-specific. */
            if (parameter.matches(TYPE_CONCERT)) {
                if (!br.containsHTML("id=\"section\\-player\"")) {
                    decryptedLinks.add(createofflineDownloadLink(parameter));
                    return decryptedLinks;
                }
                if (!br.containsHTML("class=\"video\\-container\"")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                fid = br.getRegex("\"http://concert\\.arte\\.tv/[a-z]{2}/player/(\\d+)").getMatch(0);
                hybridAPIUrl = "http://concert.arte.tv/%s/player/%s";
            } else if (parameter.matches(TYPE_CREATIVE)) {
                /* Return external links if existant */
                final String[] externURLsRegexes = { "data\\-url=\"(http://creative\\.arte\\.tv/(de|fr)/scald_dmcloud_json/\\d+)", "src=\"(https?://(?:www\\.)?youtube\\.com/embed/[^<>\"]*?)\"" };
                for (final String externURLRegex : externURLsRegexes) {
                    final String[] externURLs = br.getRegex(externURLRegex).getColumn(0);
                    if (externURLs != null && externURLs.length > 0) {
                        for (final String externURL : externURLs) {
                            decryptedLinks.add(createDownloadlink(externURL));
                        }
                    }
                }
                if (decryptedLinks.size() > 0) {
                    return decryptedLinks;
                }
                if (!br.containsHTML("class=\"video\\-container")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                fid = br.getRegex("\"http://creative\\.arte\\.tv/[a-z]{2}/player/(\\d+)").getMatch(0);
                hybridAPIUrl = "http://creative.arte.tv/%s/player/%s";
            } else {
                if (br.getRedirectLocation() != null) {
                    br.getPage(br.getRedirectLocation());
                }
                int status = br.getHttpConnection().getResponseCode();
                if (br.getHttpConnection().getResponseCode() == 400 || br.containsHTML("<h1>Error 404</h1>") || (!parameter.contains("tv/guide/") && status == 200)) {
                    decryptedLinks.add(createofflineDownloadLink(parameter));
                    return decryptedLinks;
                }
                /* new arte+7 handling */
                if (status == 301 || status == 302) {
                    br.setFollowRedirects(true);
                    if (br.getRedirectLocation() != null) {
                        parameter = br.getRedirectLocation();
                        br.getPage(parameter);
                    }
                } else if (status != 200) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                /* Make sure not to download trailers or announcements to movies by grabbing the whole section of the videoplayer! */
                final String video_section = br.getRegex("(<section class=\\'focus\\' data-action=.*?</section>)").getMatch(0);
                if (video_section == null) {
                    return null;
                }
                fid = new Regex(video_section, "/stream/player/[A-Za-z]{1,5}/([^<>\"/]*?)/").getMatch(0);
                if (fid == null) {
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
                /*
                 * first "ALL" can e.g. be replaced with "HBBTV" to only get the HBBTV qualities. Also possible:
                 * https://api.arte.tv/api/player/v1/config/fr/051939-015-A?vector=CINEMA
                 */
                hybridAPIUrl = "http://org-www.arte.tv/papi/tvguide/videos/stream/player/%s/%s/ALL/ALL.json";
            }
            if (fid == null) {
                return null;
            }
            /*
             * Now let's check which languages the user wants. We'll do the quality selection later but we have to access webpages to get
             * the different languages so let's keep the load low by only grabbing what the user selected.
             */
            if (cfg.getBooleanProperty(LOAD_LANGUAGE_URL, true)) {
                selectedLanguages.add(this.getUrlLang());
            } else {
                if (cfg.getBooleanProperty(LOAD_LANGUAGE_GERMAN, true)) {
                    selectedLanguages.add("de");
                }
                if (cfg.getBooleanProperty(LOAD_LANGUAGE_FRENCH, true)) {
                    selectedLanguages.add("fr");
                }
            }
            /* Finally, grab all we can get (in the selected language(s)) */
            for (final String selectedLanguage : selectedLanguages) {
                setSelectedLang_format_code(selectedLanguage);
                final String apiurl = this.getAPIUrl(hybridAPIUrl, selectedLanguage, fid);
                br.getPage(apiurl);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    /* In most cases this simply means that one of the selected languages is not available so let's go on. */
                    logger.info("This language is not available: " + selectedLanguage);
                    continue;
                }
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                final LinkedHashMap<String, Object> videoJsonPlayer = (LinkedHashMap<String, Object>) entries.get("videoJsonPlayer");
                // final String sourceURL = (String) videoJsonPlayer.get("VTR");
                /* Title is sometimes null e.g. for expired videos */
                final String json_title = (String) videoJsonPlayer.get("VTI");
                if (json_title != null) {
                    title = encodeUnicode((String) videoJsonPlayer.get("VTI"));
                }
                final String description = (String) videoJsonPlayer.get("VDE");
                final String errormessage = (String) entries.get("msg");
                if (errormessage != null) {
                    final DownloadLink offline = createofflineDownloadLink(parameter);
                    offline.setFinalFileName(title + errormessage);
                    try {
                        offline.setComment(description);
                    } catch (final Throwable e) {
                        /* Not available in 0.9.581 Stable */
                    }
                    ret.add(offline);
                    return ret;
                }
                if (thumbnailUrl == null) {
                    thumbnailUrl = (String) videoJsonPlayer.get("programImage");
                }
                final String vru = (String) videoJsonPlayer.get("VRU");
                final String vra = (String) videoJsonPlayer.get("VRA");
                if (vru != null && vra != null) {
                    /*
                     * In this case the video is not yet released and there usually is a value "VDB" which contains the release-date of the
                     * video --> But we don't need that - right now, such videos are simply offline and will be added as offline.
                     */
                    final String expired_message = jd.plugins.hoster.ArteTv.getExpireMessage(selectedLanguage, convertDateFormat(vra), convertDateFormat(vru));
                    if (expired_message != null) {
                        final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                        try {
                            link.setComment(description);
                        } catch (final Throwable e) {
                            /* Not available in 0.9.581 Stable */
                        }
                        link.setProperty("offline", true);
                        link.setFinalFileName(expired_message + "_" + title);
                        decryptedLinks.add(link);
                        return decryptedLinks;
                    }
                }
                final Collection<Object> vsr_quals = ((LinkedHashMap<String, Object>) videoJsonPlayer.get("VSR")).values();
                /* One packagename for every language */
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);

                for (final Object o : vsr_quals) {
                    final LinkedHashMap<String, Object> qualitymap = (LinkedHashMap<String, Object>) o;
                    final String protocol = "http";
                    final int videoBitrate = ((Number) qualitymap.get("bitrate")).intValue();
                    final int width = ((Number) qualitymap.get("width")).intValue();
                    final int height = ((Number) qualitymap.get("height")).intValue();
                    final String versionCode = (String) qualitymap.get("versionCode");
                    final String versionLibelle = (String) qualitymap.get("versionLibelle");
                    final String versionShortLibelle = (String) qualitymap.get("versionShortLibelle");
                    final String url = (String) qualitymap.get("url");

                    final int format_code = getFormatCode(versionShortLibelle, versionCode);
                    final String short_lang_current = get_short_lang_from_format_code(format_code);
                    final String quality_intern = selectedLanguage + "_" + get_intern_format_code_from_format_code(format_code) + "_" + protocol + "_" + videoBitrate;
                    final String linkid = fid + "_" + quality_intern;
                    final String filename = title + "_" + getLongLanguage(selectedLanguage) + "_" + get_user_format_from_format_code(format_code) + "_" + width + "x" + height + "_" + videoBitrate + ".mp4";
                    /* Ignore HLS/RTMP versions */
                    if (!url.startsWith("http") || url.contains(".m3u8")) {
                        logger.info("Skipping " + filename + " because it is not a supported streaming format");
                        continue;
                    }
                    if (!short_lang_current.equals(selectedLanguage)) {
                        logger.info("Skipping " + filename + " because it is not the selected language");
                        continue;
                    }
                    final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));

                    link.setFinalFileName(filename);
                    try {
                        /* JD2 only */
                        link.setContentUrl(parameter);
                    } catch (Throwable e) {
                        /* Stable */
                        link.setBrowserUrl(parameter);
                    }
                    link._setFilePackage(fp);
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
                    try {
                        try {
                            link.setComment(description);
                        } catch (final Throwable e) {
                            /* Not available in 0.9.581 Stable */
                        }
                        link.setContentUrl(parameter);
                        link.setLinkID(linkid);
                    } catch (final Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                        link.setBrowserUrl(parameter);
                        link.setProperty("LINKDUPEID", linkid);
                    }
                    if (fastLinkcheck) {
                        link.setAvailable(true);
                    }
                    bestMap.put(quality_intern, link);
                }

                /* Build a list of selected formats */
                for (final String format : formats) {
                    if (cfg.getBooleanProperty(format, true)) {
                        if (cfg.getBooleanProperty(V_NORMAL, true)) {
                            selectedFormats.add(selectedLanguage + "_1_" + format);
                        }
                        /* 1 = German, 2 = French, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
                        if (cfg.getBooleanProperty(V_SUBTITLED, true)) {
                            selectedFormats.add(selectedLanguage + "_3_" + format);
                        }
                        if (cfg.getBooleanProperty(V_SUBTITLE_DISABLED_PEOPLE, true)) {
                            selectedFormats.add(selectedLanguage + "_4_" + format);
                        }
                        if (cfg.getBooleanProperty(V_AUDIO_DESCRIPTION, true)) {
                            selectedFormats.add(selectedLanguage + "_5_" + format);
                        }

                    }
                }
            }

            /* We should always have 3 links (their basic qualities) or more! */
            if (bestMap.isEmpty()) {
                logger.warning("Decrypter broken");
                return null;
            }

            /* Add selected & existing formats */
            for (final String selectedFormat : selectedFormats) {
                final DownloadLink thisformat = bestMap.get(selectedFormat);
                if (thisformat != null) {
                    decryptedLinks.add(thisformat);
                }
            }

            /* Check if user wants to download the thumbnail as well. */
            if (cfg.getBooleanProperty(THUMBNAIL, true) && thumbnailUrl != null) {
                final DownloadLink link = createDownloadlink("directhttp://" + thumbnailUrl);
                link.setFinalFileName(title + ".jpg");
                decryptedLinks.add(link);
            }
            if (decryptedLinks.size() > 1) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
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

    /* Collection of possible values */
    private final String[] versionCodes             = { "VO", "VO-STA", "VOF-STMF", "VA-STMA", "VOF-STA", "VOA-STMA", "VAAUD", "VE", "VF-STMF" };
    private final String[] versionShortLibelleCodes = { "DE", "VA", "VE", "FR", "VF", "OmU", "VO", "VOF", "VOSTF" };

    /* Non-subtitled versions, 3 = Subtitled versions, 4 = Subtitled versions for disabled people, 5 = Audio descriptions */
    private int getFormatCode(final String versionShortLibelle, final String versionCode) throws DecrypterException {
        /* versionShortLibelle: What is UTH?? */
        /* versionCode: VO is not necessarily french */
        if (versionShortLibelle == null || versionCode == null) {
            throw new DecrypterException("Decrypter broken");
        }
        int lint;
        if (versionCode.equals("VO") && parameter.matches(TYPE_CONCERT)) {
            /* Special case - no different versions available --> We already got the version we want */
            lint = languageVersion;
        } else if ("VOF-STA".equalsIgnoreCase(versionCode) || "VOF-STMF".equals(versionCode) || "VA-STMA".equals(versionCode)) {
            /* Definitly NOT subtitled: VF-STMF */
            lint = 3;
        } else if (versionCode.equals("VOA-STMA")) {
            lint = 4;
        } else if (versionCode.equals("VAAUD")) {
            lint = 5;
        } else if (versionShortLibelle.equals("OmU") || versionShortLibelle.equals("VO") || versionShortLibelle.equals("VE") || versionCode.equals("VE")) {
            /* VE Actually means English but there is no specified selection for this. */
            /* Without language --> So it simply is our current language */
            lint = languageVersion;
        } else if (versionShortLibelle.equals("DE") || versionShortLibelle.equals("VA") || versionCode.equals("VO-STA") || versionShortLibelle.equals("VOSTA")) {
            /* German */
            lint = 1;
        } else if (versionShortLibelle.equals("FR") || versionShortLibelle.equals("VF") || versionShortLibelle.equals("VOF") || versionShortLibelle.equals("VOSTF") || versionCode.equals("VF-STMF")) {
            /* French - use same number than for german as the handling has changed. */
            lint = 2;
        } else {
            /* Unknown - use language inside the link */
            /* Unknown language Strings so far: VOA */
            /* This should never happen... */
            lint = languageVersion;
            throw new DecrypterException("Decrypter broken");
        }
        return lint;
    }

    /* 1 = No subtitle, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
    private String get_user_format_from_format_code(final int version) {
        switch (version) {
        case 1:
            /* German */
            return "no_subtitle";
        case 2:
            /* French */
            return "no_subtitle";
        case 3:
            return "subtitled";
        case 4:
            return "subtitled_handicapped";
        case 5:
            return "audio_description";
        default:
            /* Obviously this should never happen */
            return "WTF_PLUGIN_FAILED";
        }
    }

    /**
     * Inout: Normal formatCode Output: formatCode for internal use (1+2 = 1) 1=German, 2 = French, both no_subtitle --> We only need the
     * 'no subtitle' information which has the code 1.
     */
    private int get_intern_format_code_from_format_code(final int formatCode) {
        if (formatCode == 1 || formatCode == 2) {
            return 1;
        } else {
            return formatCode;
        }
    }

    /* 1 = No subtitle, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
    private String get_short_lang_from_format_code(final int version) {
        switch (version) {
        case 1:
            /* German */
            return "de";
        case 2:
            /* French */
            return "fr";
        case 3:
            /* We assume that a subtitled version is always german too */
            return "de";
        default:
            /* Obviously this should never happen */
            return "WTF_PLUGIN_FAILED";
        }
    }

    private void setSelectedLang_format_code(final String short_lang) {
        if ("de".equals(short_lang)) {
            this.languageVersion = 1;
        } else {
            this.languageVersion = 2;
        }
    }

    private String getAPIUrl(final String hybridAPIlink, final String lang, final String id) {
        String apilink;
        if (parameter.matches(TYPE_GUIDE)) {
            final String api_language = this.artetv_api_language(lang);
            final String id_without_lang = id.substring(0, id.length() - 1);
            final String id_with_lang = id_without_lang + api_language;
            apilink = String.format(hybridAPIlink, api_language, id_with_lang);
        } else {
            apilink = String.format(hybridAPIlink, lang, id);
        }
        return apilink;
    }

    private String artetv_api_language() {
        return artetv_api_language(getUrlLang());
    }

    private String artetv_api_language(final String lang) {
        String apilang;
        if ("de".equals(lang)) {
            apilang = "D";
        } else {
            apilang = "F";
        }
        return apilang;
    }

    private String getUrlFilename() {
        String urlfilename;
        urlfilename = new Regex(parameter, "([A-Za-z0-9\\-]+)$").getMatch(0);
        return urlfilename;
    }

    private String getLongLanguage(final String shortLanguage) {
        String long_language;
        if (shortLanguage.equals("de")) {
            long_language = "german";
        } else {
            long_language = "french";
        }
        return long_language;
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
        String urlfilename;
        if (parameter.matches(TYPE_CONCERT)) {
            urlfilename = new Regex(parameter, "concert\\.arte\\.tv/(de|fr)/(.+)").getMatch(1);
        } else {
            urlfilename = new Regex(parameter, "arte\\.tv/guide/[a-z]{2}/(.+)").getMatch(0);
        }
        return urlfilename;
    }

    private String getUrlLang() {
        final String lang = new Regex(parameter, "((?:concert|creative)\\.arte\\.tv|guide)/(\\w+)/.+").getMatch(1);
        return lang;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}