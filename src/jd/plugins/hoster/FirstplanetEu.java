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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "firstplanet.eu" }, urls = { "http://(?:www\\.)?firstplanet\\.eu/file/\\d+/[a-z0-9\\-]+" }, flags = { 2 })
public class FirstplanetEu extends PluginForHost {

    public FirstplanetEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://firstplanet.eu/payment/");
    }

    @Override
    public String getAGBLink() {
        return "http://firstplanet.eu/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 2;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 4;

    private static final boolean api_use_api_availablecheck   = true;
    private static final boolean api_use_api_free             = true;
    private static final boolean api_use_api_premium          = true;

    private String               api_token                    = null;

    private static final String  API_ENDPOINT                 = "http://firstplanet.eu/api/";

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);
    private String               fid                          = null;
    private String               slug                         = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        fid = getFID(link);
        slug = getSlug(link);
        final String url_name = fid + "_" + slug;
        link.setLinkID(fid);
        link.setName(url_name);
        String filename, filesize, hashes = null;
        if (api_use_api_availablecheck) {
            this.prepBRAPI(this.br);
            this.postPage(API_ENDPOINT, "{\"method\":\"file.getInfo\",\"params\":{\"link\":\"" + link.getDownloadURL() + "\"}}");
            filename = getJson("name");
            filesize = getJson("size");
            hashes = getJson("md5sha1");
            if (filename == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(filename);
            link.setDownloadSize(Long.parseLong(filesize));
        } else {
            this.setBrowserExclusive();
            this.br.setAllowedResponseCodes(500);
            this.br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("class=\"sto\">[\t\n\r ]*?<h1>([^<>\"]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = url_name;
            }
            filesize = br.getRegex("class=\"size\">([^<>\"]*?)<").getMatch(0);
            if (filename == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setName(Encoding.htmlDecode(filename.trim()));
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (hashes != null && hashes.length() >= 32) {
            if (hashes.length() == 32) {
                link.setMD5Hash(hashes);
            } else {
                link.setMD5Hash(hashes.substring(0, 32));
                link.setSha1Hash(hashes.substring(32, hashes.length()));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink downloadLink, final Account aa, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (aa == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        String dllink = null;
        if (api_use_api_free) {
            loginAPI(aa, false);
            dllink = checkDirectLink(downloadLink, directlinkproperty);
            if (dllink == null) {
                this.getPage("http://firstplanet.eu/api-support/config/grc-site-key");
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, this.br, this.br.toString()).getToken();
                this.postPage(API_ENDPOINT, "{\"method\":\"file.requestFreeDownload\",\"params\":{\"accessToken\":\"" + this.api_token + "\",\"link\":\"" + downloadLink.getDownloadURL() + "\",\"captcha\":\"" + recaptchaV2Response + "\"}}");
                br.getPage(downloadLink.getDownloadURL());
                dllink = this.br.getRedirectLocation();
                if (dllink == null) {
                    /* This should never happen but lets assume that the captcha-wrong errorhandling failed for some reason. */
                    logger.warning("Failed to find final downloadlink");
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            // loginWebsite(aa, false);
        }

        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
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

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/file/(\\d+)").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    private String getSlug(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/([^/]+)$").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://firstplanet.eu";
    private static Object       LOCK     = new Object();

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(MAINPAGE, cookies);
                    return;
                }
                br.setFollowRedirects(false);
                br.getPage(MAINPAGE);
                final Form loginform = this.br.getFormbyKey("login");
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                // br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" +
                // Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (!this.br.containsHTML("\\?do=login\\-logout")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(MAINPAGE), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void loginAPI(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            br.setCookiesExclusive(true);
            prepBRAPI(this.br);
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
            br.setFollowRedirects(false);
            this.postPage(API_ENDPOINT, "{\"method\":\"user.login\",\"params\":{\"username\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() + "\"}}");
            api_token = getJson("result");
            if (api_token == null) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        String expire = null;
        if (api_use_api_free || api_use_api_premium) {
            loginAPI(account, true);
            postPage(API_ENDPOINT, "{\"method\":\"user.getServices\",\"params\":{\"accessToken\":\"" + this.api_token + "\"}}");
            final String kind = getJson("kind");
            long trafficleft = 0;
            final String trafficleft_str = getJson("numeric");
            if (trafficleft_str != null) {
                trafficleft = Long.parseLong(trafficleft_str);
            }
            expire = br.getRegex("(\\d{1,2}\\.\\d{1,2}\\.\\d{4})").getMatch(0);
            if ("traffic".equals(kind) && trafficleft_str != null) {
                /* Premium volume account */
                ai.setTrafficLeft(trafficleft);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
                ai.setStatus("Premium volume account");
            } else if (expire != null) {
                /* Premium time account */
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy", Locale.ENGLISH));
                if (trafficleft > 0) {
                    /* Maybe time accounts can also have traffic?! */
                    ai.setTrafficLeft(trafficleft);
                }
                maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
                ai.setStatus("Premium time account");
            } else {
                /* Free account or unsupported account type. */
                maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setType(AccountType.FREE);
                /* free accounts can still have captcha */
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
                ai.setStatus("Registered (free) user");
            }
        } else {
            if (this.br.getURL() == null || !this.br.getURL().contains("/account/services")) {
                this.br.getPage("http://firstplanet.eu/account/services");
            }
            ai.setUnlimitedTraffic();
            expire = br.getRegex(">Neomezené stahování do</span>[\t\n\r ]*?<span class=\"right\">(\\d{1,2}\\.\\d{1,2}\\.\\d{4})</span>").getMatch(0);
            if (expire == null) {
                maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setType(AccountType.FREE);
                /* free accounts can still have captcha */
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
                ai.setStatus("Registered (free) user");
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy", Locale.ENGLISH));
                maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
                ai.setStatus("Premium account");
            }
            account.setValid(true);
        }
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (account.getType() == AccountType.FREE) {
            doFree(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = null;
            if (api_use_api_premium) {
                loginAPI(account, false);
                dllink = this.checkDirectLink(link, "premium_directlink");
                if (dllink == null) {
                    this.br.setFollowRedirects(false);
                    br.getPage(link.getDownloadURL());
                    dllink = this.br.getRedirectLocation();
                }
            } else {
                loginWebsite(account, false);
                dllink = this.checkDirectLink(link, "premium_directlink");
                br.getPage(link.getDownloadURL());
                if (dllink == null) {
                    dllink = br.getRegex("\"(/file/[^<>\"]*?)\" class=\"download ajax\"").getMatch(0);
                    if (dllink == null) {
                        dllink = "/file/" + fid + "/" + slug + "?download-id=" + fid + "&download-slug=" + slug + "&do=download-download";
                    }
                    dllink = Encoding.htmlDecode(dllink);
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
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

    private Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    private void postPage(final String url, final String data) throws IOException, PluginException {
        br.postPageRaw(url, data);
        handleErrors();
    }

    private void getPage(final String url) throws IOException, PluginException {
        br.getPage(url);
        handleErrors();
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private void handleErrors() throws PluginException {
        final String errorcode = getJson("code");
        if (errorcode != null) {
            switch (Integer.parseInt(errorcode)) {
            case -32001:
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Invalid access token");
            case -32002:
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case -32010:
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case -32011:
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            case -32004:
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            case -32602:
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            default:
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
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