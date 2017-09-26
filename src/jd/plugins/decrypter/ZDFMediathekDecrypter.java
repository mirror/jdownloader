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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ZdfDeMediathek.ZdfmediathekConfigInterface;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zdf.de", "neo-magazin-royale.de", "heute.de" }, urls = { "https?://(?:www\\.)?zdf\\.de/.+/[A-Za-z0-9_\\-]+\\.html|https?://(?:www\\.)?zdf\\.de/uri/(syncvideoimport_beitrag_\\d+|[a-f0-9\\-]+)", "https?://(?:www\\.)?neo\\-magazin\\-royale\\.de/.+", "https?://(?:www\\.)?heute\\.de/.+" })
public class ZDFMediathekDecrypter extends PluginForDecrypt {
    ArrayList<DownloadLink>    decryptedLinks                = new ArrayList<DownloadLink>();
    private String             PARAMETER                     = null;
    private String             PARAMETER_ORIGINAL            = null;
    private String             url_subtitle                  = null;
    private boolean            fastlinkcheck                 = false;
    private boolean            grabBest                      = false;
    private boolean            grabSubtitles                 = false;
    private long               filesizeSubtitle              = 0;
    private final String       TYPE_ZDF                      = "https?://(?:www\\.)?zdf\\.de/.+";
    private final String       TYPER_ZDF_REDIRECT            = ".+/uri/(syncvideoimport_beitrag_\\d+|[a-f0-9\\-]+)";
    private final String       TYPE_ZDF_EMBEDDED_HEUTE       = "https?://(?:www\\.)?heute\\.de/.+";
    private final String       TYPE_ZDF_EMBEDDED_NEO_MAGAZIN = "https?://(?:www\\.)?neo\\-magazin\\-royale\\.de/.+";
    private final String       API_BASE                      = "https://api.zdf.de/";
    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String> all_known_qualities           = Arrays.asList("hls_mp4_720", "http_mp4_hd", "http_webm_hd", "hls_mp4_480", "http_mp4_veryhigh", "http_webm_veryhigh", "hls_mp4_360", "http_webm_high", "hls_mp4_270", "http_mp4_high", "http_webm_low", "hls_mp4_170", "http_mp4_low", "hls_aac_0");

    public ZDFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Example of a podcast-URL: http://www.zdf.de/ZDFmediathek/podcast/1074856?view=podcast */
    /** Related sites: see RegExes, and also: 3sat.de */
    @SuppressWarnings({ "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.br.setAllowedResponseCodes(500);
        PARAMETER = param.toString();
        PARAMETER_ORIGINAL = param.toString();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        if (this.PARAMETER_ORIGINAL.matches(TYPE_ZDF_EMBEDDED_HEUTE)) {
            this.crawlEmbeddedUrlsHeute();
        } else if (this.PARAMETER_ORIGINAL.matches(TYPE_ZDF_EMBEDDED_NEO_MAGAZIN)) {
            this.crawlEmbeddedUrlsNeoMagazin();
        } else if (PARAMETER_ORIGINAL.matches(TYPE_ZDF)) {
            getDownloadLinksZdfNew();
        } else {
            logger.info("Unsupported URL(s)");
        }
        if (decryptedLinks == null) {
            logger.warning("Decrypter out of date for link: " + PARAMETER);
            return null;
        }
        return decryptedLinks;
    }

    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink dl = super.createDownloadlink(url.replaceAll("https?://", "decryptedmediathek://"));
        if (this.fastlinkcheck) {
            dl.setAvailable(true);
        }
        return dl;
    }

    private void crawlEmbeddedUrlsHeute() throws Exception {
        br.getPage(this.PARAMETER);
        if (br.containsHTML("Der Beitrag konnte nicht gefunden werden") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
            return;
        }
        final String[] ids = this.br.getRegex("\"videoId\"\\s*?:\\s*?\"([^\"]*?)\"").getColumn(0);
        for (final String videoid : ids) {
            /* These urls go back into the decrypter. */
            final String mainlink = "https://www." + this.getHost() + "/nachrichten/heute-journal/" + videoid + ".html";
            decryptedLinks.add(super.createDownloadlink(mainlink));
        }
        return;
    }

    private void crawlEmbeddedUrlsNeoMagazin() throws Exception {
        br.getPage(this.PARAMETER);
        if (br.containsHTML("Der Beitrag konnte nicht gefunden werden") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER_ORIGINAL));
            return;
        }
        final ZdfmediathekConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.ZdfDeMediathek.ZdfmediathekConfigInterface.class);
        final boolean neomagazinroyale_only_add_current_episode = cfg.isNeoMagazinRoyaleDeOnlyGrabCurrentEpisode();
        final String[] htmls = this.br.getRegex("<div[^>]*?class=\"modules\" id=\"teaser\\-\\d+\"[^>]*?>.*?</div>\\s*?</div>\\s*?</div>\\s*?</div>").getColumn(-1);
        for (final String html : htmls) {
            /* These urls go back into the decrypter. */
            final String videoid = new Regex(html, "data\\-sophoraid=\"([^\"]+)\"").getMatch(0);
            final String title = new Regex(html, "class=\"headline\"[^>]*?><h3[^>]*?class=\"h3 zdf\\-\\-primary\\-light\"[^>]*?>([^<>]+)<").getMatch(0);
            if (videoid == null) {
                /* Probably no video content. */
                continue;
            }
            final String mainlink = "https://www.zdf.de/comedy/neo-magazin-mit-jan-boehmermann/" + videoid + ".html";
            /* Check if user only wants current Neo Magazin episode and if we have it. */
            if (neomagazinroyale_only_add_current_episode && title != null && new Regex(title, Pattern.compile(".*?NEO MAGAZIN ROYALE.*?vom.*?", Pattern.CASE_INSENSITIVE)).matches()) {
                /* Clear list */
                decryptedLinks.clear();
                /* Only add this one entry */
                decryptedLinks.add(super.createDownloadlink(mainlink));
                /* Return --> Done */
                return;
            }
            decryptedLinks.add(super.createDownloadlink(mainlink));
        }
        return;
    }

    private void crawlEmbeddedUrlsZdfNew() throws IOException {
        this.br.getPage(this.PARAMETER);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            this.decryptedLinks.add(this.createOfflinelink(this.PARAMETER));
            return;
        }
        final String[] embedded_player_ids = this.br.getRegex("data\\-zdfplayer\\-id=\"([^<>\"]+)\"").getColumn(0);
        for (final String embedded_player_id : embedded_player_ids) {
            final String finallink = String.format("https://www.zdf.de/jdl/jdl/%s.html", embedded_player_id);
            this.decryptedLinks.add(super.createDownloadlink(finallink));
        }
    }

    // private String getApiTokenFromHtml(Browser br, final String url) throws IOException {
    // final Browser brc;
    // if (br == null) {
    // brc = this.br;
    // } else {
    // brc = br.cloneBrowser();
    // brc.setFollowRedirects(true);
    // brc.getPage(url);
    // }
    // String apitoken = brc.getRegex("apiToken\\s*?:\\s*?\\'([a-f0-9]+)\\'").getMatch(0);
    // if (apitoken == null) {
    // apitoken = PluginJSonUtils.getJsonNested(brc, "apiToken");
    // }
    // return apitoken;
    // }
    @SuppressWarnings({ "unchecked" })
    private void getDownloadLinksZdfNew() throws Exception {
        List<String> all_selected_qualities = new ArrayList<String>();
        final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
        final ZdfmediathekConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.ZdfDeMediathek.ZdfmediathekConfigInterface.class);
        grabBest = cfg.isGrabBESTEnabled();
        fastlinkcheck = cfg.isFastLinkcheckEnabled();
        grabSubtitles = cfg.isGrabSubtitleEnabled();
        final boolean grabHlsAudio = cfg.isGrabAudio();
        if (grabHlsAudio) {
            all_selected_qualities.add("hls_aac_0");
        }
        final boolean grabHls170 = cfg.isGrabHLS170pVideoEnabled();
        final boolean grabHls270 = cfg.isGrabHLS270pVideoEnabled();
        final boolean grabHls360 = cfg.isGrabHLS360pVideoEnabled();
        final boolean grabHls480 = cfg.isGrabHLS480pVideoEnabled();
        final boolean grabHls570 = cfg.isGrabHLS570pVideoEnabled();
        final boolean grabHls720 = cfg.isGrabHLS720pVideoEnabled();
        if (grabHls170) {
            all_selected_qualities.add("hls_mp4_170");
        }
        if (grabHls270) {
            all_selected_qualities.add("hls_mp4_270");
        }
        if (grabHls360) {
            all_selected_qualities.add("hls_mp4_360");
        }
        if (grabHls480) {
            all_selected_qualities.add("hls_mp4_480");
        }
        if (grabHls570) {
            all_selected_qualities.add("hls_mp4_570");
        }
        if (grabHls720) {
            all_selected_qualities.add("hls_mp4_720");
        }
        final boolean grabHttpMp4Low = cfg.isGrabHTTPMp4LowVideoEnabled();
        final boolean grabHttpMp4High = cfg.isGrabHTTPMp4HighVideoEnabled();
        final boolean grabHttpMp4VeryHigh = cfg.isGrabHTTPMp4VeryHighVideoEnabled();
        final boolean grabHttpMp4HD = cfg.isGrabHTTPMp4HDVideoEnabled();
        if (grabHttpMp4Low) {
            all_selected_qualities.add("http_mp4_low");
        }
        if (grabHttpMp4High) {
            all_selected_qualities.add("http_mp4_high");
        }
        if (grabHttpMp4VeryHigh) {
            all_selected_qualities.add("http_mp4_veryhigh");
        }
        if (grabHttpMp4HD) {
            all_selected_qualities.add("http_mp4_hd");
        }
        final boolean grabHttpWebmLow = cfg.isGrabHTTPWebmLowVideoEnabled();
        final boolean grabHttpWebmHigh = cfg.isGrabHTTPWebmHighVideoEnabled();
        final boolean grabHttpWebmVeryHigh = cfg.isGrabHTTPWebmVeryHighVideoEnabled();
        final boolean grabHttpWebmHD = cfg.isGrabHTTPWebmHDVideoEnabled();
        if (grabHttpWebmLow) {
            all_selected_qualities.add("http_webm_low");
        }
        if (grabHttpWebmHigh) {
            all_selected_qualities.add("http_webm_high");
        }
        if (grabHttpWebmVeryHigh) {
            all_selected_qualities.add("http_webm_veryhigh");
        }
        if (grabHttpWebmHD) {
            all_selected_qualities.add("http_webm_hd");
        }
        final boolean user_selected_nothing = all_selected_qualities.size() == 0;
        if (user_selected_nothing) {
            logger.info("User selected no quality at all --> Adding ALL qualities instead");
            all_selected_qualities = all_known_qualities;
        }
        /*
         * Grabbing hls means we make an extra http request --> Only do this if wished by the user or if the user set bad plugin settings!
         */
        final boolean grabHLS = grabHlsAudio || grabHls170 || grabHls270 || grabHls360 || grabHls480 || grabHls570 || grabHls720 || user_selected_nothing;
        /*
         * 2017-02-08: The only thing download has and http stream has not == http veryhigh --> Only grab this if user has selected it
         * explicitly!
         */
        boolean grabDownloadUrls = !grabBest && grabHttpMp4HD;
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
            decryptedLinks.add(this.createOfflinelink(PARAMETER));
            return;
        }
        final String apitoken = "d2726b6c8c655e42b68b0db26131b15b22bd1a32";// getApiTokenFromHtml();
        /* 2016-12-21: By hardcoding the apitoken we can save one http request thus have a faster crawl process :) */
        this.br.getHeaders().put("Api-Auth", "Bearer " + apitoken);
        this.br.getPage(API_BASE + "/content/documents/" + sophoraID + ".json?profile=player");
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(PARAMETER));
            return;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
        LinkedHashMap<String, Object> entries_2 = null;
        final String contentType = (String) entries.get("contentType");
        String title = (String) entries.get("title");
        final String editorialDate = (String) entries.get("editorialDate");
        final Object tvStationo = entries.get("tvService");
        final String tvStation = tvStationo != null && tvStationo instanceof String ? (String) tvStationo : "ZDF";
        // final Object hasVideoo = entries.get("hasVideo");
        // final boolean hasVideo = hasVideoo != null && hasVideoo instanceof Boolean ? ((Boolean) entries.get("hasVideo")).booleanValue() :
        // false;
        entries_2 = (LinkedHashMap<String, Object>) entries.get("http://zdf.de/rels/brand");
        final String tvShow = entries_2 != null ? (String) entries_2.get("title") : null;
        entries_2 = (LinkedHashMap<String, Object>) entries.get("mainVideoContent");
        if (entries_2 == null) {
            /* Not a single video? Maybe we have a playlist / embedded video(s)! */
            logger.info("Content is not a video --> Scanning html for embedded content");
            crawlEmbeddedUrlsZdfNew();
            return;
        }
        entries_2 = (LinkedHashMap<String, Object>) entries_2.get("http://zdf.de/rels/target");
        final String id;
        final String player_url_template = (String) entries_2.get("http://zdf.de/rels/streams/ptmd-template");
        /* 2017-02-03: Not required at the moment */
        // if (!hasVideo) {
        // logger.info("Content is not a video --> Nothing to download");
        // return ret;
        // }
        if (inValidate(contentType) || inValidate(title) || inValidate(editorialDate) || inValidate(tvStation) || inValidate(player_url_template)) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        /* Show is not always available - merge it with the title, if tvShow is available. */
        if (tvShow != null) {
            title = tvShow + " - " + title;
        }
        final String date_formatted = new Regex(editorialDate, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        id = new Regex(player_url_template, "/([^/]+)$").getMatch(0);
        if (date_formatted == null || id == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        final String filename_packagename_base_title = String.format("%s_%s_%s", date_formatted, tvStation, title);
        short counter = 0;
        short highestHlsMasterValue = 0;
        short hlsMasterValueTemp = 0;
        int highestHlsBandwidth = 0;
        boolean finished = false;
        boolean grabDownloadUrlsPossible = false;
        DownloadLink highestHlsDownload = null;
        do {
            if (this.isAbort()) {
                return;
            }
            if (counter == 0) {
                accessPlayerJson(player_url_template, "ngplayer_2_3");
            } else if (grabDownloadUrls && grabDownloadUrlsPossible) {
                accessPlayerJson(player_url_template, "zdf_pd_download_1");
                finished = true;
            } else {
                /* Fail safe && case when there are no additional downloadlinks available. */
                finished = true;
                break;
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
            final Object downloadAllowed_o = JavaScriptEngineFactory.walkJson(entries, "attributes/downloadAllowed/value");
            if (downloadAllowed_o != null && downloadAllowed_o instanceof Boolean) {
                /* Usually this is set in the first loop to decide whether a 2nd loop is required. */
                grabDownloadUrlsPossible = ((Boolean) downloadAllowed_o).booleanValue();
            }
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("priorityList");
            final Object subtitleO = JavaScriptEngineFactory.walkJson(entries, "captions/{0}/uri");
            url_subtitle = subtitleO != null ? (String) subtitleO : null;
            if (this.url_subtitle != null & this.grabSubtitles) {
                /* Grab the filesize here once so if the user adds many links, JD will not check the same subtitle URL multiple times. */
                URLConnectionAdapter con = null;
                try {
                    con = this.br.openHeadConnection(this.url_subtitle);
                    if (!con.getContentType().contains("html")) {
                        filesizeSubtitle = con.getLongContentLength();
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            for (final Object priority_o : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) priority_o;
                final ArrayList<Object> formitaeten = (ArrayList<Object>) entries.get("formitaeten");
                for (final Object formitaet_o : formitaeten) {
                    entries = (LinkedHashMap<String, Object>) formitaet_o;
                    final boolean isAdaptive = ((Boolean) entries.get("isAdaptive")).booleanValue();
                    final String type = (String) entries.get("type");
                    String protocol = "http";
                    if (isAdaptive && !type.contains("m3u8")) {
                        /* 2017-02-03: Skip HDS as HLS already contains all segment quelities. */
                        continue;
                    } else if (isAdaptive) {
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
                    if (isAdaptive && !grabHLS) {
                        /* Skip hls if not required by the user. */
                        continue;
                    }
                    final ArrayList<Object> qualities = (ArrayList<Object>) entries.get("qualities");
                    for (final Object qualities_o : qualities) {
                        /* Extra abort handling within here to abort hls decryption as it also needs http requests. */
                        if (this.isAbort()) {
                            return;
                        }
                        entries = (LinkedHashMap<String, Object>) qualities_o;
                        final String quality = (String) entries.get("quality");
                        if (inValidate(quality)) {
                            continue;
                        }
                        entries = (LinkedHashMap<String, Object>) entries.get("audio");
                        final ArrayList<Object> tracks = (ArrayList<Object>) entries.get("tracks");
                        for (final Object tracks_o : tracks) {
                            entries = (LinkedHashMap<String, Object>) tracks_o;
                            final String cdn = (String) entries.get("cdn");
                            final String clss = (String) entries.get("class");
                            final String language = (String) entries.get("language");
                            final long filesize = JavaScriptEngineFactory.toLong(entries.get("filesize"), 0);
                            String uri = (String) entries.get("uri");
                            if (inValidate(cdn) || inValidate(clss) || inValidate(language) || inValidate(uri)) {
                                continue;
                            }
                            String final_download_url;
                            String linkid;
                            String final_filename;
                            final String linkid_format = "%s_%s_%s_%s_%s_%s";
                            final String final_filename_format = "%s_%s_%s.%s";
                            String quality_selector_string = "%s_%s_%s";
                            DownloadLink dl;
                            if (isAdaptive) {
                                /* Segment download */
                                final String hls_master_quality_str = new Regex(uri, "m3u8/(\\d+)/").getMatch(0);
                                if (hls_master_quality_str == null) {
                                    /*
                                     * Fatal failure - without this value we cannot know which hls masters we already crawled and which not
                                     * resulting in unnecessary http requests!
                                     */
                                    continue;
                                }
                                hlsMasterValueTemp = Short.parseShort(hls_master_quality_str);
                                if (hlsMasterValueTemp <= highestHlsMasterValue) {
                                    /* Skip hls masters which we have already decrypted. */
                                    continue;
                                }
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
                                    final_download_url = hlscontainer.getDownloadurl();
                                    ext = hlscontainer.getFileExtension().replace(".", "");
                                    linkid = String.format(linkid_format, id, type, cdn, language, protocol, resolution);
                                    final_filename = encodeUnicode(String.format(final_filename_format, filename_packagename_base_title, protocol, resolution, ext));
                                    dl = createDownloadlink(final_download_url);
                                    if (hlscontainer.getBandwidth() > highestHlsBandwidth) {
                                        /*
                                         * While adding the URLs, let's find the BEST quality url. In case we need it later we will already
                                         * know which one is the BEST.
                                         */
                                        highestHlsBandwidth = hlscontainer.getBandwidth();
                                        highestHlsDownload = dl;
                                    }
                                    setDownloadlinkProperties(dl, date_formatted, final_filename, type, linkid);
                                    dl.setProperty("hlsBandwidth", hlscontainer.getBandwidth());
                                    if (duration > 0 && hlscontainer.getBandwidth() > 0) {
                                        dl.setDownloadSize(duration / 1000 * hlscontainer.getBandwidth() / 8);
                                    }
                                    all_found_downloadlinks.put(String.format(quality_selector_string, protocol, ext, height_for_quality_selection), dl);
                                }
                                /* Set this so we do not crawl this particular hls master again next round. */
                                highestHlsMasterValue = hlsMasterValueTemp;
                            } else {
                                /* http download */
                                final_download_url = uri;
                                linkid = String.format(linkid_format, id, type, cdn, language, protocol, quality);
                                final_filename = encodeUnicode(String.format(final_filename_format, filename_packagename_base_title, protocol, quality, ext));
                                /* TODO: Check if this might still be useful ... */
                                // boolean isHBBTV = false;
                                // final String fixme = new Regex(uri,
                                // "https?://(?:www\\.)?metafilegenerator\\.de/ondemand/zdf/hbbtv/([A-Za-z0-9]+/zdf/\\d+/\\d+/[^<>\"]+\\.mp4)").getMatch(0);
                                // if (fixme != null) {
                                // /* E.g. http://rodl.zdf.de/none/zdf/16/03/160304_top_mom_2328k_p35v12.mp4 */
                                // /* Fix invalid / unauthorized hbbtv urls so that we get downloadable http urls */
                                // uri = "http://rodl.zdf.de/" + fixme;
                                // isHBBTV = true;
                                // }
                                dl = createDownloadlink(final_download_url);
                                /* Usually the filesize is only given for the official downloads. */
                                if (filesize > 0) {
                                    dl.setAvailable(true);
                                    dl.setDownloadSize(filesize);
                                }
                                setDownloadlinkProperties(dl, date_formatted, final_filename, type, linkid);
                                all_found_downloadlinks.put(String.format(quality_selector_string, protocol, ext, quality), dl);
                            }
                        }
                    }
                }
            }
            counter++;
        } while (!finished);
        /* Finally, check which qualities the user actually wants to have. */
        if (this.grabBest && highestHlsDownload != null) {
            /* Best is easy and even if it was an unknown quality, we knew that highest hls == always BEST! */
            addDownloadLink(highestHlsDownload);
        } else {
            boolean atLeastOneSelectedItemExists = false;
            final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
            HashMap<String, DownloadLink> all_selected_downloadlinks = new HashMap<String, DownloadLink>();
            final Iterator<Entry<String, DownloadLink>> iterator_all_found_downloadlinks = all_found_downloadlinks.entrySet().iterator();
            while (iterator_all_found_downloadlinks.hasNext()) {
                final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                final String dl_quality_string = dl_entry.getKey();
                if (all_selected_qualities.contains(dl_quality_string)) {
                    atLeastOneSelectedItemExists = true;
                    all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                } else if (!all_known_qualities.contains(dl_quality_string) && grabUnknownQualities) {
                    logger.info("Found unknown quality: " + dl_quality_string);
                    if (grabUnknownQualities) {
                        logger.info("Adding unknown quality: " + dl_quality_string);
                        all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                    }
                }
            }
            if (!atLeastOneSelectedItemExists) {
                logger.info("Possible user error: User selected only qualities which are not available --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    decryptedLinks.add(dl_entry.getValue());
                }
            } else {
                if (cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled()) {
                    all_selected_downloadlinks = findBESTInsideGivenMap(all_selected_downloadlinks);
                }
                /* Finally add selected URLs */
                final Iterator<Entry<String, DownloadLink>> it = all_selected_downloadlinks.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, DownloadLink> entry = it.next();
                    final DownloadLink dl = entry.getValue();
                    addDownloadLink(dl);
                }
            }
        }
        if (all_found_downloadlinks.isEmpty()) {
            logger.info("Failed to find any quality at all");
        }
        if (decryptedLinks.size() > 1) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(filename_packagename_base_title);
            fp.addLinks(decryptedLinks);
        }
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

    private void addDownloadLink(final DownloadLink dl) {
        decryptedLinks.add(dl);
        if (grabSubtitles && this.url_subtitle != null) {
            final String current_ext = dl.getFinalFileName().substring(dl.getFinalFileName().lastIndexOf("."));
            final String final_filename = dl.getFinalFileName().replace(current_ext, ".xml");
            final String linkid = dl.getLinkID() + "_subtitle";
            final DownloadLink dl_subtitle = this.createDownloadlink(this.url_subtitle);
            setDownloadlinkProperties(dl_subtitle, dl.getStringProperty("date", null), final_filename, "subtitle", linkid);
            if (filesizeSubtitle > 0) {
                dl_subtitle.setDownloadSize(filesizeSubtitle);
                dl_subtitle.setAvailable(true);
            }
            decryptedLinks.add(dl_subtitle);
        }
    }

    private void setDownloadlinkProperties(final DownloadLink dl, final String date_formatted, final String final_filename, final String type, final String linkid) {
        dl.setFinalFileName(final_filename);
        dl.setLinkID(linkid);
        dl.setProperty("date", date_formatted);
        dl.setProperty("directName", final_filename);
        dl.setProperty("streamingType", type);
        dl.setContentUrl(PARAMETER_ORIGINAL);
    }

    private void accessPlayerJson(final String player_url_template, final String playerID) throws IOException {
        /* E.g. "/tmd/2/{playerId}/vod/ptmd/mediathek/161215_sendungroyale065ddm_nmg" */
        String player_url = player_url_template.replace("{playerId}", playerID);
        if (player_url.startsWith("/")) {
            player_url = API_BASE + player_url;
        }
        this.br.getPage(player_url);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}