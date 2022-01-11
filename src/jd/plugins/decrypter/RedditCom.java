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

import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.RedditConfig;
import org.jdownloader.plugins.components.config.RedditConfig.FilenameScheme;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "reddit.com" }, urls = { "https?://(?:(?:www|old)\\.)?reddit\\.com/(?:r/[^/]+(?:/comments/[a-z0-9]+/[A-Za-z0-9\\-_]+)?|gallery/[a-z0-9]+|user/[^/]+(?:/saved)?)" })
public class RedditCom extends PluginForDecrypt {
    public RedditCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-07-21: Let's be gentle and avoid doing too many API requests. */
        return 1;
    }

    @Override
    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink dl = super.createDownloadlink(url);
        if (fp != null) {
            dl._setFilePackage(fp);
        }
        distribute(dl);
        return dl;
    }

    /* TODO: Eliminate this public variable */
    private FilePackage         fp                      = null;
    private static final String TYPE_SUBREDDIT          = "https?://[^/]+/r/([^/]+)";
    private static final String TYPE_SUBREDDIT_COMMENTS = "https?://[^/]+/r/([^/]+)/comments/([a-z0-9]+)/([A-Za-z0-9\\-_]+)";
    private static final String TYPE_GALLERY            = "https?://[^/]+/gallery/([a-z0-9]+)";
    private static final String TYPE_USER               = "https://[^/]+/user/([^/]+)";
    private static final String TYPE_USER_SAVED_OBJECTS = "https://[^/]+/user/([^/]+)/saved";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        jd.plugins.hoster.RedditCom.prepBRAPI(this.br);
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
        final ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> lastItemDupes = new ArrayList<String>();
        /* Prepare crawl process */
        final String subredditTitle = new Regex(param.getCryptedUrl(), TYPE_SUBREDDIT).getMatch(0);
        if (subredditTitle == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp = FilePackage.getInstance();
        fp.setName("/r/" + subredditTitle);
        /* 2020-09-22: TODO Crawling entire user-profiles or subreddits will cause a lot of http requests - do not enable this yet! */
        final boolean stopAfterFirstPage = true;
        final int maxItemsPerCall = 100;
        final UrlQuery query = new UrlQuery();
        query.add("type", "links");
        query.add("limit", Integer.toString(maxItemsPerCall));
        int page = 0;
        do {
            page++;
            logger.info("Crawling page: " + page);
            br.getPage("https://www." + this.getHost() + "/r/" + subredditTitle + "/.json?" + query.toString());
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            crawledLinks.addAll(this.crawlListing(entries));
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
            } else if (stopAfterFirstPage) {
                logger.info("Stopping after first page");
                break;
            }
            lastItemDupes.add(fullnameAfter);
            query.remove("after");
            query.add("after", fullnameAfter);
        } while (!this.isAbort());
        return crawledLinks;
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> lastItemDupes = new ArrayList<String>();
        /* Prepare crawl process */
        final String userTitle = new Regex(param.getCryptedUrl(), TYPE_USER).getMatch(0);
        if (userTitle == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp = FilePackage.getInstance();
        fp.setName("/u/" + userTitle);
        /* 2020-09-22: TODO Crawling entire user-profiles or subreddits will cause a lot of http requests - do not enable this yet! */
        final boolean stopAfterFirstPage = true;
        final int maxItemsPerCall = 100;
        final UrlQuery query = new UrlQuery();
        query.add("type", "links");
        query.add("limit", Integer.toString(maxItemsPerCall));
        int page = 0;
        do {
            page++;
            logger.info("Crawling page: " + page);
            br.getPage("https://www." + this.getHost() + "/user/" + userTitle + "/.json?" + query.toString());
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            crawledLinks.addAll(crawlListing(entries));
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
            } else if (stopAfterFirstPage) {
                logger.info("Stopping after first page");
                break;
            }
            lastItemDupes.add(fullnameAfter);
            query.remove("after");
            query.add("after", fullnameAfter);
        } while (!this.isAbort());
        return crawledLinks;
    }

    private ArrayList<DownloadLink> crawlUserSavedObjects(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> crawledLinks = new ArrayList<DownloadLink>();
        /* Login required */
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc == null) {
            throw new AccountRequiredException();
        }
        /* Login */
        final PluginForHost plugin = JDUtilities.getPluginForHost(this.getHost());
        plugin.setBrowser(this.br);
        ((jd.plugins.hoster.RedditCom) plugin).login(acc, false);
        final ArrayList<String> lastItemDupes = new ArrayList<String>();
        /* Prepare crawl process */
        fp = FilePackage.getInstance();
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
            crawledLinks.addAll(this.crawlListing(entries));
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
        br.getPage("https://www.reddit.com/comments/" + commentID + "/.json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        /* [0] = post/"first comment" */
        /* [1] = Comments */
        final Map<String, Object> entries = (Map<String, Object>) ressourcelist.get(0);
        crawledLinks.addAll(this.crawlListing(entries));
        return crawledLinks;
    }

    private static final String TYPE_CRAWLED_SELFHOSTED_VIDEO = "https?://v\\.redd\\.it/[a-z0-9]+.*";
    private static final String TYPE_CRAWLED_SELFHOSTED_IMAGE = "https?://i\\.redd\\.it/[a-z0-9]+.*";

    private ArrayList<DownloadLink> crawlListing(final Map<String, Object> entries) throws Exception {
        /* https://www.reddit.com/dev/api/#fullnames */
        final ArrayList<DownloadLink> crawledItems = new ArrayList<DownloadLink>();
        final DecimalFormat df = new DecimalFormat("00");
        final ArrayList<Object> items = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "data/children");
        for (final Object itemO : items) {
            final Map<String, Object> post = (Map<String, Object>) itemO;
            final String kind = (String) post.get("kind");
            final Map<String, Object> data = (Map<String, Object>) post.get("data");
            final String author = (String) data.get("author");
            final long createdTimestamp = JavaScriptEngineFactory.toLong(data.get("created"), 0) * 1000;
            final Date theDate = new Date(createdTimestamp);
            final String dateFormatted = new SimpleDateFormat("yyy-MM-dd").format(theDate);
            final String title = (String) data.get("title");
            final String subredditTitle = (String) data.get("subreddit");
            if (!"t3".equalsIgnoreCase(kind)) {
                /*
                 * Skip everything except links (e.g. skips comments, awards and so on. See API docs --> In-text search for "type prefixes")
                 */
                continue;
            } else if (StringUtils.isEmpty(title) || StringUtils.isEmpty(subredditTitle)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final ArrayList<DownloadLink> thisCrawledLinks = new ArrayList<DownloadLink>();
            try {
                if (this.fp == null) {
                    fp = FilePackage.getInstance();
                    fp.setName(title);
                }
                final FilenameScheme scheme = PluginJsonConfig.get(RedditConfig.class).getPreferredFilenameScheme();
                String filenameBase = null;
                String filenameBeginning = null;
                if (scheme == FilenameScheme.DATE_SUBREDDIT_SERVER_FILENAME) {
                    filenameBase = null;
                    filenameBeginning = dateFormatted + "_" + subredditTitle + "_";
                } else if (scheme == FilenameScheme.DATE_SUBREDDIT_TITLE) {
                    filenameBase = dateFormatted + "_" + subredditTitle + " - " + title;
                } else if (scheme != null) {
                    logger.warning("Developer mistake! Unsupported FilenameScheme: " + scheme.name());
                }
                /* 2020-07-23: TODO: This field might indicate selfhosted content: is_reddit_media_domain */
                /* Look for single URLs e.g. single pictures (e.g. often imgur.com URLs, can also be selfhosted content) */
                boolean addedRedditSelfhostedVideo = false;
                String externalURL = (String) data.get("url");
                if (!StringUtils.isEmpty(externalURL) && !this.canHandle(externalURL)) {
                    if (Encoding.isHtmlEntityCoded(externalURL)) {
                        externalURL = Encoding.htmlDecode(externalURL);
                    }
                    logger.info("Found external URL: " + externalURL);
                    String serverFilename = Plugin.getFileNameFromURL(new URL(externalURL));
                    final DownloadLink dl = this.createDownloadlink(externalURL);
                    if (externalURL.matches(TYPE_CRAWLED_SELFHOSTED_VIDEO)) {
                        addedRedditSelfhostedVideo = true;
                        serverFilename = applyFilenameExtension(serverFilename, ".mp4");
                        if (filenameBase != null) {
                            dl.setFinalFileName(filenameBase + ".mp4");
                        } else if (serverFilename != null && filenameBeginning != null) {
                            dl.setName(filenameBeginning + serverFilename);
                        } else if (serverFilename != null) {
                            dl.setName(serverFilename);
                        }
                        /* Skip availablecheck as we know that this content is online and is a directurl. */
                        dl.setAvailable(true);
                    } else if (externalURL.matches(TYPE_CRAWLED_SELFHOSTED_IMAGE)) {
                        serverFilename = applyFilenameExtension(serverFilename, ".jpg");
                        if (filenameBase != null) {
                            dl.setFinalFileName(filenameBase + ".jpg");
                        } else if (serverFilename != null && filenameBeginning != null) {
                            dl.setName(filenameBeginning + serverFilename);
                        } else if (serverFilename != null) {
                            dl.setName(serverFilename);
                        }
                        /* Skip availablecheck as we know that this content is online and is a directurl. */
                        dl.setAvailable(true);
                    }
                    thisCrawledLinks.add(dl);
                }
                final Object is_galleryO = data.get("is_gallery");
                if (is_galleryO == Boolean.TRUE) {
                    final Map<String, Object> media_metadata = (Map<String, Object>) data.get("media_metadata");
                    final Iterator<Entry<String, Object>> iterator = media_metadata.entrySet().iterator();
                    int imageNumber = 0;
                    while (iterator.hasNext()) {
                        imageNumber += 1;
                        final Entry<String, Object> entry = iterator.next();
                        final Map<String, Object> mediaInfo = (Map<String, Object>) entry.getValue();
                        /* "image/png" --> "png" */
                        String mediaType = (String) mediaInfo.get("m");
                        String extension = getExtensionFromMimeType(mediaType);
                        if (extension == null && mediaType.contains("/")) {
                            final String[] mediaTypeSplit = mediaType.split("/");
                            extension = mediaTypeSplit[mediaTypeSplit.length - 1];
                        }
                        if (extension == null) {
                            // fallback
                            extension = "jpg";
                        }
                        final String media_id = (String) mediaInfo.get("id");
                        final String serverFilename = media_id + "." + extension;
                        final DownloadLink image = this.createDownloadlink("https://i.redd.it/" + serverFilename);
                        if (filenameBase != null) {
                            image.setFinalFileName(filenameBase + "_" + df.format(imageNumber) + "." + extension);
                        } else if (serverFilename != null && filenameBeginning != null) {
                            image.setName(filenameBeginning + df.format(imageNumber) + "_" + serverFilename);
                        } else if (serverFilename != null) {
                            image.setName(serverFilename);
                        }
                        image.setAvailable(true);
                        image.setProperty(jd.plugins.hoster.RedditCom.PROPERTY_INDEX, imageNumber);
                        thisCrawledLinks.add(image);
                    }
                    break;
                }
                /* Look for embedded content from external sources - the object is always given but can be empty */
                final Object embeddedMediaO = data.get("media_embed");
                if (embeddedMediaO != null) {
                    Map<String, Object> embeddedMediaInfo = (Map<String, Object>) embeddedMediaO;
                    if (!embeddedMediaInfo.isEmpty()) {
                        logger.info("Found media_embed");
                        String media_embedStr = (String) embeddedMediaInfo.get("content");
                        final String[] links = HTMLParser.getHttpLinks(media_embedStr, this.br.getURL());
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
                    final ArrayList<Object> imagesO = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(data, "preview/images");
                    if (imagesO != null) {
                        logger.info(String.format("Found %d selfhosted images", imagesO.size()));
                        for (final Object imageO : imagesO) {
                            final Map<String, Object> imageInfo = (Map<String, Object>) imageO;
                            final String bestImage = (String) JavaScriptEngineFactory.walkJson(imageInfo, "source/url");
                            if (bestImage != null) {
                                final DownloadLink dl = this.createDownloadlink("directhttp://" + bestImage);
                                dl.setAvailable(true);
                                thisCrawledLinks.add(dl);
                            }
                        }
                    } else {
                        logger.info("Failed to find selfhosted image(s)");
                    }
                }
                /* Look for URLs inside post text. selftext is always present but empty when not used. */
                final String postText = (String) data.get("selftext");
                if (!StringUtils.isEmpty(postText)) {
                    logger.info("Looking for URLs in selftext");
                    String[] urls = HTMLParser.getHttpLinks(postText, null);
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
            } finally {
                for (final DownloadLink thisCrawledLink : thisCrawledLinks) {
                    /* Set properties for Packagizer usage */
                    thisCrawledLink.setProperty(jd.plugins.hoster.RedditCom.PROPERTY_TITLE, title);
                    thisCrawledLink.setProperty(jd.plugins.hoster.RedditCom.PROPERTY_USERNAME, author);
                    thisCrawledLink.setProperty(jd.plugins.hoster.RedditCom.PROPERTY_DATE, dateFormatted);
                    thisCrawledLink.setProperty(jd.plugins.hoster.RedditCom.PROPERTY_SUBREDDIT, subredditTitle);
                    crawledItems.add(thisCrawledLink);
                }
            }
        }
        return crawledItems;
    }

    public static final String getApiBaseOauth() {
        return jd.plugins.hoster.RedditCom.getApiBaseOauth();
    }

    @Override
    public Class<? extends RedditConfig> getConfigInterface() {
        return RedditConfig.class;
    }
}
