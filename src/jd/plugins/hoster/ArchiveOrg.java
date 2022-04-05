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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.ArchiveOrgConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;

import jd.PluginWrapper;
import jd.http.Browser;
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
    private final boolean       FREE_RESUME                          = true;
    private final int           FREE_MAXCHUNKS                       = 0;
    private final int           FREE_MAXDOWNLOADS                    = 20;
    private final boolean       ACCOUNT_FREE_RESUME                  = true;
    private final int           ACCOUNT_FREE_MAXCHUNKS               = 0;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS            = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private static final String PROPERTY_DOWNLOAD_SERVERSIDE_BROKEN  = "download_serverside_broken";
    public static final String  PROPERTY_IS_BOOK_PREVIEW             = "is_book_preview";
    public static final String  PROPERTY_BOOK_LOANED_UNTIL_TIMESTAMP = "book_loaned_until_timestamp";
    private boolean             registered_only                      = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        // final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, null, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        registered_only = false;
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

    private boolean isBookPreview(final DownloadLink link) {
        return link.hasProperty(PROPERTY_IS_BOOK_PREVIEW);
    }

    private void connectionErrorhandling(final URLConnectionAdapter con, final DownloadLink link, final Account account) throws PluginException, IOException {
        if (con.getResponseCode() == 403 || StringUtils.containsIgnoreCase(con.getContentType(), "html")) {
            br.followConnection(true);
            /* <h1>Item not available</h1> */
            if (br.containsHTML("(?i)>\\s*Item not available<")) {
                if (br.containsHTML("(?i)>\\s*The item is not available due to issues")) {
                    registered_only = true;
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
            if (isBookPreview(link) && account != null) {
                /* 2021-11-18 */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Borrowed book downloading is not supported");
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else if (isBookPreview(link) && con.getURL().toString().contains("preview-unavailable.png")) {
            // https://archive.org/bookreader/static/preview-unavailable.png
            /* Page of a book which is only available when book is borrowed by user (paid content). */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Book preview unavailable");
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        if (registered_only) {
            throw new AccountRequiredException();
        }
        if (link.getPluginPatternMatcher().matches("(?i).+\\.(zip|rar)/.+")) {
            doDownload(null, link, false, 1, "free_directlink");
        } else {
            doDownload(null, link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
        }
    }

    private void doDownload(final Account account, final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), resumable, maxchunks);
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                /* 2021-08-09: Added this as alternative method e.g. for users that have registered on archive.org via Google login. */
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    if (this.checkCookies(this.br, account, userCookies)) {
                        /*
                         * User can entry anything into username field but we want unique strings --> Try to find "real username" in HTML
                         * code.
                         */
                        final String realUsername = br.getRegex("username=\"([^\"]+)\"").getMatch(0);
                        if (realUsername != null) {
                            account.setUser(realUsername);
                        } else {
                            logger.warning("Failed to find \"real\" username");
                        }
                        return;
                    } else {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException("Login cookies expired");
                        } else {
                            throw new AccountInvalidException("Login cookies invalid");
                        }
                    }
                }
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(account.getHoster(), cookies);
                    if (!force) {
                        /* We trust these cookies --> Do not check them */
                        logger.info("Trust login cookies without check");
                        return;
                    } else {
                        if (this.checkCookies(this.br, account, cookies)) {
                            account.saveCookies(br.getCookies(account.getHoster()), "");
                            return;
                        }
                    }
                }
                logger.info("Performing full login");
                if (!account.getUser().matches(".+@.+\\..+")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage("https://" + account.getHoster() + "/account/login");
                br.postPageRaw(br.getURL(), "remember=true&referer=https%3A%2F%2Farchive.org%2FCREATE%2F&login=true&submit_by_js=true&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedIN(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException ignore) {
                if (ignore.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw ignore;
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
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (link.getPluginPatternMatcher().matches("(?i).+\\.zip/.+")) {
            doDownload(account, link, false, 1, "account_free_directlink");
        } else {
            doDownload(account, link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        }
    }

    @Override
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        /* Sync this between hoster- and decrypter plugin! */
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