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
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "coub.com" }, urls = { "https?://(?:www\\.)?coub\\.com/view/[A-Za-z0-9]+" })
public class CoubCom extends PluginForHost {
    public CoubCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "http://coub.com/tos";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private String               DLLINK            = null;

    @SuppressWarnings("unchecked")
    /** Using API: http://coub.com/dev/docs */
    /**Example for profile decrypter: http://coub.com/api/v2/timeline/channel/22e53751f21ebf9707d4707fc452cb72?per_page=9&permalink=22e53751f21ebf9707d4707fc452cb72&order_by=newest&page=3*/
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        DLLINK = null;
        this.setBrowserExclusive();
        final String fid = getFID(link);
        this.br.getPage("https://coub.com/api/v2/coubs/" + fid);
        if (this.br.getHttpConnection().getResponseCode() == 403) {
            /* {"error":"You are not authorized to access this page.","exc":"CanCan::AccessDenied"} */
            logger.info("Possible private content --> Not sure but probably offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final String filename = getFilename(this, entries, fid);
        DLLINK = (String) JavaScriptEngineFactory.walkJson(entries, "file_versions/web/template");
        if (filename == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Format URL so that it is valid */
        DLLINK = DLLINK.replace("%{type}", "mp4").replace("%{version}", "big");
        link.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                /* Do NOT use HEAD requests here! */
                con = br2.openGetConnection(DLLINK);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setProperty("directlink", DLLINK);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    public static String getFilename(final Plugin plugin, final LinkedHashMap<String, Object> entries, final String fid) {
        String filename = (String) entries.get("raw_video_title");
        if (filename == null) {
            filename = (String) entries.get("title");
        }
        if (filename == null) {
            /* This should never happen! */
            filename = fid;
        }
        if (!StringUtils.endsWithCaseInsensitive(filename, ".mp4") && !StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
            filename += ".mp4";
        }
        filename = plugin.encodeUnicode(filename);
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
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
        downloadLink.setProperty(directlinkproperty, DLLINK);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    // private static final String MAINPAGE = "http://coub.com";
    // private static Object LOCK = new Object();
    //
    // @SuppressWarnings("unchecked")
    // private void login(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
    // // Load cookies
    // br.setCookiesExclusive(true);
    // final Cookies cookies = account.loadCookies("");
    // if (cookies != null && !force) {
    // this.br.setCookies(this.getHost(), cookies);
    // return;
    // }
    // br.setFollowRedirects(false);
    // br.getPage("");
    // br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
    // if (br.getCookie(MAINPAGE, "") == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // account.saveCookies(this.br.getCookies(this.getHost()), "");
    // } catch (final PluginException e) {
    // account.clearCookies("");
    // throw e;
    // }
    // }
    // }
    //
    // @Override
    // public AccountInfo fetchAccountInfo(final Account account) throws Exception {
    // final AccountInfo ai = new AccountInfo();
    // try {
    // login(account, true);
    // } catch (PluginException e) {
    // account.setValid(false);
    // throw e;
    // }
    // String space = br.getRegex("").getMatch(0);
    // if (space != null) {
    // ai.setUsedSpace(space.trim());
    // }
    // ai.setUnlimitedTraffic();
    // if (account.getBooleanProperty("free", false)) {
    // maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
    // account.setType(AccountType.FREE);
    // /* free accounts can still have captcha */
    // account.setMaxSimultanDownloads(maxPrem.get());
    // account.setConcurrentUsePossible(false);
    // ai.setStatus("Registered (free) user");
    // } else {
    // final String expire = br.getRegex("").getMatch(0);
    // if (expire == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // } else {
    // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
    // }
    // maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
    // account.setType(AccountType.PREMIUM);
    // account.setMaxSimultanDownloads(maxPrem.get());
    // account.setConcurrentUsePossible(true);
    // ai.setStatus("Premium account");
    // }
    // account.setValid(true);
    // return ai;
    // }
    //
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // requestFileInformation(link);
    // login(account, false);
    // br.setFollowRedirects(false);
    // br.getPage(link.getDownloadURL());
    // if (account.getBooleanProperty("free", false)) {
    // doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    // } else {
    // String dllink = this.checkDirectLink(link, "premium_directlink");
    // if (dllink == null) {
    // dllink = br.getRegex("").getMatch(0);
    // if (dllink == null) {
    // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // }
    // dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
    // if (dl.getConnection().getContentType().contains("html")) {
    // if (dl.getConnection().getResponseCode() == 403) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
    // } else if (dl.getConnection().getResponseCode() == 404) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
    // }
    // logger.warning("The final dllink seems not to be a file!");
    // br.followConnection();
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // link.setProperty("premium_directlink", dllink);
    // dl.startDownload();
    // }
    // }
    //
    // @Override
    // public int getMaxSimultanPremiumDownloadNum() {
    // /* workaround for free/premium issue on stable 09581 */
    // return maxPrem.get();
    // }
    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}