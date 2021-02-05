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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "archive.org" }, urls = { "https?://(?:www\\.)?archivedecrypted\\.org/download/[^/]+/[^/]+(/.+)?" })
public class ArchiveOrg extends PluginForHost {
    public ArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://archive.org/account/login.createaccount.php");
    }

    @Override
    public String getAGBLink() {
        return "https://archive.org/about/terms.php";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String oldURL = link.getPluginPatternMatcher();
        final String newURL = oldURL.replace("archivedecrypted.org/", "archive.org/");
        link.setContentUrl(newURL);
        link.setPluginPatternMatcher(newURL);
    }

    /* Connection stuff */
    private final boolean FREE_RESUME               = true;
    private final int     FREE_MAXCHUNKS            = 0;
    private final int     FREE_MAXDOWNLOADS         = 20;
    private final boolean ACCOUNT_FREE_RESUME       = true;
    private final int     ACCOUNT_FREE_MAXCHUNKS    = 0;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private boolean       registered_only           = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        registered_only = false;
        if (link.getPluginPatternMatcher().endsWith("my_dir")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        this.setBrowserExclusive();
        if (!isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(link.getPluginPatternMatcher());
                connectionErrorhandling(br.getHttpConnection(), link);
                if (this.looksLikeDownloadableContent(con)) {
                    link.setFinalFileName(getFileNameFromHeader(con));
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return AvailableStatus.TRUE;
                } else if (con.getResponseCode() == 200) { // txt/xml, size not available
                    link.setFinalFileName(getFileNameFromHeader(con));
                    return AvailableStatus.TRUE;
                } else {
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

    private void connectionErrorhandling(final URLConnectionAdapter con, final DownloadLink link) throws PluginException, IOException {
        if (con.getResponseCode() == 403 || StringUtils.containsIgnoreCase(con.getContentType(), "html")) {
            con.disconnect();
            br.getPage(link.getPluginPatternMatcher());
            if (br.containsHTML(">Item not available<")) {
                if (br.containsHTML("The item is not available due to issues")) {
                    registered_only = true;
                    throw new AccountRequiredException();
                }
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        if (registered_only) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (link.getPluginPatternMatcher().matches("(?i).+\\.zip/.+")) {
            doDownload(null, link, false, 1, "free_directlink");
        } else {
            doDownload(null, link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
        }
    }

    private void doDownload(final Account account, final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            connectionErrorhandling(br.getHttpConnection(), link);
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
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(account.getHoster(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !force) {
                        /* We trust these cookies --> Do not check them */
                        logger.info("Trust login cookies as they're not yet that old");
                        return;
                    }
                    br.getPage("https://archive.org/account/");
                    if (this.isLoggedIN(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://" + account.getHoster() + "/account/login.php");
                br.postPage("/account/login.php", "remember=CHECKED&referer=https%3A%2F%2F" + account.getHoster() + "%2F&action=login&submit=Log+in&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedIN(br)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    public boolean isLoggedIN(final Browser br) {
        return br.getCookie(br.getHost(), "logged-in-sig", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, true);
        login(account, false);
        if (link.getPluginPatternMatcher().matches("(?i).+\\.zip/.+")) {
            doDownload(account, link, false, 1, "account_free_directlink");
        } else {
            doDownload(account, link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
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
    public void resetDownloadlink(DownloadLink link) {
    }
}