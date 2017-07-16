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
import java.util.Locale;

import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "suicidegirls.com" }, urls = { "http://suicidegirlsdecrypted/\\d+|https?://(?:www\\.)?suicidegirls\\.com/videos/\\d+/[A-Za-z0-9\\-_]+/" })
public class SuicidegirlsCom extends PluginForHost {

    public SuicidegirlsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.suicidegirls.com/shop/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.suicidegirls.com/legal/";
    }

    /* Linktypes */
    private static final String  TYPE_DECRYPTED               = "http://suicidegirlsdecrypted/\\d+";
    private static final String  TYPE_VIDEO                   = "https?://(?:www\\.)?suicidegirls\\.com/videos/\\d+/[A-Za-z0-9\\-_]+/";

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private String               dllink                       = null;

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = login(br);
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        String filename = null;
        if (link.getDownloadURL().matches(TYPE_VIDEO)) {
            br.getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = br.getRegex("<source src=\"(http[^<>\"]*?)\" type=\\'video/mp4\\'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://[^/]+/videos/[^/]+\\.mp4)\"").getMatch(0);
            }
            filename = br.getRegex("<h2 class=\"title\">SuicideGirls: ([^<>\"]*?)</h2>").getMatch(0);
            if (filename == null) {
                /* Fallback to url-filename */
                filename = new Regex(link.getDownloadURL(), "suicidegirls\\.com/videos/\\d+/([A-Za-z0-9\\-_]+)/").getMatch(0);
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename += ".mp4";
        } else {
            dllink = link.getStringProperty("directlink", null);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        URLConnectionAdapter con = null;
        try {
            try {
                con = br.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                if (filename == null) {
                    filename = getFileNameFromHeader(con);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setProperty("directlink", dllink);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        // String dllink = checkDirectLink(downloadLink, directlinkproperty);
        // if (dllink == null) {
        // dllink = br.getRegex("").getMatch(0);
        // if (dllink == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public static final String MAINPAGE = "http://suicidegirls.com";
    private static Object      LOCK     = new Object();

    public void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBR(br);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(MAINPAGE, cookies);
                    // do a test
                    br.getPage("https://www.suicidegirls.com/member/account/");
                    if (br.containsHTML(">Log Out<")) {
                        return;
                    }
                    br = prepBR(new Browser());
                }
                br.getPage("https://www.suicidegirls.com");
                final Form loginform = br.getFormbyProperty("id", "login-form");
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // login can contain recaptchav2
                if (loginform.containsHTML("g-recaptcha") && loginform.containsHTML("data-sitekey")) {
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {

                        @Override
                        public String getSiteKey() {
                            return getSiteKey(loginform.getHtmlCode());
                        };

                    }.getToken();
                    loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                final String error = PluginJSonUtils.getJsonValue(br, "");
                // br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" +
                // Encoding.urlEncode(account.getPass()));
                if (!"0".equals(error)) {
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
            login(br, account, false);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (br.getURL() == null || !br.getURL().contains("/member/account")) {
            br.getPage("https://www.suicidegirls.com/member/account/");
        }
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("YOUR ACCOUNT IS CLOSING IN <a>(\\d+ weeks?, \\d+ days?)<").getMatch(0);
        long expire_long = -1;
        if (expire != null) {
            final Regex info = new Regex(expire, "(\\d+) weeks?, (\\d+) days?");
            final String weeks = info.getMatch(0);
            final String days = info.getMatch(1);
            final long days_total = Long.parseLong(weeks) * 7 * +Long.parseLong(days);
            expire_long = System.currentTimeMillis() + days_total * 24 * 60 * 60 * 1000l;
        }
        if (expire == null && expire_long == -1) {
            expire = br.getRegex("Your account will be cancelled on (\\w+ \\d+, \\d{4})").getMatch(0);
            expire_long = TimeFormatter.getMilliSeconds(expire, "MMMM dd, yyyy", Locale.ENGLISH);
        }
        if (expire_long > -1) {
            ai.setValidUntil(expire_long);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        } else {
            // free account
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(br, account, false);
        requestFileInformation(link, account);
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            // String dllink = this.checkDirectLink(link, "premium_directlink");
            // if (dllink == null) {
            // dllink = br.getRegex("").getMatch(0);
            // if (dllink == null) {
            // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    public Account login(final Browser br) {
        final ArrayList<Account> accounts = AccountController.getInstance().list("suicidegirls.com");
        if (accounts != null && accounts.size() != 0) {
            final LogInterface logger = br.getLogger();
            for (final Account account : accounts) {
                try {
                    login(br, account, false);
                    return account;
                } catch (final PluginException e) {
                    logger.log(e);
                    account.setValid(false);
                } catch (final Exception e) {
                    logger.log(e);
                }
            }
        }
        return null;
    }

    public Browser prepBR(final Browser br) {
        br.setCookie(MAINPAGE, "burlesque_ad_closed", "True");
        br.setCookie(MAINPAGE, "django_language", "en");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}