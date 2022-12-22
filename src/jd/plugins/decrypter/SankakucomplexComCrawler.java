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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.SankakucomplexComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.SankakucomplexCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SankakucomplexComCrawler extends PluginForDecrypt {
    public SankakucomplexComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sankakucomplex.com" });
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
            String regex = "https?://(?:(beta|www|chan)\\.)?" + buildHostsPatternPart(domains) + "/(";
            regex += "[a-z]{2}/books/\\d+";
            regex += "|[a-z]{2}\\?tags=pool:\\d+";
            regex += "|[a-z0-9]{2}/books\\?tags=.+";
            regex += "|[a-z0-9]{2}\\?tags=.+";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    private final String       TYPE_BOOK       = "https?://[^/]+/([a-z]{2})/books/(\\d+)";
    private final String       TYPE_TAGS_BOOK  = "https?://[^/]+/([a-z]{2})\\?tags=pool:(\\d+)";
    private final String       TYPE_TAGS_BOOKS = "https?://[^/]+/([a-z0-9]{2})/books\\?tags=(.+)";
    private final String       TYPE_TAGS_POSTS = "https?://[^/]+/([a-z]{2})\\?tags=([^&]+)";
    public static final String API_BASE        = "https://capi-v2.sankakucomplex.com";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_TAGS_BOOKS)) {
            return crawlTagsBooks(param);
        } else if (param.getCryptedUrl().matches(TYPE_TAGS_BOOKS)) {
            return crawlBook(param);
        } else {
            return crawlTagsPosts(param);
        }
    }

    private ArrayList<DownloadLink> crawlTagsPosts(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final SankakucomplexComConfig cfg = PluginJsonConfig.get(SankakucomplexComConfig.class);
        final int maxPage = cfg.getPostTagCrawlerMaxPageLimit();
        if (maxPage == 0) {
            logger.info("Stopping because: User disabled posts tag crawler");
            return ret;
        }
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_TAGS_POSTS);
        final String languageFromURL = urlinfo.getMatch(0);
        String tags = urlinfo.getMatch(1);
        if (languageFromURL == null || tags == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        tags = URLEncode.decodeURIComponent(tags);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(tags);
        final String tagsUrlEncoded = Encoding.urlEncode(tags);
        final int maxItemsPerPage = 40;
        final UrlQuery query = new UrlQuery();
        query.add("lang", languageFromURL);
        query.add("limit", Integer.toString(maxItemsPerPage));
        query.add("hide_posts_in_books", "in-larger-tags");
        query.add("tags", tagsUrlEncoded);
        int page = 1;
        do {
            br.setFollowRedirects(true);
            br.getPage(API_BASE + "/posts/keyset?" + query.toString());
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> meta = (Map<String, Object>) entries.get("meta");
            final String nextPageHash = (String) meta.get("next");
            final List<Map<String, Object>> data = (List<Map<String, Object>>) entries.get("data");
            if (data.isEmpty()) {
                if (ret.isEmpty()) {
                    logger.info("Looks like users' given tag does not lead to any search results");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    logger.info("Stopping because: Current page is empty");
                    break;
                }
            }
            for (final Map<String, Object> post : data) {
                final DownloadLink link = this.createDownloadlink("https://beta.sankakucomplex.com/" + languageFromURL + "/post/show/" + post.get("id") + "?tags=" + tagsUrlEncoded);
                SankakucomplexCom.parseFileInfoAndSetFilenameAPI(link, post);
                link._setFilePackage(fp);
                ret.add(link);
                distribute(link);
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (page == maxPage) {
                logger.info("Stopping because: Reached user defined max page limit of " + maxPage);
                break;
            } else if (StringUtils.isEmpty(nextPageHash)) {
                logger.info("Stopping because: Reached end(?)");
                break;
            } else if (data.size() < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains less items than " + maxItemsPerPage);
                break;
            } else {
                page++;
                query.addAndReplace("next", Encoding.urlEncode(nextPageHash));
            }
        } while (true);
        return ret;
    }

    /** Crawls books via tag. Typically used to crawl all books of a user. */
    private ArrayList<DownloadLink> crawlTagsBooks(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final SankakucomplexComConfig cfg = PluginJsonConfig.get(SankakucomplexComConfig.class);
        final int maxPage = cfg.getBookTagCrawlerMaxPageLimit();
        if (maxPage == 0) {
            logger.info("Stopping because: User disabled books tag crawler");
            return ret;
        }
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_TAGS_BOOKS);
        final String languageFromURL = urlinfo.getMatch(0);
        String tags = urlinfo.getMatch(1);
        if (languageFromURL == null || tags == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        tags = URLEncode.decodeURIComponent(tags);
        final int maxItemsPerPage = 20;
        final UrlQuery query = new UrlQuery();
        query.add("lang", languageFromURL);
        query.add("limit", Integer.toString(maxItemsPerPage));
        query.add("includes[]", "series");
        query.add("tags", Encoding.urlEncode(tags));
        query.add("pool_type", "0");
        int page = 1;
        do {
            br.setFollowRedirects(true);
            br.getPage(API_BASE + "/pools/keyset?" + query.toString());
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> meta = (Map<String, Object>) entries.get("meta");
            final String nextPageHash = (String) meta.get("next");
            final List<Map<String, Object>> data = (List<Map<String, Object>>) entries.get("data");
            if (data.isEmpty()) {
                if (ret.isEmpty()) {
                    logger.info("Looks like users' given tag does not lead to any search results");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    logger.info("Stopping because: Current page is empty");
                    break;
                }
            }
            for (final Map<String, Object> book : data) {
                final DownloadLink link = this.createDownloadlink("https://beta.sankakucomplex.com/" + languageFromURL + "/books/" + book.get("id") + "?tags=" + tags);
                ret.add(link);
                distribute(link);
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (page == maxPage) {
                logger.info("Stopping because: Reached user defined max page limit of " + maxPage);
                break;
            } else if (StringUtils.isEmpty(nextPageHash)) {
                logger.info("Stopping because: Reached end(?)");
                break;
            } else if (data.size() < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains less items than " + maxItemsPerPage);
                break;
            } else {
                page++;
                query.addAndReplace("next", Encoding.urlEncode(nextPageHash));
            }
        } while (true);
        return ret;
    }

    /** Crawls all pages of a book. */
    private ArrayList<DownloadLink> crawlBook(final CryptedLink param) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_BOOK);
        final String languageFromURL = urlinfo.getMatch(0);
        final String bookID = urlinfo.getMatch(1);
        if (languageFromURL == null || bookID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(API_BASE + "/pools/" + bookID + "?lang=" + languageFromURL + "&includes[]=series&exceptStatuses[]=deleted");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> author = (Map<String, Object>) entries.get("author");
        String bookTitle = (String) entries.get("name_en");
        if (StringUtils.isEmpty(bookTitle)) {
            bookTitle = (String) entries.get("name_ja");
        }
        final List<Map<String, Object>> posts = (List<Map<String, Object>>) entries.get("posts");
        int page = 0;
        for (final Map<String, Object> post : posts) {
            final DownloadLink link = this.createDownloadlink("https://beta.sankakucomplex.com/" + languageFromURL + "/post/show/" + post.get("id") + "?tags=pool%3A" + bookID + "&page=" + page);
            link.setProperty(SankakucomplexCom.PROPERTY_BOOK_TITLE, bookTitle);
            link.setProperty(SankakucomplexCom.PROPERTY_PAGE_NUMBER, page);
            link.setProperty(SankakucomplexCom.PROPERTY_PAGE_NUMBER_MAX, posts.size() - 1);
            SankakucomplexCom.parseFileInfoAndSetFilenameAPI(link, post);
            ret.add(link);
            page++;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(author.get("name") + " - " + bookTitle);
        fp.addLinks(ret);
        return ret;
    }
}
