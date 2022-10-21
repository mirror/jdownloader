//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
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
import jd.plugins.hoster.SrfCh;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.SrfChConfig;
import org.jdownloader.plugins.components.config.SrfChConfig.QualitySelectionFallbackMode;
import org.jdownloader.plugins.components.config.SrfChConfig.QualitySelectionMode;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SrfChCrawler extends PluginForDecrypt {
    public SrfChCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "srf.ch" });
        ret.add(new String[] { "rts.ch" });
        ret.add(new String[] { "rsi.ch" });
        ret.add(new String[] { "rtr.ch" });
        ret.add(new String[] { "swissinfo.ch" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    public static final String PROPERTY_WIDTH  = "width";
    public static final String PROPERTY_HEIGHT = "height";
    public static final String PROPERTY_URN    = "urn";
    public static final String IS_AUDIO        = "is_audio";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final SrfChConfig cfg = PluginJsonConfig.get(SrfChConfig.class);
        return this.crawl(param, cfg.getQualitySelectionMode(), cfg.getQualitySelectionFallbackMode());
    }

    public ArrayList<DownloadLink> crawl(final CryptedLink param, final QualitySelectionMode mode, final QualitySelectionFallbackMode qualitySelectionFallbackMode) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (getSelectedQualities().isEmpty() && qualitySelectionFallbackMode == QualitySelectionFallbackMode.NONE) {
            logger.info("User deselected all qualities and has fallback set to none --> Returning nothing");
            return ret;
        }
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(new int[] { 410 });
        final GetRequest getRequest = br.createGetRequest(param.getCryptedUrl());
        URLConnectionAdapter con = null;
        try {
            con = this.br.openRequestConnection(getRequest);
            if (this.looksLikeDownloadableContent(con)) {
                final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(getRequest, con);
                ret.add(direct);
                return ret;
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable ignore) {
            }
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex(">\\s*window\\.__SSR_VIDEO_DATA__ = (\\{.*?\\})</script>").getMatch(0);
        if (json != null) {
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final Map<String, Object> initialData = (Map<String, Object>) entries.get("initialData");
            final Map<String, Object> show = (Map<String, Object>) initialData.get("show");
            final Map<String, Object> videoDetail = (Map<String, Object>) initialData.get("videoDetail");
            if (videoDetail != null) {
                return this.crawlMedia(videoDetail.get("urn").toString(), mode, qualitySelectionFallbackMode);
            } else if (show != null) {
                /* Crawl latest episode of tv-series-overview */
                final Map<String, Object> latestMedia = (Map<String, Object>) show.get("latestMedia");
                return this.crawlMedia(latestMedia.get("urn").toString(), mode, qualitySelectionFallbackMode);
            } else {
                /* Unsupported content */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            /*
             * Example: https://www.srf.ch/news/panorama/gefaelschte-unterschrift-immobilienfirma-betreibt-ahnungslose-frau
             */
            final String[] urns = br.getRegex("data-assetid=\"(urn:srf:(audio|video):[a-f0-9\\-]+)\"").getColumn(0);
            if (urns == null || urns.length == 0) {
                logger.info("Failed to find any downloadable content");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final HashSet<String> urnsWithoutDuplicates = new HashSet<String>(Arrays.asList(urns));
            int progress = 1;
            for (final String urn : urnsWithoutDuplicates) {
                logger.info("Crawling embedded media " + progress + "/" + urnsWithoutDuplicates.size());
                final ArrayList<DownloadLink> results = this.crawlMedia(urn, mode, qualitySelectionFallbackMode);
                this.distribute(results);
                ret.addAll(results);
                progress++;
            }
        }
        return ret;
    }

    private List<Integer> getSelectedQualities() {
        final SrfChConfig cfg = PluginJsonConfig.get(SrfChConfig.class);
        final List<Integer> selectedQualities = new ArrayList<Integer>();
        if (cfg.isCrawl180p()) {
            selectedQualities.add(180);
        }
        if (cfg.isCrawl270p()) {
            // different aspect ratio
            selectedQualities.add(270);
            selectedQualities.add(272);
            selectedQualities.add(288);
        }
        if (cfg.isCrawl360p()) {
            selectedQualities.add(360);
        }
        if (cfg.isCrawl540p()) {
            // different aspect ratio
            selectedQualities.add(540);
            selectedQualities.add(544);
        }
        if (cfg.isCrawl720p()) {
            selectedQualities.add(720);
        }
        if (cfg.isCrawl1080p()) {
            selectedQualities.add(1080);
        }
        return selectedQualities;
    }

    private String toSlug(final String str) {
        return str.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]", "-");
    }

    private ArrayList<DownloadLink> crawlMedia(final String urn, final QualitySelectionMode mode, final QualitySelectionFallbackMode fallbackMode) throws Exception {
        if (StringUtils.isEmpty(urn)) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* xml also possible: http://il.srgssr.ch/integrationlayer/1.0/<channelname>/srf/video/play/<videoid>.xml */
        this.br.getPage("https://il.srgssr.ch/integrationlayer/2.0/mediaComposition/byUrn/" + urn + ".json?onlyChapters=false&vector=portalplay");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final Map<String, Object> show = (Map<String, Object>) root.get("show");
        final String showTitle = show.get("title").toString();
        final List<Map<String, Object>> chapterList = (List<Map<String, Object>>) root.get("chapterList");
        if (chapterList.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Integer> selectedQualities = getSelectedQualities();
        final SrfCh hosterPlugin = (SrfCh) this.getNewPluginForHostInstance(this.getHost());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int numberofGeoBlockedItems = 0;
        String lastBlockReason = null;
        /* 2022-07-22: I've never seen an item containing multiple chapters but whatever... */
        for (final Map<String, Object> chapter : chapterList) {
            final String chapterID = chapter.get("id").toString();
            final String title = chapter.get("title").toString();
            final String dateFormatted = new Regex(chapter.get("date"), "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            final String thisURN = chapter.get("urn").toString(); // should be == urn
            final String mediaExt;
            final String mediaType = (String) chapter.get("mediaType");
            final String contentURL;
            final boolean isAudio = mediaType.equalsIgnoreCase("AUDIO");
            if (isAudio) {
                mediaExt = ".mp3";
                contentURL = "https://www." + this.getHost() + "/play/tv/" + toSlug(showTitle) + "/video/" + toSlug(title) + "?urn=" + thisURN;
            } else if (mediaType.equalsIgnoreCase("VIDEO")) {
                mediaExt = ".mp4";
                contentURL = "https://www." + this.getHost() + "/play/tv/" + toSlug(showTitle) + "/video/" + toSlug(title) + "?urn=" + thisURN;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported mediaType:" + mediaType);
            }
            final String titleBase = dateFormatted + "_" + chapter.get("vendor") + "_" + title;
            String description = (String) chapter.get("description");
            if (StringUtils.isEmpty(description)) {
                description = (String) JavaScriptEngineFactory.walkJson(root, "show/description");
            }
            String URLHTTP360p = (String) chapter.get("podcastSdUrl");
            String URLHTTP720p = (String) chapter.get("podcastHdUrl");
            String bestHLSMasterURL = null;
            lastBlockReason = (String) chapter.get("blockReason");
            if (!StringUtils.isEmpty(lastBlockReason)) {
                /* 2022-09-03: GEO-block can be easily circumvented as they will provide working HLS URLs anyways. */
                logger.info("Chapter might to be GEO-blocked: " + chapterID);
                numberofGeoBlockedItems++;
            }
            try {
                // final String id = (String) entries.get("id");
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) chapter.get("resourceList");
                if (ressourcelist == null || ressourcelist.size() == 0) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                boolean foundHD = false;
                for (final Map<String, Object> resource : ressourcelist) {
                    /* Every resource is usually available in "SD" and "HD" */
                    final String quality = resource.get("quality").toString();
                    final String url = resource.get("url").toString();
                    final String protocol = resource.get("protocol").toString();
                    if (quality.equalsIgnoreCase("HD")) {
                        foundHD = true;
                    }
                    if (protocol.equalsIgnoreCase("HTTP") || protocol.equalsIgnoreCase("HTTPS")) {
                        if (foundHD) {
                            if (StringUtils.isEmpty(URLHTTP720p)) {
                                URLHTTP720p = url;
                            }
                        } else {
                            if (StringUtils.isEmpty(URLHTTP360p)) {
                                URLHTTP360p = url;
                            }
                        }
                    } else if (protocol.equalsIgnoreCase("HLS")) {
                        bestHLSMasterURL = url;
                    } else {
                        /* Skip unsupported protocol */
                        logger.info("Skipping protocol: " + protocol);
                        continue;
                    }
                    if (foundHD) {
                        break;
                    }
                }
                final Map<Integer, DownloadLink> foundQualities = new HashMap<Integer, DownloadLink>();
                if (URLHTTP360p != null) {
                    final DownloadLink dlHTTP360p = new DownloadLink(hosterPlugin, hosterPlugin.getHost(), URLHTTP360p, true);
                    dlHTTP360p.setProperty(PROPERTY_WIDTH, 640);
                    dlHTTP360p.setProperty(PROPERTY_HEIGHT, 360);
                    if (isAudio) {
                        dlHTTP360p.setProperty(IS_AUDIO, true);
                    }
                    foundQualities.put(360, dlHTTP360p);
                }
                if (URLHTTP720p != null) {
                    final DownloadLink dlHTTP720p = new DownloadLink(hosterPlugin, hosterPlugin.getHost(), URLHTTP720p, true);
                    dlHTTP720p.setProperty(PROPERTY_WIDTH, 1280);
                    dlHTTP720p.setProperty(PROPERTY_HEIGHT, 720);
                    if (isAudio) {
                        dlHTTP720p.setProperty(IS_AUDIO, true);
                    }
                    foundQualities.put(720, dlHTTP720p);
                }
                /* Decide whether or not we need to crawl HLS qualities. */
                boolean crawlHLS;
                if (isAudio) {
                    /* No HLS available for audio files */
                    crawlHLS = false;
                } else if (mode == QualitySelectionMode.BEST) {
                    /* Best quality is only possible via HLS. Max quality via http: 720p. */
                    crawlHLS = true;
                } else {
                    crawlHLS = false;
                    /* Check if user wants to have a quality that isn't already found as http quality. */
                    for (final int selectedQuality : selectedQualities) {
                        if (!foundQualities.containsKey(selectedQuality)) {
                            /* User wants quality that we didn't already found -> If at all, it will only be available via HLS. */
                            crawlHLS = true;
                            break;
                        }
                    }
                }
                int bestHeight = 0;
                DownloadLink best = null;
                if (crawlHLS) {
                    String acl = new Regex(bestHLSMasterURL, "https?://[^/]+(/.+\\.csmil)").getMatch(0);
                    if (acl != null) {
                        /* Sign HLS master URL if needed */
                        acl += "/*";
                        br.getPage("https://player.rts.ch/akahd/token?acl=" + acl);
                        final Map<String, Object> signInfoRoot = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                        final Map<String, Object> authMap = (Map<String, Object>) signInfoRoot.get("token");
                        String authparams = (String) authMap.get("authparams");
                        if (StringUtils.isEmpty(authparams)) {
                            logger.warning("Failed to find authparams");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        authparams = new Regex(authparams, "hdnts=(.+)").getMatch(0);
                        authparams = URLEncode.encodeURIComponent(authparams);
                        authparams = authparams.replace("*", "%2A");
                        String bestHLSMasterURLFixed = bestHLSMasterURL;
                        // bestHLSMasterURL += "&hdnts=" + authparams;
                        final String param_caption = new Regex(bestHLSMasterURL, "caption=([^\\&]+)").getMatch(0);
                        if (param_caption != null) {
                            String param_caption_new = param_caption;
                            param_caption_new = Encoding.htmlDecode(param_caption_new);
                            param_caption_new = URLEncode.encodeURIComponent(param_caption_new);
                            param_caption_new = param_caption_new.replace("%3D", "=");
                            bestHLSMasterURLFixed = bestHLSMasterURLFixed.replace(param_caption, param_caption_new);
                        }
                        final UrlQuery hlsMasterQuery = UrlQuery.parse(bestHLSMasterURLFixed);
                        hlsMasterQuery.add("hdnts", authparams);
                        String bestHLSMasterURLFixedWithoutParams = URLHelper.getUrlWithoutParams(bestHLSMasterURLFixed);
                        br.getPage(bestHLSMasterURLFixedWithoutParams + "?" + hlsMasterQuery.toString());
                    } else {
                        /* No signing of the URL needed -> Probably we will get split audio/video streams. */
                        br.getPage(bestHLSMasterURL);
                    }
                    final List<HlsContainer> containers = HlsContainer.getHlsQualities(br);
                    for (final HlsContainer container : containers) {
                        if (foundQualities.containsKey(container.getHeight())) {
                            /* Skip already found qualities */
                            continue;
                        }
                        final DownloadLink result = new DownloadLink(hosterPlugin, hosterPlugin.getHost(), container.getDownloadurl(), true);
                        result.setProperty(PROPERTY_WIDTH, container.getWidth());
                        result.setProperty(PROPERTY_HEIGHT, container.getHeight());
                        foundQualities.put(container.getHeight(), result);
                        if (best == null || container.getHeight() > bestHeight) {
                            best = result;
                            bestHeight = container.getHeight();
                        }
                    }
                }
                final ArrayList<DownloadLink> retChapter = new ArrayList<DownloadLink>();
                if (mode == QualitySelectionMode.BEST) {
                    retChapter.add(best);
                } else {
                    /* Add all selected qualities */
                    for (final int selectedQuality : selectedQualities) {
                        if (foundQualities.containsKey(selectedQuality)) {
                            retChapter.add(foundQualities.get(selectedQuality));
                        }
                    }
                    if (retChapter.isEmpty()) {
                        /* None of users' selected qualities have been found. */
                        if (fallbackMode == QualitySelectionFallbackMode.BEST) {
                            retChapter.add(best);
                        } else if (fallbackMode == QualitySelectionFallbackMode.ALL) {
                            retChapter.addAll(foundQualities.values());
                        } else {
                            logger.info("None of the user selected qualities have been found and user configured \"Add nothing\" as fallback");
                        }
                    }
                }
                /* Set miscellaneous properties */
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(titleBase);
                if (!StringUtils.isEmpty(description)) {
                    fp.setComment(description);
                }
                final int numberofReturnedMediaItems = retChapter.size();
                final SrfChConfig cfg = PluginJsonConfig.get(SrfChConfig.class);
                if (cfg.isCrawlThumbnail()) {
                    final String thumbnailURL = (String) chapter.get("imageUrl");
                    if (!StringUtils.isEmpty(thumbnailURL)) {
                        final DownloadLink thumbnail = new DownloadLink(hosterPlugin, hosterPlugin.getHost(), thumbnailURL, true);
                        final String ext = Plugin.getFileNameExtensionFromURL(thumbnailURL);
                        if (ext != null) {
                            thumbnail.setFinalFileName(titleBase + ext);
                        }
                        retChapter.add(thumbnail);
                    }
                }
                for (final DownloadLink result : retChapter) {
                    if (result.hasProperty(PROPERTY_HEIGHT)) {
                        /* Only set filename for video/audio items as for thumbnails name is already set. */
                        if (result.hasProperty(IS_AUDIO) && numberofReturnedMediaItems == 1) {
                            /*
                             * Special case: Podcast with only one available quality --> Do not include any quality identifier in filename
                             * especially as we only know the width of video items but do not have any information about audio items at this
                             * stage.
                             */
                            result.setFinalFileName(titleBase + mediaExt);
                        } else {
                            result.setFinalFileName(titleBase + "_" + result.getStringProperty(PROPERTY_HEIGHT) + mediaExt);
                        }
                    }
                    result.setContentUrl(contentURL);
                    result.setAvailable(true);
                    result._setFilePackage(fp);
                    result.setProperty(PROPERTY_URN, thisURN);
                }
                ret.addAll(retChapter);
            } catch (final Exception e) {
                if (numberofGeoBlockedItems > 0) {
                    /* Failure most likely due to an item being GEO-blocked */
                    break;
                } else {
                    throw e;
                }
            }
        }
        if (ret.isEmpty() && numberofGeoBlockedItems > 0) {
            throw new DecrypterRetryException(RetryReason.GEO, "Content blocked because: " + lastBlockReason);
        }
        return ret;
    }
}
