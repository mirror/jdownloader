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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hentai-foundry.com" }, urls = { "https?://www\\.hentai-foundry\\.com/pictures/user/[A-Za-z0-9\\-_]+/\\d+|https?://www\\.hentai-foundry\\.com/stories/user/[A-Za-z0-9\\-_]+/\\d+/[A-Za-z0-9\\-_]+\\.pdf" })
public class HentaiFoundryCom extends PluginForHost {
    public HentaiFoundryCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://www.hentai-foundry.com/users/create");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other: connections & downloads limites cause we re downloading small files
    private static final String type_direct_pdf = "https?://www\\.hentai\\-foundry\\.com/stories/user/[A-Za-z0-9\\-_]+/\\d+/[A-Za-z0-9\\-_]+\\.pdf";
    private static final String type_picture    = "https?://www\\.hentai\\-foundry\\.com/pictures/user/[A-Za-z0-9\\-_]+/\\d+";
    private String              dllink          = null;

    @Override
    public String getAGBLink() {
        return "https://www.hentai-foundry.com/";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "Filename_id", "Choose file name + id?").setDefaultValue(false));
    }

    public static String getFID(final String url) {
        return new Regex(url, "/user/[A-Za-z0-9\\-_]+/(\\d+)").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    @SuppressWarnings("deprecation")
    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        dllink = null;
        String title = null;
        String ext = null;
        final String fid = getFID(link.getDownloadURL());
        br.setFollowRedirects(true);
        if (account != null) {
            login(br, account, false);
        }
        if (link.getDownloadURL().matches(type_direct_pdf)) {
            dllink = link.getDownloadURL() + "?enterAgree=1&size=0";
            ext = ".pdf";
            title = fid + "_" + new Regex(link.getDownloadURL(), "([A-Za-z0-9\\-_]+\\.pdf)$").getMatch(0);
        } else {
            br.getPage(link.getDownloadURL() + "?enterAgree=1&size=0");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            title = br.getRegex("<title>([^<>]*?) - Hentai Foundry</title>").getMatch(0);
            dllink = br.getRegex("(//pictures\\.hentai-foundry\\.com/{1,}[^<>\"\\[\\]]+)\"").getMatch(0);
            if (title == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink != null) {
                dllink = Request.getLocation(Encoding.htmlDecode(dllink), br.getRequest());
            }
            if (getPluginConfig().getBooleanProperty("Filename_id", true)) {
                title = Encoding.htmlDecode(title) + "_" + fid;
            } else {
                title = fid + "_" + Encoding.htmlDecode(title);
            }
            title = title.trim();
        }
        if (ext == null && dllink != null) {
            ext = getFileNameExtensionFromString(dllink, ".png");
            /*
             * 2017-01-30: Fallback for some pictures - their urls end with "." and they do not even have an extensions via browser -->
             * Usually these are .jpg files.
             */
            if (!ext.matches("\\.[A-Za-z]{3,5}")) {
                ext = ".jpg";
            }
        } else if (ext == null) {
            ext = ".jpg";
        }
        if (title != null) {
            link.setFinalFileName(this.applyFilenameExtension(title, ext));
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                if (title != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, con));
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
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Image broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    @SuppressWarnings("unchecked")
    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof Map<?, ?> && !force) {
                    final Map<String, String> cookies = (Map<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(account.getHoster(), key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("https://www.hentai-foundry.com/?enterAgree=1&size=0");
                final String csrftoken = br.getRegex("value=\"([^\"]+)\" name=\"YII_CSRF_TOKEN\"").getMatch(0);
                if (csrftoken == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage("/site/login", "LoginForm%5BrememberMe%5D=1&YII_CSRF_TOKEN=" + csrftoken + "&LoginForm%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("site/logout'>Logout</a>")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(br.getHost());
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(br, account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* free accounts can still have captcha */
        account.setMaxSimultanDownloads(5);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
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
