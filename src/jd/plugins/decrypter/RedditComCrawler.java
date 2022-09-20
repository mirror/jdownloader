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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.RedditCom;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "reddit.com" }, urls = { "https?://(?:(?:www|old)\\.)?reddit\\.com/(?:r/[^/]+(?:/comments/[a-z0-9]+(/[A-Za-z0-9\\-_]+/?)?)?|gallery/[a-z0-9]+|user/[^/]+(?:/saved)?)" })
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

    private static final String TYPE_SUBREDDIT          = "(?:https?://[^/]+)?/r/([^/]+)$";
    private static final String TYPE_SUBREDDIT_COMMENTS = "(?:https?://[^/]+)?/r/([^/]+)/comments/([a-z0-9]+)(/([^/\\?]+)/?)?";
    private static final String TYPE_GALLERY            = "(?:https?://[^/]+)?/gallery/([a-z0-9]+)";
    private static final String TYPE_USER               = "(?:https?://[^/]+)?/user/([^/]+)";
    private static final String TYPE_USER_SAVED_OBJECTS = "(?:https?://[^/]+)?/user/([^/]+)/saved";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        RedditCom.prepBRAPI(this.br);
        if (param.getCryptedUrl().matches(TYPE_USER_SAVED_OBJECTS)) {
            return crawlUserSavedObjects(param);
        } else if (param.getCryptedUrl().matches(TYPE_USER)) {
            return crawlUser(param);
        } else if (param.getCryptedUrl().matches(TYPE_SUBREDDIT_COMMENTS)) {
            return crawlCommentURL(param);
        } else if (param.getCryptedUrl().matches(TYPE_GALLERY)) {
            return this.crawlGalleryURL(param);
        } else {
            return crawlSubreddit(param);
        }
    }

    private ArrayList<DownloadLink> crawlSubreddit(final CryptedLink param) throws Exception {
        /* Prepare crawl process */
        final String subredditTitle = new Regex(param.getCryptedUrl(), TYPE_SUBREDDIT).getMatch(0);
        if (subredditTitle == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        final String url = "https://www." + this.getHost() + "/r/" + subredditTitle + "/.json";
        fp.setName("/r/" + subredditTitle);
        if (PluginJsonConfig.get(RedditConfig.class).isCrawlCompleteSubreddits()) {
            /* Crawl until we've reached the end. */
            return this.crawlPagination(url, fp, -1);
        } else {
            /* Crawl only first page */
            return this.crawlPagination(url, fp, 1);
        }
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param) throws Exception {
        /* Prepare crawl process */
        final String userTitle = new Regex(param.getCryptedUrl(), TYPE_USER).getMatch(0);
        if (userTitle == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("/u/" + userTitle);
        final String url = "https://www." + this.getHost() + "/user/" + userTitle + "/.json";
        if (PluginJsonConfig.get(RedditConfig.class).isCrawlCompleteUserProfiles()) {
            /* Crawl until we've reached the end. */
            return this.crawlPagination(url, fp, -1);
        } else {
            /* Crawl only first page */
            return this.crawlPagination(url, fp, 1);
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
                logger.info("Stopping because: Reached desired max. page: " + maxPage);
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
        final String commentID = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(0);
        return crawlComments(commentID);
    }

    /** According to: https://www.reddit.com/r/redditdev/comments/b8yd3r/reddit_api_possible_to_get_posts_by_id/ */
    private ArrayList<DownloadLink> crawlCommentURL(final CryptedLink param) throws Exception {
        final String commentID = new Regex(param.getCryptedUrl(), TYPE_SUBREDDIT_COMMENTS).getMatch(1);
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

    private static final String TYPE_CRAWLED_SELFHOSTED_VIDEO = "https?://v\\.redd\\.it/[a-z0-9]+.*";
    private static final String TYPE_CRAWLED_SELFHOSTED_IMAGE = "https?://i\\.redd\\.it/[a-z0-9]+.*";

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
            final String urlSlug = new Regex(permalink, TYPE_SUBREDDIT_COMMENTS).getMatch(3);
            if (urlSlug == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final ArrayList<DownloadLink> thisCrawledLinks = new ArrayList<DownloadLink>();
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
                String filenameBaseForMultiItems = chosenFilenameScheme.replace("*date*", dateFormatted);
                filenameBaseForMultiItems = filenameBaseForMultiItems.replace("*date_timestamp*", Long.toString(createdDateTimestampMillis));
                filenameBaseForMultiItems = filenameBaseForMultiItems.replace("*date_timedelta_formatted*", createdTimedeltaString);
                filenameBaseForMultiItems = filenameBaseForMultiItems.replace("*subreddit_title*", subredditTitle);
                filenameBaseForMultiItems = filenameBaseForMultiItems.replace("*username*", author);
                filenameBaseForMultiItems = filenameBaseForMultiItems.replace("*post_id*", postID);
                filenameBaseForMultiItems = filenameBaseForMultiItems.replace("*post_slug*", urlSlug);
                filenameBaseForMultiItems = filenameBaseForMultiItems.replace("*post_title*", title);
                /* For items without index */
                final String filenameBaseForSingleItems = filenameBaseForMultiItems.replace("*index*", "");
                /* Look for single URLs e.g. single pictures (e.g. often imgur.com URLs, can also be selfhosted content) */
                boolean addedRedditSelfhostedVideo = false;
                String externalURL = (String) data.get("url");
                DownloadLink lastAddedMediaItem = null;
                if (!StringUtils.isEmpty(externalURL) && !this.canHandle(externalURL)) {
                    if (Encoding.isHtmlEntityCoded(externalURL)) {
                        externalURL = Encoding.htmlDecode(externalURL);
                    }
                    logger.info("Found external URL: " + externalURL);
                    final String serverFilename = Plugin.getFileNameFromURL(new URL(externalURL));
                    final String serverFilenameWithoutExt;
                    String ext = null;
                    if (serverFilename.contains(".")) {
                        ext = Plugin.getFileNameExtensionFromURL(externalURL);
                        serverFilenameWithoutExt = serverFilename.substring(0, serverFilename.lastIndexOf("."));
                    } else {
                        serverFilenameWithoutExt = serverFilename;
                    }
                    final DownloadLink dl = this.createDownloadlink(externalURL);
                    if (externalURL.matches(TYPE_CRAWLED_SELFHOSTED_VIDEO)) {
                        if (ext == null) {
                            /* Fallback */
                            ext = ".mp4";
                        }
                        addedRedditSelfhostedVideo = true;
                        dl.setFinalFileName(filenameBaseForSingleItems.replace("*original_filename_without_ext*", serverFilenameWithoutExt).replace("*ext*", ext));
                        /* Skip availablecheck as we know that this content is online and is a directurl. */
                        dl.setAvailable(true);
                        lastAddedMediaItem = dl;
                    } else if (externalURL.matches(TYPE_CRAWLED_SELFHOSTED_IMAGE)) {
                        if (ext == null) {
                            /* Fallback */
                            ext = ".jpg";
                        }
                        dl.setFinalFileName(filenameBaseForSingleItems.replace("*original_filename_without_ext*", serverFilenameWithoutExt).replace("*ext*", ext));
                        /* Skip availablecheck as we know that this content is online and is a directurl. */
                        dl.setAvailable(true);
                        lastAddedMediaItem = dl;
                    }
                    thisCrawledLinks.add(dl);
                }
                /* 2022-03-10: When a gallery is removed, 'is_gallery' can be true while 'gallery_data' does not exist. */
                final Object is_galleryO = data.get("is_gallery");
                final Object galleryO = data.get("gallery_data");
                if (is_galleryO == Boolean.TRUE && galleryO != null) {
                    final Map<String, Object> gallery_data = (Map<String, Object>) galleryO;
                    final List<Map<String, Object>> galleryItems = (List<Map<String, Object>>) gallery_data.get("items");
                    final Map<String, Object> media_metadata = (Map<String, Object>) data.get("media_metadata");
                    int imageNumber = 1;
                    final int padLength = StringUtils.getPadLength(galleryItems.size());
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
                        final String extensionWithDot = "." + extension;
                        final String serverFilenameWithoutExt = mediaID;
                        final DownloadLink image = this.createDownloadlink("https://i.redd.it/" + serverFilenameWithoutExt + "." + extension);
                        final String indexStr = StringUtils.formatByPadLength(padLength, imageNumber);
                        if (galleryItems.size() == 1 || chosenFilenameScheme.contains("*index*")) {
                            image.setFinalFileName(filenameBaseForMultiItems.replace("*original_filename_without_ext*", serverFilenameWithoutExt).replace("*index*", indexStr).replace("*ext*", extensionWithDot));
                        } else {
                            /* Special: Add number of image as part of filename extension. */
                            image.setFinalFileName(filenameBaseForMultiItems.replace("*original_filename_without_ext*", serverFilenameWithoutExt).replace("*ext*", "_" + indexStr + extensionWithDot));
                        }
                        image.setAvailable(true);
                        image.setProperty(RedditCom.PROPERTY_INDEX, imageNumber);
                        if (!StringUtils.isEmpty(caption)) {
                            image.setComment(caption);
                        }
                        thisCrawledLinks.add(image);
                        lastAddedMediaItem = image;
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
                        final String[] links = HTMLParser.getHttpLinks(mediaEmbedStr, this.br.getURL());
                        for (final String url : links) {
                            final DownloadLink dl = this.createDownloadlink(url);
                            thisCrawledLinks.add(dl);
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
                                        final DownloadLink dl = this.createDownloadlink(hls_url);
                                        thisCrawledLinks.add(dl);
                                    }
                                }
                            }
                        }
                        /* Stop once one type has been found! */
                        break;
                    }
                }
                /* Look for selfhosted photo content, Only add image if nothing else is found */
                if (thisCrawledLinks.size() == 0) {
                    postContainsRealMedia = false;
                    final List<Map<String, Object>> images = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(data, "preview/images");
                    if (images != null) {
                        logger.info(String.format("Found %d selfhosted images", images.size()));
                        int imageNumber = 1;
                        final int padLength = StringUtils.getPadLength(images.size());
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
                            final String extensionWithDot = Plugin.getFileNameExtensionFromString(serverFilename);
                            final DownloadLink image = this.createDownloadlink("directhttp://" + bestImageURL);
                            final String indexStr = StringUtils.formatByPadLength(padLength, imageNumber);
                            if (images.size() == 1 || chosenFilenameScheme.contains("*index*")) {
                                image.setFinalFileName(filenameBaseForMultiItems.replace("*original_filename_without_ext*", serverFilenameWithoutExt).replace("*index*", indexStr).replace("*ext*", extensionWithDot));
                            } else {
                                /* Special: Add number of image as part of filename extension. */
                                image.setFinalFileName(filenameBaseForMultiItems.replace("*original_filename_without_ext*", serverFilenameWithoutExt).replace("*ext*", "_" + indexStr + extensionWithDot));
                            }
                            image.setAvailable(true);
                            thisCrawledLinks.add(image);
                            lastAddedMediaItem = image;
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
                                    thisCrawledLinks.add(dl);
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
                        final String filename;
                        if (lastAddedMediaItem != null) {
                            /* Use filename that matches other found media item. */
                            filename = lastAddedMediaItem.getName().substring(0, lastAddedMediaItem.getName().lastIndexOf(".")) + ".txt";
                        } else {
                            filename = fp.getName() + ".txt";
                        }
                        text.setFinalFileName(filename);
                        try {
                            text.setDownloadSize(postText.getBytes("UTF-8").length);
                        } catch (final UnsupportedEncodingException ignore) {
                            ignore.printStackTrace();
                        }
                        text.setProperty(RedditCom.PROPERTY_CRAWLER_FILENAME, filename);
                        text.setAvailable(true);
                        thisCrawledLinks.add(text);
                    }
                }
                if (thisCrawledLinks.isEmpty() && skippedItems == 0) {
                    if (removed_by_category != null) {
                        final String subredditURL = "https://" + this.getHost() + permalink;
                        final DownloadLink dummy = this.createOfflinelink(subredditURL, "REMOVED_BY_" + removed_by_category + "_" + postID, "This post has been removed by " + removed_by_category + ".");
                        thisCrawledLinks.add(dummy);
                    } else {
                        logger.warning("Post offline or contains unsupported content: " + postID);
                    }
                }
            } finally {
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
                    /* Not (yet) required */
                    // this.distribute(thisCrawledLink);
                    crawledItems.add(thisCrawledLink);
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
