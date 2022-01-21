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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.ArdConfigInterface;
import org.jdownloader.plugins.components.config.DasersteConfig;
import org.jdownloader.plugins.components.config.EurovisionConfig;
import org.jdownloader.plugins.components.config.KikaDeConfig;
import org.jdownloader.plugins.components.config.MdrDeConfig;
import org.jdownloader.plugins.components.config.MediathekDasersteConfig;
import org.jdownloader.plugins.components.config.MediathekProperties;
import org.jdownloader.plugins.components.config.SandmannDeConfig;
import org.jdownloader.plugins.components.config.SportschauConfig;
import org.jdownloader.plugins.components.config.SputnikDeConfig;
import org.jdownloader.plugins.components.config.TagesschauDeConfig;
import org.jdownloader.plugins.components.config.WDRConfig;
import org.jdownloader.plugins.components.config.WDRMausConfig;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.MediathekHelper;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ardmediathek.de", "mediathek.daserste.de", "daserste.de", "sandmann.de", "wdr.de", "sportschau.de", "wdrmaus.de", "kika.de", "eurovision.de", "sputnik.de", "mdr.de", "ndr.de", "tagesschau.de" }, urls = { "https?://(?:[A-Z0-9]+\\.)?ardmediathek\\.de/.+", "https?://(?:www\\.)?mediathek\\.daserste\\.de/.*?documentId=\\d+[^/]*?", "https?://www\\.daserste\\.de/.*?\\.html", "https?://(?:www\\.)?sandmann\\.de/.+", "https?://(?:[a-z0-9]+\\.)?wdr\\.de/[^<>\"]+\\.html|https?://deviceids-[a-z0-9\\-]+\\.wdr\\.de/ondemand/\\d+/\\d+\\.js", "https?://(?:\\w+\\.)?sportschau\\.de/.*?\\.html", "https?://(?:www\\.)?wdrmaus\\.de/.+", "https?://(?:www\\.)?kika\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?eurovision\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?sputnik\\.de/[^<>\"]+\\.html",
        "https?://(?:www\\.)?mdr\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?ndr\\.de/[^<>\"]+\\.html", "https?://(?:www\\.)?tagesschau\\.de/[^<>\"]+\\.html" })
public class Ardmediathek extends PluginForDecrypt {
    /* Constants */
    private static final String type_embedded       = "https?://deviceids-[a-z0-9\\-]+\\.wdr\\.de/ondemand/\\d+/\\d+\\.js";
    /* Variables */
    private final List<String>  all_known_qualities = new ArrayList<String>();
    private final List<String>  selectedQualities   = new ArrayList<String>();
    private String              subtitleLink        = null;
    private boolean             grabHLS             = false;
    private ArdConfigInterface  cfg                 = null;

    public Ardmediathek(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends ArdConfigInterface> getConfigInterface() {
        if ("ardmediathek.de".equalsIgnoreCase(getHost())) {
            return ArdConfigInterface.class;
        } else if ("daserste.de".equalsIgnoreCase(getHost())) {
            return DasersteConfig.class;
        } else if ("mediathek.daserste.de".equalsIgnoreCase(getHost())) {
            return MediathekDasersteConfig.class;
        } else if ("wdrmaus.de".equalsIgnoreCase(getHost())) {
            return WDRMausConfig.class;
        } else if ("wdr.de".equalsIgnoreCase(getHost())) {
            return WDRConfig.class;
        } else if ("sportschau.de".equalsIgnoreCase(getHost())) {
            return SportschauConfig.class;
        } else if ("kika.de".equalsIgnoreCase(getHost())) {
            return KikaDeConfig.class;
        } else if ("eurovision.de".equalsIgnoreCase(getHost())) {
            return EurovisionConfig.class;
        } else if ("sputnik.de".equalsIgnoreCase(getHost())) {
            return SputnikDeConfig.class;
        } else if ("sandmann.de".equalsIgnoreCase(getHost())) {
            return SandmannDeConfig.class;
        } else if ("mdr.de".equalsIgnoreCase(getHost())) {
            return MdrDeConfig.class;
        } else if ("tagesschau.de".equalsIgnoreCase(getHost())) {
            return TagesschauDeConfig.class;
        } else {
            return ArdConfigInterface.class;
        }
    }

    private void initSelectedQualities() {
        if (cfg.isGrabHLS180pVideoEnabled()) {
            selectedQualities.add("hls_180");
        }
        if (cfg.isGrabHLS144pVideoEnabled()) {
            selectedQualities.add("hls_144");
        }
        if (cfg.isGrabHLS270pVideoEnabled()) {
            selectedQualities.add("hls_270");
        }
        if (cfg.isGrabHLS280pVideoEnabled()) {
            selectedQualities.add("hls_280");
        }
        if (cfg.isGrabHLS288pVideoEnabled()) {
            selectedQualities.add("hls_288");
        }
        if (cfg.isGrabHLS360pVideoEnabled()) {
            selectedQualities.add("hls_360");
        }
        if (cfg.isGrabHLS540pVideoEnabled()) {
            selectedQualities.add("hls_540");
        }
        if (cfg.isGrabHLS720pVideoEnabled()) {
            selectedQualities.add("hls_720");
        }
        if (cfg.isGrabHLS1080pVideoEnabled()) {
            selectedQualities.add("hls_1080");
        }
        /* If user has deselected all HLS qualities, we can later skip HLS crawling entirely which speeds up the crawl process. */
        if (selectedQualities.size() > 0) {
            this.grabHLS = true;
        } else {
            this.grabHLS = false;
        }
        if (cfg.isGrabHTTP144pVideoEnabled()) {
            selectedQualities.add("http_144");
        }
        if (cfg.isGrabHTTP180pVideoEnabled()) {
            selectedQualities.add("http_180");
        }
        if (cfg.isGrabHTTP270pVideoEnabled()) {
            selectedQualities.add("http_270");
        }
        if (cfg.isGrabHTTP280pVideoEnabled()) {
            selectedQualities.add("http_280");
        }
        if (cfg.isGrabHTTP288pVideoEnabled()) {
            selectedQualities.add("http_288");
        }
        if (cfg.isGrabHTTP360pVideoEnabled()) {
            selectedQualities.add("http_360");
        }
        if (cfg.isGrabHTTP540pVideoEnabled()) {
            selectedQualities.add("http_540");
        }
        if (cfg.isGrabHTTP720pVideoEnabled()) {
            selectedQualities.add("http_720");
        }
        if (cfg.isGrabHTTP1080pVideoEnabled()) {
            selectedQualities.add("http_1080");
        }
    }

    private void initKnownQualities() {
        for (final VideoResolution res : VideoResolution.values()) {
            all_known_qualities.add("http_" + res.getHeight());
            all_known_qualities.add("hls_" + res.getHeight());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        cfg = PluginJsonConfig.get(getConfigInterface());
        initSelectedQualities();
        initKnownQualities();
        /*
         * 2018-02-22: Important: So far there is only one OLD website, not compatible with the "decryptMediathek" function! Keep this in
         * mind when changing things!
         */
        final String host = this.getHost();
        if (param.getCryptedUrl().matches(type_embedded)) {
            return this.crawlWdrMediathekEmbedded(param, param.getCryptedUrl());
        } else if (host.equalsIgnoreCase("wdr.de") || host.equalsIgnoreCase("wdrmaus.de")) {
            return this.crawlWdrMediathek(param);
        } else if (host.equalsIgnoreCase("daserste.de") || host.equalsIgnoreCase("kika.de") || host.equalsIgnoreCase("sputnik.de") || host.equalsIgnoreCase("mdr.de")) {
            return crawlDasersteVideo(param);
        } else if (host.equalsIgnoreCase("tagesschau.de")) {
            return this.crawlTagesschauVideos(param);
        } else if (host.equalsIgnoreCase("ardmediathek.de")) {
            /* 2020-05-26: Separate handling required */
            return this.crawlArdmediathekDeNew(param);
        } else {
            return this.crawlMediathek(param);
        }
    }

    private void errorGEOBlocked(final CryptedLink param) throws DecrypterRetryException {
        throw new DecrypterRetryException(RetryReason.GEO);
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find subtitle URL inside xml String
     */
    private String getXMLSubtitleURL(final Browser xmlBR) throws IOException {
        final String subtitleURL = getXML(xmlBR.toString(), "videoSubtitleUrl");
        if (subtitleURL != null) {
            return xmlBR.getURL(subtitleURL).toString();
        } else {
            return null;
        }
    }

    /**
     * Find subtitle URL inside json String
     *
     * @throws MalformedURLException
     */
    private String getJsonSubtitleURL(final Browser jsonBR) throws IOException {
        String subtitleURL;
        if (br.getURL().contains("wdr.de/")) {
            subtitleURL = PluginJSonUtils.getJsonValue(jsonBR, "captionURL");
            if (subtitleURL == null) {
                // TODO: check other formats
                subtitleURL = PluginJSonUtils.getJsonValue(jsonBR, "xml");
            }
        } else {
            subtitleURL = PluginJSonUtils.getJson(jsonBR, "_subtitleUrl");
        }
        if (subtitleURL != null) {
            return jsonBR.getURL(subtitleURL).toString();
        } else {
            return null;
        }
    }

    private String getHlsToHttpURLFormat(final String hlsMaster, final String exampleHTTPURL) {
        final Regex regex_hls = new Regex(hlsMaster, ".+/([^/]+/[^/]+/[^,/]+)(?:/|_|\\.),([A-Za-z0-9_,\\-]+),\\.mp4\\.csmil/?");
        String urlpart = regex_hls.getMatch(0);
        String urlpart2 = new Regex(hlsMaster, "//[^/]+/[^/]+/(.*?)(?:/|_),").getMatch(0);
        String http_url_format = null;
        /**
         * hls --> http urls (whenever possible) <br />
         */
        /* First case */
        if (hlsMaster.contains("sr_hls_od-vh") && urlpart != null) {
            http_url_format = "http://mediastorage01.sr-online.de/Video/" + urlpart + "_%s.mp4";
        }
        /* 2020-06-02: Do NOT yet try to make a generic RegEx for all types of HLS URLs!! */
        final String pattern_ard = ".*//hlsodswr-vh\\.akamaihd\\.net/i/(.*?),.*?\\.mp4\\.csmil/master\\.m3u8";
        final String pattern_hr = ".*//hrardmediathek-vh\\.akamaihd.net/i/(.*?),.+\\.mp4\\.csmil/master\\.m3u8$";
        /* Daserste */
        if (hlsMaster.contains("dasersteuni-vh.akamaihd.net")) {
            if (urlpart2 != null) {
                http_url_format = "https://pdvideosdaserste-a.akamaihd.net/" + urlpart2 + "/%s.mp4";
            }
        } else if (hlsMaster.contains("br-i.akamaihd.net")) {
            if (urlpart2 != null) {
                http_url_format = "http://cdn-storage.br.de/" + urlpart2 + "_%s.mp4";
            }
        } else if (hlsMaster.contains("wdradaptiv-vh.akamaihd.net") && urlpart2 != null) {
            /* wdr */
            http_url_format = "http://wdrmedien-a.akamaihd.net/" + urlpart2 + "/%s.mp4";
        } else if (hlsMaster.contains("rbbmediaadp-vh") && urlpart2 != null) {
            /* For all RBB websites e.g. also sandmann.de */
            http_url_format = "https://rbbmediapmdp-a.akamaihd.net/" + urlpart2 + "_%s.mp4";
        } else if (hlsMaster.contains("ndrod-vh.akamaihd.net") && urlpart != null) {
            /* 2018-03-07: There is '/progressive/' and '/progressive_geo/' --> We have to grab this from existing http urls */
            final String baseHttp;
            if (exampleHTTPURL != null) {
                baseHttp = new Regex(exampleHTTPURL, "(https?://[^/]+/[^/]+/)").getMatch(0);
            } else {
                baseHttp = br.getRegex("(https?://mediandr\\-a\\.akamaihd\\.net/progressive[^/]*?/)[^\"]+\\.mp4").getMatch(0);
            }
            if (baseHttp != null) {
                http_url_format = baseHttp + urlpart + ".%s.mp4";
            }
        } else if (new Regex(hlsMaster, pattern_ard).matches()) {
            urlpart = new Regex(hlsMaster, pattern_ard).getMatch(0);
            http_url_format = "https://pdodswr-a.akamaihd.net/" + urlpart + "%s.mp4";
        } else if (new Regex(hlsMaster, pattern_hr).matches()) {
            urlpart = new Regex(hlsMaster, pattern_hr).getMatch(0);
            http_url_format = "http://hrardmediathek-a.akamaihd.net/" + urlpart + "%skbit.mp4";
        } else {
            /* Unsupported URL */
            logger.info("Warning: Unsupported HLS pattern, cannot create HTTP URLs for: " + hlsMaster);
            return null;
        }
        return http_url_format;
    }

    private ArrayList<DownloadLink> crawlArdmediathekDeNew(final CryptedLink param) throws Exception {
        /* E.g. old classic.ardmediathek.de URLs */
        final boolean requiresOldContentIDHandling;
        String ardDocumentID = new Regex(param.getCryptedUrl(), "documentId=(\\d+)").getMatch(0);
        Map<String, Object> entries = null;
        final ArdMetadata metadata = new ArdMetadata();
        if (ardDocumentID != null) {
            requiresOldContentIDHandling = true;
            metadata.setTitle(ardDocumentID);
            metadata.setContentID(ardDocumentID);
        } else {
            final URL url = new URL(param.getCryptedUrl());
            requiresOldContentIDHandling = false;
            String ardBase64;
            final String pattern_player = ".+/player/([^/]+).*";
            if (param.getCryptedUrl().matches(pattern_player)) {
                /* E.g. URLs that are a little bit older */
                ardBase64 = new Regex(url.getPath(), pattern_player).getMatch(0);
            } else {
                /* New URLs */
                ardBase64 = new Regex(url.getPath(), "/([^/]+)/?$").getMatch(0);
            }
            if (ardBase64 == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (Encoding.isUrlCoded(ardBase64)) {
                ardBase64 = Encoding.urlDecode(ardBase64, true);
            }
            /* Check if we really have a base64 String otherwise we can abort right away */
            final String ardBase64Decoded = Encoding.Base64Decode(ardBase64);
            if (StringUtils.equals(ardBase64, ardBase64Decoded)) {
                logger.info("Unsupported URL (?)");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage("https://page.ardmediathek.de/page-gateway/pages/daserste/item/" + Encoding.urlEncode(ardBase64) + "?devicetype=pc&embedded=false");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ardDocumentID = PluginJSonUtils.getJson(br, "contentId");
            // final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("");
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "widgets/{0}/");
            final String broadcastedOn = (String) entries.get("broadcastedOn");
            final String title = (String) entries.get("title");
            final String showname = (String) JavaScriptEngineFactory.walkJson(entries, "show/title");
            final String type = (String) entries.get("type");
            if ("player_live".equalsIgnoreCase(type)) {
                logger.info("Cannot download livestreams");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (entries.get("blockedByFsk") == Boolean.TRUE) {
                /* AGE restricted content (can only be watched in the night) */
                throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "FSK_BLOCKED_" + title, "FSK_BLOCKED", null);
            } else if (StringUtils.isEmpty(broadcastedOn)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (StringUtils.isAllEmpty(title, showname)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String date_formatted = new Regex(broadcastedOn, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            if (date_formatted == null) {
                /* Fallback */
                date_formatted = broadcastedOn;
            }
            metadata.setTitle(title);
            metadata.setSubtitle(showname);
            metadata.setDateTimestamp(getDateMilliseconds(broadcastedOn));
            if (ardDocumentID != null) {
                metadata.setContentID(ardDocumentID);
            }
        }
        metadata.setChannel("ardmediathek");
        if (requiresOldContentIDHandling) {
            if (StringUtils.isEmpty(ardDocumentID)) {
                /* Probably offline content */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* 2020-05-26: Also possible: http://page.ardmediathek.de/page-gateway/playerconfig/<documentID> */
            /* Old way: http://www.ardmediathek.de/play/media/%s?devicetype=pc&features=flash */
            br.getPage(String.format("http://page.ardmediathek.de/page-gateway/mediacollection/%s?devicetype=pc", ardDocumentID));
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "mediaCollection/embedded");
        } else {
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        }
        final HashMap<String, DownloadLink> foundQualities = crawlARDJson(param, metadata, entries);
        return this.handleUserQualitySelection(foundQualities);
    }

    private ArrayList<DownloadLink> crawlMediathek(final CryptedLink param) throws Exception {
        /* TODO: 2022-01-21: Maybe split this up into multiple methods because at this moment it's still quite chaotic. */
        String contentID = null;
        Browser brHTML = new Browser();
        /**
         * Look for embedded content which will go back into this crawler. Especially needed for: wdr.de, wdrmaus.de, sportschau.de,
         * sandmann.de
         */
        br.getPage(param.getCryptedUrl());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        brHTML = br.cloneBrowser();
        final ArrayList<DownloadLink> embeds = crawlEmbeddedContent(this.br);
        if (!embeds.isEmpty()) {
            return embeds;
        }
        String url_json = null;
        if (this.getHost().equals("sportschau.de")) {
            /* Special handling: Embedded videoplayer --> "ardjson" URL will be inside that html */
            final String embedPlayerURL = br.getRegex("allowFullScreen\\s*src=\"(/[^\"]+)\"").getMatch(0);
            /* This step is optional in case the user directly adds an embedded URL. */
            if (embedPlayerURL != null && embedPlayerURL.contains("-ardplayer")) {
                url_json = embedPlayerURL.replace("-ardplayer", "-ardjson");
            } else if (br.getURL().contains("-ardplayer")) {
                url_json = br.getURL().replace("-ardplayer", "-ardjson");
            } else if (br.getURL().contains("-ardjson")) {
                /* URL has already been accessed. */
                url_json = br.getURL();
            }
        } else {
            if (this.getHost().equalsIgnoreCase("sandmann.de")) {
                url_json = br.getRegex("data\\-media\\-ref=\"([^\"]*?\\.jsn)[^\"]*?\"").getMatch(0);
                if (!StringUtils.isEmpty(url_json)) {
                    if (url_json.startsWith("/")) {
                        url_json = "https://www.sandmann.de" + url_json;
                    }
                    /* This is a very ugly contentID */
                    contentID = new Regex(url_json, "sandmann\\.de/(.+)").getMatch(0);
                }
            } else if (this.getHost().contains("ndr.de") || this.getHost().equalsIgnoreCase("eurovision.de")) {
                /* E.g. daserste.ndr.de, blabla.ndr.de */
                contentID = br.getRegex("([A-Za-z0-9]+\\d+)\\-(?:ard)?player_[^\"]+\"").getMatch(0);
                if (!StringUtils.isEmpty(contentID)) {
                    url_json = String.format("https://www.ndr.de/%s-ardjson.json", contentID);
                }
            }
        }
        String title = null;
        String show = null;
        String channel = null;
        /* E.g. wdr.de, Tags: schema.org */
        final String jsonSchemaOrg = brHTML.getRegex("<script[^>]*?type=\"application/ld\\+json\"[^>]*?>(.*?)</script>").getMatch(0);
        /* These RegExes should be compatible with all websites */
        /* Date is already provided in the format we need. */
        String date = brHTML.getRegex("<meta property=\"video:release_date\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2})[^\"]*?\"[^>]*?/?>").getMatch(0);
        if (date == null) {
            date = brHTML.getRegex("<span itemprop=\"datePublished\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2})[^\"]*?\"[^>]*?/?>").getMatch(0);
        }
        String description = brHTML.getRegex("<meta property=\"og:description\" content=\"([^\"]+)\"").getMatch(0);
        final String host = getHost();
        if (url_json != null) {
            // br.getPage(url_json);
            param.setCryptedUrl(url_json);
            return this.crawlArdMediaObject(param);
        } else if (jsonSchemaOrg != null) {
            /* 2018-02-15: E.g. daserste.de, wdr.de */
            final String headline = brHTML.getRegex("<h3 class=\"headline\">([^<>]+)</h3>").getMatch(0);
            try {
                Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(jsonSchemaOrg);
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
                if (StringUtils.isEmpty(date) && !StringUtils.isEmpty(uploadDate)) {
                    /* Fallback */
                    date = new Regex(uploadDate, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
                }
                /* Find more data */
                entries = (Map<String, Object>) entries.get("productionCompany");
                if (entries != null) {
                    channel = (String) entries.get("name");
                }
            } catch (final Throwable e) {
            }
            if (StringUtils.isEmpty(title) && headline != null) {
                /* 2018-04-11: ardmediathek.de */
                title = headline;
            }
        } else if (host.equalsIgnoreCase("ndr.de") || host.equalsIgnoreCase("eurovision.de")) {
            /* ndr.de */
            if (brHTML.getURL().contains("daserste.ndr.de") && StringUtils.isEmpty(date)) {
                date = brHTML.getRegex("<p>Dieses Thema im Programm:</p>\\s*?<h2>[^<>]*?(\\d{2}\\.\\d{2}\\.\\d{4})[^<>]*?</h2>").getMatch(0);
            }
            title = brHTML.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\"/>").getMatch(0);
            if (StringUtils.isEmpty(date)) {
                /* Last chance */
                date = PluginJSonUtils.getJson(br, "assetid");
                if (!StringUtils.isEmpty(date)) {
                    date = new Regex(date, "TV\\-(\\d{8})").getMatch(0);
                }
            }
        }
        if (StringUtils.isEmpty(title)) {
            /* This should never happen */
            title = "UnknownTitle_" + UniqueAlltimeID.create();
        }
        title = Encoding.htmlDecode(title).trim();
        title = encodeUnicode(title);
        final ArdMetadata metadata = new ArdMetadata(title);
        metadata.setSubtitle(show);
        if (date != null) {
            metadata.setDateTimestamp(getDateMilliseconds(date));
        }
        if (!StringUtils.isEmpty(channel)) {
            metadata.setChannel(channel);
        } else {
            /* Fallback */
            channel = host.substring(0, host.lastIndexOf(".")).replace(".", "_");
        }
        if (contentID != null) {
            metadata.setContentID(contentID);
        }
        Object entries = null;
        try {
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        } catch (final Throwable ignore) {
        }
        final HashMap<String, DownloadLink> foundQualitiesMap = crawlARDJson(param, metadata, entries);
        return this.handleUserQualitySelection(foundQualitiesMap);
    }

    private ArrayList<DownloadLink> crawlWdrMediathek(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> embeds = crawlEmbeddedContent(this.br);
        if (!embeds.isEmpty()) {
            return embeds;
        }
        /* TODO: Allow for crawling multiple of those */
        final String jsonInHTML = br.getRegex("globalObject\\.gseaInlineMediaData\\[\"[^\"]+\"\\] =\\s*(\\{.*?\\});\\s*</script>").getMatch(0);
        if (jsonInHTML != null) {
            /* E.g. https://www1.wdr.de/orchester-und-chor/wdrmusikvermittlung/videos/video-wdr-dackl-jazz-konzert-100.html */
            final Map<String, Object> root = JSonStorage.restoreFromString(jsonInHTML, TypeRef.HASHMAP);
            return crawlWdrMediaObject(param, root);
        } else {
            final String urlJson = this.br.getRegex(".mediaObj.\\s*:\\s*\\{\\s*.url.\\s*:\\s*.(https?://[^<>\"\\']+)").getMatch(0);
            if (urlJson == null) {
                /* Assume that content is offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return crawlWdrMediathekEmbedded(param, urlJson);
        }
    }

    private ArrayList<DownloadLink> crawlWdrMediathekEmbedded(final CryptedLink param, final String url) throws Exception {
        br.getPage(url);
        final String json = br.getRegex("\\$mediaObject\\.jsonpHelper\\.storeAndPlay\\((\\{.*?\\})\\);").getMatch(0);
        if (json == null) {
            /* Assume that content is offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
        return crawlWdrMediaObject(param, root);
    }

    private ArrayList<DownloadLink> crawlArdMediaObject(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> _info = (Map<String, Object>) root.get("_info");
        final String date = (String) _info.get("clipDate");
        final String description = (String) _info.get("clipDescription");
        final ArdMetadata metadata = new ArdMetadata(_info.get("clipTitle").toString());
        metadata.setSubtitle(_info.get("seriesTitle").toString());
        if (date != null) {
            metadata.setDateTimestamp(getDateMilliseconds(date));
        }
        metadata.setChannel(_info.get("channelTitle").toString());
        if (!StringUtils.isEmpty(description)) {
            metadata.setDescription(description);
        }
        /* Fallback as they do not provide contentIDs... */
        metadata.setContentID(br.getURL());
        final HashMap<String, DownloadLink> foundQualitiesMap = crawlARDJson(param, metadata, root);
        return this.handleUserQualitySelection(foundQualitiesMap);
    }

    private ArrayList<DownloadLink> crawlWdrMediaObject(final CryptedLink param, final Map<String, Object> wdrMediaObject) throws Exception {
        final Map<String, Object> trackerData = (Map<String, Object>) wdrMediaObject.get("trackerData");
        final String date = (String) trackerData.get("trackerClipAirTime");
        final ArdMetadata metadata = new ArdMetadata(trackerData.get("trackerClipTitle").toString());
        metadata.setSubtitle(trackerData.get("trackerClipSubcategory").toString());
        if (date != null) {
            metadata.setDateTimestamp(getDateMilliseconds(date));
        }
        metadata.setChannel(trackerData.get("trackerClipCategory").toString());
        metadata.setContentID(trackerData.get("trackerClipId").toString());
        final HashMap<String, DownloadLink> foundQualitiesMap = crawlARDJson(param, metadata, wdrMediaObject);
        return this.handleUserQualitySelection(foundQualitiesMap);
    }

    private ArrayList<DownloadLink> crawlEmbeddedContent(final Browser br) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] embeddedVideosTypeOldJson = br.getRegex("(?:\\'|\")mediaObj(?:\\'|\"):\\s*?\\{\\s*?(?:\\'|\")url(?:\\'|\"):\\s*?(?:\\'|\")(https?://[^<>\"]+\\.js)(?:\\'|\")").getColumn(0);
        for (final String embeddedVideo : embeddedVideosTypeOldJson) {
            ret.add(this.createDownloadlink(embeddedVideo));
        }
        return ret;
    }

    private HashMap<String, DownloadLink> crawlARDJson(final CryptedLink param, final ArdMetadata metadata, final Object mediaCollection) throws Exception {
        /* We know how their http urls look - this way we can avoid HDS/HLS/RTMP */
        /*
         * http://adaptiv.wdr.de/z/medp/ww/fsk0/104/1046579/,1046579_11834667,1046579_11834665,1046579_11834669,.mp4.csmil/manifest.f4
         */
        // //wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/139/1394333/,1394333_16295554,1394333_16295556,1394333_16295555,1394333_16295557,1394333_16295553,1394333_16295558,.mp4.csmil/master.m3u8
        /*
         * Grab all http qualities inside json
         */
        subtitleLink = getJsonSubtitleURL(this.br);
        final HashMap<String, DownloadLink> foundQualitiesMap = new HashMap<String, DownloadLink>();
        final List<String> httpStreamsQualityIdentifiers = new ArrayList<String>();
        /* For http stream quality identifiers which have been created by the hls --> http URLs converter */
        final List<String> httpStreamsQualityIdentifiers_2_over_hls_master = new ArrayList<String>();
        Map<String, Object> map;
        String exampleHTTPURL = null;
        String hlsMaster = null;
        if (mediaCollection instanceof Map && ((Map<String, Object>) mediaCollection).containsKey("mediaResource")) {
            /* E.g. older wdr.de json --> Only extract hls-master, then generate http URLs down below */
            final Map<String, Object> mediaResource = (Map<String, Object>) ((Map<String, Object>) mediaCollection).get("mediaResource");
            /* All of these are usually HLS e.g. present for wdr.de */
            final String[] mediaNames = new String[] { "dflt", "alt" };
            for (final String mediaType : mediaNames) {
                if (mediaResource.containsKey(mediaType)) {
                    final Map<String, Object> media = (Map<String, Object>) mediaResource.get(mediaType);
                    final String hlsMasterTmp = (String) media.get("videoURL");
                    if (media.get("mediaFormat").toString().equalsIgnoreCase("hls") && !StringUtils.isEmpty(hlsMasterTmp)) {
                        hlsMaster = hlsMasterTmp;
                        break;
                    }
                }
            }
        } else {
            if (mediaCollection instanceof Map) {
                map = (Map<String, Object>) mediaCollection;
                if (!map.containsKey("_mediaArray")) {
                    /* 2020-06-08: For new ARD URLs */
                    map = (Map<String, Object>) JavaScriptEngineFactory.walkJson(map, "widgets/{0}/mediaCollection/embedded");
                }
            } else {
                map = null;
            }
            if (map != null && map.containsKey("_mediaArray")) {
                try {
                    final List<Map<String, Object>> mediaArray = (List<Map<String, Object>>) map.get("_mediaArray");
                    mediaArray: for (Map<String, Object> media : mediaArray) {
                        final List<Map<String, Object>> mediaStreamArray = (List<Map<String, Object>>) media.get("_mediaStreamArray");
                        for (int mediaStreamIndex = 0; mediaStreamIndex < mediaStreamArray.size(); mediaStreamIndex++) {
                            // list is sorted from best to lowest quality, first one is m3u8
                            final Map<String, Object> mediaStream = mediaStreamArray.get(mediaStreamIndex);
                            final Object _stream = mediaStream.get("_stream");
                            final int quality;
                            final Object qualityO = mediaStream.get("_quality");
                            if (qualityO instanceof Number || (qualityO != null && qualityO.toString().matches("\\d+"))) {
                                quality = Integer.parseInt(qualityO.toString());
                            } else {
                                /* E.g. skip quality "auto" (HLS) */
                                if (_stream instanceof String) {
                                    final String url = _stream.toString();
                                    if (url.contains(".m3u8")) {
                                        hlsMaster = url;
                                    }
                                }
                                continue;
                            }
                            final List<String> streams;
                            if (_stream instanceof String) {
                                streams = new ArrayList<String>();
                                streams.add((String) _stream);
                            } else {
                                streams = ((List<String>) _stream);
                            }
                            for (int index = 0; index < streams.size(); index++) {
                                final String stream = streams.get(index);
                                if (stream == null || !StringUtils.endsWithCaseInsensitive(stream, ".mp4")) {
                                    /* Skip invalid objects */
                                    continue;
                                }
                                final String url = br.getURL(stream).toString();
                                if (exampleHTTPURL == null) {
                                    exampleHTTPURL = url;
                                }
                                VideoResolution resolution = null;
                                /*
                                 * Sometimes the resolutions is given, sometimes we have to assume it and sometimes (e.g. HLS streaming)
                                 * there are multiple qualities available for one stream URL.
                                 */
                                final Object widthO = mediaStream.get("_width");
                                final Object heightO = mediaStream.get("_height");
                                if (widthO != null && heightO != null) {
                                    resolution = VideoResolution.getByWidth(Integer.parseInt(widthO.toString()));
                                    if (resolution == null) {
                                        logger.warning("Unknown width: " + widthO);
                                    }
                                }
                                if (resolution == null) {
                                    resolution = VideoResolution.getByURL(url);
                                }
                                if (resolution == null) {
                                    /* Old handling! Do not trust this! */
                                    if (quality == 0 && streams.size() == 1) {
                                        resolution = VideoResolution.P_180;
                                    } else if (quality == 1 && streams.size() == 1) {
                                        resolution = VideoResolution.P_288;
                                    } else if (quality == 1 && streams.size() == 2) {
                                        switch (index) {
                                        case 0:
                                            resolution = VideoResolution.P_288;
                                            break;
                                        case 1:
                                        default:
                                            resolution = VideoResolution.P_270;
                                            break;
                                        }
                                    } else if (quality == 2 && streams.size() == 1) {
                                        resolution = VideoResolution.P_540;
                                    } else if (quality == 2 && streams.size() == 2) {
                                        switch (index) {
                                        case 0:
                                            resolution = VideoResolution.P_360;
                                            break;
                                        case 1:
                                        default:
                                            resolution = VideoResolution.P_540;
                                            break;
                                        }
                                    } else if (quality == 3 && streams.size() == 1) {
                                        resolution = VideoResolution.P_540;
                                    } else if (quality == 3 && streams.size() == 2) {
                                        switch (index) {
                                        case 0:
                                            resolution = VideoResolution.P_720;
                                            break;
                                        case 1:
                                        default:
                                            resolution = VideoResolution.P_540;
                                            break;
                                        }
                                    }
                                }
                                if (resolution == null) {
                                    logger.warning("Skipping unknown resolution for URL: " + url);
                                    continue;
                                }
                                final DownloadLink download = addQuality(param, metadata, foundQualitiesMap, url, null, 0, resolution, false);
                                if (download != null) {
                                    httpStreamsQualityIdentifiers.add(getQualityIdentifier(url, 0, resolution));
                                    if (cfg.isGrabBESTEnabled()) {
                                        // we iterate mediaStreamArray from best to lowest
                                        // TODO: optimize for cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled()
                                        break mediaArray;
                                    }
                                }
                            }
                        }
                    }
                } catch (final Throwable ignore) {
                    logger.log(ignore);
                }
            }
        }
        /*
         * TODO: It might only make sense to attempt this if we found more than 3 http qualities previously because usually 3 means we will
         * also only have 3 hls qualities --> There are no additional http qualities!
         */
        // hlsMaster =
        // "https://wdradaptiv-vh.akamaihd.net/i/medp/ondemand/weltweit/fsk0/232/2326527/,2326527_32403893,2326527_32403894,2326527_32403895,2326527_32403891,2326527_32403896,2326527_32403892,.mp4.csmil/master.m3u8";
        String http_url_audio = br.getRegex("((?:https?:)?//[^<>\"]+\\.mp3)\"").getMatch(0);
        final String quality_string = new Regex(hlsMaster, ".*?/i/.*?,([A-Za-z0-9_,\\-\\.]+),?\\.mp4\\.csmil.*?").getMatch(0);
        if (StringUtils.isEmpty(hlsMaster) && http_url_audio == null && httpStreamsQualityIdentifiers.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /*
         * This is a completely different attempt to find HTTP URLs. As long as it works, this may be more reliable than everything above
         * here!
         */
        final boolean tryToFindAdditionalHTTPURLs = true;
        if (tryToFindAdditionalHTTPURLs && hlsMaster != null) {
            final HashMap<String, DownloadLink> foundQualitiesMap_http_urls_via_HLS_master = new HashMap<String, DownloadLink>();
            final String http_url_format = getHlsToHttpURLFormat(hlsMaster, exampleHTTPURL);
            final String[] qualities_hls = quality_string != null ? quality_string.split(",") : null;
            if (http_url_format != null && qualities_hls != null && qualities_hls.length > 0) {
                /* Access HLS master to find correct resolution for each ID (the only possible way) */
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(br.getURL(hlsMaster).toString());
                    if (con.getURL().toString().contains("/static/geoblocking.mp4")) {
                        if (httpStreamsQualityIdentifiers.size() == 0) {
                            this.errorGEOBlocked(param);
                        }
                    } else {
                        br.followConnection(true);
                    }
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
                final String[] resolutionsInOrder = br.getRegex("RESOLUTION=(\\d+x\\d+)").getColumn(0);
                if (resolutionsInOrder != null) {
                    logger.info("Crawling additional http urls");
                    for (int counter = 0; counter <= qualities_hls.length - 1; counter++) {
                        if (counter > qualities_hls.length - 1 || counter > resolutionsInOrder.length - 1) {
                            break;
                        }
                        final String quality_id = qualities_hls[counter];
                        final String final_url = String.format(http_url_format, quality_id);
                        final String resolutionStr = resolutionsInOrder[counter];
                        final String[] height_width = resolutionStr.split("x");
                        final int width = Integer.parseInt(height_width[0]);
                        // final int height = Integer.parseInt(height_width[1]);
                        final VideoResolution resolution = VideoResolution.getByWidth(width);
                        if (resolution == null) {
                            logger.warning("Skipping unsupported width: " + width);
                            continue;
                        }
                        final String qualityIdentifier = getQualityIdentifier(final_url, 0, resolution);
                        if (!httpStreamsQualityIdentifiers_2_over_hls_master.contains(qualityIdentifier)) {
                            logger.info("Found (additional) http quality via HLS Master: " + qualityIdentifier);
                            addQuality(param, metadata, foundQualitiesMap_http_urls_via_HLS_master, final_url, null, 0, resolution, false);
                            httpStreamsQualityIdentifiers_2_over_hls_master.add(qualityIdentifier);
                        }
                    }
                }
            }
            /*
             * Decide whether we want to use the existing http URLs or whether we want to prefer the ones we've generated out of their HLS
             * URLs.
             */
            final int numberof_http_qualities_found_inside_json = foundQualitiesMap.keySet().size();
            final int numberof_http_qualities_found_via_hls_to_http_conversion = foundQualitiesMap_http_urls_via_HLS_master.keySet().size();
            if (numberof_http_qualities_found_via_hls_to_http_conversion > numberof_http_qualities_found_inside_json) {
                /*
                 * 2019-04-15: Prefer URLs created via this way because if we don't, we may get entries labled as different qualities which
                 * may be duplicates!
                 */
                logger.info(String.format("Found [%d] qualities via HLS --> HTTP conversion which is more than number of http URLs inside json [%d]", numberof_http_qualities_found_via_hls_to_http_conversion, numberof_http_qualities_found_inside_json));
                logger.info("--> Using converted URLs instead");
                foundQualitiesMap.clear();
                foundQualitiesMap.putAll(foundQualitiesMap_http_urls_via_HLS_master);
            }
        }
        if (hlsMaster != null) {
            addHLS(param, metadata, foundQualitiesMap, br, hlsMaster, false);
        }
        if (http_url_audio != null) {
            if (http_url_audio.startsWith("//")) {
                /* 2019-04-11: Workaround for missing protocol */
                http_url_audio = "https:" + http_url_audio;
            }
            addQuality(param, metadata, foundQualitiesMap, http_url_audio, null, 0, null, false);
        }
        return foundQualitiesMap;
    }

    /**
     * Handling for older ARD websites. </br>
     * INFORMATION: network = akamai or limelight == RTMP
     */
    private ArrayList<DownloadLink> crawlDasersteVideo(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        String url_xml = null;
        String contentID = null;
        if (this.getHost().equalsIgnoreCase("daserste.de")) {
            /* The fast way - we do not even have to access the main URL which the user has added :) */
            final String[] playerConfigs = br.getRegex("data-ctrl-player=\"(\\{[^\"]+)\"").getColumn(0);
            if (playerConfigs.length == 0) {
                /* Look for URLs going to playable content */
                final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                final String[] urlsToPlayableContent = br.getRegex("class=\"av-playerContainer notDownloadable\">\\s*<a href=\"(/[^\"]+)\"").getColumn(0);
                for (final String urlToPlayableContent : urlsToPlayableContent) {
                    ret.add(this.createDownloadlink(br.getURL(urlToPlayableContent).toString()));
                }
                return ret;
            } else {
                /* Also possible: ~PlayerJson.json --> But this contains much less information! */
                url_xml = param.getCryptedUrl().replace(".html", "~playerXml.xml");
            }
        } else if (param.getCryptedUrl().matches(".+mdr\\.de/.+/((?:video|audio)\\-\\d+)\\.html")) {
            /* Some special mdr.de URLs --> We do not have to access main URL so this way we can speed up the crawl process a bit :) */
            contentID = new Regex(param.getCryptedUrl(), "((?:audio|video)\\-\\d+)\\.html$").getMatch(0);
            url_xml = String.format("https://www.mdr.de/mediathek/mdr-videos/d/%s-avCustom.xml", contentID);
        } else {
            /* E.g. kika.de, sputnik.de, mdr.de */
            br.getPage(param.getCryptedUrl());
            if (isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            url_xml = br.getRegex("\\'((?:https?://|(?:\\\\)?/)[^<>\"]+\\-avCustom\\.xml)\\'").getMatch(0);
            if (!StringUtils.isEmpty(url_xml)) {
                if (url_xml.contains("\\")) {
                    url_xml = url_xml.replace("\\", "");
                }
                contentID = new Regex(url_xml, "((?:audio|video)\\-\\d+)").getMatch(0);
            }
        }
        if (url_xml == null) {
            /* Probably no downloadable content available */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final HashMap<String, DownloadLink> foundQualitiesMap = new HashMap<String, DownloadLink>();
        br.getPage(url_xml);
        /* Usually daserste.de as there is no way to find a contentID inside URL added by the user. */
        final String id = br.getRegex("<c7>(.*?)</c7>").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getHttpConnection().getContentType().contains("xml")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.subtitleLink = getXMLSubtitleURL(this.br);
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
        String title = getXML(br.toString(), "shareTitle");
        if (StringUtils.isEmpty(title)) {
            title = getXML(br.toString(), "broadcastName");
        }
        if (StringUtils.isEmpty(title)) {
            /* E.g. sputnik.de */
            title = getXML(br.toString(), "headline");
        }
        if (StringUtils.isEmpty(title)) {
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        final ArdMetadata metadata = new ArdMetadata(title);
        if (show != null) {
            metadata.setSubtitle(show);
        }
        if (date != null) {
            metadata.setDateTimestamp(getDateMilliseconds(date));
        }
        if (id != null) {
            metadata.setContentID(Hash.getSHA1(id));
        }
        final ArrayList<String> hls_master_dupelist = new ArrayList<String>();
        final String assetsAudiodescription = br.getRegex("<assets type=\"audiodesc\">(.*?)</assets>").getMatch(0);
        final String assetsNormal = br.getRegex("<assets>(.*?)</assets>").getMatch(0);
        boolean isAudioDescription = false;
        String[] mediaStreamArray = null;
        if (this.cfg.isPreferAudioDescription() && assetsAudiodescription != null) {
            logger.info("Crawling asset-type audiodescription");
            isAudioDescription = true;
            mediaStreamArray = new Regex(assetsAudiodescription, "(<asset.*?</asset>)").getColumn(0);
        }
        if (mediaStreamArray == null || mediaStreamArray.length == 0) {
            logger.info("Crawling asset-type normal");
            isAudioDescription = false;
            mediaStreamArray = new Regex(assetsNormal, "(<asset.*?</asset>)").getColumn(0);
        }
        if (mediaStreamArray.length == 0) {
            /* 2021-05-10: Only check for this if no downloadurls are available! */
            final String fskRating = this.br.getRegex("<fskRating>fsk(\\d+)</fskRating>").getMatch(0);
            if (fskRating != null && Short.parseShort(fskRating) >= 12) {
                /* Video is age restricted --> Only available from >=8PM. */
                final String filenameURL = Plugin.getFileNameFromURL(new URL(param.getCryptedUrl()));
                if (filenameURL != null) {
                    throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, "FSK_BLOCKED_" + filenameURL, "FSK_BLOCKED", null);
                } else {
                    throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, "FSK_BLOCKED", "FSK_BLOCKED", null);
                }
            }
        }
        for (final String stream : mediaStreamArray) {
            final String streamType = getXML(stream, "streamLabel");
            if (StringUtils.equalsIgnoreCase(streamType, "DASH-Streaming")) {
                /*
                 * 2021-11-18: Usually DASH comes with separated video/audio and also HLS should be available too --> Skip DASH </br> Seen
                 * for: mdr.de livestreams e.g. https://www.mdr.de/video/livestreams/mdr-plus/sport-eventlivestreamzweiww-328.html
                 */
                logger.info("Skipping DASH stream");
                continue;
            }
            /* E.g. kika.de */
            final String hls_master;
            String http_url = getXML(stream, "progressiveDownloadUrl");
            if (StringUtils.isEmpty(http_url)) {
                /* E.g. daserste.de */
                http_url = getXML(stream, "fileName");
                if (StringUtils.isEmpty(http_url)) {
                    /* hls master fallback, eg livestreams */
                    http_url = getXML(stream, "adaptiveHttpStreamingRedirectorUrl");
                }
            }
            /* E.g. daserste.de */
            String filesizeStr = getXML(stream, "size");
            if (StringUtils.isEmpty(filesizeStr)) {
                /* E.g. kika.de */
                filesizeStr = getXML(stream, "fileSize");
            }
            final String bitrate_video = getXML(stream, "bitrateVideo");
            final String bitrate_audio = getXML(stream, "bitrateAudio");
            final String width_str = getXML(stream, "frameWidth");
            final String height_str = getXML(stream, "frameHeight");
            /* This sometimes contains resolution: e.g. <profileName>Video 2018 | MP4 720p25 | Web XL| 16:9 | 1280x720</profileName> */
            final String profileName = getXML(stream, "profileName");
            final String resolutionInProfileName = new Regex(profileName, "(\\d+x\\d+)").getMatch(0);
            int width = -1;
            int height = -1;
            if (width_str != null && width_str.matches("\\d+")) {
                width = Integer.parseInt(width_str);
            }
            if (height_str != null && height_str.matches("\\d+")) {
                height = Integer.parseInt(height_str);
            }
            if (width == 0 && height == 0 && resolutionInProfileName != null) {
                final String[] resInfo = resolutionInProfileName.split("x");
                width = Integer.parseInt(resInfo[0]);
                height = Integer.parseInt(resInfo[1]);
            }
            if (StringUtils.isEmpty(http_url) || isUnsupportedProtocolDasersteVideo(http_url)) {
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
                addHLS(param, metadata, foundQualitiesMap, this.br, hls_master, isAudioDescription);
                hls_master_dupelist.add(hls_master);
            }
            if (!StringUtils.isEmpty(http_url)) {
                /* http */
                long bitrate;
                final String bitrateFromURLStr = new Regex(http_url, "(\\d+)k").getMatch(0);
                if (!StringUtils.isEmpty(bitrate_video) && !StringUtils.isEmpty(bitrate_audio)) {
                    bitrate = Long.parseLong(bitrate_video) + Long.parseLong(bitrate_audio);
                    if (bitrate < 10000) {
                        bitrate = bitrate * 1000;
                    }
                } else if (bitrateFromURLStr != null) {
                    bitrate = Long.parseLong(bitrateFromURLStr);
                } else {
                    bitrate = 0;
                }
                /* Find video resolution */
                VideoResolution resolution = VideoResolution.getByWidth(width);
                if (resolution == null) {
                    resolution = VideoResolution.getByHeight(height);
                }
                if (resolution == null) {
                    resolution = VideoResolution.getByURL(http_url);
                }
                if (resolution == null) {
                    /* Skip unsupported resolutions */
                    logger.warning("Found unsupported resolution for URL: " + http_url);
                    continue;
                } else {
                    addQuality(param, metadata, foundQualitiesMap, http_url, filesizeStr, bitrate, resolution, isAudioDescription);
                }
            }
        }
        return this.handleUserQualitySelection(foundQualitiesMap);
    }

    private ArrayList<DownloadLink> crawlTagesschauVideos(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Crawl all embedded items on this page */
        final String[] embedDatas = br.getRegex("data-ts_component='ts-mediaplayer'\\s*data-config='([^\\']+)'").getColumn(0);
        for (final String embedData : embedDatas) {
            final String embedJson = Encoding.htmlDecode(embedData);
            final Map<String, Object> root = JSonStorage.restoreFromString(embedJson, TypeRef.HASHMAP);
            final Map<String, Object> mc = (Map<String, Object>) root.get("mc");
            final String _type = (String) mc.get("_type");
            if (_type == null || !_type.equalsIgnoreCase("video")) {
                /* Skip unsupported items */
                continue;
            }
            /* Collect metadata */
            try {
                final String contentID = (String) JavaScriptEngineFactory.walkJson(root, "pc/_pixelConfig/{0}/clipData/assetid");
                final ArdMetadata metadata = new ArdMetadata(mc.get("_title").toString());
                final Map<String, Object> _info = (Map<String, Object>) mc.get("_info");
                final Map<String, Object> _download = (Map<String, Object>) mc.get("_download");
                if (_info != null) {
                    final String clipDate = _info.get("clipDate").toString();
                    final long timestamp = TimeFormatter.getMilliSeconds(clipDate, "dd.MM.yyyy HH:mm", Locale.GERMANY);
                    metadata.setDateTimestamp(timestamp);
                    metadata.setChannel(_info.get("channelTitle").toString());
                }
                if (_download != null) {
                    /* Kind of our fallback as '_info' doesn't always exist */
                    final String dateStr = (String) _download.get("date");
                    if (metadata.getChannel() == null) {
                        metadata.setChannel(_download.get("channel").toString());
                    }
                    if (metadata.getDateTimestamp() == -1 && !StringUtils.isEmpty(dateStr)) {
                        final long timestamp = TimeFormatter.getMilliSeconds(dateStr, "EEE MMM dd HH:mm:ss ZZZ yyyy", Locale.ENGLISH);
                        metadata.setDateTimestamp(timestamp);
                    }
                }
                if (contentID != null) {
                    metadata.setContentID(contentID);
                }
                final HashMap<String, DownloadLink> foundQualitiesMap = this.crawlARDJson(param, metadata, mc);
                final ArrayList<DownloadLink> result = this.handleUserQualitySelection(foundQualitiesMap);
                /* Make sure user gets results right away. */
                for (final DownloadLink link : result) {
                    this.distribute(link);
                }
                ret.addAll(result);
                if (this.isAbort()) {
                    /* Abort by user */
                    break;
                }
            } catch (final Throwable ignore) {
                logger.warning("WTF");
            }
        }
        return ret;
    }

    private void addHLS(final CryptedLink param, final ArdMetadata metadata, final HashMap<String, DownloadLink> foundQualities, final Browser br, final String hlsMaster, final boolean isAudioDescription) throws Exception {
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
            hlsBR.getPage(hlsMaster);
        }
        /* Get all HLS qualities */
        final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(hlsBR);
        final HashMap<Integer, List<HlsContainer>> dupeMap = new HashMap<Integer, List<HlsContainer>>();
        for (final HlsContainer hlscontainer : allHlsContainers) {
            if (!dupeMap.containsKey(hlscontainer.getWidth())) {
                dupeMap.put(hlscontainer.getWidth(), new ArrayList<HlsContainer>());
            }
            dupeMap.get(hlscontainer.getWidth()).add(hlscontainer);
        }
        /* Collect highest bitrate version for each resolution */
        final List<HlsContainer> allHlsContainersWithoutWidthDupes = new ArrayList<HlsContainer>();
        for (final List<HlsContainer> hlscontainers : dupeMap.values()) {
            if (hlscontainers.size() == 1) {
                allHlsContainersWithoutWidthDupes.add(hlscontainers.get(0));
            } else {
                logger.info("Found multiple bitrates for heigth: " + hlscontainers.get(0).getHeight());
                int maxBandwidth = -1;
                HlsContainer highestBitrateCandidate = null;
                for (final HlsContainer hlscontainer : hlscontainers) {
                    if (hlscontainer.getBandwidth() > maxBandwidth) {
                        highestBitrateCandidate = hlscontainer;
                        maxBandwidth = hlscontainer.getBandwidth();
                    }
                }
                if (highestBitrateCandidate == null) {
                    /* This should never happen */
                    logger.warning("WTF m3u8 file broken for heigth " + hlscontainers.get(0).getHeight() + "??");
                } else {
                    allHlsContainersWithoutWidthDupes.add(highestBitrateCandidate);
                }
            }
        }
        for (final HlsContainer hlscontainer : allHlsContainersWithoutWidthDupes) {
            if (hlscontainer.isVideo()) {
                final String final_download_url = hlscontainer.getDownloadurl();
                final VideoResolution resolution = VideoResolution.getByWidth(hlscontainer.getWidth());
                if (resolution == null) {
                    logger.warning("Skipping unknown width: " + hlscontainer.getWidth());
                } else {
                    addQuality(param, metadata, foundQualities, final_download_url, null, hlscontainer.getBandwidth(), resolution, isAudioDescription);
                }
            }
        }
    }

    private DownloadLink addQuality(final CryptedLink param, final ArdMetadata metadata, final HashMap<String, DownloadLink> qualitiesMap, final String directurl, final String filesize_str, long bitrate, final VideoResolution resolution, final boolean isAudioDescription) {
        /* Errorhandling */
        final String ext;
        if (directurl == null) {
            /* Skip items with bad data. */
            return null;
        } else if (directurl.contains(".mp3")) {
            ext = "mp3";
        } else {
            ext = "mp4";
        }
        long filesize = -1;
        boolean setVerifiedFilesize = false;
        if (filesize_str != null && filesize_str.matches("\\d+")) {
            filesize = Long.parseLong(filesize_str);
        }
        /* Use real resolution inside filenames */
        final String protocol;
        if (directurl.contains("m3u8")) {
            protocol = "hls";
        } else {
            protocol = "http";
            /* Only grab filesize if we need it for BEST-comparison later. */
            if (filesize == -1 && (cfg.isGrabBESTEnabled() || cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled())) {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                URLConnectionAdapter con = null;
                try {
                    con = brc.openHeadConnection(directurl);
                    if (!jd.plugins.hoster.ARDMediathek.isVideoContent(con)) {
                        brc.followConnection(true);
                        return null;
                    } else {
                        brc.followConnection(true);
                        if (con.getCompleteContentLength() > 0) {
                            filesize = con.getCompleteContentLength();
                            setVerifiedFilesize = true;
                        }
                    }
                } catch (IOException e) {
                    logger.log(e);
                    return null;
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
            }
        }
        final String qualityStringForQualitySelection = getQualityIdentifier(directurl, bitrate, resolution);
        final DownloadLink link = createDownloadlink(directurl.replaceAll("https?://", getHost() + "decrypted://"));
        final MediathekProperties data = link.bindData(MediathekProperties.class);
        data.setTitle(metadata.getTitle());
        data.setSourceHost(getHost());
        data.setChannel(metadata.getChannel());
        if (resolution != null) {
            data.setResolution(resolution.toString());
        }
        data.setBitrateTotal(bitrate);
        data.setProtocol(protocol);
        data.setFileExtension(ext);
        data.setAudioDescription(isAudioDescription);
        if (metadata.getDateTimestamp() > -1) {
            data.setReleaseDate(metadata.getDateTimestamp());
        }
        data.setShow(metadata.getSubtitle());
        link.setFinalFileName(MediathekHelper.getMediathekFilename(link, data, true, false));
        link.setContentUrl(param.getCryptedUrl());
        if (metadata.getContentID() == null) {
            /* ContentID should always be available! */
            logger.log(new Exception("FixMe!"));
        } else {
            /* Needed for linkid / dupe check! */
            link.setProperty("itemId", metadata.getContentID());
        }
        if (filesize > 0) {
            if (setVerifiedFilesize) {
                link.setVerifiedFileSize(filesize);
            } else {
                link.setDownloadSize(filesize);
            }
            /* Filesize available? We know that content is online and can set AvailableStatus right away ("Fast linkcheck"). */
            link.setAvailable(true);
        } else if (cfg.isFastLinkcheckEnabled()) {
            link.setAvailable(true);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(metadata.getPackagename());
        if (metadata.getDescription() != null) {
            fp.setComment(metadata.getDescription());
        }
        link._setFilePackage(fp);
        qualitiesMap.put(qualityStringForQualitySelection, link);
        return link;
    }

    /* Returns quality identifier String, compatible with quality selection values. Format: protocol_bitrateCorrected_heightCorrected */
    private String getQualityIdentifier(final String directurl, long bitrate, final VideoResolution resolution) {
        final String protocol;
        if (directurl.contains("m3u8")) {
            protocol = "hls";
        } else {
            protocol = "http";
        }
        /* Use this for quality selection as real resolution can be slightly different than the values which our users can select. */
        final int height;
        if (resolution != null) {
            height = resolution.getHeight();
        } else {
            height = 0;
        }
        final String qualityStringForQualitySelection = protocol + "_" + height;
        return qualityStringForQualitySelection;
    }

    private ArrayList<DownloadLink> handleUserQualitySelection(final HashMap<String, DownloadLink> foundQualitiesMap) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* We have to re-add the subtitle for the best quality if wished by the user */
        HashMap<String, DownloadLink> finalSelectedQualityMap = new HashMap<String, DownloadLink>();
        if (cfg.isGrabBESTEnabled()) {
            /* User wants BEST only */
            finalSelectedQualityMap = findBESTInsideGivenMap(foundQualitiesMap);
        } else {
            /* 2022-01-20: Grabbing unknown qualities is not supported anymore for now. */
            // final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
            final boolean grabUnknownQualities = false;
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
                selectedQualities.addAll(all_known_qualities);
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
            /* Check if user maybe only wants the best quality inside his selected video qualities. */
            if (cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled()) {
                finalSelectedQualityMap = findBESTInsideGivenMap(finalSelectedQualityMap);
            }
        }
        /* Finally add selected URLs */
        final Iterator<Entry<String, DownloadLink>> it = finalSelectedQualityMap.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> entry = it.next();
            final DownloadLink dl = entry.getValue();
            if (cfg.isGrabSubtitleEnabled() && !StringUtils.isEmpty(subtitleLink)) {
                final DownloadLink subtitle = createDownloadlink(subtitleLink.replaceAll("https?://", getHost() + "decrypted://"));
                final MediathekProperties data_src = dl.bindData(MediathekProperties.class);
                final MediathekProperties data_subtitle = subtitle.bindData(MediathekProperties.class);
                data_subtitle.setStreamingType("subtitle");
                data_subtitle.setSourceHost(data_src.getSourceHost());
                data_subtitle.setChannel(data_src.getChannel());
                data_subtitle.setProtocol(data_src.getProtocol() + "sub");
                data_subtitle.setResolution(data_src.getResolution());
                data_subtitle.setBitrateTotal(data_src.getBitrateTotal());
                data_subtitle.setTitle(data_src.getTitle());
                data_subtitle.setFileExtension("xml");
                if (data_src.getShow() != null) {
                    data_subtitle.setShow(data_src.getShow());
                }
                if (data_src.getReleaseDate() > 0) {
                    data_subtitle.setReleaseDate(data_src.getReleaseDate());
                }
                subtitle.setAvailable(true);
                subtitle.setFinalFileName(MediathekHelper.getMediathekFilename(subtitle, data_subtitle, true, true));
                subtitle.setProperty("itemId", dl.getProperty("itemId", null));
                subtitle.setContentUrl(dl.getContentUrl());
                subtitle._setFilePackage(dl.getFilePackage());
                ret.add(subtitle);
            }
            ret.add(dl);
        }
        return ret;
    }

    private boolean isUnsupportedProtocolDasersteVideo(final String directlink) {
        final boolean isUnsupported = directlink == null || !StringUtils.startsWithCaseInsensitive(directlink, "http") || StringUtils.endsWithCaseInsensitive(directlink, "manifest.f4m");
        return isUnsupported;
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

    /**
     * Given width may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    @Deprecated
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

    public class ArdMetadata {
        private String title         = null;
        private String subtitle      = null;
        private String channel       = null;
        private String description   = null;
        private String contentID     = null;
        private long   dateTimestamp = -1;

        protected String getTitle() {
            return title;
        }

        protected void setTitle(String title) {
            this.title = title;
        }

        protected String getSubtitle() {
            return subtitle;
        }

        protected void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        protected String getChannel() {
            return channel;
        }

        protected void setChannel(String channel) {
            this.channel = channel;
        }

        protected String getContentID() {
            return contentID;
        }

        protected void setDateTimestamp(final long dateTimestamp) {
            this.dateTimestamp = dateTimestamp;
        }

        protected long getDateTimestamp() {
            return dateTimestamp;
        }

        public ArdMetadata() {
        }

        public ArdMetadata(final String title) {
            this.title = title;
        }

        protected void setContentID(String contentID) {
            this.contentID = contentID;
        }

        /** Returns date in format yyyy-MM-dd */
        protected String getFormattedDate() {
            if (this.dateTimestamp == -1) {
                return null;
            } else {
                return new SimpleDateFormat("yyyy-MM-dd").format(new Date(this.dateTimestamp));
            }
        }

        protected String getPackagename() {
            final String dateFormatted = this.getFormattedDate();
            if (dateFormatted != null) {
                return dateFormatted + "_" + this.title;
            } else {
                return this.title;
            }
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public enum VideoResolution {
        // Order is default quality sort order
        P_1080(1920, 1080),
        P_720(1280, 720),
        /** 2022-10-21: Removed as we now detect quality/resolution by heigt */
        // P_544(960, 544)
        P_540(960, 540),
        P_360(640, 360),
        P_288(512, 288),
        P_270(480, 270),
        P_180(320, 180),
        P_144(256, 144);

        private int height;
        private int width;

        private VideoResolution(int width, int height) {
            this.height = height;
            this.width = width;
        }

        @Override
        public String toString() {
            return this.width + "x" + this.height;
        }

        public int getHeight() {
            return height;
        }

        public String getLabel() {
            return height + "p";
        }

        public int getWidth() {
            return width;
        }

        public static VideoResolution getByWidth(int width) {
            for (final VideoResolution r : values()) {
                if (r.getWidth() == width) {
                    return r;
                }
            }
            return null;
        }

        public static VideoResolution getByHeight(int height) {
            for (final VideoResolution r : values()) {
                if (r.getHeight() == height) {
                    return r;
                }
            }
            return null;
        }

        public static VideoResolution getByURL(final String url) {
            if (url == null) {
                return null;
            }
            String width_str = new Regex(url, "(hi|hq|ln|lo|mn)\\.mp4$").getMatch(0);
            if (width_str == null) {
                /* Type 2 */
                width_str = new Regex(url, "(s|m|sm|ml|l)\\.mp4$").getMatch(0);
            }
            if (width_str == null) {
                /* Type 3 */
                width_str = new Regex(url, "(webm|webs|webl|webxl|webxxl)").getMatch(0);
            }
            if (width_str == null) {
                /* Type 4 */
                width_str = new Regex(url, "/(\\d{1,4})\\-\\d+\\.mp4$").getMatch(0);
            }
            if (width_str != null) {
                /* Convert given quality-text to width. */
                if (width_str.equals("mn") || width_str.equals("sm")) {
                    return P_270;
                } else if (width_str.equals("hi") || width_str.equals("m") || width_str.equals("_ard") || width_str.equals("webm")) {
                    return P_288;
                } else if (width_str.equals("ln") || width_str.equals("ml")) {
                    return P_360;
                } else if (width_str.equals("lo") || width_str.equals("s") || width_str.equals("webs")) {
                    return P_180;
                } else if (width_str.equals("hq") || width_str.equals("l") || width_str.equals("webl")) {
                    return P_540;
                } else if (width_str.equals("webxl")) {
                    return P_720;
                } else if (width_str.equals("webxl")) {
                    return P_1080;
                } else {
                    return null;
                }
            } else {
                /* More loose checks */
                if (StringUtils.containsIgnoreCase(url, "0.mp4") || StringUtils.containsIgnoreCase(url, "128k.mp4")) {
                    return P_180;
                } else if (StringUtils.containsIgnoreCase(url, "lo.mp4")) {
                    return P_144;
                } else if (StringUtils.containsIgnoreCase(url, "A.mp4") || StringUtils.containsIgnoreCase(url, "mn.mp4") || StringUtils.containsIgnoreCase(url, "256k.mp4")) {
                    return P_270;
                } else if (StringUtils.containsIgnoreCase(url, "B.mp4") || StringUtils.containsIgnoreCase(url, "hi.mp4") || StringUtils.containsIgnoreCase(url, "512k.mp4")) {
                    return P_288;
                } else if (StringUtils.containsIgnoreCase(url, "C.mp4") || StringUtils.containsIgnoreCase(url, "hq.mp4") || StringUtils.containsIgnoreCase(url, "1800k.mp4")) {
                    return P_540;
                } else if (StringUtils.containsIgnoreCase(url, "E.mp4") || StringUtils.containsIgnoreCase(url, "ln.mp4") || StringUtils.containsIgnoreCase(url, "1024k.mp4") || StringUtils.containsIgnoreCase(url, "1.mp4")) {
                    return P_360;
                } else if (StringUtils.containsIgnoreCase(url, "X.mp4") || StringUtils.containsIgnoreCase(url, "hd.mp4")) {
                    return P_720;
                } else {
                    return null;
                }
            }
        }
    }

    // private String getXML(final String parameter) {
    // return getXML(this.br.toString(), parameter);
    // }
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

    /** Returns milliseconds for various date formats */
    private long getDateMilliseconds(String input) {
        if (input == null) {
            return -1;
        }
        final long date_milliseconds;
        if (input.matches("\\d{4}\\-\\d{2}\\-\\d{2}")) {
            date_milliseconds = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd", Locale.GERMAN);
        } else if (input.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")) {
            date_milliseconds = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy HH:mm", Locale.GERMAN);
        } else {
            /* 2015-06-23T20:15:00.000+02:00 --> 2015-06-23T20:15:00.000+0200 */
            input = new Regex(input, "^(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
            date_milliseconds = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd", Locale.GERMAN);
        }
        return date_milliseconds;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}
