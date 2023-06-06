package jd.plugins.decrypter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class OrfAt extends PluginForDecrypt {
    public OrfAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING, LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "orf.at" });
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
            String pattern = "https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/(";
            pattern += "(?:player|programm)/\\d+/[a-zA-Z0-9]+";
            pattern += "|artikel/\\d+/[a-zA-Z0-9]+";
            pattern += "|program/(\\w+)/(\\w+)\\.html";
            pattern += "|.*collection/(\\d+)(/(\\d+)(/[a-z0-9\\-]+)?)?";
            pattern += "|(podcasts?/[a-z0-9]+/[a-z0-9\\-]+(/[A-Za-z0-9\\-]+)?|[a-z0-9]+/\\d+/[a-zA-Z0-9]+)";
            pattern += ")";
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    private final String                                      PATTERN_BROADCAST_OLD = "(?i)https?://([a-z0-9]+)\\.orf\\.at/(?:player|programm)/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      PATTERN_BROADCAST_NEW = "(?i)https?://radiothek\\.orf\\.at/([a-z0-9]+)/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      PATTERN_ARTICLE       = "(?i)https?://([a-z0-9]+)\\.orf\\.at/artikel/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      PATTERN_PODCAST       = "(?i)https?://[^/]+/podcasts?/([a-z0-9]+)/([A-Za-z0-9\\-]+)(/([a-z0-9\\-]+))?";
    private final String                                      PATTERN_COLLECTION    = "(?i)^(https?://.*(?:/collection|podcast/highlights))/(\\d+)(/(\\d+)(/[a-z0-9\\-]+)?)?";
    private final String                                      PATTERN_VIDEO         = "(?i)^https?://[^/]+/program/(\\w+)/(\\w+)\\.html";
    private final String                                      API_BASE              = "https://audioapi.orf.at";
    private final String                                      PROPERTY_SLUG         = "slug";
    /* E.g. https://radiothek.orf.at/ooe --> "ooe" --> Channel == "oe2o" */
    private static LinkedHashMap<String, Map<String, Object>> CHANNEL_CACHE         = new LinkedHashMap<String, Map<String, Object>>() {
                                                                                        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                                                                                            return size() > 50;
                                                                                        };
                                                                                    };

    /** Wrapper for podcast URLs containing md5 file-hashes inside URL. */
    protected DownloadLink createPodcastDownloadlink(final String directurl) throws MalformedURLException {
        final DownloadLink link = this.createDownloadlink(directurl, true);
        final UrlQuery query = UrlQuery.parse(directurl);
        final String md5Hash = query.get("etag");
        if (md5Hash != null) {
            link.setMD5Hash(md5Hash);
        }
        return link;
    }

    @Override
    protected DownloadLink createDownloadlink(final String directurl) {
        final DownloadLink link = super.createDownloadlink(directurl);
        link.setProperty(DirectHTTP.PROPERTY_REQUEST_TYPE, "GET");
        return link;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(PATTERN_ARTICLE)) {
            return this.crawlArticle(param);
        } else if (param.getCryptedUrl().matches(PATTERN_COLLECTION)) {
            return this.crawlCollection(param);
        } else if (param.getCryptedUrl().matches(PATTERN_PODCAST)) {
            return this.crawlPodcast(param);
        } else if (param.getCryptedUrl().matches(PATTERN_BROADCAST_OLD) || param.getCryptedUrl().matches(PATTERN_BROADCAST_NEW)) {
            return crawlProgramm(param);
        } else if (param.getCryptedUrl().matches(PATTERN_VIDEO)) {
            return crawlVideo(param);
        } else {
            /* Unsupported URL -> Developer mistake */
            logger.info("Unsupported URL:" + param);
            return new ArrayList<DownloadLink>(0);
        }
    }

    private ArrayList<DownloadLink> crawlArticle(final CryptedLink param) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String title = br.getRegex("\"og:title\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        final String oon_audioEntries[] = br.getRegex("<div class=\"oon-audio\"(.*?)\\s*</div>").getColumn(0);
        if (oon_audioEntries.length > 0) {
            for (final String oon_audioEntry : oon_audioEntries) {
                final String url = new Regex(oon_audioEntry, "data-url\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
                if (url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink link = createDownloadlink(url);
                link.setContentUrl(param.getCryptedUrl());
                String name = getFileNameFromURL(new URL(url));
                if (title != null) {
                    name = title + "-" + name;
                    fp.add(link);
                }
                if (name != null) {
                    link.setFinalFileName(name);
                }
                link.setProperty(DirectHTTP.FIXNAME, name);
                ret.add(link);
            }
            return ret;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /** Crawls all episodes of a podcast or only a specific episode. */
    private ArrayList<DownloadLink> crawlPodcast(final CryptedLink param) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), PATTERN_PODCAST);
        final String channelSlug = urlinfo.getMatch(0);
        final String podcastSeriesSlug = urlinfo.getMatch(1);
        final String podcastEpisodeTitleSlug = urlinfo.getMatch(3); // optional
        if (channelSlug == null || podcastSeriesSlug == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Old API call: (2022-09-27: Still working) */
        // br.getPage(API_BASE + "/radiothek/podcast/" + channelSlug + "/" + podcastSeriesSlug + ".json?_o=" + hostFromURL);
        br.getPage(API_BASE + "/radiothek/api/2.0/podcast/" + channelSlug + "/" + podcastSeriesSlug + "?episodes&_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* This should never happen but for sure users could modify added URLs and render them invalid. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> payload = (Map<String, Object>) entries.get("payload");
        final String station = payload.get("station").toString();
        final String podcastSlug = payload.get("slug").toString();
        final String podcastDescription = (String) payload.get("description");
        final String author = payload.get("author").toString();
        final String podcastTitle = payload.get("title").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(author + " - " + podcastTitle);
        if (!StringUtils.isEmpty(podcastDescription)) {
            fp.setComment(podcastDescription);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        boolean foundSpecificEpisode = false;
        final List<Map<String, Object>> episodes = (List<Map<String, Object>>) payload.get("episodes");
        for (final Map<String, Object> episode : episodes) {
            String directurl = null;
            final List<Map<String, Object>> enclosures = (List<Map<String, Object>>) episode.get("enclosures");
            for (final Map<String, Object> enclosure : enclosures) {
                if (enclosure.get("type").toString().equals("audio/mpeg")) {
                    directurl = enclosure.get("url").toString();
                    break;
                }
            }
            if (StringUtils.isEmpty(directurl)) {
                /* Most likely unsupported streaming type */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String episodeSlug = episode.get("slug").toString();
            final DownloadLink link = createPodcastDownloadlink(directurl);
            final String episodeDateStr = episode.get("published").toString();
            final String dateFormatted = new Regex(episodeDateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            final String filename = dateFormatted + "_" + author + " - " + episode.get("title").toString() + ".mp3";
            link.setFinalFileName(filename);
            link.setDownloadSize(calculateFilesize(((Number) episode.get("duration")).longValue()));
            link.setProperty(DirectHTTP.FIXNAME, filename);
            final String contentURL = (String) JavaScriptEngineFactory.walkJson(episode, "link/url");
            if (!StringUtils.isEmpty(contentURL)) {
                link.setContentUrl(contentURL);
            } else {
                /* ContentURLs are not always given in json e.g. https://sound.orf.at/podcast/tv/report-werkstatt */
                link.setContentUrl("https://sound.orf.at/podcast/" + station + "/" + podcastSlug + "/" + episodeSlug);
            }
            final String description = (String) episode.get("description");
            if (!StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
            link.setAvailable(true);
            link._setFilePackage(fp);
            if (podcastEpisodeTitleSlug != null && episodeSlug.equals(podcastEpisodeTitleSlug)) {
                /* User added link to single episode and we found it --> Clear previous findings and only return that single episode. */
                ret.clear();
                ret.add(link);
                foundSpecificEpisode = true;
                break;
            } else {
                ret.add(link);
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (podcastEpisodeTitleSlug != null && !foundSpecificEpisode) {
            /* Rare case */
            logger.warning("Failed to find specific episode --> Adding all instead");
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlProgramm(final CryptedLink param) throws Exception {
        final String broadCastID;
        final String broadcastDay;
        final String domainID;
        if (param.getCryptedUrl().matches(PATTERN_BROADCAST_OLD)) {
            broadCastID = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_OLD).getMatch(2);
            broadcastDay = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_OLD).getMatch(1);
            domainID = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_OLD).getMatch(0);
        } else {
            broadCastID = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_NEW).getMatch(2);
            broadcastDay = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_NEW).getMatch(1);
            domainID = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_NEW).getMatch(0);
        }
        return crawlProgramm(domainID, broadCastID, broadcastDay);
    }

    private ArrayList<DownloadLink> crawlProgramm(final String domainID, final String broadcastID, final String broadcastDay) throws Exception {
        if (broadcastID == null || broadcastDay == null || domainID == null || domainID.length() < 3) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* E.g. change "wien" to "wie": https://wien.orf.at/player/20220925/WTIT */
        final String domainIDCorrected = domainID.toLowerCase(Locale.ENGLISH).substring(0, 3);
        synchronized (CHANNEL_CACHE) {
            /* 2021-03-31 */
            if (!CHANNEL_CACHE.containsKey(domainIDCorrected)) {
                br.getPage("https://assets.orf.at/vue-storyserver/radiothek-item-player/js/app.js");
                final String channelInfoJs = br.getRegex("stations:(\\{.*?\\}\\}\\})\\},").getMatch(0);
                final Map<String, Map<String, Object>> channelInfo = (Map<String, Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(channelInfoJs);
                CHANNEL_CACHE.clear();
                CHANNEL_CACHE.putAll(channelInfo);
                // final String[][] shortnameToChannelNames = br.getRegex("/([^/]+)/json/" +
                // Regex.escape("4.0/broadcast{/programKey}{/broadcastDay}\")}") + ".*?,channel:\"([^\"]+)\"").getMatches();
                // for (final String[] channelInfo : shortnameToChannelNames) {
                // CHANNEL_CACHE.put(channelInfo[0], channelInfo[1]);
                // }
                if (!CHANNEL_CACHE.containsKey(domainIDCorrected)) {
                    /* Most likely invalid domainID. */
                    logger.info("Failed to find channel for: " + domainIDCorrected);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
        final Map<String, Object> channelInfo = CHANNEL_CACHE.get(domainIDCorrected);
        final Map<String, Object> loopstream = (Map<String, Object>) channelInfo.get("loopstream");
        br.setAllowedResponseCodes(410);
        br.getPage(API_BASE + "/" + domainIDCorrected + "/api/json/current/broadcast/" + broadcastID + "/" + broadcastDay + "?_s=" + System.currentTimeMillis());
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if (br.getHttpConnection().getResponseCode() == 410 || "Broadcast is no longer available".equals(response.get("message"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String broadcastDescription = (String) response.get("description");
        final String broadcastStartISO = response.get("startISO").toString();
        final String broadcastDateFormatted = new Regex(broadcastStartISO, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        // final String broadCastDay = response.get("broadcastDay").toString();
        final String title = (String) response.get("title");
        final String programTitle = (String) response.get("programTitle");
        final List<Map<String, Object>> streams = (List<Map<String, Object>>) response.get("streams");
        String titleBase = response.get("station").toString();
        if (!StringUtils.isEmpty(programTitle)) {
            titleBase += " - " + programTitle + " - " + title;
        } else {
            titleBase += " - " + title;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setCleanupPackageName(false);
        fp.setName(broadcastDateFormatted + "_" + titleBase);
        if (!StringUtils.isEmpty(broadcastDescription)) {
            fp.setComment(broadcastDescription);
        }
        int position = 1;
        final String userid = UUID.randomUUID().toString();
        final int padLength = StringUtils.getPadLength(streams.size());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Map<String, Object> stream : streams) {
            final String loopStreamId = (String) stream.get("loopStreamId");
            if (loopStreamId == null) {
                continue;
            }
            final String startISO = stream.get("startISO").toString();
            final String dateFormatted = new Regex(startISO, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            final long startTimestamp = ((Number) stream.get("start")).longValue();
            final long endTimestamp = ((Number) stream.get("end")).longValue();
            final long runtimeMilliseconds = endTimestamp - startTimestamp;
            final long startOffset = ((Number) stream.get("startOffset")).longValue();
            final long endOffset = ((Number) stream.get("endOffset")).longValue();
            final long offset = startOffset - endOffset;
            final DownloadLink link = createDownloadlink("directhttp://https://" + loopstream.get("host") + "/?channel=" + loopstream.get("channel") + "&shoutcast=0&player=" + domainIDCorrected + "_v1&referer=" + domainIDCorrected + ".orf.at&_=" + System.currentTimeMillis() + "&userid=" + userid + "&id=" + loopStreamId + "&offset=" + offset + "&offsetende=" + runtimeMilliseconds);
            if (streams.size() > 1) {
                link.setFinalFileName(dateFormatted + "_" + titleBase + "_" + StringUtils.formatByPadLength(padLength, position) + ".mp3");
            } else {
                link.setFinalFileName(dateFormatted + "_" + titleBase + ".mp3");
            }
            link.setDownloadSize(this.calculateFilesize(runtimeMilliseconds));
            link.setAvailable(true);
            link.setLinkID(domainIDCorrected + ".orf.at://" + broadcastID + "/" + broadcastDay + "/" + position);
            ret.add(link);
            fp.add(link);
            position++;
        }
        return ret;
    }

    /** Crawls all items of a collection. A collection can contain single episode of various podcasts. */
    private ArrayList<DownloadLink> crawlCollection(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex collectionPatternRegex = new Regex(param.getCryptedUrl(), PATTERN_COLLECTION);
        if (!collectionPatternRegex.matches()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String baseURL = collectionPatternRegex.getMatch(0);
        final String collectionID = collectionPatternRegex.getMatch(1);
        final String collectionTargetItemID = collectionPatternRegex.getMatch(3);
        final String collectionURL = baseURL + "/" + collectionID;
        br.getPage("https://collector.orf.at/api/frontend/collections/" + collectionID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final Map<String, Object> payload = (Map<String, Object>) entries.get("payload");
        final Map<String, Object> collectionContent = (Map<String, Object>) payload.get("content");
        final String collectionTitle = collectionContent.get("title").toString();
        final String collectionDescription = (String) collectionContent.get("description");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(collectionTitle);
        if (!StringUtils.isEmpty(collectionDescription)) {
            fp.setComment(collectionDescription);
        }
        final List<Map<String, Object>> items = (List<Map<String, Object>>) collectionContent.get("items");
        final ArrayList<Map<String, Object>> itemsToCrawl = new ArrayList<Map<String, Object>>();
        /*
         * 2022-09-23: Disabled because collection URLs will always contain the id to a single item. By default browser will redirect to the
         * id of the first item so let's always crawl all items for now.
         */
        final boolean crawlOnlySpecificItemIfGivenInURL = false;
        for (final Map<String, Object> item : items) {
            final String collectionItemID = item.get("id").toString();
            if (collectionTargetItemID == null || crawlOnlySpecificItemIfGivenInURL == false) {
                /* Crawl all items */
                itemsToCrawl.add(item);
            } else if (collectionTargetItemID.equals(collectionItemID)) {
                /* Only crawl this one specific item */
                itemsToCrawl.clear();
                itemsToCrawl.add(item);
                break;
            }
        }
        int progress = 0;
        int numberofOfflineItems = 0;
        for (final Map<String, Object> item : itemsToCrawl) {
            progress++;
            final String collectionItemID = item.get("id").toString();
            logger.info("Crawling collection item " + progress + "/" + items.size() + " ID: " + collectionItemID);
            final Map<String, Object> collectionItemContent = (Map<String, Object>) item.get("content");
            final Map<String, Object> target = (Map<String, Object>) item.get("target");
            if ((Boolean) target.get("isGone")) {
                /* This should never happen?! */
                numberofOfflineItems++;
                continue;
            }
            final String collectionItemTitle = collectionItemContent.get("title").toString();
            final String collectionItemStation = collectionItemContent.get("station").toString();
            final String collectionItemContentURL = collectionURL + "/" + collectionItemID + "/" + toSlug(collectionItemTitle);
            final String targetType = target.get("type").toString();
            final Map<String, Object> params = (Map<String, Object>) target.get("params");
            final ArrayList<DownloadLink> thisresults = new ArrayList<DownloadLink>();
            /*
             * If offline exception happens for one item here this seems to mean that the whole collection is offline. Website will then
             * simply redirect to a random other collection.
             */
            if (targetType.equalsIgnoreCase("podcast-episode")) {
                final DownloadLink broadcast = crawlPodcastEpisodeByGUID(params.get("guid").toString());
                thisresults.add(broadcast);
            } else if (targetType.equalsIgnoreCase("broadcastitem")) {
                final DownloadLink podcast = this.crawlBroadcastItem(collectionItemStation, params.get("id").toString());
                thisresults.add(podcast);
            } else if (targetType.equalsIgnoreCase("broadcast")) {
                thisresults.addAll(this.crawlBroadcast(collectionItemStation, params.get("id").toString()));
            } else if (targetType.equalsIgnoreCase("upload")) {
                final DownloadLink upload = this.crawlUpload(params.get("id").toString());
                thisresults.add(upload);
            } else {
                logger.warning("Unsupported targetType " + targetType + " for collection item: " + collectionItemContentURL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /*
             * Item can be linked directly or as part of a collection. In this case we want to present it to the user as part of a
             * collection as this is what he will get via browser too.
             */
            for (final DownloadLink thisresult : thisresults) {
                thisresult.setContentUrl(collectionItemContentURL);
                thisresult.setContainerUrl(collectionURL);
                thisresult.setAvailable(true);
                thisresult._setFilePackage(fp);
                distribute(thisresult);
                ret.add(thisresult);
            }
        }
        if (ret.isEmpty()) {
            /* Rare case: All items are offline? */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (numberofOfflineItems > 0) {
            logger.info("Skippen offline items: " + numberofOfflineItems);
        }
        return ret;
    }

    private DownloadLink crawlPodcastEpisodeByGUID(final String podcastGUID) throws IOException, PluginException {
        br.getPage(API_BASE + "/radiothek/api/2.0/episode/" + podcastGUID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
        final Map<String, Object> podcast = (Map<String, Object>) payload.get("podcast");
        final String author = podcast.get("author").toString();
        final String podcastEpisodeTitle = podcast.get("title").toString();
        String directurl = null;
        final List<Map<String, Object>> enclosures = (List<Map<String, Object>>) payload.get("enclosures");
        for (final Map<String, Object> enclosure : enclosures) {
            if (enclosure.get("type").toString().equals("audio/mpeg")) {
                directurl = enclosure.get("url").toString();
                break;
            }
        }
        if (StringUtils.isEmpty(directurl)) {
            /* Most likely unsupported streaming type */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String contentURL = (String) JavaScriptEngineFactory.walkJson(payload, "link/url");
        final DownloadLink link = createPodcastDownloadlink(directurl);
        if (!StringUtils.isEmpty(contentURL)) {
            link.setContentUrl(contentURL);
        }
        link.setProperty(PROPERTY_SLUG, podcast.get("slug"));
        final String dateStr = payload.get("published").toString();
        final String dateFormatted = new Regex(dateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String filename = dateFormatted + "_" + author + " - " + podcastEpisodeTitle + " - " + payload.get("title").toString() + ".mp3";
        link.setFinalFileName(filename);
        link.setProperty(DirectHTTP.FIXNAME, filename);
        final String description = (String) payload.get("description");
        if (!StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        link.setDownloadSize(calculateFilesize(((Number) payload.get("duration")).longValue()));
        link.setAvailable(true);
        return link;
    }

    private DownloadLink crawlBroadcastItem(final String radioStation, final String broadcastID) throws IOException, PluginException {
        if (radioStation == null || broadcastID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(API_BASE + "/" + radioStation + "/api/json/5.0/broadcastitem/" + broadcastID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
        final Map<String, Object> broadcast = (Map<String, Object>) payload.get("broadcast");
        final ArrayList<Map<String, Object>> streams = new ArrayList<Map<String, Object>>();
        streams.add((Map<String, Object>) payload.get("stream"));
        final ArrayList<DownloadLink> results = crawlProcessBroadcastItems(broadcast, streams);
        return results.get(0);
    }

    private ArrayList<DownloadLink> crawlBroadcast(final String radioStation, final String broadcastID) throws IOException, PluginException {
        if (radioStation == null || broadcastID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(API_BASE + "/" + radioStation + "/api/json/5.0/broadcast/" + broadcastID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final Map<String, Object> broadcast = (Map<String, Object>) resp.get("payload");
        return crawlProcessBroadcastItems(broadcast, (List<Map<String, Object>>) broadcast.get("streams"));
    }

    private ArrayList<DownloadLink> crawlProcessBroadcastItems(final Map<String, Object> broadcast, final List<Map<String, Object>> streams) throws IOException, PluginException {
        final String broadcastTitle = broadcast.get("title").toString();
        // final String broadcastSubtitle = (String) broadcast.get("subtitle");
        final String broadcastDescription = (String) broadcast.get("description");
        final String broadcastStartDate = broadcast.get("start").toString();
        final String broadcastStartDateFormatted = new Regex(broadcastStartDate, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        final String station = broadcast.get("station").toString();
        final String titleBase = station + " - " + broadcastTitle;
        final FilePackage fp = FilePackage.getInstance();
        final String packagenameBase = broadcastStartDateFormatted + "_" + titleBase;
        fp.setName(packagenameBase);
        if (!StringUtils.isEmpty(broadcastDescription)) {
            fp.setComment(broadcastDescription);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final int padLength = StringUtils.getPadLength(streams.size());
        int position = 1;
        for (final Map<String, Object> stream : streams) {
            final String streamStartDate = stream.get("start").toString();
            final String streamStartDateFormatted = new Regex(streamStartDate, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            String filenameBase = streamStartDateFormatted + "_" + titleBase;
            if (streams.size() > 1) {
                filenameBase += " " + StringUtils.formatByPadLength(padLength, position);
            }
            filenameBase += ".mp3";
            final Map<String, Object> urls = (Map<String, Object>) stream.get("urls");
            // final String originalFilename = stream.get("loopStreamId").toString();
            String downloadurl = urls.get("progressive").toString();
            /* Remove placeholders inside URL which we're not filling in. */
            downloadurl = downloadurl.replaceAll("\\{\\&[^\\}]+\\}", "");
            final DownloadLink link = this.createDownloadlink("directhttp://" + downloadurl);
            link.setFinalFileName(filenameBase);
            link.setProperty(DirectHTTP.FIXNAME, filenameBase);
            link.setDownloadSize(calculateFilesize(((Number) stream.get("duration")).longValue()));
            link.setAvailable(true);
            ret.add(link);
            position++;
        }
        return ret;
    }

    private DownloadLink crawlUpload(final String uploadID) throws IOException, PluginException {
        br.getPage(API_BASE + "/radiothek/api/2.0/upload/" + uploadID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
        if ((Boolean) payload.get("isOnline") == Boolean.FALSE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String moderator = (String) payload.get("moderator");
        final String uploadTitle = payload.get("title").toString();
        String directurl = null;
        final List<Map<String, Object>> enclosures = (List<Map<String, Object>>) payload.get("enclosures");
        for (final Map<String, Object> enclosure : enclosures) {
            if (enclosure.get("type").toString().equals("audio/mpeg")) {
                directurl = enclosure.get("url").toString();
                break;
            }
        }
        if (StringUtils.isEmpty(directurl)) {
            /* Most likely unsupported streaming type */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink link = createPodcastDownloadlink(directurl);
        link.setProperty(PROPERTY_SLUG, payload.get("slug"));
        final String dateStr = payload.get("postDate").toString();
        final String dateFormatted = new Regex(dateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        String filename = dateFormatted + "_" + payload.get("station");
        if (!StringUtils.isEmpty(moderator)) {
            filename += "_ " + moderator;
        }
        filename += " - " + uploadTitle + " - " + payload.get("title").toString() + ".mp3";
        link.setFinalFileName(filename);
        link.setProperty(DirectHTTP.FIXNAME, filename);
        final String description = (String) payload.get("description");
        if (!StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        link.setDownloadSize(calculateFilesize(((Number) payload.get("duration")).longValue()));
        link.setAvailable(true);
        return link;
    }

    private ArrayList<DownloadLink> crawlVideo(final CryptedLink param) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final HashSet<String> dupes = new HashSet<String>();
        final String[] programIDs = br.getRegex("data-ppid=\"([a-f0-9\\-]+)\"").getColumn(0);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final String programID : programIDs) {
            if (dupes.add(programID)) {
                ret.addAll(crawlVideoProgramID(programID));
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlVideoProgramID(final String programID) throws Exception {
        if (StringUtils.isEmpty(programID)) {
            throw new IllegalArgumentException();
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getHeaders().put("Origin", "https://tv.orf.at");
        br.getHeaders().put("Referer", "https://tv.orf.at/");
        br.getPage("https://api-tvthek.orf.at/api/v4.2/public/content-by-dds-programplanguid/" + programID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> episode = (Map<String, Object>) entries.get("episode");
        // if(Boolean.TRUE.equals(episode.get("is_drm_protected"))) {
        //
        // }
        final String title = episode.get("title").toString();
        final String description = episode.get("description").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.setComment(description);
        /* There are multiple HLS sources available. Looks like mirrors. */
        final Map<String, Object> hlsmap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(episode, "sources/hls/{0}");
        final String hlsMaster = hlsmap.get("src").toString();
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        brc.getPage(hlsMaster);
        final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(brc);
        for (final HlsContainer hlsContainer : hlsContainers) {
            final DownloadLink video = this.createDownloadlink(hlsContainer.getDownloadurl());
            video.setFinalFileName(title + "_" + hlsContainer.getHeight() + "p.mp4");
            ret.add(video);
        }
        final String thumbnailurl = (String) episode.get("related_audiodescription_episode_image_url");
        if (thumbnailurl != null) {
            final DownloadLink thumbnail = this.createDownloadlink(thumbnailurl);
            thumbnail.setFinalFileName(title + Plugin.getFileNameExtensionFromURL(thumbnailurl));
            thumbnail.setAvailable(true);
            ret.add(thumbnail);
        }
        final Map<String, Object> subtitlemap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(episode, "_embedded/subtitle");
        final String subtitleURL = subtitlemap != null ? (String) subtitlemap.get("srt_url") : null;
        if (!StringUtils.isEmpty(subtitleURL)) {
            final DownloadLink subtitle = this.createDownloadlink(subtitleURL);
            subtitle.setFinalFileName(title + ".srt");
            subtitle.setAvailable(true);
            ret.add(subtitle);
        }
        fp.addLinks(ret);
        return ret;
    }

    private String toSlug(final String str) {
        final String preparedSlug = str.toLowerCase(Locale.ENGLISH).replace("ü", "u").replace("ä", "a").replace("ö", "o");
        String slug = preparedSlug.replaceAll("[^a-z0-9]", "-");
        /* Remove double-minus */
        slug = slug.replaceAll("-{2,}", "-");
        /* Do not begin with minus */
        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        /* Do not end with minus */
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug;
    }

    /** Calculates filesizes on the assumption that audio is delivered with quality 192kb/s. */
    private long calculateFilesize(final long durationMilliseconds) {
        final long durationSeconds = durationMilliseconds / 1000;
        return (durationSeconds * 192 * 1024) / 8;
    }
}
