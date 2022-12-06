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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.RedditConfig;
import org.jdownloader.plugins.components.config.RedditConfig.CommentsPackagenameScheme;
import org.jdownloader.plugins.components.config.RedditConfig.FilenameScheme;
import org.jdownloader.plugins.components.config.RedditConfig.TextCrawlerMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.RedditCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { RedditCom.class })
public class RedditComCrawler extends PluginForDecrypt {
    public RedditComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-07-21: Let's be gentle and avoid doing too many API requests. */
        return 1;
    }

    private static List<String[]> getPluginDomains() {
        return RedditCom.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        ret.add("https?://(?:(?:www|old)\\.)?reddit\\.com/(?:r/[^/]+(?:/comments/[a-z0-9]+(/[A-Za-z0-9\\-_]+/?)?)?|gallery/[a-z0-9]+|user/[^/]+(?:/saved)?)" + "|" + PATTERN_SELFHOSTED_VIDEO);
        return ret.toArray(new String[0]);
    }

    public static final String  PATTERN_SELFHOSTED_IMAGE   = "https?://i\\.redd\\.it/([a-z0-9]+)\\.[A-Za-z]{2,5}";
    public static final String  PATTERN_SELFHOSTED_VIDEO   = "https?://v\\.redd\\.it/([a-z0-9]+)";
    private static final String PATTERN_SUBREDDIT          = "(?:https?://[^/]+)?/r/([^/]+)$";
    private static final String PATTERN_SUBREDDIT_COMMENTS = "(?:https?://[^/]+)?/r/([^/]+)/comments/([a-z0-9]+)(/([^/\\?]+)/?)?";
    private static final String PATTERN_GALLERY            = "(?:https?://[^/]+)?/gallery/([a-z0-9]+)";
    private static final String PATTERN_USER               = "(?:https?://[^/]+)?/user/([^/]+)";
    private static final String PATTERN_USER_SAVED_OBJECTS = "(?:https?://[^/]+)?/user/([^/]+)/saved";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        RedditCom.prepBRAPI(this.br);
        if (param.getCryptedUrl().matches(PATTERN_SELFHOSTED_VIDEO)) {
            return crawlSingleVideourl(param);
        } else if (param.getCryptedUrl().matches(PATTERN_USER_SAVED_OBJECTS)) {
            return crawlUserSavedObjects(param);
        } else if (param.getCryptedUrl().matches(PATTERN_USER)) {
            return crawlUser(param);
        } else if (param.getCryptedUrl().matches(PATTERN_SUBREDDIT_COMMENTS)) {
            return crawlCommentURL(param);
        } else if (param.getCryptedUrl().matches(PATTERN_GALLERY)) {
            return this.crawlGalleryURL(param);
        } else {
            return crawlSubreddit(param);
        }
    }

    private ArrayList<DownloadLink> crawlSingleVideourl(final CryptedLink param) throws Exception {
        final String videoID = new Regex(param.getCryptedUrl(), PATTERN_SELFHOSTED_VIDEO).getMatch(0);
        if (videoID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(false);
        brc.getPage("https://www." + this.getHost() + "/video/" + videoID);
        final String redirect = brc.getRedirectLocation();
        if (redirect == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String commentID = new Regex(redirect, PATTERN_SUBREDDIT_COMMENTS).getMatch(1);
        if (commentID == null) {
            /* Redirect to unsupported/unexpected URL. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return crawlComments(commentID);
    }

    private ArrayList<DownloadLink> crawlSubreddit(final CryptedLink param) throws Exception {
        /* Prepare crawl process */
        final String subredditTitle = new Regex(param.getCryptedUrl(), PATTERN_SUBREDDIT).getMatch(0);
        if (subredditTitle == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final int maxPagesToCrawl = PluginJsonConfig.get(RedditConfig.class).getSubredditCrawlerMaxPages();
        if (maxPagesToCrawl == 0) {
            logger.info("User has disabled subreddit crawler");
            return new ArrayList<DownloadLink>();
        } else {
            /* Crawl until we've reached the end. */
            final FilePackage fp = FilePackage.getInstance();
            final String url = "https://www." + this.getHost() + "/r/" + subredditTitle + "/.json";
            fp.setName("/r/" + subredditTitle);
            return this.crawlPagination(url, fp, maxPagesToCrawl);
        }
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param) throws Exception {
        /* Prepare crawl process */
        final String userTitle = new Regex(param.getCryptedUrl(), PATTERN_USER).getMatch(0);
        if (userTitle == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final int maxPagesToCrawl = PluginJsonConfig.get(RedditConfig.class).getProfileCrawlerMaxPages();
        if (maxPagesToCrawl == 0) {
            logger.info("User has disabled user profile crawler");
            return new ArrayList<DownloadLink>();
        } else {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName("/u/" + userTitle);
            final String url = "https://www." + this.getHost() + "/user/" + userTitle + "/.json";
            return this.crawlPagination(url, fp, maxPagesToCrawl);
        }
    }

    /**
     * Use this to crawl complete subreddits or user-profiles. </br>
     *
     * @param url
     *            json-URL to crawl
     * @param fp
     *            FilePackage to set on crawled items.
     * @param maxPage
     *            Max. page to crawl. -1 = crawl all pages.
     */
    private ArrayList<DownloadLink> crawlPagination(final String url, final FilePackage fp, final int maxPage) throws Exception {
        final ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        final Set<String> lastItemDupes = new HashSet<String>();
        final int maxItemsPerCall = 100;
        final UrlQuery query = new UrlQuery();
        // query.add("type", "links");
        query.add("limit", Integer.toString(maxItemsPerCall));
        int page = 1;
        int numberofItemsCrawled = 0;
        fp.setAllowMerge(true);
        fp.setAllowInheritance(true);
        fp.setCleanupPackageName(false);
        do {
            br.getPage(url + "?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> data = (Map<String, Object>) root.get("data");
            final int numberofItemsOnCurrentPage = ((Number) data.get("dist")).intValue();
            numberofItemsCrawled += numberofItemsOnCurrentPage;
            crawledLinks.addAll(this.crawlListing(root, fp));
            logger.info("Crawled page " + page + " | Crawled items so far: " + numberofItemsCrawled);
            final String nextPageToken = (String) data.get("after");
            /* Multiple fail safes to prevent an infinite loop. */
            if (StringUtils.isEmpty(nextPageToken)) {
                logger.info("Stopping because: nextPageToken is not given");
                break;
            } else if (!lastItemDupes.add(nextPageToken)) {
                logger.info("Stopping because: We already know this nextPageToken");
                break;
            } else if (numberofItemsOnCurrentPage < maxItemsPerCall) {
                logger.info("Stopping because: Found only " + numberofItemsOnCurrentPage + "/" + maxItemsPerCall + " items this round");
                break;
            } else if (maxPage > -1 && page >= maxPage) {
                logger.info("Stopping because: Reached desired max page: " + maxPage);
                break;
            }
            query.addAndReplace("after", nextPageToken);
            page++;
        } while (!this.isAbort());
        return crawledLinks;
    }

    /** TODO: Try to crawlPagination instead! */
    private ArrayList<DownloadLink> crawlUserSavedObjects(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        /* Login required */
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc == null) {
            throw new AccountRequiredException();
        }
        /* Login */
        final PluginForHost plugin = this.getNewPluginForHostInstance(this.getHost());
        ((jd.plugins.hoster.RedditCom) plugin).login(acc, false);
        final ArrayList<String> lastItemDupes = new ArrayList<String>();
        /* Prepare crawl process */
        final FilePackage fp = FilePackage.getInstance();
        fp.setAllowInheritance(true);
        fp.setName("saved items of user" + acc.getUser());
        final int maxItemsPerCall = 100;
        final UrlQuery query = new UrlQuery();
        query.add("type", "links");
        query.add("limit", Integer.toString(maxItemsPerCall));
        int page = 0;
        do {
            page++;
            logger.info("Crawling page: " + page);
            br.getPage(getApiBaseOauth() + "/user/" + Encoding.urlEncode(acc.getUser()) + "/saved?" + query.toString());
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            crawledLinks.addAll(this.crawlListing(entries, fp));
            entries = (Map<String, Object>) entries.get("data");
            final String fullnameAfter = (String) entries.get("after");
            final long numberofItems = JavaScriptEngineFactory.toLong(entries.get("dist"), 0);
            /* Multiple fail safes to prevent an infinite loop. */
            if (StringUtils.isEmpty(fullnameAfter)) {
                logger.info("Seems like we've crawled everything");
                break;
            } else if (numberofItems < maxItemsPerCall) {
                logger.info("Stopping because we got less than " + maxItemsPerCall + " items");
                break;
            } else if (lastItemDupes.contains(fullnameAfter)) {
                logger.info("Stopping because we already know this fullnameAfter");
                break;
            }
            lastItemDupes.add(fullnameAfter);
            query.remove("after");
            query.add("after", fullnameAfter);
        } while (!this.isAbort());
        return crawledLinks;
    }

    /** 2020-11-11: Currently does the same as {@link #crawlCommentURL()} */
    private ArrayList<DownloadLink> crawlGalleryURL(final CryptedLink param) throws Exception {
        final String commentID = new Regex(param.getCryptedUrl(), PATTERN_GALLERY).getMatch(0);
        return crawlComments(commentID);
    }

    /** According to: https://www.reddit.com/r/redditdev/comments/b8yd3r/reddit_api_possible_to_get_posts_by_id/ */
    private ArrayList<DownloadLink> crawlCommentURL(final CryptedLink param) throws Exception {
        final String commentID = new Regex(param.getCryptedUrl(), PATTERN_SUBREDDIT_COMMENTS).getMatch(1);
        return crawlComments(commentID);
    }

    private ArrayList<DownloadLink> crawlComments(final String commentID) throws Exception {
        if (commentID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        br.getPage("https://www." + this.getHost() + "/comments/" + commentID + "/.json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<Object> ressourcelist = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        /* [0] = post/"first comment" */
        /* [1] = Comments */
        final Map<String, Object> entries = (Map<String, Object>) ressourcelist.get(0);
        crawledLinks.addAll(this.crawlListing(entries, null));
        return crawledLinks;
    }

    private ArrayList<DownloadLink> crawlListing(final Map<String, Object> entries, FilePackage fp) throws Exception {
        /* https://www.reddit.com/dev/api/#fullnames */
        final ArrayList<DownloadLink> crawledItems = new ArrayList<DownloadLink>();
        final List<Object> items = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "data/children");
        final RedditConfig cfg = PluginJsonConfig.get(RedditConfig.class);
        int skippedItems = 0;
        for (final Object itemO : items) {
            final Map<String, Object> post = (Map<String, Object>) itemO;
            final String kind = (String) post.get("kind");
            final Map<String, Object> data = (Map<String, Object>) post.get("data");
            final String postID = (String) data.get("id");
            final String author = (String) data.get("author");
            final long createdDateTimestampMillis = ((Number) data.get("created")).longValue() * 1000;
            final long createdTimedeltaSeconds = (System.currentTimeMillis() - createdDateTimestampMillis) / 1000;
            final String createdTimedeltaString = TimeFormatter.formatSeconds(createdTimedeltaSeconds, 0);
            final String dateFormatted = new SimpleDateFormat("yyy-MM-dd").format(new Date(createdDateTimestampMillis));
            final String title = (String) data.get("title");
            final String subredditTitle = (String) data.get("subreddit");
            final String permalink = (String) data.get("permalink");
            final String postText = (String) data.get("selftext");
            if (!"t3".equalsIgnoreCase(kind)) {
                /*
                 * Skip everything except links (e.g. skips comments, awards and so on. See API docs --> In-text search for "type prefixes")
                 */
                continue;
            } else if (StringUtils.isEmpty(title) || StringUtils.isEmpty(subredditTitle)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean postContainsRealMedia = true;
            final String urlSlug = new Regex(permalink, PATTERN_SUBREDDIT_COMMENTS).getMatch(3);
            if (urlSlug == null) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final ArrayList<DownloadLink> thisCrawledLinks = new ArrayList<DownloadLink>();
            final ArrayList<DownloadLink> thisCrawledExternalLinks = new ArrayList<DownloadLink>();
            try {
                if (fp == null) {
                    /* No packagename given? Set FilePackage with name of comment/post. */
                    fp = FilePackage.getInstance();
                    String packagename;
                    final CommentsPackagenameScheme packagenameScheme = cfg.getPreferredCommentsPackagenameScheme();
                    final String customPackagenameScheme = cfg.getCustomCommentsPackagenameScheme();
                    if (packagenameScheme == CommentsPackagenameScheme.CUSTOM && !StringUtils.isEmpty(customPackagenameScheme)) {
                        packagename = customPackagenameScheme;
                    } else if (packagenameScheme == CommentsPackagenameScheme.DATE_SUBREDDIT_POSTID_SLUG && urlSlug != null) {
                        packagename = "*date*_*subreddit_title*_*post_id*_*post_slug*";
                    } else if (packagenameScheme == CommentsPackagenameScheme.DATE_SUBREDDIT_POSTID_TITLE) {
                        packagename = "*date*_*subreddit_title*_*post_id*_*post_title*";
                    } else {
                        packagename = "*post_title*";
                    }
                    packagename = packagename.replace("*date*", dateFormatted);
                    packagename = packagename.replace("*date_timestamp*", Long.toString(createdDateTimestampMillis));
                    packagename = packagename.replace("*date_timedelta_formatted*", createdTimedeltaString);
                    packagename = packagename.replace("*subreddit_title*", subredditTitle);
                    packagename = packagename.replace("*username*", author);
                    packagename = packagename.replace("*post_id*", postID);
                    packagename = packagename.replace("*post_slug*", urlSlug);
                    packagename = packagename.replace("*post_title*", title);
                    fp.setName(packagename);
                }
                /* Look for single URLs e.g. single pictures (e.g. often imgur.com URLs, can also be selfhosted content) */
                boolean addedRedditSelfhostedVideo = false;
                String maybeExternalURL = (String) data.get("url");
                if (Encoding.isHtmlEntityCoded(maybeExternalURL)) {
                    maybeExternalURL = Encoding.htmlDecode(maybeExternalURL);
                }
                /* The following if statement is not needed is is only here because that field rarely contains a relative URL. */
                if (maybeExternalURL.startsWith("/")) {
                    maybeExternalURL = br.getURL(maybeExternalURL).toString();
                }
                if (!StringUtils.isEmpty(maybeExternalURL) && (maybeExternalURL.matches(PATTERN_SELFHOSTED_VIDEO) || maybeExternalURL.matches(PATTERN_SELFHOSTED_IMAGE) || !this.canHandle(maybeExternalURL))) {
                    final String serverFilename = Plugin.getFileNameFromURL(new URL(maybeExternalURL));
                    final String serverFilenameWithoutExt;
                    if (serverFilename.contains(".")) {
                        serverFilenameWithoutExt = serverFilename.substring(0, serverFilename.lastIndexOf("."));
                    } else {
                        serverFilenameWithoutExt = serverFilename;
                    }
                    final DownloadLink dl = this.createDownloadlink(maybeExternalURL);
                    if (maybeExternalURL.matches(PATTERN_SELFHOSTED_VIDEO)) {
                        dl.setProperty(RedditCom.PROPERTY_SERVER_FILENAME_WITHOUT_EXT, serverFilenameWithoutExt);
                        dl.setProperty(RedditCom.PROPERTY_TYPE, RedditCom.PROPERTY_TYPE_video);
                        addedRedditSelfhostedVideo = true;
                        /* Skip availablecheck as we know that this content is online and is a directurl. */
                        dl.setAvailable(true);
                        thisCrawledLinks.add(dl);
                    } else if (maybeExternalURL.matches(PATTERN_SELFHOSTED_IMAGE)) {
                        dl.setProperty(RedditCom.PROPERTY_SERVER_FILENAME_WITHOUT_EXT, serverFilenameWithoutExt);
                        dl.setProperty(RedditCom.PROPERTY_TYPE, RedditCom.PROPERTY_TYPE_image);
                        /* Skip availablecheck as we know that this content is online and is a directurl. */
                        dl.setAvailable(true);
                        thisCrawledLinks.add(dl);
                    } else {
                        logger.info("Found external URL in 'url' field: " + maybeExternalURL);
                        thisCrawledExternalLinks.add(dl);
                    }
                }
                /* 2022-03-10: When a gallery is removed, 'is_gallery' can be true while 'gallery_data' does not exist. */
                final Object is_galleryO = data.get("is_gallery");
                final Object galleryO = data.get("gallery_data");
                if (is_galleryO == Boolean.TRUE && galleryO != null) {
                    final Map<String, Object> gallery_data = (Map<String, Object>) galleryO;
                    final List<Map<String, Object>> galleryItems = (List<Map<String, Object>>) gallery_data.get("items");
                    final Map<String, Object> media_metadata = (Map<String, Object>) data.get("media_metadata");
                    int imageNumber = 1;
                    for (final Map<String, Object> galleryItem : galleryItems) {
                        final String mediaID = galleryItem.get("media_id").toString();
                        final String caption = (String) galleryItem.get("caption");
                        final Map<String, Object> mediaInfo = (Map<String, Object>) media_metadata.get(mediaID);
                        /* "image/png" --> "png" */
                        String mediaType = (String) mediaInfo.get("m");
                        String extension = getExtensionFromMimeType(mediaType);
                        if (extension == null && mediaType.contains("/")) {
                            final String[] mediaTypeSplit = mediaType.split("/");
                            extension = mediaTypeSplit[mediaTypeSplit.length - 1];
                        }
                        if (extension == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final DownloadLink image = this.createDownloadlink("https://i.redd.it/" + mediaID + "." + extension);
                        image.setProperty(RedditCom.PROPERTY_SERVER_FILENAME_WITHOUT_EXT, mediaID);
                        image.setProperty(RedditCom.PROPERTY_TYPE, RedditCom.PROPERTY_TYPE_image);
                        image.setProperty(RedditCom.PROPERTY_INDEX, imageNumber);
                        image.setProperty(RedditCom.PROPERTY_INDEX_MAX, galleryItems.size());
                        image.setAvailable(true);
                        if (!StringUtils.isEmpty(caption)) {
                            image.setComment(caption);
                        }
                        thisCrawledLinks.add(image);
                        imageNumber++;
                    }
                    break;
                }
                /* Look for embedded content from external sources - the object is always given but can be empty */
                final Object embeddedMediaO = data.get("media_embed");
                if (embeddedMediaO != null) {
                    final Map<String, Object> embeddedMediaInfo = (Map<String, Object>) embeddedMediaO;
                    if (!embeddedMediaInfo.isEmpty()) {
                        logger.info("Found media_embed");
                        String mediaEmbedStr = (String) embeddedMediaInfo.get("content");
                        final String[] urls = HTMLParser.getHttpLinks(mediaEmbedStr, this.br.getURL());
                        for (final String url : urls) {
                            final DownloadLink dl = this.createDownloadlink(url);
                            thisCrawledExternalLinks.add(dl);
                        }
                    }
                }
                /* Look for selfhosted video content. Prefer content without https */
                if (!addedRedditSelfhostedVideo) {
                    final String[] mediaTypes = new String[] { "media", "secure_media" };
                    for (final String mediaType : mediaTypes) {
                        final Object mediaO = data.get(mediaType);
                        if (mediaO != null) {
                            final Map<String, Object> mediaInfo = (Map<String, Object>) mediaO;
                            if (!mediaInfo.isEmpty()) {
                                logger.info("Found mediaType '" + mediaType + "'");
                                /* This is not always given */
                                final Object redditVideoO = mediaInfo.get("reddit_video");
                                if (redditVideoO != null) {
                                    final Map<String, Object> redditVideo = (Map<String, Object>) redditVideoO;
                                    /* TODO: 2022-01-12: Check filenames for such URLs (apply user preferred FilenameScheme) */
                                    String hls_url = (String) redditVideo.get("hls_url");
                                    if (!StringUtils.isEmpty(hls_url)) {
                                        if (Encoding.isHtmlEntityCoded(hls_url)) {
                                            hls_url = Encoding.htmlDecode(hls_url);
                                        }
                                        final String videoID = new Regex(hls_url, PATTERN_SELFHOSTED_VIDEO).getMatch(0);
                                        if (videoID != null) {
                                            final DownloadLink video = this.createDownloadlink("https://v.redd.it/" + videoID);
                                            video.setProperty(RedditCom.PROPERTY_TYPE, RedditCom.PROPERTY_TYPE_video);
                                            video.setAvailable(true);
                                            thisCrawledLinks.add(video);
                                            addedRedditSelfhostedVideo = true;
                                        } else {
                                            /* Most likely directurl */
                                            final DownloadLink dl = this.createDownloadlink(hls_url);
                                            thisCrawledLinks.add(dl);
                                        }
                                    }
                                }
                            }
                        }
                        /* Stop once one media type has been found as the rest are usually mirrors. */
                        break;
                    }
                }
                /* Look for selfhosted photo content, Only add image if nothing else is found */
                if (thisCrawledLinks.size() == 0) {
                    postContainsRealMedia = false;
                    final List<Map<String, Object>> images = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(data, "preview/images");
                    if (images != null) {
                        /* Images slash selfhosted preview images */
                        logger.info(String.format("Found %d selfhosted images", images.size()));
                        int imageNumber = 1;
                        for (final Map<String, Object> imageInfo : images) {
                            String bestImageURL = (String) JavaScriptEngineFactory.walkJson(imageInfo, "source/url");
                            if (StringUtils.isEmpty(bestImageURL)) {
                                /* Skip invalid items */
                                continue;
                            }
                            /* Fix encoding */
                            bestImageURL = bestImageURL.replace("&amp;", "&");
                            final String serverFilename = Plugin.getFileNameFromURL(new URL(bestImageURL));
                            final String serverFilenameWithoutExt;
                            if (serverFilename.contains(".")) {
                                serverFilenameWithoutExt = serverFilename.substring(0, serverFilename.lastIndexOf("."));
                            } else {
                                serverFilenameWithoutExt = serverFilename;
                            }
                            final DownloadLink image = this.createDownloadlink("directhttp://" + bestImageURL);
                            image.setProperty(RedditCom.PROPERTY_TYPE, RedditCom.PROPERTY_TYPE_image);
                            image.setProperty(RedditCom.PROPERTY_SERVER_FILENAME_WITHOUT_EXT, serverFilenameWithoutExt);
                            image.setProperty(RedditCom.PROPERTY_INDEX, imageNumber);
                            image.setProperty(RedditCom.PROPERTY_INDEX_MAX, images.size());
                            image.setAvailable(true);
                            thisCrawledLinks.add(image);
                            imageNumber++;
                        }
                    } else {
                        logger.info("Failed to find selfhosted image(s)");
                    }
                }
                /* If this != null the post was removed. Still we might be able to find an external image URL sometimes (field "url"). */
                final String removed_by_category = (String) data.get("removed_by_category");
                if (!StringUtils.isEmpty(postText) && removed_by_category == null) {
                    /* Look for URLs inside post text. Field 'selftext' is always present but empty when not used. */
                    final String[] urls = HTMLParser.getHttpLinks(postText, null);
                    if (cfg.isCrawlUrlsInsidePostText()) {
                        if (!StringUtils.isEmpty(postText)) {
                            if (urls.length > 0) {
                                logger.info(String.format("Found %d URLs in selftext", urls.length));
                                for (final String url : urls) {
                                    final DownloadLink dl = this.createDownloadlink(url);
                                    thisCrawledExternalLinks.add(dl);
                                }
                            } else {
                                logger.info("Failed to find any URLs in selftext");
                            }
                        }
                    } else {
                        skippedItems += urls.length;
                    }
                    final TextCrawlerMode mode = cfg.getCrawlerTextDownloadMode();
                    if (mode == TextCrawlerMode.ALWAYS || (mode == TextCrawlerMode.ONLY_IF_NO_MEDIA_AVAILABLE && !postContainsRealMedia)) {
                        final DownloadLink text = this.createDownloadlink("reddidtext://" + postID);
                        text.setProperty(RedditCom.PROPERTY_TYPE, "text");
                        try {
                            text.setDownloadSize(postText.getBytes("UTF-8").length);
                        } catch (final UnsupportedEncodingException ignore) {
                            ignore.printStackTrace();
                        }
                        text.setAvailable(true);
                        thisCrawledLinks.add(text);
                    }
                }
                if ((thisCrawledLinks.isEmpty() && thisCrawledExternalLinks.isEmpty()) && skippedItems == 0) {
                    if (removed_by_category != null) {
                        final String subredditURL = "https://" + this.getHost() + permalink;
                        final DownloadLink dummy = this.createOfflinelink(subredditURL, "REMOVED_BY_" + removed_by_category + "_" + postID, "This post has been removed by " + removed_by_category + ".");
                        thisCrawledLinks.add(dummy);
                    } else {
                        logger.warning("Post is offline or contains unsupported content: " + postID);
                    }
                }
            } finally {
                final FilenameScheme scheme = cfg.getPreferredFilenameScheme();
                final String customFilenameScheme = cfg.getCustomFilenameScheme();
                final String chosenFilenameScheme;
                if (scheme == FilenameScheme.CUSTOM && !StringUtils.isEmpty(customFilenameScheme)) {
                    chosenFilenameScheme = customFilenameScheme;
                } else if (scheme == FilenameScheme.DATE_SUBREDDIT_POSTID_SERVER_FILENAME) {
                    chosenFilenameScheme = "*date*_*subreddit_title*_*post_id*_*original_filename_without_ext**ext*";
                } else if (scheme == FilenameScheme.DATE_SUBREDDIT_POSTID_TITLE) {
                    chosenFilenameScheme = "*date*_*subreddit_title*_*post_id*_*post_title**ext*";
                } else if (scheme == FilenameScheme.SERVER_FILENAME) {
                    chosenFilenameScheme = "*original_filename_without_ext**ext*";
                } else {
                    /* FilenameScheme.DATE_SUBREDDIT_POSTID_SLUG and fallback */
                    chosenFilenameScheme = "*date*_*subreddit_title*_*post_id*_*post_slug**ext*";
                }
                String filenameBase = chosenFilenameScheme.replace("*date*", dateFormatted);
                filenameBase = filenameBase.replace("*date_timestamp*", Long.toString(createdDateTimestampMillis));
                filenameBase = filenameBase.replace("*date_timedelta_formatted*", createdTimedeltaString);
                filenameBase = filenameBase.replace("*subreddit_title*", subredditTitle);
                filenameBase = filenameBase.replace("*username*", author);
                filenameBase = filenameBase.replace("*post_id*", postID);
                filenameBase = filenameBase.replace("*post_slug*", urlSlug);
                filenameBase = filenameBase.replace("*post_title*", title);
                for (final DownloadLink thisCrawledLink : thisCrawledLinks) {
                    thisCrawledLink._setFilePackage(fp);
                    /* Set properties for Packagizer usage */
                    thisCrawledLink.setProperty(RedditCom.PROPERTY_TITLE, title);
                    thisCrawledLink.setProperty(RedditCom.PROPERTY_USERNAME, author);
                    thisCrawledLink.setProperty(RedditCom.PROPERTY_DATE, dateFormatted);
                    thisCrawledLink.setProperty(RedditCom.PROPERTY_DATE_TIMESTAMP, createdDateTimestampMillis);
                    thisCrawledLink.setProperty(RedditCom.PROPERTY_DATE_TIMEDELTA_FORMATTED, createdTimedeltaString);
                    thisCrawledLink.setProperty(RedditCom.PROPERTY_SUBREDDIT, subredditTitle);
                    thisCrawledLink.setProperty(RedditCom.PROPERTY_POST_ID, postID);
                    if (urlSlug != null) {
                        thisCrawledLink.setProperty(RedditCom.PROPERTY_SLUG, urlSlug);
                    }
                    if (!StringUtils.isEmpty(postText)) {
                        thisCrawledLink.setProperty(RedditCom.PROPERTY_POST_TEXT, postText);
                    }
                    if (thisCrawledLink.getAvailableStatus() == AvailableStatus.TRUE) {
                        /* Set filename for items that are online */
                        final String extensionWithDot;
                        final String type = thisCrawledLink.getStringProperty(RedditCom.PROPERTY_TYPE);
                        if (StringUtils.equalsIgnoreCase(type, RedditCom.PROPERTY_TYPE_text)) {
                            extensionWithDot = ".txt";
                        } else if (StringUtils.equalsIgnoreCase(type, RedditCom.PROPERTY_TYPE_video)) {
                            extensionWithDot = ".mp4";
                        } else {
                            /* Obtain extension from URL. */
                            extensionWithDot = Plugin.getFileNameExtensionFromString(thisCrawledLink.getPluginPatternMatcher());
                        }
                        String serverFilenameWithoutExt = thisCrawledLink.getStringProperty(RedditCom.PROPERTY_SERVER_FILENAME_WITHOUT_EXT);
                        if (serverFilenameWithoutExt == null) {
                            serverFilenameWithoutExt = "";
                        }
                        final int padLength = thisCrawledLink.getIntegerProperty(RedditCom.PROPERTY_INDEX_MAX, 1);
                        final int itemPosition = thisCrawledLink.getIntegerProperty(RedditCom.PROPERTY_INDEX);
                        final String indexStr = StringUtils.formatByPadLength(padLength, itemPosition);
                        final String filename;
                        if (padLength == 1 || chosenFilenameScheme.contains("*index*")) {
                            /* Use users' desired naming scheme */
                            filename = filenameBase.replace("*original_filename_without_ext*", serverFilenameWithoutExt).replace("*index*", indexStr).replace("*ext*", extensionWithDot);
                        } else {
                            /*
                             * We know that this item is part of e.g. a gallery so let's add the position number as part of the file
                             * -extension to make sure we'll get uniqe filenames
                             */
                            filename = filenameBase.replace("*original_filename_without_ext*", serverFilenameWithoutExt).replace("*ext*", "_" + indexStr + extensionWithDot);
                        }
                        thisCrawledLink.setFinalFileName(filename);
                        thisCrawledLink.setProperty(RedditCom.PROPERTY_CRAWLER_FILENAME, filename);
                    }
                    this.distribute(thisCrawledLink);
                    crawledItems.add(thisCrawledLink);
                }
                /* Do not set reddit specific properties on crawled external URLs. */
                for (final DownloadLink thisCrawledExternalLink : thisCrawledExternalLinks) {
                    thisCrawledExternalLink._setFilePackage(fp);
                    this.distribute(thisCrawledExternalLink);
                    crawledItems.add(thisCrawledExternalLink);
                }
            }
        }
        if (skippedItems > 0) {
            logger.info("Items skipped due to users' plugin settings: " + skippedItems);
        }
        return crawledItems;
    }

    public static final String getApiBaseOauth() {
        return RedditCom.getApiBaseOauth();
    }
}
