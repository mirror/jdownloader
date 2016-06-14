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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "instagram.com" }, urls = { "https?://(?:www\\.)?(?:instagram\\.com|instagr\\.am)/p/[A-Za-z0-9_-]+" }, flags = { 2 })
public class InstaGramCom extends PluginForHost {

    @SuppressWarnings("deprecation")
    public InstaGramCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium(MAINPAGE + "/accounts/login/");
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/about/legal/terms/#";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("instagr.am/", "instagram.com/").replaceFirst("^http://", "https://").replaceFirst("://in", "://www.in"));
    }

    /* Connection stuff */
    private static final boolean RESUME                            = true;
    /* Chunkload makes no sense for pictures/small files */
    private static final int     MAXCHUNKS_pictures                = 1;
    private static final int     MAXCHUNKS_videos                  = 0;
    private static final int     MAXDOWNLOADS                      = -1;

    private static final String  MAINPAGE                          = "https://www.instagram.com";
    public static final String   QUIT_ON_RATE_LIMIT_REACHED        = "QUIT_ON_RATE_LIMIT_REACHED";
    public static final boolean  defaultQUIT_ON_RATE_LIMIT_REACHED = false;

    private static Object        LOCK                              = new Object();

    private boolean              is_private_url                    = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        /*
         * Decrypter can set this status - basically to be able to handfle private urls correctly in host plugin in case users' account gets
         * disabled for whatever reason.
         */
        prepBR(this.br);
        is_private_url = downloadLink.getBooleanProperty("private_url", false);
        boolean is_logged_in = false;
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(this.br, aa, false);
                is_logged_in = true;
            } catch (final Throwable e) {
            }
        }
        if (this.is_private_url && !is_logged_in) {
            downloadLink.getLinkStatus().setStatusText("Login required to download this content");
            return AvailableStatus.UNCHECKABLE;
        }
        String getlink = downloadLink.getDownloadURL();
        if (!getlink.endsWith("/")) {
            /* Add slash to the end to prevent 302 redirect to speed up the download process a tiny bit. */
            getlink += "/";
        }
        br.getPage(getlink);
        if (br.containsHTML("Oops, an error occurred") || br.getRequest().getHttpConnection().getResponseCode() == 404) {
            /* This will also happen if a user tries to access private urls without being logged in! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        DLLINK = PluginJSonUtils.getJson(this.br, "video_url");
        // Maybe we have a picture
        if (DLLINK == null) {
            DLLINK = br.getRegex("property=\"og:image\" content=\"(http[^<>\"]*?)\"").getMatch(0);
            String remove = new Regex(DLLINK, "(/[a-z0-9]+?x[0-9]+/)").getMatch(0); // Size
            if (remove != null) {
                DLLINK = DLLINK.replace(remove, "/");
            }
            downloadLink.setContentUrl(DLLINK);
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK.replace("\\", ""));
        final String username = br.getRegex("\"owner\".*?\"username\": ?\"([^<>\"]*?)\"").getMatch(0);
        final String linkid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9_-]+)$").getMatch(0);
        String filename = null;
        if (StringUtils.isNotEmpty(username)) {
            filename = username + " - " + linkid;
        } else {
            filename = linkid;
        }
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".mp4";
        }
        if (ext.length() < 3) {
            ext = ".jpg";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
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
        if (this.is_private_url) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        handleDownload(downloadLink);
    }

    public void handleDownload(final DownloadLink downloadLink) throws Exception {
        int maxchunks = MAXCHUNKS_pictures;
        if (downloadLink.getFinalFileName().contains(".mp4")) {
            maxchunks = MAXCHUNKS_videos;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, RESUME, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAXDOWNLOADS;
    }

    @SuppressWarnings("unchecked")
    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBR(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.getPage(MAINPAGE + "/accounts/login/");
                try {
                    br.setHeader("X-Instagram-AJAX", "1");
                    br.setHeader("X-CSRFToken", br.getCookie("instagram.com", "csrftoken"));
                    br.setHeader("X-Requested-With", "XMLHttpRequest");
                    br.postPage("/accounts/login/ajax/", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                } finally {
                    br.setHeader("X-Instagram-AJAX", null);
                    br.setHeader("X-CSRFToken", null);
                    br.setHeader("X-Requested-With", null);
                }
                if (!br.containsHTML("\"authenticated\"\\s*:\\s*true\\s*")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
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
        account.setConcurrentUsePossible(true);
        ai.setStatus("Free Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* We're already logged in - no need to login again here! */
        this.handleDownload(link);
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.73 Safari/537.36");
        br.setCookie(MAINPAGE, "ig_pr", "1");
        // 429 == too many requests, we need to rate limit requests.
        br.setAllowedResponseCodes(429);
        br.setRequestIntervalLimit("instagram.com", 250);
        br.setFollowRedirects(true);
        return br;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), QUIT_ON_RATE_LIMIT_REACHED, JDL.L("plugins.hoster.InstaGramCom.quitOnRateLimitReached", "Abort crawl process once rate limit is reached?")).setDefaultValue(defaultQUIT_ON_RATE_LIMIT_REACHED));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return MAXDOWNLOADS;
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
