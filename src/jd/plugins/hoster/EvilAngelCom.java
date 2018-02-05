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

import java.text.SimpleDateFormat;
import java.util.Date;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "evilangel.com", "evilangelnetwork.com" }, urls = { "https?://members\\.evilangel.com/(?:en/)?[A-Za-z0-9\\-_]+/(?:download/\\d+/\\d+p/mp4|film/\\d+)", "https?://members\\.evilangelnetwork\\.com/[A-Za-z]{2}/video/[A-Za-z0-9\\-_]+/\\d+" })
public class EvilAngelCom extends antiDDoSForHost {
    public EvilAngelCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.evilangel.com/en/join");
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            prepBr.setCookiesExclusive(true);
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setCookie(host, "enterSite", "en");
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    @Override
    public String getAGBLink() {
        return "http://www.evilangel.com/en/terms";
    }

    private String              dllink                     = null;
    public static final long    trust_cookie_age           = 300000l;
    private static final String HTML_LOGGEDIN              = "id=\"headerLinkLogout\"";
    public static final String  LOGIN_PAGE                 = "http://members.evilangel.com/en";
    private static final String URL_EVILANGEL_FILM         = "https?://members\\.evilangel.com/[A-Za-z]{2}/[A-Za-z0-9\\-_]+/film/\\d+";
    private static final String URL_EVILANGELNETWORK_VIDEO = "https?://members\\.evilangelnetwork\\.com/[A-Za-z]{2}/video/[A-Za-z0-9\\-_]+/\\d+";

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    /**
     * NOTE: While making the plugin, the testaccount was banned temporarily and we didn't get new password/username from the user->Plugin
     * isn't 100% done yet! http://svn.jdownloader.org/issues/6793
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            String filename = null;
            loginEvilAngelNetwork(this.br, aa, LOGIN_PAGE, HTML_LOGGEDIN);
            if (link.getDownloadURL().matches(URL_EVILANGEL_FILM)) {
                getPage(link.getDownloadURL());
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = getVideoTitle();
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename = Encoding.htmlDecode(filename.trim());
                dllink = getDllink(this.br);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = "http://members.evilangel.com" + dllink;
                final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
                if (quality == null) {
                    filename += ".mp4";
                } else {
                    filename = filename + "-" + quality + ".mp4";
                }
            } else if (link.getDownloadURL().matches(URL_EVILANGELNETWORK_VIDEO)) {
                getPage(link.getDownloadURL());
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = getVideoTitle();
                if (filename == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename = Encoding.htmlDecode(filename.trim());
                dllink = getDllink(this.br);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = "http://members.evilangelnetwork.com" + dllink;
                final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
                if (quality == null) {
                    filename += ".mp4";
                } else {
                    filename = filename + "-" + quality + ".mp4";
                }
            } else {
                dllink = link.getDownloadURL();
            }
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    if (filename == null) {
                        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                    } else {
                        link.setFinalFileName(filename);
                    }
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
        } else {
            link.getLinkStatus().setStatusText("Links can only be checked and downloaded via account!");
            return AvailableStatus.UNCHECKABLE;
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            return super.canHandle(downloadLink, account);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Links can only be checked and downloaded via account!");
    }

    private String getVideoTitle() {
        String title = br.getRegex("<h1 class=\"title\">([^<>\"]*?)</h1>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<h1 class=\"h1_title\">([^<>\"]*?)</h1>").getMatch(0);
            if (title == null) {
                title = br.getRegex("<h2 class=\"h2_title\">([^<>\"]*?)</h2>").getMatch(0);
            }
        }
        return title;
    }

    public static String getDllink(final Browser br) {
        /** INFO: There are also .wmv versions available but we prefer .mp4 here as 1080p is only available as .mp4 */
        String dllink = null;
        final String[] qualities = { "1080p", "720p", "540p", "480p", "240p", "160p" };
        for (final String quality : qualities) {
            dllink = br.getRegex("\"(/(?:en/)?[A-Za-z0-9\\-_]+/download/\\d+/" + quality + "(?:/[^/]*?)?/mp4)\"").getMatch(0);
            if (dllink != null) {
                break;
            }
        }
        return dllink;
    }

    private static Object LOCK = new Object();

    /** Function can be used for all evilangel type of networks/websites. */
    public void loginEvilAngelNetwork(Browser ibr, final Account account, String getpage, final String html_loggedin) throws Exception {
        synchronized (LOCK) {
            try {
                final String host_account = account.getHoster();
                final String url_main = "http://" + host_account + "/";
                final Cookies cookies = account.loadCookies("");
                br = ibr.cloneBrowser();
                if (host_account.equals("evilangelnetwork.com")) {
                    getpage = "http://www.evilangelnetwork.com/en/login";
                } else if (host_account.equalsIgnoreCase("evilangel.com")) {
                    getpage = "http://members.evilangel.com/en";
                } else {
                    /* getpage must have already been set via parameter */
                }
                if (cookies != null) {
                    br.setCookies(host_account, cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        ibr = br.cloneBrowser();
                        return;
                    }
                    getPage(br, getpage);
                    if (br.containsHTML(html_loggedin)) {
                        account.saveCookies(br.getCookies(host_account), "");
                        ibr = br.cloneBrowser();
                        return;
                    }
                    br = new Browser();
                }
                /* We re over 18 */
                br.setFollowRedirects(true);
                getPage(br, getpage);
                if (br.containsHTML(">We are experiencing some problems\\!<")) {
                    final AccountInfo ai = new AccountInfo();
                    ai.setStatus("Your IP is banned. Please re-connect to get a new IP to be able to log-in!");
                    account.setAccountInfo(ai);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final Form login = br.getFormbyProperty("id", "loginForm");
                if (login == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                {
                    final Date d = new Date();
                    SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
                    final String date = sd.format(d);
                    sd = new SimpleDateFormat("k:mm");
                    final String time = sd.format(d);
                    final String timedatestring = date + " " + time;
                    br.setCookie(url_main, "mDateTime", Encoding.urlEncode(timedatestring));
                    br.setCookie(url_main, "mOffset", "1");
                    br.setCookie(url_main, "origin", "promo");
                    br.setCookie(url_main, "timestamp", Long.toString(System.currentTimeMillis()));
                }
                login.setAction("/en/login");
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                if (login.containsHTML("g-recaptcha")) {
                    // recaptchav2
                    final DownloadLink orig = this.getDownloadLink();
                    try {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account Login!", getHost(), getHost(), true);
                        this.setDownloadLink(dummyLink);
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                            @Override
                            public String getSiteKey() {
                                return getSiteKey(login.getHtmlCode());
                            }
                        }.getToken();
                        login.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    } finally {
                        this.setDownloadLink(orig);
                    }
                }
                login.remove("submit");
                submitForm(login);
                if (br.containsHTML(">Your account is deactivated for abuse")) {
                    final AccountInfo ai = new AccountInfo();
                    ai.setStatus("Your account is deactivated for abuse. Please re-activate it to use it in JDownloader.");
                    account.setAccountInfo(ai);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Your account is deactivated for abuse. Please re-activate it to use it in JDownloader.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.containsHTML(">Wrong username or password provided. Please try again\\\\.<") || !br.containsHTML(html_loggedin)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(host_account), "");
                ibr = br.cloneBrowser();
                account.setType(AccountType.PREMIUM);
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
            // Prevent direct login to prevent login captcha
            loginEvilAngelNetwork(this.br, account, LOGIN_PAGE, HTML_LOGGEDIN);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}