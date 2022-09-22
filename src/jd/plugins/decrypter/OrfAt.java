package jd.plugins.decrypter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
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
            pattern += "|(podcasts?/[a-z0-9]+/[a-z0-9\\-]+(/[a-z0-9\\-]+)?|[a-z0-9]+/\\d+/[a-zA-Z0-9]+)";
            pattern += ")";
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    private final String                                      PATTERN_BROADCAST_OLD = "https?://([a-z0-9]+)\\.orf\\.at/(?:player|programm)/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      PATTERN_BROADCAST_NEW = "https?://radiothek\\.orf\\.at/([a-z0-9]+)/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      PATTERN_ARTICLE       = "https?://([a-z0-9]+)\\.orf\\.at/artikel/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      PATTERN_PODCAST       = "https?://[^/]+/podcasts?/([a-z0-9]+)/([a-z0-9\\-]+)(/([a-z0-9\\-]+))?";
    private final String                                      PATTERN_COLLECTION    = "https?://[^/]+/collection/(\\d+)(/([a-z0-9\\-]+))?";
    private final String                                      API_BASE              = "https://audioapi.orf.at";
    /* E.g. https://radiothek.orf.at/ooe --> "ooe" --> Channel == "oe2o" */
    private static LinkedHashMap<String, Map<String, Object>> CHANNEL_CACHE         = new LinkedHashMap<String, Map<String, Object>>() {
                                                                                        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                                                                                            return size() > 50;
                                                                                        };
                                                                                    };

    protected DownloadLink createPodcastDownloadlink(final String directurl) throws MalformedURLException {
        final DownloadLink link = createDownloadlink(directurl, true);
        final UrlQuery query = UrlQuery.parse(directurl);
        final String md5Hash = query.get("etag");
        if (md5Hash != null) {
            link.setMD5Hash(md5Hash);
        }
        return link;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(PATTERN_ARTICLE)) {
            return this.crawlArticle(param);
        } else if (param.getCryptedUrl().matches(PATTERN_PODCAST)) {
            return this.crawlPodcasts(param);
        } else if (param.getCryptedUrl().matches(PATTERN_BROADCAST_OLD) || param.getCryptedUrl().matches(PATTERN_BROADCAST_NEW)) {
            return crawlBroadcasts(param);
        } else if (param.getCryptedUrl().matches(PATTERN_COLLECTION)) {
            return this.crawlCollection(param);
        } else {
            /* Unsupported URL -> Developer mistake */
            return null;
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
                link.setProperty(DirectHTTP.PROPERTY_REQUEST_TYPE, "GET");
                ret.add(link);
            }
            return ret;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /** Crawls all episodes of a podcast or only a specific episode. */
    private ArrayList<DownloadLink> crawlPodcasts(final CryptedLink param) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), PATTERN_PODCAST);
        final String channelSlug = urlinfo.getMatch(0);
        final String podcastSeriesSlug = urlinfo.getMatch(1);
        final String podcastEpisodeTitleSlug = urlinfo.getMatch(3); // optional
        if (channelSlug == null || podcastSeriesSlug == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String hostFromURL = Browser.getHost(param.getCryptedUrl(), true);
        /**
         * 2022-09-22: New endpoint: https://audioapi.orf.at/radiothek/api/2.0/podcast/fm4/fm4-science-busters/raketenschaf?_o=sound.orf.at
         * </br>
         * (Old one is still working!)
         */
        br.getPage(API_BASE + "/radiothek/podcast/" + channelSlug + "/" + podcastSeriesSlug + ".json?_o=" + hostFromURL);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* This should never happen but for sure users could modify added URLs and render them invalid. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> podcast = (Map<String, Object>) entries.get("data");
        final String author = podcast.get("author").toString();
        final String podcastTitle = podcast.get("title").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(author + " - " + podcastTitle);
        fp.setComment(podcast.get("description").toString());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        boolean foundSpecificEpisode = false;
        final List<Map<String, Object>> episodes = (List<Map<String, Object>>) podcast.get("episodes");
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
            final String slug = episode.get("slug").toString();
            final DownloadLink link = createPodcastDownloadlink(directurl);
            final String filename = new SimpleDateFormat("yyyy-MM-dd").format(new Date(((Number) episode.get("published")).longValue())) + "_" + author + " - " + episode.get("title").toString() + ".mp3";
            link.setFinalFileName(filename);
            link.setProperty(DirectHTTP.FIXNAME, filename);
            link.setContentUrl(episode.get("link").toString());
            final String description = (String) episode.get("description");
            if (!StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
            link.setAvailable(true);
            link._setFilePackage(fp);
            if (podcastEpisodeTitleSlug != null && slug.equals(podcastEpisodeTitleSlug)) {
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

    private ArrayList<DownloadLink> crawlBroadcasts(final CryptedLink param) throws Exception {
        final String broadCastID;
        final String broadCastKey;
        final String domainID;
        if (param.getCryptedUrl().matches(PATTERN_BROADCAST_OLD)) {
            broadCastID = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_OLD).getMatch(2);
            broadCastKey = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_OLD).getMatch(1);
            domainID = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_OLD).getMatch(0);
        } else {
            broadCastID = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_NEW).getMatch(2);
            broadCastKey = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_NEW).getMatch(1);
            domainID = new Regex(param.getCryptedUrl(), PATTERN_BROADCAST_NEW).getMatch(0);
        }
        if (broadCastID == null || broadCastKey == null || domainID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        synchronized (CHANNEL_CACHE) {
            /* 2021-03-31 */
            if (!CHANNEL_CACHE.containsKey(domainID)) {
                br.getPage("https://assets.orf.at/vue-storyserver/radiothek-item-player/js/app.js");
                final String channelInfoJs = br.getRegex("stations:(\\{.*?\\}\\}\\})\\},F=function").getMatch(0);
                final Map<String, Map<String, Object>> channelInfo = (Map<String, Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(channelInfoJs);
                CHANNEL_CACHE.clear();
                CHANNEL_CACHE.putAll(channelInfo);
                // final String[][] shortnameToChannelNames = br.getRegex("/([^/]+)/json/" +
                // Regex.escape("4.0/broadcast{/programKey}{/broadcastDay}\")}") + ".*?,channel:\"([^\"]+)\"").getMatches();
                // for (final String[] channelInfo : shortnameToChannelNames) {
                // CHANNEL_CACHE.put(channelInfo[0], channelInfo[1]);
                // }
                if (!CHANNEL_CACHE.containsKey(domainID)) {
                    /* Most likely invalid domainID. */
                    logger.info("Failed to find channel for: " + domainID);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
        final Map<String, Object> channelInfo = CHANNEL_CACHE.get(domainID);
        final Map<String, Object> loopstream = (Map<String, Object>) channelInfo.get("loopstream");
        br.setAllowedResponseCodes(410);
        br.getPage(API_BASE + "/" + domainID + "/api/json/current/broadcast/" + broadCastID + "/" + broadCastKey + "?_s=" + System.currentTimeMillis());
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (br.getHttpConnection().getResponseCode() == 410 || "Broadcast is no longer available".equals(response.get("message"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String broadCastDay = response.get("broadcastDay").toString();
        final String title = (String) response.get("title");
        /* TODO: What are the other items there for? */
        // final List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        final List<Map<String, Object>> streams = (List<Map<String, Object>>) response.get("streams");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title + "_" + broadCastDay);
        int index = 0;
        final String userid = UUID.randomUUID().toString();
        final int padLength = StringUtils.getPadLength(streams.size());
        for (final Map<String, Object> stream : streams) {
            final String loopStreamId = (String) stream.get("loopStreamId");
            if (loopStreamId == null) {
                continue;
            }
            final long startTime = ((Number) stream.get("start")).longValue();
            final long endTime = ((Number) stream.get("end")).longValue();
            final long offsetende = endTime - startTime;
            final long startOffset = ((Number) stream.get("startOffset")).longValue();
            final long endOffset = ((Number) stream.get("endOffset")).longValue();
            final long offset = startOffset - endOffset;
            final DownloadLink link = createDownloadlink("directhttp://https://" + loopstream.get("host") + "/?channel=" + loopstream.get("channel") + "&shoutcast=0&player=" + domainID + "_v1&referer=" + domainID + ".orf.at&_=" + System.currentTimeMillis() + "&userid=" + userid + "&id=" + loopStreamId + "&offset=" + offset + "&offsetende=" + offsetende);
            if (streams.size() > 1) {
                link.setFinalFileName(title + "_" + broadCastDay + "_" + String.format(Locale.US, "%0" + padLength + "d", (++index)) + ".mp3");
            } else {
                link.setFinalFileName(title + "_" + broadCastDay + ".mp3");
            }
            link.setProperty("requestType", "GET");
            link.setAvailable(true);
            link.setLinkID(domainID + ".orf.at://" + broadCastID + "/" + broadCastKey + "/" + index);
            ret.add(link);
            fp.add(link);
        }
        return ret;
    }

    /** Crawls all items of a collection. A collection can contain single episode of various podcasts. */
    private ArrayList<DownloadLink> crawlCollection(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String collectionID = new Regex(param.getCryptedUrl(), PATTERN_COLLECTION).getMatch(0);
        if (collectionID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("https://collector.orf.at/api/frontend/collections/" + collectionID + "?_o=sound.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final Map<String, Object> payload = (Map<String, Object>) entries.get("payload");
        final Map<String, Object> content = (Map<String, Object>) payload.get("content");
        final String collectionTitle = content.get("title").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(collectionTitle);
        final List<Map<String, Object>> items = (List<Map<String, Object>>) content.get("items");
        int progress = 0;
        /* TODO: Add support for crawling single collection items */
        for (final Map<String, Object> item : items) {
            progress++;
            final String collectionItemID = item.get("id").toString();
            final Map<String, Object> target = (Map<String, Object>) item.get("target");
            final String targetType = target.get("type").toString();
            if (!targetType.equalsIgnoreCase("podcast-episode")) {
                /* TODO: Maybe add support for some other types such as "broadcastitem". */
                logger.info("Skipping unsupported targetType: " + targetType);
                continue;
            }
            final Map<String, Object> params = (Map<String, Object>) target.get("params");
            final String guid = params.get("guid").toString();
            logger.info("Crawling collection item " + progress + "/" + items.size() + " ID: " + collectionItemID + " GUID: " + guid);
            br.getPage("https://audioapi.orf.at/radiothek/api/2.0/episode/" + guid + "?_o=sound.orf.at");
            final Map<String, Object> resp = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            final Map<String, Object> episode = (Map<String, Object>) resp.get("payload");
            final Map<String, Object> podcast = (Map<String, Object>) episode.get("podcast");
            final String author = podcast.get("author").toString();
            /* TODO: Improve filenames */
            final String podcastTitle = podcast.get("title").toString();
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
            // final String slug = episode.get("slug").toString();
            final DownloadLink link = createPodcastDownloadlink(directurl);
            link.setContentUrl("https://sound.orf.at/collection/" + collectionID + "/" + collectionItemID);
            final String dateStr = episode.get("published").toString();
            final String dateFormatted = new Regex(dateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
            final String filename = dateFormatted + "_" + author + " - " + podcastTitle + " - " + episode.get("title").toString() + ".mp3";
            link.setFinalFileName(filename);
            link.setProperty(DirectHTTP.FIXNAME, filename);
            final String description = (String) episode.get("description");
            if (!StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
            link.setAvailable(true);
            link._setFilePackage(fp);
            ret.add(link);
        }
        if (ret.isEmpty()) {
            /* Rare case */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return ret;
    }
}
