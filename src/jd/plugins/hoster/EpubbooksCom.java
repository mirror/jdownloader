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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "epubbooks.com" }, urls = { "httpss?://(?:www\\.)?epubbooks\\.com/downloads/\\d+" })
public class EpubbooksCom extends PluginForHost {
    public EpubbooksCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.epubbooks.com/sign_up");
    }

    @Override
    public String getAGBLink() {
        return "https://www.epubbooks.com/terms";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME                  = false;
    private final int     FREE_MAXCHUNKS               = 1;
    private final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean ACCOUNT_FREE_RESUME          = false;
    private final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        final String mainlink = link.getStringProperty("mainlink", null);
        if (mainlink != null) {
            br.getPage(mainlink);
            if (isOffline(this.br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            if (this.br.containsHTML("You have reached your \\d+\\-hour download limit")) {
                /* Usually 24-H downloadlimit. */
                /* 2017-01-23: Limit seems to be 4-10 files per day - there are currently no premium accounts at all. */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Downloadlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://www." + account.getHoster() + "/");
                    if (isLoggedinHtml(br)) {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                }
                br.getPage("https://www." + account.getHoster() + "/login");
                final String csrftoken = br.getRegex("name=\"csrf\\-token\" content=\"([^<>\"]+)\"").getMatch(0);
                if (csrftoken == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String postdata = "utf8=%E2%9C%93&authenticity_token=" + Encoding.urlEncode(csrftoken) + "&user%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&user%5Bremember_me%5D=0&user%5Bremember_me%5D=1&commit=Sign+in";
                br.postPage(br.getURL(), postdata);
                if (br.getCookie(account.getHoster(), "remember_user_token") == null || !isLoggedinHtml(br)) {
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

    public static boolean isLoggedinHtml(final Browser br) {
        return br.containsHTML("/logout");
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
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