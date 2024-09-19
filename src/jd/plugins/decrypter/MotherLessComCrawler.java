//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.MotherlessComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.MotherLessCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MotherLessComCrawler extends PluginForDecrypt {
    public MotherLessComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "motherless.com" });
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
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/";
            regex += "(g/[\\w\\-%]+/[A-Fa-f0-9]+";
            regex += "|g/[\\w\\-%]+";
            regex += "|g(i|v)/[a-z0-9\\-_]+";
            regex += "|G[A-Fa-f0-9]+/[A-Fa-f0-9]+";
            regex += "|G[A-Fa-f0-9]+";
            regex += "|GI[A-Fa-f0-9]+";
            regex += "|GV[A-Fa-f0-9]+";
            regex += "|u/[^/]+(\\?t=[aiv])?";
            regex += "|f/[^/]+/(?:images|galleries|videos)";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
    }

    // DEV NOTES:
    //
    // - supports spanning pages and galleries with submenu (MORE links).
    // - set same User-Agent from hoster plugin, making it harder to distinguish.
    // - Server issues can return many 503's in high load situations.
    // - Server also punishes user who downloads with too many connections. This is a linkchecking issue also, as grabs info from headers.
    private final String TYPE_USER                               = "https?://[^/]+/u/.*";
    private final String TYPE_GALLERY_IMAGE_AND_VIDEO            = "(?i)https?://[^/]+/(G([A-Fa-f0-9]+))";
    private final String TYPE_GALLERY_IMAGE                      = "https?://[^/]+/GI[A-Fa-f0-9]+$";
    private final String TYPE_GALLERY_VIDEO                      = "https?://[^/]+/GV[A-Fa-f0-9]+$";
    private final String TYPE_GROUP_CATEGORIES_OVERVIEW          = "https?://[^/]+/g/([\\w\\-%]+)$";
    private final String TYPE_GROUP_CATEGORY_IMAGE               = "https?://[^/]+/gi/([\\w\\-%]+)$";
    private final String TYPE_GROUP_CATEGORY_VIDEO               = "https?://[^/]+/gv/([\\w\\-%]+)$";
    private final String TYPE_SINGLE_ITEM_IN_CONTEXT_OF_GALLERY1 = "https?://[^/]+/g/([^/]+)/([A-Fa-f0-9]+)";
    private final String TYPE_SINGLE_ITEM_IN_CONTEXT_OF_GALLERY2 = "https?://[^/]+/(G[A-F0-9]+)/([A-Fa-f0-9]+)$";
    private final String TYPE_FAVOURITES_ALL                     = "https?://[^/]+/f/([^/]+)/(images|galleries|videos)";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_SINGLE_ITEM_IN_CONTEXT_OF_GALLERY1)) {
            /* Single image/video in context of gallery --> Pass to host plugin */
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            final String contentID = new Regex(param.getCryptedUrl(), TYPE_SINGLE_ITEM_IN_CONTEXT_OF_GALLERY1).getMatch(1);
            ret.add(this.createDownloadlink("https://" + this.getHost() + "/" + contentID));
            return ret;
        } else if (param.getCryptedUrl().matches(TYPE_SINGLE_ITEM_IN_CONTEXT_OF_GALLERY2)) {
            /* Single image/video in context of gallery --> Pass to host plugin */
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            final String contentID = new Regex(param.getCryptedUrl(), TYPE_SINGLE_ITEM_IN_CONTEXT_OF_GALLERY2).getMatch(1);
            ret.add(this.createDownloadlink("https://" + this.getHost() + "/" + contentID));
            return ret;
        }
        br.setLoadLimit(br.getLoadLimit() * 5);
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 503 });
        if (param.getCryptedUrl().matches(TYPE_FAVOURITES_ALL)) {
            return crawlGallery(param);
        } else if (param.getCryptedUrl().matches(TYPE_USER)) {
            return crawlUser(param);
        } else if (param.getCryptedUrl().matches(TYPE_GALLERY_IMAGE_AND_VIDEO)) {
            return crawlSubGalleries(param);
        } else if (param.getCryptedUrl().matches(TYPE_GALLERY_IMAGE) || param.getCryptedUrl().matches(TYPE_GALLERY_VIDEO)) {
            return crawlGallery(param);
        } else if (param.getCryptedUrl().matches(TYPE_GROUP_CATEGORIES_OVERVIEW) || param.getCryptedUrl().matches(TYPE_GROUP_CATEGORY_IMAGE) || param.getCryptedUrl().matches(TYPE_GROUP_CATEGORY_VIDEO)) {
            return this.crawlGroups(param);
        } else {
            /* Unsupported URL --> Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param) throws IOException, PluginException {
        return this.crawlGallery(param);
    }

    private ArrayList<DownloadLink> crawlSubGalleries(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String subgalID = new Regex(param.getCryptedUrl(), TYPE_GALLERY_IMAGE_AND_VIDEO).getMatch(1);
        if (subgalID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(param.getCryptedUrl());
        if (MotherLessCom.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] subGalURLs = br.getRegex("(/G(I|V)" + subgalID + ")").getColumn(0);
        /* Remove duplicates */
        final ArrayList<String> subGalsDeduped = new ArrayList<String>();
        for (final String subGalURL : subGalURLs) {
            if (!subGalsDeduped.contains(subGalURL)) {
                subGalsDeduped.add(subGalURL);
            }
        }
        int counter = 1;
        for (final String subGalURL : subGalsDeduped) {
            logger.info("Crawling subGallery " + counter + "/" + subGalURLs.length + " | URL: " + subGalURL);
            br.getPage(subGalURL);
            ret.addAll(crawlGallery(param));
            if (this.isAbort()) {
                break;
            }
            counter++;
        }
        return ret;
    }

    // finds the uid within the grouping
    private String formLink(String singlelink) {
        if (singlelink.startsWith("/")) {
            singlelink = "https://" + this.getHost() + singlelink;
        }
        final String contentID = new Regex(singlelink, "https?://[^/]+/[A-Z0-9]+/([A-Z0-9]+)").getMatch(0);
        if (contentID != null) {
            singlelink = "https://" + this.getHost() + "/" + contentID;
        }
        return singlelink;
    }

    private ArrayList<DownloadLink> crawlGallery(final CryptedLink param) throws IOException, PluginException {
        return crawlGallery(param, -1);
    }

    /**
     * Supports all kinds of multiple page urls that motherless.com has including pagination.
     *
     * @throws PluginException
     */
    private ArrayList<DownloadLink> crawlGallery(final CryptedLink param, final int maxItemsLimit) throws IOException, PluginException {
        br.getPage(param.getCryptedUrl());
        if (MotherLessCom.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String relative_path = null;
        final HashSet<String> dupes = new HashSet<String>();
        FilePackage fp = null;
        String fpName = null;
        if (br.getURL().matches(TYPE_FAVOURITES_ALL)) {
            final String username = new Regex(param.getCryptedUrl(), TYPE_FAVOURITES_ALL).getMatch(0);
            /* images or videos */
            final String media_type = new Regex(param.getCryptedUrl(), TYPE_FAVOURITES_ALL).getMatch(1);
            fpName = "Favorites of " + username + " - " + media_type;
            relative_path = username + "/" + media_type;
        } else {
            /* Try to fallback to url */
            fpName = br._getURL().getPath();
        }
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
        }
        /* TODO: 2022-07-07: Check if this is still needed */
        // String GM = br.getRegex("<a href=\"(/GM\\w+)\"").getMatch(0); // Gallery Mixed
        // if (GM != null) {
        // br.getPage(GM);
        // }
        /**
         * Find number of pages to walk through. Website displays max 6 pages so for galleries containing more than 6 pages this value will
         * be updated after each loop! </br>
         * Example with a lot of pages: https://motherless.com/GIAEE5076
         */
        int maxPage = getMaxPage(br);
        final HashSet<String> pages = new HashSet<String>();
        logger.info("Found " + maxPage + " page(s) [!page number may change later!], crawling now...");
        int page = 1;
        while (true) {
            int numberofResultsThisPage = 0;
            /* Find all galleries of [user | user favorites, and so on] */
            /* E.g. /f/username/galleries */
            final String[] galleries = br.getRegex("data-gallery-codename=\"([A-Z0-9]+)").getColumn(0);
            if (galleries != null && galleries.length > 0) {
                for (final String galleryID : galleries) {
                    ret.add(this.createDownloadlink("https://" + this.getHost() + "/G" + galleryID));
                    numberofResultsThisPage += 1;
                }
            }
            final String[] videolinks = br.getRegex("<a href=\"(?:https?://[^/]+)?(/[^\"]+)\" class=\"img-container\" target=\"_self\">\\s*<span class=\"currently-playing-icon\"").getColumn(0);
            if (videolinks != null && videolinks.length != 0) {
                for (String singlelink : videolinks) {
                    final String contentID = new Regex(singlelink, "/([A-Z0-9]+$)").getMatch(0);
                    if (!dupes.add(contentID)) {
                        continue;
                    } else if (contentID != null) {
                        singlelink = "https://" + this.getHost() + "/" + contentID;
                    }
                    singlelink = formLink(singlelink);
                    final DownloadLink dl = createDownloadlink(singlelink);
                    dl.setContentUrl(singlelink);
                    dl.setProperty(MotherLessCom.PROPERTY_TYPE, "video");
                    dl.setAvailable(true);
                    if (relative_path != null) {
                        dl.setRelativeDownloadFolderPath(relative_path);
                    }
                    final String title = regexMediaTitle(br, contentID);
                    if (title != null) {
                        dl.setProperty(MotherLessCom.PROPERTY_TITLE, Encoding.htmlDecode(title).trim());
                    }
                    MotherLessCom.setFilename(dl);
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                    distribute(dl);
                    ret.add(dl);
                    numberofResultsThisPage += 1;
                }
            }
            final String[] picturelinks = br.getRegex("<a href=\"(?:https?://[^/]+)?(?:/g/[^/]+)?(/[a-zA-Z0-9]+){1,2}\"[^>]*class=\"img-container\"").getColumn(0);
            if (picturelinks != null && picturelinks.length > 0) {
                for (String singlelink : picturelinks) {
                    final String contentID = new Regex(singlelink, "/([A-Z0-9]+$)").getMatch(0);
                    if (!dupes.add(contentID)) {
                        /* Already added as video content --> Skip */
                        continue;
                    }
                    singlelink = formLink(singlelink);
                    final DownloadLink dl = createDownloadlink(singlelink);
                    dl.setContentUrl(singlelink);
                    dl.setProperty(MotherLessCom.PROPERTY_TYPE, "image");
                    dl.setAvailable(true);
                    if (relative_path != null) {
                        dl.setRelativeDownloadFolderPath(relative_path);
                    }
                    final String title = regexMediaTitle(br, contentID);
                    if (title != null) {
                        dl.setProperty(MotherLessCom.PROPERTY_TITLE, Encoding.htmlDecode(title).trim());
                    }
                    MotherLessCom.setFilename(dl);
                    if (fp != null) {
                        dl._setFilePackage(fp);
                    }
                    distribute(dl);
                    ret.add(dl);
                    numberofResultsThisPage += 1;
                }
            }
            logger.info("Crawled page " + page + "/" + maxPage + " | Results on this page: " + numberofResultsThisPage + " | Results so far: " + ret.size() + " | Limit: " + maxItemsLimit);
            String nextPageURL = br.getRegex("(?i)<a href=\"(/[^<>\"]+\\?page=\\d+)\"[^>]+>NEXT").getMatch(0);
            if (nextPageURL == null) {
                nextPageURL = br.getRegex("<a href=\"(/[^<>\"]+\\?page=\\d+)\"[^>]+rel\\s*=\\s*\"next\"").getMatch(0);
            }
            if (nextPageURL == null) {
                /* 2022-05-30 */
                nextPageURL = br.getRegex("link rel=\"next\" href=\"([^\"]+)\"").getMatch(0);
            }
            if (page == maxPage) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (numberofResultsThisPage == 0) {
                logger.info("Stopping because: Failed to find any results on current page");
                break;
            } else if (nextPageURL == null) {
                logger.info("Stopping because: Failed to find nextPage");
                break;
            } else if (!pages.add(nextPageURL)) {
                /* Fail-safe */
                logger.info("Stopping because: Already crawled current nextPage");
                break;
            } else if (maxItemsLimit != -1 && ret.size() >= maxItemsLimit) {
                logger.info("Stopping because: Reached max items limit of " + maxItemsLimit);
                this.displayBubbleNotification(fpName, "Stopping because: Reached user defined limit of: " + maxItemsLimit + "\r\nYou can adjust this limit in the motherless.com plugin settings.");
                break;
            } else if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else {
                /* Double-check that we're accessing the expected page */
                final UrlQuery nextPageQuery = UrlQuery.parse(nextPageURL);
                final String nextPageNumberStr = nextPageQuery.get("page");
                if (nextPageNumberStr == null || Integer.parseInt(nextPageNumberStr) != (page + 1)) {
                    /* This should never happen */
                    logger.warning("Stopping because: nextPageNumberStr does not match expected value: Got: " + nextPageNumberStr + " | Expected: " + (page + 1));
                    break;
                }
                br.getPage(nextPageURL);
                final int newMaxPage = getMaxPage(br);
                if (newMaxPage != maxPage && newMaxPage > maxPage) {
                    logger.info("maxPage changed: Old: " + maxPage + " | New: " + newMaxPage);
                    maxPage = newMaxPage;
                }
                page++;
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlGroups(final CryptedLink param) throws IOException, PluginException {
        final int groupCrawlerMaxLimit = PluginJsonConfig.get(MotherlessComConfig.class).getGroupCrawlerLimit();
        if (groupCrawlerMaxLimit == 0) {
            logger.info("Returning empty array because user set limit of group crawler to 0 and thus disabled it");
            return new ArrayList<DownloadLink>();
        }
        if (param.getCryptedUrl().matches(TYPE_GROUP_CATEGORIES_OVERVIEW)) {
            return crawlGroupCategoriesOverview(param);
        } else {
            return crawlGroupImagesAndVideos(param);
        }
    }

    /** Crawls category-URLs of a group e.g. user adds /g/bla and this returns /gv/bla and /gi/bla */
    private ArrayList<DownloadLink> crawlGroupCategoriesOverview(final CryptedLink param) throws IOException, PluginException {
        final String groupSlug = new Regex(param.getCryptedUrl(), TYPE_GROUP_CATEGORIES_OVERVIEW).getMatch(0);
        if (groupSlug == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(param.getCryptedUrl());
        if (MotherLessCom.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] urls = br.getRegex("(/g(i|v)/" + groupSlug + ")").getColumn(0);
        for (String url : urls) {
            url = br.getURL(url).toString();
            ret.add(this.createDownloadlink(url));
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlGroupImagesAndVideos(final CryptedLink param) throws IOException, PluginException {
        return this.crawlGallery(param, PluginJsonConfig.get(MotherlessComConfig.class).getGroupCrawlerLimit());
    }

    /**
     * Returns max page number for pagination according to current html code. </br>
     * This can vary e.g. on first page it looks like last page is number 6 but once we are on page 4 the highest page number visible
     * changes to 8.
     */
    private int getMaxPage(final Browser br) throws MalformedURLException {
        final String[] pageURLs = br.getRegex("<a href=\"([^\"]+page=\\d+[^\"]*)\"").getColumn(0);
        int maxPage = 1;
        for (final String pageURL : pageURLs) {
            final UrlQuery query = UrlQuery.parse(pageURL);
            final int page = Integer.parseInt(query.get("page"));
            if (page > maxPage) {
                maxPage = page;
            }
        }
        return maxPage;
    }

    private static String regexMediaTitle(final Browser br, final String contentID) {
        return br.getRegex("<a href=\"[^\"]+[^/]+/" + contentID + "\" title=\"([^\"]+)\"").getMatch(0);
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-21: Avoid overloading the website */
        return 1;
    }
}