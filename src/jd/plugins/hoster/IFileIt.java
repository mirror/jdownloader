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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ezfile.ch", "filecloud.io" }, urls = { "https?://(www\\.)?(filecloud\\.io|ezfile\\.ch)/[a-z0-9]+", "fhrfzjnerhfDELETEMEdhzrnfdgvfcas4378zhb" }, flags = { 2, 0 })
public class IFileIt extends PluginForHost {

    private final String         useragent                    = "JDownloader";
    /* must be static so all plugins share same lock */
    private static Object        LOCK                         = new Object();
    private final int            MAXPREMIUMCHUNKS             = -2;

    /* TODO: Check/update these limits */
    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -2;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private static final String  NOCHUNKS                     = "NOCHUNKS";
    private static final String  NORESUME                     = "NORESUME";
    public static final String   MAINPAGE                     = "https://ezfile.ch";
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);
    private static AtomicBoolean UNDERMAINTENANCE             = new AtomicBoolean(false);
    private static final String  UNDERMAINTENANCEUSERTEXT     = "The site is under maintenance!";

    /* Completely useless API: https://ezfile.ch/?m=apidoc */
    private static final String  NICE_HOST                    = "ezfile.ch";
    private static final String  APIKEY                       = "o4hdfne7z19qt8cy5l2m";
    private String               dllink                       = null;

    public IFileIt(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/user-register.html");
        this.setStartIntervall(5 * 1000l);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("filecloud.io/", "ezfile.ch/"));
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public String rewriteHost(String host) {
        if ("filecloud.io".equals(getHost())) {
            if (host == null || "filecloud.io".equals(host)) {
                return "ezfile.ch";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/?m=help&a=tos";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        /* Correct old links */
        correctDownloadLink(downloadLink);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(br);
        final boolean isfollowingRedirect = br.isFollowingRedirects();
        // clear old browser
        br = prepBrowser(new Browser());
        // can be direct link!
        URLConnectionAdapter con = null;
        br.setFollowRedirects(true);
        try {
            con = br.openGetConnection(downloadLink.getDownloadURL());
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                if (con.getResponseCode() == 503) {
                    // they are using cloudflare these days!
                    // downloadLink.getLinkStatus().setStatusText(UNDERMAINTENANCEUSERTEXT);
                    // UNDERMAINTENANCE.set(true);
                    return AvailableStatus.UNCHECKABLE;
                }
                br.followConnection();
            } else {
                downloadLink.setName(getFileNameFromHeader(con));
                try {
                    // @since JD2
                    downloadLink.setVerifiedFileSize(con.getLongContentLength());
                } catch (final Throwable t) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                }
                // lets also set dllink
                dllink = br.getURL();
                // set constants so we can save link, no point wasting this link!
                return AvailableStatus.TRUE;
            }
        } finally {
            br.setFollowRedirects(isfollowingRedirect);
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        if (br.containsHTML("The file at this URL was either removed or")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("class=\"fa fa-file[a-z0-9\\- ]+\"></i>\\&nbsp;([^<>\"]*?) \\[(\\d+(?:,\\d+)?(?:\\.\\d{1,2})? [A-Za-z]{1,5})\\]</span>");
        String filename = finfo.getMatch(0);
        String filesize = finfo.getMatch(1);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        filesize = filesize.replace(",", "");
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink, boolean viaAccount) throws Exception, PluginException {
        final String fid = getFid(downloadLink);
        if (dllink == null) {
            dllink = checkDirectLink(downloadLink, "free_directlink");
        }
        if (dllink == null) {
            final String f1 = br.getRegex("\\'f1\\':[\t\n\r ]*?\\'([^<>\"\\']*?)\\'").getMatch(0);
            final String f2 = br.getRegex("\\'f2\\':[\t\n\r ]*?\\'([^<>\"\\']*?)\\'").getMatch(0);
            if (f1 == null || f2 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String postData = "fkey=" + fid + "&f1=" + f1 + "&f2=" + f2 + "&r=";
            final String recaptchaV2Response = getRecaptchaV2Response();
            postData += Encoding.urlEncode(recaptchaV2Response);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("/?m=download&a=request", postData);
            this.dllink = getJson("downloadUrl");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.setFollowRedirects(false);
        int chunks = FREE_MAXCHUNKS;
        boolean resume = FREE_RESUME;
        if (viaAccount) {
            chunks = ACCOUNT_FREE_MAXCHUNKS;
            resume = ACCOUNT_FREE_RESUME;
        }
        if (downloadLink.getBooleanProperty(IFileIt.NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Possible cloudflare failure", 10 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("free_directlink", dllink);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) {
                    return;
                }
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(IFileIt.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(IFileIt.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(IFileIt.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(IFileIt.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (UNDERMAINTENANCE.get()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, UNDERMAINTENANCEUSERTEXT);
        }
        doFree(downloadLink, false);
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
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
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                String csrftoken = null;
                br.setFollowRedirects(true);
                prepBrowser(br);
                br.postPage("https://ezfile.ch/user-login_p.html", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                br.getPage("https://ezfile.ch/?m=user&a=enter");
                br.getPage("https://login.persona.org/communication_iframe");
                br.getPage("https://login.persona.org/wsapi/session_context");
                csrftoken = getJson("csrf_token");
                br.getPage("https://login.persona.org/sign_in");
                br.getPage("https://login.persona.org/wsapi/session_context");
                br.getPage("https://login.persona.org/wsapi/address_info?email=psp%40jdownloader.org&issuer=default");
                // br.getPage("https://login.persona.org/wsapi/list_emails");
                br.postPageRaw("https://login.persona.org/wsapi/authenticate_user", "");
                br.postPageRaw("https://login.persona.org/wsapi/prolong_session", "{\"csrf\":\"" + csrftoken + "\"}");
                /* Example postdata: http://jdownloader.net:8081/pastebin/133630 */
                br.postPageRaw("https://login.persona.org/wsapi/cert_key", "");
                /* Example assertion value: http://jdownloader.net:8081/pastebin/133631 */
                br.postPage("https://ezfile.ch/?m=user&a=persona", "assertion=");
                br.getPage(MAINPAGE + "/user-login.html");
                // We don't know if a captcha is needed so first we try without,
                // if we get an errormessage we know a captcha is needed
                boolean accountValid = false;
                if (!accountValid) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (true) {
            /* TODO */
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("/?m=user&a=account");
        long traffic_left_long = 0;
        final String traffic_left = br.getRegex("Traffic Allowance: <a href=\"[^\"]+\" style=\"[^\"]+\">([^<>\"]*?)</a>").getMatch(0);
        if (traffic_left == null) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        traffic_left_long = SizeFormatter.getSize(traffic_left);
        // Only free acc support till now
        if (traffic_left_long == 0) {
            ai.setStatus("Registered (free) account");
            account.setProperty("free", true);
            try {
                account.setType(AccountType.FREE);
                maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
                account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
                /* free accounts can still have captcha. */
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
            /* No premium traffic means unlimited fre traffic */
            ai.setUnlimitedTraffic();
        } else {
            ai.setStatus("Premium account");
            account.setProperty("free", false);
            ai.setValidUntil(Long.parseLong(getJson("premium_until", br)) * 1000);
            try {
                account.setType(AccountType.PREMIUM);
                maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            ai.setTrafficLeft(traffic_left_long);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (UNDERMAINTENANCE.get()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, UNDERMAINTENANCEUSERTEXT);
        }
        login(account, false);
        if (!account.getBooleanProperty("free", false)) {
            /* TODO */
            if (true) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String apikey = null;
            final String fid = getFid(link);
            if (apikey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            try {
                br.postPage((br.getHttpConnection() == null ? MAINPAGE : "") + "/api-fetch_download_url.api", "akey=" + apikey + "&ukey=" + fid);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            if (br.containsHTML("\"message\":\"no such file\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String finallink = getJson("download_url", br);
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            finallink = finallink.replace("\\", "");

            int maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
            if (link.getBooleanProperty(NOCHUNKS, false)) {
                maxchunks = 1;
            }

            dl = jd.plugins.BrowserAdapter.openDownload(br, link, finallink, ACCOUNT_PREMIUM_RESUME, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 503) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many connections", 10 * 60 * 1000l);
                }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(IFileIt.NOCHUNKS, false) == false) {
                    link.setProperty(IFileIt.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } else {
            br.setFollowRedirects(true);
            doFree(link, true);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = openConnection(br2, dllink);
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

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
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

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    public Browser prepBrowser(Browser br) {
        if (br == null) {
            br = new Browser();
        }
        br.getHeaders().put("User-Agent", useragent);
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.setCookie("http://filecloud.io/", "lang", "en");
        return br;
    }

    private String getFid(final DownloadLink downloadLink) {
        return new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    private void xmlrequest(final Browser br, final String url, final String postData) throws IOException {
        br.getHeaders().put("User-Agent", useragent);
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.postPage(url, postData);
        br.getHeaders().remove("X-Requested-With");
    }

    public static String getJson(final String parameter, final Browser br) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) {
            result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
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