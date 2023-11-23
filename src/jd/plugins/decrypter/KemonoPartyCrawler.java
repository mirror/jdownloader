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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.KemonoPartyConfig;
import org.jdownloader.plugins.components.config.KemonoPartyConfig.TextCrawlMode;
import org.jdownloader.plugins.components.config.KemonoPartyConfigCoomerParty;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.KemonoParty;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KemonoPartyCrawler extends PluginForDecrypt {
    public KemonoPartyCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "coomer.su", "coomer.party" }); // onlyfans.com content
        ret.add(new String[] { "kemono.su", "kemono.party" }); // content of other websites such as patreon.com
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[^/]+/user/[^/]+(/post/\\d+)?");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_PROFILE = "(?i)(?:https?://[^/]+)?/([^/]+)/user/([^/\\?]+)(\\?o=(\\d+))?$";
    private final String TYPE_POST    = "(?i)(?:https?://[^/]+)?/([^/]+)/user/([^/]+)/post/(\\d+)$";
    private KemonoParty  hostPlugin   = null;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_PROFILE)) {
            return this.crawlProfile(param);
        } else if (param.getCryptedUrl().matches(TYPE_POST)) {
            return this.crawlPost(param);
        } else {
            /* Unsupported URL --> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> crawlProfile(final CryptedLink param) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_PROFILE);
        if (!urlinfo.patternFind()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String portal = urlinfo.getMatch(0);
        final String userID = urlinfo.getMatch(1);
        final boolean useAPI = true;
        if (useAPI) {
            HashSet<String> dupes = null;
            final boolean useExtendedDupecheck = false;
            // if(PluginJsonConfig.get(getConfigInterface()).isEnableProfileCrawlerExtendedDupeFiltering())
            if (useExtendedDupecheck) {
                dupes = new HashSet<String>();
            }
            return crawlProfileAPI(dupes, portal, userID);
        } else {
            return crawlProfileWebsite(param, portal, userID);
        }
    }

    private ArrayList<DownloadLink> crawlProfileAPI(final HashSet<String> dupes, final String portal, final String userID) throws Exception {
        if (portal == null || userID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = getFilePackageForProfileCrawler(portal, userID);
        int offset = 0;
        int page = 1;
        final int maxItemsPerPage = 50;
        do {
            br.getPage("https://" + this.getHost() + "/api/v1/" + portal + "/user/" + userID + "?o=" + offset);
            final List<HashMap<String, Object>> posts = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.LIST_HASHMAP);
            if (posts == null || posts.isEmpty()) {
                if (ret.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    /* This should never happen */
                    logger.info("Stopping because: Go empty page");
                    break;
                }
            }
            for (final HashMap<String, Object> post : posts) {
                final ArrayList<DownloadLink> thisresults = this.crawlProcessPostAPI(dupes, post);
                for (final DownloadLink thisresult : thisresults) {
                    thisresult._setFilePackage(fp);
                    distribute(thisresult);
                }
                ret.addAll(thisresults);
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size() + " | Offset: " + offset);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (posts.size() < maxItemsPerPage) {
                logger.info("Stopping because: Reached last page(?) Page: " + page);
                break;
            } else {
                /* Continue to next page */
                offset += posts.size();
                page++;
            }
        } while (!this.isAbort());
        return ret;
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlProfileWebsite(final CryptedLink param, final String portal, final String userID) throws Exception {
        if (portal == null || userID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Always begin on page 1 no matter which page param is given in users' added URL. */
        br.getPage("https://" + this.getHost() + "/" + portal + "/user/" + userID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().matches(TYPE_PROFILE)) {
            /* E.g. redirect to main page */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Find number of last page (for logging purposes) */
        int maxpage = getMaxPageWebsite(br, portal, userID, 1);
        int totalNumberofItems = -1;
        String totalNumberofItemsStr = br.getRegex("Showing \\d+ - \\d+ of (\\d+)").getMatch(0);
        if (totalNumberofItemsStr != null) {
            totalNumberofItems = Integer.parseInt(totalNumberofItemsStr);
        } else {
            totalNumberofItemsStr = "unknown";
        }
        final FilePackage fp = getFilePackageForProfileCrawler(portal, userID);
        final HashSet<String> dupes = new HashSet<String>();
        int page = 1;
        do {
            final String[] posturls = br.getRegex("(?:https?://[^/]+)?/([^/]+)/user/([^/]+)/post/(\\d+)").getColumn(-1);
            int numberofAddedItems = 0;
            for (String posturl : posturls) {
                posturl = br.getURL(posturl).toString();
                if (dupes.add(posturl)) {
                    final DownloadLink result = this.createDownloadlink(posturl);
                    result._setFilePackage(fp);
                    ret.add(result);
                    distribute(result);
                    numberofAddedItems++;
                }
            }
            maxpage = getMaxPageWebsite(br, portal, userID, maxpage);
            logger.info("Crawled page " + page + "/" + maxpage + " | Found items: " + ret.size() + "/" + totalNumberofItemsStr);
            final String nextpageurl = br.getRegex("(/[^\"]+\\?o=\\d+)\"[^>]*>(?:<b>)?\\s*" + (page + 1)).getMatch(0);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (ret.size() == totalNumberofItems) {
                logger.info("Stopping because: Found all items");
                break;
            } else if (nextpageurl == null) {
                /* Additional fail-safe */
                logger.info("Stopping because: Failed to find nextpageurl - last page is: " + br.getURL());
                break;
            } else if (numberofAddedItems == 0) {
                logger.info("Stopping because: Failed to find any [new] items on current page");
                break;
            } else {
                page++;
                br.getPage(nextpageurl);
            }
        } while (true);
        return ret;
    }

    private int getMaxPageWebsite(Browser br, final String portal, final String username, int maxPage) {
        final String[] pages = br.getRegex("href=\"/" + Pattern.quote(portal) + "/user/" + Pattern.quote(username) + "\\?o=\\d+\"[^>]*>\\s*(?:<b>)?\\s*(\\d+)").getColumn(0);
        int ret = maxPage;
        for (final String pageStr : pages) {
            final int page = Integer.parseInt(pageStr);
            if (page > ret) {
                ret = page;
            }
        }
        return ret;
    }

    private FilePackage getFilePackageForProfileCrawler(final String portal, final String userID) {
        final FilePackage fp = FilePackage.getInstance();
        fp.setAllowMerge(true);
        fp.setAllowInheritance(true);
        fp.setName(portal + " - " + userID);
        fp.setPackageKey(KemonoParty.UNIQUE_ID_PREFIX + "portal/" + portal + "/userid/" + userID);
        return fp;
    }

    private FilePackage getFilePackageForPostCrawler(final String portal, final String userID, final String postID, final String postTitle) {
        final FilePackage fp = FilePackage.getInstance();
        if (postTitle != null) {
            fp.setName(portal + " - " + userID + " - " + postID + " - " + postTitle);
        } else {
            /* Fallback */
            fp.setName(portal + " - " + userID + " - " + postID);
        }
        fp.setPackageKey(KemonoParty.UNIQUE_ID_PREFIX + "portal/" + portal + "/userid/" + userID + "/postid/" + postID);
        return fp;
    }

    private ArrayList<DownloadLink> crawlPost(final CryptedLink param) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_POST);
        if (!urlinfo.patternFind()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String portal = urlinfo.getMatch(0);
        final String userID = urlinfo.getMatch(1);
        final String postID = urlinfo.getMatch(2);
        final boolean crawlPostAPI = true;
        if (crawlPostAPI) {
            return crawlPostAPI(param, portal, userID, postID);
        } else {
            return crawlPostWebsite(param, portal, userID, postID);
        }
    }

    /** API docs: https://kemono.su/api/schema */
    private ArrayList<DownloadLink> crawlPostAPI(final CryptedLink param, final String portal, final String userID, final String postID) throws Exception {
        if (portal == null || userID == null || postID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("https://" + this.getHost() + "/api/v1/" + portal + "/user/" + userID + "/post/" + postID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* E.g. {"error":"Not Found"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        return crawlProcessPostAPI(null, entries);
    }

    /** Processes a map of an API response containing information about a users' post. */
    private ArrayList<DownloadLink> crawlProcessPostAPI(HashSet<String> dupes, final Map<String, Object> postmap) throws PluginException {
        final String portal = postmap.get("service").toString();
        final String userID = postmap.get("user").toString();
        final String postID = postmap.get("id").toString();
        final String posturl = "https://" + this.getHost() + "/" + portal + "/user/" + userID + "/post/" + postID;
        final String postTitle = postmap.get("title").toString();
        final String publishedDateStr = postmap.get("published").toString();
        final ArrayList<DownloadLink> kemonoResults = new ArrayList<DownloadLink>();
        int numberofResultsSimpleCount = 0;
        int index = 0;
        final Map<String, Object> filemap = (Map<String, Object>) postmap.get("file");
        if (dupes == null) {
            dupes = new HashSet<String>();
        }
        if (!filemap.isEmpty()) {
            final DownloadLink media = buildFileDownloadLinkAPI(dupes, filemap, index);
            kemonoResults.add(media);
            index++;
            numberofResultsSimpleCount++;
        }
        final List<Map<String, Object>> attachments = (List<Map<String, Object>>) postmap.get("attachments");
        for (final Map<String, Object> attachment : attachments) {
            final DownloadLink media = buildFileDownloadLinkAPI(dupes, attachment, index);
            if (media != null) {
                kemonoResults.add(media);
                index++;
            }
            numberofResultsSimpleCount++;
        }
        logger.info("Portal: " + portal + " | UserID: " + userID + " | PostID: " + postID + " | File items in API response: " + numberofResultsSimpleCount + " | Number of unique file items: " + kemonoResults.size());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = getFilePackageForPostCrawler(portal, userID, postID, postTitle);
        final String postTextContent = (String) postmap.get("content");
        if (!StringUtils.isEmpty(postTextContent)) {
            final KemonoPartyConfig cfg = PluginJsonConfig.get(getConfigInterface());
            final TextCrawlMode mode = cfg.getTextCrawlMode();
            if (cfg.isCrawlHttpLinksFromPostContent()) {
                final String[] urls = HTMLParser.getHttpLinks(postTextContent, br.getURL());
                if (urls != null && urls.length > 0) {
                    for (final String url : urls) {
                        ret.add(this.createDownloadlink(url));
                    }
                }
            }
            if (mode == TextCrawlMode.ALWAYS || (mode == TextCrawlMode.ONLY_IF_NO_MEDIA_ITEMS_ARE_FOUND && kemonoResults.isEmpty())) {
                ensureInitHosterplugin();
                final DownloadLink textfile = new DownloadLink(this.hostPlugin, this.getHost(), posturl);
                textfile.setProperty(KemonoParty.PROPERTY_TEXT, postTextContent);
                textfile.setFinalFileName(fp.getName() + ".txt");
                try {
                    textfile.setDownloadSize(postTextContent.getBytes("UTF-8").length);
                } catch (final UnsupportedEncodingException ignore) {
                    ignore.printStackTrace();
                }
                kemonoResults.add(textfile);
            }
        }
        for (final DownloadLink kemonoResult : kemonoResults) {
            if (postTitle != null) {
                kemonoResult.setProperty(KemonoParty.PROPERTY_TITLE, postTitle);
            }
            if (publishedDateStr != null) {
                kemonoResult.setProperty(KemonoParty.PROPERTY_DATE, publishedDateStr);
            }
            kemonoResult.setProperty(KemonoParty.PROPERTY_PORTAL, portal);
            kemonoResult.setProperty(KemonoParty.PROPERTY_USERID, userID);
            kemonoResult.setProperty(KemonoParty.PROPERTY_POSTID, postID);
            kemonoResult.setAvailable(true);
            /* Add kemono item to our list of total results. */
            ret.add(kemonoResult);
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            fp.setName(String.format("[@DEV: %d Expected kemono results] ", kemonoResults.size()) + fp.getName());
        }
        fp.addLinks(ret);
        return ret;
    }

    private DownloadLink buildFileDownloadLinkAPI(final HashSet<String> dupes, final Map<String, Object> filemap, final int index) throws PluginException {
        this.ensureInitHosterplugin();
        final String filename = filemap.get("name").toString();
        final String filepath = filemap.get("path").toString();
        final String url = "https://" + this.getHost() + "/data" + filepath + "?f=" + Encoding.urlEncode(filename);
        final String sha256hash = KemonoParty.getSha256HashFromURL(url);
        final String dupeCheckString;
        if (sha256hash != null) {
            dupeCheckString = sha256hash;
        } else {
            dupeCheckString = filepath;
        }
        if (!dupes.add(dupeCheckString)) {
            /* Skip dupe */
            return null;
        }
        final DownloadLink media = new DownloadLink(this.hostPlugin, this.getHost(), url);
        media.setFinalFileName(filename);
        media.setProperty(KemonoParty.PROPERTY_BETTER_FILENAME, filename);
        media.setProperty(KemonoParty.PROPERTY_POST_CONTENT_INDEX, index);
        if (sha256hash != null) {
            media.setSha256Hash(sha256hash);
        }
        return media;
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlPostWebsite(final CryptedLink param, final String portal, final String userID, final String postID) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (portal == null || userID == null || postID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String posturl = "https://" + this.getHost() + "/" + portal + "/user/" + userID + "/post/" + postID;
        br.setAllowedResponseCodes(500);// DDOS-GUARD
        int retry = 3;
        while (retry > 0) {
            br.getPage(posturl);
            if (br.getHttpConnection().getResponseCode() == 500 && !isAbort()) {
                sleep(1000, param);
                retry--;
            } else {
                break;
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().matches(TYPE_POST)) {
            /* E.g. redirect to main page of user because single post does not exist */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String postTitle = br.getRegex("class=\"post__title\">\\s*<span>([^<]+)</span>").getMatch(0);
        if (postTitle != null) {
            postTitle = Encoding.htmlDecode(postTitle).trim();
        }
        String publishedDateStr = br.getRegex("\"post__published\"[^>]*>\\s*<time[^>]*class\\s*=\\s*\"timestamp[^>]*datetime\\s*=\\s*\"\\s*([0-9\\-: ]+)").getMatch(0);
        if (publishedDateStr == null) {
            publishedDateStr = br.getRegex("<meta name\\s*=\\s*\"published\"\\s*content\\s*=\\s*\"\\s*([0-9\\-: ]+)").getMatch(0);
        }
        final ArrayList<DownloadLink> kemonoResults = new ArrayList<DownloadLink>();
        final String[] directURLs = br.getRegex("\"((https?://[^/]+)?/data/[^\"]+)").getColumn(0);
        if (directURLs != null && directURLs.length > 0) {
            /* Remove duplicates from results so our index will be correct down below. */
            ensureInitHosterplugin();
            final ArrayList<String> videoItemsToSkip = new ArrayList<String>();
            final HashSet<String> dups = new HashSet<String>();
            int index = 0;
            final ArrayList<DownloadLink> videoItemsUnfiltered = new ArrayList<DownloadLink>();
            for (final String directURL : directURLs) {
                final String urlFull = br.getURL(directURL).toString();
                final String sha256hash = KemonoParty.getSha256HashFromURL(urlFull);
                final String dupeCheckString;
                if (sha256hash != null) {
                    dupeCheckString = sha256hash;
                } else {
                    dupeCheckString = new URL(urlFull).getPath();
                }
                if (!dups.add(dupeCheckString)) {
                    /* Skip dupes */
                    continue;
                }
                final DownloadLink media = new DownloadLink(this.hostPlugin, this.getHost(), urlFull);
                media.setProperty(KemonoParty.PROPERTY_POST_CONTENT_INDEX, index);
                boolean isFilenameFromHTML = false;
                String betterFilename = getBetterFilenameFromURL(urlFull);
                if (betterFilename == null) {
                    /* 2023-03-01 */
                    betterFilename = br.getRegex(Pattern.quote(directURL) + "\"\\s+download=\"([^\"]+)\"").getMatch(0);
                    isFilenameFromHTML = true;
                }
                if (!StringUtils.isEmpty(betterFilename)) {
                    betterFilename = Encoding.htmlDecode(betterFilename).trim();
                    media.setFinalFileName(betterFilename);
                    if (isFilenameFromHTML) {
                        media.setProperty(KemonoParty.PROPERTY_BETTER_FILENAME, betterFilename);
                    }
                    media.setProperty(DirectHTTP.FIXNAME, betterFilename);
                    final String internalVideoFilename = new Regex(urlFull, "(?i)([a-f0-9]{64}\\.(m4v|mp4))").getMatch(0);
                    if (internalVideoFilename != null) {
                        videoItemsToSkip.add(internalVideoFilename);
                    }
                }
                if (sha256hash != null) {
                    media.setSha256Hash(sha256hash);
                }
                videoItemsUnfiltered.add(media);
                index++;
            }
            if (videoItemsToSkip.size() > 0) {
                logger.info("Filtering duplicated video items: " + videoItemsUnfiltered);
                for (final DownloadLink link : videoItemsUnfiltered) {
                    boolean filter = false;
                    for (final String videoItemToSkip : videoItemsToSkip) {
                        if (link.getPluginPatternMatcher().endsWith(videoItemToSkip)) {
                            filter = true;
                            break;
                        }
                    }
                    if (!filter) {
                        kemonoResults.add(link);
                    }
                }
            } else {
                kemonoResults.addAll(videoItemsUnfiltered);
            }
        }
        final FilePackage fp = getFilePackageForPostCrawler(portal, userID, postID, postTitle);
        final String postTextContent = br.getRegex("<div\\s*class\\s*=\\s*\"post__content\"[^>]*>(.+)</div>\\s*<footer").getMatch(0);
        if (!StringUtils.isEmpty(postTextContent)) {
            final KemonoPartyConfig cfg = PluginJsonConfig.get(getConfigInterface());
            final TextCrawlMode mode = cfg.getTextCrawlMode();
            if (cfg.isCrawlHttpLinksFromPostContent()) {
                final String[] urls = HTMLParser.getHttpLinks(postTextContent, br.getURL());
                if (urls != null && urls.length > 0) {
                    for (final String url : urls) {
                        ret.add(this.createDownloadlink(url));
                    }
                }
            }
            if (mode == TextCrawlMode.ALWAYS || (mode == TextCrawlMode.ONLY_IF_NO_MEDIA_ITEMS_ARE_FOUND && kemonoResults.isEmpty())) {
                ensureInitHosterplugin();
                final DownloadLink textfile = new DownloadLink(this.hostPlugin, this.getHost(), posturl);
                textfile.setProperty(KemonoParty.PROPERTY_TEXT, postTextContent);
                textfile.setFinalFileName(fp.getName() + ".txt");
                try {
                    textfile.setDownloadSize(postTextContent.getBytes("UTF-8").length);
                } catch (final UnsupportedEncodingException ignore) {
                    ignore.printStackTrace();
                }
                kemonoResults.add(textfile);
            }
        }
        for (final DownloadLink kemonoResult : kemonoResults) {
            if (postTitle != null) {
                kemonoResult.setProperty(KemonoParty.PROPERTY_TITLE, postTitle);
            }
            if (publishedDateStr != null) {
                kemonoResult.setProperty(KemonoParty.PROPERTY_DATE, publishedDateStr);
            }
            kemonoResult.setProperty(KemonoParty.PROPERTY_PORTAL, portal);
            kemonoResult.setProperty(KemonoParty.PROPERTY_USERID, userID);
            kemonoResult.setProperty(KemonoParty.PROPERTY_POSTID, postID);
            kemonoResult.setAvailable(true);
            /* Add kemono item to our list of total results. */
            ret.add(kemonoResult);
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            fp.setName(String.format("[@DEV: %d Expected kemono results] ", kemonoResults.size()) + fp.getName());
        }
        fp.addLinks(ret);
        return ret;
    }

    public static String getBetterFilenameFromURL(final String url) throws MalformedURLException {
        final UrlQuery query = UrlQuery.parse(url);
        final String betterFilename = query.get("f");
        if (betterFilename != null) {
            return Encoding.htmlDecode(betterFilename).trim();
        } else {
            return null;
        }
    }

    private void ensureInitHosterplugin() throws PluginException {
        if (this.hostPlugin == null) {
            this.hostPlugin = (KemonoParty) getNewPluginForHostInstance(this.getHost());
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* Try to avoid getting blocked by DDOS-GUARD / rate-limited. */
        return 1;
    }

    @Override
    public Class<? extends KemonoPartyConfig> getConfigInterface() {
        if ("kemono.party".equalsIgnoreCase(getHost())) {
            return KemonoPartyConfig.class;
        } else {
            return KemonoPartyConfigCoomerParty.class;
        }
    }
}
