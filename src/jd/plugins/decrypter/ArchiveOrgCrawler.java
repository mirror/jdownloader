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

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.archiveorg.ArchiveOrgConfig;
import org.jdownloader.plugins.components.archiveorg.ArchiveOrgConfig.BookCrawlMode;
import org.jdownloader.plugins.components.archiveorg.ArchiveOrgConfig.PlaylistCrawlMode;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.HashInfo;
import jd.plugins.hoster.ArchiveOrg;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "archive.org", "subdomain.archive.org" }, urls = { "https?://(?:www\\.)?archive\\.org/((?:details|download|stream|embed)/.+|search\\?query=.+)", "https?://[^/]+\\.archive\\.org/view_archive\\.php\\?archive=[^\\&]+(?:\\&file=[^\\&]+)?" })
public class ArchiveOrgCrawler extends PluginForDecrypt {
    public ArchiveOrgCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        /* 2023-07-10: looks like some detail pages return 503 instead of 200 */
        br.setAllowedResponseCodes(503);
        return br;
    }

    private static boolean isArchiveURL(final String url) throws MalformedURLException {
        final UrlQuery query = UrlQuery.parse(url);
        return url.contains("view_archive.php") && query.get("file") == null;
    }

    private final String  PATTERN_DOWNLOAD         = "(?i)https?://[^/]+/download/([\\w\\-]+).*";
    private final String  PATTERN_SEARCH           = "(?i)https?://[^/]+/search\\?query=.+";
    private ArchiveOrg    hostPlugin               = null;
    private final boolean USE_NEW_HANDLING_2024_04 = true;

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl().replace("://www.", "://").replaceFirst("(?i)/(stream|embed)/", "/download/");
        if (new Regex(contenturl, PATTERN_DOWNLOAD).patternFind()) {
            return crawlPatternSlashDownload(contenturl);
        } else if (contenturl.matches(PATTERN_SEARCH)) {
            return this.crawlSearchQueryURL(br, param);
        } else if (USE_NEW_HANDLING_2024_04) {
            return this.crawlMetadataJsonV2(contenturl);
        } else {
            /* 2024-03-27: TODO: Remove this old/deprecated code */
            /*
             * 2020-08-26: Login might sometimes be required for book downloads.
             */
            ensureInitHosterplugin();
            final Account account = AccountController.getInstance().getValidAccount(hostPlugin.getHost());
            if (account != null) {
                /* Login whenever possible. */
                hostPlugin.login(account, false);
            }
            URLConnectionAdapter con = null;
            boolean isArchiveContentURL = isArchiveURL(contenturl);
            if (isArchiveContentURL) {
                br.getPage(contenturl);
            } else {
                try {
                    /* Check if we have a direct URL --> Host plugin */
                    con = br.openGetConnection(contenturl);
                    isArchiveContentURL = isArchiveURL(con.getURL().toExternalForm());
                    /*
                     * 2020-03-04: E.g. directurls will redirect to subdomain e.g. ia800503.us.archive.org --> Sometimes the only way to
                     * differ between a file or expected html.
                     */
                    final String host = Browser.getHost(con.getURL(), true);
                    if (!isArchiveContentURL && (this.looksLikeDownloadableContent(con) || con.getLongContentLength() > br.getLoadLimit() || !host.equals("archive.org"))) {
                        // final DownloadLink fina = this.createDownloadlink(parameter.replace("archive.org", host_decrypted));
                        final DownloadLink link = new DownloadLink(hostPlugin, null, hostPlugin.getHost(), contenturl, true);
                        if (this.looksLikeDownloadableContent(con)) {
                            if (con.getCompleteContentLength() > 0) {
                                if (con.isContentDecoded()) {
                                    link.setDownloadSize(con.getCompleteContentLength());
                                } else {
                                    link.setVerifiedFileSize(con.getCompleteContentLength());
                                }
                            }
                            link.setFinalFileName(getFileNameFromHeader(con));
                            link.setAvailable(true);
                        } else {
                            /* 2021-02-05: Either offline or account-only. Assume offline for now. */
                            link.setAvailable(false);
                        }
                        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                        ret.add(link);
                        return ret;
                    } else {
                        final int previousLoadLimit = br.getLoadLimit();
                        try {
                            br.setLoadLimit(Integer.MAX_VALUE);
                            br.followConnection();
                        } finally {
                            br.setLoadLimit(previousLoadLimit);
                        }
                    }
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final boolean isBookPreviewAvailable = findBookReaderURLWebsite(br) != null;
            if (isBookPreviewAvailable) {
                /* Book is officially downloadable and loose book pages are also available -> Process as wished by the user-preferences. */
                final boolean isOfficiallyDownloadable = br.containsHTML("class=\"download-button\"") && !br.containsHTML("class=\"download-lending-message\"");
                final BookCrawlMode mode = PluginJsonConfig.get(ArchiveOrgConfig.class).getBookCrawlMode();
                if (isOfficiallyDownloadable) {
                    if (mode == BookCrawlMode.PREFER_ORIGINAL) {
                        try {
                            logger.info("Trying to crawl original files");
                            return crawlDetailsWebsite(br.cloneBrowser(), param);
                        } catch (final Exception e) {
                            /* Rare case e.g.: https://archive.org/details/isbn_9789814585354 */
                            logger.info("Details crawler failed -> Fallback to loose book pages");
                            return crawlBookWebsite(br, param, account);
                        }
                    } else if (mode == BookCrawlMode.ORIGINAL_AND_LOOSE_PAGES) {
                        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                        try {
                            ret.addAll(crawlDetailsWebsite(br, param));
                        } catch (final Exception ignore) {
                            logger.log(ignore);
                            logger.info("Details crawler failed -> Fallback to returning loose book pages ONLY");
                        }
                        ret.addAll(crawlBookWebsite(br, param, account));
                        return ret;
                    } else {
                        /* Only loose book pages can be crawled. */
                        return crawlBookWebsite(br, param, account);
                    }
                } else {
                    return crawlBookWebsite(br, param, account);
                }
            } else if (isArchiveContentURL) {
                return crawlArchiveContent();
            } else if (StringUtils.containsIgnoreCase(contenturl, "/details/")) {
                return crawlDetailsWebsite(br, param);
            } else {
                return crawlFiles(contenturl);
            }
        }
    }

    private void ensureInitHosterplugin() throws PluginException {
        if (this.hostPlugin == null) {
            this.hostPlugin = (ArchiveOrg) getNewPluginForHostInstance("archive.org");
        }
    }

    private ArrayList<DownloadLink> crawlPatternSlashDownload(final String url) throws Exception {
        if (url == null) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        final String urlWithoutParams = URLHelper.getUrlWithoutParams(url);
        final String path = new URL(urlWithoutParams).getPath().replaceFirst("(?i)^/download/", "/");
        final String identifier = getIdentifierFromURL(url);
        if (identifier == null) {
            /* Invalid URL/identifier */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (USE_NEW_HANDLING_2024_04) {
            return this.crawlMetadataJsonV2(identifier, url);
        } else {
            // TODO: 2024-03-28 Delete this old/deprecated handling
            final boolean allowCheckForDirecturl = true;
            if (path.contains("/") && allowCheckForDirecturl) {
                /**
                 * 2023-05-30: Especially important when user adds a like to a file inside a .zip file as that will not be contained in the
                 * XML which we are crawling below. </br>
                 * Reference: https://board.jdownloader.org/showthread.php?t=89368
                 */
                logger.info("Path contains subpath -> Checking for single directurl");
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(url);
                    ensureInitHosterplugin();
                    if (this.looksLikeDownloadableContent(con)) {
                        logger.info("URL is directurl");
                        final DownloadLink link = new DownloadLink(hostPlugin, null, hostPlugin.getHost(), url, true);
                        if (con.getCompleteContentLength() > 0) {
                            if (con.isContentDecoded()) {
                                link.setDownloadSize(con.getCompleteContentLength());
                            } else {
                                link.setVerifiedFileSize(con.getCompleteContentLength());
                            }
                        }
                        final String filenameFromHeader = getFileNameFromHeader(con);
                        if (filenameFromHeader != null) {
                            link.setFinalFileName(Encoding.htmlDecode(filenameFromHeader).trim());
                        }
                        link.setAvailable(true);
                        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                        ret.add(link);
                        return ret;
                    } else {
                        logger.info("URL is not a directurl");
                        switch (con.getResponseCode()) {
                        case 404:
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        case 400:
                            throw new DecrypterRetryException(RetryReason.HOST);
                        default:
                            break;
                        }
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            return crawlXML(url, br, path);
        }
    }

    public static String getIdentifierFromURL(final String url) {
        return new Regex(url, "/(?:details|download|metadata)/([A-Za-z0-9\\-_\\.]{2,})").getMatch(0);
    }

    private ArrayList<DownloadLink> crawlCollection(final String collectionIdentifier) throws Exception {
        if (StringUtils.isEmpty(collectionIdentifier)) {
            throw new IllegalArgumentException();
        }
        return crawlViaScrapeAPI(br, "collection:" + collectionIdentifier, -1);
    }

    private ArrayList<DownloadLink> crawlSearchQueryURL(final Browser br, final CryptedLink param) throws Exception {
        final ArchiveOrgConfig cfg = PluginJsonConfig.get(ArchiveOrgConfig.class);
        final int maxResults = cfg.getSearchTermCrawlerMaxResultsLimit();
        if (maxResults == 0) {
            logger.info("User disabled search term crawler -> Returning empty array");
            return new ArrayList<DownloadLink>();
        }
        final UrlQuery query = UrlQuery.parse(param.getCryptedUrl());
        String searchQuery = query.get("query");
        if (searchQuery != null) {
            /* Gets encoded later -> Needs to be decoded here. */
            searchQuery = Encoding.htmlDecode(searchQuery).trim();
        }
        if (StringUtils.isEmpty(searchQuery)) {
            /* User supplied invalid URL. */
            throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, "INVALID_SEARCH_QUERY");
        }
        final ArrayList<DownloadLink> searchResults = crawlViaScrapeAPI(br, searchQuery, maxResults);
        if (searchResults.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "NO_SEARCH_RESULTS_FOR_QUERY_" + searchQuery);
        }
        return searchResults;
    }

    /**
     * Uses search APIv1 </br>
     * API: Docs: https://archive.org/help/aboutsearch.htm
     */
    private ArrayList<DownloadLink> crawlViaScrapeAPI(final Browser br, final String searchTerm, final int maxResultsLimit) throws Exception {
        if (StringUtils.isEmpty(searchTerm)) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        } else if (maxResultsLimit == 0) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        logger.info("Searching for: " + searchTerm + " | maxResultsLimit = " + maxResultsLimit);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final int maxNumberofItemsPerPage = 10000;
        final int minNumberofItemsPerPage = 100;
        final UrlQuery query = new UrlQuery();
        query.add("fields", "identifier");
        query.add("q", Encoding.urlEncode(searchTerm));
        final int maxNumberofItemsPerPageForThisRun;
        if (maxResultsLimit == -1) {
            /* -1 means unlimited -> Use internal hardcoded limit. */
            maxNumberofItemsPerPageForThisRun = maxNumberofItemsPerPage;
        } else if (maxResultsLimit <= minNumberofItemsPerPage) {
            maxNumberofItemsPerPageForThisRun = minNumberofItemsPerPage;
        } else if (maxResultsLimit < maxNumberofItemsPerPage) {
            maxNumberofItemsPerPageForThisRun = maxResultsLimit;
        } else {
            maxNumberofItemsPerPageForThisRun = maxNumberofItemsPerPage;
        }
        query.add("count", Integer.toString(maxNumberofItemsPerPageForThisRun));
        String cursor = null;
        int page = 1;
        do {
            br.getPage("https://" + this.getHost() + "/services/search/v1/scrape?" + query.toString());
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final int maxItems = ((Number) entries.get("total")).intValue();
            if (maxItems == 0) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, searchTerm);
            }
            final List<Map<String, Object>> items = (List<Map<String, Object>>) entries.get("items");
            boolean stopDueToCrawlLimitReached = false;
            for (final Map<String, Object> item : items) {
                final DownloadLink link = this.createDownloadlink("https://archive.org/details/" + item.get("identifier").toString());
                ret.add(link);
                /* The following statement makes debugging easier. */
                if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    distribute(link);
                }
                if (ret.size() == maxResultsLimit) {
                    /* Do not step out of main loop yet so we can get the log output down below one last time. */
                    stopDueToCrawlLimitReached = true;
                    break;
                }
            }
            final String lastCursor = cursor;
            cursor = (String) entries.get("cursor");
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size() + "/" + maxItems + " | maxResultsLimit: " + maxResultsLimit + " | Cursor: " + lastCursor + " | Next cursor: " + cursor);
            if (stopDueToCrawlLimitReached) {
                logger.info("Stopping because: Reached max allowed results: " + maxResultsLimit);
                break;
            } else if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (StringUtils.isEmpty(cursor)) {
                logger.info("Stopping because: Reached last page: " + lastCursor);
                break;
            } else if (ret.size() >= maxItems) {
                /* Additional fail-safe */
                logger.info("Stopping because: Found all items: " + maxItems);
                break;
            } else if (items.size() < maxNumberofItemsPerPageForThisRun) {
                /* Additional fail-safe */
                logger.info("Stopping because: Current page contains less items than max allowed per page for this run: " + maxNumberofItemsPerPageForThisRun);
                break;
            } else {
                /* Continue to next page */
                query.add("cursor", Encoding.urlEncode(cursor));
                page++;
            }
        } while (true);
        return ret;
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlArchiveContent() throws Exception {
        /* 2024-03-26: TODO: Check what this is doing and if it is still working */
        /* 2020-09-07: Contents of a .zip/.rar file are also accessible and downloadable separately. */
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String archiveName = new Regex(br.getURL(), ".*/([^/]+)$").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(archiveName).trim());
        final String[] htmls = br.getRegex("<tr><td>(.*?)</tr>").getColumn(0);
        for (final String html : htmls) {
            String url = new Regex(html, "(?i)(/download/[^\"\\']+)").getMatch(0);
            final String filesizeBytesStr = new Regex(html, "id=\"size\">(\\d+)").getMatch(0);
            if (StringUtils.isEmpty(url)) {
                /* Skip invalid items */
                continue;
            }
            url = "https://archive.org" + url;
            final DownloadLink dl = this.createDownloadlink(url);
            if (filesizeBytesStr != null) {
                dl.setDownloadSize(Long.parseLong(filesizeBytesStr));
            }
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            ret.add(dl);
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlBook(final Browser br, final String ajaxurl, final Account account) throws Exception {
        if (StringUtils.isEmpty(ajaxurl)) {
            throw new IllegalArgumentException();
        }
        final String identifier = UrlQuery.parse(ajaxurl).get("id");
        if (StringUtils.isEmpty(identifier)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(ajaxurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        ensureInitHosterplugin();
        final Map<String, Object> root = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> data = (Map<String, Object>) root.get("data");
        final Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
        final Object descriptionObject = metadata.get("description");
        final String description;
        if (descriptionObject instanceof String) {
            description = (String) descriptionObject;
        } else if (descriptionObject instanceof List) {
            description = StringUtils.join((List) descriptionObject, ";");
        } else {
            description = null;
        }
        final Map<String, Object> lendingInfo = (Map<String, Object>) data.get("lendingInfo");
        // final Map<String, Object> lendingStatus = (Map<String, Object>) lendingInfo.get("lendingStatus");
        final long daysLeftOnLoan = ((Number) lendingInfo.get("daysLeftOnLoan")).longValue();
        final long secondsLeftOnLoan = ((Number) lendingInfo.get("secondsLeftOnLoan")).longValue();
        long loanedSecondsLeft = 0;
        if (daysLeftOnLoan > 0) {
            loanedSecondsLeft += daysLeftOnLoan * 24 * 60 * 60;
        }
        if (secondsLeftOnLoan > 0) {
            loanedSecondsLeft += secondsLeftOnLoan;
        }
        final Map<String, Object> brOptions = (Map<String, Object>) data.get("brOptions");
        final boolean isLendingRequired = (Boolean) lendingInfo.get("isLendingRequired") == Boolean.TRUE;
        String contentURLFormat = generateBookContentURL(identifier);
        final String bookId = brOptions.get("bookId").toString();
        String title = ((String) brOptions.get("bookTitle")).trim();
        final String subPrefix = (String) brOptions.get("subPrefix");
        final boolean isMultiVolumeBook;
        if (subPrefix != null && !subPrefix.equals(bookId)) {
            /**
             * Books can have multiple volumes. In this case lending the main book will basically lend all volumes alltogether. </br>
             * Problem: Title is the same for all items --> Append this subPrefix to the title to fix that.
             */
            title += " - " + subPrefix;
            contentURLFormat += "/" + subPrefix;
            isMultiVolumeBook = true;
        } else {
            isMultiVolumeBook = false;
        }
        final String pageFormat;
        if (ajaxurl.matches("(?i).*/page/n\\d+.*")) {
            pageFormat = "/page/n%d";
        } else {
            pageFormat = "/page/%d";
        }
        /*
         * Defines how book pages will be arranged on the archive.org website. User can open single pages faster in browser if we get this
         * right.
         */
        final String bookDisplayMode = new Regex(ajaxurl, "(?i)/mode/([^/]+)").getMatch(0);
        final List<Object> imagesO = (List<Object>) brOptions.get("data");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int internalPageIndex = 0;
        for (final Object imageO : imagesO) {
            /**
             * Most of all objects will contain an array with 2 items --> Books always have two viewable pages. </br>
             * Exception = First page --> Cover
             */
            final List<Object> pagesO = (List<Object>) imageO;
            for (final Object pageO : pagesO) {
                final Map<String, Object> bookpage = (Map<String, Object>) pageO;
                /* When this starts at 0 this means the book has a cover else this will start at 1 -> No cover. */
                final int archiveOrgPageIndex = ((Number) bookpage.get("leafNum")).intValue();
                final String url = bookpage.get("uri").toString();
                final DownloadLink link = new DownloadLink(hostPlugin, null, "archive.org", url, true);
                String contentURL = contentURLFormat;
                if (archiveOrgPageIndex > 1) {
                    contentURL += String.format(pageFormat, archiveOrgPageIndex + 1);
                }
                if (bookDisplayMode != null) {
                    contentURL += "/mode/" + bookDisplayMode;
                }
                link.setContentUrl(contentURL);
                link.setProperty(ArchiveOrg.PROPERTY_BOOK_ID, identifier);
                link.setProperty(ArchiveOrg.PROPERTY_BOOK_PAGE, archiveOrgPageIndex);
                link.setProperty(ArchiveOrg.PROPERTY_BOOK_PAGE_INTERNAL_INDEX, internalPageIndex);
                if (isMultiVolumeBook) {
                    link.setProperty(ArchiveOrg.PROPERTY_BOOK_SUB_PREFIX, subPrefix);
                }
                if (Boolean.TRUE.equals(isLendingRequired)) {
                    link.setProperty(ArchiveOrg.PROPERTY_IS_LENDING_REQUIRED, true);
                }
                if (loanedSecondsLeft > 0) {
                    link.setProperty(ArchiveOrg.PROPERTY_IS_BORROWED_UNTIL_TIMESTAMP, System.currentTimeMillis() + loanedSecondsLeft * 1000);
                }
                /**
                 * Mark pages that are not viewable in browser as offline. </br>
                 * If we have borrowed this book, this field will not exist at all.
                 */
                final Object viewable = bookpage.get("viewable");
                if (Boolean.FALSE.equals(viewable)) {
                    /* Only downloadable with account */
                    if (PluginJsonConfig.get(ArchiveOrgConfig.class).isMarkNonViewableBookPagesAsOfflineIfNoAccountIsAvailable() && account == null) {
                        link.setAvailable(false);
                    } else {
                        /* Always mark all pages as online. Non-viewable pages can only be downloaded when an account is present. */
                        link.setAvailable(true);
                    }
                } else {
                    link.setAvailable(true);
                    if (account == null || loanedSecondsLeft == 0) {
                        link.setProperty(ArchiveOrg.PROPERTY_IS_FREE_DOWNLOADABLE_BOOK_PREVIEW_PAGE, true);
                    }
                }
                ret.add(link);
                internalPageIndex++;
            }
        }
        if (account != null) {
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
        /* Add additional properties and filenames. */
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        fp.setPackageKey("internetarchive://book/" + identifier + "/" + subPrefix);
        /* Now we know how many pages the book has -> Set additional information and filename. */
        final int padLength = StringUtils.getPadLength(internalPageIndex);
        for (final DownloadLink result : ret) {
            final int thispage = result.getIntegerProperty(ArchiveOrg.PROPERTY_BOOK_PAGE_INTERNAL_INDEX, 0);
            /* Set filename */
            result.setFinalFileName(StringUtils.formatByPadLength(padLength, thispage) + "_" + title + ".jpg");
            /* Assign FilePackage to item so all results of this run get placed into one package. */
            result._setFilePackage(fp);
        }
        return ret;
    }

    /** Crawls desired book. Given browser instance needs to access URL to book in beforehand! */
    @Deprecated
    public ArrayList<DownloadLink> crawlBookWebsite(final Browser br, final CryptedLink param, final Account account) throws Exception {
        /* Crawl all pages of a book */
        final String bookAjaxURL = findBookReaderURLWebsite(br);
        if (bookAjaxURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return crawlBook(br, bookAjaxURL, account);
    }

    private String findBookReaderURLWebsite(final Browser br) {
        String url = br.getRegex("(?i)(?:\\'|\")([^\\'\"]+BookReaderJSIA\\.php\\?[^\\'\"]+)").getMatch(0);
        if (url != null) {
            url = PluginJSonUtils.unescape(url);
            return url;
        }
        return null;
    }

    private static HashMap<String, AtomicInteger> LOCKS = new HashMap<String, AtomicInteger>();

    private Object requestLock(String name) {
        synchronized (LOCKS) {
            AtomicInteger lock = LOCKS.get(name);
            if (lock == null) {
                lock = new AtomicInteger(0);
                LOCKS.put(name, lock);
            }
            lock.incrementAndGet();
            return lock;
        }
    }

    private synchronized void unLock(String name) {
        synchronized (LOCKS) {
            final AtomicInteger lock = LOCKS.get(name);
            if (lock != null) {
                if (lock.decrementAndGet() == 0) {
                    LOCKS.remove(name);
                }
            }
        }
    }

    private ArrayList<DownloadLink> crawlMetadataJsonV2(String sourceurl) throws Exception {
        if (sourceurl == null) {
            throw new IllegalArgumentException();
        }
        final String identifier = getIdentifierFromURL(sourceurl);
        if (identifier == null) {
            /* Invalid URL */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return this.crawlMetadataJsonV2(identifier, sourceurl);
    }

    /** Work in progress, see https://archive.org/metadata/<identifier> */
    private ArrayList<DownloadLink> crawlMetadataJsonV2(final String identifier, String sourceurl) throws Exception {
        if (StringUtils.isEmpty(identifier)) {
            throw new IllegalArgumentException();
        }
        /* Check if given ideantifier looks to be a user profile. */
        if (identifier.startsWith("@")) {
            return this.crawlProfile(identifier, sourceurl);
        }
        /* The following request will return an empty map if the given identifier is invalid. */
        final Browser brc = br.cloneBrowser();
        /* The json answer can be really big. */
        brc.setLoadLimit(Integer.MAX_VALUE);
        brc.getPage("https://archive.org/metadata/" + Encoding.urlEncode(identifier));
        final Map<String, Object> root = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
        // final Boolean is_dark = (Boolean) root.get("is_dark"); // This means that the content is offline(?)
        final List<Map<String, Object>> root_files = (List<Map<String, Object>>) root.get("files");
        if (root_files == null || root_files.isEmpty()) {
            /* Deleted item */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root_metadata = (Map<String, Object>) root.get("metadata");
        /* There is different servers to choose from e.g. see also fields "d1", "d2" and "workable_servers". */
        final String mediatype = root_metadata.get("mediatype").toString();
        if (StringUtils.equalsIgnoreCase(mediatype, "collection")) {
            /* Crawl item with collection crawler */
            return this.crawlCollection(identifier);
        }
        final ArchiveOrgConfig cfg = PluginJsonConfig.get(ArchiveOrgConfig.class);
        String desiredSubpathDecoded = null;
        if (sourceurl != null) {
            sourceurl = URLHelper.getUrlWithoutParams(sourceurl);
        }
        final String fullpath = new URL(sourceurl).getPath();
        /* JSON metadata will always contain all files but in this case we only want to get all files in a specific subfolder. */
        final String[] urlParts = fullpath.split("/");
        // final Pattern ignorepart = Pattern.compile("detail|download");
        boolean buildSubpathNow = false;
        for (final String urlPart : urlParts) {
            if (urlPart.equals(identifier)) {
                /* Everything alter the identifier will be used as our internal path. */
                buildSubpathNow = true;
            } else if (buildSubpathNow) {
                if (desiredSubpathDecoded == null) {
                    desiredSubpathDecoded = Encoding.htmlDecode(urlPart);
                } else {
                    desiredSubpathDecoded += "/" + Encoding.htmlDecode(urlPart);
                }
            }
        }
        final String server = root.get("server").toString();
        final String dir = root.get("dir").toString();
        /* Crawl files */
        final ArrayList<DownloadLink> originalItems = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> audioPlaylistItems = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> desiredSubpathItems = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Object descriptionObject = root_metadata.get("description");
        final String description;
        if (descriptionObject instanceof String) {
            description = (String) descriptionObject;
        } else if (descriptionObject instanceof List) {
            description = StringUtils.join((List) descriptionObject, ";");
        } else {
            description = null;
        }
        final FilePackage fpRoot = FilePackage.getInstance();
        fpRoot.setName(identifier);
        if (!StringUtils.isEmpty(description)) {
            fpRoot.setComment(description);
        }
        final Map<String, FilePackage> packagemap = new HashMap<String, FilePackage>();
        packagemap.put(identifier, fpRoot);
        final boolean crawlOriginalFilesOnly = cfg.isFileCrawlerCrawlOnlyOriginalVersions();
        final boolean crawlArchiveView = cfg.isFileCrawlerCrawlArchiveView();
        final boolean crawlMetadataFiles = cfg.isFileCrawlerCrawlMetadataFiles();
        final boolean crawlThumbnails = cfg.isFileCrawlerCrawlThumbnails();
        final boolean crawlRestrictedItems = cfg.isFileCrawlerCrawlRestrictedItems();
        logger.info("Crawling all files below path: " + desiredSubpathDecoded);
        final List<String> skippedItemsFilepaths = new ArrayList<String>();
        final List<List<String>> originalFilesListsForVideoStreams = new ArrayList<List<String>>();
        final HashSet<String> originalFilenamesDupeCollection = new HashSet<String>();
        /** Restricted access usually means that original files are not downloadable or only DRM protected / encrypted items exist. */
        final boolean isAccessRestricted = StringUtils.equalsIgnoreCase((String) root_metadata.get("access-restricted-item"), "true");
        for (final Map<String, Object> filemap : root_files) {
            final String source = filemap.get("source").toString(); // "original", "derivative" or "metadata"
            final String format = (String) filemap.get("format");
            /* Boolean as string */
            final boolean isOldVersion = StringUtils.equalsIgnoreCase((String) filemap.get("old_version"), "true");
            final boolean isOriginal = source.equalsIgnoreCase("original");
            final boolean isMetadata = StringUtils.equalsIgnoreCase(format, "metadata");
            final boolean isThumbnail = StringUtils.equalsIgnoreCase(format, "Thumbnail");
            // final boolean isArchiveViewSupported = false; // TODO: Check this
            // final Object originalO = filemap.get("original");
            /* Boolean as string */
            final boolean isAccountRequiredForDownload = StringUtils.equalsIgnoreCase((String) filemap.get("private"), "true");
            final boolean isRestrictedDownload = isAccountRequiredForDownload && isAccessRestricted;
            String pathWithFilename = filemap.get("name").toString();
            if (Encoding.isHtmlEntityCoded(pathWithFilename)) {
                /* Will sometimes contain "&amp;" */
                pathWithFilename = Encoding.htmlOnlyDecode(pathWithFilename);
            }
            /* Find path- and filename */
            /* Relative path to this file including identifier as root folder. */
            String thisPath;
            String filename = null;
            if (pathWithFilename.contains("/")) {
                /* Separate path and filename. */
                thisPath = "";
                final String[] pathSegments = pathWithFilename.split("/");
                int index = 0;
                for (final String pathSegment : pathSegments) {
                    final boolean isLastSegment = index == pathSegments.length - 1;
                    if (isLastSegment) {
                        filename = pathSegment;
                    } else {
                        if (thisPath == null) {
                            thisPath = pathSegment;
                        } else {
                            if (thisPath.length() > 0) {
                                thisPath += "/";
                            }
                            thisPath += pathSegment;
                        }
                    }
                    index++;
                }
                /* Add identifier slash root dir name to path. */
                thisPath = identifier + "/" + thisPath;
            } else {
                /* Root */
                thisPath = identifier;
                filename = pathWithFilename;
            }
            final Object fileSizeO = filemap.get("size");
            final Object audioTrackPositionO = filemap.get("track");
            String url = "https://archive.org/download/" + identifier;
            if (pathWithFilename.startsWith("/")) {
                url += URLEncode.encodeURIComponent(pathWithFilename);
            } else {
                url += "/" + URLEncode.encodeURIComponent(pathWithFilename);
            }
            // final String directurl = "https://" + server + dir + "/" + URLEncode.encodeURIComponent(pathWithFilename);
            final DownloadLink file = this.createDownloadlink(url);
            file.setProperty(ArchiveOrg.PROPERTY_ARTIST, filemap.get("artist")); // Optional field
            file.setProperty(ArchiveOrg.PROPERTY_TITLE, filemap.get("title")); // Optional field
            file.setProperty(ArchiveOrg.PROPERTY_ARTIST, filemap.get("artist")); // optional field
            file.setProperty(ArchiveOrg.PROPERTY_GENRE, filemap.get("genre")); // optional field
            if (fileSizeO != null) {
                if (fileSizeO instanceof Number) {
                    file.setVerifiedFileSize(((Number) fileSizeO).longValue());
                } else {
                    file.setVerifiedFileSize(Long.parseLong(fileSizeO.toString()));
                }
            }
            final String crc32 = (String) filemap.get("crc32");
            if (crc32 != null) {
                file.setHashInfo(HashInfo.parse(crc32));
            }
            final String md5 = (String) filemap.get("md5");
            if (md5 != null) {
                file.setMD5Hash(md5);
            }
            final String sha1 = (String) filemap.get("sha1");
            if (sha1 != null) {
                file.setSha1Hash(sha1);
            }
            file.setProperty(ArchiveOrg.PROPERTY_TIMESTAMP_FROM_API_LAST_MODIFIED, filemap.get("mtime"));
            if (isRestrictedDownload) {
                file.setProperty(ArchiveOrg.PROPERTY_IS_RESTRICTED, true);
                /*
                 * Item is not downloadable at all -> Disable it so download will not even be attempted and also as a visual indicator for
                 * the user.
                 */
                file.setEnabled(false);
            } else if (isAccountRequiredForDownload) {
                file.setProperty(ArchiveOrg.PROPERTY_IS_ACCOUNT_REQUIRED, true);
            }
            if (audioTrackPositionO != null) {
                /* Track position given -> Item must be part of a playlist. */
                final DownloadLink audioPlaylistItem = this.createDownloadlink(url);
                audioPlaylistItem.setProperties(file.getProperties());
                audioPlaylistItem.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_POSITION, Integer.parseInt(audioTrackPositionO.toString()));
                /* Add item to list of playlist results. */
                audioPlaylistItems.add(audioPlaylistItem);
            }
            file.setAvailable(true);
            /* Set new filename */
            ArchiveOrg.setFinalFilename(file, filename);
            file.setRelativeDownloadFolderPath(thisPath);
            FilePackage fp = packagemap.get(thisPath);
            if (fp == null) {
                fp = FilePackage.getInstance();
                fp.setName(thisPath);
                packagemap.put(thisPath, fp);
            }
            file._setFilePackage(fp);
            if (desiredSubpathDecoded != null && pathWithFilename.startsWith(desiredSubpathDecoded)) {
                /* Add item to list of results which match our given subpath. */
                desiredSubpathItems.add(file);
            }
            if (isOriginal) {
                originalItems.add(file);
            }
            /* Add items to list of all results. */
            /* Check some skip conditions */
            if (isOldVersion) {
                /* Skip old elements. */
                skippedItemsFilepaths.add(pathWithFilename);
            } else if (isMetadata && !crawlMetadataFiles) {
                /* Only include metadata if wished by the user. */
                skippedItemsFilepaths.add(pathWithFilename);
            } else if (isThumbnail && !crawlThumbnails) {
                /* Only include thumbnails if wished by the user. */
                skippedItemsFilepaths.add(pathWithFilename);
            } else if (isRestrictedDownload && !crawlRestrictedItems) {
                skippedItemsFilepaths.add(pathWithFilename);
            } else {
                ret.add(file);
            }
            // TODO: Check this: Make this work or remove this feature
            // if (crawlArchiveView && isArchiveViewSupported) {
            // final DownloadLink archiveViewURL = createDownloadlink(url + "/");
            // ret.add(archiveViewURL);
            // }
            final Object original = filemap.get("original");
            if (original instanceof List) {
                originalFilesListsForVideoStreams.add((List<String>) original);
            } else if (original instanceof String) {
                originalFilenamesDupeCollection.add(original.toString());
            }
        }
        /* Log skipped results */
        if (skippedItemsFilepaths.size() > 0) {
            logger.info("Skipped file items: " + skippedItemsFilepaths.size());
            logger.info(skippedItemsFilepaths.toString());
        }
        /* Handle playlist related stuff */
        final FilePackage playlistpackage = FilePackage.getInstance();
        final String metadataTitle = (String) root_metadata.get("title");
        if (!StringUtils.isEmpty(metadataTitle)) {
            playlistpackage.setName(metadataTitle + " - playlist");
        } else {
            /* Fallback */
            playlistpackage.setName(identifier + " - playlist");
        }
        playlistpackage.setPackageKey("internetarchive://identifier/" + identifier + "/playlist");
        /* Build video Stream playlist if needed */
        final ArrayList<DownloadLink> videoPlaylistItems = new ArrayList<DownloadLink>();
        final PlaylistCrawlMode playlistCrawlMode = cfg.getPlaylistCrawlMode();
        if (StringUtils.equalsIgnoreCase(mediatype, "texts")) {
            /* Book crawl handling */
            final BookCrawlMode mode = cfg.getBookCrawlMode();
            /* Decide whether or not we need to crawl the loose book pages. */
            if (isAccessRestricted || mode == BookCrawlMode.LOOSE_PAGES || mode == BookCrawlMode.ORIGINAL_AND_LOOSE_PAGES) {
                /* Crawl book */
                /* Books can be split into multiple "parts" -> Collect them here */
                final HashSet<String> subPrefixes = new HashSet<String>();
                final String subPrefixInSourceURL = new Regex(sourceurl, "(?i)/details/[^/]+/([^/#\\?\\&]+)").getMatch(0);
                for (final String str : originalFilenamesDupeCollection) {
                    final Regex chapterregex = new Regex(str, "(.+)_(djvu\\.xml|page_numbers\\.json)");
                    if (chapterregex.patternFind()) {
                        final String subPrefix = chapterregex.getMatch(0);
                        subPrefixes.add(subPrefix);
                    }
                }
                if (subPrefixes.isEmpty()) {
                    logger.warning("FATAL: Book handling failed: Failed to find any subPrefixes");
                    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    return ret;
                }
                if (mode == BookCrawlMode.ORIGINAL_AND_LOOSE_PAGES) {
                    /*
                     * Make original files already appear in linkgrabber now already because crawling loose book pages can take a LOT of
                     * time.
                     */
                    distribute(ret);
                }
                ensureInitHosterplugin();
                final Account account = AccountController.getInstance().getValidAccount(hostPlugin.getHost());
                if (account != null) {
                    /* Login if possible as this can have an influence on the books' 'lending-status'. */
                    hostPlugin.login(account, false);
                }
                logger.info("Crawling book with " + subPrefixes.size() + " subPrefixes | subPrefixInSourceURL = " + subPrefixInSourceURL);
                if (subPrefixInSourceURL != null && subPrefixes.contains(subPrefixInSourceURL)) {
                    /* User wants to crawl specific subPrefix only. */
                    subPrefixes.clear();
                    subPrefixes.add(subPrefixInSourceURL);
                }
                int position = 1;
                final ArrayList<DownloadLink> bookResults = new ArrayList<DownloadLink>();
                for (final String subPrefix : subPrefixes) {
                    logger.info("Crawling book prefix " + position + "/" + subPrefixes.size());
                    final UrlQuery query = new UrlQuery();
                    query.add("id", identifier);
                    query.add("itemPath", dir);
                    query.add("server", server);
                    query.add("format", "jsonp");
                    query.add("subPrefix", subPrefix);
                    query.add("requestUri", "/details/" + identifier);
                    final String url = "https://" + server + "/BookReader/BookReaderJSIA.php?" + query.toString();
                    final ArrayList<DownloadLink> thisBookResults = this.crawlBook(br, url, null);
                    /* Make resulting items appear in linkgrabber now already. */
                    distribute(thisBookResults);
                    if (this.isAbort()) {
                        logger.info("Stopping because: Aborted by user");
                        break;
                    } else {
                        position++;
                    }
                }
                if (mode == BookCrawlMode.ORIGINAL_AND_LOOSE_PAGES) {
                    /*
                     * User also wants to have original book files -> Add loose book pages to our results and return all results.
                     */
                    ret.addAll(bookResults);
                    return ret;
                } else {
                    /* Return loose pages only */
                    return bookResults;
                }
            } else {
                // BookCrawlMode.PREFER_ORIGINAL
            }
        } else if (StringUtils.equalsIgnoreCase(mediatype, "movies") && originalFilesListsForVideoStreams.size() == 1) {
            /* Video "playlist" handling */
            final List<String> thisVideoSegments = originalFilesListsForVideoStreams.get(0);
            int position = 0;
            int offsetSeconds = 0;
            final int stepSeconds = 60;
            /* Video can't be officially downloaded but it can be streamed in segments of X seconds each -> Generate those stream-links */
            for (int counter = 0; counter < thisVideoSegments.size(); counter++) {
                final String directurl = "https://archive.org/download/" + identifier + "/" + identifier + ".mp4?t=" + offsetSeconds + "/" + (offsetSeconds + stepSeconds) + "&ignore=x.mp4";
                final DownloadLink video = this.createDownloadlink(directurl);
                video.setProperty(ArchiveOrg.PROPERTY_FILETYPE, ArchiveOrg.FILETYPE_VIDEO);
                video.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_POSITION, position);
                video.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_SIZE, thisVideoSegments.size());
                ArchiveOrg.setFinalFilename(video, identifier + ".mp4");
                video.setAvailable(true);
                video._setFilePackage(playlistpackage);
                videoPlaylistItems.add(video);
                ret.add(video);
                /* Increment counters */
                position++;
                offsetSeconds += stepSeconds;
            }
            if (playlistCrawlMode == PlaylistCrawlMode.PLAYLIST_ONLY) {
                /* User prefers to only get stream downloads slash "video playlist". */
                return videoPlaylistItems;
            } else if (isAccessRestricted) {
                /*
                 * Original file is not downloadable at all -> Force-return playlist items to provide downloadable items for the user.
                 */
                ret.addAll(videoPlaylistItems);
            } else if (playlistCrawlMode == PlaylistCrawlMode.PLAYLIST_AND_FILES) {
                ret.addAll(videoPlaylistItems);
            } else {
                /* Do not return any playlist items but only original files. */
            }
        } else if (audioPlaylistItems.size() > 0) {
            /* Audio playlist handling */
            // final boolean isAudioPlaylist = mediatype.equalsIgnoreCase("audio");
            final int playlistSize = audioPlaylistItems.size();
            /* Add some additional properties for special playlist items. */
            for (final DownloadLink audioPlaylistItem : audioPlaylistItems) {
                /* Playlist size can only be determined after first loop -> Set that property here. */
                audioPlaylistItem.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_SIZE, playlistSize);
                audioPlaylistItem.setProperty(ArchiveOrg.PROPERTY_FILETYPE, ArchiveOrg.FILETYPE_AUDIO);
                audioPlaylistItem.setAvailable(true);
                audioPlaylistItem._setFilePackage(playlistpackage);
                ArchiveOrg.setFinalFilename(audioPlaylistItem, audioPlaylistItem.getName());
            }
            if (playlistCrawlMode == PlaylistCrawlMode.PLAYLIST_ONLY) {
                /* Return playlist items only */
                return audioPlaylistItems;
            } else if (playlistCrawlMode == PlaylistCrawlMode.PLAYLIST_AND_FILES) {
                /* Return playlist and original files. */
                ret.addAll(audioPlaylistItems);
                return ret;
            } else {
                /* Do not return any playlist items but only original files. */
            }
        }
        /* "Normal" file handling */
        if (desiredSubpathDecoded != null) {
            /* Return only links below desired subpath if desired subpath is available. */
            if (desiredSubpathItems.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return desiredSubpathItems;
        }
        if (crawlOriginalFilesOnly && originalItems.size() > 0) {
            /* Return only original items */
            return originalItems;
        } else {
            /* Return all collected items */
            return ret;
        }
    }

    /** Returns all uploads of a profile. */
    private ArrayList<DownloadLink> crawlProfile(String username, final String sourceurl) throws Exception {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException();
        }
        if (!username.startsWith("@")) {
            /* Curate given parameter. */
            username += "@";
        }
        final UrlQuery sourceurlquery = UrlQuery.parse(sourceurl);
        final String startPageStr = sourceurlquery.get("page");
        /* Allow user to define custom start-page in given URL. */
        final int startPage;
        if (startPageStr != null && startPageStr.matches("\\d+")) {
            logger.info("Starting from user defined page: " + startPageStr);
            startPage = Integer.parseInt(startPageStr);
        } else {
            logger.info("Starting from page 1");
            startPage = 1;
        }
        logger.info("Starting from page " + startPage);
        final int maxItemsPerPage = 100;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final UrlQuery query = new UrlQuery();
        query.add("user_query", "");
        query.add("page_type", "account_details");
        query.add("page_target", Encoding.urlEncode(username));
        query.add("page_elements", "%5B%22uploads%22%5D");
        query.add("hits_per_page", Integer.toString(maxItemsPerPage));
        query.add("sort", "publicdate%3Adesc");
        query.add("aggregations", "false");
        if (sourceurl != null) {
            /* Not important */
            query.add("client_url", Encoding.urlEncode(sourceurl));
        }
        final Browser brc = br.cloneBrowser();
        brc.setAllowedResponseCodes(400);
        int page = startPage;
        final HashSet<String> dupes = new HashSet<String>();
        do {
            query.addAndReplace("page", Integer.toString(page));
            /* This looks to be an internally used version of public crawl/search API v2 beta, see: https://archive.org/services/swagger/ */
            brc.getPage("https://archive.org/services/search/beta/page_production/?" + query.toString());
            if (brc.getHttpConnection().getResponseCode() == 400) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid profile name");
            }
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> hitsmap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "response/body/page_elements/uploads/hits");
            final int totalNumberofItems = ((Number) hitsmap.get("total")).intValue();
            final List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsmap.get("hits");
            int numberofNewItemsThisPage = 0;
            for (final Map<String, Object> hit : hits) {
                final Map<String, Object> fields = (Map<String, Object>) hit.get("fields");
                final String identifier = fields.get("identifier").toString();
                if (!dupes.add(identifier)) {
                    continue;
                }
                numberofNewItemsThisPage++;
                final DownloadLink result = this.createDownloadlink("https://" + this.getHost() + "/download/" + identifier);
                ret.add(result);
            }
            logger.info("Crawled page " + page + " | Crawled new items this page: + " + numberofNewItemsThisPage + " | Crawled items so far: " + ret.size() + "/" + totalNumberofItems);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (hits.size() < maxItemsPerPage) {
                /* Main stop condition */
                logger.info("Stopping because: Current page contains less items than " + maxItemsPerPage + " --> Reached end");
                break;
            } else if (ret.size() >= totalNumberofItems) {
                /* Fail-safe 1 */
                logger.info("Stopping because: Found all items: " + totalNumberofItems);
                break;
            } else if (numberofNewItemsThisPage == 0) {
                /* Fail-safe 2 */
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else {
                /* Continue to next page */
                page++;
            }
        } while (!this.isAbort());
        return ret;
    }

    /** Crawls all files from "/download/..." URLs. */
    @Deprecated
    private ArrayList<DownloadLink> crawlFiles(final String contenturl) throws Exception {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*The item is not available")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("\"/download/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Maybe invalid link or nothing there to download");
        }
        final String subfolderPathURLEncoded = new Regex(contenturl, "(?i)https?://[^/]+/(?:download|details)/(.*?)/?$").getMatch(0);
        final String titleSlug = new Regex(contenturl, "(?i)https?://[^/]+/(?:download|details)/([^/]+)").getMatch(0);
        if (titleSlug == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return crawlXML(br.getURL(), br, subfolderPathURLEncoded);
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlDetailsWebsite(final Browser br, final CryptedLink param) throws Exception {
        final String urlWithoutParams = URLHelper.getUrlWithoutParams(br._getURL());
        final String identifier = getIdentifierFromURL(br._getURL().toExternalForm());
        // TODO: Get rid of 'titleSlug' during refactoring
        final String titleSlug = new Regex(urlWithoutParams, "(?i)/details/([^/]+)").getMatch(0);
        if (identifier == null) {
            /* Invalid URL */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (titleSlug == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String downloadurl = br.getURL("/download/" + titleSlug).toExternalForm();
        if (br.containsHTML("id=\"gamepadtext\"")) {
            /* 2020-09-29: Rare case: Download browser emulated games */
            return this.crawlXML(br.getURL(), br, titleSlug);
        }
        final ArrayList<DownloadLink> playlistItems = new ArrayList<DownloadLink>();
        final String videoJson = br.getRegex("class=\"js-tv3-init\"[^>]*value='(\\{.*?\\})").getMatch(0);
        if (videoJson != null) {
            /* 2022-10-31: Example: https://archive.org/details/MSNBCW_20211108_030000_Four_Seasons_Total_Documentary */
            final Map<String, Object> entries = restoreFromString(videoJson, TypeRef.MAP);
            final String slug = entries.get("TV3.identifier").toString();
            final List<String> urls = (List<String>) entries.get("TV3.clipstream_clips");
            int position = 1;
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(slug);
            for (final String url : urls) {
                final DownloadLink video = this.createDownloadlink(url);
                video.setProperty(ArchiveOrg.PROPERTY_FILETYPE, ArchiveOrg.FILETYPE_VIDEO);
                video.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_POSITION, position);
                video.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_SIZE, urls.size());
                ArchiveOrg.setFinalFilename(video, slug + ".mp4");
                video.setAvailable(true);
                video._setFilePackage(fp);
                playlistItems.add(video);
                position++;
            }
        }
        final String audioPlaylistJson = br.getRegex("class=\"js-play8-playlist\"[^>]*value='(\\[.*?\\])'/>").getMatch(0);
        final String metadataJson = br.getRegex("class=\"js-ia-metadata\"[^>]*value='(\\{.*?\\})'/>").getMatch(0);
        if (audioPlaylistJson != null) {
            final ArrayList<DownloadLink> audioPlaylistItemsSimple = new ArrayList<DownloadLink>();
            final Map<String, DownloadLink> filepathToPlaylistItemMapping = new HashMap<String, DownloadLink>();
            final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) restoreFromString(audioPlaylistJson, TypeRef.OBJECT);
            if (ressourcelist.isEmpty()) {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            int position = 1;
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(titleSlug);
            for (final Map<String, Object> audiomap : ressourcelist) {
                final List<Map<String, Object>> sources = (List<Map<String, Object>>) audiomap.get("sources");
                if (sources.size() > 1) {
                    /* Rare case -> Log it */
                    logger.info("Found audio item with multiple sources: " + audiomap);
                }
                final Map<String, Object> source0 = sources.get(0);
                final String title = (String) audiomap.get("title");
                final DownloadLink audio = this.createDownloadlink(br.getURL(source0.get("file").toString()).toExternalForm());
                audio.setProperty(ArchiveOrg.PROPERTY_FILETYPE, ArchiveOrg.FILETYPE_AUDIO);
                audio.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_POSITION, position);
                audio.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_SIZE, ressourcelist.size());
                audio.setProperty(ArchiveOrg.PROPERTY_ARTIST, audiomap.get("artist")); // optional field
                audio.setProperty(ArchiveOrg.PROPERTY_TITLE, title);
                String filenameOrPath = (String) audiomap.get("orig");
                if (StringUtils.isEmpty(filenameOrPath)) {
                    filenameOrPath = title;
                }
                final String filename = getFilenameFromPath(filenameOrPath);
                ArchiveOrg.setFinalFilename(audio, filename);
                audio.setAvailable(true);
                audio._setFilePackage(fp);
                audioPlaylistItemsSimple.add(audio);
                filepathToPlaylistItemMapping.put(filenameOrPath, audio);
                position++;
            }
            final ArrayList<DownloadLink> audioPlaylistItemsDetailed = new ArrayList<DownloadLink>();
            if (metadataJson != null) {
                /**
                 * Try to find more metadata to the results we already have and combine them with the track-position-data we know. </br>
                 * In the end we should get the best of both worlds: All tracks with track numbers, metadata and file hashes for CRC
                 * checking.
                 */
                logger.info("Looking for more detailed audio metadata");
                audioPlaylistItemsDetailed.addAll(this.crawlMetadataJson(metadataJson, filepathToPlaylistItemMapping));
            } else {
                logger.warning("Failed to find metadataJson -> Can't search for detailed audio information");
            }
            if (audioPlaylistItemsDetailed.size() == ressourcelist.size()) {
                logger.info("Found valid detailed audio information");
                playlistItems.addAll(audioPlaylistItemsDetailed);
            } else {
                logger.info("Failed to obtain detailed audio information");
                playlistItems.addAll(audioPlaylistItemsSimple);
            }
        }
        final String downloadlinkToAllFilesDownload = br.getRegex("(?i)href=\"(/download/[^\"]*?)\">\\s*SHOW ALL").getMatch(0);
        final ArchiveOrgConfig cfg = PluginJsonConfig.get(ArchiveOrgConfig.class);
        final PlaylistCrawlMode playlistCrawlMode = cfg.getPlaylistCrawlMode();
        if (playlistCrawlMode == PlaylistCrawlMode.PLAYLIST_ONLY && playlistItems.size() > 0) {
            logger.info("Returning streaming items ONLY");
            return playlistItems;
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (downloadlinkToAllFilesDownload == null || playlistCrawlMode == PlaylistCrawlMode.PLAYLIST_AND_FILES) {
            ret.addAll(playlistItems);
        }
        if (downloadlinkToAllFilesDownload != null) {
            /* This link will go back into this crawler to find all individual downloadlinks. */
            ret.add(createDownloadlink(downloadurl));
            return ret;
        } else if (br.containsHTML("(?i)>\\s*You must log in to view this content") || br.containsHTML("(?i)>\\s*Item not available|>\\s*The item is not available due to issues with the item's content")) {
            /* 2021-02-24: <p class="theatre-title">You must log in to view this content</p> */
            if (br.containsHTML("/download/" + titleSlug)) {
                /* Account is still required but we can go ahead and crawl all individual file URLs via XML. */
                ret.add(createDownloadlink(downloadurl));
                return ret;
            } else {
                throw new AccountRequiredException();
            }
        }
        if (ret.isEmpty()) {
            final boolean isUserProfile = identifier.startsWith("@");
            if (isUserProfile) {
                return this.crawlProfile(identifier, br.getURL());
            } else {
                logger.info("Crawling collection...");
                final ArrayList<DownloadLink> collectionResults = crawlCollection(identifier);
                if (collectionResults.isEmpty()) {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_COLLECTION_" + identifier);
                }
                ret.addAll(collectionResults);
            }
        }
        return ret;
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlXML(final String contenturl, final Browser xmlbr, final String path) throws IOException, PluginException, DecrypterRetryException {
        if (xmlbr == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (StringUtils.isEmpty(path)) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        /*
         * 2020-03-04: Prefer crawling xml if possible as we then get all contents of that folder including contents of subfolders via only
         * one request!
         */
        String titleSlug = null;
        String desiredSubpathDecoded = null;
        if (path.contains("/")) {
            /* XML will always contain all files but in this case we only want to get all files in a specific subfolder. */
            final String[] urlParts = path.split("/");
            boolean buildSubpathNow = false;
            for (final String urlPart : urlParts) {
                if (!urlPart.isEmpty() && titleSlug == null) {
                    /* First non-empty segment = Root = Slug of element-title */
                    titleSlug = urlPart;
                    buildSubpathNow = true;
                } else if (buildSubpathNow) {
                    if (desiredSubpathDecoded == null) {
                        desiredSubpathDecoded = Encoding.htmlDecode(urlPart);
                    } else {
                        desiredSubpathDecoded += "/" + Encoding.htmlDecode(urlPart);
                    }
                }
            }
        } else {
            titleSlug = path;
        }
        if (StringUtils.isEmpty(titleSlug)) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        String xmlResponse = null;
        final String xmlurl = "https://archive.org/download/" + titleSlug + "/" + titleSlug + "_files.xml";
        final String cacheKey = xmlurl;
        final Object lock = requestLock(cacheKey);
        try {
            synchronized (lock) {
                LinkCrawler crawler = getCrawler();
                if (crawler != null) {
                    crawler = crawler.getRoot();
                }
                if (crawler != null) {
                    final Reference<String> reference = (Reference<String>) crawler.getCrawlerCache(cacheKey);
                    xmlResponse = reference != null ? reference.get() : null;
                }
                if (StringUtils.isEmpty(xmlResponse)) {
                    final int previousLoadLimit = xmlbr.getLoadLimit();
                    try {
                        xmlbr.setLoadLimit(Integer.MAX_VALUE);
                        xmlbr.getPage(xmlurl);
                        if (xmlbr.getHttpConnection().getResponseCode() == 404) {
                            /* Should be a super rare case. */
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        xmlResponse = xmlbr.getRequest().getHtmlCode();
                        if (crawler != null && StringUtils.isNotEmpty(xmlResponse)) {
                            crawler.putCrawlerCache(cacheKey, new SoftReference<String>(xmlResponse));
                        }
                    } finally {
                        xmlbr.setLoadLimit(previousLoadLimit);
                    }
                }
            }
        } finally {
            unLock(cacheKey);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArchiveOrgConfig cfg = PluginJsonConfig.get(ArchiveOrgConfig.class);
        final boolean crawlOriginalFilesOnly = cfg.isFileCrawlerCrawlOnlyOriginalVersions();
        final boolean crawlArchiveView = cfg.isFileCrawlerCrawlArchiveView();
        final boolean crawlMetadataFiles = cfg.isFileCrawlerCrawlMetadataFiles();
        final String[] items = new Regex(xmlResponse, "<file\\s*(.*?)\\s*</file>").getColumn(0);
        if (items == null || items.length == 0) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, path);
        }
        logger.info("Crawling all files below path: " + path);
        final String basePath = "https://archive.org/download/" + titleSlug;
        final List<String> skippedItems = new ArrayList<String>();
        for (final String item : items) {
            /* <old_version>true</old_version> */
            final boolean isOldVersion = item.contains("old_version");
            final boolean isOriginal = item.contains("source=\"original\"");
            final boolean isMetadata = item.contains("<format>Metadata</format>");
            final boolean isArchiveViewSupported = item.matches("(?i)(?s).*<format>\\s*(RAR|ZIP)\\s*</format>.*");
            final boolean isAccountRequiredForDownload = item.contains("<private>true</private>");
            String pathWithFilename = new Regex(item, "name=\"([^\"]+)").getMatch(0);
            final String filesizeBytesStr = new Regex(item, "<size>(\\d+)</size>").getMatch(0);
            final String sha1hash = new Regex(item, "<sha1>([a-f0-9]+)</sha1>").getMatch(0);
            final String lastModifiedTimestamp = new Regex(item, "<mtime>(\\d+)</mtime>").getMatch(0);
            // final String md5hash = new Regex(item, "<md5>([a-f0-9]+)</md5>").getMatch(0);
            // final String crc32hash = new Regex(item, "<crc32>([a-f0-9]+)</crc32>").getMatch(0);
            if (pathWithFilename == null) {
                /* This should never happen */
                continue;
            } else if (isOldVersion) {
                /* Always skip old version elements. */
                skippedItems.add(pathWithFilename);
                continue;
            } else if (isMetadata && !crawlMetadataFiles) {
                /* Only include metadata in downloads if wished by the user. */
                skippedItems.add(pathWithFilename);
                continue;
            } else if (crawlOriginalFilesOnly && !isOriginal) {
                /* Skip non-original content if user only wants original content. */
                skippedItems.add(pathWithFilename);
                continue;
            }
            if (Encoding.isHtmlEntityCoded(pathWithFilename)) {
                /* Will sometimes contain "&amp;" */
                pathWithFilename = Encoding.htmlOnlyDecode(pathWithFilename);
            }
            if (desiredSubpathDecoded != null && !pathWithFilename.startsWith(desiredSubpathDecoded)) {
                /** Skip elements which do not match the sub-path we're trying to find items in or single file desired by user. */
                skippedItems.add(pathWithFilename);
                continue;
            }
            String relativePathEncoded;
            String filename = null;
            /* Search filename and properly encode relative URL to file. */
            if (pathWithFilename.contains("/")) {
                final String[] urlParts = pathWithFilename.split("/");
                relativePathEncoded = "";
                int index = 0;
                for (final String urlPart : urlParts) {
                    final boolean isLastSegment = index == urlParts.length - 1;
                    relativePathEncoded += URLEncode.encodeURIComponent(urlPart);
                    if (isLastSegment) {
                        filename = urlPart;
                    } else {
                        relativePathEncoded += "/";
                    }
                    index++;
                }
            } else {
                relativePathEncoded = URLEncode.encodeURIComponent(pathWithFilename);
                filename = pathWithFilename;
            }
            final String url = basePath + "/" + relativePathEncoded;
            final DownloadLink dlitem = createDownloadlink(url);
            dlitem.setVerifiedFileSize(SizeFormatter.getSize(filesizeBytesStr));
            dlitem.setAvailable(true);
            ArchiveOrg.setFinalFilename(dlitem, filename);
            String thisPath = new Regex(url, "(?i)download/(.+)/[^/]+$").getMatch(0);
            if (Encoding.isUrlCoded(thisPath)) {
                thisPath = Encoding.htmlDecode(thisPath);
            }
            dlitem.setRelativeDownloadFolderPath(thisPath);
            if (isAccountRequiredForDownload) {
                dlitem.setProperty(ArchiveOrg.PROPERTY_IS_ACCOUNT_REQUIRED, true);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(thisPath);
            dlitem._setFilePackage(fp);
            if (sha1hash != null) {
                dlitem.setSha1Hash(sha1hash);
            }
            if (lastModifiedTimestamp != null) {
                dlitem.setProperty(ArchiveOrg.PROPERTY_TIMESTAMP_FROM_API_LAST_MODIFIED, Long.parseLong(lastModifiedTimestamp));
            }
            ret.add(dlitem);
            if (crawlArchiveView && isArchiveViewSupported) {
                final DownloadLink archiveViewURL = createDownloadlink(url + "/");
                ret.add(archiveViewURL);
            }
        }
        if (desiredSubpathDecoded != null && ret.isEmpty()) {
            /* Users' desired subfolder or file was not found -> Throw exception to provide feedback to user. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (skippedItems.size() > 0) {
            logger.info("Skipped items: " + skippedItems.size());
            logger.info(skippedItems.toString());
        }
        if (desiredSubpathDecoded != null && ret.size() == 1) {
            /**
             * Force auto package handling for single items e.g. if user only added a single file which is part of a huge folder. </br>
             * Reference: https://board.jdownloader.org/showthread.php?t=92666&page=2
             */
            final Regex typeDownload = new Regex(contenturl, PATTERN_DOWNLOAD);
            if (typeDownload.patternFind() && StringUtils.equalsIgnoreCase(ret.get(0).getPluginPatternMatcher(), contenturl)) {
                CrawledLink source = getCurrentLink().getSourceLink();
                boolean crawlerSource = false;
                while (source != null) {
                    if (canHandle(source.getURL())) {
                        crawlerSource = true;
                        break;
                    } else {
                        source = source.getSourceLink();
                    }
                }
                if (!crawlerSource) {
                    logger.info("remove filePackage from direct added download link:" + path);
                    ret.get(0)._setFilePackage(null);
                }
            }
        }
        return ret;
    }

    /**
     * Crawls json which can sometimes be found in html of such URLs: "/details/<identifier>" </br>
     * If a filename/path to track position mapping is given, this handling tries to find the playlist position of crawled items based on
     * the mapping.
     */
    @Deprecated
    private ArrayList<DownloadLink> crawlMetadataJson(final String json, final Map<String, DownloadLink> filepathToPlaylistItemMapping) throws Exception {
        final Map<String, Object> metamap_root = restoreFromString(json, TypeRef.MAP);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final List<Map<String, Object>> skippedItems = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> itemsWithDuplicatedTracknumbers = new ArrayList<Map<String, Object>>();
        final Map<String, Object> metadata = (Map<String, Object>) metamap_root.get("metadata");
        String title = (String) metadata.get("title");
        if (title == null) {
            /* Fallback */
            title = metadata.get("identifier").toString();
        }
        /* There is different servers to choose from e.g. see also fields "d1", "d2" and "workable_servers". */
        final String server = metamap_root.get("server").toString();
        final String dir = metamap_root.get("dir").toString();
        final List<Map<String, Object>> filemaps = (List<Map<String, Object>>) metamap_root.get("files");
        if (filemaps == null || filemaps.isEmpty()) {
            /* This should never happen. */
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, title);
        }
        final String description = (String) metadata.get("description");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        final HashSet<Integer> usedTrackPositions = new HashSet<Integer>();
        int playlistSize = filepathToPlaylistItemMapping != null ? filepathToPlaylistItemMapping.size() : null;
        for (final Map<String, Object> filemap : filemaps) {
            final String source = filemap.get("source").toString(); // "original" or "derivative"
            final String audioTrackPositionStr = (String) filemap.get("track");
            String filenameOrPath = (String) filemap.get("orig");
            final Object sizeO = filemap.get("size");
            if (StringUtils.isEmpty(filenameOrPath)) {
                filenameOrPath = (String) filemap.get("original");
                if (StringUtils.isEmpty(filenameOrPath)) {
                    filenameOrPath = (String) filemap.get("name");
                }
            }
            final DownloadLink playlistItem = filepathToPlaylistItemMapping != null ? filepathToPlaylistItemMapping.get(filenameOrPath) : null;
            int audioTrackPosition = -1;
            if (playlistItem != null) {
                /* Get track position from mapping. Trust this mapping more than "track" field in metadata. */
                audioTrackPosition = playlistItem.getIntegerProperty(ArchiveOrg.PROPERTY_PLAYLIST_POSITION, -1);
            } else if (audioTrackPositionStr != null) {
                /*
                 * Obtain position from metadata. This is not the position we want but we will also set it as a property, it could be useful
                 * too.
                 */
                audioTrackPosition = Integer.parseInt(audioTrackPositionStr);
            }
            if (filepathToPlaylistItemMapping != null && (playlistItem == null || !source.equalsIgnoreCase("original"))) {
                /* Skip non audio and non-original files if a filenameToTrackPositionMapping is available. */
                skippedItems.add(filemap);
                continue;
            } else if (filepathToPlaylistItemMapping != null && !usedTrackPositions.add(audioTrackPosition)) {
                /*
                 * Log if an item with the same position exists multiple times.
                 */
                logger.info("Found duplicated playlist item on position: " + audioTrackPosition + " | Track: " + audioTrackPosition);
                itemsWithDuplicatedTracknumbers.add(filemap);
            }
            final String directurl = "https://" + server + dir + "/" + URLEncode.encodeURIComponent(filenameOrPath);
            final DownloadLink file = new DownloadLink(hostPlugin, null, "archive.org", directurl, true);
            if (audioTrackPosition != -1) {
                /**
                 * Size of playlist must not be pre-given </br>
                 * Size of playlist = Highest track-number.
                 */
                if (audioTrackPosition > playlistSize) {
                    /* Determine size of playlist as it must not be pre-given. */
                    playlistSize = audioTrackPosition;
                }
                file.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_POSITION, audioTrackPosition);
            }
            file.setProperty(ArchiveOrg.PROPERTY_ARTIST, filemap.get("artist")); // Optional field
            file.setProperty(ArchiveOrg.PROPERTY_TITLE, filemap.get("title")); // Optional field
            if (sizeO != null) {
                if (sizeO instanceof Number) {
                    file.setVerifiedFileSize(((Number) sizeO).longValue());
                } else {
                    file.setVerifiedFileSize(Long.parseLong(sizeO.toString()));
                }
            }
            final String crc32 = (String) filemap.get("crc32");
            if (crc32 != null) {
                file.setHashInfo(HashInfo.parse(crc32));
            }
            final String md5 = (String) filemap.get("md5");
            if (md5 != null) {
                file.setMD5Hash(md5);
            }
            final String sha1 = (String) filemap.get("sha1");
            if (sha1 != null) {
                file.setSha1Hash(sha1);
            }
            file.setProperty(ArchiveOrg.PROPERTY_TIMESTAMP_FROM_API_LAST_MODIFIED, filemap.get("mtime"));
            if (playlistItem != null) {
                /* Inherit filename from playlistItem */
                file.setFinalFileName(playlistItem.getName());
                file.setProperty(ArchiveOrg.PROPERTY_FILETYPE, ArchiveOrg.FILETYPE_AUDIO);
            } else {
                /* Set new filename */
                final String filename = getFilenameFromPath(filenameOrPath);
                ArchiveOrg.setFinalFilename(file, filename);
            }
            file._setFilePackage(fp);
            file.setAvailable(true);
            ret.add(file);
        }
        /* Add some additional properties */
        for (final DownloadLink file : ret) {
            /* Playlist size can only be determined after first loop -> Set that property here. */
            if (playlistSize != -1) {
                file.setProperty(ArchiveOrg.PROPERTY_PLAYLIST_SIZE, playlistSize);
            }
        }
        if (skippedItems.size() > 0) {
            /*
             * The list of skipped items is usually very big so we do not want to log the skipped items, only the number of skipped items.
             */
            logger.info("Skipped items: " + skippedItems.size());
        }
        if (itemsWithDuplicatedTracknumbers.size() > 0) {
            logger.info("All detected items with duplicated tracknumbers: " + itemsWithDuplicatedTracknumbers.size() + " -> " + itemsWithDuplicatedTracknumbers);
        }
        return ret;
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        return hostPlugin.looksLikeDownloadableContent(urlConnection);
    }

    private static String getFilenameFromPath(final String filenameOrPath) {
        final String filename;
        if (filenameOrPath.contains("/")) {
            /* We have a path -> Extract filename */
            final String[] pathSegments = filenameOrPath.split("/");
            final String filenameOnly = pathSegments[pathSegments.length - 1];
            filename = filenameOnly;
        } else {
            /* We already have our filename and not a path. */
            filename = filenameOrPath;
        }
        return filename;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ArchiveOrgConfig.class;
    }

    public static String generateBookContentURL(final String bookID) {
        return "https://archive.org/details/" + bookID;
    }
}