//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "creative.arte.tv_extern" }, urls = { "http://creative\\.arte\\.tv/(de|fr)/scald_dmcloud_json/\\d+" }, flags = { 0 })
public class ArteMediathekDecrypterExtern extends PluginForDecrypt {

    private static final String           EXCEPTION_LINKOFFLINE      = "EXCEPTION_LINKOFFLINE";

    private static final String           TYPE_CREATIVE              = "http://creative\\.arte\\.tv/(de|fr)/scald_dmcloud_json/\\d+";

    private static final String           V_NORMAL                   = "V_NORMAL";
    private static final String           V_SUBTITLED                = "V_SUBTITLED";
    private static final String           V_SUBTITLE_DISABLED_PEOPLE = "V_SUBTITLE_DISABLED_PEOPLE";
    private static final String           V_AUDIO_DESCRIPTION        = "V_AUDIO_DESCRIPTION";
    private static final String           http_extern_1000           = "http_extern_1000";
    private static final String           hls_extern_250             = "hls_extern_250";
    private static final String           hls_extern_500             = "hls_extern_500";
    private static final String           hls_extern_1000            = "hls_extern_1000";
    private static final String           hls_extern_2000            = "hls_extern_2000";
    private static final String           hls_extern_4000            = "hls_extern_4000";
    private static final String           LOAD_LANGUAGE_URL          = "LOAD_LANGUAGE_URL";
    private static final String           LOAD_LANGUAGE_GERMAN       = "LOAD_LANGUAGE_GERMAN";
    private static final String           LOAD_LANGUAGE_FRENCH       = "LOAD_LANGUAGE_FRENCH";
    private static final String           THUMBNAIL                  = "THUMBNAIL";
    private static final String           FAST_LINKCHECK             = "FAST_LINKCHECK";

    final String[]                        formats                    = { http_extern_1000, hls_extern_250, hls_extern_500, hls_extern_1000, hls_extern_2000, hls_extern_4000 };

    private HashMap<String, DownloadLink> bestMap                    = new HashMap<String, DownloadLink>();
    private String                        parameter;
    private String                        plain_domain_decrypter     = null;
    private String                        title                      = null;
    private String                        apiurl;
    private String                        sourceURL;
    private String                        fid;
    private String                        description;
    private int                           languageVersion            = 1;
    private boolean                       fastLinkcheck              = false;
    private FilePackage                   fp                         = FilePackage.getInstance();

    public ArteMediathekDecrypterExtern(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * TODO: Check if whether we can also have subtitled versions or even other languages than given in the link here - probably not -->
     * Change code accordingly!
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        /* Load host plugin to access some static methods later */
        JDUtilities.getPluginForHost("arte.tv");
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        int foundFormatsNum = 0;
        parameter = param.toString();
        ArrayList<String> selectedFormats = new ArrayList<String>();
        ArrayList<String> selectedLanguages = new ArrayList<String>();
        title = getUrlFilename();
        String thumbnailUrl = null;
        final String plain_domain = new Regex(parameter, "([a-z]+\\.arte\\.tv)").getMatch(0);
        plain_domain_decrypter = plain_domain.replace("arte.tv", "artejd_decrypted_jd.tv");
        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String hybridAPIUrl = null;
        fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);

        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        try {
            /* First we need to have some basic data - this part is link-specific. */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            fid = new Regex(parameter, "scald_dmcloud_json/(\\d+)").getMatch(0);
            hybridAPIUrl = "http://creative.arte.tv/%s/scald_dmcloud_json/%s";
            /*
             * Now let's check which languages the user wants. We'll do the quality selection later but we have to access webpages to get
             * the different languages so let's keep the load low by only grabbing what the user selected.
             */
            if (cfg.getBooleanProperty(LOAD_LANGUAGE_URL, false)) {
                selectedLanguages.add(this.getUrlLang());
            } else {
                if (cfg.getBooleanProperty(LOAD_LANGUAGE_GERMAN, false)) {
                    selectedLanguages.add("de");
                }
                if (cfg.getBooleanProperty(LOAD_LANGUAGE_FRENCH, false)) {
                    selectedLanguages.add("fr");
                }
            }
            /* Finally, grab all we can get (in the selected language(s)) */
            for (final String selectedLanguage : selectedLanguages) {
                setSelectedLang_format_code(selectedLanguage);
                apiurl = this.getAPIUrl(hybridAPIUrl, selectedLanguage, fid);
                br.getPage(apiurl);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    /* In most cases this simply means that one of the selected languages is not available so let's go on. */
                    logger.info("This language is not available: " + selectedLanguage);
                    continue;
                }
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                final LinkedHashMap<String, Object> videoJsonPlayer = (LinkedHashMap<String, Object>) entries.get("videoJsonPlayer");
                sourceURL = (String) videoJsonPlayer.get("VTR");
                title = encodeUnicode((String) videoJsonPlayer.get("VTI"));
                description = (String) videoJsonPlayer.get("VDE");
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
                final Collection<Object> vsr_quals = ((LinkedHashMap<String, Object>) videoJsonPlayer.get("VSR")).values();
                /* One packagename for every language */
                fp = FilePackage.getInstance();
                fp.setName(title);

                for (final Object o : vsr_quals) {
                    final LinkedHashMap<String, Object> qualitymap = (LinkedHashMap<String, Object>) o;
                    final String short_lang_current = get_short_lang_from_format_code(this.languageVersion);
                    final String quality = (String) qualitymap.get("quality");
                    final String url = (String) qualitymap.get("url");
                    if (!short_lang_current.equals(selectedLanguage)) {
                        logger.info("Skipping " + quality + " because it is not the selected language");
                        foundFormatsNum++;
                        continue;
                    }
                    if (url.contains(".m3u8") || url.contains("/hls/")) {
                        getHLSQualities(qualitymap, selectedLanguage);
                    } else if (url.startsWith("http")) {
                        getHTTPQuality(qualitymap, selectedLanguage);
                    } else {
                        /* Ignore others/rtmp versions */
                        continue;
                    }
                    foundFormatsNum++;

                }

                /* Build a list of selected formats */
                for (final String format : formats) {
                    if (cfg.getBooleanProperty(format, false)) {
                        if (cfg.getBooleanProperty(V_NORMAL, false)) {
                            selectedFormats.add(selectedLanguage + "_1_" + format);
                        }
                        /* 1 = German, 2 = French, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
                        if (cfg.getBooleanProperty(V_SUBTITLED, false)) {
                            selectedFormats.add(selectedLanguage + "_3_" + format);
                        }
                        if (cfg.getBooleanProperty(V_SUBTITLE_DISABLED_PEOPLE, false)) {
                            selectedFormats.add(selectedLanguage + "_4_" + format);
                        }
                        if (cfg.getBooleanProperty(V_AUDIO_DESCRIPTION, false)) {
                            selectedFormats.add(selectedLanguage + "_5_" + format);
                        }

                    }
                }
            }

            /* User did not activate all versions --> Show this info in filename so he can correct his mistake. */
            if (bestMap.isEmpty() && foundFormatsNum > 0) {
                title = jd.plugins.hoster.ArteTv.getPhrase("ERROR_USER_NEEDS_TO_CHANGE_FORMAT_SELECTION") + title;
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
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

            /* User did not activate all versions --> Show this info in filename so he can correct his mistake. */
            if (decryptedLinks.isEmpty() && foundFormatsNum > 0) {
                title = jd.plugins.hoster.ArteTv.getPhrase("ERROR_USER_NEEDS_TO_CHANGE_FORMAT_SELECTION") + title;
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }

            /* Check if user wants to download the thumbnail as well. */
            if (cfg.getBooleanProperty(THUMBNAIL, false) && thumbnailUrl != null) {
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

    @SuppressWarnings("deprecation")
    private void getHLSQualities(final LinkedHashMap<String, Object> qualitymap, final String selectedLanguage) throws DecrypterException, IOException {
        final String protocol = "hls_extern";
        int videoBitrate = 0;
        String filename;
        String linkid;
        String quality_intern;
        final String url = (String) qualitymap.get("url");
        br.setFollowRedirects(true);
        br.getPage(url);
        final String[] hls_medias = br.getRegex("#EXT-X-STREAM-INF:(.*?\\.m3u8.*?)\n").getColumn(-1);
        if (hls_medias == null) {
            throw new DecrypterException("Decrypter broken");
        }
        final String hls_base = new Regex(br.getURL(), "(http://[^<>\"]*?\\.ism/)abs").getMatch(0);
        for (final String hls_media : hls_medias) {
            final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
            final String hls_part = new Regex(hls_media, "(abs\\-(?:audio|video).+)").getMatch(0);
            final String resolution = new Regex(hls_media, "RESOLUTION=(\\d+x\\d+)").getMatch(0);
            if (resolution == null || hls_part == null) {
                /* Skip audio-only */
                continue;
            }
            final String hls_directlink = hls_base + hls_part.trim();
            videoBitrate = resolutionToBitrate(resolution);
            quality_intern = selectedLanguage + "_" + get_intern_format_code_from_format_code(this.languageVersion) + "_" + protocol + "_" + videoBitrate;
            linkid = fid + "_" + quality_intern;
            filename = title + "_" + getLongLanguage(selectedLanguage) + "_" + get_user_format_from_format_code(this.languageVersion) + "_" + resolution + "_" + videoBitrate + ".mp4";
            link.setFinalFileName(filename);
            try {
                /* JD2 only */
                link.setContentUrl(parameter);
            } catch (Throwable e) {
                /* Stable */
                link.setBrowserUrl(parameter);
            }
            link._setFilePackage(fp);
            link.setProperty("directURL", hls_directlink);
            link.setProperty("directName", filename);
            link.setProperty("quality_intern", quality_intern);
            link.setProperty("langShort", selectedLanguage);
            link.setProperty("mainlink", parameter);
            link.setProperty("apiurl", apiurl);
            try {
                try {
                    link.setComment(description);
                } catch (final Throwable e) {
                    /* Not available in 0.9.581 Stable */
                }
                link.setContentUrl(sourceURL);
                link.setLinkID(linkid);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                link.setBrowserUrl(sourceURL);
                link.setProperty("LINKDUPEID", linkid);
            }
            if (fastLinkcheck) {
                link.setAvailable(true);
            }
            bestMap.put(quality_intern, link);
        }
    }

    @SuppressWarnings("deprecation")
    private void getHTTPQuality(final LinkedHashMap<String, Object> qualitymap, final String selectedLanguage) throws DecrypterException {
        final String protocol;
        final String quality = (String) qualitymap.get("quality");
        final String url = (String) qualitymap.get("url");
        int videoBitrate;
        int width;
        int height;
        String filename;
        String linkid;
        String quality_intern;
        protocol = "http_extern";
        final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
        if (quality.equals("High quality")) {
            videoBitrate = 1000;
            width = 504;
            height = 284;
        } else {
            /* This should never happen */
            videoBitrate = 0;
            width = 0;
            height = 0;
        }
        quality_intern = selectedLanguage + "_" + get_intern_format_code_from_format_code(this.languageVersion) + "_" + protocol + "_" + videoBitrate;
        linkid = fid + "_" + quality_intern;
        filename = title + "_" + getLongLanguage(selectedLanguage) + "_" + get_user_format_from_format_code(this.languageVersion) + "_" + width + "x" + height + "_" + videoBitrate + ".mp4";
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
        try {
            try {
                link.setComment(description);
            } catch (final Throwable e) {
                /* Not available in 0.9.581 Stable */
            }
            link.setContentUrl(sourceURL);
            link.setLinkID(linkid);
        } catch (final Throwable e) {
            /* Not available in old 0.9.581 Stable */
            link.setBrowserUrl(sourceURL);
            link.setProperty("LINKDUPEID", linkid);
        }
        if (fastLinkcheck) {
            link.setAvailable(true);
        }
        bestMap.put(quality_intern, link);
    }

    /* Non-subtitled versions, 3 = Subtitled versions, 4 = Subtitled versions for disabled people, 5 = Audio descriptions */
    private int getFormatCode(final String versionShortLibelle, final String versionCode) throws DecrypterException {
        /* versionShortLibelle: What is UTH?? */
        /* versionCode: VO is not necessarily french */
        if (versionShortLibelle == null || versionCode == null) {
            throw new DecrypterException("Decrypter broken");
        }
        int lint;
        if ("VOF-STA".equalsIgnoreCase(versionCode) || "VOF-STMF".equals(versionCode) || "VA-STMA".equals(versionCode)) {
            /* Definitly NOT subtitled: VF-STMF */
            lint = 3;
        } else if (versionCode.equals("VOA-STMA")) {
            lint = 4;
        } else if (versionCode.equals("VAAUD")) {
            lint = 5;
        } else if (versionShortLibelle.equals("OmU") || versionShortLibelle.equals("VO")) {
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
        apilink = String.format(hybridAPIlink, lang, id);
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
        urlfilename = new Regex(parameter, "arte\\.tv/guide/[a-z]{2}/(.+)").getMatch(0);
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

    /** Finds the video bitrate to a resolution based on the width of the video. */
    private int resolutionToBitrate(final String resolution) {
        int final_bitrate;
        final int resolution_width = Integer.parseInt(new Regex(resolution, "(\\d+)x\\d+").getMatch(0));
        if (resolution_width < 250) {
            final_bitrate = 250;
        } else if (resolution_width >= 250 && resolution_width < 450) {
            final_bitrate = 500;
        } else if (resolution_width >= 450 && resolution_width < 650) {
            final_bitrate = 1000;
        } else if (resolution_width >= 650 && resolution_width < 1000) {
            final_bitrate = 2000;
        } else {
            final_bitrate = 4000;
        }
        return final_bitrate;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}