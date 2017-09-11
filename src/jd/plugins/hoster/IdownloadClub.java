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

import java.util.Locale;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "idownload.club" }, urls = { "https?://(?:www\\.)?idownload\\.club/members/ext/get_file\\.php\\?file=.+" })
public class IdownloadClub extends antiDDoSForHost {

    public IdownloadClub(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.idownload.club/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.idownload.club/terms-of-service.html";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME                  = false;
    private final int     FREE_MAXCHUNKS               = 1;
    private final int     FREE_MAXDOWNLOADS            = -1;
    private final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    private String        dllink                       = null;
    private boolean       server_issues                = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        server_issues = false;
        this.setBrowserExclusive();
        dllink = link.getDownloadURL();

        final String filename_url = new Regex(link.getDownloadURL(), "\\?file=([^\\&=\\?]+)").getMatch(0);
        if (filename_url != null) {
            link.setName(filename_url);
        }

        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(aa, true);
        } else {
            logger.info("Cannot check links of this host without account!");
            return AvailableStatus.UNCHECKABLE;
        }

        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(dllink));
            if (!con.getContentType().contains("html")) {
                link.setFinalFileName(getFileNameFromHeader(con));
                link.setDownloadSize(con.getLongContentLength());
            } else {
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                server_issues = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        /* Premiumonly */
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force && cookies.get("PHPSESSID") != null) {
                    br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                getPage("https://www." + getHost());
                postPage("/members/index.php", "hidLogin=1&chkKeepLogged=on&txtEmail=" + Encoding.urlEncode(account.getUser()) + "&txtPassword=" + Encoding.urlEncode(account.getPass()));
                final String txtPassword_hashed = br.getRegex("txtPassword=\"\\+encodeURIComponent\\('([^<>\"']+)'\\)").getMatch(0);
                if (br.getCookie(this.getHost(), "PHPSESSID") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                getPage("/members/ext/index.php?page=VIDEO");
                if (!br.containsHTML("\\?logout=logout")) {
                    /* Double-check */
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        // (10/Sep/2017&nbsp;-&nbsp;10/Oct/2017)
        final String expire = br.getRegex("\\(\\d+/[A-Za-z]+/\\d{4}&nbsp;-&nbsp;(\\d+/[A-Za-z]+/\\d{4})\\)").getMatch(0);
        String days_left = br.getRegex(";\\s*(\\d+)\\s*Days Left").getMatch(0);
        long expireIn = days_left == null ? -1 : (System.currentTimeMillis() + Integer.parseInt(days_left) * (1000l * 60 * 60 * 24));
        if (expire != null && days_left == null) {
            days_left = "notnull";
            expireIn = TimeFormatter.getMilliSeconds(expire, "dd/MMM/yyyy", Locale.ENGLISH);
        }
        if (days_left == null || expireIn - System.currentTimeMillis() < 60 * 1000l) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setValidUntil(expireIn);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}