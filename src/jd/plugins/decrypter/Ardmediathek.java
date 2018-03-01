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
import java.util.Map.Entry;

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

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.translate._JDT;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ardmediathek.de", "mediathek.daserste.de", "daserste.de", "mediathek.rbb-online.de", "sandmann.de", "wdr.de", "sportschau.de", "one.ard.de", "wdrmaus.de", "sr-mediathek.sr-online.de", "ndr.de", "kika.de", "eurovision" }, urls = { "https?://(?:www\\.)?ardmediathek\\.de/.*?documentId=\\d+[^/]*?", "https?://(?:www\\.)?mediathek\\.daserste\\.de/.*?documentId=\\d+[^/]*?", "https?://www\\.daserste\\.de/[^<>\"]+/(?:videos|videosextern)/[a-z0-9\\-]+\\.html", "https?://(?:www\\.)?mediathek\\.rbb\\-online\\.de/tv/[^<>\"]+documentId=\\d+[^/]*?", "https?://(?:www\\.)?sandmann\\.de/.+", "https?://(?:[a-z0-9]+\\.)?wdr\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?sportschau\\.de/.*?\\.html", "https?://(?:www\\.)?one\\.ard\\.de/tv/[^<>\"]+documentId=\\d+[^/]*?", "https?://(?:www\\.)?wdrmaus\\.de/.+",
        "https?://sr\\-mediathek\\.sr\\-online\\.de/index\\.php\\?seite=\\d+\\&id=\\d+", "https?://(?:[a-z0-9]+\\.)?ndr\\.de/.*?\\.html", "https?://(?:www\\.)?kika\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?eurovision\\.de/[^<>\"]+\\.html" })
public class Ardmediathek extends PluginForDecrypt {
    private static final String                 EXCEPTION_LINKOFFLINE = "EXCEPTION_LINKOFFLINE";
    /* Constants */
    private static final String                 type_unsupported      = ".+ardmediathek\\.de/(tv/live\\?kanal=\\d+|dossiers/.*)";
    private static final String                 type_invalid          = ".+(ardmediathek|mediathek\\.daserste)\\.de/(download|livestream).+";
    /* Variables */
    private final HashMap<String, DownloadLink> foundQualitiesMap     = new HashMap<String, DownloadLink>();
    ArrayList<DownloadLink>                     decryptedLinks        = new ArrayList<DownloadLink>();
    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String>                  all_known_qualities   = Arrays.asList("http_0_720", "hls_0_720", "http_0_540", "hls_0_540", "http_0_360", "hls_0_360", "http_0_280", "hls_0_280", "http_0_270", "hls_0_270", "http_0_180", "hls_0_180");
    private String                              subtitleLink          = null;
    private String                              parameter             = null;
    private String                              title                 = null;
    private String                              date_formatted        = null;
    private boolean                             grabHLS               = false;

    public Ardmediathek(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @PluginHost(host = "eurovision.de", type = Type.CRAWLER)
    public static interface EurovisionConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "kika.de", type = Type.CRAWLER)
    public static interface KikaDeConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "ndr.de", type = Type.CRAWLER)
    public static interface NdrDeConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "sr-mediathek.sr-online.de", type = Type.CRAWLER)
    public static interface SrOnlineConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "sportschau.de", type = Type.CRAWLER)
    public static interface SportschauConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "wdr.de", type = Type.CRAWLER)
    public static interface WDRConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "wdrmaus.de", type = Type.CRAWLER)
    public static interface WDRMausConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "one.ard.de", type = Type.CRAWLER)
    public static interface OneARDConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "mediathek.daserste.de", type = Type.CRAWLER)
    public static interface MediathekDasersteConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "daerste.de", type = Type.CRAWLER)
    public static interface DasersteConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "mediathek.rbb-online.de", type = Type.CRAWLER)
    public static interface MediathekRbbOnlineConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "sandmann.de", type = Type.CRAWLER)
    public static interface SandmannDeConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "ardmediathek.de", type = Type.CRAWLER)
    public static interface ArdConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }

            public String getGrabSubtitleEnabled_label() {
                return _JDT.T.lit_add_subtitles();
            }

            public String getGrabAudio_label() {
                return _JDT.T.lit_add_audio();
            }

            public String getGrabBESTEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality();
            }

            public String getOnlyBestVideoQualityOfSelectedQualitiesEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality_within_user_selected_formats();
            }

            public String getAddUnknownQualitiesEnabled_label() {
                return _JDT.T.lit_add_unknown_formats();
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(true)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(10)
        boolean isGrabSubtitleEnabled();

        void setGrabSubtitleEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(11)
        boolean isGrabAudio();

        void setGrabAudio(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isGrabBESTEnabled();

        void setGrabBESTEnabled(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @Order(21)
        boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

        void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(21)
        boolean isAddUnknownQualitiesEnabled();

        void setAddUnknownQualitiesEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(30)
        boolean isGrabHLS180pVideoEnabled();

        void setGrabHLS180pVideoEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(40)
        boolean isGrabHLS270pVideoEnabled();

        void setGrabHLS270pVideoEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(41)
        boolean isGrabHLS280pVideoEnabled();

        void setGrabHLS280pVideoEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(50)
        boolean isGrabHLS360pVideoEnabled();

        void setGrabHLS360pVideoEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(60)
        boolean isGrabHLS480pVideoEnabled();

        void setGrabHLS480pVideoEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(70)
        boolean isGrabHLS540pVideoEnabled();

        void setGrabHLS540pVideoEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(80)
        boolean isGrabHLS720pVideoEnabled();

        void setGrabHLS720pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrabHTTP180pVideoEnabled();

        void setGrabHTTP180pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(100)
        boolean isGrabHTTP270pVideoEnabled();

        void setGrabHTTP270pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(110)
        boolean isGrabHTTP280pVideoEnabled();

        void setGrabHTTP280pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(120)
        boolean isGrabHTTP360pVideoEnabled();

        void setGrabHTTP360pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(130)
        boolean isGrabHTTP480pVideoEnabled();

        void setGrabHTTP480pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(140)
        boolean isGrabHTTP540pVideoEnabled();

        void setGrabHTTP540pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(150)
        boolean isGrabHTTP720pVideoEnabled();

        void setGrabHTTP720pVideoEnabled(boolean b);
    }

    /** 2018-02-22: TODO: Update this to work fine for all supported websites/domains! */
    @Override
    public Class<? extends ArdConfigInterface> getConfigInterface() {
        if ("ardmediathek.de".equalsIgnoreCase(getHost())) {
            return ArdConfigInterface.class;
        }
        return MediathekRbbOnlineConfig.class;
    }

    /**
     * Examples of other, unsupported linktypes:
     *
     * http://daserste.ndr.de/panorama/aktuell/Mal-eben-die-Welt-retten-Studie-belegt-Gefahren-durch-Voluntourismus-,volontourismus136.html
     *
     */
    /** TODO: Maybe add setting for version for disabled ppl ('Gebärdenversion'), example: http://www.wdrmaus.de/aktuelle-sendung/ */
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
        final boolean addAudio = cfg.isGrabAudio();
        final boolean addHLS180 = cfg.isGrabHLS180pVideoEnabled();
        final boolean addHLS270 = cfg.isGrabHLS270pVideoEnabled();
        final boolean addHLS280 = cfg.isGrabHLS280pVideoEnabled();
        final boolean addHLS360 = cfg.isGrabHLS360pVideoEnabled();
        final boolean addHLS540 = cfg.isGrabHLS540pVideoEnabled();
        final boolean addHLS720 = cfg.isGrabHLS720pVideoEnabled();
        grabHLS = addHLS180 || addHLS270 || addHLS280 || addHLS360 || addHLS540 || addHLS720;
        if (addHLS180) {
            selectedQualities.add("hls_0_180");
        }
        if (addHLS270) {
            selectedQualities.add("hls_0_270");
        }
        if (addHLS280) {
            selectedQualities.add("hls_0_280");
        }
        if (addHLS360) {
            selectedQualities.add("hls_0_360");
        }
        if (addHLS540) {
            selectedQualities.add("hls_0_540");
        }
        if (addHLS720) {
            selectedQualities.add("hls_0_720");
        }
        if (cfg.isGrabHTTP180pVideoEnabled()) {
            selectedQualities.add("http_0_180");
        }
        if (cfg.isGrabHTTP270pVideoEnabled()) {
            selectedQualities.add("http_0_270");
        }
        if (cfg.isGrabHTTP280pVideoEnabled()) {
            selectedQualities.add("http_0_280");
        }
        if (cfg.isGrabHTTP360pVideoEnabled()) {
            selectedQualities.add("http_0_360");
        }
        if (cfg.isGrabHTTP540pVideoEnabled()) {
            selectedQualities.add("http_0_540");
        }
        if (cfg.isGrabHTTP720pVideoEnabled()) {
            selectedQualities.add("http_0_720");
        }
        if (addAudio) {
            selectedQualities.add("audio");
        }
        try {
            final String host = Browser.getHost(this.parameter);
            /*
             * 2018-02-22: Important: So far there is only one OLD website, not compatible with the "decryptMediathek" function! Keep this
             * in mind when changing things!
             */
            if (host.equalsIgnoreCase("daserste.de") || host.equalsIgnoreCase("kika.de")) {
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

    /* Returns title, with fallback if nothing found in html */
    private String getMediathekTitle() {
        final String jsonSchemaOrg = br.getRegex("<script type=\"application/ld\\+json\">(.*?)</script>").getMatch(0);
        String title = null;
        String provider = null;
        /* TODO: Find name of the Show whenever possible */
        String show = null;
        /* These RegExes should be compatible with all websites */
        /* Date is already provided in the format we need. */
        String date = null;
        this.date_formatted = br.getRegex("<meta property=\"video:release_date\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2})[^\"]*?\"[^>]*?/>").getMatch(0);
        String description = br.getRegex("<meta property=\"og:description\" content=\"([^\"]+)\"").getMatch(0);
        final String host = br.getHost();
        if (jsonSchemaOrg != null) {
            /* 2018-02-15: E.g. daserste.de, wdr.de */
            final String headline = br.getRegex("<h3 class=\"headline\">([^<>]+)</h3>").getMatch(0);
            try {
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(jsonSchemaOrg);
                final String uploadDate = (String) entries.get("uploadDate");
                title = (String) entries.get("name");
                if ("Video".equalsIgnoreCase(title) && !StringUtils.isEmpty(headline)) {
                    /**
                     * TODO: 2018-02-22: Some of these schema-objects contain wrong information e.g.
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
            final String content_ids_str = br.getRegex("var _contentId = \\[([^<>\\[\\]]+)\\];").getMatch(0);
            if (content_ids_str != null) {
                final String[] content_ids = content_ids_str.split(",");
                if (content_ids != null && content_ids.length >= 3) {
                    show = content_ids[0];
                    title = content_ids[2];
                }
            }
            if (StringUtils.isEmpty(title)) {
                title = br.getRegex("<title>([^<>]+) \\- Die Sendung mit der Maus \\- WDR</title>").getMatch(0);
            }
            if (StringUtils.isEmpty(show) && (br.getURL().contains("/lachgeschichten") || br.getURL().contains("/sachgeschichten"))) {
                // show = "Die Sendung mit der Maus";
                show = "Lach- und Sachgeschichten";
            }
            /*
             * 2018-02-22: TODO: This may sometimes be inaccurate when there are multiple videoObjects on one page (rare case) e.g.
             * http://www.wdrmaus.de/extras/mausthemen/eisenbahn/index.php5
             */
            date = br.getRegex("Sendetermin: (\\d{2}\\.\\d{2}\\.\\d{4})").getMatch(0);
        } else if (host.contains("sr-online.de")) {
            /* sr-mediathek.sr-online.de */
            title = br.getRegex("<div class=\"ardplayer\\-title\">([^<>\"]+)</div>").getMatch(0);
            date = br.getRegex("<p>Video \\| (\\d{2}\\.\\d{2}\\.\\d{4}) \\| Dauer:").getMatch(0);
        } else if (host.equalsIgnoreCase("ndr.de") || host.equalsIgnoreCase("eurovision.de")) {
            /* ndr.de */
            title = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\"/>").getMatch(0);
        } else {
            /* E.g. ardmediathek.de */
            show = br.getRegex("name=\"dcterms\\.isPartOf\" content=\"([^<>\"]*?)\"").getMatch(0);
            title = br.getRegex("<meta name=\"dcterms\\.title\" content=\"([^\"]+)\"").getMatch(0);
        }
        if (StringUtils.isEmpty(this.date_formatted) && !StringUtils.isEmpty(date)) {
            /* Rare case: Date available but we need to change it to our target-format. */
            this.date_formatted = formatDate(date);
        }
        if (StringUtils.isEmpty(title)) {
            title = getTitleArdmediathek(br);
        }
        if (StringUtils.isEmpty(provider)) {
            /* Fallback */
            provider = host.substring(0, br.getHost().lastIndexOf("."));
        }
        title = Encoding.htmlDecode(title).trim();
        if (!StringUtils.isEmpty(show)) {
            title = show + " - " + title;
        }
        title = provider + "_" + title;
        if (this.date_formatted != null) {
            title = this.date_formatted + "_" + title;
        }
        title = encodeUnicode(title);
        return title;
    }

    /** Find json URL which leads to subtitle and video stream URLs. */
    private String getVideoJsonURL() throws MalformedURLException {
        String url_json = null;
        final String ardBroadcastID = new Regex(br.getURL(), "(?:\\?|\\&)documentId=(\\d+)").getMatch(0);
        if (Browser.getHost(this.parameter).contains("sr-online.de")) {
            url_json = String.format("http://www.sr-mediathek.de/sr_player/mc.php?id=%s&tbl=&pnr=0&hd=0&devicetype=", new Regex(br.getURL(), "id=(\\d+)").getMatch(0));
        } else if (Browser.getHost(this.parameter).equalsIgnoreCase("sandmann.de")) {
            url_json = br.getRegex("data\\-media\\-ref=\"([^\"]+)\"").getMatch(0);
            if (!StringUtils.isEmpty(url_json) && url_json.startsWith("/")) {
                url_json = "https://www.sandmann.de" + url_json;
            }
        } else if (ardBroadcastID != null) {
            url_json = String.format("http://www.ardmediathek.de/play/media/%s?devicetype=pc&features=flash", ardBroadcastID);
        } else if (Browser.getHost(this.parameter).contains("ndr.de") || Browser.getHost(this.parameter).equalsIgnoreCase("eurovision.de")) {
            /* E.g. daserste.ndr.de, blabla.ndr.de */
            final String video_id = br.getRegex("([A-Za-z0-9]+\\d+)\\-(?:ard)?player_[^\"]+\"").getMatch(0);
            if (!StringUtils.isEmpty(video_id)) {
                url_json = String.format("https://www.ndr.de/%s-ardjson.json", video_id);
            }
        } else {
            // if (br.getURL().contains("one.ard.")) {
            // /* one.ard.de --> Back then this was "einsfestival" */
            // } else {
            // url_json =
            // this.br.getRegex("(?:\\'|\")mediaObj(?:\\'|\"):\\s*?\\{\\s*?(?:\\'|\")url(?:\\'|\"):\\s*?(?:\\'|\")(https?://[^<>\"]+\\.js)(?:\\'|\")").getMatch(0);
            // }
            /* wdr.de, one.ard.de */
            url_json = this.br.getRegex("(?:\\'|\")mediaObj(?:\\'|\"):\\s*?\\{\\s*?(?:\\'|\")url(?:\\'|\"):\\s*?(?:\\'|\")(https?://[^<>\"]+\\.js)(?:\\'|\")").getMatch(0);
        }
        return url_json;
    }

    /** Find xml URL which leads to subtitle and video stream URLs. */
    private String getVideoXMLURL() throws Exception {
        final String host = Browser.getHost(this.parameter);
        String url_xml = null;
        if (host.equalsIgnoreCase("daserste.de")) {
            /* We do not even have to access the main URL which the user has added :) */
            url_xml = parameter.replace(".html", "~playerXml.xml");
        } else {
            br.getPage(this.parameter);
            if (isOffline(this.br)) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            if (host.equalsIgnoreCase("kika.de")) {
                url_xml = br.getRegex("\\{dataURL\\s*?:\\s*?\\'(https?://[^<>\"]+\\-avCustom\\.xml)\\'").getMatch(0);
            }
        }
        return url_xml;
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

    private String getTitleArdmediathek(final Browser br) {
        String title = br.getRegex("<(div|span) class=\"(MainBoxHeadline|BoxHeadline)\">([^<]+)</").getMatch(2);
        String titleUT = br.getRegex("<span class=\"(BoxHeadlineUT|boxSubHeadline)\">([^<]+)</").getMatch(1);
        if (titleUT == null) {
            titleUT = br.getRegex("<h3 class=\"mt\\-title\"><a>([^<>\"]*?)</a></h3>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<title>ard\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("class=\"mt\\-icon mt\\-icon_video\"></span><img src=\"[^\"]+\" alt=\"([^\"]+)\"").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("class=\"mt\\-icon mt\\-icon\\-toggle_arrows\"></span>([^<>\"]*?)</a>").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        }
        if (title == null) {
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        title = title.replaceAll("\\n|\\t|,", "").trim();
        return title;
    }

    /** Last revision with old handling: 38658 */
    private void decryptMediathek() throws Exception {
        if (isOffline(this.br)) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        title = getMediathekTitle();
        final Browser br = new Browser();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final String url_json = getVideoJsonURL();
        if (StringUtils.isEmpty(url_json)) {
            /* No downloadable content --> URL should be offline (or only text content) */
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        br.getPage(url_json);
        /* No json --> No media to crawl (rare case!)! */
        if (!br.getHttpConnection().getContentType().contains("application/json") && !br.getHttpConnection().getContentType().contains("application/javascript") && !br.containsHTML("\\{") || br.getHttpConnection().getResponseCode() == 404) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        subtitleLink = getJsonSubtitleURL(br);
        /* We know how their http urls look - this way we can avoid HDS/HLS/RTMP */
        /*
         * http://adaptiv.wdr.de/z/medp/ww/fsk0/104/1046579/,1046579_11834667,1046579_11834665,1046579_11834669,.mp4.csmil/manifest.f4
         */
        // //wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/139/1394333/,1394333_16295554,1394333_16295556,1394333_16295555,1394333_16295557,1394333_16295553,1394333_16295558,.mp4.csmil/master.m3u8
        /* TODO: Maybe improve finding these mp3 URLs. */
        final String http_url_audio = br.getRegex("(https?://[^<>\"]+\\.mp3)\"").getMatch(0);
        final String hls_master = br.getRegex("(//[^<>\"]+\\.m3u8[^<>\"]*?)").getMatch(0);
        final Regex regex_hls = new Regex(hls_master, ".+/([^/]+/[^/]+/[^,/]+)(?:/|_|\\.),([A-Za-z0-9_,\\-]+),\\.mp4\\.csmil/?");
        final String quality_string = regex_hls.getMatch(1);
        if (StringUtils.isEmpty(hls_master) && http_url_audio == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Plugin broken");
        }
        if (hls_master != null) {
            String urlpart = regex_hls.getMatch(0);
            String urlpart2 = new Regex(hls_master, "//[^/]+/[^/]+/(.*?)(?:/|_),").getMatch(0);
            String http_url_format = null;
            /**
             * hls --> http urls (whenever possible) <br />
             * TODO: Improve this part!
             */
            /* First case */
            if (hls_master.contains("rbbmediaadp-vh") && urlpart != null) {
                /* For all RBB websites e.g. sandmann.de */
                http_url_format = "https://rbbmediapmdp-a.akamaihd.net/content/" + urlpart + "_%s.mp4";
            } else if (hls_master.contains("sr_hls_od-vh") && urlpart != null) {
                http_url_format = "http://mediastorage01.sr-online.de/Video/" + urlpart + "_%s.mp4";
            } else if (hls_master.contains("ndrod-vh.akamaihd.net") && urlpart != null) {
                http_url_format = "https://mediandr-a.akamaihd.net/progressive/" + urlpart + ".%s.mp4";
            }
            /* 2nd case */
            if (hls_master.contains("dasersteuni-vh.akamaihd.net") || hls_master.contains("br-i.akamaihd.net")) {
                if (urlpart2 != null) {
                    http_url_format = "https://pdvideosdaserste-a.akamaihd.net/" + urlpart2 + "/%s.mp4";
                }
            } else if (hls_master.contains("wdradaptiv-vh.akamaihd.net") && urlpart2 != null) {
                /* wdr */
                http_url_format = "http://wdrmedien-a.akamaihd.net/" + urlpart2 + "/%s.mp4";
            }
            /* Access HLS master to find correct resolution for each ID (the only possible way) */
            br.getPage("http:" + hls_master);
            final String[] resolutionsInOrder = br.getRegex("RESOLUTION=(\\d+x\\d+)").getColumn(0);
            final String[] qualities = quality_string != null ? quality_string.split(",") : null;
            if (resolutionsInOrder != null && qualities != null) {
                logger.info("Crawling http urls");
                for (int counter = 0; counter <= qualities.length - 1; counter++) {
                    if (counter > qualities.length - 1 || counter > resolutionsInOrder.length - 1) {
                        break;
                    }
                    /* Old */
                    // String final_url = "http://http-ras.wdr.de/CMS2010/mdb/ondemand/" + region + "/" + fsk_url + "/";
                    /* 2016-02-16 new e.g. http://ondemand-ww.wdr.de/medp/fsk0/105/1058266/1058266_12111633.mp4 */
                    /* 2018-02-13: new e.g. http://wdrmedien-a.akamaihd.net/medp/ondemand/de/fsk0/158/1580248/1580248_18183304.mp4 */
                    final String quality_id = qualities[counter];
                    final String final_url = String.format(http_url_format, quality_id);
                    // final String linkid = qualities[counter];
                    final String resolution = resolutionsInOrder[counter];
                    final String[] height_width = resolution.split("x");
                    final String width = height_width[0];
                    final String height = height_width[1];
                    addQuality(final_url, null, Integer.parseInt(width), Integer.parseInt(height));
                }
            } else {
                logger.warning("Failed to find http urls; adding hls only");
            }
            addHLS(br, hls_master);
        }
        if (http_url_audio != null) {
            addQuality(http_url_audio, null, 0, 0);
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
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getHttpConnection().getContentType().contains("xml")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        final String fskRating = this.br.getRegex("<fskRating>fsk(\\d+)</fskRating>").getMatch(0);
        if (fskRating != null && Short.parseShort(fskRating) >= 12) {
            /* Video is age restricted --> Only available from >=8PM. */
            decryptedLinks.add(this.createOfflinelink(parameter, "FSK_BLOCKED"));
            return;
        }
        final String date = getXML("broadcastDate");
        title = getXML("shareTitle");
        if (StringUtils.isEmpty(title)) {
            title = getXML("broadcastName");
        }
        if (this.title == null || date == null) {
            throw new DecrypterException("Decrypter broken");
        }
        title = Encoding.htmlDecode(title).trim();
        title = encodeUnicode(title);
        this.date_formatted = formatDateDasErste(date);
        title = this.date_formatted + "_daserste_" + title;
        final String[] mediaStreamArray = br.getRegex("(<asset.*?</asset>)").getColumn(0);
        for (final String stream : mediaStreamArray) {
            /* E.g. kika.de */
            String directurl = getXML(stream, "progressiveDownloadUrl");
            if (StringUtils.isEmpty(directurl)) {
                /* E.g. daserste.de */
                directurl = getXML(stream, "fileName");
            }
            final String filesize = getXML(stream, "size");
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
            if (StringUtils.isEmpty(directurl) || isUnsupportedProtocolDasersteVideo(directurl) || !directurl.startsWith("http")) {
                continue;
            }
            if (directurl.contains(".m3u8")) {
                /* HLS */
                addHLS(this.br, directurl);
            } else {
                /* HTTP */
                addQualityDasersteVideo(directurl, filesize, width, height);
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
            addQuality(final_download_url, null, hlscontainer.getWidth(), hlscontainer.getHeight());
        }
    }

    /* Especially for video.daserste.de */
    private void addQualityDasersteVideo(final String directurl, final String filesize_str, int width, int height) {
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
        addQuality(directurl, filesize_str, width, height);
    }

    private void addQuality(final String directurl, final String filesize_str, int width, int height) {
        /* Errorhandling */
        final String ext;
        if (directurl == null || ((width == 0 || height == 0) && !directurl.contains(".mp3"))) {
            /* Skip items with bad data. */
            return;
        } else if (directurl.contains(".mp3")) {
            ext = ".mp3";
        } else {
            ext = ".mp4";
        }
        /* Use this for quality selection as real resolution can be slightly different than the values which our users can select. */
        final String height_final = getHeightForQualitySelection(height);
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
        /* 2018-02-28: This should work fine for all hls- and http urls */
        final String filename_server = jd.plugins.hoster.ARDMediathek.getUniqueURLServerFilenameString(directurl);
        final String plain_name = title + "_" + protocol + "_" + resolution;
        final String linkid;
        if (!StringUtils.isEmpty(filename_server)) {
            linkid = filename_server;
        } else {
            linkid = plain_name;
        }
        final String full_name = plain_name + ext;
        final String qualityStringSelection = protocol + "_0_" + height_final;
        // final String qualityStringFull = protocol + "_" + resolution;
        final DownloadLink link = createDownloadlink(directurl.replaceAll("https?://", "ardmediathek://"));
        link.setFinalFileName(full_name);
        link.setContentUrl(this.parameter);
        /* This is not perfect but should be enough as a unique identifier! */
        link.setLinkID(linkid);
        /* 2018-02-22: Only store what we really need! */
        link.setProperty("plain_name", plain_name);
        // link.setProperty("mainlink", this.parameter);
        link.setProperty("directName", full_name);
        // new properties that can be used for future linkIDs
        link.setProperty("itemSrc", getHost());
        link.setProperty("itemType", protocol);
        link.setProperty("itemRes", width + "x" + height);
        String itemID = new Regex(parameter, "(?:\\?|\\&)documentId=(\\d+)").getMatch(0);
        if (itemID == null) {
            // TODO
        }
        link.setProperty("itemId", itemID);
        if (filesize > 0) {
            link.setDownloadSize(filesize);
            link.setAvailable(true);
        } else if (cfg.isFastLinkcheckEnabled()) {
            link.setAvailable(true);
        }
        foundQualitiesMap.put(qualityStringSelection, link);
    }

    private void handleUserQualitySelection(List<String> selectedQualities) {
        /* We have to re-add the subtitle for the best quality if wished by the user */
        HashMap<String, DownloadLink> finalSelectedQualityMap = new HashMap<String, DownloadLink>();
        if (PluginJsonConfig.get(getConfigInterface()).isGrabBESTEnabled()) {
            /* User wants BEST only */
            finalSelectedQualityMap = findBESTInsideGivenMap(this.foundQualitiesMap);
        } else {
            final boolean grabUnknownQualities = PluginJsonConfig.get(getConfigInterface()).isAddUnknownQualitiesEnabled();
            final boolean grab_best_out_of_user_selection = PluginJsonConfig.get(getConfigInterface()).isOnlyBestVideoQualityOfSelectedQualitiesEnabled();
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
            if (PluginJsonConfig.get(getConfigInterface()).isGrabSubtitleEnabled() && !StringUtils.isEmpty(subtitleLink)) {
                final String plain_name = dl.getStringProperty("plain_name", null);
                final String linkid = dl.getLinkID() + "_subtitle";
                final String subtitle_filename = plain_name + ".xml";
                final DownloadLink dl_subtitle = createDownloadlink(subtitleLink.replaceAll("https?://", "ardmediathek://"));
                dl_subtitle.setAvailable(true);
                dl_subtitle.setFinalFileName(subtitle_filename);
                dl_subtitle.setProperty("directName", subtitle_filename);
                dl_subtitle.setProperty("streamingType", "subtitle");
                dl_subtitle.setProperty("mainlink", parameter);
                dl_subtitle.setContentUrl(parameter);
                dl_subtitle.setLinkID(linkid);
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

    /**
     * Given width may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    private String getHeightForQualitySelection(final int height) {
        final String heightelect;
        if (height > 0 && height <= 200) {
            heightelect = "180";
        } else if (height > 200 && height <= 272) {
            heightelect = "270";
        } else if (height > 272 && height <= 320) {
            heightelect = "280";
        } else if (height > 320 && height <= 400) {
            heightelect = "360";
        } else if (height > 400 && height <= 600) {
            heightelect = "540";
        } else if (height > 600 && height <= 800) {
            heightelect = "720";
        } else {
            /* Either unknown quality or audio (0x0) */
            heightelect = Integer.toString(height);
        }
        return heightelect;
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String getXML(final String source, final String parameter) {
        return new Regex(source, "<" + parameter + "[^<]*?>([^<>]*?)</" + parameter + ">").getMatch(0);
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
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

    // private String formatDateArdMediathek(final String input) {
    // final long date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
    // String formattedDate = null;
    // final String targetFormat = "yyyy-MM-dd";
    // Date theDate = new Date(date);
    // try {
    // final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
    // formattedDate = formatter.format(theDate);
    // } catch (Exception e) {
    // /* prevent input error killing plugin */
    // formattedDate = input;
    // }
    // return formattedDate;
    // }
    private String formatDateDasErste(String input) {
        /* 2015-06-23T20:15:00.000+02:00 --> 2015-06-23T20:15:00.000+0200 */
        input = new Regex(input, "^(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        final long date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd", Locale.GERMAN);
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
        /* 13.01.2016 --> 2016-01-13 */
        final long date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
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