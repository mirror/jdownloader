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

import java.util.ArrayList;
import java.util.LinkedHashMap;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "udemy.com" }, urls = { "https?://(?:www\\.)?udemy\\.com/(.+\\?dtcode=[A-Za-z0-9]+|.+/#/lecture/\\d+)" }, flags = { 2 })
public class UdemyCom extends PluginForHost {

    public UdemyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.udemy.com/courses/");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension   = ".mp4";

    /* Connection stuff */
    private static final boolean FREE_RESUME         = true;
    private static final int     FREE_MAXCHUNKS      = 0;
    private static final int     FREE_MAXDOWNLOADS   = 20;

    private String               DLLINK              = null;

    private static final String  TYPE_FREE           = "https?://(?:www\\.)?udemy\\.com/.+\\?dtcode=[A-Za-z0-9]+";
    private static final String  TYPE_ACCOUNT_NEEDED = "https?://(?:www\\.)?udemy\\.com/.+/#/lecture/\\d+";

    @Override
    public String getAGBLink() {
        return "https://www.udemy.com/terms/";
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        DLLINK = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = null;
        String url_embed = null;
        boolean loggedin = false;
        final String fid_accountneeded = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                this.login(aa, false);
                loggedin = true;
            } catch (final Throwable e) {
            }
        }
        if (!loggedin && downloadLink.getDownloadURL().matches(TYPE_ACCOUNT_NEEDED)) {
            downloadLink.setName(fid_accountneeded);
            downloadLink.getLinkStatus().setStatusText("Cannot check this url without account");
            return AvailableStatus.TRUE;
        } else if (downloadLink.getDownloadURL().matches(TYPE_ACCOUNT_NEEDED)) {
            /* Prepare the API-Headers to get the videourl */
            downloadLink.setName(fid_accountneeded);
            final String clientid = this.br.getCookie(MAINPAGE, "client_id");
            final String bearertoken = this.br.getCookie(MAINPAGE, "access_token");
            final String newrelicid = "XAcEWV5ADAEDUlhaDw==";
            if (clientid == null || bearertoken == null || newrelicid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage(downloadLink.getDownloadURL());
            final String courseid = this.br.getRegex("data-course-id=\"(\\d+)\"").getMatch(0);
            if (courseid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getHeaders().put("X-NewRelic-ID", newrelicid);
            // this.br.getHeaders().put("X-Udemy-Client-Id", clientid);
            this.br.getHeaders().put("Authorization", "Bearer " + bearertoken);
            this.br.getHeaders().put("X-Udemy-Authorization", "Bearer " + bearertoken);
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.br.getPage("https://www.udemy.com/api-2.0/users/me/subscribed-courses/" + courseid + "/lectures/" + fid_accountneeded + "?video_only=&auto_play=&fields%5Blecture%5D=asset%2Cembed_url&fields%5Basset%5D=asset_type%2Cdownload_urls%2Ctitle&instructorPreviewMode=False");
            // this.br.getPage("https://www.udemy.com/api-2.0/lectures/" + fid_accountneeded +
            // "/content?videoOnly=0&instructorPreviewMode=False");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(this.br.toString());
            entries = (LinkedHashMap<String, Object>) entries.get("asset");
            filename = (String) entries.get("title");
            final ArrayList<Object> ressourcelist = (ArrayList) DummyScriptEnginePlugin.walkJson(entries, "download_urls/Video");
            DLLINK = (String) DummyScriptEnginePlugin.walkJson(ressourcelist.get(ressourcelist.size() - 1), "file");
            if (DLLINK != null) {
                if (filename == null) {
                    filename = this.br.getRegex("response\\-content\\-disposition=attachment%3Bfilename=([^<>\"/\\\\]*)(mp4)?\\.mp4").getMatch(0);
                    if (filename == null) {
                        filename = fid_accountneeded;
                    } else {
                        filename = fid_accountneeded + "_" + filename;
                    }
                }
            }
        } else {
            br.getPage(downloadLink.getDownloadURL());
            if (br.getURL().contains("/search/") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Normal (FREE) url */
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                filename = new Regex(downloadLink.getDownloadURL(), "udemy\\.com/(.+)\\?dtcode=").getMatch(0);
            }
            url_embed = this.br.getRegex("(https?://(?:www\\.)?udemy\\.com/embed/video/[^<>\"]*?)\"").getMatch(0);
            if (url_embed == null || filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage(url_embed);
            DLLINK = br.getRegex("\"file\":\"(http[^<>\"]*?)\",\"label\":\"720p").getMatch(0);
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openHeadConnection(DLLINK);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", DLLINK);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(TYPE_ACCOUNT_NEEDED)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        handleDownload(downloadLink);
    }

    public void handleDownload(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, FREE_RESUME, FREE_MAXCHUNKS);
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

    private static final String MAINPAGE = "https://udemy.com";
    private static Object       LOCK     = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                br.getPage("https://www.udemy.com/join/login-popup/?displayType=ajax&display_type=popup&showSkipButton=1&returnUrlAfterLogin=https%3A%2F%2Fwww.udemy.com%2F&next=https%3A%2F%2Fwww.udemy.com%2F&locale=de_DE");
                final String csrftoken = this.br.getCookie(MAINPAGE, "csrftoken");
                if (csrftoken == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final String postData = "csrfmiddlewaretoken=" + csrftoken + "&locale=de_DE&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&displayType=ajax";
                br.postPage("https://www.udemy.com/join/login-popup/?displayType=ajax&display_type=popup&showSkipButton=1&returnUrlAfterLogin=https%3A%2F%2Fwww.udemy.com%2F&next=https%3A%2F%2Fwww.udemy.com%2F&locale=de_DE", postData);
                if (this.br.containsHTML("data-purpose=\"do-login\"")) {
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
        account.setConcurrentUsePossible(true);
        account.setType(AccountType.PREMIUM);
        /* There is no separate free/premium - users can buy videos which will be available for their accounts only afterwards. */
        ai.setStatus("Valid account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* No need to log in - we're already logged in! */
        handleDownload(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
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
