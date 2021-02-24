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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ZdfDeMediathek;
import jd.plugins.hoster.ZdfDeMediathek.ZdfmediathekConfigInterface;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zdf.de", "3sat.de" }, urls = { "https?://(?:www\\.)?zdf\\.de/.+/[A-Za-z0-9_\\-]+\\.html|https?://(?:www\\.)?zdf\\.de/uri/(?:syncvideoimport_beitrag_\\d+|transfer_SCMS_[a-f0-9\\-]+|[a-z0-9\\-]+)", "https?://(?:www\\.)?3sat\\.de/.+/[A-Za-z0-9_\\-]+\\.html|https?://(?:www\\.)?3sat\\.de/uri/(?:syncvideoimport_beitrag_\\d+|transfer_SCMS_[a-f0-9\\-]+|[a-z0-9\\-]+)" })
public class ZDFMediathekDecrypter extends PluginForDecrypt {
    private String              PARAMETER                 = null;
    private String              PARAMETER_ORIGINAL        = null;
    private boolean             fastlinkcheck             = false;
    private final String        TYPE_ZDF                  = "https?://(?:www\\.)?(?:zdf\\.de|3sat\\.de)/.+";
    /* Not sure where these URLs come from. Probably old RSS readers via old APIs ... */
    private final String        TYPER_ZDF_REDIRECT        = "https?://[^/]+/uri/.+";
    private List<String>        userSelectedSubtitleTypes = new ArrayList<String>();
    private Map<String, String> allSubtitles              = new HashMap<String, String>();

    public ZDFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private List<String> getKnownQualities() {
        /** Returns all possible quality identifier strings in order highest --> lowest */
        final List<String> all_known_qualities = new ArrayList<String>();
        final String[] knownProtocols = { "http", "hls" };
        /** 2021-02-01: Removed all .webm qualities from settings */
        final String[] knownExtensions = { "mp4", "webm" };
        final String[] knownQualityNames = { "1080", "hd", "veryhigh", "720", "480", "360", "high", "low", "170" };
        final String[] knownAudioClasses = { "main", "ad", "ot" };
        for (final String protocol : knownProtocols) {
            for (final String extension : knownExtensions) {
                for (final String qualityName : knownQualityNames) {
                    for (final String audioClass : knownAudioClasses) {
                        final String qualityIdentifier = protocol + "_" + extension + "_" + qualityName + "_" + audioClass;
                        all_known_qualities.add(qualityIdentifier);
                    }
                }
            }
        }
        /* Add all possible audio-only versions */
        for (final String audioClass : knownAudioClasses) {
            all_known_qualities.add("hls_aac_0_" + audioClass);
        }
        return all_known_qualities;
    }

    /** Example of a podcast-URL: http://www.zdf.de/ZDFmediathek/podcast/1074856?view=podcast */
    /** Related sites: see RegExes, and also: 3sat.de */
    @SuppressWarnings({ "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.br.setAllowedResponseCodes(new int[] { 500 });
        PARAMETER = param.toString();
        PARAMETER_ORIGINAL = param.toString();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final ArrayList<DownloadLink> crawledLinks = getDownloadLinksZdfNew();
        if (crawledLinks == null) {
            logger.warning("Decrypter out of date for link: " + PARAMETER);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return crawledLinks;
    }

    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink dl = super.createDownloadlink(url.replaceAll("https?://", "decryptedmediathek://"));
        if (this.fastlinkcheck) {
            dl.setAvailable(true);
        }
        return dl;
    }

    /** Do not delete this code! This can crawl embedded ZDF IDs! */
    // private void crawlEmbeddedUrlsHeute() throws Exception {
    // br.getPage(this.PARAMETER);
    // if (br.containsHTML("Der Beitrag konnte nicht gefunden werden") || this.br.getHttpConnection().getResponseCode() == 404 ||
    // this.br.getHttpConnection().getResponseCode() == 500) {
    // decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
    // return;
    // }
    // final String[] ids = this.br.getRegex("\"videoId\"\\s*:\\s*\"([^\"]*?)\"").getColumn(0);
    // for (final String videoid : ids) {
    // /* These urls go back into the decrypter. */
    // final String mainlink = "https://www." + this.getHost() + "/nachrichten/heute-journal/" + videoid + ".html";
    // decryptedLinks.add(super.createDownloadlink(mainlink));
    // }
    // return;
    // }
    private ArrayList<DownloadLink> crawlEmbeddedUrlsZdfNew() throws IOException {
        final ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
        this.br.getPage(this.PARAMETER);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            results.add(this.createOfflinelink(this.PARAMETER));
            return results;
        } else {
            final String[] embedded_player_ids = this.br.getRegex("data\\-zdfplayer\\-id=\"([^<>\"]+)\"").getColumn(0);
            for (final String embedded_player_id : embedded_player_ids) {
                final String finallink = String.format("https://www.zdf.de/jdl/jdl/%s.html", embedded_player_id);
                results.add(super.createDownloadlink(finallink));
            }
            return results;
        }
    }

    /** Returns API parameters from html. */
    private String[] getApiParams(final Browser br, final String url, final boolean returnHardcodedData) throws IOException {
        String apitoken;
        /* 2020-03-19: apitoken2 not required anymore?! */
        String apitoken2;
        String api_base;
        String profile;
        if (url == null) {
            return null;
        } else if (returnHardcodedData) {
            if (url.contains("3sat.de")) {
                /* 3sat.de */
                /* 2019-11-29 */
                apitoken = "22918a9c7a733c027addbcc7d065d4349d375825";
                apitoken2 = "13e717ac1ff5c811c72844cebd11fc59ecb8bc03";
                api_base = "https://api.3sat.de";
                /* 2020-03-31 */
                profile = "player2";
            } else {
                /* zdf.de / heute.de and so on */
                /* 2020-03-19 */
                apitoken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJOVDdrVWFTanNzNTZkaTZ5UjViQk9nbkpqZWw5QXQ1UGszX2JGcVRTMzFJIn0.eyJqdGkiOiJmYzlhZTA1Yi0wYmJhLTQ1YjMtOTU5Mi04YTIzMTAyNTg1Y2YiLCJleHAiOjE1ODUwNjc3MTYsIm5iZiI6MCwiaWF0IjoxNTg0NDYyOTE2LCJpc3MiOiJodHRwczovL3Nzby1yaC1zc28uYXBwcy5vcGVuc2hpZnQuemRmLmRlL2F1dGgvcmVhbG1zLzNzY2FsZSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJhY2RmZTZkYy03NGZhLTRlODAtOTNjNC1iZjNlZDNiYzFkMTQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiIxMjhkNzI2ZSIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6IjUyNzlhNGRjLTc0ODItNGJkZS1iOTlhLWIzZTNjZTdiMDNmNCIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiY2xpZW50SG9zdCI6IjE3Mi4yMy43NS4yNDQiLCJjbGllbnRJZCI6IjEyOGQ3MjZlIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LTEyOGQ3MjZlIiwiY2xpZW50QWRkcmVzcyI6IjE3Mi4yMy43NS4yNDQiLCJlbWFpbCI6InNlcnZpY2UtYWNjb3VudC0xMjhkNzI2ZUBwbGFjZWhvbGRlci5vcmcifQ.j2ssjrzdAhyOr54D18oYvhb-0LR0tViQZyIwIa9KN40h6pG1E32Dmiu2Rf_6lGNLl4uRO0I-CRIKti9HyIZSNHuH_3Gia5PqHLzWaVhapePMC_weuqdY5qJ5UkZQhD_zO5CFhxruRv7-Bhw3QEG5l8RSuN0Chh9YfD2MKZkpgHdMBO52m4rDSTKsxDuYGBIQlk_gaf3mUQiBdHoxZGTyf4yL9evy8rQUiCa4NgDsBBKaTDbc9cOwsMDlYsWLuqaStWIdJj9PYXjtjBlmCPhJgqO-c_DlFyjGCi2u0-BxMZP15iDGquAPa58LJywHWj-xajcmkN6Q9yDvo_P0zlIjzg";
                apitoken2 = null;
                api_base = "https://api.zdf.de";
                /* 2020-03-31 */
                profile = "player-3";
            }
            return new String[] { apitoken, apitoken2, api_base, profile };
        } else {
            final Browser brc;
            if (br == null) {
                brc = this.br;
            } else {
                brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                brc.getPage(url);
            }
            apitoken = brc.getRegex("\"apiToken\"\\s*:\\s*\"([^\"\\']+)\"").getMatch(0);
            apitoken2 = null;
            api_base = brc.getRegex("apiService\\s*:\\s*'(https?://[^<>\"\\']+)'").getMatch(0);
            profile = brc.getRegex("\\.json\\?profile=([^\"]+)\"").getMatch(0);
            if (apitoken == null || api_base == null || profile == null) {
                return null;
            }
            return new String[] { apitoken, apitoken2, api_base, profile };
        }
    }

    @SuppressWarnings({ "unchecked" })
    private ArrayList<DownloadLink> getDownloadLinksZdfNew() throws Exception {
        ArrayList<DownloadLink> allDownloadLinks = new ArrayList<DownloadLink>();
        final String sophoraIDSource;
        if (this.PARAMETER.matches(TYPER_ZDF_REDIRECT)) {
            this.br.setFollowRedirects(false);
            this.br.getPage(this.PARAMETER);
            sophoraIDSource = this.br.getRedirectLocation();
            this.br.setFollowRedirects(true);
        } else {
            sophoraIDSource = this.PARAMETER;
        }
        final String sophoraID = new Regex(sophoraIDSource, "/([^/]+)\\.html").getMatch(0);
        if (sophoraID == null) {
            /* Probably no videocontent - most likely, used added an invalid TYPER_ZDF_REDIRECT url. */
            allDownloadLinks.add(this.createOfflinelink(PARAMETER));
            return allDownloadLinks;
        }
        final String apiParams[] = getApiParams(br, PARAMETER_ORIGINAL, false);
        /* 2016-12-21: By hardcoding the apitoken we can save one http request thus have a faster crawl process :) */
        this.br.getHeaders().put("Api-Auth", "Bearer " + apiParams[0]);
        this.br.getPage(apiParams[2] + "/content/documents/" + sophoraID + ".json?profile=" + apiParams[3]);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            allDownloadLinks.add(this.createOfflinelink(PARAMETER));
            return allDownloadLinks;
        }
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
        Map<String, Object> entries2 = null;
        final String contentType = (String) entries.get("contentType");
        String title = (String) entries.get("title");
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = sophoraID;
        }
        final String editorialDate = (String) entries.get("editorialDate");
        final Object tvStationo = entries.get("tvService");
        final String tv_station = tvStationo != null && tvStationo instanceof String ? (String) tvStationo : "ZDF";
        // final Object hasVideoo = entries.get("hasVideo");
        // final boolean hasVideo = hasVideoo != null && hasVideoo instanceof Boolean ? ((Boolean) entries.get("hasVideo")).booleanValue() :
        // false;
        entries2 = (Map<String, Object>) entries.get("http://zdf.de/rels/brand");
        entries2 = (Map<String, Object>) entries.get("mainVideoContent");
        if (entries2 == null) {
            /* Not a single video? Maybe we have a playlist / embedded video(s)! */
            logger.info("Content is not a video --> Scanning html for embedded content");
            final ArrayList<DownloadLink> results = crawlEmbeddedUrlsZdfNew();
            if (results.size() == 0) {
                results.add(this.createOfflinelink(this.PARAMETER_ORIGINAL, "NO_DOWNLOADABLE_CONTENT"));
            }
            return results;
        }
        entries2 = (Map<String, Object>) entries2.get("http://zdf.de/rels/target");
        final String tv_show = (String) entries2.get("title");
        entries2 = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries2, "streams/default");
        final String player_url_template = (String) entries2.get("http://zdf.de/rels/streams/ptmd-template");
        String internal_videoid = (String) entries2.get("extId");
        if (StringUtils.isEmpty(player_url_template)) {
            allDownloadLinks.add(this.createOfflinelink(this.PARAMETER_ORIGINAL, "NO_DOWNLOADABLE_CONTENT"));
            return allDownloadLinks;
        }
        /* 2017-02-03: Not required at the moment */
        // if (!hasVideo) {
        // logger.info("Content is not a video --> Nothing to download");
        // return ret;
        // }
        if (StringUtils.isEmpty(contentType) || StringUtils.isEmpty(editorialDate) || StringUtils.isEmpty(tv_station)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Show is not always available - merge it with the title, if tvShow is available. */
        if (tv_show != null) {
            title = tv_show + " - " + title;
        }
        String base_title = title;
        final String date_formatted = new Regex(editorialDate, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        if (StringUtils.isEmpty(internal_videoid)) {
            internal_videoid = new Regex(player_url_template, "/([^/]{2,})$").getMatch(0);
        }
        if (date_formatted == null || internal_videoid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /** Now collect all user selected qualities. */
        final List<String> allKnownQualities = this.getKnownQualities();
        final ArrayList<DownloadLink> allSelectedDownloadlinks = new ArrayList<DownloadLink>();
        final List<String> selectedQualityStringsTmp = new ArrayList<String>();
        final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
        final ZdfmediathekConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.ZdfDeMediathek.ZdfmediathekConfigInterface.class);
        final ArrayList<String> selectedAudioVideoVersions = new ArrayList<String>();
        /* Every video should have this version available */
        selectedAudioVideoVersions.add("main");
        /* Check for other versions, desired by the user */
        if (cfg.isGrabVideoVersionAudioDeskription()) {
            selectedAudioVideoVersions.add("ad");
        }
        if (cfg.isGrabVideoVersionOriginalAudio()) {
            selectedAudioVideoVersions.add("ot");
        }
        final boolean grabBest = cfg.isGrabBESTEnabled();
        fastlinkcheck = cfg.isFastLinkcheckEnabled();
        if (cfg.isGrabSubtitleEnabled()) {
            this.userSelectedSubtitleTypes.add("omu");
        }
        if (cfg.isGrabSubtitleForDisabledPeopleEnabled()) {
            this.userSelectedSubtitleTypes.add("hoh");
        }
        final boolean grabHlsAudio = cfg.isGrabAudio();
        if (grabHlsAudio) {
            selectedQualityStringsTmp.add("hls_aac_0");
        }
        final boolean grabHls170 = cfg.isGrabHLS170pVideoEnabled();
        final boolean grabHls270 = cfg.isGrabHLS270pVideoEnabled();
        final boolean grabHls360 = cfg.isGrabHLS360pVideoEnabled();
        final boolean grabHls480 = cfg.isGrabHLS480pVideoEnabled();
        final boolean grabHls570 = cfg.isGrabHLS570pVideoEnabled();
        final boolean grabHls720 = cfg.isGrabHLS720pVideoEnabled();
        if (grabHls170) {
            selectedQualityStringsTmp.add("hls_mp4_170");
        }
        if (grabHls270) {
            selectedQualityStringsTmp.add("hls_mp4_270");
        }
        if (grabHls360) {
            selectedQualityStringsTmp.add("hls_mp4_360");
        }
        if (grabHls480) {
            selectedQualityStringsTmp.add("hls_mp4_480");
        }
        if (grabHls570) {
            selectedQualityStringsTmp.add("hls_mp4_570");
        }
        if (grabHls720) {
            selectedQualityStringsTmp.add("hls_mp4_720");
        }
        boolean grabOfficialDownloadUrls = false;
        final int selectedQualityStringsTmpLengthOld = selectedQualityStringsTmp.size();
        if (cfg.isGrabHTTPMp4LowVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_low");
        }
        if (cfg.isGrabHTTPMp4MediumVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_medium");
        }
        if (cfg.isGrabHTTPMp4HighVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_high");
        }
        if (cfg.isGrabHTTPMp4VeryHighVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_veryhigh");
        }
        if (cfg.isGrabHTTPMp4HDVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_hd");
        }
        if (selectedQualityStringsTmp.size() > selectedQualityStringsTmpLengthOld) {
            grabOfficialDownloadUrls = true;
        }
        final List<String> selectedQualityStrings = new ArrayList<String>();
        for (final String selectedQualityTmp : selectedQualityStringsTmp) {
            for (final String selectedAudioVideoVersion : selectedAudioVideoVersions) {
                selectedQualityStrings.add(selectedQualityTmp + "_" + selectedAudioVideoVersion);
            }
        }
        /*
         * Grabbing hls means we make an extra http request --> Only do this if wished by the user or if the user set bad plugin settings!
         */
        final boolean grabHLS = grabHlsAudio || ((grabHls170 || grabHls270 || grabHls360 || grabHls480 || grabHls570 || grabHls720) && !grabBest);
        final Map<String, Object> audioVideoMap = new HashMap<String, Object>();
        final String filename_packagename_base_title = date_formatted + "_" + tv_station + "_" + base_title;
        short crawledDownloadTypesCounter = 0;
        boolean grabDownloadUrlsPossible = false;
        final List<String> hlsDupeArray = new ArrayList<String>();
        boolean atLeastOneSelectedVideoAudioVersionIsAvailable = false;
        do {
            /**
             * TODO: Maybe prefer official download over stream download if available. Check if there are disadvantages e.g. lower quality
             * when doing so!
             */
            if (crawledDownloadTypesCounter == 0) {
                /* First round: Grab streams */
                logger.info("Crawling stream URLs");
                accessPlayerJson(apiParams[2], player_url_template, "ngplayer_2_3");
            } else if (grabOfficialDownloadUrls && grabDownloadUrlsPossible) {
                /* 2nd round: Grab ffficial video download URLs if possible and wanted by the user */
                logger.info("Crawling download URLs");
                accessPlayerJson(apiParams[2], player_url_template, "zdf_pd_download_1");
                break;
            } else {
                /* Fail safe && case when there are no additional downloadlinks available. */
                logger.info("Stopping as we've crawled all qualities");
                break;
            }
            entries = JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
            /* 1. Collect subtitles */
            final Object captionsO = JavaScriptEngineFactory.walkJson(entries, "captions");
            if (captionsO instanceof List && this.allSubtitles.isEmpty()) {
                /* Captions can be available in different versions (languages- and types) */
                final List<Object> subtitlesO = (List<Object>) entries.get("captions");
                Map<String, Object> subInfo = null;
                for (final Object subtitleO : subtitlesO) {
                    subInfo = (Map<String, Object>) subtitleO;
                    final String subtitleType = (String) subInfo.get("class");
                    final String uri = (String) subInfo.get("uri");
                    final boolean formatIsSupported = uri != null && uri.toLowerCase(Locale.ENGLISH).contains(".xml");
                    /* E.g. "ebu-tt-d-basic-de" or "webvtt" */
                    // final String format = (String) subInfo.get("format");
                    /* Skip unsupported formats */
                    if (!formatIsSupported) {
                        continue;
                    } else {
                        allSubtitles.put(subtitleType, uri);
                    }
                }
            }
            /* 2. Grab video streams */
            final Object downloadAllowed_o = JavaScriptEngineFactory.walkJson(entries, "attributes/downloadAllowed/value");
            if (downloadAllowed_o != null && downloadAllowed_o instanceof Boolean) {
                /* Are official video download-URLs existant? */
                grabDownloadUrlsPossible = ((Boolean) downloadAllowed_o).booleanValue();
            }
            final ArrayList<Object> priorityList = (ArrayList<Object>) entries.get("priorityList");
            for (final Object priority_o : priorityList) {
                entries = (Map<String, Object>) priority_o;
                final List<Object> formitaeten = (List<Object>) entries.get("formitaeten");
                for (final Object formitaet_o : formitaeten) {
                    /* 2020-12-21: Skips (two) lower http qualities - just a test */
                    // final String facet = (String) JavaScriptEngineFactory.walkJson(entries, "facets/{0}");
                    // if ("restriction_useragent".equalsIgnoreCase(facet)) {
                    // continue;
                    // }
                    entries = (Map<String, Object>) formitaet_o;
                    final boolean isAdaptive = ((Boolean) entries.get("isAdaptive")).booleanValue();
                    final String type = (String) entries.get("type");
                    /* Process qualities */
                    final List<Object> qualities = (List<Object>) entries.get("qualities");
                    for (final Object qualities_o : qualities) {
                        entries = (Map<String, Object>) qualities_o;
                        final String quality = (String) entries.get("quality");
                        entries = (Map<String, Object>) entries.get("audio");
                        final List<Object> tracks = (List<Object>) entries.get("tracks");
                        for (final Object trackO : tracks) {
                            entries = (Map<String, Object>) trackO;
                            final String audio_class = (String) entries.get("class");
                            if (!atLeastOneSelectedVideoAudioVersionIsAvailable && selectedAudioVideoVersions.contains(audio_class)) {
                                atLeastOneSelectedVideoAudioVersionIsAvailable = true;
                            }
                            final List<Object> qualitiesForThisLang;
                            if (audioVideoMap.containsKey(audio_class)) {
                                qualitiesForThisLang = (List<Object>) audioVideoMap.get(audio_class);
                            } else {
                                qualitiesForThisLang = new ArrayList<Object>();
                            }
                            entries.put("isAdaptive", isAdaptive);
                            entries.put("type", type);
                            entries.put("quality", quality);
                            qualitiesForThisLang.add(entries);
                            audioVideoMap.put(audio_class, qualitiesForThisLang);
                        }
                    }
                }
            }
            crawledDownloadTypesCounter++;
        } while (!this.isAbort());
        final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
        final ArrayList<DownloadLink> userSelectedQualities = new ArrayList<DownloadLink>();
        final Iterator<Entry<String, Object>> iterator = audioVideoMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<String, Object> entry = iterator.next();
            final String audio_class = entry.getKey();
            /* Skip here if we're allowed to -> Skips crawl processes of HLS versions for unwanted video-versions -> Saves time */
            if (atLeastOneSelectedVideoAudioVersionIsAvailable && !selectedAudioVideoVersions.contains(audio_class)) {
                logger.info("Skipping audioVideoVersion: " + audio_class);
                continue;
            }
            final ArrayList<DownloadLink> userSelectedQualitiesTmp = new ArrayList<DownloadLink>();
            final HashMap<String, DownloadLink> selectedQualitiesMapTmp = new HashMap<String, DownloadLink>();
            final List<Object> qualitiesList = (List<Object>) entry.getValue();
            DownloadLink highestHlsDownload = null;
            DownloadLink highestHTTPDownloadLink = null;
            int highestHlsBandwidth = 0;
            int highestHTTPQualityValue = 0;
            for (final Object qualityO : qualitiesList) {
                entries = (Map<String, Object>) qualityO;
                final boolean isAdaptive = ((Boolean) entries.get("isAdaptive"));
                final String type = (String) entries.get("type");
                String protocol = "http";
                /* First check for global skip conditions */
                if (isAdaptive && !type.contains("m3u8")) {
                    /* 2017-02-03: Skip HDS as HLS already contains all segment qualities. */
                    continue;
                }
                /* Now set some properties that are relevant for all items that are processed in this loop. */
                if (isAdaptive) {
                    protocol = "hls";
                }
                String ext;
                if (type.contains("vorbis")) {
                    /* http webm streams. */
                    ext = "webm";
                } else {
                    /* http mp4- and segment streams. */
                    ext = "mp4";
                }
                final String language = (String) entries.get("language");
                String uri = (String) entries.get("uri");
                if (StringUtils.isEmpty(audio_class) || StringUtils.isEmpty(language) || StringUtils.isEmpty(uri)) {
                    /* Skip invalid objects */
                    continue;
                }
                final String cdn = (String) entries.get("cdn");
                final long filesize = JavaScriptEngineFactory.toLong(entries.get("filesize"), 0);
                final String audio_class_user_readable = convertInternalAudioClassToUserReadable(audio_class);
                String finalDownloadURL;
                String linkid;
                String final_filename;
                /* internal_videoid, type, cdn, language, audio_class, protocol, resolution */
                final String linkid_format = "%s_%s_%s_%s_%s_%s_%s";
                /*
                 * Each final filename should contain: filename_packagename_base_title, protocol, resolution, language,
                 * audio_class_user_readable, ext
                 */
                DownloadLink dl;
                if (isAdaptive) {
                    if (!grabHLS) {
                        /* Skip hls if not required by the user. */
                        continue;
                    }
                    /* HLS Segment download */
                    String hls_master_quality_str = new Regex(uri, "m3u8/(\\d+)/").getMatch(0);
                    if (hls_master_quality_str == null) {
                        // we asume this leads to m3u8 with multiple qualities
                        // better than not processing any m3u8
                        hls_master_quality_str = String.valueOf(Short.MAX_VALUE);
                    }
                    final String hls_master_dupe_string = hls_master_quality_str + "_" + audio_class;
                    if (hlsDupeArray.contains(hls_master_dupe_string)) {
                        /* Skip dupes */
                        continue;
                    }
                    hlsDupeArray.add(hls_master_dupe_string);
                    /* Access (hls) master. */
                    this.br.getPage(uri);
                    final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(this.br);
                    long duration = -1;
                    for (final HlsContainer hlscontainer : allHlsContainers) {
                        if (duration == -1) {
                            duration = 0;
                            final List<M3U8Playlist> playList = hlscontainer.getM3U8(br.cloneBrowser());
                            if (playList != null) {
                                for (M3U8Playlist play : playList) {
                                    duration += play.getEstimatedDuration();
                                }
                            }
                        }
                        final String height_for_quality_selection = getHeightForQualitySelection(hlscontainer.getHeight());
                        final String resolution = hlscontainer.getResolution();
                        finalDownloadURL = hlscontainer.getDownloadurl();
                        ext = hlscontainer.getFileExtension().replace(".", "");
                        linkid = this.getHost() + "://" + String.format(linkid_format, internal_videoid, type, cdn, language, audio_class, protocol, resolution);
                        final_filename = filename_packagename_base_title + "_" + protocol + "_" + resolution + "_" + language + "_" + audio_class_user_readable + "." + ext;
                        dl = createDownloadlink(finalDownloadURL);
                        if (hlscontainer.getBandwidth() > highestHlsBandwidth) {
                            /*
                             * While adding the URLs, let's find the BEST quality url. In case we need it later we will already know which
                             * one is the BEST.
                             */
                            highestHlsBandwidth = hlscontainer.getBandwidth();
                            highestHlsDownload = dl;
                        }
                        setDownloadlinkProperties(dl, final_filename, type, linkid, title, tv_show, date_formatted, tv_station);
                        dl.setProperty(ZdfDeMediathek.PROPERTY_hlsBandwidth, hlscontainer.getBandwidth());
                        if (duration > 0 && hlscontainer.getBandwidth() > 0) {
                            dl.setDownloadSize(duration / 1000 * hlscontainer.getBandwidth() / 8);
                        }
                        final String qualitySelectorString = generateQualitySelectorString(protocol, ext, Integer.toString(hlscontainer.getHeight()), language, audio_class);
                        all_found_downloadlinks.put(qualitySelectorString, dl);
                        addDownloadLinkAndGenerateSubtitleDownloadLink(allDownloadLinks, dl);
                        if (containsQuality(selectedQualityStrings, qualitySelectorString)) {
                            userSelectedQualitiesTmp.add(dl);
                            selectedQualitiesMapTmp.put(qualitySelectorString, dl);
                        } else if (grabUnknownQualities && !containsQuality(allKnownQualities, qualitySelectorString)) {
                            userSelectedQualitiesTmp.add(dl);
                        }
                        all_found_downloadlinks.put(generateQualitySelectorString(protocol, ext, height_for_quality_selection, language, audio_class), dl);
                    }
                    /**
                     * Extra check for abort here to abort hls crawling as it needs one extra http request to crawl each HLS-master.
                     */
                    if (this.isAbort()) {
                        break;
                    }
                } else {
                    /* http download */
                    String quality = (String) entries.get("quality");
                    finalDownloadURL = uri;
                    /*
                     * 2020-12-21: Some tests: There are higher http qualities available than what we get via API (see also mediathekview)
                     * ...
                     */
                    /* Do NOT alter official downloadurls such as "http://downloadzdf-a.akamaihd.net/..." */
                    boolean downloadurlWasModified = true;
                    if (finalDownloadURL.matches("https?://[a-z0-9]*rodlzdf[^/]+\\.akamaihd\\.net/.+_\\d+k_p\\d+v\\d+\\.mp4")) {
                        /* Improve "veryhigh" --> hd */
                        if (finalDownloadURL.contains("_1628k_p13v15.mp4")) {
                            finalDownloadURL = finalDownloadURL.replace("_1628k_p13v15.mp4", "_3360k_p36v15.mp4");
                            quality = "hd";
                        } else if (finalDownloadURL.contains("_808k_p11v15.mp4")) {
                            /* Improve "high/medium" --> veryhigh (?) */
                            finalDownloadURL = finalDownloadURL.replace("_808k_p11v15.mp4", "_2360k_p35v15.mp4");
                            quality = "veryhigh";
                        } else if (finalDownloadURL.contains("_508k_p9v15.mp4")) {
                            /* Improve "low" -> medium (?) */
                            finalDownloadURL = finalDownloadURL.replace("_508k_p9v15.mp4", "_808k_p11v15.mp4");
                            quality = "medium";
                        } else {
                            logger.info("Not altering quality: " + quality);
                            downloadurlWasModified = false;
                        }
                    } else {
                        downloadurlWasModified = false;
                    }
                    linkid = this.getHost() + "://" + String.format(linkid_format, internal_videoid, type, cdn, language, audio_class, protocol, quality);
                    final_filename = filename_packagename_base_title + "_" + protocol + "_" + quality + "_" + language + "_" + audio_class_user_readable + "." + ext;
                    dl = createDownloadlink(finalDownloadURL);
                    /**
                     * Usually filesize is only given for the official downloads.</br>
                     * Only set it here if we haven't touched the original downloadurls!
                     */
                    if (filesize > 0 && !downloadurlWasModified) {
                        dl.setAvailable(true);
                        dl.setVerifiedFileSize(filesize);
                        // dl.setDownloadSize(filesize);
                    }
                    setDownloadlinkProperties(dl, final_filename, type, linkid, title, tv_show, date_formatted, tv_station);
                    final String qualitySelectorString = generateQualitySelectorString(protocol, ext, quality, language, audio_class);
                    all_found_downloadlinks.put(qualitySelectorString, dl);
                    addDownloadLinkAndGenerateSubtitleDownloadLink(allDownloadLinks, dl);
                    if (containsQuality(selectedQualityStrings, qualitySelectorString)) {
                        userSelectedQualitiesTmp.add(dl);
                        selectedQualitiesMapTmp.put(qualitySelectorString, dl);
                    } else if (grabUnknownQualities && !containsQuality(allKnownQualities, qualitySelectorString)) {
                        userSelectedQualitiesTmp.add(dl);
                    }
                    final int httpQualityValueTmp = convertInternalHttpQualityIdentifierToIntegerValue(quality);
                    if (httpQualityValueTmp > highestHTTPQualityValue) {
                        highestHTTPQualityValue = httpQualityValueTmp;
                        highestHTTPDownloadLink = dl;
                    }
                }
            }
            /* Finally, check which qualities the user actually wants to have. */
            if (grabBest) {
                /* Best: Prefer best HLS quality - fallback to best http quality */
                if (highestHlsDownload != null) {
                    userSelectedQualitiesTmp.clear();
                    userSelectedQualitiesTmp.add(highestHlsDownload);
                } else if (highestHTTPDownloadLink != null) {
                    userSelectedQualitiesTmp.clear();
                    userSelectedQualitiesTmp.add(highestHTTPDownloadLink);
                } else {
                    /* No BEST candidate available in current round! */
                }
            } else if (cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled()) {
                /* Best of selected */
                final DownloadLink bestOfSelection = findBESTInsideGivenMapNew(selectedQualitiesMapTmp, allKnownQualities);
                userSelectedQualitiesTmp.clear();
                userSelectedQualitiesTmp.add(bestOfSelection);
            } else {
                /* Add all selected */
            }
            /* Finally add DownloadLinks + subtitles */
            for (final DownloadLink userSelectedQualityTmp : userSelectedQualitiesTmp) {
                addDownloadLinkAndGenerateSubtitleDownloadLink(userSelectedQualities, userSelectedQualityTmp);
            }
        }
        if (allDownloadLinks.isEmpty()) {
            logger.warning("Failed to find any results at all");
            return allSelectedDownloadlinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filename_packagename_base_title);
        if (!userSelectedQualities.isEmpty()) {
            logger.info("Using user selected qualities");
            fp.addLinks(userSelectedQualities);
            return userSelectedQualities;
        } else {
            /* E.g. if user has only selected qualities that are not available or if user has disabled all possible qualities. */
            logger.info("Fallback to all found qualities");
            fp.addLinks(allDownloadLinks);
            return allDownloadLinks;
        }
    }

    private String generateQualitySelectorString(final String protocol, final String ext, final String quality, final String language, final String audio_class) {
        final String quality_selector_string = protocol + "_" + ext + "_" + quality + "_" + audio_class;
        return quality_selector_string;
    }

    private boolean containsQuality(final List<String> qualities, final String qualityID) {
        for (String quality : qualities) {
            if (StringUtils.startsWithCaseInsensitive(qualityID, quality)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given width may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    private String getHeightForQualitySelection(final int height) {
        final String heightselect;
        if (height > 0 && height <= 200) {
            heightselect = "170";
        } else if (height > 200 && height <= 300) {
            heightselect = "270";
        } else if (height > 300 && height <= 400) {
            heightselect = "360";
        } else if (height > 400 && height <= 500) {
            heightselect = "480";
        } else if (height > 500 && height <= 600) {
            heightselect = "570";
        } else if (height > 600 && height <= 800) {
            heightselect = "720";
        } else {
            /* Either unknown quality or audio (0x0) */
            heightselect = Integer.toString(height);
        }
        return heightselect;
    }
    // private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> bestMap) {
    // HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
    // DownloadLink keep = null;
    // if (bestMap.size() > 0) {
    // for (final String quality : all_known_qualities) {
    // keep = bestMap.get(quality);
    // if (keep != null) {
    // newMap.put(quality, keep);
    // break;
    // }
    // }
    // }
    // if (newMap.isEmpty()) {
    // /* Failover in case of bad user selection or general failure! */
    // newMap = bestMap;
    // }
    // return newMap;
    // }

    private DownloadLink findBESTInsideGivenMapNew(final HashMap<String, DownloadLink> bestMap, final List<String> allKnownQualities) {
        DownloadLink keep = null;
        if (bestMap.size() > 0) {
            for (final String quality : allKnownQualities) {
                keep = bestMap.get(quality);
                if (keep != null) {
                    return keep;
                }
            }
        }
        return null;
    }

    private void addDownloadLinkAndGenerateSubtitleDownloadLink(final ArrayList<DownloadLink> decryptedLinks, final DownloadLink dl) {
        decryptedLinks.add(dl);
        for (final String selectedSubtitleType : this.userSelectedSubtitleTypes) {
            if (this.allSubtitles.containsKey(selectedSubtitleType)) {
                final String current_ext = dl.getFinalFileName().substring(dl.getFinalFileName().lastIndexOf("."));
                final String longSubtitleName = this.convertInternalSubtitleClassToUserReadable(selectedSubtitleType);
                final String final_filename = dl.getFinalFileName().replace(current_ext, "_" + longSubtitleName + ".xml");
                final String linkid = dl.getLinkID() + "_" + longSubtitleName;
                final DownloadLink dl_subtitle = this.createDownloadlink(this.allSubtitles.get(selectedSubtitleType));
                setDownloadlinkProperties(dl_subtitle, final_filename, "subtitle", linkid, null, null, null, null);
                decryptedLinks.add(dl_subtitle);
            }
        }
    }

    private void setDownloadlinkProperties(final DownloadLink dl, final String final_filename, final String streamingType, final String linkid, final String title, final String tv_show, final String date_formatted, final String tv_station) {
        dl.setFinalFileName(final_filename);
        dl.setLinkID(linkid);
        /* Very important! */
        dl.setProperty(ZdfDeMediathek.PROPERTY_streamingType, streamingType);
        /* The following properties are only relevant for packagizer usage. */
        if (!StringUtils.isEmpty(title)) {
            dl.setProperty(ZdfDeMediathek.PROPERTY_title, title);
        }
        if (!StringUtils.isEmpty(tv_show)) {
            dl.setProperty(ZdfDeMediathek.PROPERTY_tv_show, tv_show);
        }
        if (!StringUtils.isEmpty(date_formatted)) {
            dl.setProperty(ZdfDeMediathek.PROPERTY_date_formatted, date_formatted);
        }
        if (!StringUtils.isEmpty(tv_station)) {
            dl.setProperty(ZdfDeMediathek.PROPERTY_tv_station, tv_station);
        }
        dl.setContentUrl(PARAMETER_ORIGINAL);
    }

    private void accessPlayerJson(final String api_base, final String player_url_template, final String playerID) throws IOException {
        /* E.g. "/tmd/2/{playerId}/vod/ptmd/mediathek/161215_sendungroyale065ddm_nmg" */
        String player_url = player_url_template.replace("{playerId}", playerID);
        if (player_url.startsWith("/")) {
            player_url = api_base + player_url;
        }
        this.br.getPage(player_url);
    }

    public static String formatDateZDF(String input) {
        final long date;
        if (input.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{2}:\\d{2}")) {
            /* tivi.de */
            input = input.substring(0, input.lastIndexOf(":")) + "00";
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.GERMAN);
        } else {
            /* zdf.de/zdfmediathek */
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy HH:mm", Locale.GERMAN);
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

    /** The result of this can be used to sort http qualities e.g. input "low" --> Output "100". */
    private int convertInternalHttpQualityIdentifierToIntegerValue(final String httpQualityIdentifier) {
        if (httpQualityIdentifier == null) {
            return 0;
        } else if (httpQualityIdentifier.equalsIgnoreCase("low")) {
            return 100;
        } else if (httpQualityIdentifier.equalsIgnoreCase("med")) {
            return 200;
        } else if (httpQualityIdentifier.equalsIgnoreCase("high")) {
            return 300;
        } else if (httpQualityIdentifier.equalsIgnoreCase("veryhigh")) {
            return 400;
        } else if (httpQualityIdentifier.equalsIgnoreCase("hd")) {
            return 500;
        } else {
            /* Unknown quality identifier! */
            return 1;
        }
    }

    private String convertInternalAudioClassToUserReadable(final String audio_class) {
        if (audio_class == null) {
            return null;
        } else if (audio_class.equals("main")) {
            return "TV_Ton";
        } else if (audio_class.equals("ad")) {
            return "Audiodeskription";
        } else if (audio_class.equals("ot")) {
            return "Originalton";
        } else {
            /* This should never happen! */
            return "Ton_unbekannt";
        }
    }

    private String convertInternalSubtitleClassToUserReadable(final String audio_class) {
        if (audio_class == null) {
            return null;
        } else if (audio_class.equals("omu")) {
            return "subtitle";
        } else if (audio_class.equals("hoh")) {
            return "subtitle_disabled_people";
        } else {
            /* This should never happen! */
            return "subtitle_unknown";
        }
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}