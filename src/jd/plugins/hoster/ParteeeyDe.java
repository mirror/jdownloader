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

import java.util.LinkedHashMap;

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
import jd.plugins.PluginForHost;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "parteeey.de" }, urls = { "https?://(?:www\\.)?parteeey\\.de/(?:#mulFile\\-|galerie/datei\\?p=)\\d+" })
public class ParteeeyDe extends PluginForHost {

    public ParteeeyDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.parteeey.de/registrierung");
    }

    @Override
    public String getAGBLink() {
        return "https://www.parteeey.de/nutzungsbedingungen";
    }

    /* Connection stuff - disable resume & chunks to keep serverload low. */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;

    public static final String   default_extension = ".jpg";

    public static final long     trust_cookie_age  = 300000l;

    private String               DLLINK            = null;
    private boolean              SERVER_ERROR      = false;

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        DLLINK = null;
        SERVER_ERROR = false;
        this.setBrowserExclusive();
        prepBR(this.br);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Account needed for linkcheck- and download");
            return AvailableStatus.UNCHECKABLE;
        }
        login(this.br, aa, false);

        String url_thumb = link.getStringProperty("thumburl", null);
        String filename = null;
        String filename_decrypter = link.getStringProperty("decrypterfilename", null);
        final String linkid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (linkid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setLinkID(linkid);

        /*
         * 2016-08-21: Prefer download via official download function as this gives us the highest quality possible - usually a bit(some
         * KBs) better than the image displayed in the gallery via browser.
         */
        DLLINK = "https://www." + this.getHost() + "/galerie/datei/herunterladen/" + linkid;
        /* 2016-08-21: This if-statement is obsolete but let's keep it as it might comes in handy it in the future. */
        if (DLLINK == null) {
            /* 2016-08-21: Set width & height to higher values so that we get the max quality possible. */
            this.br.postPage("https://www." + this.getHost() + "/Ajax/mulFileInfo", "filId=" + linkid + "&width=5000&height=5000&filIdPrevious=&filIdNext=");
            try {
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                DLLINK = (String) entries.get("path");
            } catch (final Throwable e) {
            }
            if (DLLINK == null) {
                if (url_thumb != null && !url_thumb.startsWith("http")) {
                    url_thumb = "https://www." + this.getHost() + "/" + url_thumb;
                }
                DLLINK = url_thumb;
            } else {
                if (!DLLINK.startsWith("http") && !DLLINK.startsWith("/")) {
                    DLLINK = "https://www." + this.getHost() + "/" + DLLINK;
                }
            }
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }

        final String url_filename = getFilenameFromDirecturl(DLLINK);
        if (url_filename != null) {
            filename = url_filename;
        } else if (filename_decrypter != null) {
            filename = filename_decrypter;
            if (!filename.endsWith(default_extension)) {
                filename += default_extension;
            }
        } else {
            filename = linkid + default_extension;
        }
        link.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            con = this.br.openHeadConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                SERVER_ERROR = true;
            }
            link.setProperty("directlink", DLLINK);
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
        /* Account needed to download. */
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (SERVER_ERROR) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, FREE_RESUME, FREE_MAXCHUNKS);
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
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://parteeey.de";
    private static Object       LOCK     = new Object();

    public static void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBR(br);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(MAINPAGE, cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    br.getPage("https://www." + account.getHoster());
                    if (br.containsHTML("class=\"navbar\\-user\\-info\"")) {
                        /* Cookies refreshed/re-used successfully - let's save them again to refresh the timestamp. */
                        account.saveCookies(br.getCookies(MAINPAGE), "");
                        return;
                    }
                    /* Something failed - reset Browser and perform a full login. */
                    br = prepBR(new Browser());
                }
                br.setFollowRedirects(true);
                br.postPage("https://www." + account.getHoster() + "/login", "loginData%5BauthsysAuthProvider%5D%5BrememberLogin%5D=on&sent=true&url=%2F&usedProvider=authsysAuthProvider&loginData%5BauthsysAuthProvider%5D%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&loginData%5BauthsysAuthProvider%5D%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("Ihre Login\\-Daten sind ungültig") || !br.containsHTML("/logout\"")) {
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
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        doFree(link);
    }

    public static String getFilenameFromDirecturl(final String directurl) {
        if (directurl == null) {
            return null;
        }
        final String url_name = new Regex(directurl, "/[a-z]+_\\d+_\\d+_\\d+_(.+)$").getMatch(0);
        return url_name;
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}