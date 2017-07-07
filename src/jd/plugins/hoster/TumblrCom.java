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
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tumblr.com" }, urls = { "http://[\\w\\.\\-]*?tumblrdecrypted\\.com/post/\\d+" })
public class TumblrCom extends PluginForHost {
    public static final long trust_cookie_age = 300000l;
    private String           dllink           = null;

    public TumblrCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.tumblr.com/register");
    }

    @Override
    public String getAGBLink() {
        return "http://www.tumblr.com/terms_of_service";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        // Links come from a decrypter
        link.setUrlDownload(link.getDownloadURL().replace("tumblrdecrypted.com/", "tumblr.com/"));
    }

    private void getDllink() throws IOException {
        br.setFollowRedirects(false);
        dllink = br.getRegex("\"><img src=\"(( +)?https?://\\d+\\.media\\.tumblr\\.com/[^<>\"/\\']*?\\.jpg)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(( +)?https?://\\d+\\.media\\.tumblr\\.com/[^<>\"/\\']*?\\.(jpg|gif|png))\"").getMatch(0);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String ADDITION = "P3BsZWFkPXBsZWFzZS1kb250LWRvd25sb2FkLXRoaXMtb3Itb3VyLWxhd3llcnMtd29udC1sZXQtdXMtaG9zdC1hdWRpbw==";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            /* Login whenever possible to be able to download account-only-stuff */
            login(this.br, aa, false);
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The URL you requested could not be found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = checkDirectLink(downloadLink, "audiodirectlink");
        if (dllink != null) {
            dllink += Encoding.Base64Decode(ADDITION);
        } else {
            String filename = downloadLink.getFinalFileName();
            if (filename == null) {
                filename = new Regex(br.getURL(), "tumblr\\.com/post/\\d+/(.+)").getMatch(0);
            }
            if (filename == null) {
                filename = new Regex(downloadLink.getDownloadURL(), "tumblr\\.com/post/(\\d+)").getMatch(0);
            }
            filename = filename.trim();
            if (br.containsHTML(">renderVideo\\(")) {
                dllink = br.getRegex("\\'(https?://[^<>\"/]*?\\.tumblr\\.com/video_file/\\d+/[^<>\"/]*?)\\'").getMatch(0);
                downloadLink.setFinalFileName(filename + ".mp4");
            } else if (br.containsHTML("class=\"audio_player\"")) {
                dllink = br.getRegex("\\?audio_file=(http[^<>\"]*?)\\&color").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = Encoding.htmlDecode(dllink.trim()) + Encoding.Base64Decode(ADDITION);
            } else {
                getDllink();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = Encoding.htmlDecode(dllink.trim());
                final String ext = getFileNameExtensionFromString(dllink, ".jpg");
                downloadLink.setFinalFileName(filename + ext);
            }
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
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
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("deprecation")
    public static void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    /* Check cookies */
                    br.getPage("https://www." + account.getHoster() + "/dashboard");
                    if ("1".equals(br.getCookie(account.getHoster(), "logged_in"))) {
                        /* Refresh timestamp */
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                    /* Delete cookies / Headers to perform a full login */
                    br = new Browser();
                }
                br.getPage("https://www." + account.getHoster() + "/login");
                final String formkey = br.getRegex("name=\"form_key\" value=\"([^\"]+)\"").getMatch(0);
                if (formkey == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final String postData = "determine_email=" + Encoding.urlEncode(account.getUser()) + "&user%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&user%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&tumblelog%5Bname%5D=&user%5Bage%5D=&context=home_signup&version=STANDARD&follow=&http_referer=https%3A%2F%2Fwww.tumblr.com%2F&form_key=" + Encoding.urlEncode(formkey) + "&seen_suggestion=0&used_suggestion=0&used_auto_suggestion=0&about_tumblr_slide=&random_username_suggestions=%5B%5D";
                br.postPage("/login", postData);
                if (!"1".equals(br.getCookie(account.getHoster(), "logged_in"))) {
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
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* No need to login here - we're already logged in! */
        doFree(link);
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