package jd.plugins.decrypter;

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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "orf.at" }, urls = { "https?://[a-z0-9]+\\.orf\\.at/(?:player|programm)/\\d+/[a-zA-Z0-9]+|https?://[a-z0-9]+\\.orf\\.at/artikel/\\d+/[a-zA-Z0-9]+|https?://radiothek\\.orf\\.at/(podcasts/[a-z0-9]+/[a-z0-9\\-]+(/[a-z0-9\\-]+)?|[a-z0-9]+/\\d+/[a-zA-Z0-9]+)" })
public class OrfAt extends PluginForDecrypt {
    public OrfAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    private final String                                      TYPE_OLD      = "https?://([a-z0-9]+)\\.orf\\.at/(?:player|programm)/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      TYPE_ARTICLE  = "https?://([a-z0-9]+)\\.orf\\.at/artikel/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      TYPE_NEW      = "https?://radiothek\\.orf\\.at/([a-z0-9]+)/(\\d+)/([a-zA-Z0-9]+)";
    private final String                                      TYPE_PODCAST  = "https?://radiothek\\.orf\\.at/podcasts/([a-z0-9]+)/([a-z0-9\\-]+)(/([a-z0-9\\-]+))?";
    private final String                                      API_BASE      = "https://audioapi.orf.at";
    /* E.g. https://radiothek.orf.at/ooe --> "ooe" --> Channel == "oe2o" */
    private static LinkedHashMap<String, Map<String, Object>> CHANNEL_CACHE = new LinkedHashMap<String, Map<String, Object>>() {
                                                                                protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                                                                                    return size() > 50;
                                                                                };
                                                                            };

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_ARTICLE)) {
            return this.crawlArticle(param);
        } else if (param.getCryptedUrl().matches(TYPE_PODCAST)) {
            return this.crawlPodcasts(param);
        } else {
            return crawlBroadcasts(param);
        }
    }

    private ArrayList<DownloadLink> crawlArticle(final CryptedLink param) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                link.setFinalFileName(name);
                link.setProperty(DirectHTTP.FIXNAME, name);
                link.setProperty(DirectHTTP.PROPERTY_REQUEST_TYPE, "GET");
                ret.add(link);
            }
            return ret;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private ArrayList<DownloadLink> crawlPodcasts(final CryptedLink param) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_PODCAST);
        final String channelSlug = urlinfo.getMatch(0);
        final String podcastSeriesSlug = urlinfo.getMatch(1);
        final String podcastEpisodeTitleSlug = urlinfo.getMatch(3); // optional
        if (channelSlug == null || podcastSeriesSlug == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(API_BASE + "/radiothek/podcast/" + channelSlug + "/" + podcastSeriesSlug + ".json?_o=radiothek.orf.at");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* This should never happen but for sure users could modify added URLs and render them invalid. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        final String author = data.get("author").toString();
        final String podcastTitle = data.get("title").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(author + " - " + podcastTitle);
        fp.setComment(data.get("description").toString());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        boolean foundSpecificEpisode = false;
        final List<Map<String, Object>> episodes = (List<Map<String, Object>>) data.get("episodes");
        for (final Map<String, Object> episode : episodes) {
            String directurl = null;
            final List<Map<String, Object>> enclosures = (List<Map<String, Object>>) episode.get("enclosures");
            for (final Map<String, Object> enclosure : enclosures) {
                if (enclosure.get("type").toString().equals("audio/mpeg")) {
                    directurl = enclosure.get("url").toString();
                    break;
                }
            }
            if (directurl == null) {
                /* Most likely unsupported streaming type */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String slug = episode.get("slug").toString();
            final UrlQuery query = UrlQuery.parse(directurl);
            final String md5Hash = query.get("etag");
            final DownloadLink link = this.createDownloadlink(directurl);
            final String filename = new SimpleDateFormat("yyyy-MM-dd").format(new Date(((Number) episode.get("published")).longValue())) + "_" + author + " - " + episode.get("title").toString() + ".mp3";
            link.setFinalFileName(filename);
            link.setProperty(DirectHTTP.FIXNAME, filename);
            link.setContentUrl(episode.get("link").toString());
            if (md5Hash != null) {
                link.setMD5Hash(md5Hash);
            }
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
        if (param.getCryptedUrl().matches(TYPE_OLD)) {
            broadCastID = new Regex(param.getCryptedUrl(), TYPE_OLD).getMatch(2);
            broadCastKey = new Regex(param.getCryptedUrl(), TYPE_OLD).getMatch(1);
            domainID = new Regex(param.getCryptedUrl(), TYPE_OLD).getMatch(0);
        } else {
            broadCastID = new Regex(param.getCryptedUrl(), TYPE_NEW).getMatch(2);
            broadCastKey = new Regex(param.getCryptedUrl(), TYPE_NEW).getMatch(1);
            domainID = new Regex(param.getCryptedUrl(), TYPE_NEW).getMatch(0);
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
}
