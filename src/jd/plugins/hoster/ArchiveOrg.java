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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.archiveorg.ArchiveOrgConfig;
import org.jdownloader.plugins.components.archiveorg.ArchiveOrgConfig.PlaylistFilenameScheme;
import org.jdownloader.plugins.components.archiveorg.ArchiveOrgLendingInfo;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.ArchiveOrgCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "archive.org" }, urls = { "https?://(?:[\\w\\.]+)?archive\\.org/download/[^/]+/[^/]+(/.+)?" })
public class ArchiveOrg extends PluginForHost {
    public ArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://archive.org/account/login.createaccount.php");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    @Override
    public String getAGBLink() {
        return "https://archive.org/about/terms.php";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        if (this.isBook(link)) {
            return this.getHost() + "://" + this.getBookID(link) + "/" + this.getBookSubPrefix(link) + "/" + this.getBookPageIndexNumber(link);
        } else {
            return super.getLinkID(link);
        }
    }

    /* Connection stuff */
    private final int                                     MAXDOWNLOADS                                    = -1;
    private final String                                  PROPERTY_DOWNLOAD_SERVERSIDE_BROKEN             = "download_serverside_broken";
    public static final String                            PROPERTY_BOOK_ID                                = "book_id";
    public static final String                            PROPERTY_BOOK_SUB_PREFIX                        = "book_sub_prefix";
    /* Book page by number by archive.org. Can satart at 0 or 1. Do not use as a real page index! */
    public static final String                            PROPERTY_BOOK_PAGE                              = "book_page";
    /* Real page index */
    public static final String                            PROPERTY_BOOK_PAGE_INTERNAL_INDEX               = "book_page_internal_index";
    /* For officially downloadable files */
    public static final String                            PROPERTY_IS_ACCOUNT_REQUIRED                    = "is_account_required";
    /* For book page downloads */
    public static final String                            PROPERTY_IS_LENDING_REQUIRED                    = "is_lending_required";
    public static final String                            PROPERTY_IS_FREE_DOWNLOADABLE_BOOK_PREVIEW_PAGE = "is_free_downloadable_book_preview_page";
    public static final String                            PROPERTY_IS_BORROWED_UNTIL_TIMESTAMP            = "is_borrowed_until_timestamp";
    public static final String                            PROPERTY_PLAYLIST_POSITION                      = "position";
    public static final String                            PROPERTY_PLAYLIST_SIZE                          = "playlist_size";
    public static final String                            PROPERTY_TITLE                                  = "title";
    public static final String                            PROPERTY_ARTIST                                 = "artist";
    private final String                                  PROPERTY_ACCOUNT_TIMESTAMP_BORROW_LIMIT_REACHED = "timestamp_borrow_limit_reached";
    private static HashMap<String, ArchiveOrgLendingInfo> bookBorrowSessions                              = new HashMap<String, ArchiveOrgLendingInfo>();

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (link.getPluginPatternMatcher().endsWith("my_dir")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        if (account != null) {
            login(account, false);
        }
        br.setFollowRedirects(true);
        if (!isDownload) {
            if (this.requiresAccount(link) && account == null) {
                return AvailableStatus.UNCHECKABLE;
            } else if (this.isBookLendingRequired(link)) {
                /* Do not lend books during availablecheck */
                return AvailableStatus.UNCHECKABLE;
            }
            URLConnectionAdapter con = null;
            try {
                /* 2021-02-25: Do not use HEAD request anymore! */
                prepDownloadHeaders(br, link);
                con = br.openGetConnection(getDirectURL(link, account));
                connectionErrorhandling(con, link, account, null);
                String filenameFromHeader = getFileNameFromHeader(con);
                if (filenameFromHeader != null) {
                    filenameFromHeader = Encoding.htmlDecode(filenameFromHeader);
                    setFinalFilename(link, filenameFromHeader);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                return AvailableStatus.TRUE;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return AvailableStatus.UNCHECKABLE;
    }

    /**
     * Use this whenever you wnt to set a filename especially if there is a chance that the item is part of an audio playlist or a video
     * streaming item.
     */
    public static void setFinalFilename(final DownloadLink link, final String originalFilename) {
        if (StringUtils.isEmpty(originalFilename)) {
            return;
        }
        final int playlistPosition = link.getIntegerProperty(PROPERTY_PLAYLIST_POSITION, -1);
        final String fileExtension = Files.getExtension(originalFilename);
        final ExtensionsFilterInterface fileType = CompiledFiletypeFilter.getExtensionsFilterInterface(fileExtension);
        final boolean isAudio = CompiledFiletypeFilter.AudioExtensions.MP3.isSameExtensionGroup(fileType);
        // final boolean isVideo = CompiledFiletypeFilter.VideoExtensions.MP4.isSameExtensionGroup(fileType);
        if (playlistPosition != -1) {
            final int playlistSize = link.getIntegerProperty(PROPERTY_PLAYLIST_SIZE, -1);
            final int padLength = StringUtils.getPadLength(playlistSize);
            final String positionFormatted = StringUtils.formatByPadLength(padLength, playlistPosition);
            if (isAudio) {
                /* File is part of audio playlist. Format: <positionFormatted>.<rawFilename> */
                String title = link.getStringProperty(PROPERTY_TITLE);
                if (title == null) {
                    /* Title is not always given. Use filename without extension as title */
                    title = originalFilename.substring(0, originalFilename.lastIndexOf("."));
                }
                if (PluginJsonConfig.get(ArchiveOrgConfig.class).getPlaylistFilenameScheme() == PlaylistFilenameScheme.PLAYLIST_TITLE_WITH_TRACK_NUMBER && title != null) {
                    final String artist = link.getStringProperty(PROPERTY_ARTIST);
                    String playlistFilename = positionFormatted + "." + title;
                    if (artist != null) {
                        playlistFilename += " - " + artist;
                    }
                    playlistFilename += "." + fileExtension;
                    link.setFinalFileName(playlistFilename);
                } else {
                    link.setFinalFileName(originalFilename);
                }
            } else {
                /* Video streaming file. Format: <rawFilenameWithoutExt>_<positionFormatted>.mp4 */
                final String filenameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf("."));
                link.setFinalFileName(filenameWithoutExt + "_" + positionFormatted + ".mp4");
            }
        } else {
            link.setFinalFileName(originalFilename);
        }
    }

    private boolean isFreeDownloadableBookPreviewPage(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_FREE_DOWNLOADABLE_BOOK_PREVIEW_PAGE)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isBook(final DownloadLink link) {
        if (link.hasProperty("is_book")) {
            /* Legacy */
            return true;
        } else if (link.hasProperty(PROPERTY_BOOK_ID)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isBookLendingRequired(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_LENDING_REQUIRED)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if this book page is borrowed at this moment. </br>
     * This information is only useful with the combination of the borrow-cookies and can become invalid at any point of time if e.g. the
     * user returns the book manually via browser.
     */
    private boolean isLendAtThisMoment(final DownloadLink link) {
        final long borrowedUntilTimestamp = link.getLongProperty(PROPERTY_IS_BORROWED_UNTIL_TIMESTAMP, -1);
        if (borrowedUntilTimestamp > System.currentTimeMillis()) {
            return true;
        } else {
            return false;
        }
    }

    private int getBookPageIndexNumber(final DownloadLink link) {
        final int internalBookPageIndex = link.getIntegerProperty(PROPERTY_BOOK_PAGE_INTERNAL_INDEX, -1);
        if (internalBookPageIndex != -1) {
            return internalBookPageIndex;
        } else {
            /* All of this is legacy. TODO: Remove in 01-2023 */
            final int archiveOrgBookPageNumber = link.getIntegerProperty(PROPERTY_BOOK_PAGE, -1);
            if (archiveOrgBookPageNumber != -1) {
                return archiveOrgBookPageNumber;
            } else {
                /* Legacy handling for older items */
                final String pageStr = new Regex(link.getContentUrl(), ".*/page/n?(\\d+)").getMatch(0);
                if (pageStr != null) {
                    return Integer.parseInt(pageStr) - 1;
                } else {
                    /* Fallback: This should never happen */
                    return 1;
                }
            }
        }
    }

    /**
     * A special string that is the same as the bookID but different for multi volume books. </br>
     * ...thus only relevant for multi volume books.
     */
    private String getBookSubPrefix(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_BOOK_SUB_PREFIX);
    }

    private boolean requiresAccount(final DownloadLink link) {
        if (this.isBookLendingRequired(link) && !this.isFreeDownloadableBookPreviewPage(link)) {
            return true;
        } else {
            return false;
        }
    }

    private String getBookID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_BOOK_ID);
    }

    private void connectionErrorhandling(final URLConnectionAdapter con, final DownloadLink link, final Account account, final ArchiveOrgLendingInfo oldLendingInfo) throws Exception {
        if (this.isBook(link)) {
            /* Check errors for books */
            if (!this.looksLikeDownloadableContent(con, link)) {
                final int responsecode = con.getResponseCode();
                if (account != null && isBookLendingRequired(link) && (responsecode == 403 || responsecode == 404)) {
                    synchronized (bookBorrowSessions) {
                        final ArchiveOrgLendingInfo lendingInfo = getLendingInfo(this.getBookID(link), account);
                        if (oldLendingInfo != lendingInfo) {
                            /* Info has been modified in the meanwhile --> Retry */
                            throw new PluginException(LinkStatus.ERROR_RETRY, "Retry after auto re-loan of other download candidate");
                        }
                        /* Protection against re-loaning the same book over and over again in a short period of time. */
                        final Long timeUntilNextLoanAllowed = lendingInfo != null ? lendingInfo.getTimeUntilNextLoanAllowed() : null;
                        if (timeUntilNextLoanAllowed != null) {
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wait until this book can be auto re-loaned", timeUntilNextLoanAllowed.longValue());
                        } else {
                            this.borrowBook(br, account, this.getBookID(link), false);
                            throw new PluginException(LinkStatus.ERROR_RETRY, "Retry after auto re-loan of current download candidate");
                        }
                    }
                } else {
                    /* Unknown reason of failure */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 3 * 60 * 1000l);
                }
            } else if (con.getURL().toString().contains("preview-unavailable.png")) {
                // https://archive.org/bookreader/static/preview-unavailable.png
                /* This page of a book is only available when book is borrowed by user. */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Book preview unavailable");
            }
        }
        /* Generic errorhandling */
        if (!this.looksLikeDownloadableContent(con, link)) {
            br.followConnection(true);
            /* <h1>Item not available</h1> */
            if (br.containsHTML("(?i)>\\s*Item not available<")) {
                if (br.containsHTML("(?i)>\\s*The item is not available due to issues")) {
                    /* First check for this flag */
                    if (link.getBooleanProperty(PROPERTY_DOWNLOAD_SERVERSIDE_BROKEN, false)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The item is not available due to issues with the item's content");
                    } else if (account != null) {
                        /* Error happened while we're logged in -> Dead end --> Also set this flag to ensure that */
                        link.setProperty(PROPERTY_DOWNLOAD_SERVERSIDE_BROKEN, true);
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The item is not available due to issues with the item's content");
                    } else {
                        throw new AccountRequiredException();
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403: Item not available");
                }
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(null, link);
    }

    private void handleDownload(final Account account, final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        ArchiveOrgLendingInfo lendingInfoForBeforeDownload = null;
        if (account != null) {
            this.login(account, false);
            lendingInfoForBeforeDownload = this.getLendingInfo(link, account);
        } else {
            if (this.requiresAccount(link)) {
                throw new AccountRequiredException();
            }
        }
        cleanupBorrowSessionMap();
        prepDownloadHeaders(br, link);
        final String directurl = getDirectURL(link, account);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, isResumeable(link, account), getMaxChunks(link, account));
        connectionErrorhandling(br.getHttpConnection(), link, account, lendingInfoForBeforeDownload);
        if (!this.looksLikeDownloadableContent(dl.getConnection(), link)) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        if (dl.startDownload()) {
            /* Return books once all pages of a book have been downloaded. */
            try {
                synchronized (bookBorrowSessions) {
                    /* lendingInfo could have changed in the meantime */
                    final ArchiveOrgLendingInfo lendingInfoForAfterDownload = this.getLendingInfo(link, account);
                    if (lendingInfoForAfterDownload != null) {
                        lendingInfoForAfterDownload.setBookPageDownloadStatus(this.getBookPageIndexNumber(link), true);
                        if (lendingInfoForAfterDownload.looksLikeBookDownloadIsComplete()) {
                            final String bookID = this.getBookID(link);
                            try {
                                logger.info("Returning book " + bookID);
                                final UrlQuery query = new UrlQuery();
                                query.add("action", "return_loan");
                                query.add("identifier", bookID);
                                br.postPage("https://" + this.getHost() + "/services/loans/loan", query);
                                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                                if (Boolean.TRUE.equals(entries.get("success"))) {
                                    logger.info("Successfully returned book " + bookID);
                                } else {
                                    logger.info("Failed to return book " + bookID + " json response: " + br.getRequest().getHtmlCode());
                                }
                            } catch (final Throwable wtf) {
                                logger.log(wtf);
                                logger.warning("Failed to return book: Exception happened");
                            } finally {
                                /* Remove from cache */
                                bookBorrowSessions.remove(getLendingInfoKey(bookID, account));
                            }
                        }
                    }
                }
            } catch (final Exception ignore) {
                logger.log(ignore);
            }
        }
    }

    /** Removes expired entries from bookBorrowSessions. */
    private void cleanupBorrowSessionMap() {
        synchronized (bookBorrowSessions) {
            final Iterator<Entry<String, ArchiveOrgLendingInfo>> iterator = bookBorrowSessions.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, ArchiveOrgLendingInfo> entry = iterator.next();
                final ArchiveOrgLendingInfo lendingInfo = entry.getValue();
                if (!lendingInfo.isValid() || lendingInfo.looksLikeBookDownloadIsComplete()) {
                    iterator.remove();
                }
            }
        }
    }

    private String getDirectURL(final DownloadLink link, final Account account) throws Exception {
        if (this.isBook(link)) {
            String directurl = null;
            if (this.isFreeDownloadableBookPreviewPage(link)) {
                /* Directurl can be downloaded without the need of an account and special cookies. */
                directurl = link.getPluginPatternMatcher();
            } else {
                /* Account + cookies require and the book has to be borrowed to download this page. */
                if (account == null) {
                    throw new AccountRequiredException();
                }
                final String bookID = this.getBookID(link);
                ArchiveOrgLendingInfo lendingInfo = this.getLendingInfo(bookID, account);
                if (lendingInfo != null) {
                    directurl = lendingInfo.getPageURL(this.getBookPageIndexNumber(link));
                }
                if (lendingInfo != null && directurl != null) {
                    /* Use existing session */
                    br.setCookies(lendingInfo.getCookies());
                } else {
                    /* Create new session */
                    this.borrowBook(br, account, bookID, false);
                    lendingInfo = this.getLendingInfo(bookID, account);
                    directurl = lendingInfo.getPageURL(this.getBookPageIndexNumber(link));
                    if (StringUtils.isEmpty(directurl)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            final ArchiveOrgConfig cfg = PluginJsonConfig.get(ArchiveOrgConfig.class);
            final UrlQuery query = UrlQuery.parse(directurl);
            query.add("rotate", "0");
            /* This one defines the image quality. This may only work for borrowed books but we'll append it to all book URLs regardless. */
            query.add("scale", Integer.toString(cfg.getBookImageQuality()));
            /* Get url without parameters */
            String newURL = URLHelper.getUrlWithoutParams(directurl);
            /* Append our new query */
            newURL += "?" + query.toString();
            return newURL;
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    private void prepDownloadHeaders(final Browser br, final DownloadLink link) {
        if (this.isBook(link)) {
            br.getHeaders().put("Referer", "https://" + this.getHost() + "/");
            br.getHeaders().put("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
            br.getHeaders().put("Sec-Fetch-Site", "name-site");
            br.getHeaders().put("Sec-Fetch-Mode", "no-cors");
            br.getHeaders().put("Sec-Fetch-Dest", "image");
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (this.isBook(link)) {
            return false;
        } else if (link.getPluginPatternMatcher().matches("(?i).+\\.(zip|rar)/.+")) {
            return false;
        } else {
            return true;
        }
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        if (this.isBook(link)) {
            return 1;
        } else if (link.getPluginPatternMatcher().matches("(?i).+\\.(zip|rar)/.+")) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                /* 2021-08-09: Added this as alternative method e.g. for users that have registered on archive.org via Google login. */
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    if (!force) {
                        /* Do not check cookies */
                        br.setCookies(account.getHoster(), userCookies);
                        return;
                    } else if (this.checkCookies(this.br, account, userCookies)) {
                        /*
                         * User can entry anything into username field when using cookie login but we want unique strings --> Try to find
                         * "real username" in HTML code.
                         */
                        final String realUsername = br.getRegex("username=\"([^\"]+)\"").getMatch(0);
                        if (realUsername == null) {
                            logger.warning("Failed to find \"real\" username");
                        } else if (!StringUtils.equals(realUsername, account.getUser())) {
                            account.setUser(realUsername);
                        }
                        return;
                    } else {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(account.getHoster(), cookies);
                    if (!force) {
                        /* Do not check cookies */
                        return;
                    } else {
                        if (this.checkCookies(this.br, account, cookies)) {
                            account.saveCookies(br.getCookies(br.getHost()), "");
                            return;
                        }
                    }
                }
                logger.info("Performing full login");
                if (!account.getUser().matches(".+@.+\\..+")) {
                    throw new AccountInvalidException("Please enter your e-mail address in the username field!");
                }
                br.getPage("https://" + this.getHost() + "/account/login");
                br.postPageRaw(br.getURL(), "remember=true&referer=https%3A%2F%2Farchive.org%2FCREATE%2F&login=true&submit_by_js=true&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedIN(br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException ignore) {
                if (ignore.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw ignore;
            }
        }
    }

    /**
     * Borrows given bookID which gives us a token we can use to download all pages of that book. </br>
     * It is typically valid for one hour.
     */
    private void borrowBook(final Browser br, final Account account, final String bookID, final boolean skipAllExceptLastStep) throws Exception {
        if (account == null) {
            /* Account is required to borrow books. */
            throw new AccountRequiredException();
        } else if (bookID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Check for reached limit so we don't waste more http requests when we know that this limit has been reached. */
        final long timestampBorrowLimitReached = account.getLongProperty(PROPERTY_ACCOUNT_TIMESTAMP_BORROW_LIMIT_REACHED, 0);
        final long timePassedSinceLimitReached = Time.systemIndependentCurrentJVMTimeMillis() - timestampBorrowLimitReached;
        if (timePassedSinceLimitReached < 1 * 60 * 60 * 1000l) {
            this.exceptionReachedAccountBorrowLimit();
        }
        synchronized (bookBorrowSessions) {
            final UrlQuery query = new UrlQuery();
            query.add("identifier", Encoding.urlEncode(bookID));
            Map<String, Object> entries = null;
            final String urlBase = "https://" + this.getHost();
            br.setAllowedResponseCodes(400);
            if (!skipAllExceptLastStep) {
                query.add("action", "grant_access");
                br.postPage(urlBase + "/services/loans/loan/searchInside.php", query);
                entries = restoreFromString(br.toString(), TypeRef.MAP);
                query.addAndReplace("action", "browse_book");
                br.postPage("/services/loans/loan/", query);
                entries = restoreFromString(br.toString(), TypeRef.MAP);
                if (br.getHttpConnection().getResponseCode() == 400) {
                    final String error = (String) entries.get("error");
                    if (StringUtils.equalsIgnoreCase(error, "This book is not available to borrow at this time. Please try again later.")) {
                        /**
                         * Happens if you try to borrow a book that can't be borrowed or if you try to borrow a book while too many
                         * (2022-08-31: max 10) books per hour have already been borrowed with the current account. </br>
                         * With setting this timestamp we can ensure not to waste more http requests on trying to borrow books but simply
                         * set error status on all future links [for the next 60 minutes].
                         */
                        account.setProperty(PROPERTY_ACCOUNT_TIMESTAMP_BORROW_LIMIT_REACHED, Time.systemIndependentCurrentJVMTimeMillis());
                        /*
                         * Remove session of book associated with current book page [if present] so that there will be no further download
                         * attempts and all pages will run into this error directly.
                         */
                        bookBorrowSessions.remove(getLendingInfoKey(bookID, account));
                        exceptionReachedAccountBorrowLimit();
                    } else {
                        // throw new PluginException(LinkStatus.ERROR_FATAL, "Book borrow failure: " + error);
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Book borrow failure: " + error);
                    }
                }
            }
            /* This should set a cookie called "br-load-<bookID>" */
            query.addAndReplace("action", "create_token");
            br.postPage(urlBase + "/services/loans/loan/", query);
            entries = restoreFromString(br.toString(), TypeRef.MAP);
            final String borrowToken = (String) entries.get("token");
            if (StringUtils.isEmpty(borrowToken)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Book borrow failure #2");
            }
            if (skipAllExceptLastStep) {
                logger.info("Successfully checked borrow status of book: " + bookID);
            } else {
                logger.info("Successfully borrowed book: " + bookID);
            }
            // account.saveCookies(br.getCookies(br.getHost()), "");
            final Cookies borrowCookies = new Cookies();
            for (final Cookie cookie : br.getCookies(br.getHost()).getCookies()) {
                /* Collect borrow cookies and save them separately */
                if (cookie.getKey().matches("(br|loan)-.*")) {
                    borrowCookies.add(cookie);
                }
            }
            if (borrowCookies.isEmpty()) {
                /* This should never happen */
                logger.warning("WTF book was borrowed but no borrow-cookies are present!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final ArchiveOrgCrawler crawler = (ArchiveOrgCrawler) this.getNewPluginForDecryptInstance(this.getHost());
            final String bookURL = ArchiveOrgCrawler.generateBookContentURL(bookID);
            br.getPage(bookURL);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final ArrayList<String> pageURLs = new ArrayList<String>();
            final ArrayList<DownloadLink> results = crawler.crawlBook(br, new CryptedLink(bookURL), account);
            for (final DownloadLink result : results) {
                if (!this.isLendAtThisMoment(result)) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Book lending failure: Loaned book is still not viewable");
                }
                pageURLs.add(result.getPluginPatternMatcher());
            }
            /* Keep track of download progress even if book needs to be lend again in the middle of downloading! */
            final ArchiveOrgLendingInfo existingLendingInfo = this.getLendingInfo(bookID, account);
            if (existingLendingInfo != null) {
                logger.info("Updated existing ArchiveOrgLendingInfo");
                /* Update timestamp so we know when book was borrowed last time */
                existingLendingInfo.updateTimestamp();
                /* Set new borrow-cookies */
                existingLendingInfo.setCookies(borrowCookies);
                /* Update page URLKs although they should not have changed. */
                existingLendingInfo.updateOrAddBookPages(pageURLs);
            } else {
                logger.info("Added new ArchiveOrgLendingInfo");
                final ArchiveOrgLendingInfo newLendingInfo = new ArchiveOrgLendingInfo(borrowCookies);
                newLendingInfo.setPageURLs(pageURLs);
                bookBorrowSessions.put(getLendingInfoKey(bookID, account), newLendingInfo);
            }
        }
    }

    private void exceptionReachedAccountBorrowLimit() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Reached max borrow limit with this account: Refresh account and stop/start downloads to retry immediately", 5 * 60 * 1000l);
    }

    public ArchiveOrgLendingInfo getLendingInfo(final DownloadLink link, final Account account) {
        return getLendingInfo(this.getBookID(link), account);
    }

    /** Returns LendingInfo/session for given bookID + acccount. */
    public ArchiveOrgLendingInfo getLendingInfo(final String bookID, final Account account) {
        if (account == null || bookID == null) {
            return null;
        }
        final String key = getLendingInfoKey(bookID, account);
        synchronized (bookBorrowSessions) {
            final ArchiveOrgLendingInfo ret = bookBorrowSessions.get(key);
            return ret;
        }
    }

    private static String getLendingInfoKey(final String bookID, final Account account) {
        return account.getUser() + "_" + bookID;
    }

    /** Checks if given cookies grant us access to given account. */
    private boolean checkCookies(final Browser br, final Account account, final Cookies cookies) throws IOException {
        br.setCookies(account.getHoster(), cookies);
        br.getPage("https://" + this.getHost() + "/account/");
        if (this.isLoggedIN(br)) {
            logger.info("Cookie login successful");
            return true;
        } else {
            logger.info("Cookie login failed");
            return false;
        }
    }

    public boolean isLoggedIN(final Browser br) {
        return br.getCookie(br.getHost(), "logged-in-sig", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        /* This host does not provide any kind of paid accounts. */
        account.setType(AccountType.FREE);
        cleanupBorrowSessionMap();
        account.removeProperty(PROPERTY_ACCOUNT_TIMESTAMP_BORROW_LIMIT_REACHED);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(account, link);
    }

    @Override
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter con) {
        return looksLikeDownloadableContent(con, false);
    }

    public boolean looksLikeDownloadableContent(final URLConnectionAdapter con, final DownloadLink link) {
        if (this.isBook(link)) {
            return looksLikeDownloadableContent(con, false);
        } else {
            return looksLikeDownloadableContent(con, true);
        }
    }

    public boolean looksLikeDownloadableContent(final URLConnectionAdapter con, final boolean isOfficialDownloadurl) {
        if (super.looksLikeDownloadableContent(con)) {
            return true;
        } else {
            if (con.getResponseCode() == 200 || con.getResponseCode() == 206) {
                if (isOfficialDownloadurl) {
                    /**
                     * It's an official downloadurl but they're not necessarily sending a Content-Disposition header so checks down below
                     * could e.g. fail for .html files. </br>
                     */
                    return true;
                } else if (StringUtils.containsIgnoreCase(con.getURL().getPath(), ".xml")) {
                    /* 2021-02-15: Special handling for .xml files */
                    return StringUtils.containsIgnoreCase(con.getContentType(), "xml");
                } else if (con.getURL().getPath().matches("(?i).*\\.(txt|log)$")) {
                    /* 2021-05-03: Special handling for .txt files */
                    return StringUtils.containsIgnoreCase(con.getContentType(), "text/plain");
                } else if (StringUtils.containsIgnoreCase(con.getURL().getPath(), ".html")) {
                    /* 2023-02-13: Special handling for .html files */
                    return StringUtils.containsIgnoreCase(con.getContentType(), "html") || StringUtils.containsIgnoreCase(con.getContentType(), "text/plain");
                } else {
                    /* MimeType file-extension and extension at the end of the URL are the same -> Also accept as downloadable content. */
                    final String extension = getExtensionFromMimeType(con.getContentType());
                    if (StringUtils.endsWithCaseInsensitive(con.getURL().getPath(), "." + extension)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null && link.getBooleanProperty(PROPERTY_IS_ACCOUNT_REQUIRED, false) == true) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            /*
             * Remove this property otherwise there is the possibility that the user gets a permanent error for certain files while they
             * might just be temporarily unavailable (this should never happen...)!
             */
            link.removeProperty(PROPERTY_DOWNLOAD_SERVERSIDE_BROKEN);
            /* If this is a book page: Reset downloaded-flag for book page to prevent returning the book too early. */
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            if (account != null) {
                synchronized (bookBorrowSessions) {
                    final ArchiveOrgLendingInfo lendingInfo = this.getLendingInfo(link, account);
                    if (lendingInfo != null) {
                        lendingInfo.setBookPageDownloadStatus(this.getBookPageIndexNumber(link), false);
                    }
                }
            }
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ArchiveOrgConfig.class;
    }
}