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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.YoutubeDashV2;
import jd.plugins.hoster.ZdfDeMediathek;
import jd.plugins.hoster.ZdfDeMediathek.ZdfmediathekConfigInterface;
import jd.plugins.hoster.ZdfDeMediathek.ZdfmediathekConfigInterface.SubtitleType;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zdf.de", "3sat.de", "phoenix.de" }, urls = { "https?://(?:www\\.)?zdf\\.de/(?:.+/)?[A-Za-z0-9_\\-]+\\.html|https?://(?:www\\.)?zdf\\.de/uri/(?:syncvideoimport_beitrag_\\d+|transfer_SCMS_[a-f0-9\\-]+|[a-z0-9\\-]+)", "https?://(?:www\\.)?3sat\\.de/.+/[A-Za-z0-9_\\-]+\\.html|https?://(?:www\\.)?3sat\\.de/uri/(?:syncvideoimport_beitrag_\\d+|transfer_SCMS_[a-f0-9\\-]+|[a-z0-9\\-]+)", "https?://(?:www\\.)?phoenix\\.de/(?:.*?-\\d+\\.html.*|podcast/[A-Za-z0-9]+/video/rss\\.xml)" })
public class ZDFMediathekDecrypter extends PluginForDecrypt {
    private boolean             fastlinkcheck             = false;
    private final String        TYPE_ZDF                  = "https?://(?:www\\.)?(?:zdf\\.de|3sat\\.de)/.+";
    private static final String TYPE_PHOENIX              = "https?://(?:www\\.)?phoenix\\.de/.*-(\\d+)\\.html.*";
    private final String        TYPE_PHOENIX_RSS          = "http://(?:www\\.)?phoenix\\.de/podcast/.+";
    /* Not sure where these URLs come from. Probably old RSS readers via old APIs ... */
    private final String        TYPER_ZDF_REDIRECT        = "https?://[^/]+/uri/.+";
    private List<String>        userSelectedSubtitleTypes = new ArrayList<String>();
    private Map<String, String> subtitlesXML              = new HashMap<String, String>();
    private Map<String, String> subtitlesVTT              = new HashMap<String, String>();

    public ZDFMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /** Example of a podcast-URL: http://www.zdf.de/ZDFmediathek/podcast/1074856?view=podcast */
    /** Related sites: see RegExes, and also: 3sat.de */
    @SuppressWarnings({ "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.br.setAllowedResponseCodes(new int[] { 500 });
        setBrowserExclusive();
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_PHOENIX)) {
            return crawlPhoenix(param);
        } else if (param.getCryptedUrl().matches(TYPE_PHOENIX_RSS)) {
            return crawlPhoenixRSS(param);
        } else {
            return crawlZdfNew(param);
        }
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal("api.zdf.de", 750);
        Browser.setRequestIntervalLimitGlobal("api.3sat.de", 750);
    }

    private static enum QUALITY {
        LOW,
        MEDIUM,
        HIGH,
        VERYHIGH,
        HD,
        FHD,
        UHD
    }

    private final static Map<String, List<String[]>> QUALITIES_MAP = new HashMap<String, List<String[]>>();
    static {
        QUALITIES_MAP.put("v11", Arrays.asList(new String[][] { new String[] { "1456k_p13", QUALITY.MEDIUM.name() }, new String[] { "2256k_p14", QUALITY.VERYHIGH.name() }, new String[] { "2328k_p35", QUALITY.VERYHIGH.name() } }));
        QUALITIES_MAP.put("v12", Arrays.asList(new String[][] { new String[] { "1456k_p13", QUALITY.MEDIUM.name() }, new String[] { "2256k_p14", QUALITY.VERYHIGH.name() }, new String[] { "2328k_p35", QUALITY.VERYHIGH.name() }, new String[] { "3256k_p15", QUALITY.HD.name() }, new String[] { "3328k_p36", QUALITY.HD.name() } }));
        QUALITIES_MAP.put("v13", Arrays.asList(new String[][] { new String[] { "1496k_p13", QUALITY.MEDIUM.name() }, new String[] { "2296k_p14", QUALITY.VERYHIGH.name() }, new String[] { "2328k_p35", QUALITY.VERYHIGH.name() }, new String[] { "3296k_p15", QUALITY.HD.name() }, new String[] { "3328k_p36", QUALITY.HD.name() } }));
        QUALITIES_MAP.put("v14", Arrays.asList(new String[][] { new String[] { "1496k_p13", QUALITY.MEDIUM.name() }, new String[] { "2296k_p14", QUALITY.VERYHIGH.name() }, new String[] { "2328k_p35", QUALITY.VERYHIGH.name() }, new String[] { "3328k_p35", QUALITY.HD.name() }, new String[] { "3328k_p36", QUALITY.HD.name() } }));
        QUALITIES_MAP.put("v15", Arrays.asList(new String[][] { new String[] { "1628k_p13", QUALITY.MEDIUM.name() }, new String[] { "2360k_p35", QUALITY.VERYHIGH.name() }, new String[] { "3360k_p36", QUALITY.HD.name() } }));
        /*
         * new String[] { "508k_p9", QUALITY.LOW.name() }
         *
         * new String[] { "808k_p11", QUALITY.HIGH.name() }
         */
        QUALITIES_MAP.put("v17", Arrays.asList(new String[][] { new String[] { "1628k_p13", QUALITY.MEDIUM.name() }, new String[] { "2360k_p35", QUALITY.VERYHIGH.name() }, new String[] { "3360k_p36", QUALITY.HD.name() }, new String[] { "6628k_p61", QUALITY.FHD.name() }, new String[] { "6660k_p37", QUALITY.FHD.name() } }));
    }

    public static List<String[]> getBetterQualities(final String url) {
        final String base[] = new Regex(url, "((\\d{3,4}k_p\\d{1,2})(v\\d{2})\\.mp4)", Pattern.CASE_INSENSITIVE).getRow(0);
        if (base != null && base.length == 3) {
            final String qualityModifierComplete = base[0];
            final String bitrateAndP = base[1];
            final String version = base[2];
            final List<String[]> qualities = QUALITIES_MAP.get(version.toLowerCase(Locale.ENGLISH));
            if (qualities != null) {
                boolean unknownQuality = false;
                while (true) {
                    final Iterator<String[]> it = qualities.iterator();
                    while (it.hasNext()) {
                        String next[] = unknownQuality ? null : it.next();
                        String thisBitrateAndP = next != null ? next[0] : null;
                        /* Find list where first item equals */
                        if (thisBitrateAndP == null || thisBitrateAndP.equalsIgnoreCase(bitrateAndP)) {
                            final List<String[]> ret = new ArrayList<String[]>();
                            while (it.hasNext()) {
                                next = it.next();
                                thisBitrateAndP = next[0];
                                final String nextURL = url.replaceFirst("(?i)" + Pattern.quote(qualityModifierComplete), thisBitrateAndP + version + ".mp4");
                                ret.add(new String[] { nextURL, next[1].toLowerCase(Locale.ENGLISH) });
                            }
                            if (ret.size() > 0) {
                                /* Reverse sort so highest quality is on the beginning */
                                Collections.reverse(ret);
                                return ret;
                            } else {
                                return null;
                            }
                        }
                    }
                    if (unknownQuality == false) {
                        unknownQuality = true;
                    } else {
                        break;
                    }
                }
            }
        }
        return null;
    }

    private List<String> getKnownQualityIdentifiers() {
        /** Returns all possible quality identifier strings in order highest --> lowest */
        final List<String> all_known_qualities = new ArrayList<String>();
        final String[] knownProtocols = { "http", "hls" };
        /** 2021-02-01: Removed all .webm qualities from settings */
        final String[] knownExtensions = { "mp4", "webm" };
        /* 2021-07-22: medium and high are swapped --> https://svn.jdownloader.org/issues/89857 */
        final String[] knownQualityNames = { "uhd", "fhd", "1080", "hd", "veryhigh", "720", "480", "360", "medium", "high", "low", "170" };
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

    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink dl = super.createDownloadlink(url.replaceAll("https?://", "decryptedmediathek://"));
        if (this.fastlinkcheck) {
            dl.setAvailable(true);
        }
        return dl;
    }

    /**
     * Do not delete this code! This can crawl embedded ZDF IDs!
     *
     * @throws PluginException
     */
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
    private ArrayList<DownloadLink> crawlEmbeddedUrlsZdfNew(final CryptedLink param, final String apiToken) throws IOException, PluginException {
        final ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
        final GetRequest request = br.createGetRequest(param.getCryptedUrl());
        request.getHeaders().put("Api-Auth", "Bearer " + apiToken);
        br.getPage(request);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
    private String[] getApiParams(final Browser br, final String url) throws IOException, PluginException {
        final Browser brc;
        if (br == null) {
            brc = this.br;
        } else if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            brc.getPage(url);
        }
        String apitoken = brc.getRegex("\"apiToken\"\\s*:\\s*\"([^\"\\']+)").getMatch(0);
        if (apitoken == null) {
            apitoken = brc.getRegex("apiToken\\s*:\\s*'([^\"\\']+)").getMatch(0);
        }
        String api_base = brc.getRegex("apiService\\s*:\\s*'(https?://[^<>\"\\']+)").getMatch(0);
        String embed_content = brc.getRegex("\"embed_content\"\\s*:\\s*\"(/.*?)\"").getMatch(0);
        if (embed_content == null) {
            embed_content = brc.getRegex("embed_content\\s*:\\s*'([^\"\\']+)").getMatch(0);
        }
        String config = brc.getRegex("\"config\"\\s*:\\s*\"(https?://.*?)\"").getMatch(0);
        if (config == null) {
            config = brc.getRegex("player\\s*:\\s*\\{[^\\}]*js\\s*:\\s*'(https?://[^\"\\']+)").getMatch(0);
        }
        String profile = brc.getRegex("\\.json\\?profile=([^\"]+)\"").getMatch(0);
        if (config != null) {
            brc.getPage(config);
            String tmp = brc.getRegex("\"apiProfile\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            if (tmp == null) {
                tmp = brc.getRegex("apiProfile\\s*:\\s*(?:\"|')([^\"\\']+)").getMatch(0);
            }
            if (tmp == null) {
                tmp = brc.getRegex("DEFAULT_API_PROFILE\\s*=\\s*(?:\"|')([^\"\\']+)").getMatch(0);
            }
            if (tmp != null) {
                profile = tmp;
            }
        }
        if (apitoken == null || api_base == null || profile == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return new String[] { apitoken, api_base, profile, embed_content };
        }
    }

    private ArrayList<DownloadLink> crawlPhoenixRSS(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String date_general = getXML("pubDate");
        String title_general = getXML("title");
        final String[] items = br.getRegex("<item>(.*?)</item>").getColumn(0);
        if (items == null || items.length == 0 || title_general == null || date_general == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String fpname = formatDatePHOENIX(date_general) + "_" + title_general;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpname);
        for (final String item : items) {
            String url = new Regex(item, "<enclosure[^>]*type='video/mp4'[^>]*url='(https://[^\\']+)' />").getMatch(0);
            if (url == null) {
                url = getXML(item, "guid");
            }
            final String title = getXML(item, "title");
            final String description = getXML(item, "description");
            final String date = getXML(item, "pubDate");
            final String tvstation = getXML(item, "itunes:author");
            final String filesize = new Regex(item, "length=\\'(\\d+)\\'").getMatch(0);
            if (url == null || title == null || date == null || tvstation == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink dl = super.createDownloadlink("directhttp://" + url);
            String final_filename = formatDatePHOENIX(date) + "_" + tvstation + "_" + title + ".mp4";
            if (description != null) {
                dl.setComment(description);
            }
            dl.setProperty("date", date);
            dl.setFinalFileName(final_filename);
            dl.setDownloadSize(Long.parseLong(filesize));
            dl.setAvailable(true);
            ret.add(dl);
        }
        fp.addLinks(ret);
        return ret;
    }

    private ArrayList<DownloadLink> crawlPhoenix(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String phoenixContentID = new Regex(param.getCryptedUrl(), TYPE_PHOENIX).getMatch(0);
        if (phoenixContentID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String urlBase = "https://www.phoenix.de";
        br.getPage(urlBase + "/response/id/" + phoenixContentID);
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Map<String, Object>> videos = (List<Map<String, Object>>) entries.get("absaetze");
        int numberofSkippedItems = 0;
        int index = 0;
        for (final Map<String, Object> video : videos) {
            logger.info("Processing item " + (index + 1) + "/" + videos.size());
            final String type = (String) video.get("typ");
            if (type.equals("video-youtube")) {
                /* Embedded youtube video */
                final String youtubeVideoID = (String) video.get("id");
                ret.add(super.createDownloadlink(YoutubeDashV2.generateContentURL(youtubeVideoID)));
            } else if (type.equals("video-smubl")) {
                /* "Selfhosted" content --> Hosted on zdf.de */
                final String zdfContentID = video.get("content").toString();
                br.getPage(urlBase + "/php/mediaplayer/data/beitrags_details.php?id=" + zdfContentID + "&profile=player2");
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                ret.addAll(handleZdfJson(param, br, null));
            } else {
                numberofSkippedItems++;
                logger.warning("Skipping unsupported type?! --> " + type);
            }
            index++;
        }
        if (numberofSkippedItems == videos.size()) {
            logger.info("Failed to find any downloadable content");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlZdfNew(final CryptedLink param) throws Exception {
        final String sophoraIDSource;
        if (param.getCryptedUrl().matches(TYPER_ZDF_REDIRECT)) {
            this.br.setFollowRedirects(false);
            this.br.getPage(param.getCryptedUrl());
            sophoraIDSource = this.br.getRedirectLocation();
            if (sophoraIDSource == null) {
                /* Probably offline content */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.br.setFollowRedirects(true);
        } else {
            sophoraIDSource = param.getCryptedUrl();
        }
        String sophoraID = new Regex(sophoraIDSource, "/([^/]+)\\.html").getMatch(0);
        if (sophoraID == null) {
            /* Probably no videocontent - most likely, used added an invalid TYPER_ZDF_REDIRECT url. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String apiParams[] = getApiParams(br, param.getCryptedUrl());
        /* 2016-12-21: By hardcoding the apitoken we can save one http request thus have a faster crawl process :) */
        if (apiParams[3] != null) {
            sophoraID = apiParams[3];
        }
        final GetRequest request = br.createGetRequest(apiParams[1] + "/content/documents/" + sophoraID + ".json?profile=" + apiParams[2]);
        request.getHeaders().put("Api-Auth", "Bearer " + apiParams[0]);
        this.br.getPage(request);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return handleZdfJson(param, br, apiParams[0]);
    }

    /** Handles ZDF json present in given browser after API request has been made before. */
    private ArrayList<DownloadLink> handleZdfJson(final CryptedLink param, final Browser br, final String apiToken) throws Exception {
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        final Map<String, Object> mainVideoContent = (Map<String, Object>) entries.get("mainVideoContent");
        if (mainVideoContent == null) {
            /* Not a single video? Maybe we have a playlist / embedded video(s)! */
            logger.info("Content is not a video --> Scanning html for embedded content");
            final ArrayList<DownloadLink> results = crawlEmbeddedUrlsZdfNew(param, apiToken);
            if (results.size() == 0) {
                results.add(this.createOfflinelink(param.getCryptedUrl(), "NO_DOWNLOADABLE_CONTENT"));
            }
            return results;
        }
        final Map<String, Object> relsTarget = (Map<String, Object>) mainVideoContent.get("http://zdf.de/rels/target");
        String streamsJsonURL;
        String downloadsJsonURL = null;
        final Map<String, Object> streamInfoMap;
        if (relsTarget.containsKey("streams")) {
            /* zdf.de */
            streamInfoMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(relsTarget, "streams", "default");
            final String player_url_template = (String) streamInfoMap.get("http://zdf.de/rels/streams/ptmd-template");
            /* E.g. "/tmd/2/{playerId}/vod/ptmd/mediathek/161215_sendungroyale065ddm_nmg" */
            /** 2024-02-21: Possible values: zdf_pd_download_1, ngplayer_2_3, android_native_5 */
            /* Regarding android_native_5: See also: https://github.com/mediathekview/MServer/issues/592 */
            streamsJsonURL = player_url_template.replace("{playerId}", "android_native_5");
            downloadsJsonURL = player_url_template.replace("{playerId}", "zdf_pd_download_1");
        } else {
            /* phoenix.de: Only streams given and URL is pre-formatted */
            streamInfoMap = relsTarget;
            streamsJsonURL = (String) streamInfoMap.get("http://zdf.de/rels/streams/ptmd-template");
            streamsJsonURL = streamsJsonURL.replace("{playerId}", "android_native_5");
        }
        String title = (String) entries.get("title");
        final String description = (String) entries.get("leadParagraph");
        final String editorialDate = (String) entries.get("editorialDate");
        final Object tvStationo = entries.get("tvService");
        final String tv_station = tvStationo != null && tvStationo instanceof String ? (String) tvStationo : "ZDF";
        final Map<String, Object> seriesProgrammeItem0 = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "programmeItem/{0}");
        final Map<String, Object> seriesMetadata = seriesProgrammeItem0 != null ? (Map<String, Object>) seriesProgrammeItem0.get("http://zdf.de/rels/target") : null;
        final Map<String, Object> seriesSeasonInfo = seriesMetadata != null ? (Map<String, Object>) seriesMetadata.get("http://zdf.de/rels/cmdm/season") : null;
        final String seriesTitle = seriesMetadata != null ? (String) seriesMetadata.get("title") : null;
        String tv_show = (String) JavaScriptEngineFactory.walkJson(entries, "http://zdf.de/rels/brand", "title");
        if (seriesTitle != null) {
            tv_show = seriesTitle;
        }
        final Number seriesSeasonNumber = seriesSeasonInfo != null ? (Number) seriesSeasonInfo.get("seasonNumber") : null;
        final Number seriesEpisodeNumber = seriesMetadata != null ? (Number) seriesMetadata.get("episodeNumber") : null;
        if (tv_show != null && seriesSeasonNumber != null && seriesEpisodeNumber != null) {
            final DecimalFormat df = new DecimalFormat("00");
            final String seasonEpisodeString = "S" + df.format(seriesSeasonNumber.intValue()) + "E" + df.format(seriesEpisodeNumber);
            title = tv_show + " " + seasonEpisodeString + " - " + title;
        } else if (tv_show != null) {
            title = tv_show + " - " + title;
        }
        final String date_formatted = new Regex(editorialDate, "(\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        if (date_formatted == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /** Now collect all user selected qualities. */
        final List<String> allKnownQualities = this.getKnownQualityIdentifiers();
        final ArrayList<DownloadLink> allSelectedDownloadlinks = new ArrayList<DownloadLink>();
        final List<String> selectedQualityStringsTmp = new ArrayList<String>();
        final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
        final ZdfmediathekConfigInterface cfg = PluginJsonConfig.get(ZdfmediathekConfigInterface.class);
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
        if (cfg.isGrabAudio()) {
            selectedQualityStringsTmp.add("hls_aac_0");
        }
        if (cfg.isGrabHLS170pVideoEnabled()) {
            selectedQualityStringsTmp.add("hls_mp4_170");
        }
        if (cfg.isGrabHLS270pVideoEnabled()) {
            selectedQualityStringsTmp.add("hls_mp4_270");
        }
        if (cfg.isGrabHLS360pVideoEnabled()) {
            selectedQualityStringsTmp.add("hls_mp4_360");
        }
        if (cfg.isGrabHLS480pVideoEnabled()) {
            selectedQualityStringsTmp.add("hls_mp4_480");
        }
        if (cfg.isGrabHLS570pVideoEnabled()) {
            selectedQualityStringsTmp.add("hls_mp4_570");
        }
        if (cfg.isGrabHLS720pVideoEnabled()) {
            selectedQualityStringsTmp.add("hls_mp4_720");
        }
        if (cfg.isGrabHLS1080pVideoEnabled()) {
            selectedQualityStringsTmp.add("hls_mp4_1080");
        }
        boolean grabOfficialDownloadUrls = false;
        final int selectedQualityStringsTmpLengthOld = selectedQualityStringsTmp.size();
        if (cfg.isGrabHTTPMp4LowVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_low");
        }
        if (cfg.isGrabHTTPWebmLowVideoEnabled()) {
            selectedQualityStringsTmp.add("http_webm_low");
        }
        if (cfg.isGrabHTTPMp4MediumVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_medium");
        }
        if (cfg.isGrabHTTPWebmMediumVideoEnabled()) {
            selectedQualityStringsTmp.add("http_webm_medium");
        }
        if (cfg.isGrabHTTPMp4HighVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_high");
        }
        if (cfg.isGrabHTTPWebmHighVideoEnabled()) {
            selectedQualityStringsTmp.add("http_webm_high");
        }
        if (cfg.isGrabHTTPMp4VeryHighVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_veryhigh");
        }
        if (cfg.isGrabHTTPWebmVeryHighVideoEnabled()) {
            selectedQualityStringsTmp.add("http_webm_veryhigh");
        }
        if (cfg.isGrabHTTPMp4HDVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_hd");
        }
        if (cfg.isGrabHTTPWebmHDVideoEnabled()) {
            selectedQualityStringsTmp.add("http_webm_hd");
        }
        if (cfg.isGrabHTTPMp4FHDVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_fhd");
        }
        if (cfg.isGrabHTTPWebmFHDVideoEnabled()) {
            selectedQualityStringsTmp.add("http_webm_fhd");
        }
        if (cfg.isGrabHTTPMp4UHDVideoEnabled()) {
            selectedQualityStringsTmp.add("http_mp4_uhd");
        }
        if (cfg.isGrabHTTPWebmUHDVideoEnabled()) {
            selectedQualityStringsTmp.add("http_webm_uhd");
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
        boolean grabHTTPMp4 = false;
        boolean grabHTTPWebm = false;
        boolean grabHLSVideo = false;
        boolean grabHLSAudio = false;
        for (final String selectedQualityTmp : selectedQualityStringsTmp) {
            if (selectedQualityTmp.startsWith("http_mp4")) {
                grabHTTPMp4 = true;
            } else if (selectedQualityTmp.startsWith("http_webm")) {
                grabHTTPWebm = true;
            } else if (selectedQualityTmp.startsWith("hls_webm") || selectedQualityTmp.startsWith("hls_mp4")) {
                grabHLSVideo = true;
            } else if (selectedQualityTmp.startsWith("hls_aac")) {
                grabHLSAudio = true;
            }
        }
        boolean grabHLS = grabHLSAudio || (grabHLSVideo && !grabBest);
        final Map<String, List<Object>> audioVideoMap = new HashMap<String, List<Object>>();
        final String filename_packagename_base_title = date_formatted + "_" + tv_station + "_" + title;
        boolean grabDownloadUrlsPossible = false;
        final List<String> hlsDupeArray = new ArrayList<String>();
        boolean atLeastOneSelectedVideoAudioVersionIsAvailable = false;
        final HashSet<String> crawledTypes = new HashSet<String>();
        String internalVideoID = null;
        do {
            /**
             * TODO: Maybe prefer official download over stream download if available. Check if there are disadvantages e.g. lower quality
             * when doing so!
             */
            if (streamsJsonURL != null && crawledTypes.add(streamsJsonURL)) {
                /* First round: Grab streams */
                logger.info("Crawling stream URLs");
                final GetRequest request = br.createGetRequest(streamsJsonURL);
                request.getHeaders().put("Api-Auth", "Bearer " + apiToken);
                br.getPage(request);
            } else if (grabOfficialDownloadUrls && grabDownloadUrlsPossible && downloadsJsonURL != null && crawledTypes.add(downloadsJsonURL)) {
                /* 2nd round: Grab ffficial video download URLs if possible and wanted by the user */
                logger.info("Crawling download URLs");
                final GetRequest request = br.createGetRequest(downloadsJsonURL);
                request.getHeaders().put("Api-Auth", "Bearer " + apiToken);
                br.getPage(request);
            } else {
                /* Fail safe && case when there are no additional downloadlinks available. */
                logger.info("Stopping as we've crawled all qualities");
                break;
            }
            final Map<String, Object> player = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            logger.info("Crawling playerId: " + player.get("playerId"));
            if (internalVideoID == null) {
                /* Set this on our first round */
                internalVideoID = (String) player.get("basename");
            }
            /* 1. Collect subtitles */
            final Object captionsO = JavaScriptEngineFactory.walkJson(player, "captions");
            if (captionsO instanceof List && this.subtitlesXML.isEmpty()) {
                /* Captions can be available in different versions (languages- and types) */
                final List<Object> subtitlesO = (List<Object>) player.get("captions");
                Map<String, Object> subInfo = null;
                for (final Object subtitleO : subtitlesO) {
                    subInfo = (Map<String, Object>) subtitleO;
                    final String subtitleType = (String) subInfo.get("class");
                    final String uri = (String) subInfo.get("uri");
                    /* E.g. "ebu-tt-d-basic-de" or "webvtt" */
                    // final String format = (String) subInfo.get("format");
                    /* Skip unsupported formats */
                    if (uri.toLowerCase(Locale.ENGLISH).endsWith(".xml")) {
                        subtitlesXML.put(subtitleType, uri);
                    } else if (uri.toLowerCase(Locale.ENGLISH).endsWith(".vtt")) {
                        subtitlesVTT.put(subtitleType, uri);
                    } else {
                        logger.info("Detected unsupported subtitle-format: " + uri);
                        continue;
                    }
                }
            }
            /* 2. Grab video streams */
            final Object downloadAllowed_o = JavaScriptEngineFactory.walkJson(player, "attributes/downloadAllowed/value");
            if (downloadAllowed_o != null && downloadAllowed_o instanceof Boolean) {
                /* Are official video download-URLs existent? */
                grabDownloadUrlsPossible = ((Boolean) downloadAllowed_o).booleanValue();
            }
            final List<Map<String, Object>> priorityList = (List<Map<String, Object>>) player.get("priorityList");
            for (final Map<String, Object> priority : priorityList) {
                final List<Map<String, Object>> formitaeten = (List<Map<String, Object>>) priority.get("formitaeten");
                for (final Map<String, Object> formitaet : formitaeten) {
                    final boolean isAdaptive = ((Boolean) formitaet.get("isAdaptive")).booleanValue();
                    final String type = (String) formitaet.get("type");
                    /* Process qualities */
                    final List<Map<String, Object>> qualities = (List<Map<String, Object>>) formitaet.get("qualities");
                    for (final Map<String, Object> quality : qualities) {
                        final Map<String, Object> audio = (Map<String, Object>) quality.get("audio");
                        final List<Map<String, Object>> tracks = (List<Map<String, Object>>) audio.get("tracks");
                        for (final Object trackO : tracks) {
                            final Map<String, Object> track = (Map<String, Object>) trackO;
                            final String audio_class = (String) track.get("class");
                            if (!atLeastOneSelectedVideoAudioVersionIsAvailable && selectedAudioVideoVersions.contains(audio_class)) {
                                atLeastOneSelectedVideoAudioVersionIsAvailable = true;
                            }
                            final List<Object> qualitiesForThisLang;
                            if (audioVideoMap.containsKey(audio_class)) {
                                qualitiesForThisLang = audioVideoMap.get(audio_class);
                            } else {
                                qualitiesForThisLang = new ArrayList<Object>();
                                audioVideoMap.put(audio_class, qualitiesForThisLang);
                            }
                            track.put("isAdaptive", isAdaptive);
                            track.put("type", type);
                            track.put("quality", quality.get("quality"));
                            qualitiesForThisLang.add(track);
                        }
                    }
                }
            }
        } while (!this.isAbort());
        final ArrayList<DownloadLink> allDownloadLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> directurlDupesList = new ArrayList<String>();
        final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
        final ArrayList<DownloadLink> userSelectedQualities = new ArrayList<DownloadLink>();
        final Iterator<Entry<String, List<Object>>> iterator = audioVideoMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<String, List<Object>> entry = iterator.next();
            final String audio_class = entry.getKey();
            /* Skip here if we're allowed to -> Skips crawl processes of HLS versions for unwanted video-versions -> Saves time */
            if (atLeastOneSelectedVideoAudioVersionIsAvailable && !selectedAudioVideoVersions.contains(audio_class)) {
                logger.info("Skipping audioVideoVersion: " + audio_class);
                continue;
            }
            final ArrayList<DownloadLink> userSelectedQualitiesTmp = new ArrayList<DownloadLink>();
            final HashMap<String, DownloadLink> selectedQualitiesMapTmp = new HashMap<String, DownloadLink>();
            final List<Object> qualitiesList = entry.getValue();
            DownloadLink highestHLS = null;
            DownloadLink highestHTTPMp4 = null;
            DownloadLink highestHTTPWebm = null;
            int highestHlsBandwidth = 0;
            int highestHTTPMp4QualityValue = 0;
            int highestHTTPWebmQualityValue = 0;
            final Set<String> httpQualities = new HashSet<String>();
            for (final Object qualityO : qualitiesList) {
                final Map<String, Object> qualitymap = (Map<String, Object>) qualityO;
                final boolean isAdaptive = ((Boolean) qualitymap.get("isAdaptive"));
                final String type = (String) qualitymap.get("type");
                /* First check for global skip conditions */
                if (isAdaptive && !type.contains("m3u8")) {
                    /* 2017-02-03: Skip HDS as HLS already contains all segment qualities. */
                    continue;
                }
                /* Now set some properties that are relevant for all items that are processed in this loop. */
                String ext;
                if (type.contains("webm")) {
                    /* http webm streams. */
                    ext = "webm";
                } else if (type.contains("vorbis")) {
                    /* http webm streams. */
                    ext = "webm";
                } else {
                    /* http mp4- and segment streams. */
                    ext = "mp4";
                }
                final String language = (String) qualitymap.get("language");
                String uri = (String) qualitymap.get("uri");
                if (StringUtils.isEmpty(audio_class) || StringUtils.isEmpty(language) || StringUtils.isEmpty(uri)) {
                    /* Skip invalid objects */
                    continue;
                }
                /* Often the same quality is available twice: Via https://nrodlzdf-a.akamaihd.net/ and https://rodlzdf-a.akamaihd.net/ */
                final String uriDeduped = uri.replaceFirst("(?i)https://[^/]+/", "");
                if (directurlDupesList.contains(uriDeduped)) {
                    continue;
                }
                directurlDupesList.add(uriDeduped);
                final String cdn = (String) qualitymap.get("cdn");
                long filesize = JavaScriptEngineFactory.toLong(qualitymap.get("filesize"), 0);
                final String audio_class_user_readable = convertInternalAudioClassToUserReadable(audio_class);
                String linkid;
                String final_filename;
                /* internal_videoid, type, cdn, language, audio_class, protocol, resolution */
                final String linkid_format = "%s_%s_%s_%s_%s_%s_%s";
                /*
                 * Each final filename should contain: filename_packagename_base_title, protocol, resolution, language,
                 * audio_class_user_readable, ext
                 */
                final String protocol;
                if (isAdaptive) {
                    protocol = "hls";
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
                    final GetRequest request = br.createGetRequest(uri);
                    request.getHeaders().put("Api-Auth", "Bearer " + apiToken);
                    br.getPage(request);
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
                        final String finalDownloadURL = hlscontainer.getDownloadurl();
                        ext = hlscontainer.getFileExtension().replace(".", "");
                        linkid = this.getHost() + "://" + String.format(linkid_format, internalVideoID, type, cdn, language, audio_class, protocol, resolution);
                        final_filename = filename_packagename_base_title + "_" + protocol + "_" + resolution + "_" + language + "_" + audio_class_user_readable + "." + ext;
                        final DownloadLink dl = createDownloadlink(finalDownloadURL);
                        dl.setContentUrl(param.getCryptedUrl());
                        if (hlscontainer.getBandwidth() > highestHlsBandwidth) {
                            /*
                             * While adding the URLs, let's find the BEST quality url. In case we need it later we will already know which
                             * one is the BEST.
                             */
                            highestHlsBandwidth = hlscontainer.getBandwidth();
                            highestHLS = dl;
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
                        logger.info("Stopping because: Aborted by user");
                        break;
                    }
                } else {
                    /* http download */
                    protocol = "http";
                    final String realQuality = ((String) qualitymap.get("quality")).toLowerCase(Locale.ENGLISH);
                    final ArrayList<Object[]> qualities = new ArrayList<Object[]>();
                    /**
                     * Sometimes we can modify the final downloadurls and thus get higher quality streams. </br> We want to keep all
                     * versions though!
                     */
                    final List<String[]> betterQualities = getBetterQualities(uri);
                    final HashSet<String> optimizedQualityIdentifiers = new HashSet<String>();
                    if (betterQualities != null) {
                        /* We cannot be 100% sure if these will work thus let's check... */
                        for (final String[] betterQualityEntry : betterQualities) {
                            final String betterQuality = betterQualityEntry[1];
                            if (!httpQualities.contains(ext + betterQuality)) {
                                final long filesizeNew = this.checkDownloadable(betterQualityEntry[0]);
                                if (filesizeNew > -1) {
                                    logger.info("Optimization for: " + realQuality + "(" + uri + ")->" + betterQuality + "(" + betterQualityEntry[0] + ")");
                                    qualities.add(new Object[] { betterQuality, betterQualityEntry[0], filesizeNew });
                                    httpQualities.add(ext + betterQuality);
                                    optimizedQualityIdentifiers.add(ext + betterQuality);
                                }
                            }
                            if (this.isAbort()) {
                                throw new InterruptedException();
                            }
                        }
                    }
                    if (httpQualities.contains(ext + realQuality)) {
                        logger.info("Skipping given quality because it was optimized");
                    } else {
                        qualities.add(new Object[] { realQuality, uri, filesize });
                        httpQualities.add(ext + realQuality);
                    }
                    for (final Object[] qualityInfo : qualities) {
                        final String httpQualityIdentifierWeak = qualityInfo[0].toString();
                        final String finalDownloadURL = qualityInfo[1].toString();
                        final long thisFilesize = ((Number) qualityInfo[2]).longValue();
                        linkid = this.getHost() + "://" + String.format(linkid_format, internalVideoID, type, cdn, language, audio_class, protocol, httpQualityIdentifierWeak);
                        final String qualityIdentifierForFilename;
                        if (cfg.isUseVideoResolutionAsQualityModifierForHTTPVideoStreams()) {
                            qualityIdentifierForFilename = convertInternalHttpQualityIdentifierToVideoResolution(httpQualityIdentifierWeak);
                        } else {
                            qualityIdentifierForFilename = httpQualityIdentifierWeak;
                        }
                        final_filename = filename_packagename_base_title + "_" + protocol + "_" + qualityIdentifierForFilename + "_" + language + "_" + audio_class_user_readable + "." + ext;
                        final DownloadLink dl = createDownloadlink(finalDownloadURL);
                        dl.setContentUrl(param.getCryptedUrl());
                        /**
                         * Usually filesize is only given for the official downloads.</br> Only set it here if we haven't touched the
                         * original downloadurls!
                         */
                        if (thisFilesize > 0) {
                            dl.setAvailable(true);
                            dl.setVerifiedFileSize(thisFilesize);
                        }
                        setDownloadlinkProperties(dl, final_filename, type, linkid, title, tv_show, date_formatted, tv_station);
                        final String qualitySelectorString = generateQualitySelectorString(protocol, ext, httpQualityIdentifierWeak, language, audio_class);
                        final boolean isMp4 = StringUtils.startsWithCaseInsensitive(qualitySelectorString, "http_mp4");
                        final boolean isWebm = StringUtils.startsWithCaseInsensitive(qualitySelectorString, "http_webm");
                        all_found_downloadlinks.put(qualitySelectorString, dl);
                        addDownloadLinkAndGenerateSubtitleDownloadLink(allDownloadLinks, dl);
                        if (containsQuality(selectedQualityStrings, qualitySelectorString)) {
                            userSelectedQualitiesTmp.add(dl);
                            selectedQualitiesMapTmp.put(qualitySelectorString, dl);
                        } else if (grabUnknownQualities && !containsQuality(allKnownQualities, qualitySelectorString)) {
                            userSelectedQualitiesTmp.add(dl);
                        }
                        /* Find highest quality. */
                        final int httpQualityValueTmp = convertInternalHttpQualityIdentifierToIntegerValue(httpQualityIdentifierWeak);
                        if (isMp4) {
                            if (httpQualityValueTmp > highestHTTPMp4QualityValue) {
                                highestHTTPMp4QualityValue = httpQualityValueTmp;
                                highestHTTPMp4 = dl;
                            }
                        } else if (isWebm) {
                            if (httpQualityValueTmp > highestHTTPWebmQualityValue) {
                                highestHTTPWebmQualityValue = httpQualityValueTmp;
                                highestHTTPWebm = dl;
                            }
                        }
                    }
                }
            }
            /* Now decide what's the BEST candidate for this round --> Prefer HTTP over HLS */
            DownloadLink best = null;
            if (highestHTTPMp4 != null && grabHTTPMp4) {
                best = highestHTTPMp4;
            } else if (highestHTTPWebm != null && grabHTTPWebm) {
                best = highestHTTPWebm;
            } else if (highestHLS != null && grabHLS) {
                best = highestHLS;
            } else {
                /* No BEST candidate available in current round! */
            }
            /* Finally, check which qualities the user actually wants to have. */
            if (grabBest) {
                if (best != null) {
                    userSelectedQualitiesTmp.clear();
                    userSelectedQualitiesTmp.add(best);
                }
            } else if (cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled()) {
                /* Best of selected */
                final DownloadLink bestOfSelection = findBESTInsideGivenMapNew(selectedQualitiesMapTmp, allKnownQualities);
                if (bestOfSelection != null) {
                    userSelectedQualitiesTmp.clear();
                    userSelectedQualitiesTmp.add(bestOfSelection);
                } else if (best != null) {
                    logger.info("Failed to find user selected BEST --> Fallback to 'best best'");
                    userSelectedQualitiesTmp.clear();
                    userSelectedQualitiesTmp.add(best);
                } else {
                    /* Add all selected as final fallback */
                }
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
        if (description != null) {
            fp.setComment(description);
        }
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

    /** Returns filesize if given URL looks to be downloadable (= leads to accepted file content). */
    private long checkDownloadable(final String url) {
        URLConnectionAdapter con = null;
        final Browser br2 = this.br.cloneBrowser();
        try {
            br2.setFollowRedirects(true);
            con = br2.openHeadConnection(url);
            if (this.looksLikeDownloadableContent(con)) {
                return con.getCompleteContentLength();
            } else {
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return -1;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
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
        if (bestMap.size() > 0) {
            DownloadLink keep = null;
            for (final String quality : allKnownQualities) {
                keep = bestMap.get(quality);
                if (keep != null) {
                    return keep;
                }
            }
        }
        return null;
    }

    private void addDownloadLinkAndGenerateSubtitleDownloadLink(final ArrayList<DownloadLink> ret, final DownloadLink dl) {
        ret.add(dl);
        final ZdfmediathekConfigInterface cfg = PluginJsonConfig.get(ZdfmediathekConfigInterface.class);
        final SubtitleType subtitleType = cfg.getPreferredSubtitleType();
        final Map<String, String> subtitleSource;
        final String ext;
        boolean convertSubtitle = false;
        if (subtitleType == SubtitleType.WEBVTT) {
            subtitleSource = this.subtitlesVTT;
            ext = ".vtt";
        } else if (subtitleType == SubtitleType.SRT) {
            subtitleSource = this.subtitlesXML;
            ext = ".xml";
            /* xml -> srt */
            convertSubtitle = true;
        } else {
            subtitleSource = this.subtitlesXML;
            ext = ".xml";
            /* xml -> srt */
            convertSubtitle = false;
        }
        for (final String selectedSubtitleType : this.userSelectedSubtitleTypes) {
            if (subtitleSource.containsKey(selectedSubtitleType)) {
                final String current_ext = dl.getFinalFileName().substring(dl.getFinalFileName().lastIndexOf("."));
                final String longSubtitleName = this.convertInternalSubtitleClassToUserReadable(selectedSubtitleType);
                final String final_filename = dl.getFinalFileName().replace(current_ext, "_" + longSubtitleName + ext);
                final String linkid = dl.getLinkID() + "_" + longSubtitleName;
                final DownloadLink dl_subtitle = this.createDownloadlink(subtitleSource.get(selectedSubtitleType));
                setDownloadlinkProperties(dl_subtitle, final_filename, "subtitle_" + ext, linkid, null, null, null, null);
                if (convertSubtitle) {
                    dl_subtitle.setProperty(ZdfDeMediathek.PROPERTY_convert_subtitle, true);
                }
                ret.add(dl_subtitle);
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
        } else if (httpQualityIdentifier.equalsIgnoreCase("med") || httpQualityIdentifier.equalsIgnoreCase("medium")) {
            return 200;
        } else if (httpQualityIdentifier.equalsIgnoreCase("high")) {
            return 300;
        } else if (httpQualityIdentifier.equalsIgnoreCase("veryhigh")) {
            return 400;
        } else if (httpQualityIdentifier.equalsIgnoreCase("hd")) {
            return 500;
        } else if (httpQualityIdentifier.equalsIgnoreCase("fhd")) {
            return 800;
        } else if (httpQualityIdentifier.equalsIgnoreCase("uhd")) {
            return 1000;
        } else {
            /* Unknown quality identifier! */
            return 1;
        }
    }

    private static String convertInternalHttpQualityIdentifierToVideoResolution(final String httpQualityIdentifier) {
        if (httpQualityIdentifier == null) {
            return null;
        } else if (httpQualityIdentifier.equalsIgnoreCase("low")) {
            return "480x270";
        } else if (httpQualityIdentifier.equalsIgnoreCase("med") || httpQualityIdentifier.equalsIgnoreCase("medium")) {
            /* TODO: 2022-06-08 This doesn't look right. "high" should be higher than "med"? But filesize implies otherwise... */
            return "960x540";
        } else if (httpQualityIdentifier.equalsIgnoreCase("high")) {
            return "640x360";
        } else if (httpQualityIdentifier.equalsIgnoreCase("veryhigh")) {
            return "1024x576";
        } else if (httpQualityIdentifier.equalsIgnoreCase("hd")) {
            return "1280x720";
        } else if (httpQualityIdentifier.equalsIgnoreCase("fhd")) {
            return "1920x1080";
        } else if (httpQualityIdentifier.equalsIgnoreCase("uhd")) {
            return " 3840x2160";
        } else {
            /* Unknown quality identifier! */
            return null;
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

    /* Some old phoenix methods down below */
    private String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "><\\!\\[CDATA\\[([^<>]*?)\\]\\]></" + parameter + ">").getMatch(0);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "( type=\"[^<>/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
        }
        return result;
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String formatDatePHOENIX(String input) {
        /* It contains the day twice --> Fix that */
        if (input.contains(",")) {
            input = input.substring(input.lastIndexOf(",") + 2);
        }
        // Tue, 23 Jun 2015 11:33:00 +0200
        final long date = TimeFormatter.getMilliSeconds(input, "dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
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

    /**
     * Searches for videos in zdfmediathek that match the given search term. </br> This is mostly used as a workaround to find stuff that is
     * hosted on their other website on zdfmediathek instead as zdfmediathek is providing a fairly stable search function while other
     * websites hosting the same content such as kika.de can be complicated to parse. </br> This does not (yet) support pagination!
     */
    public ArrayList<DownloadLink> crawlZDFMediathekSearchResultsVOD(final String tvChannel, final String searchTerm, final int maxResults) throws Exception {
        if (StringUtils.isEmpty(tvChannel) || StringUtils.isEmpty(searchTerm)) {
            /* Developer mistake */
            return null;
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage("https://www.zdf.de/suche?q=" + Encoding.urlEncode(searchTerm) + "&synth=true&sender=" + Encoding.urlEncode(tvChannel) + "&from=&to=&attrs=&abName=&abGroup=gruppe-a");
        final String[] urls = br.getRegex("\"(/[^\"]+)\"[^>]*class=\"teaser-title-link m-clickarea-action js-track-click\"").getColumn(0);
        logger.info("Found " + urls.length + " search results on page 1");
        for (String url : urls) {
            url = br.getURL(url).toString();
            ret.add(super.createDownloadlink(url));
            if (ret.size() == maxResults) {
                break;
            }
        }
        return ret;
    }
}