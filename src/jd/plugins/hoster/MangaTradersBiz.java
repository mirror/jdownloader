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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.IO;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.cryptojs.CryptoJS;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision: 36558 $", interfaceVersion = 3, names = { "mangatraders.biz" }, urls = { "https?://(?:www\\.)*?mangatraders\\.(biz|org)/downloadlink/[A-Za-z0-9]+" })
public class MangaTradersBiz extends antiDDoSForHost {

    private boolean      weAreAlreadyLoggedIn = false;

    private final String mainPage             = "http://mangatraders.biz";
    private final String type_2016_11_15      = ".+/downloadlink/([A-Za-z0-9]+)";
    private final String cookieName           = "UserSession";

    public static Object ACCLOCK              = new Object();

    @Override
    public String rewriteHost(String host) {
        if (host == null || "mangatraders.org".equals(host)) {
            return "mangatraders.biz";
        }
        return super.rewriteHost(host);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    /**
     * because stable is lame!
     */
    public void setBrowser(final Browser ibr) {
        this.br = ibr;
    }

    public void getPage(final String page) throws Exception {
        super.getPage(page);
    }

    public MangaTradersBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.mangatraders.biz/register/");
        /* 2016-11-30: Only one download every 20 seconds possible (via account). */
        this.setStartIntervall(20);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        final String linkpart = new Regex(link.getDownloadURL(), "https?://[^/]+(/.+)").getMatch(0);
        link.setUrlDownload("http://" + this.getHost() + linkpart);
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        br.setFollowRedirects(false);
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            String mainlink;
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null || !aa.isValid()) {
                logger.info("The user didn't enter account data even if they're needed to check the links for this host.");
                return false;
            }
            for (final DownloadLink dl : urls) {
                br = new Browser();
                login(aa, false);
                if (!dl.getDownloadURL().matches(type_2016_11_15)) {
                    /* Old urls are not supported anymore */
                    dl.setAvailable(false);
                    continue;
                }
                mainlink = dl.getStringProperty("mainlink", null);
                getPage(mainlink);
                if (jd.plugins.decrypter.MangaTradersBiz.isOffline(this.br)) {
                    dl.setAvailable(false);
                } else {
                    /* 2016-11-15: We cannot check filesize anymore as (registered) users can only start new downloads every 20 seconds. */
                    dl.setAvailable(true);
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /* 2016-11-15: From now on this is not necessary anymore. */
    private String processJS() {
        String result = null;
        try {
            final String user = br.getRegex("class=\"user\" value=\"(.*?)\">").getMatch(0);
            String link = br.getRegex("class=\"link\" value=\"(.*?)\"").getMatch(0);
            if (user == null || link == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link = Encoding.htmlOnlyDecode(link);
            final String js3 = "var newLocation=JSON.parse(CryptoJS.AES.decrypt(link, user, {format: CryptoJSAesJson}).toString(CryptoJS.enc.Utf8));";
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            engine.put("link", link);
            engine.put("user", user);
            engine.eval(IO.readURLToString(CryptoJS.class.getResource("aes.js")));
            engine.eval(IO.readURLToString(CryptoJS.class.getResource("aes-json-format.js")));
            engine.eval(js3);
            result = (String) engine.get("newLocation");
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return result;
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
        return "http://www.mangatraders.biz/register/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /**
     * JD 2 Code. DO NOT USE OVERRIDE FOR COMPATIBILITY REASONS
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        // Don't check the links because the download will then fail ;)
        // requestFileInformation(downloadLink);
        // Usually JD is already logged in after the linkcheck so if JD is logged in we don't have to log in again here
        if (!weAreAlreadyLoggedIn || br.getCookie(mainPage, cookieName) == null) {
            login(account, false);
        }
        final String dllink = getDllink(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleErrors() throws PluginException {
        final String waitseconds = this.br.getRegex("Error\\s*?:\\s*?You may only download once every (\\d+) seconds").getMatch(0);
        if (waitseconds != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, wait some seconds", Long.parseLong(waitseconds) * 1001l);
        } else if (this.br.containsHTML("Error\\s*?:\\s*?Please Login")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Temporary login issue", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
    }

    private String getDllink(final DownloadLink dl) throws Exception {
        final String linkid = new Regex(dl.getDownloadURL(), type_2016_11_15).getMatch(0);
        postPage("http://" + this.getHost() + "/_standard/php/volume.download.php", "linkValue=" + Encoding.urlEncode(linkid));
        handleErrors();
        final String dllink = this.br.toString();
        if (dllink == null || !dllink.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (ACCLOCK) {
            // used in finally to restore browser redirect status.
            final boolean frd = br.isFollowingRedirects();
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(getHost(), cookies);
                    return;
                }
                // Clear the Referer or the download could start here which then causes an exception
                br = new Browser();
                br.setFollowRedirects(true);
                getPage("http://" + this.getHost());
                postPage("/auth/process.login.php", "EmailAddress=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()) + "&rememberMe=1");
                if (!this.br.toString().equalsIgnoreCase("ok")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.saveCookies(br.getCookies(this.getHost()), "");
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
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        final boolean checked = checkLinks(new DownloadLink[] { downloadLink });
        // we can't throw exception in checklinks! This is needed to prevent multiple captcha events!
        if (!checked && hasAntiddosCaptchaRequirement()) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (!checked || !downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}