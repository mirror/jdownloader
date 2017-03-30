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

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.jdownloader.translate._JDT;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ardmediathek.de", "rbb-online.de" }, urls = { "http://(?:www\\.)?(?:ardmediathek|mediathek\\.daserste)\\.de/.+|http://www\\.daserste\\.de/[^<>\"]+/(?:videos|videosextern)/[a-z0-9\\-]+\\.html", "http://(?:www\\.)?mediathek\\.rbb\\-online\\.de/tv/[^<>\"]+documentId=\\d+[^<>\"/]+bcastId=\\d+" })
public class Ardmediathek extends PluginForDecrypt {
    private static final String                 EXCEPTION_LINKOFFLINE = "EXCEPTION_LINKOFFLINE";
    /* Constants */
    private static final String                 type_unsupported      = "http://(www\\.)?ardmediathek\\.de/(tv/live\\?kanal=\\d+|dossiers/.*)";
    private static final String                 type_invalid          = "http://(www\\.)?(ardmediathek|mediathek\\.daserste)\\.de/(download|livestream).+";
    private static final String                 type_ard_mediathek    = "http://(www\\.)?(ardmediathek|mediathek\\.daserste)\\.de/.+";
    private static final String                 type_ardvideo         = "http://www\\.daserste\\.de/.+";
    private static final String                 type_rbb_mediathek    = "http://(?:www\\.)?mediathek\\.rbb\\-online\\.de/tv/[^<>\"]+documentId=\\d+[^<>\"/]+bcastId=\\d+";
    /* Variables */
    private final HashMap<String, DownloadLink> foundQualitiesMap     = new HashMap<String, DownloadLink>();
    ArrayList<DownloadLink>                     decryptedLinks        = new ArrayList<DownloadLink>();
    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String>                  all_known_qualities   = Arrays.asList("http_0_720", "hls_0_720", "http_0_540", "hls_0_540", "http_0_360", "hls_0_360", "http_0_280", "hls0_280", "http_0_270", "hls_0_270", "http_0_180", "hls_0_180");

    private String                              subtitleLink          = null;
    private String                              parameter             = null;
    private String                              title                 = null;
    private String                              date                  = null;
    private String                              date_formatted        = null;

    private boolean                             grabHLS               = false;

    public Ardmediathek(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @PluginHost(host = "ardmediathek.de", type = Type.CRAWLER)
    public static interface RbbOnlineConfig extends ArdConfigInterface {
    }

    @PluginHost(host = "rbb-online.de", type = Type.CRAWLER)
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

    @Override
    public Class<? extends ArdConfigInterface> getConfigInterface() {
        if ("ardmediathek.de".equalsIgnoreCase(getHost())) {
            return ArdConfigInterface.class;
        }
        return RbbOnlineConfig.class;
    }

    /**
     * Examples of other, unsupported linktypes:
     *
     * http://daserste.ndr.de/panorama/aktuell/Mal-eben-die-Welt-retten-Studie-belegt-Gefahren-durch-Voluntourismus-,volontourismus136.html
     *
     */
    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        parameter = Encoding.htmlDecode(param.toString());
        if (parameter.matches(type_unsupported) || parameter.matches(type_invalid)) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }

        final ArdConfigInterface cfg = PluginJsonConfig.get(getConfigInterface());
        final List<String> selectedQualities = new ArrayList<String>();
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

        try {
            if (br.getURL().matches(type_ard_mediathek) || parameter.matches(type_rbb_mediathek)) {
                decryptMediathek();
            } else {
                decryptDasersteVideo();
            }
            handleUserQualitySelection(selectedQualities);
        } catch (final DecrypterException e) {
            try {
                if (e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                    decryptedLinks.add(getOffline(parameter));
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
            decryptedLinks.add(getOffline(parameter));
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    /* INFORMATION: network = akamai or limelight == RTMP */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void decryptMediathek() throws Exception {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        String show = br.getRegex("name=\"dcterms\\.isPartOf\" content=\"([^<>\"]*?)\"").getMatch(0);
        title = br.getRegex("<meta name=\"dcterms\\.title\" content=\"([^\"]+)\"").getMatch(0);
        final String realBaseUrl = new Regex(br.getBaseURL(), "(^.*\\.de)").getMatch(0);
        String broadcastID;
        if (parameter.matches("http://(www\\.)?mediathek\\.daserste\\.de/topvideos/[a-z0-9\\-_]+")) {
            broadcastID = new Regex(parameter, "/topvideos/(\\d+)").getMatch(0);
        } else {
            // ardmediathek.de
            broadcastID = new Regex(br.getURL(), "(?:\\?|\\&)documentId=(\\d+)").getMatch(0);
            // mediathek.daserste.de
            if (broadcastID == null) {
                broadcastID = new Regex(br.getURL(), realBaseUrl + "/[^/]+/[^/]+/(\\d+)").getMatch(0);
            }
            if (broadcastID == null) {
                broadcastID = new Regex(br.getURL(), realBaseUrl + "/suche/(\\d+)").getMatch(0);
            }
        }
        if (broadcastID == null) {
            logger.info("ARDMediathek: MediaID is null! link offline?");
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        if (br.containsHTML("(<h1>Leider konnte die gew\\&uuml;nschte Seite<br />nicht gefunden werden\\.</h1>|Die angeforderte Datei existiert leider nicht)") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        this.date = br.getRegex("Video der Sendung vom (\\d{2}\\.\\d{2}\\.\\d{4})").getMatch(0);
        if (this.date == null) {
            this.date = br.getRegex("class=\"subtitle\">([^<>\"]*?) \\| ").getMatch(0);
        }
        final String original_ard_ID = broadcastID;
        if (title == null) {
            title = getTitle(br);
        }
        title = Encoding.htmlDecode(title).trim();
        title = encodeUnicode(title);
        if (show != null) {
            show = Encoding.htmlDecode(show).trim();
            show = encodeUnicode(show);
            title = show + " - " + title;
        }
        if (this.date != null) {
            this.date_formatted = formatDateArdMediathek(this.date);
            title = this.date_formatted + "_ard_" + title;
        }
        final Browser br = new Browser();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.ardmediathek.de/play/media/" + original_ard_ID + "?devicetype=pc&features=flash");
        /* No json --> No media to crawl! */
        if (!br.getHttpConnection().getContentType().contains("application/json")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        subtitleLink = getJson("_subtitleUrl", br.toString());
        if (subtitleLink != null && !subtitleLink.startsWith("http://")) {
            subtitleLink = "http://www.ardmediathek.de" + subtitleLink;
        }
        boolean hls_grabbed = false;
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final ArrayList<Object> _mediaArray = (ArrayList) entries.get("_mediaArray");
        for (final Object mediaO : _mediaArray) {
            final LinkedHashMap<String, Object> mediaObjectInfo = (LinkedHashMap<String, Object>) mediaO;
            final ArrayList<Object> mediaStreamArray = (ArrayList) mediaObjectInfo.get("_mediaStreamArray");
            for (final Object stream : mediaStreamArray) {
                final LinkedHashMap<String, Object> streammap = (LinkedHashMap<String, Object>) stream;
                final Object stream_o = streammap.get("_stream");
                final ArrayList<Object> directURLs;
                if (stream_o instanceof ArrayList) {
                    /* Multiple URLs */
                    directURLs = (ArrayList<Object>) stream_o;
                } else {
                    /* Single URL */
                    directURLs = new ArrayList<Object>();
                    directURLs.add(streammap.get("_stream"));
                }

                final int width = (int) JavaScriptEngineFactory.toLong(streammap.get("_width"), 0);
                final int height = (int) JavaScriptEngineFactory.toLong(streammap.get("_height"), 0);
                final int qualityNumber = (int) JavaScriptEngineFactory.toLong(streammap.get("_quality"), -1);
                int counter = 0;

                for (final Object qualityURLo : directURLs) {
                    final String directurl = (String) qualityURLo;
                    if (isUnsupportedProtocol(directurl)) {
                        continue;
                    }
                    if (directurl.contains(".m3u8")) {
                        if (hls_grabbed) {
                            /*
                             * json can contain multiple hls master URLs leading to the same video qualities (sometimes on different
                             * servers) --> Only grab one of them!
                             */
                            continue;
                        }
                        addHLS(directurl);
                        hls_grabbed = true;
                    } else {
                        if (counter == 0) {
                            /*
                             * Only use that information for the first URL of that array because all URLs after that might be of lower
                             * quality!
                             */
                            addQuality(directurl, null, width, height, qualityNumber);
                        } else {
                            addQuality(directurl, null, 0, 0, -1);
                        }
                    }
                    counter++;
                }
            }
        }
    }

    /* INFORMATION: network = akamai or limelight == RTMP */
    private void decryptDasersteVideo() throws Exception {
        final String xml_URL = parameter.replace(".html", "~playerXml.xml");
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(xml_URL);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getHttpConnection().getContentType().equals("application/xml")) {
            throw new DecrypterException(EXCEPTION_LINKOFFLINE);
        }
        final String fskRating = this.br.getRegex("<fskRating>fsk(\\d+)</fskRating>").getMatch(0);
        if (fskRating != null && Short.parseShort(fskRating) >= 12) {
            /* Video is age restricted --> Only available from >=8PM. */
            decryptedLinks.add(this.getOffline(parameter, "FSK_BLOCKED"));
            return;
        }
        this.date = getXML("broadcastDate");
        title = getXML("shareTitle");
        if (this.title == null || this.date == null) {
            throw new DecrypterException("Decrypter broken");
        }
        title = Encoding.htmlDecode(title).trim();
        title = encodeUnicode(title);
        this.date_formatted = formatDateDasErste(this.date);
        title = this.date_formatted + "_daserste_" + title;
        final String[] mediaStreamArray = br.getRegex("(<asset.*?</asset>)").getColumn(0);
        for (final String stream : mediaStreamArray) {
            final String directurl = getXML(stream, "fileName");
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
            if (isUnsupportedProtocol(directurl)) {
                continue;
            }
            if (directurl.contains(".m3u8")) {
                /* HLS */
                addHLS(directurl);
            } else {
                /* HTTP */
                addQuality(directurl, filesize, width, height, -1);
            }
        }
        return;
    }

    private void addHLS(final String hls_master) throws Exception {
        if (!this.grabHLS) {
            /* Avoid this http request if user hasn't selected any hls qualities */
            return;
        }
        /* Access (hls) master. */
        final Browser hlsBR = this.br.cloneBrowser();
        hlsBR.getPage(hls_master);
        final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(hlsBR);
        for (final HlsContainer hlscontainer : allHlsContainers) {
            final String final_download_url = hlscontainer.getDownloadurl();
            addQuality(final_download_url, null, hlscontainer.getWidth(), hlscontainer.getHeight(), -1);
        }
    }

    private void addQuality(final String directurl, final String filesize_str, int width, int height, final int quality_number) {
        /* Get/Fix correct width/height values. */
        String width_URL = new Regex(directurl, "(hi|hq|ln|lo|mn|s|m|sm|ml|l)\\.mp4$").getMatch(0);
        if (width_URL == null) {
            width_URL = new Regex(directurl, "/(\\d{1,4})\\-\\d+\\.mp4$").getMatch(0);
        }
        width = getWidth(width_URL, width, quality_number);
        height = getHeight(width_URL, width, height, quality_number);
        /* Errorhandling */
        if (width == 0 || height == 0) {
            /* Skip items for which we cannot find out the resolution. */
            return;
        }

        final String height_final = getHeightForQualitySelection(height);

        long filesize = 0;
        if (filesize_str != null && filesize_str.matches("\\d+")) {
            filesize = Long.parseLong(filesize_str);
        }

        final String resolution = width + "x" + height;
        final String protocol;
        if (directurl.contains("m3u8")) {
            protocol = "hls";
        } else {
            protocol = "http";
        }

        final ArdConfigInterface cfg = PluginJsonConfig.get(getConfigInterface());
        final String plain_name = title + "_" + protocol + "_" + resolution;
        final String full_name = plain_name + ".mp4";
        final String qualityStringSelection = protocol + "_0_" + height_final;
        final String qualityStringFull = protocol + "_" + resolution;
        final String linkid = plain_name + "_" + protocol + "_0_" + height;
        final DownloadLink link = createDownloadlink(directurl.replaceAll("https?://", "ardmediathek://"));
        link.setFinalFileName(full_name);
        link.setContentUrl(this.parameter);
        link.setLinkID(linkid);
        if (this.date != null) {
            link.setProperty("date", this.date);
        }
        link.setProperty("directName", full_name);
        link.setProperty("plain_name", plain_name);
        link.setProperty("directQuality", qualityStringFull);
        link.setProperty("mainlink", this.parameter);
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
            boolean atLeastOneSelectedItemExists = false;
            for (final String quality : all_known_qualities) {
                if (selectedQualities.contains(quality) && foundQualitiesMap.containsKey(quality)) {
                    atLeastOneSelectedItemExists = true;
                }
            }
            if (atLeastOneSelectedItemExists) {
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
            if (PluginJsonConfig.get(getConfigInterface()).isOnlyBestVideoQualityOfSelectedQualitiesEnabled()) {
                finalSelectedQualityMap = findBESTInsideGivenMap(finalSelectedQualityMap);
            }
        }
        /* Finally add selected URLs */
        final Iterator<Entry<String, DownloadLink>> it = finalSelectedQualityMap.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> entry = it.next();
            final DownloadLink dl = entry.getValue();
            if (PluginJsonConfig.get(getConfigInterface()).isGrabSubtitleEnabled() && subtitleLink != null && !isEmpty(subtitleLink)) {
                final String plain_name = dl.getStringProperty("plain_name", null);
                final String linkid = dl.getLinkID() + "_subtitle";
                final String subtitle_filename = plain_name + ".xml";
                final DownloadLink dl_subtitle = createDownloadlink(subtitleLink.replaceAll("https?://", "ardmediathek://"));
                dl_subtitle.setAvailable(true);
                dl_subtitle.setFinalFileName(subtitle_filename);
                dl_subtitle.setProperty("directName", subtitle_filename);
                dl_subtitle.setProperty("streamingType", "subtitle");
                dl_subtitle.setProperty("directQuality", dl.getStringProperty("directQuality", null));
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

    private boolean isUnsupportedProtocol(final String directlink) {
        final boolean isHTTPUrl = directlink == null || !directlink.startsWith("http") || directlink.endsWith("manifest.f4m");
        return isHTTPUrl;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> bestMap) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (bestMap.size() > 0) {
            for (final String quality : all_known_qualities) {
                keep = bestMap.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = bestMap;
        }
        return newMap;
    }

    /** Returns videos' width. Do not remove parts of thise code without understanding them - this code is crucial for the plugin! */
    private int getWidth(final String width_str, final int width_given, final int quality_number) {
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
                } else if (width_str.equals("hi") || width_str.equals("m")) {
                    width = 512;
                } else if (width_str.equals("ln") || width_str.equals("ml")) {
                    width = 640;
                } else if (width_str.equals("lo") || width_str.equals("s")) {
                    width = 320;
                } else if (width_str.equals("hq") || width_str.equals("l")) {
                    width = 960;
                } else {
                    width = 0;
                }
            }
        } else {
            width = convertQualityNumberToWidth(quality_number);
        }
        return width;
    }

    /** Returns videos' height. Do not remove parts of thise code without understanding them - this code is crucial for the plugin! */
    private int getHeight(final String width_str, final int width, final int height_given, final int quality_number) {
        final int height;
        if (height_given > 0) {
            height = height_given;
        } else if (width_str != null) {
            height = Integer.parseInt(convertWidthToHeight(width_str));
        } else {
            height = Integer.parseInt(convertQualityNumberToHeight(quality_number));
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
            } else if (width_str.equals("hi") || width_str.equals("m")) {
                height = "288";
            } else if (width_str.equals("ln") || width_str.equals("ml")) {
                height = "360";
            } else if (width_str.equals("lo") || width_str.equals("s")) {
                height = "180";
            } else if (width_str.equals("hq") || width_str.equals("l")) {
                height = "540";
            } else {
                height = "0";
            }
        }
        return height;
    }

    /**
     * 2017-03-30: TODO: Either leave this disabled or improve upper handling - especially when a "_mediaStreamArray" object contains a
     * "_stream" array with multiple qualities, it is just trail and error which quality is which ...
     */
    private String convertQualityNumberToHeight(final int quality_number) {
        String height;
        switch (quality_number) {
        case 0:
            height = "180";
            break;
        case 1:
            height = "270";
            break;
        case 2:
            height = "288";
            break;
        case 3:
            height = "360";
            break;
        case 4:
            height = "540";
            break;
        default:
            height = "0";
            break;
        }
        /* Set this to 0 as long as it does not work reliable! */
        height = "0";
        return height;
    }

    /**
     * 2017-03-30: TODO: Either leave this disabled or improve upper handling - especially when a "_mediaStreamArray" object contains a
     * "_stream" array with multiple qualities, it is just trail and error which quality is which ...
     */
    private int convertQualityNumberToWidth(final int quality_number) {
        int width;
        switch (quality_number) {
        case 0:
            width = 320;
            break;
        case 1:
            width = 480;
            break;
        case 2:
            width = 512;
            break;
        case 3:
            width = 640;
            break;
        case 4:
            width = 720;
            break;
        default:
            width = 0;
            break;
        }
        /* Set this to 0 as long as it does not work reliable! */
        width = 0;
        return width;
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

    private DownloadLink getOffline(final String parameter) {
        final DownloadLink dl = this.createOfflinelink(parameter);
        dl.setFinalFileName(new Regex(parameter, "https?://[^/]+/(.+)").getMatch(0));
        return dl;
    }

    private DownloadLink getOffline(final String parameter, final String fail_reason) {
        final DownloadLink dl = getOffline(parameter);
        dl.setFinalFileName(fail_reason + "_" + dl.getFinalFileName());
        return dl;
    }

    private String getTitle(final Browser br) {
        String title = br.getRegex("<(div|span) class=\"(MainBoxHeadline|BoxHeadline)\">([^<]+)</").getMatch(2);
        String titleUT = br.getRegex("<span class=\"(BoxHeadlineUT|boxSubHeadline)\">([^<]+)</").getMatch(1);
        if (titleUT == null) {
            titleUT = br.getRegex("<h3 class=\"mt\\-title\"><a>([^<>\"]*?)</a></h3>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<title>ard\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
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

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String getXML(final String source, final String parameter) {
        return new Regex(source, "<" + parameter + "[^<]*?>([^<>]*?)</" + parameter + ">").getMatch(0);
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([0-9\\.]+)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
        return result;
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private String formatDateArdMediathek(final String input) {
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

    private String formatDateDasErste(String input) {
        /* 2015-06-23T20:15:00.000+02:00 --> 2015-06-23T20:15:00.000+0200 */
        input = input.substring(0, input.lastIndexOf(":")) + "00";
        final long date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.GERMAN);
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