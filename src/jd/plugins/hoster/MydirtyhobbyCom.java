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

import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
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
import jd.plugins.PluginForHost;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mydirtyhobby.com" }, urls = { "https?://(?:www\\.)?mydirtyhobby\\.com/profil/\\d+[A-Za-z0-9\\-]+/videos/\\d+[A-Za-z0-9\\-]+" }, flags = { 2 })
public class MydirtyhobbyCom extends PluginForHost {

    public MydirtyhobbyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.mydirtyhobby.com/");
    }

    @Override
    public String getAGBLink() {
        return "http://cdn1.e5.mdhcdn.com/u/TermsofUse_de.pdf";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private final String         html_buy                     = "name=\"buy\"";
    private final String         html_logout                  = "(/\\?ac=dologout|/logout\")";
    private final String         default_extension            = ".flv";

    private String               dllink                       = null;
    private boolean              premiumonly                  = false;
    private boolean              serverissues                 = false;

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        premiumonly = false;
        serverissues = false;
        this.br = prepBR(new Browser());
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            this.login(aa, false);
        }
        br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String username = this.br.getRegex("<dt>Hinzugefügt von</dt>[\t\n\r ]*?<dd class=\"right\">[\t\n\r ]*?<a href=\"/profil/[^\"]+\" title=\"([^<>\"]+)\"").getMatch(0);
        if (username == null) {
            username = "amateur";
        }
        String filename = br.getRegex("<h\\d+ class=\"page\\-title pull\\-left\">([^<>\"]+)</h\\d+>").getMatch(0);
        if (filename == null) {
            /* Fallback to url-filename */
            filename = new Regex(link.getDownloadURL(), "/videos/\\d+([A-Za-z0-9\\-]+)$").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        filename = username + " - " + filename;
        if (br.containsHTML(html_buy) || aa == null) {
            /* User has an account but he did not buy this video or user does not even have an account --> No way to download it */
            link.setName(filename + default_extension);
            premiumonly = true;
            return AvailableStatus.TRUE;
        }
        dllink = this.br.getRegex("data\\-(?:flv|mp4)=\"(https?://[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) {
            dllink = this.br.getRegex("\"(https?://[^<>\"\\']+\\.flv[^<>\"\\']+)\"").getMatch(0);
        }
        if (dllink != null) {
            /* Fix final downloadlink */
            dllink = dllink.replace("%252525", "%25");
            /* Set final filename */
            if (dllink.contains(".flv")) {
                filename += ".flv";
            } else {
                filename += ".mp4";
            }
            link.setFinalFileName(filename);
            /* Get- and set filesize */
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br.openHeadConnection(dllink);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    serverissues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* Final filename not given */
            link.setName(filename + default_extension);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://mydirtyhobby.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("deprecation")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                /* Re-use cookies whenever possible - avoid login captcha! */
                if (cookies != null) {
                    this.br.setCookies(MAINPAGE, cookies);
                    this.br.getPage(MAINPAGE);
                    if (this.br.containsHTML(html_logout)) {
                        account.saveCookies(this.br.getCookies(MAINPAGE), "");
                        return;
                    }
                    /* Full login needed */
                    this.br = prepBR(new Browser());
                }
                br.getPage("http://www.mydirtyhobby.com/n/login");
                /*
                 * In case we need a captcha it will only appear after the first login attempt so we need (max) 2 attempts to ensure that
                 * user can enter the captcha if needed.
                 */
                for (int i = 0; i <= 1; i++) {
                    String postdata = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                    if (this.br.containsHTML("class=\"g\\-recaptcha\"")) {
                        if (this.getDownloadLink() == null) {
                            // login wont contain downloadlink
                            this.setDownloadLink(new DownloadLink(this, "Account Login!", this.getHost(), this.getHost(), true));
                        }
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, this.br).getToken();
                        postdata += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                    }
                    br.postPage("/n/login", postdata);
                    if (!this.br.containsHTML(html_logout) && this.br.containsHTML("class=\"g\\-recaptcha\"")) {
                        continue;
                    }
                    break;
                }
                if (!this.br.containsHTML(html_logout)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(MAINPAGE), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        return br;
    }

    /** There are no free- or premium accounts. Users can only watch the videos they bought. */
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
        maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(maxPrem.get());
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (premiumonly) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (serverissues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}