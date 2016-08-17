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

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "udemy.com" }, urls = { "https?://(?:www\\.)?udemydecrypted\\.com/(.+\\?dtcode=[A-Za-z0-9]+|.+/[^<>\"]+/lecture/\\d+)" }) 
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
    private static final String  default_Extension    = ".mp4";

    /* Connection stuff */
    private static final boolean FREE_RESUME          = true;
    private static final int     FREE_MAXCHUNKS       = 0;
    private static final int     FREE_MAXDOWNLOADS    = 20;

    private String               dllink               = null;

    private static final String  TYPE_SINGLE_FREE_OLD = "https?://(?:www\\.)?udemy\\.com/.+\\?dtcode=[A-Za-z0-9]+";
    public static final String   TYPE_SINGLE_PREMIUM  = ".+/lecture/\\d+$";

    @Override
    public String getAGBLink() {
        return "https://www.udemy.com/terms/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("udemydecrypted.com/", "udemy.com/"));
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = null;
        String url_embed = null;
        boolean loggedin = false;
        String description = null;
        final String fid_accountneeded = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(this.br, aa, false);
                loggedin = true;
            } catch (final Throwable e) {
            }
        }
        String ext = null;
        if (!loggedin && downloadLink.getDownloadURL().matches(TYPE_SINGLE_PREMIUM)) {
            downloadLink.setName(fid_accountneeded);
            downloadLink.getLinkStatus().setStatusText("Cannot check this url without account");
            return AvailableStatus.TRUE;
        } else if (downloadLink.getDownloadURL().matches(TYPE_SINGLE_PREMIUM)) {
            /* Prepare the API-Headers to get the videourl */
            if (!downloadLink.isNameSet()) {
                downloadLink.setName(fid_accountneeded);
            }
            this.br.getPage(downloadLink.getDownloadURL());
            final String courseid = getCourseID(this.br);
            if (courseid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            prepBRAPI(this.br);
            /*
             * 2016-04-08: Changed parameters - parameters before:
             * ?video_only=&auto_play=&fields%5Blecture%5D=asset%2Cembed_url&fields%5Basset
             * %5D=asset_type%2Cdownload_urls%2Ctitle&instructorPreviewMode=False
             */
            this.br.getPage("https://www.udemy.com/api-2.0/users/me/subscribed-courses/" + courseid + "/lectures/" + fid_accountneeded + "?fields%5Basset%5D=@min,download_urls,external_url,slide_urls&fields%5Bcourse%5D=id,is_paid,url&fields%5Blecture%5D=@default,view_html,course&page_config=ct_v4");
            // this.br.getPage("https://www.udemy.com/api-2.0/lectures/" + fid_accountneeded +
            // "/content?videoOnly=0&instructorPreviewMode=False");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            final String title_cleaned = (String) entries.get("title_cleaned");
            description = (String) entries.get("description");
            String json_view_html = (String) entries.get("view_html");
            entries = (LinkedHashMap<String, Object>) entries.get("asset");
            String asset_type = (String) entries.get("asset_type");
            if (asset_type == null) {
                asset_type = "Video";
            } else {
                if (!asset_type.equals("Video")) {
                    ext = ".pdf";
                }
            }
            final String json_download_path = "download_urls/" + asset_type;
            final Object download_urls_o = entries.get("download_urls");
            if (download_urls_o != null) {
                filename = (String) entries.get("title");
                final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, json_download_path);
                dllink = (String) JavaScriptEngineFactory.walkJson(ressourcelist.get(ressourcelist.size() - 1), "file");
                if (dllink != null) {
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
                /* json handling did not work or officially there are no downloadlinks --> Grab links from html inside json */
                if (json_view_html == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                json_view_html = json_view_html.replace("\\", "");
                final String[] possibleQualities = { "1080", "720", "480", "360", "240" };
                for (final String possibleQuality : possibleQualities) {
                    dllink = new Regex(json_view_html, "<source src=\"(http[^<>\"]+)\"[^>]+data\\-res=\"" + possibleQuality + "\" />").getMatch(0);
                    if (dllink != null) {
                        break;
                    }
                }
                if (dllink == null) {
                    /* Last chance -see if we can find ANY video-url */
                    dllink = new Regex(json_view_html, "\"(https?://udemy\\-assets\\-on\\-demand\\.udemy\\.com/[^<>\"]+\\.mp4[^<>\"]+)\"").getMatch(0);
                }
                if (dllink != null) {
                    /* Important! */
                    dllink = Encoding.htmlDecode(dllink);
                }
                if (filename == null) {
                    if (title_cleaned != null) {
                        filename = fid_accountneeded + "_" + title_cleaned;
                    } else {
                        filename = fid_accountneeded;
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
            dllink = br.getRegex("\"file\":\"(http[^<>\"]*?)\",\"label\":\"720p").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (ext == null) {
            ext = getFileNameExtensionFromString(dllink, default_Extension);
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        if (description != null && downloadLink.getComment() == null) {
            downloadLink.setComment(description);
        }
        final Browser br2 = new Browser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
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
        if (downloadLink.getDownloadURL().matches(TYPE_SINGLE_PREMIUM)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        handleDownload(downloadLink);
    }

    public void handleDownload(final DownloadLink downloadLink) throws Exception {
        /*
         * Remove old cookies and headers from Browser as they are not needed for their downloadurls in fact using them get you server
         * response 400.
         */
        this.br = new Browser();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 400) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 403) {
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

    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(MAINPAGE, cookies);
                    return;
                }
                br.setFollowRedirects(true);
                br.getPage("https://www.udemy.com/join/login-popup/?displayType=ajax&display_type=popup&showSkipButton=1&returnUrlAfterLogin=https%3A%2F%2Fwww.udemy.com%2F&next=https%3A%2F%2Fwww.udemy.com%2F&locale=de_DE");
                final String csrftoken = br.getCookie(MAINPAGE, "csrftoken");
                if (csrftoken == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final String postData = "csrfmiddlewaretoken=" + csrftoken + "&locale=de_DE&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&displayType=ajax";
                br.postPage("https://www.udemy.com/join/login-popup/?displayType=ajax&display_type=popup&showSkipButton=1&returnUrlAfterLogin=https%3A%2F%2Fwww.udemy.com%2F&next=https%3A%2F%2Fwww.udemy.com%2F&locale=de_DE", postData);
                if (br.containsHTML("data-purpose=\"do-login\"")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(MAINPAGE), "");
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

    public static void prepBRAPI(final Browser br) {
        final String clientid = br.getCookie(MAINPAGE, "client_id");
        final String bearertoken = br.getCookie(MAINPAGE, "access_token");
        final String newrelicid = "XAcEWV5ADAEDUlhaDw==";
        if (clientid == null || bearertoken == null || newrelicid == null) {
            return;
        }
        br.getHeaders().put("X-NewRelic-ID", newrelicid);
        // this.br.getHeaders().put("X-Udemy-Client-Id", clientid);
        br.getHeaders().put("Authorization", "Bearer " + bearertoken);
        br.getHeaders().put("X-Udemy-Authorization", "Bearer " + bearertoken);
    }

    public static String getCourseID(final Browser br) {
        String courseid = br.getRegex("data\\-course\\-id=\"(\\d+)\"").getMatch(0);
        if (courseid == null) {
            courseid = br.getRegex("&quot;id&quot;:\\s*(\\d+)").getMatch(0);
        }
        return courseid;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return FREE_MAXDOWNLOADS;
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
