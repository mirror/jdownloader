//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 25467 $", interfaceVersion = 2, names = { "mangatraders.org" }, urls = { "http://(www\\.)*?mangatraders\\.org/manga/download\\.php\\?id=[a-f0-9]{10,}" }, flags = { 2 })
public class MangaTradersOrg extends PluginForHost {

    private boolean      weAreAlreadyLoggedIn = false;

    private final String mainPage             = "http://mangatraders.org";
    private final String cookieName           = "username";
    private final String blockedAccess        = "<p>You have attempted to download this file within the last 10 seconds.</p>";
    private final String offlineFile          = ">Download Manager Error - Invalid Fileid";

    public static Object ACCLOCK              = new Object();

    /**
     * because stable is lame!
     * */
    public void setBrowser(final Browser ibr) {
        this.br = ibr;
    }

    public MangaTradersOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.mangatraders.org/register/");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("/view/file/", "/download/file/"));
    }

    public boolean checkLinks(DownloadLink[] urls) {
        br.setFollowRedirects(false);
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null || !aa.isValid()) {
                logger.info("The user didn't enter account data even if they're needed to check the links for this host.");
                return false;
            }
            for (DownloadLink dl : urls) {
                br = new Browser();
                login(aa, false);
                br.getPage(dl.getDownloadURL());
                if (br.getRedirectLocation() == null) {
                    dl.setAvailable(false);
                } else {
                    URLConnectionAdapter con = null;
                    try {
                        if (isNewJD()) {
                            con = br.openHeadConnection(br.getRedirectLocation());
                            dl.setVerifiedFileSize(con.getLongContentLength());
                        } else {
                            con = br.openGetConnection(br.getRedirectLocation());
                            dl.setDownloadSize(con.getLongContentLength());
                        }
                        dl.setFinalFileName(getFileNameFromHeader(con));
                        dl.setAvailable(true);
                    } finally {
                        try {
                            con.disconnect();
                        } catch (final Throwable t) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setStatus("Free Account");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.mangatraders.org/register/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, "Download does only work with account");
    }

    /**
     * JD 2 Code. DO NOT USE OVERRIDE FOR COMPATIBILITY REASONS
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        // Don't check the links because the download will then fail ;)
        // requestFileInformation(downloadLink);
        // Usually JD is already logged in after the linkcheck so if JD is logged in we don't have to log in again here
        if (!weAreAlreadyLoggedIn || br.getCookie("http://www.mangatraders.org/", cookieName) == null) {
            login(account, false);
        }
        br.getPage(downloadLink.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            if (br.containsHTML(offlineFile)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(blockedAccess)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, wait some minutes!", 5 * 60 * 1999l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(blockedAccess)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, wait some minutes!", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (ACCLOCK) {
            // used in finally to restore browser redirect status.
            final boolean frd = br.isFollowingRedirects();
            try {
                // Load cookies
                br.setCookiesExclusive(true);
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
                            br.setCookie(mainPage, key, value);
                        }
                        return;
                    }
                }
                // Clear the Referer or the download could start here which then causes an exception
                br.getHeaders().put("Referer", "");
                br.setFollowRedirects(false);
                br.postPage("http://www.mangatraders.org/login/process.php", "email_Login=" + Encoding.urlEncode(account.getUser()) + "&password_Login=" + Encoding.urlEncode(account.getPass()) + "&redirect_Login=%2F&rememberMe=checked");
                final String userNameCookie = br.getCookie(mainPage, cookieName);
                if (userNameCookie == null || "deleted".equalsIgnoreCase(userNameCookie)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(mainPage);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                weAreAlreadyLoggedIn = true;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                account.setProperty("lastlogin", Property.NULL);
                throw e;
            } finally {
                br.setFollowRedirects(frd);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        if (checkLinks(new DownloadLink[] { downloadLink }) == false) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        }
        return getAvailableStatus(downloadLink);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    private boolean isNewJD() {
        return System.getProperty("jd.revision.jdownloaderrevision") != null ? true : false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}