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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imgs.aventertainments.com", "aventertainments.com" }, urls = { "https?://imgs\\.aventertainments\\.com/.+", "https?://www\\.aventertainments\\.com/newdlsample\\.aspx.+\\.mp4|https?://ppvclips\\d+\\.aventertainments\\.com/.+\\.m3u9" })
public class AventertainmentsCom extends PluginForHost {

    public AventertainmentsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.aventertainments.com/register.aspx?languageID=1&VODTypeID=1&Site=PPV");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https

    private final String         TYPE_IMAGE        = "https?://imgs\\.aventertainments\\.com/.+";
    private final String         TYPE_VIDEO_HTTP   = "https?://(?:www\\.)?aventertainments\\.com/newdlsample\\.aspx.*?\\.mp4";
    private final String         TYPE_VIDEO_HLS    = "https?://ppvclips\\d+\\.aventertainments\\.com/.+\\.m3u8";

    public static String         html_loggedin     = "aventertainments.com/logout\\.aspx";

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://www.aventertainments.com/aveterms.htm";
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String newurl = link.getDownloadURL().replace(".m3u9", ".m3u8");
        link.setUrlDownload(newurl);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String mainlink = link.getStringProperty("mainlink");
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String url_filename = new Regex(link.getDownloadURL(), "/([^/]+)$").getMatch(0);
        String filename = link.getFinalFileName();
        if (filename == null) {
            filename = url_filename;
        }
        if (link.getDownloadURL().matches(TYPE_VIDEO_HLS)) {
            dllink = link.getDownloadURL();
        } else {
            if (link.getDownloadURL().matches(TYPE_VIDEO_HTTP)) {
                br.setFollowRedirects(false);
                if (mainlink != null) {
                    /* Important!! */
                    this.br.getHeaders().put("Referer", mainlink);
                }
                this.br.getPage(link.getDownloadURL());
                dllink = this.br.getRedirectLocation();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                br.setFollowRedirects(true);
            } else {
                dllink = link.getDownloadURL();
            }
            dllink = Encoding.htmlDecode(dllink);
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = encodeUnicode(filename);
            final String ext = getFileNameExtensionFromString(dllink, link.getDownloadURL().matches(TYPE_IMAGE) ? ".mp4" : ".jpg");
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
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
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink.matches(TYPE_VIDEO_HLS)) {
            br.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            if (server_issues) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            } else if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    public static Browser prepBR(final Browser br) {
        br.setCookie("aventertainments.com", "IPCountry", "EN");
        br.setFollowRedirects(true);
        return br;
    }

    private static Object LOCK = new Object();

    public static void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://www.aventertainments.com/");
                    if (br.containsHTML(html_loggedin)) {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                    br = prepBR(new Browser());
                }
                br.getPage("https://www.aventertainments.com/login.aspx?languageID=1&VODTypeID=1&Site=PPV");
                final Form loginform = br.getFormbyKey("__EVENTTARGET");
                loginform.put("ctl00$ContentPlaceHolder1$uid", Encoding.urlEncode(account.getUser()));
                loginform.put("ctl00$ContentPlaceHolder1$passwd", Encoding.urlEncode(account.getPass()));
                loginform.put("ctl00$ContentPlaceHolder1$SavedLoginBox", "on");
                br.submitForm(loginform);
                if (!br.containsHTML(html_loggedin)) {
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
        account.setMaxSimultanDownloads(free_maxdownloads);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* No need to login - account is really only needed for decrypter. */
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return free_maxdownloads;
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
