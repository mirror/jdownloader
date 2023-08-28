package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BrDeMediathekCrawler extends PluginForDecrypt {
    public BrDeMediathekCrawler(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "br.de" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/mediathek/podcast/([\\w\\-]+)/([\\w\\-]+/\\d+|\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    private final String PATTERN_PODCAST_OVERVIEW       = "https?://[^/]+/mediathek/podcast/([\\w\\-]+)/(?:alle/)?(\\d+)$";
    private final String PATTERN_PODCAST_SINGLE_EPISODE = "https?://[^/]+/mediathek/podcast/([\\w\\-]+)/([\\w\\-]+)/(\\d+)";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches(PATTERN_PODCAST_OVERVIEW)) {
            /**
             * Examples: </br>
             * https://www.br.de/mediathek/podcast/pumuckl/alle/830 </br>
             * https://www.br.de/mediathek/podcast/pumuckl/830
             */
            final HashSet<String> dupes = new HashSet<String>();
            FilePackage fp = null;
            final String podcastID = new Regex(param.getCryptedUrl(), PATTERN_PODCAST_OVERVIEW).getMatch(1);
            int itemsPerPage = 24;
            int page = 0;
            do {
                page++; // Start from value "1"
                int numberofNewItems = 0;
                final UrlQuery query = new UrlQuery();
                query.add("items_per_page", Integer.toString(itemsPerPage));
                query.add("page", Integer.toString(page));
                br.getPage("https://www.br.de/mediathek/podcast/api/podcasts/" + podcastID + "/episodes?" + query.toString());
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> result = (Map<String, Object>) entries.get("result");
                final List<Map<String, Object>> episodes = (List<Map<String, Object>>) result.get("episodes");
                for (final Map<String, Object> episode : episodes) {
                    final String episodeID = episode.get("id").toString();
                    if (!dupes.add(episodeID)) {
                        continue;
                    }
                    final Map<String, Object> podcast = (Map<String, Object>) episode.get("podcast");
                    final String contentURL = "https://www.br.de/mediathek/podcast/" + podcast.get("slug") + "/" + episode.get("slug") + "/" + episodeID;
                    final String shownotes = (String) episode.get("shownotes");
                    final Map<String, Object> enclosure = (Map<String, Object>) episode.get("enclosure");
                    final DownloadLink audio = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(enclosure.get("url").toString()));
                    audio.setContentUrl(contentURL);
                    audio.setFinalFileName(enclosure.get("filename").toString());
                    audio.setDownloadSize(((Number) enclosure.get("length")).longValue());
                    audio.setAvailable(true);
                    if (!StringUtils.isEmpty(shownotes)) {
                        audio.setComment(shownotes);
                    }
                    if (fp == null) {
                        final String podcastDescription = (String) podcast.get("summary");
                        fp = FilePackage.getInstance();
                        fp.setName(podcast.get("title").toString());
                        if (!StringUtils.isEmpty(podcastDescription)) {
                            fp.setComment(podcastDescription);
                        }
                    }
                    audio._setFilePackage(fp);
                    ret.add(audio);
                    distribute(audio);
                    numberofNewItems++;
                }
                final Map<String, Object> pagedata = (Map<String, Object>) JavaScriptEngineFactory.walkJson(result, "meta/episodes");
                itemsPerPage = ((Number) pagedata.get("items_per_page")).intValue();
                final int totalPages = ((Number) pagedata.get("pages")).intValue();
                logger.info("Crawled page " + page + "/" + totalPages + " | Found items so far: " + ret.size());
                if (page >= totalPages) {
                    logger.info("Stopping because: Reached last page");
                    break;
                } else if (page >= totalPages) {
                    logger.info("Stopping because: Reached end #1");
                    break;
                } else if (numberofNewItems < itemsPerPage) {
                    logger.info("Stopping because: Reached end #2");
                    break;
                } else if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else {
                    /* Continue to next page */
                }
            } while (!this.isAbort());
        } else if (param.getCryptedUrl().matches(PATTERN_PODCAST_SINGLE_EPISODE)) {
            /**
             * Examples: </br>
             * https://www.br.de/mediathek/podcast/pumuckl/pumuckl-und-die-katze/2019744
             */
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String directurl = HTMLSearch.searchMetaTag(br, "og:audio");
            if (directurl == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final DownloadLink audio = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl));
            audio.setAvailable(true);
            ret.add(audio);
        } else {
            /* Unsupported URL -> Developer mistake! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
