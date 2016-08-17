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
import java.util.concurrent.atomic.AtomicInteger;

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

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    private String               DLLINK            = null;
    private boolean              SERVER_ERROR      = false;

    /* don't touch the following! */
    private static AtomicInteger maxPrem           = new AtomicInteger(1);

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        DLLINK = null;
        this.setBrowserExclusive();
        /* 2015-12-12: Account not necessarily needed to download their content wirh the information we already got via decrypter. */
        // final Account aa = AccountController.getInstance().getValidAccount(this);
        // if (aa == null) {
        // link.getLinkStatus().setStatusText("Cannot check/download without account");
        // return AvailableStatus.UNCHECKABLE;
        // }
        prepBR(this.br);
        String url_thumb = link.getStringProperty("thumburl", null);
        String filename = null;
        String filename_decrypter = link.getStringProperty("decrypterfilename", null);
        final String linkid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (linkid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.postPage("https://www.parteeey.de/Ajax/mulFileInfo", "filId=" + linkid + "&width=1119&height=690&filIdPrevious=&filIdNext=&_iconPath=images%2Ficons%2FLumina");
        try {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            DLLINK = (String) entries.get("path");
        } catch (final Throwable e) {
        }
        if (DLLINK == null) {
            if (url_thumb != null && !url_thumb.startsWith("http")) {
                url_thumb = "https://www.parteeey.de/" + url_thumb;
            }
            DLLINK = url_thumb;
        } else {
            if (!DLLINK.startsWith("http") && !DLLINK.startsWith("/")) {
                DLLINK = "https://www.parteeey.de/" + DLLINK;
            }
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_filename = getFilenameFromDirecturl(DLLINK);
        if (url_filename != null) {
            filename = url_filename;
        } else if (filename_decrypter != null) {
            filename = filename_decrypter;
        } else {
            filename = linkid + ".jpg";
        }
        // DLLINK = "" + linkid;
        link.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(DLLINK);
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
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (SERVER_ERROR) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
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

    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBR(br);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(MAINPAGE, cookies);
                    return;
                }
                br.setFollowRedirects(true);
                br.postPage("https://www.parteeey.de/login", "loginData%5BauthsysAuthProvider%5D%5BrememberLogin%5D=on&sent=true&url=%2F&usedProvider=authsysAuthProvider&loginData%5BauthsysAuthProvider%5D%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&loginData%5BauthsysAuthProvider%5D%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()));
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
        account.setMaxSimultanDownloads(maxPrem.get());
        account.setConcurrentUsePossible(true);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleFree(link);
    }

    public static String getFilenameFromDirecturl(final String directurl) {
        if (directurl == null) {
            return null;
        }
        final String url_name = new Regex(directurl, "/[a-z]+_\\d+_\\d+_\\d+_(.+)$").getMatch(0);
        return url_name;
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:42.0) Gecko/20100101 Firefox/42.0");
        br.setFollowRedirects(true);
        return br;
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