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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.ArchiveOrgConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;

import jd.PluginWrapper;
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
    private final int           FREE_MAXDOWNLOADS                    = 20;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS            = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private static final String PROPERTY_DOWNLOAD_SERVERSIDE_BROKEN  = "download_serverside_broken";
    public static final String  PROPERTY_BOOK_ID                     = "book_id";
    public static final String  PROPERTY_IS_LENDING_REQUIRED         = "is_lending_required";
    public static final String  PROPERTY_BOOK_LOANED_UNTIL_TIMESTAMP = "book_loaned_until_timestamp";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        // final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, null, false);
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
            URLConnectionAdapter con = null;
            try {
                /* 2021-02-25: Do NOT use head connection here anymore! */
                prepDownloadHeaders(br, link);
                con = br.openGetConnection(link.getPluginPatternMatcher());
                connectionErrorhandling(con, link, account);
                if (this.looksLikeDownloadableContent(con)) {
                    link.setFinalFileName(getFileNameFromHeader(con));
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return AvailableStatus.TRUE;
                } else {
                    /* Most likely file is offline */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return AvailableStatus.UNCHECKABLE;
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
            /* Legacy */
            return true;
        } else {
            return false;
        }
    }

    private void connectionErrorhandling(final URLConnectionAdapter con, final DownloadLink link, final Account account) throws PluginException, IOException {
        if (this.isBook(link)) {
            /* Check errors for books */
            if (con.getResponseCode() == 403) {
                if (isBookLendingRequired(link)) {
                    /* TODO: auto re-lend book */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 403: Lending of this book has expired");
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 403: You tried to download borrowed a book page without account");
                }
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 404: You tried to download borrowed a book page without account");
            }
            if (con.getURL().toString().contains("preview-unavailable.png")) {
                // https://archive.org/bookreader/static/preview-unavailable.png
                /* This page of a book is only available when book is borrowed by user. */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Book preview unavailable");
            }
        }
        if (StringUtils.containsIgnoreCase(con.getContentType(), "html")) {
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
        requestFileInformation(link, null, true);
        doDownload(null, link);
    }

    private void doDownload(final Account account, final DownloadLink link) throws Exception, PluginException {
        if (account != null) {
            this.login(account, true);
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            if (this.isBook(link) && account != null) {
                final String bookID = link.getStringProperty(PROPERTY_BOOK_ID);
                if (bookID == null) {
                    /* Old/broken items */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                // this.borrowBook(br, account, bookID);
            }
            prepDownloadHeaders(br, link);
            if (this.isBook(link)) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher() + "&rotate=0&scale=0", isResumeable(link, account), getMaxChunks(link, account));
            } else {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), isResumeable(link, account), getMaxChunks(link, account));
            }
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), isResumeable(link, account), getMaxChunks(link, account));
        }
        connectionErrorhandling(br.getHttpConnection(), link, account);
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
        if (link.getPluginPatternMatcher().matches("(?i).+\\.(zip|rar)/.+")) {
            return false;
        } else {
            return true;
        }
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        if (link.getPluginPatternMatcher().matches("(?i).+\\.(zip|rar)/.+")) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                /* 2021-08-09: Added this as alternative method e.g. for users that have registered on archive.org via Google login. */
                final Cookies userCookies = account.loadUserCookies();
                final Cookies borrowCookies = account.loadCookies("borrow");
                if (borrowCookies != null && !borrowCookies.isEmpty()) {
                    br.setCookies(borrowCookies);
                }
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

    /** Borrows given bookID which gives us a token we can use to download all pages of that book. */
    public void borrowBook(final Browser br, final Account account, final String bookID) throws Exception {
        synchronized (account) {
            if (bookID == null) {
                /* Developer mistake */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setAllowedResponseCodes(400);
            final UrlQuery query = new UrlQuery();
            query.add("action", "grant_access");
            query.add("identifier", Encoding.urlEncode(bookID));
            br.postPage("https://" + this.getHost() + "/services/loans/loan/searchInside.php", query);
            query.addAndReplace("action", "browse_book");
            br.postPage("/services/loans/loan/", query);
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            if (br.getHttpConnection().getResponseCode() == 400) {
                final String error = (String) entries.get("error");
                if (StringUtils.equalsIgnoreCase(error, "This book is not available to borrow at this time. Please try again later.")) {
                    logger.info("Borrow not needed");
                    return;
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Book borrow failure");
                }
            }
            query.addAndReplace("action", "create_token");
            /* This should set a cookie called "br-load-<bookID>" */
            br.postPage("/services/loans/loan/", query);
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final String borrowToken = (String) entries.get("token");
            if (StringUtils.isEmpty(borrowToken)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Book borrow failure #2");
            }
            logger.info("Successfully borrowed book: " + bookID);
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
            } else {
                account.saveCookies(borrowCookies, "borrow");
            }
        }
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
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        doDownload(account, link);
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
        return ACCOUNT_FREE_MAXDOWNLOADS;
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