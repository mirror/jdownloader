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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.TiktokCom;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.TiktokConfig;
import org.jdownloader.plugins.components.config.TiktokConfig.CrawlMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { TiktokCom.class })
public class TiktokComCrawler extends PluginForDecrypt {
    public TiktokComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        TiktokCom.setRequestLimits();
    }

    public static List<String[]> getPluginDomains() {
        return TiktokCom.getPluginDomains();
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
            ret.add("https?://(?:(?:www|vm)\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_REDIRECT       = "https?://vm\\.[^/]+/([A-Za-z0-9]+).*";
    private final String TYPE_APP            = "https?://[^/]+/t/([A-Za-z0-9]+).*";
    private final String TYPE_USER_USERNAME  = "https?://[^/]+/@([^\\?/]+).*";
    private final String TYPE_USER_USER_ID   = "https?://[^/]+/share/user/(\\d+).*";
    private final String TYPE_PLAYLIST_TAG   = "https?://[^/]+/tag/([^/]+)";
    private final String TYPE_PLAYLIST_MUSIC = "https?://[^/]+/music/([a-z0-9\\-]+)-(\\d+)";
    /**
     * E.g. https://www.tiktok.com/foryou?is_from_webapp=v1&item_id=12345#/@jewellry2022/video/12345 </br> --> URLs to single video from
     * recommendation
     */
    private final String TYPE_VIDEO          = "https?://[^/]+.*/(@[^/]+/video/\\d+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches(TYPE_REDIRECT) || param.getCryptedUrl().matches(TYPE_APP)) {
            /* Single redirect URLs */
            br.setFollowRedirects(false);
            br.getPage(param.getCryptedUrl().replaceFirst("http://", "https://"));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        } else if (plg.canHandle(param.getCryptedUrl())) {
            /* Single video URL --> Is handled by host plugin */
            decryptedLinks.add(this.createDownloadlink(param.getCryptedUrl()));
            return decryptedLinks;
        } else if (param.getCryptedUrl().matches(TYPE_VIDEO)) {
            /* Single video in special form --> Form new URL that host plugin can handle */
            final String urlpart = new Regex(param.getCryptedUrl(), TYPE_VIDEO).getMatch(0);
            decryptedLinks.add(this.createDownloadlink("https://" + this.getHost() + "/" + urlpart));
            return decryptedLinks;
        } else if (param.getCryptedUrl().matches(TYPE_USER_USERNAME) || param.getCryptedUrl().matches(TYPE_USER_USER_ID)) {
            return crawlProfile(param);
        } else if (param.getCryptedUrl().matches(TYPE_PLAYLIST_TAG)) {
            return this.crawlPlaylistTag(param);
        } else if (param.getCryptedUrl().matches(TYPE_PLAYLIST_MUSIC)) {
            return this.crawlPlaylistMusic(param);
        } else {
            // unsupported url pattern
            return new ArrayList<DownloadLink>(0);
        }
    }

    public ArrayList<DownloadLink> crawlProfile(final CryptedLink param) throws Exception {
        if (PluginJsonConfig.get(TiktokConfig.class).getProfileCrawlerMaxItemsLimit() == 0) {
            logger.info("User has disabled profile crawler --> Returning empty array");
            return new ArrayList<DownloadLink>();
        }
        if (PluginJsonConfig.get(TiktokConfig.class).getCrawlMode() == CrawlMode.API) {
            return crawlProfileAPI(param);
        } else {
            return crawlProfileWebsite(param);
        }
    }

    /**
     * Use website to crawl all videos of a user. </br> Pagination hasn't been implemented so this will only find the first batch of items -
     * usually around 30 items!
     */
    public ArrayList<DownloadLink> crawlProfileWebsite(final CryptedLink param) throws Exception {
        prepBRWebsite(br);
        /* Login whenever possible */
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
            ((TiktokCom) plg).login(account, false);
        }
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Profile does not exist */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String usernameSlug = new Regex(br.getURL(), TYPE_USER_USERNAME).getMatch(0);
        if (usernameSlug == null) {
            /* Redirect to somewhere else */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (TiktokCom.isBotProtectionActive(this.br)) {
            final UrlQuery query = TiktokCom.getWebsiteQuery();
            query.add("keyword", Encoding.urlEncode(usernameSlug));
            br.getPage("https://www." + this.getHost() + "/api/search/general/preview/?" + query.toString());
            sleep(1000, param);// this somehow bypass the protection, maybe calling api twice sets a cookie?
            br.getPage("https://www." + this.getHost() + "/api/search/general/preview/?" + query.toString());
            br.getPage(param.getCryptedUrl());
        }
        this.botProtectionCheck(br);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        FilePackage fp = null;
        String username = null;
        try {
            /* First try the "hard" way */
            String json = br.getRegex("window\\['SIGI_STATE'\\]\\s*=\\s*(\\{.*?\\});").getMatch(0);
            if (json == null) {
                json = br.getRegex("<script\\s*id\\s*=\\s*\"SIGI_STATE\"[^>]*>\\s*(\\{.*?\\});?\\s*</script>").getMatch(0);
            }
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final Map<String, Map<String, Object>> itemModule = (Map<String, Map<String, Object>>) entries.get("ItemModule");
            final Map<String, Object> userPost = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "ItemList/user-post");
            final List<Map<String, Object>> preloadList = (List<Map<String, Object>>) userPost.get("preloadList");
            /* Typically we get up to 30 items per page. In some cases we get only 28 or 29 for some reason. */
            final Collection<Map<String, Object>> videos = itemModule.values();
            int index = 0;
            for (final Map<String, Object> video : videos) {
                final Map<String, Object> preloadInfo = preloadList.get(index);
                final Map<String, Object> stats = (Map<String, Object>) video.get("stats");
                final Map<String, Object> streamInfo = (Map<String, Object>) video.get("video");
                final String author = video.get("author").toString();
                final String videoID = (String) video.get("id");
                final String createTimeStr = (String) video.get("createTime");
                final String description = (String) video.get("desc");
                String directurl = (String) streamInfo.get("downloadAddr");
                if (StringUtils.isEmpty(directurl)) {
                    directurl = (String) streamInfo.get("playAddr");
                }
                if (StringUtils.isEmpty(directurl)) {
                    directurl = preloadInfo.get("url").toString();
                }
                if (fp == null) {
                    username = author;
                    fp = this.getFilePackage(username);
                }
                final DownloadLink dl = this.createDownloadlink(getContentURL(author, videoID));
                final String dateFormatted = formatDate(Long.parseLong(createTimeStr));
                dl.setAvailable(true);
                TiktokCom.setDescriptionAndHashtags(dl, description);
                dl.setProperty(TiktokCom.PROPERTY_USERNAME, author);
                dl.setProperty(TiktokCom.PROPERTY_USER_ID, video.get("authorId"));
                dl.setProperty(TiktokCom.PROPERTY_DATE, dateFormatted);
                dl.setProperty(TiktokCom.PROPERTY_VIDEO_ID, videoID);
                TiktokCom.setLikeCount(dl, (Number) stats.get("diggCount"));
                TiktokCom.setPlayCount(dl, (Number) stats.get("playCount"));
                TiktokCom.setShareCount(dl, (Number) stats.get("shareCount"));
                TiktokCom.setCommentCount(dl, (Number) stats.get("commentCount"));
                if (!StringUtils.isEmpty(directurl)) {
                    dl.setProperty(TiktokCom.PROPERTY_DIRECTURL_WEBSITE, directurl);
                }
                TiktokCom.setFilename(dl);
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
                if (ret.size() == cfg.getProfileCrawlerMaxItemsLimit()) {
                    logger.info("Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit());
                    return ret;
                }
                index++;
            }
            if ((Boolean) userPost.get("hasMore") && cfg.isAddDummyURLProfileCrawlerWebsiteModeMissingPagination()) {
                final DownloadLink dummy = createLinkCrawlerRetry(getCurrentLink(), new DecrypterRetryException(RetryReason.FILE_NOT_FOUND));
                dummy.setFinalFileName("CANNOT_CRAWL_MORE_THAN_" + videos.size() + "_ITEMS_OF_PROFILE_" + usernameSlug);
                dummy.setComment("This crawler plugin cannot handle pagination yet thus it is currently impossible to crawl more than " + videos.size() + " items. Check this forum thread for more info: https://board.jdownloader.org/showthread.php?t=79982");
                if (fp != null) {
                    dummy._setFilePackage(fp);
                }
                distribute(dummy);
                ret.add(dummy);
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        if (ret.isEmpty()) {
            /* Last chance fallback */
            logger.warning("Fallback to plain html handling");
            final String[] videoIDs = br.getRegex(usernameSlug + "/video/(\\d+)\"").getColumn(0);
            for (final String videoID : videoIDs) {
                final DownloadLink dl = this.createDownloadlink(getContentURL(usernameSlug, videoID));
                TiktokCom.setFilename(dl);
                dl.setAvailable(true);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                ret.add(dl);
                if (ret.size() == cfg.getProfileCrawlerMaxItemsLimit()) {
                    logger.info("Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit());
                    return ret;
                }
            }
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlProfileAPI(final CryptedLink param) throws Exception {
        String user_id = null;
        if (param.getCryptedUrl().matches(TYPE_USER_USER_ID)) {
            /* user_id is given inside URL. */
            user_id = new Regex(param.getCryptedUrl(), TYPE_USER_USER_ID).getMatch(0);
        } else {
            /* Only username is given and we need to find the user_id. */
            final String usernameSlug = new Regex(param.getCryptedUrl(), TYPE_USER_USERNAME).getMatch(0);
            if (usernameSlug == null) {
                /* Developer mistake */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setFollowRedirects(true);
            /* Find userID */
            final Browser website = br.cloneBrowser();
            TiktokCom.prepBRWebAPI(website);
            final UrlQuery query = TiktokCom.getWebsiteQuery();
            query.add("keyword", Encoding.urlEncode(usernameSlug));
            website.getPage("https://www." + this.getHost() + "/api/search/user/preview/?" + query.toString());
            final Map<String, Object> searchResults = JSonStorage.restoreFromString(website.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            final List<Map<String, Object>> sug_list = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(searchResults, "sug_list/");
            for (Map<String, Object> sug : sug_list) {
                final Map<String, Object> info = (Map<String, Object>) sug.get("extra_info");
                final String sug_uniq_id = info != null ? StringUtils.valueOfOrNull(info.get("sug_uniq_id")) : null;
                if (StringUtils.equals(usernameSlug, sug_uniq_id)) {
                    user_id = info.get("sug_user_id").toString();
                    break;
                }
            }
            if (user_id == null) {
                logger.info("Using fallback method to find userID!");
                website.getPage(param.getCryptedUrl());
                user_id = website.getRegex("\"authorId\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                if (user_id == null && TiktokCom.isBotProtectionActive(website)) {
                    sleep(1000, param);// this somehow bypass the protection, maybe calling api twice sets a cookie?
                    website.getPage("https://www." + this.getHost() + "/api/search/general/preview/?" + query.toString());
                    website.getPage(param.getCryptedUrl());
                    user_id = website.getRegex("\"authorId\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                }
            }
            if (user_id == null) {
                logger.info("Profile doesn't exist or it's a private profile");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        prepBRAPI(this.br);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final int maxItemsPerPage = 21;
        final UrlQuery query = TiktokCom.getAPIQuery();
        query.add("user_id", user_id);
        query.add("count", Integer.toString(maxItemsPerPage));
        query.add("max_cursor", "0");
        query.add("min_cursor", "0");
        query.add("retry_type", "no_retry");
        query.add("device_id", generateDeviceID());
        int page = 1;
        FilePackage fp = null;
        String author = null;
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        do {
            TiktokCom.accessAPI(br, "/aweme/post", query);
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            final List<Map<String, Object>> videos = (List<Map<String, Object>>) entries.get("aweme_list");
            if (videos.isEmpty()) {
                if (ret.isEmpty()) {
                    /* User has no video uploads at all. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            for (final Map<String, Object> aweme_detail : videos) {
                final DownloadLink link = processVideo(aweme_detail);
                if (fp == null) {
                    /*
                     * Collect author name on first round because it is not always given before e.g. not given if user adds URL of type
                     * TYPE_USER_USER_ID.
                     */
                    author = link.getStringProperty(TiktokCom.PROPERTY_USERNAME);
                    fp = getFilePackage(author);
                }
                link._setFilePackage(fp);
                ret.add(link);
                distribute(link);
                if (ret.size() == cfg.getProfileCrawlerMaxItemsLimit()) {
                    logger.info("Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit());
                    return ret;
                }
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                break;
            } else if (((Number) entries.get("has_more")).intValue() != 1) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (videos.size() < maxItemsPerPage) {
                /* Extra fail-safe */
                logger.info("Stopping because: Current page contained less items than " + maxItemsPerPage);
                break;
            }
            query.addAndReplace("max_cursor", entries.get("max_cursor").toString());
            page++;
        } while (true);
        return ret;
    }

    public ArrayList<DownloadLink> crawlPlaylistTag(final CryptedLink param) throws Exception {
        if (PluginJsonConfig.get(TiktokConfig.class).getTagCrawlerMaxItemsLimit() == 0) {
            logger.info("User has disabled tag crawler --> Returning empty array");
            return new ArrayList<DownloadLink>();
        }
        return crawlPlaylistAPI(param);
    }

    public ArrayList<DownloadLink> crawlPlaylistAPI(final CryptedLink param) throws Exception {
        final String tagName = new Regex(param.getCryptedUrl(), TYPE_PLAYLIST_TAG).getMatch(0);
        if (tagName == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBRWebsite(br);
        br.getPage(param.getCryptedUrl());
        botProtectionCheck(br);
        final String tagID = br.getRegex("snssdk\\d+://challenge/detail/(\\d+)").getMatch(0);
        if (tagID == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("tag - " + tagName);
        return crawlPlaylistAPI("/challenge/aweme", "ch_id", tagID, fp);
    }

    /** Under development */
    public ArrayList<DownloadLink> crawlPlaylistMusic(final CryptedLink param) throws Exception {
        // TODO
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // if (PluginJsonConfig.get(TiktokConfig.class).getTagCrawlerMaxItemsLimit() == 0) {
        // logger.info("User has disabled tag crawler --> Returning empty array");
        // return new ArrayList<DownloadLink>();
        // }
        return crawlPlaylistMusicAPI(param);
    }

    /** Under development */
    public ArrayList<DownloadLink> crawlPlaylistMusicAPI(final CryptedLink param) throws Exception {
        // TODO
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_PLAYLIST_MUSIC);
        final String musicPlaylistTitle = urlinfo.getMatch(0);
        final String musicID = urlinfo.getMatch(1);
        if (musicPlaylistTitle == null || musicID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("music - " + musicPlaylistTitle);
        return crawlPlaylistAPI("/music/aweme", "music_id", musicID, fp);
    }

    /** Generic function to crawl playlist-like stuff. */
    public ArrayList<DownloadLink> crawlPlaylistAPI(final String apiPath, final String playlistKeyName, final String playlistID, final FilePackage fp) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        logger.info("Crawling playlist or playlist-like item with ID: " + playlistID);
        final int maxItemsPerPage = 20;
        final UrlQuery query = TiktokCom.getAPIQuery();
        query.add(playlistKeyName, playlistID);
        query.add("cursor", "0");
        query.add("count", Integer.toString(maxItemsPerPage));
        query.add("type", "5");
        query.add("device_id", generateDeviceID());
        prepBRAPI(this.br);
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        int page = 1;
        do {
            TiktokCom.accessAPI(br, apiPath, query);
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            final List<Map<String, Object>> videos = (List<Map<String, Object>>) entries.get("aweme_list");
            if (videos.isEmpty()) {
                if (ret.isEmpty()) {
                    /* There are no videos with this tag available. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            for (final Map<String, Object> aweme_detail : videos) {
                final DownloadLink link = this.processVideo(aweme_detail);
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                ret.add(link);
                distribute(link);
                if (ret.size() == cfg.getTagCrawlerMaxItemsLimit()) {
                    logger.info("Stopping because: Reached user defined max items limit: " + cfg.getTagCrawlerMaxItemsLimit());
                    return ret;
                }
            }
            logger.info("Crawled page " + page + "Number of items on current page " + videos.size() + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (((Integer) entries.get("has_more")).intValue() != 1) {
                logger.info("Stopping because: Reached end");
                break;
            }
            final String nextCursor = entries.get("cursor").toString();
            if (StringUtils.isEmpty(nextCursor)) {
                /* Additional fail-safe */
                logger.info("Stopping because: Failed to find cursor --> Reached end?");
                break;
            } else {
                query.addAndReplace("cursor", nextCursor);
                page++;
            }
        } while (true);
        return ret;
    }

    private ArrayList<DownloadLink> processVideoList(final List<Map<String, Object>> videos, final FilePackage fp) throws PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Map<String, Object> aweme_detail : videos) {
            final DownloadLink link = processVideo(aweme_detail);
            link._setFilePackage(fp);
            ret.add(link);
            distribute(link);
        }
        return ret;
    }

    private DownloadLink processVideo(final Map<String, Object> aweme_detail) throws PluginException {
        final Map<String, Object> author = (Map<String, Object>) aweme_detail.get("author");
        final DownloadLink link = this.createDownloadlink(getContentURL(author.get("unique_id").toString(), aweme_detail.get("aweme_id").toString()));
        TiktokCom.parseFileInfoAPI(link, aweme_detail);
        return link;
    }

    /* Throws exception if bot protection is active according to given browser instances' html code. */
    private void botProtectionCheck(final Browser br) throws DecrypterRetryException {
        if (TiktokCom.isBotProtectionActive(br)) {
            throw new DecrypterRetryException(RetryReason.CAPTCHA, "Bot protection active, cannot crawl any items", null, null);
        }
    }

    private FilePackage getFilePackage(final String name) {
        final FilePackage fp = FilePackage.getInstance();
        fp.setCleanupPackageName(false);
        fp.setName(name);
        return fp;
    }

    private String getContentURL(final String user, final String videoID) {
        return "https://www." + this.getHost() + "/@" + sanitizeUsername(user) + "/video/" + videoID;
    }

    /** Cleans up given username String. */
    public static String sanitizeUsername(final String user) {
        if (user == null) {
            return null;
        } else if (user.startsWith("@")) {
            return user.substring(1, user.length());
        } else {
            return user;
        }
    }

    /** Returns random 19 digit string. */
    public static String generateDeviceID() {
        return TiktokCom.generateRandomString("1234567890", 19);
    }

    public static String formatDate(final long date) {
        if (date <= 0) {
            return null;
        }
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date * 1000);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date);
        }
        return formattedDate;
    }

    /** Wrapper */
    private Browser prepBRWebsite(final Browser br) {
        return TiktokCom.prepBRWebsite(br);
    }

    /** Wrapper */
    private Browser prepBRWebAPI(final Browser br) {
        return TiktokCom.prepBRWebAPI(br);
    }

    /** Wrapper */
    private Browser prepBRAPI(final Browser br) {
        return TiktokCom.prepBRAPI(br);
    }
}
