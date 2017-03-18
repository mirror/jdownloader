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
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "anime.thehylia.com" }, urls = { "https?://anime\\.thehyliadecrypted\\.com/\\d+" })
public class AnimeTheHyliaCom extends PluginForHost {

    public AnimeTheHyliaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://anime.thehylia.com/forums/register.php");
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    private String               DLLINK            = null;

    @Override
    public String getAGBLink() {
        return "http://anime.thehylia.com/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        // if taken from the decrypter then property "referer" is set to the main page of the "series"
        // this is redirected url from the link, so we should set it the same way as the decrypter would
        // if it is link not from decrypter
        final String decryptedlink = downloadLink.getStringProperty("directlink", null);
        if (downloadLink.getStringProperty("referer") == null) {
            br.getPage(decryptedlink);
            if (br.getRedirectLocation() != null) {
                downloadLink.setProperty("referer", br.getRedirectLocation());
            }
        }
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(aa, false);
            } catch (final Throwable e) {
            }
        }
        final String filename = downloadLink.getStringProperty("decryptedfilename", null);
        downloadLink.setFinalFileName(filename);
        br.getHeaders().put("Referer", downloadLink.getStringProperty("referer", null));
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getStringProperty("directlink", null));
        if (br.containsHTML(">Unfortunately, due to large server expenses we are not able to accomodate lots of consecutive")) {
            downloadLink.getLinkStatus().setStatusText("Can't check filesize when servers are overloaded");
            return AvailableStatus.TRUE;
        }
        if (decryptedlink.contains("/soundtracks/")) {
            DLLINK = br.getRegex("\"(http://[^<>\"]*?\\.mp3)\">Download").getMatch(0);
        } else {
            if (br.getURL().contains("anime.thehylia.com/downloads/series/")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(">No such episode")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DLLINK = br.getRedirectLocation();
        }
        if (DLLINK == null) {
            if (br.containsHTML("issues|network capacity|problems")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Server reports errors with this file");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">Unfortunately, due to large server expenses we are not able to accomodate lots of consecutive")) {
            /* This should NEVER happen via account! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server overloaded", 10 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://anime.thehylia.com";
    private static Object       LOCK     = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(false);
                br.getPage("https://anime.thehylia.com/forums/index.php");
                final String pwhash = JDHash.getMD5(account.getPass());
                final Form loginform = this.br.getFormbyKey("cookieuser");
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform.put("vb_login_username", account.getUser());
                loginform.put("vb_login_password", "");
                loginform.put("vb_login_md5password", pwhash);
                loginform.put("vb_login_md5password_utf", pwhash);
                loginform.remove("cookieuser");
                loginform.put("cookieuser", "1");
                this.br.submitForm(loginform);
                if (br.getCookie(MAINPAGE, "dcpassword") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* free accounts can still have captcha */
        account.setMaxSimultanDownloads(FREE_MAXDOWNLOADS);
        account.setConcurrentUsePossible(false);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
