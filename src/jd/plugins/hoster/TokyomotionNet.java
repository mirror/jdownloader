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

import jd.PluginWrapper;
import jd.controlling.AccountController;
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

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tokyomotion.net" }, urls = { "https?://(?:www\\.)?tokyomotion\\.net/video/\\d+(?:/[^/]+)?" })
public class TokyomotionNet extends PluginForHost {
    public TokyomotionNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.tokyomotion.net/signup");
    }

    @Override
    public String getAGBLink() {
        return "http://www.tokyomotion.net/static/terms";
    }

    /* Connection stuff */
    private final boolean    RESUME           = true;
    private final int        MAXCHUNKS        = 0;
    private final int        MAXDOWNLOADS     = 20;
    private String           dllink           = null;
    private boolean          server_issues    = false;
    private boolean          isPrivateContent = false;
    public static final long trust_cookie_age = 300000l;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        server_issues = false;
        isPrivateContent = false;
        this.setBrowserExclusive();
        final String url_filename = new Regex(link.getDownloadURL(), "/([^/]+)$").getMatch(0);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(aa);
        }
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL().replace("http://", "https://"));
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML(">This is a private video")) {
            isPrivateContent = true;
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = url_filename;
        }
        dllink = br.getRegex("<source src=\"(https?://[^<>\"]*?)\"[^>]*?type=(?:\"|\\')video/(?:mp4|flv)(?:\"|\\')").getMatch(0);
        filename += ".mp4";
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            link.setFinalFileName(filename);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    /*
                     * Special: First URL is only accessible once but it redirects to the final URL which we can access multiple times which
                     * is why we need to get that!
                     */
                    this.dllink = con.getURL().toString();
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (isPrivateContent) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, RESUME, MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    private boolean isLoggedinHTML() {
        return br.containsHTML("/logout\"");
    }

    private void login(final Account account) throws Exception {
        synchronized (LOCK) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    br.getPage("https://www." + account.getHoster() + "/");
                    if (isLoggedinHTML()) {
                        /* Save cookies to save new timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    }
                    /* Clear cookies */
                }
                br.postPage("https://www." + account.getHoster() + "/login", "submit_login=&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedinHTML()) {
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
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* No need to login as we're already logged in in availablecheck */
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}