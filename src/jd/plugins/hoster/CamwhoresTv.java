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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "camwhores.tv" }, urls = { "https?://(?:www\\.)?camwhoresdecrypted\\.tv/.+|https?://(?:www\\.)?camwhores\\.tv/embed/\\d+" })
public class CamwhoresTv extends PluginForHost {
    public CamwhoresTv(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.camwhores.tv/");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String default_Extension            = ".mp4";
    /* Connection stuff */
    private final boolean       FREE_RESUME                  = true;
    private final int           FREE_MAXCHUNKS               = 0;
    private final int           FREE_MAXDOWNLOADS            = 20;
    private final boolean       ACCOUNT_FREE_RESUME          = true;
    private final int           ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int           ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private final int           ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private String              dllink                       = null;
    private boolean             server_issues                = false;
    private boolean             is_private_video             = false;

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("camwhoresdecrypted.tv/", "camwhores.video/"));
        final String id = new Regex(link.getDownloadURL(), "/(?:videos|embed)/(\\d+)").getMatch(0);
        link.setLinkID(getHost() + "://" + id);
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        if (link != null) {
            final String id = new Regex(link.getDownloadURL(), "/(?:videos|embed)/(\\d+)").getMatch(0);
            if (id != null) {
                return getHost() + "://" + id;
            }
        }
        return super.getMirrorID(link);
    }

    @Override
    public String getAGBLink() {
        return "http://www.camwhores.tv/terms/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "kt_tcookie", "1");
        br.setCookie(this.getHost(), "kt_is_visited", "1");
        br.getPage(link.getDownloadURL());
        if (isOffline(this.br)) {
            /* 2017-01-21: For now, we do not support private videos --> Offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        is_private_video = this.br.containsHTML("This video is a private");
        String filename = getTitle(this.br, link.getDownloadURL());
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = default_Extension;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (is_private_video && br.containsHTML("login-required")) {
            link.setName(filename);
            return AvailableStatus.TRUE;
        }
        getDllink(link);
        if (dllink != null) {
            link.setFinalFileName(filename);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setProperty("directlink", dllink);
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    private void getDllink(final DownloadLink link) throws Exception {
        dllink = jd.plugins.hoster.KernelVideoSharingCom.getDllink(br, this);
        if (dllink != null && dllink.contains("login-required")) {
            dllink = null;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doDownload(null, downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doDownload(final Account account, final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (is_private_video && account == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void login(final Account account, final boolean force, final boolean test) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (test) {
                        br.getPage("http://www." + this.getHost() + "/");
                        final String kt_member = br.getCookie(this.getHost(), "kt_member");
                        if (kt_member != null && !StringUtils.equalsIgnoreCase(kt_member, "deleted")) {
                            account.saveCookies(this.br.getCookies(this.getHost()), "");
                            return;
                        }
                    } else {
                        return;
                    }
                }
                br.clearCookies(getHost());
                br.getPage("http://www." + this.getHost() + "/login/");
                /*
                 * 2017-01-21: This request will usually return a json with some information about the account. Until now there are no premium accounts
                 * available at all.
                 */
                br.postPage("/login/", "remember_me=1&action=login&email_link=http%3A%2F%2Fwww.camwhores.tv%2Femail%2F&format=json&mode=async&username=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                final String kt_member = br.getCookie(this.getHost(), "kt_member");
                if (kt_member == null || StringUtils.equalsIgnoreCase(kt_member, "deleted")) {
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
        login(account, false, true);
        /* Registered users can watch private videos when they follow/subscribe to the uploaders. */
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* free accounts can still have captcha */
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(false);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false, false);
        requestFileInformation(link);
        final String kt_member = br.getCookie(this.getHost(), "kt_member");
        if (kt_member == null || StringUtils.equalsIgnoreCase(kt_member, "deleted")) {
            login(account, false, true);
            requestFileInformation(link);
        }
        getDllink(link);
        doDownload(account, link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    }

    private boolean isLoggedInHtml() {
        return this.br.containsHTML("/logout/");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    public static String getTitle(final Browser br, final String url) {
        String title = br.getRegex("<title>([^<>\"]*?)( / Embed Player)?</title>").getMatch(0);
        if (title == null) {
            title = new Regex(url, "/videos/\\d+/(.+)").getMatch(0);
        }
        return title;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* 2017-01-21: For now, we do not support private videos --> Offline */
            return true;
        } else {
            return false;
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
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
