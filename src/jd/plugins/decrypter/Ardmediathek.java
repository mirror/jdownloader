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

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.ArdConfigInterface;
import org.jdownloader.plugins.components.config.CheckeinsDeConfig;
import org.jdownloader.plugins.components.config.DasersteConfig;
import org.jdownloader.plugins.components.config.EurovisionConfig;
import org.jdownloader.plugins.components.config.KikaDeConfig;
import org.jdownloader.plugins.components.config.MdrDeConfig;
import org.jdownloader.plugins.components.config.MediathekDasersteConfig;
import org.jdownloader.plugins.components.config.MediathekRbbOnlineConfig;
import org.jdownloader.plugins.components.config.NdrDeConfig;
import org.jdownloader.plugins.components.config.OneARDConfig;
import org.jdownloader.plugins.components.config.RbbOnlineConfig;
import org.jdownloader.plugins.components.config.SandmannDeConfig;
import org.jdownloader.plugins.components.config.SportschauConfig;
import org.jdownloader.plugins.components.config.SputnikDeConfig;
import org.jdownloader.plugins.components.config.SrOnlineConfig;
import org.jdownloader.plugins.components.config.WDRConfig;
import org.jdownloader.plugins.components.config.WDRMausConfig;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ardmediathek.de", "mediathek.daserste.de", "daserste.de", "rbb-online.de", "sandmann.de", "wdr.de", "sportschau.de", "one.ard.de", "wdrmaus.de", "sr-online.de", "ndr.de", "kika.de", "eurovision.de", "sputnik.de", "mdr.de", "checkeins.de" }, urls = { "https?://(?:www\\.)?ardmediathek\\.de/.*?documentId=\\d+[^/]*?", "https?://(?:www\\.)?mediathek\\.daserste\\.de/.*?documentId=\\d+[^/]*?", "https?://www\\.daserste\\.de/[^<>\"]+/(?:videos|videosextern)/[a-z0-9\\-]+\\.html", "https?://(?:www\\.)?mediathek\\.rbb\\-online\\.de/tv/[^<>\"]+documentId=\\d+[^/]*?", "https?://(?:www\\.)?sandmann\\.de/.+", "https?://(?:[a-z0-9]+\\.)?wdr\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?sportschau\\.de/.*?\\.html", "https?://(?:www\\.)?one\\.ard\\.de/tv/[^<>\"]+documentId=\\d+[^/]*?", "https?://(?:www\\.)?wdrmaus\\.de/.+",
        "https?://sr\\-mediathek\\.sr\\-online\\.de/index\\.php\\?seite=\\d+\\&id=\\d+", "https?://(?:[a-z0-9]+\\.)?ndr\\.de/.*?\\.html", "https?://(?:www\\.)?kika\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?eurovision\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?sputnik\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?mdr\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?checkeins\\.de/[^<>\"]+\\.html" })
public class Ardmediathek extends PluginForDecrypt {
    private static final String                 EXCEPTION_LINKOFFLINE = "EXCEPTION_LINKOFFLINE";
    /* Constants */
    private static final String                 type_unsupported      = ".+ardmediathek\\.de/(tv/live\\?kanal=\\d+|dossiers/.*)";
    private static final String                 type_invalid          = ".+(ardmediathek|mediathek\\.daserste)\\.de/(download|livestream).+";
    /* Variables */
    private final HashMap<String, DownloadLink> foundQualitiesMap     = new HashMap<String, DownloadLink>();
    ArrayList<DownloadLink>                     decryptedLinks        = new ArrayList<DownloadLink>();
    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String>                  all_known_qualities   = Arrays.asList("http_3773000_720", "hls_3773000_720", "http_1989000_540", "hls_1989000_540", "http_1213000_360", "hls_1213000_360", "http_605000_280", "hls_605000_280", "http_448000_270", "hls_448000_270", "http_317000_270", "hls_317000_270", "http_189000_180", "hls_189000_180", "http_0_0");
    private final Map<String, Long>             heigth_to_bitrate     = new HashMap<String, Long>();
    {
        heigth_to_bitrate.put("180", 189000l);
        /* keep in mind that sometimes there are two versions for 270! This is the higher one (default)! */
        heigth_to_bitrate.put("270", 448000l);
        heigth_to_bitrate.put("280", 605000l);
        heigth_to_bitrate.put("360", 1213000l);
        heigth_to_bitrate.put("540", 1989000l);
        heigth_to_bitrate.put("576", 1728000l);
        heigth_to_bitrate.put("720", 3773000l);
    }
    private String  subtitleLink   = null;
    private String  parameter      = null;
    private String  title          = null;
    private String  date_formatted = null;
    private boolean grabHLS        = false;
    private String  contentID      = null;

    public Ardmediathek(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends ArdConfigInterface> getConfigInterface() {
        if ("ardmediathek.de".equalsIgnoreCase(getHost())) {
            return ArdConfigInterface.class;
        } else if ("mediathek.rbb-online.de".equalsIgnoreCase(getHost())) {
            return MediathekRbbOnlineConfig.class;
        } else if ("daserste.de".equalsIgnoreCase(getHost())) {
            return DasersteConfig.class;
        } else if ("mediathek.daserste.de".equalsIgnoreCase(getHost())) {
            return MediathekDasersteConfig.class;
        } else if ("one.ard.de".equalsIgnoreCase(getHost())) {
            return OneARDConfig.class;
        } else if ("wdrmaus.de".equalsIgnoreCase(getHost())) {
            return WDRMausConfig.class;
        } else if ("wdr.de".equalsIgnoreCase(getHost())) {
            return WDRConfig.class;
        } else if ("sportschau.de".equalsIgnoreCase(getHost())) {
            return SportschauConfig.class;
        } else if ("sr-online.de".equalsIgnoreCase(getHost())) {
            return SrOnlineConfig.class;
        } else if ("ndr.de".equalsIgnoreCase(getHost())) {
            return NdrDeConfig.class;
        } else if ("kika.de".equalsIgnoreCase(getHost())) {
            return KikaDeConfig.class;
        } else if ("eurovision.de".equalsIgnoreCase(getHost())) {
            return EurovisionConfig.class;
        } else if ("sputnik.de".equalsIgnoreCase(getHost())) {
            return SputnikDeConfig.class;
        } else if ("checkeins.de".equalsIgnoreCase(getHost())) {
            return CheckeinsDeConfig.class;
        } else if ("sandmann.de".equalsIgnoreCase(getHost())) {
            return SandmannDeConfig.class;
        } else if ("mdr.de".equalsIgnoreCase(getHost())) {
            return MdrDeConfig.class;
        } else if ("rbb-online.de".equalsIgnoreCase(getHost())) {
            return RbbOnlineConfig.class;
        } else {
            return ArdConfigInterface.class;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        parameter = Encoding.htmlDecode(param.toString());
        if (parameter.matches(type_unsupported) || parameter.matches(type_invalid)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final ArdConfigInterface cfg = PluginJsonConfig.get(getConfigInterface());
        final List<String> selectedQualities = new ArrayList<String>();
        /*
         * 2018-03-06: TODO: Maybe add option to download hls audio as hls master playlist will often contain a mp4 stream without video (==
         * audio only).
         */
        final boolean addAudio = cfg.isGrabAudio();
        final boolean addHLS180 = cfg.isGrabHLS180pVideoEnabled();
        final boolean addHLS270 = cfg.isGrabHLS270pVideoEnabled();
        final boolean addHLS270lower = cfg.isGrabHLS270pLowerVideoEnabled();
        final boolean addHLS280 = cfg.isGrabHLS280pVideoEnabled();
        final boolean addHLS360 = cfg.isGrabHLS360pVideoEnabled();
        final boolean addHLS540 = cfg.isGrabHLS540pVideoEnabled();
        final boolean addHLS576 = cfg.isGrabHLS576pVideoEnabled();
        final boolean addHLS720 = cfg.isGrabHLS720pVideoEnabled();
        grabHLS = addHLS180 || addHLS270lower || addHLS270 || addHLS280 || addHLS360 || addHLS540 || addHLS576 || addHLS720;
        if (addHLS180) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("180") + "_180");
        }
        if (addHLS270lower) {
            selectedQualities.add("hls_317000_270");
        }
        if (addHLS270) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("270") + "_270");
        }
        if (addHLS280) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("280") + "_280");
        }
        if (addHLS360) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("360") + "_360");
        }
        if (addHLS540) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("540") + "_540");
        }
        if (addHLS540) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("576") + "_576");
        }
        if (addHLS720) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("720") + "_720");
        }
        if (cfg.isGrabHTTP180pVideoEnabled()) {
            selectedQualities.add("http_" + heigth_to_bitrate.get("180") + "_180");
        }
        if (cfg.isGrabHTTP270pLowerVideoEnabled()) {
            selectedQualities.add("http_317000_270");
        }
        if (cfg.isGrabHTTP270pVideoEnabled()) {
            selectedQualities.add("http_" + heigth_to_bitrate.get("270") + "_270");
        }
        if (cfg.isGrabHTTP280pVideoEnabled()) {
            selectedQualities.add("http_" + heigth_to_bitrate.get("280") + "_280");
        }
        if (cfg.isGrabHTTP360pVideoEnabled()) {
            selectedQualities.add("http_" + heigth_to_bitrate.get("360") + "_360");
        }
        if (cfg.isGrabHTTP540pVideoEnabled()) {
            selectedQualities.add("http_" + heigth_to_bitrate.get("540") + "_540");
        }
        if (cfg.isGrabHTTP576pVideoEnabled()) {
            selectedQualities.add("http_" + heigth_to_bitrate.get("576") + "_576");
        }
        if (cfg.isGrabHTTP720pVideoEnabled()) {
            selectedQualities.add("http_" + heigth_to_bitrate.get("720") + "_720");
        }
        if (addAudio) {
            selectedQualities.add("http_0_0");
        }
        try {
            /*
             * 2018-02-22: Important: So far there is only one OLD website, not compatible with the "decryptMediathek" function! Keep this
             * in mind when changing things!
             */
            final String host = getHost();
            if (host.equalsIgnoreCase("daserste.de") || host.equalsIgnoreCase("kika.de") || host.equalsIgnoreCase("sputnik.de") || host.equalsIgnoreCase("mdr.de") || host.equalsIgnoreCase("checkeins.de")) {
                decryptDasersteVideo();
            } else {
                decryptMediathek();
            }
            handleUserQualitySelection(selectedQualities);
        } catch (final DecrypterException e) {
            try {
                if (e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
            } catch (final Exception x) {
            }
            throw e;
        }
        if (decryptedLinks == null) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Failed to find any links");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    /* Returns title for all XML based websites (XML has to be accessed before!) */
    private String getDasersteTitle(final Browser br) {
        final String host = getHost();
        final String provider = host.substring(0, host.lastIndexOf(".")).replace(".", "_");
        String date = getXML(br.toString(), "broadcastDate");
        if (StringUtils.isEmpty(date)) {
            /* E.g. kika.de */
            date = getXML(br.toString(), "datetimeOfBroadcasting");
        }
        if (StringUtils.isEmpty(date)) {
            /* E.g. mdr.de */
            date = getXML(br.toString(), "broadcastStartDate");
        }
        /* E.g. kika.de */
        final String show = getXML(br.toString(), "channelName");
        String video_title = getXML(br.toString(), "shareTitle");
        if (StringUtils.isEmpty(video_title)) {
            video_title = getXML(br.toString(), "broadcastName");
        }
        if (StringUtils.isEmpty(video_title)) {
            /* E.g. sputnik.de */
            video_title = getXML(br.toString(), "headline");
        }
        if (StringUtils.isEmpty(video_title)) {
            video_title = "UnknownTitle_" + System.currentTimeMillis();
        }
        if (!StringUtils.isEmpty(date)) {
            this.date_formatted = formatDateDasErste(date);
        }
        String title_final = "";
        if (this.date_formatted != null) {
            title_final = this.date_formatted + "_";
        }
        title_final += provider + "_";
        if (!StringUtils.isEmpty(show)) {
            title_final += show + " - ";
        }
        title_final += video_title;
        title_final = Encoding.htmlDecode(title_final).trim();
        title_final = encodeUnicode(title_final);
        return title_final;
    }

    /* Returns title, with fallback if nothing found in html */
    private String getMediathekTitle(final Browser brHTML, final Browser brJSON) {
        final String jsonSchemaOrg = brHTML.getRegex("<script type=\"application/ld\\+json\">(.*?)</script>").getMatch(0);
        String title = null;
        String provider = null;
        String show = null;
        /* These RegExes should be compatible with all websites */
        /* Date is already provided in the format we need. */
        String date = null;
        this.date_formatted = brHTML.getRegex("<meta property=\"video:release_date\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2})[^\"]*?\"[^>]*?/?>").getMatch(0);
        if (this.date_formatted == null) {
            this.date_formatted = brHTML.getRegex("<span itemprop=\"datePublished\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2})[^\"]*?\"[^>]*?/?>").getMatch(0);
        }
        String description = brHTML.getRegex("<meta property=\"og:description\" content=\"([^\"]+)\"").getMatch(0);
        final String host = getHost();
        if (jsonSchemaOrg != null) {
            /* 2018-02-15: E.g. daserste.de, wdr.de */
            final String headline = brHTML.getRegex("<h3 class=\"headline\">([^<>]+)</h3>").getMatch(0);
            try {
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(jsonSchemaOrg);
                final String uploadDate = (String) entries.get("uploadDate");
                title = (String) entries.get("name");
                if ("Video".equalsIgnoreCase(title) && !StringUtils.isEmpty(headline)) {
                    /**
                     * 2018-02-22: Some of these schema-objects contain wrong information e.g.
                     * https://www1.wdr.de/mediathek/video/klangkoerper/klangkoerper/video-wdr-dackl-jazzkonzert-100.html --> This is a
                     * simple fallback.
                     */
                    title = headline;
                }
                if (description == null) {
                    description = (String) entries.get("description");
                }
                if (StringUtils.isEmpty(this.date_formatted) && !StringUtils.isEmpty(uploadDate)) {
                    /* Fallback */
                    this.date_formatted = new Regex(uploadDate, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
                }
                /* Find more data */
                entries = (LinkedHashMap<String, Object>) entries.get("productionCompany");
                if (entries != null) {
                    provider = (String) entries.get("name");
                }
            } catch (final Throwable e) {
            }
        } else if (host.equalsIgnoreCase("wdrmaus.de")) {
            final String content_ids_str = brHTML.getRegex("var _contentId = \\[([^<>\\[\\]]+)\\];").getMatch(0);
            if (content_ids_str != null) {
                final String[] content_ids = content_ids_str.split(",");
                if (content_ids != null && content_ids.length >= 3) {
                    show = content_ids[0];
                    title = content_ids[2];
                }
            }
            if (StringUtils.isEmpty(title)) {
                title = brHTML.getRegex("<title>([^<>]+) \\- Die Sendung mit der Maus \\- WDR</title>").getMatch(0);
            }
            if (StringUtils.isEmpty(show) && (brHTML.getURL().contains("/lachgeschichten") || brHTML.getURL().contains("/sachgeschichten"))) {
                // show = "Die Sendung mit der Maus";
                show = "Lach- und Sachgeschichten";
            }
            /*
             * 2018-02-22: TODO: This may sometimes be inaccurate when there are multiple videoObjects on one page (rare case) e.g.
             * http://www.wdrmaus.de/extras/mausthemen/eisenbahn/index.php5 --> This is so far not a real usage case and we do not have any
             * complaints about the current plugin behavior!
             */
            date = brHTML.getRegex("Sendetermin: (\\d{2}\\.\\d{2}\\.\\d{4})").getMatch(0);
            if (StringUtils.isEmpty(date)) {
                /* Last chance */
                date = PluginJSonUtils.getJson(brJSON, "trackerClipAirTime");
            }
        } else if (host.contains("sr-online.de")) {
            /* sr-mediathek.sr-online.de */
            title = brHTML.getRegex("<div class=\"ardplayer\\-title\">([^<>\"]+)</div>").getMatch(0);
            date = brHTML.getRegex("<p>Video \\| (\\d{2}\\.\\d{2}\\.\\d{4}) \\| Dauer:").getMatch(0);
        } else if (host.equalsIgnoreCase("ndr.de") || host.equalsIgnoreCase("eurovision.de")) {
            /* ndr.de */
            if (brHTML.getURL().contains("daserste.ndr.de")) {
                date = brHTML.getRegex("<p>Dieses Thema im Programm:</p>\\s*?<h2>[^<>]*?(\\d{2}\\.\\d{2}\\.\\d{4})[^<>]*?</h2>").getMatch(0);
            }
            title = brHTML.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\"/>").getMatch(0);
            if (StringUtils.isEmpty(date)) {
                /* Last chance */
                date = PluginJSonUtils.getJson(brJSON, "assetid");
                if (!StringUtils.isEmpty(date)) {
                    date = new Regex(date, "TV\\-(\\d{8})").getMatch(0);
                }
            }
        } else {
            /* E.g. ardmediathek.de */
            show = brHTML.getRegex("name=\"dcterms\\.isPartOf\" content=\"([^<>\"]*?)\"").getMatch(0);
            title = brHTML.getRegex("<meta name=\"dcterms\\.title\" content=\"([^\"]+)\"").getMatch(0);
        }
        if (StringUtils.isEmpty(this.date_formatted) && !StringUtils.isEmpty(date)) {
            /* Rare case: Date available but we need to change it to our target-format. */
            this.date_formatted = formatDate(date);
        }
        if (StringUtils.isEmpty(title)) {
            /* This should never happen */
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        title = title.trim();
        if (StringUtils.isEmpty(provider)) {
            /* Fallback */
            provider = host.substring(0, host.lastIndexOf(".")).replace(".", "_");
        }
        if (!StringUtils.isEmpty(show)) {
            title = show.trim() + " - " + title;
        }
        title = Encoding.htmlDecode(title);
        title = provider + "_" + title;
        if (this.date_formatted != null) {
            title = this.date_formatted + "_" + title;
        }
        title = encodeUnicode(title);
        return title;
    }

    /** Find xml URL which leads to subtitle and video stream URLs. */
    private String getVideoXMLURL() throws Exception {
        final String host = getHost();
        String url_xml = null;
        if (host.equalsIgnoreCase("daserste.de") || host.equalsIgnoreCase("checkeins.de")) {
            /* The fast way - we do not even have to access the main URL which the user has added :) */
            url_xml = parameter.replace(".html", "~playerXml.xml");
        } else if (this.parameter.matches(".+mdr\\.de/.+/((?:video|audio)\\-\\d+)\\.html")) {
            /* Some special mdr.de URLs --> We do not have to access main URL so this way we can speed up the crawl process a bit :) */
            this.contentID = new Regex(this.parameter, "((?:audio|video)\\-\\d+)\\.html$").getMatch(0);
            url_xml = String.format("https://www.mdr.de/mediathek/mdr-videos/d/%s-avCustom.xml", this.contentID);
        } else {
            /* E.g. kika.de, sputnik.de, mdr.de */
            br.getPage(this.parameter);
            if (isOffline(this.br)) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            url_xml = br.getRegex("\\'((?:https?://|(?:\\\\)?/)[^<>\"]+\\-avCustom\\.xml)\\'").getMatch(0);
            if (!StringUtils.isEmpty(url_xml)) {
                if (url_xml.contains("\\")) {
                    url_xml = url_xml.replace("\\", "");
                }
                this.contentID = new Regex(url_xml, "((?:audio|video)\\-\\d+)").getMatch(0);
            }
        }
        return url_xml;
    }

    /** Finds json URL which leads to subtitle and video stream URLs AND sets unique contentID. */
    private String getVideoJsonURL() throws MalformedURLException {
        String url_json = null;
        final String ardBroadcastID = new Regex(br.getURL(), "(?:\\?|\\&)documentId=(\\d+)").getMatch(0);
        final String host = getHost();
        if (host.contains("sr-online.de")) {
            this.contentID = new Regex(br.getURL(), "id=(\\d+)").getMatch(0);
            url_json = String.format("http://www.sr-mediathek.de/sr_player/mc.php?id=%s&tbl=&pnr=0&hd=1&devicetype=", this.contentID);
        } else if (host.equalsIgnoreCase("sandmann.de")) {
            url_json = br.getRegex("data\\-media\\-ref=\"([^\"]*?\\.jsn)[^\"]*?\"").getMatch(0);
            if (!StringUtils.isEmpty(url_json)) {
                if (url_json.startsWith("/")) {
                    url_json = "https://www.sandmann.de" + url_json;
                }
                /* This is a very ugly contentID */
                this.contentID = new Regex(url_json, "sandmann\\.de/(.+)").getMatch(0);
            }
        } else if (ardBroadcastID != null) {
            this.contentID = ardBroadcastID;
            url_json = String.format("http://www.ardmediathek.de/play/media/%s?devicetype=pc&features=flash", this.contentID);
        } else if (host.contains("ndr.de") || host.equalsIgnoreCase("eurovision.de")) {
            /* E.g. daserste.ndr.de, blabla.ndr.de */
            this.contentID = br.getRegex("([A-Za-z0-9]+\\d+)\\-(?:ard)?player_[^\"]+\"").getMatch(0);
            if (!StringUtils.isEmpty(this.contentID)) {
                url_json = String.format("https://www.ndr.de/%s-ardjson.json", this.contentID);
            }
        } else {
            /* wdr.de, one.ard.de */
            url_json = this.br.getRegex("(?:\\'|\")mediaObj(?:\\'|\"):\\s*?\\{\\s*?(?:\\'|\")url(?:\\'|\"):\\s*?(?:\\'|\")(https?://[^<>\"]+\\.js)(?:\\'|\")").getMatch(0);
            if (url_json != null) {
                /* 2018-03-07: Same IDs that will also appear in every streamingURL! */
                this.contentID = new Regex(url_json, "(\\d+/\\d+)\\.js$").getMatch(0);
            }
        }
        return url_json;
    }

    /**
     * Find subtitle URL inside xml String
     */
    private String getXMLSubtitleURL(final Browser xmlBR) throws MalformedURLException {
        String subtitleURL = getXML(xmlBR.toString(), "videoSubtitleUrl");
        if (StringUtils.isEmpty(subtitleURL)) {
            /* E.g. checkeins.de */
            subtitleURL = xmlBR.getRegex("<dataTimedTextNoOffset url=\"(http[^<>\"]+\\.xml)\">").getMatch(0);
        }
        return subtitleURL;
    }

    /**
     * Find subtitle URL inside json String
     *
     * @throws MalformedURLException
     */
    private String getJsonSubtitleURL(final Browser jsonBR) throws MalformedURLException {
        String subtitleURL;
        if (br.getURL().contains("wdr.de/")) {
            subtitleURL = PluginJSonUtils.getJsonValue(jsonBR, "captionURL");
        } else {
            subtitleURL = PluginJSonUtils.getJson(jsonBR, "_subtitleUrl");
            if (subtitleURL != null && !subtitleURL.startsWith("http://")) {
                subtitleURL = jsonBR._getURL().getProtocol() + "://www." + jsonBR.getHost() + subtitleURL;
            }
        }
        return subtitleURL;
    }

    private String getHlsToHttpURLFormat(final String hls_master) {
        final Regex regex_hls = new Regex(hls_master, ".+/([^/]+/[^/]+/[^,/]+)(?:/|_|\\.),([A-Za-z0-9_,\\-]+),\\.mp4\\.csmil/?");
        String urlpart = regex_hls.getMatch(0);
        String urlpart2 = new Regex(hls_master, "//[^/]+/[^/]+/(.*?)(?:/|_),").getMatch(0);
        String http_url_format = null;
        /**
         * hls --> http urls (whenever possible) <br />
         */
        /* First case */
        if (hls_master.contains("sr_hls_od-vh") && urlpart != null) {
            http_url_format = "http://mediastorage01.sr-online.de/Video/" + urlpart + "_%s.mp4";
        }
        /* 2nd case */
        if (hls_master.contains("dasersteuni-vh.akamaihd.net")) {
            if (urlpart2 != null) {
                http_url_format = "https://pdvideosdaserste-a.akamaihd.net/" + urlpart2 + "/%s.mp4";
            }
        } else if (hls_master.contains("br-i.akamaihd.net")) {
            if (urlpart2 != null) {
                http_url_format = "http://cdn-storage.br.de/" + urlpart2 + "_%s.mp4";
            }
        } else if (hls_master.contains("wdradaptiv-vh.akamaihd.net") && urlpart2 != null) {
            /* wdr */
            http_url_format = "http://wdrmedien-a.akamaihd.net/" + urlpart2 + "/%s.mp4";
        } else if (hls_master.contains("rbbmediaadp-vh") && urlpart2 != null) {
            /* For all RBB websites e.g. also sandmann.de */
            http_url_format = "https://rbbmediapmdp-a.akamaihd.net/" + urlpart2 + "_%s.mp4";
        }
        /* 3rd case */
        if (hls_master.contains("ndrod-vh.akamaihd.net") && urlpart != null) {
            /* 2018-03-07: There is '/progressive/' and '/progressive_geo/' --> We have to grab this from existing http urls */
            final String server_http = br.getRegex("(https?://mediandr\\-a\\.akamaihd\\.net/progressive[^/]*?/)[^\"]+\\.mp4").getMatch(0);
            if (server_http != null) {
                http_url_format = server_http + urlpart + ".%s.mp4";
            }
        }
        return http_url_format;
    }

    /** Last revision with old handling: 38658 */
    private void decryptMediathek() throws Exception {
        if (isOffline(this.br)) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        final Browser brJSON = new Browser();
        setBrowserExclusive();
        brJSON.setFollowRedirects(true);
        final String url_json = getVideoJsonURL();
        if (StringUtils.isEmpty(url_json)) {
            /* No downloadable content --> URL should be offline (or only text content) */
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        brJSON.getPage(url_json);
        /* No json --> No media to crawl (rare case!)! */
        if (!brJSON.getHttpConnection().getContentType().contains("application/json") && !brJSON.getHttpConnection().getContentType().contains("application/javascript") && !brJSON.containsHTML("\\{") || brJSON.getHttpConnection().getResponseCode() == 404) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        title = getMediathekTitle(this.br, brJSON);
        subtitleLink = getJsonSubtitleURL(brJSON);
        /* We know how their http urls look - this way we can avoid HDS/HLS/RTMP */
        /*
         * http://adaptiv.wdr.de/z/medp/ww/fsk0/104/1046579/,1046579_11834667,1046579_11834665,1046579_11834669,.mp4.csmil/manifest.f4
         */
        // //wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/139/1394333/,1394333_16295554,1394333_16295556,1394333_16295555,1394333_16295557,1394333_16295553,1394333_16295558,.mp4.csmil/master.m3u8
        final String http_url_audio = brJSON.getRegex("(https?://[^<>\"]+\\.mp3)\"").getMatch(0);
        final String hls_master = brJSON.getRegex("(//[^<>\"]+\\.m3u8[^<>\"]*?)").getMatch(0);
        final Regex regex_hls = new Regex(hls_master, ".+/([^/]+/[^/]+/[^,/]+)(?:/|_|\\.),([A-Za-z0-9_,\\-]+),\\.mp4\\.csmil/?");
        final String quality_string = regex_hls.getMatch(1);
        if (StringUtils.isEmpty(hls_master) && http_url_audio == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Plugin broken");
        }
        /*
         * Grab all http qualities inside json
         */
        final List<String> httpStreamsQualityIdentifiers = new ArrayList<String>();
        try {
            final HashMap<String, Object> map = JSonStorage.restoreFromString(brJSON.toString(), TypeRef.HASHMAP);
            if (map != null && map.containsKey("_mediaArray")) {
                final List<Map<String, Object>> mediaArray = (List<Map<String, Object>>) map.get("_mediaArray");
                for (Map<String, Object> media : mediaArray) {
                    List<Map<String, Object>> mediaStreamArray = (List<Map<String, Object>>) media.get("_mediaStreamArray");
                    for (Map<String, Object> mediaStream : mediaStreamArray) {
                        final int quality;
                        if (mediaStream.get("_quality") instanceof Number) {
                            quality = ((Number) mediaStream.get("_quality")).intValue();
                        } else {
                            continue;
                        }
                        final List<String> streams;
                        if (mediaStream.get("_stream") instanceof String) {
                            streams = new ArrayList<String>();
                            streams.add((String) mediaStream.get("_stream"));
                        } else {
                            streams = ((List<String>) mediaStream.get("_stream"));
                        }
                        for (int index = 0; index < streams.size(); index++) {
                            final String stream = streams.get(index);
                            if (stream == null || !StringUtils.endsWithCaseInsensitive(stream, ".mp4")) {
                                /* Skip invalid objects */
                                continue;
                            }
                            final String url = brJSON.getURL(stream).toString();
                            final int widthInt;
                            final int heightInt;
                            if (quality == 0 && streams.size() == 1) {
                                widthInt = 320;
                                heightInt = 180;
                            } else if (quality == 1 && streams.size() == 1) {
                                widthInt = 512;
                                heightInt = 288;
                            } else if (quality == 1 && streams.size() == 2) {
                                switch (index) {
                                case 0:
                                    widthInt = 512;
                                    heightInt = 288;
                                    break;
                                case 1:
                                default:
                                    widthInt = 480;
                                    heightInt = 270;
                                    break;
                                }
                            } else if (quality == 2 && streams.size() == 1) {
                                widthInt = 960;
                                heightInt = 544;
                            } else if (quality == 2 && streams.size() == 2) {
                                switch (index) {
                                case 0:
                                    widthInt = 640;
                                    heightInt = 360;
                                    break;
                                case 1:
                                default:
                                    widthInt = 960;
                                    heightInt = 540;
                                    break;
                                }
                            } else if (quality == 3 && streams.size() == 1) {
                                widthInt = 960;
                                heightInt = 540;
                            } else if (quality == 3 && streams.size() == 2) {
                                switch (index) {
                                case 0:
                                    widthInt = 1280;
                                    heightInt = 720;
                                    break;
                                case 1:
                                default:
                                    widthInt = 960;
                                    heightInt = 540;
                                    break;
                                }
                            } else if (StringUtils.containsIgnoreCase(stream, "0.mp4") || StringUtils.containsIgnoreCase(stream, "128k.mp4")) {
                                widthInt = 320;
                                heightInt = 180;
                            } else if (StringUtils.containsIgnoreCase(stream, "lo.mp4")) {
                                widthInt = 256;
                                heightInt = 144;
                            } else if (StringUtils.containsIgnoreCase(stream, "A.mp4") || StringUtils.containsIgnoreCase(stream, "mn.mp4") || StringUtils.containsIgnoreCase(stream, "256k.mp4")) {
                                widthInt = 480;
                                heightInt = 270;
                            } else if (StringUtils.containsIgnoreCase(stream, "B.mp4") || StringUtils.containsIgnoreCase(stream, "hi.mp4") || StringUtils.containsIgnoreCase(stream, "512k.mp4")) {
                                widthInt = 512;
                                heightInt = 288;
                            } else if (StringUtils.containsIgnoreCase(stream, "C.mp4") || StringUtils.containsIgnoreCase(stream, "hq.mp4") || StringUtils.containsIgnoreCase(stream, "1800k.mp4")) {
                                widthInt = 960;
                                heightInt = 540;
                            } else if (StringUtils.containsIgnoreCase(stream, "E.mp4") || StringUtils.containsIgnoreCase(stream, "ln.mp4") || StringUtils.containsIgnoreCase(stream, "1024k.mp4") || StringUtils.containsIgnoreCase(stream, "1.mp4")) {
                                widthInt = 640;
                                heightInt = 360;
                            } else if (StringUtils.containsIgnoreCase(stream, "X.mp4") || StringUtils.containsIgnoreCase(stream, "hd.mp4")) {
                                widthInt = 1280;
                                heightInt = 720;
                            } else {
                                /*
                                 * Fallback to 'old' handling which could result in wrong resolutions (but that's better than missing
                                 * downloadlinks!)
                                 */
                                final Object width = mediaStream.get("_width");
                                final Object height = mediaStream.get("_height");
                                if (width instanceof Number) {
                                    widthInt = ((Number) width).intValue();
                                } else {
                                    switch (((Number) quality).intValue()) {
                                    case 0:
                                        widthInt = 320;
                                        break;
                                    case 1:
                                        widthInt = 512;
                                        break;
                                    case 2:
                                        widthInt = 640;
                                        break;
                                    case 3:
                                        widthInt = 1280;
                                        break;
                                    default:
                                        widthInt = -1;
                                        break;
                                    }
                                }
                                if (width instanceof Number) {
                                    heightInt = ((Number) height).intValue();
                                } else {
                                    switch (((Number) quality).intValue()) {
                                    case 0:
                                        heightInt = 180;
                                        break;
                                    case 1:
                                        heightInt = 288;
                                        break;
                                    case 2:
                                        heightInt = 360;
                                        break;
                                    case 3:
                                        heightInt = 720;
                                        break;
                                    default:
                                        heightInt = -1;
                                        break;
                                    }
                                }
                            }
                            final DownloadLink download = addQuality(url, null, 0, widthInt, heightInt);
                            if (download != null) {
                                httpStreamsQualityIdentifiers.add(getQualityIdentifier(url, 0, widthInt, heightInt));
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logger.log(e);
        }
        /*
         * TODO: It might only make sense to attempt this if we found more than 3 http qualities previously because usually 3 means we will
         * also only have 3 hls qualities --> There are no additional http qualities!
         */
        final boolean tryToFindAdditionalHTTPURLs = true;
        if (tryToFindAdditionalHTTPURLs) {
            final String http_url_format = getHlsToHttpURLFormat(hls_master);
            final String[] qualities_hls = quality_string != null ? quality_string.split(",") : null;
            if (http_url_format != null && qualities_hls != null && qualities_hls.length > 0) {
                /* Access HLS master to find correct resolution for each ID (the only possible way) */
                brJSON.getPage("http:" + hls_master);
                final String[] resolutionsInOrder = brJSON.getRegex("RESOLUTION=(\\d+x\\d+)").getColumn(0);
                if (resolutionsInOrder != null) {
                    logger.info("Crawling additional http urls");
                    for (int counter = 0; counter <= qualities_hls.length - 1; counter++) {
                        if (counter > qualities_hls.length - 1 || counter > resolutionsInOrder.length - 1) {
                            break;
                        }
                        final String quality_id = qualities_hls[counter];
                        final String final_url = String.format(http_url_format, quality_id);
                        // final String linkid = qualities[counter];
                        final String resolution = resolutionsInOrder[counter];
                        final String[] height_width = resolution.split("x");
                        final String width = height_width[0];
                        final String height = height_width[1];
                        final int widthInt = Integer.parseInt(width);
                        final int heightInt = Integer.parseInt(height);
                        final String qualityIdentifier = getQualityIdentifier(final_url, 0, widthInt, heightInt);
                        if (!httpStreamsQualityIdentifiers.contains(qualityIdentifier)) {
                            logger.info("Adding missing http quality: " + qualityIdentifier);
                            addQuality(final_url, null, 0, widthInt, heightInt);
                        }
                    }
                }
            }
        }
        addHLS(brJSON, hls_master);
        if (http_url_audio != null) {
            addQuality(http_url_audio, null, 0, 0, 0);
        }
    }

    /* INFORMATION: network = akamai or limelight == RTMP */
    private void decryptDasersteVideo() throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final String xml_URL = getVideoXMLURL();
        if (xml_URL == null) {
            /* Probably no downloadable content available */
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        br.getPage(xml_URL);
        /* Usually daserste.de and checkeins.de as there is no way to find a contentID inside URL added by the user. */
        final String id = br.getRegex("<c7>(.*?)</c7>").getMatch(0);
        if (id != null && this.contentID == null) {
            contentID = Hash.getSHA1(id);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getHttpConnection().getContentType().contains("xml")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        this.subtitleLink = getXMLSubtitleURL(this.br);
        /* E.g. checkeins.de */
        final String fskRating = this.br.getRegex("<fskRating>fsk(\\d+)</fskRating>").getMatch(0);
        if (fskRating != null && Short.parseShort(fskRating) >= 12) {
            /* Video is age restricted --> Only available from >=8PM. */
            decryptedLinks.add(this.createOfflinelink(parameter, "FSK_BLOCKED"));
            return;
        }
        this.title = getDasersteTitle(this.br);
        final ArrayList<String> hls_master_dupelist = new ArrayList<String>();
        final String[] mediaStreamArray = br.getRegex("(<asset.*?</asset>)").getColumn(0);
        for (final String stream : mediaStreamArray) {
            /* E.g. kika.de */
            final String hls_master;
            String http_url = getXML(stream, "progressiveDownloadUrl");
            if (StringUtils.isEmpty(http_url)) {
                /* E.g. daserste.de */
                http_url = getXML(stream, "fileName");
            }
            /* E.g. daserste.de */
            String filesize = getXML(stream, "size");
            if (StringUtils.isEmpty(filesize)) {
                /* E.g. kika.de */
                filesize = getXML(stream, "fileSize");
            }
            final String bitrate_video = getXML(stream, "bitrateVideo");
            final String bitrate_audio = getXML(stream, "bitrateAudio");
            final String width_str = getXML(stream, "frameWidth");
            final String height_str = getXML(stream, "frameHeight");
            int width = 0;
            int height = 0;
            if (width_str != null && width_str.matches("\\d+")) {
                width = Integer.parseInt(width_str);
            }
            if (height_str != null && height_str.matches("\\d+")) {
                height = Integer.parseInt(height_str);
            }
            if (StringUtils.isEmpty(http_url) || isUnsupportedProtocolDasersteVideo(http_url) || !http_url.startsWith("http")) {
                continue;
            }
            if (http_url.contains(".m3u8")) {
                hls_master = http_url;
                http_url = null;
            } else {
                /* hls master is stored in separate tag e.g. kika.de */
                hls_master = getXML(stream, "adaptiveHttpStreamingRedirectorUrl");
            }
            /* HLS master url may exist in every XML item --> We only have to add all HLS qualities once! */
            if (!StringUtils.isEmpty(hls_master) && !hls_master_dupelist.contains(hls_master)) {
                /* HLS */
                addHLS(this.br, hls_master);
                hls_master_dupelist.add(hls_master);
            }
            if (!StringUtils.isEmpty(http_url)) {
                /* http */
                long bitrate;
                if (!StringUtils.isEmpty(bitrate_video) && !StringUtils.isEmpty(bitrate_audio)) {
                    bitrate = Long.parseLong(bitrate_video) + Long.parseLong(bitrate_audio);
                    if (bitrate < 10000) {
                        bitrate = bitrate * 1000;
                    }
                } else {
                    bitrate = 0;
                }
                addQualityDasersteVideo(http_url, filesize, bitrate, width, height);
            }
        }
        return;
    }

    private void addHLS(final Browser br, final String hls_master) throws Exception {
        if (!this.grabHLS) {
            /* Avoid this http request if user hasn't selected any hls qualities */
            return;
        }
        Browser hlsBR;
        if (br.getURL().contains(".m3u8")) {
            /* HLS master has already been accessed before so no need to access it again. */
            hlsBR = br;
        } else {
            /* Access (hls) master. */
            hlsBR = br.cloneBrowser();
            hlsBR.getPage(hls_master);
        }
        final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(hlsBR);
        for (final HlsContainer hlscontainer : allHlsContainers) {
            final String final_download_url = hlscontainer.getDownloadurl();
            addQuality(final_download_url, null, hlscontainer.getBandwidth(), hlscontainer.getWidth(), hlscontainer.getHeight());
        }
    }

    /* Especially for video.daserste.de */
    private void addQualityDasersteVideo(final String directurl, final String filesize_str, long bitrate, int width, int height) {
        /* Get/Fix correct width/height values. */
        /* Type 1 */
        String width_URL = new Regex(directurl, "(hi|hq|ln|lo|mn)\\.mp4$").getMatch(0);
        if (width_URL == null) {
            /* Type 2 */
            width_URL = new Regex(directurl, "(s|m|sm|ml|l)\\.mp4$").getMatch(0);
        }
        if (width_URL == null) {
            /* Type 3 */
            width_URL = new Regex(directurl, "\\d+((?:_(?:webs|webl))?_ard)\\.mp4$").getMatch(0);
        }
        if (width_URL == null) {
            /* Type 4 */
            width_URL = new Regex(directurl, "/(\\d{1,4})\\-\\d+\\.mp4$").getMatch(0);
        }
        width = getWidth(width_URL, width);
        height = getHeight(width_URL, width, height);
        addQuality(directurl, filesize_str, bitrate, width, height);
    }

    /* Returns quality identifier String, compatible with quality selection values. Format: protocol_bitrateCorrected_heightCorrected */
    private String getQualityIdentifier(final String directurl, long bitrate, int width, int height) {
        final String protocol;
        if (directurl.contains("m3u8")) {
            protocol = "hls";
        } else {
            protocol = "http";
        }
        /* Use this for quality selection as real resolution can be slightly different than the values which our users can select. */
        final int height_corrected = getHeightForQualitySelection(height);
        final long bitrate_corrected;
        if (bitrate > 0) {
            bitrate_corrected = getBitrateForQualitySelection(bitrate, directurl);
        } else {
            bitrate_corrected = getDefaultBitrateForHeight(height_corrected);
        }
        final String qualityStringForQualitySelection = protocol + "_" + bitrate_corrected + "_" + height_corrected;
        return qualityStringForQualitySelection;
    }

    private DownloadLink addQuality(final String directurl, final String filesize_str, long bitrate, int width, int height) {
        /* Errorhandling */
        final String ext;
        if (directurl == null || ((width == 0 || height == 0) && !directurl.contains(".mp3"))) {
            /* Skip items with bad data. */
            return null;
        } else if (directurl.contains(".mp3")) {
            ext = ".mp3";
        } else {
            ext = ".mp4";
        }
        long filesize = 0;
        if (filesize_str != null && filesize_str.matches("\\d+")) {
            filesize = Long.parseLong(filesize_str);
        }
        /* Use real resolution inside filenames */
        final String resolution = width + "x" + height;
        final String protocol;
        if (directurl.contains("m3u8")) {
            protocol = "hls";
        } else {
            protocol = "http";
        }
        final ArdConfigInterface cfg = PluginJsonConfig.get(getConfigInterface());
        final String qualityStringForQualitySelection = getQualityIdentifier(directurl, bitrate, width, height);
        /* TODO: Maybe it makes sense not to include bitrate in the title if it is 0 - or use bitrate_corrected instead then. */
        final String plain_name = title + "_" + protocol + "_" + bitrate + "_" + resolution;
        final String file_name = plain_name + ext;
        final DownloadLink link = createDownloadlink(directurl.replaceAll("https?://", getHost() + "decrypted://"));
        link.setFinalFileName(file_name);
        link.setContentUrl(this.parameter);
        link.setProperty("plain_name", plain_name);
        link.setProperty("directName", file_name);
        /* new properties that can be used for future linkIDs */
        link.setProperty("itemSrc", getHost());
        link.setProperty("itemType", protocol);
        link.setProperty("itemRes", width + "x" + height);
        final String itemID = contentID;
        if (itemID == null) {
            logger.log(new Exception("FixMe!"));
        }
        link.setProperty("itemId", itemID);
        if (filesize > 0) {
            link.setDownloadSize(filesize);
            link.setAvailable(true);
        } else if (cfg.isFastLinkcheckEnabled()) {
            link.setAvailable(true);
        }
        foundQualitiesMap.put(qualityStringForQualitySelection, link);
        return link;
    }

    private void handleUserQualitySelection(List<String> selectedQualities) {
        /* We have to re-add the subtitle for the best quality if wished by the user */
        HashMap<String, DownloadLink> finalSelectedQualityMap = new HashMap<String, DownloadLink>();
        final Class<? extends ArdConfigInterface> cfgInterface = getConfigInterface();
        final ArdConfigInterface cfg = PluginJsonConfig.get(cfgInterface);
        if (cfg.isGrabBESTEnabled()) {
            /* User wants BEST only */
            finalSelectedQualityMap = findBESTInsideGivenMap(this.foundQualitiesMap);
        } else {
            final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
            final boolean grab_best_out_of_user_selection = cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled();
            boolean atLeastOneSelectedItemExists = false;
            for (final String quality : all_known_qualities) {
                if (selectedQualities.contains(quality) && foundQualitiesMap.containsKey(quality)) {
                    atLeastOneSelectedItemExists = true;
                }
            }
            if (!atLeastOneSelectedItemExists) {
                /* Only logger */
                logger.info("Possible user error: User selected only qualities which are not available --> Adding ALL");
            } else if (selectedQualities.size() == 0) {
                /* Errorhandling for bad user selection */
                logger.info("User selected no quality at all --> Adding ALL qualities instead");
                selectedQualities = all_known_qualities;
            }
            final Iterator<Entry<String, DownloadLink>> it = foundQualitiesMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, DownloadLink> entry = it.next();
                final String quality = entry.getKey();
                final DownloadLink dl = entry.getValue();
                final boolean isUnknownQuality = !all_known_qualities.contains(quality);
                if (isUnknownQuality) {
                    logger.info("Found unknown quality: " + quality);
                    if (grabUnknownQualities) {
                        logger.info("Adding unknown quality: " + quality);
                        finalSelectedQualityMap.put(quality, dl);
                    }
                } else if (selectedQualities.contains(quality) || !atLeastOneSelectedItemExists) {
                    /* User has selected this particular quality OR we have to add it because user plugin settings were bad! */
                    finalSelectedQualityMap.put(quality, dl);
                }
            }
            /* Check if user maybe only wants the best quality inside his selected videoqualities. */
            if (grab_best_out_of_user_selection) {
                finalSelectedQualityMap = findBESTInsideGivenMap(finalSelectedQualityMap);
            }
        }
        /* Finally add selected URLs */
        final Iterator<Entry<String, DownloadLink>> it = finalSelectedQualityMap.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> entry = it.next();
            final DownloadLink dl = entry.getValue();
            if (cfg.isGrabSubtitleEnabled() && !StringUtils.isEmpty(subtitleLink)) {
                final String plain_name = dl.getStringProperty("plain_name", null);
                final String subtitle_filename = plain_name + ".xml";
                final DownloadLink dl_subtitle = createDownloadlink(subtitleLink.replaceAll("https?://", getHost() + "decrypted://"));
                dl_subtitle.setAvailable(true);
                dl_subtitle.setFinalFileName(subtitle_filename);
                dl_subtitle.setProperty("directName", subtitle_filename);
                dl_subtitle.setProperty("streamingType", "subtitle");
                dl_subtitle.setProperty("mainlink", parameter);
                dl_subtitle.setProperty("itemSrc", getHost());
                dl_subtitle.setProperty("itemType", dl.getProperty("itemType", null) + "sub");
                dl_subtitle.setProperty("itemRes", dl.getProperty("itemRes", null));
                dl_subtitle.setProperty("itemId", dl.getProperty("itemId", null));
                dl_subtitle.setContentUrl(parameter);
                decryptedLinks.add(dl_subtitle);
            }
            decryptedLinks.add(dl);
        }
        if (all_known_qualities.isEmpty()) {
            logger.info("Failed to find any quality at all");
        }
        if (decryptedLinks.size() > 1) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }
    }

    private boolean isUnsupportedProtocolDasersteVideo(final String directlink) {
        final boolean isHTTPUrl = directlink == null || !directlink.startsWith("http") || directlink.endsWith("manifest.f4m");
        return isHTTPUrl;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> map_with_all_qualities) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (map_with_all_qualities.size() > 0) {
            for (final String quality : all_known_qualities) {
                keep = map_with_all_qualities.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = map_with_all_qualities;
        }
        return newMap;
    }

    /** Returns videos' width. Do not remove parts of this code without understanding them - this code is crucial for the plugin! */
    private int getWidth(final String width_str, final int width_given) {
        final int width;
        if (width_given > 0) {
            width = width_given;
        } else if (width_str != null) {
            if (width_str.matches("\\d+")) {
                width = Integer.parseInt(width_str);
            } else {
                /* Convert given quality-text to width. */
                if (width_str.equals("mn") || width_str.equals("sm")) {
                    width = 480;
                } else if (width_str.equals("hi") || width_str.equals("m") || width_str.equals("_ard")) {
                    width = 512;
                } else if (width_str.equals("ln") || width_str.equals("ml")) {
                    width = 640;
                } else if (width_str.equals("lo") || width_str.equals("s") || width_str.equals("_webs_ard")) {
                    width = 320;
                } else if (width_str.equals("hq") || width_str.equals("l") || width_str.equals("_webl_ard")) {
                    width = 960;
                } else {
                    width = 0;
                }
            }
        } else {
            /* This should never happen! */
            width = 0;
        }
        return width;
    }

    /** Returns videos' height. Do not remove parts of thise code without understanding them - this code is crucial for the plugin! */
    private int getHeight(final String width_str, final int width, final int height_given) {
        final int height;
        if (height_given > 0) {
            height = height_given;
        } else if (width_str != null) {
            height = Integer.parseInt(convertWidthToHeight(width_str));
        } else {
            /* This should never happen! */
            height = 0;
        }
        return height;
    }

    private String convertWidthToHeight(final String width_str) {
        final String height;
        if (width_str == null) {
            height = "0";
        } else if (width_str.matches("\\d+")) {
            final int width = Integer.parseInt(width_str);
            if (width == 320) {
                height = "180";
            } else if (width == 480) {
                height = "270";
            } else if (width == 512) {
                height = "288";
            } else if (width == 640) {
                height = "360";
            } else if (width == 960) {
                height = "540";
            } else {
                height = Integer.toString(width / 2);
            }
        } else {
            /* Convert given quality-text to height. */
            if (width_str.equals("mn") || width_str.equals("sm")) {
                height = "270";
            } else if (width_str.equals("hi") || width_str.equals("m") || width_str.equals("_ard")) {
                height = "288";
            } else if (width_str.equals("ln") || width_str.equals("ml")) {
                height = "360";
            } else if (width_str.equals("lo") || width_str.equals("s") || width_str.equals("_webs_ard")) {
                height = "180";
            } else if (width_str.equals("hq") || width_str.equals("l") || width_str.equals("_webl_ard")) {
                height = "540";
            } else {
                height = "0";
            }
        }
        return height;
    }

    /* Returns default videoBitrate for width values. */
    private long getDefaultBitrateForHeight(final int height) {
        final String height_str = Integer.toString(height);
        long bitrateVideo;
        if (heigth_to_bitrate.containsKey(height_str)) {
            bitrateVideo = heigth_to_bitrate.get(height_str);
        } else {
            /* Unknown or audio */
            bitrateVideo = 0;
        }
        return bitrateVideo;
    }

    /**
     * Given width may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    private int getHeightForQualitySelection(final int height) {
        final int heightelect;
        if (height > 0 && height <= 250) {
            heightelect = 180;
        } else if (height > 250 && height <= 272) {
            heightelect = 270;
        } else if (height > 272 && height <= 320) {
            heightelect = 280;
        } else if (height > 320 && height <= 400) {
            heightelect = 360;
        } else if (height > 400 && height < 576) {
            heightelect = 540;
        } else if (height >= 576 && height <= 600) {
            heightelect = 576;
        } else if (height > 600 && height <= 800) {
            heightelect = 720;
        } else {
            /* Either unknown quality or audio (0x0) */
            heightelect = height;
        }
        return heightelect;
    }

    /**
     * Given bandwidth may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    private long getBitrateForQualitySelection(final long bandwidth, final String directurl) {
        final long bandwidthselect;
        if (directurl != null && directurl.contains(".mp3")) {
            /* Audio --> There is usually only 1 bandwidth available so for our selection, we use the value 0. */
            bandwidthselect = 0;
        } else if (bandwidth > 0 && bandwidth <= 250000) {
            bandwidthselect = 189000;
        } else if (bandwidth > 250000 && bandwidth <= 350000) {
            /* lower 270 */
            bandwidthselect = 317000;
        } else if (bandwidth > 350000 && bandwidth <= 480000) {
            /* higher/normal 270 */
            bandwidthselect = 448000;
        } else if (bandwidth > 480000 && bandwidth <= 800000) {
            /* 280 */
            bandwidthselect = 605000;
        } else if (bandwidth > 800000 && bandwidth <= 1600000) {
            /* 360 */
            bandwidthselect = 1213000;
        } else if (bandwidth > 1600000 && bandwidth <= 2800000) {
            /* 540 */
            bandwidthselect = 1989000;
        } else if (bandwidth > 2800000 && bandwidth <= 4500000) {
            /* 720 */
            bandwidthselect = 3773000;
        } else {
            /* Probably unknown quality */
            bandwidthselect = bandwidth;
        }
        return bandwidthselect;
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String getXML(final String source, final String parameter) {
        return new Regex(source, "<" + parameter + "[^<]*?>([^<>]*?)</" + parameter + ">").getMatch(0);
    }

    public static final String correctRegionString(final String input) {
        String output;
        if (input.equals("de")) {
            output = "de";
        } else {
            output = "weltweit";
        }
        return output;
    }

    private String formatDateDasErste(String input) {
        final long date;
        if (input.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy HH:mm", Locale.GERMAN);
        } else {
            /* 2015-06-23T20:15:00.000+02:00 --> 2015-06-23T20:15:00.000+0200 */
            input = new Regex(input, "^(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd", Locale.GERMAN);
        }
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

    private String formatDate(final String input) {
        /* 13.01.2016 or 20160113 --> 2016-01-13 */
        final long date;
        if (input.matches("\\d{8}")) {
            /* e.g. eurovision.de */
            date = TimeFormatter.getMilliSeconds(input, "yyyyMMdd", Locale.GERMAN);
        } else {
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
        }
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