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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.archiveorg.ArchiveOrgConfig;
import org.jdownloader.plugins.components.archiveorg.ArchiveOrgLendingInfo;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "archive.org" }, urls = { "https?://(?:[\\w\\.]+)?archive\\.org/download/[^/]+/[^/]+(/.+)?" })
public class ArchiveOrg extends PluginForHost {
    public ArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://archive.org/account/login.createaccount.php");
    }

    @Override
    public String getAGBLink() {
        return "https://archive.org/about/terms.php";
    }

    /* Connection stuff */
    private final int                                     MAXDOWNLOADS                                  = -1;
    private static final String                           PROPERTY_DOWNLOAD_SERVERSIDE_BROKEN           = "download_serverside_broken";
    public static final String                            PROPERTY_BOOK_ID                              = "book_id";
    public static final String                            PROPERTY_BOOK_SUB_PREFIX                      = "book_sub_prefix";
    public static final String                            PROPERTY_IS_LENDING_REQUIRED                  = "is_lending_required";
    public static final String                            PROPERTY_IS_UN_DOWNLOADABLE_BOOK_PREVIEW_PAGE = "is_un_downloadable_book_preview_page";
    private static HashMap<String, ArchiveOrgLendingInfo> bookBorrowSessions                            = new HashMap<String, ArchiveOrgLendingInfo>();

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (link.getPluginPatternMatcher().endsWith("my_dir")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isUnDownloadableBookPreviewPage(link)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        if (account != null) {
            login(account, false);
        }
        br.setFollowRedirects(true);
        if (!isDownload) {
            if (account == null && this.requiresAccount(link)) {
                return AvailableStatus.UNCHECKABLE;
            }
            URLConnectionAdapter con = null;
            try {
                /* 2021-02-25: Do not use HEAD request anymore! */
                prepDownloadHeaders(br, link);
                con = br.openGetConnection(getDirectURL(link));
                connectionErrorhandling(con, link, account, null);
                final String filenameFromHeader = getFileNameFromHeader(con);
                if (filenameFromHeader != null) {
                    link.setFinalFileName(filenameFromHeader);
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

    private boolean isUnDownloadableBookPreviewPage(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_UN_DOWNLOADABLE_BOOK_PREVIEW_PAGE)) {
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

    private boolean requiresAccount(final DownloadLink link) {
        if (this.isBookLendingRequired(link)) {
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
            if (!this.looksLikeDownloadableContent(con)) {
                final int responsecode = con.getResponseCode();
                if (account != null && isBookLendingRequired(link) && (responsecode == 403 || responsecode == 404)) {
                    synchronized (bookBorrowSessions) {
                        final ArchiveOrgLendingInfo lendingInfo = getLendingInfo(this.getBookID(link), account);
                        if (oldLendingInfo != lendingInfo) {
                            /* Info has been modified in the meanwhile --> Retry */
                            throw new PluginException(LinkStatus.ERROR_RETRY, "Retry after auto re-loan of other download candidate");
                        }
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
        if (!this.looksLikeDownloadableContent(con)) {
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
        ArchiveOrgLendingInfo lendingInfo = null;
        if (account != null) {
            this.login(account, false);
            if (this.isBook(link)) {
                if (this.isBookLendingRequired(link)) {
                    final String bookID = getBookID(link);
                    if (bookID == null) {
                        /* Old/broken items */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    lendingInfo = this.getLendingInfo(this.getBookID(link), account);
                    if (lendingInfo != null) {
                        br.setCookies(lendingInfo.getCookies());
                    } else {
                        /**
                         * Try download anyways as our errorhandling will borrow the book which is then downloadable for us. </br>
                         * User could e.g. have added account via cookie login so borrow session for some books may be available regardless.
                         */
                        logger.info("Borrow required but no borrow session available -> We will most likely run into errorhandling soon");
                    }
                }
            }
        } else {
            if (this.requiresAccount(link)) {
                throw new AccountRequiredException();
            }
        }
        cleanupBorrowSessionMap();
        prepDownloadHeaders(br, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, getDirectURL(link), isResumeable(link, account), getMaxChunks(link, account));
        connectionErrorhandling(br.getHttpConnection(), link, account, lendingInfo);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        dl.startDownload();
    }

    /** Removes expired entries from bookBorrowSessions. */
    private void cleanupBorrowSessionMap() {
        synchronized (bookBorrowSessions) {
            final Iterator<Entry<String, ArchiveOrgLendingInfo>> iterator = bookBorrowSessions.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, ArchiveOrgLendingInfo> entry = iterator.next();
                final ArchiveOrgLendingInfo lendingInfo = entry.getValue();
                if (!lendingInfo.isValid()) {
                    iterator.remove();
                }
            }
        }
    }

    private String getDirectURL(final DownloadLink link) throws MalformedURLException {
        if (this.isBook(link)) {
            final ArchiveOrgConfig cfg = PluginJsonConfig.get(ArchiveOrgConfig.class);
            final URL uri = new URL(link.getPluginPatternMatcher());
            final UrlQuery query = UrlQuery.parse(uri.getQuery());
            query.add("rotate", "0");
            /* This one defines the image quality. This may only work for borrowed books but we'll append it to all book URLs regardless. */
            query.add("scale", Integer.toString(cfg.getBookImageQuality()));
            /* Get url without query */
            String url = URLHelper.getURL(uri, false, true, true).toString();
            /* Append our new query */
            url += "?" + query.toString();
            return url;
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    private void prepDownloadHeaders(final Browser br, final DownloadLink link) {
        if (this.isBook(link)) {
            br.getHeaders().put("Referer", "https://archive.org/");
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
                         * User can entry anything into username field but we want unique strings --> Try to find "real username" in HTML
                         * code.
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
    public void borrowBook(final Browser br, final Account account, final String bookID, final boolean skipAllExceptLastStep) throws Exception {
        if (account == null) {
            /* Account is required to borrow books. */
            throw new AccountRequiredException();
        } else if (bookID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                query.addAndReplace("action", "browse_book");
                br.postPage("/services/loans/loan/", query);
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                if (br.getHttpConnection().getResponseCode() == 400) {
                    /*
                     * Happens if you try to borrow a book that can't be borrowed or if you try to borrow a book while too many (2022-08-31:
                     * max 10) books per hour have already been borrowed with the current account.
                     */
                    final String error = (String) entries.get("error");
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Book borrow failure: " + error);
                }
            }
            /* This should set a cookie called "br-load-<bookID>" */
            query.addAndReplace("action", "create_token");
            br.postPage(urlBase + "/services/loans/loan/", query);
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
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
                logger.warning("WTF book was borrowed but no borrow-cookies are present!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            bookBorrowSessions.put(getLendingInfoKey(bookID, account), new ArchiveOrgLendingInfo(borrowCookies));
        }
    }

    /** Returns LendingInfo/session for given bookID + acccount. */
    public ArchiveOrgLendingInfo getLendingInfo(final String bookID, final Account account) {
        final String key = getLendingInfoKey(bookID, account);
        synchronized (bookBorrowSessions) {
            final ArchiveOrgLendingInfo ret = bookBorrowSessions.get(key);
            return ret;
        }
    }

    private static String getLendingInfoKey(final String bookID, final Account account) {
        return account.getUser() + "_" + bookID;
    }

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
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(account, link);
    }

    @Override
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        final boolean ret = super.looksLikeDownloadableContent(urlConnection);
        if (ret) {
            return true;
        } else if (StringUtils.containsIgnoreCase(urlConnection.getURL().getPath(), ".xml")) {
            /* 2021-02-15: Special handling for .xml files */
            return StringUtils.containsIgnoreCase(urlConnection.getContentType(), "xml");
        } else if (urlConnection.getURL().getPath().matches("(?i).*\\.(txt|log)$")) {
            /* 2021-05-03: Special handling for .txt files */
            return StringUtils.containsIgnoreCase(urlConnection.getContentType(), "text/plain");
        } else {
            /* MimeType file-extension and extension at the end of the URL are the same -> Also accept as downloadable content. */
            final String extension = getExtensionFromMimeType(urlConnection.getContentType());
            return extension != null && StringUtils.endsWithCaseInsensitive(urlConnection.getURL().getPath(), "." + extension);
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
    public void resetDownloadlink(final DownloadLink link) {
        /*
         * Remove this property otherwise there is the possibility that the user gets a permanent error for certain files while they might
         * just be temporarily unavailable (this should never happen...)!
         */
        link.removeProperty(PROPERTY_DOWNLOAD_SERVERSIDE_BROKEN);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ArchiveOrgConfig.class;
    }
}