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
import java.util.Locale;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "share-rapid.cz", "file-share.top" }, urls = { "http://(www\\.)?(share\\-rapid\\.(biz|com|info|cz|eu|info|net|sk)|((mediatack|rapidspool|e\\-stahuj|premium\\-rapidshare|qiuck|rapidshare\\-premium|share\\-credit|srapid|share\\-free)\\.cz)|((strelci|share\\-ms|)\\.net)|jirkasekyrka\\.com|((kadzet|universal\\-share)\\.com)|sharerapid\\.(biz|cz|net|org|sk)|stahuj\\-zdarma\\.eu|share\\-central\\.cz|rapids\\.cz|megarapid\\.cz)/(stahuj|soubor)/([0-9]+/.+|[a-z0-9]+)", "https?://(?:www\\.)?file\\-share\\.top/file/\\d+/[^/]+" }, flags = { 2, 2 })
public class ShareRapidCz extends PluginForHost {

    private static final String  TYPE_CURRENT                 = "https?://(?:www\\.)?file\\-share\\.top/file/\\d+/[^/]+";
    private static final String  MAINPAGE                     = "http://file-share.top/";
    private static Object        LOCK                         = new Object();

    /* Connection stuff */
    // private static final boolean FREE_RESUME = true;
    // private static final int FREE_MAXCHUNKS = 0;
    private static final int     FREE_MAXDOWNLOADS            = -1;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    public ShareRapidCz(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sharerapid.cz/dobiti/?zeme=1");
    }

    @Override
    public String[] siteSupportedNames() {
        // this shall prevent share-rapid.cz from been tested, it has no dns record.
        return new String[] { "file-share.top" };
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "share-rapid.cz".equals(host) || "sharerapid.cz".equals(host) || "sharerapid.sk".equals(host) || "megarapid.cz".equals(host) || "share-rapid.cz".equals(host)) {
            return "file-share.top";
        }
        return super.rewriteHost(host);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.getDownloadURL().matches(TYPE_CURRENT)) {
            /* Older urls --> Offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        setBrowserExclusive();
        prepBr(this.br);
        br.getPage(link.getDownloadURL());
        checkOffline();
        br.setFollowRedirects(true);
        String filename = br.getRegex("class=\"fa fa-file-o\"></i>([^<>\"]*?)<small>").getMatch(0);
        final String filesize = br.getRegex("<strong>Velikost:</strong>([^<>\"]*?)</p>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        }
        return AvailableStatus.TRUE;
    }

    private void checkOffline() throws PluginException {
        if (br.containsHTML("Nastala chyba 404") || br.containsHTML("Soubor byl smazán") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        long realTraffic = 0l;
        String trafficleft = null;
        /**
         * Expire unlimited -> Unlimited traffic for a specified amount of time Normal expire -> Expire date + trafficleft
         *
         */
        final String expireUnlimited = br.getRegex("<td>Paušální stahování aktivní\\. Vyprší </td><td><strong>([0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4} - [0-9]{1,2}:[0-9]{1,2})</strong>").getMatch(0);
        if (expireUnlimited != null) {
            /* TODO: Check if this case still exists */
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireUnlimited, "dd.MM.yy - HH:mm", Locale.ENGLISH));
            ai.setUnlimitedTraffic();
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account with unlimited traffic");
            account.setValid(true);
            return ai;
        } else {
            trafficleft = br.getMatch("class=\"fa fa\\-database\"></i>([^<>\"]*?)</a>");
            if (trafficleft != null) {
                logger.info("Available traffic equals: " + trafficleft);
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
                realTraffic = SizeFormatter.getSize(trafficleft);
            } else {
                ai.setUnlimitedTraffic();
            }
            final String expires = br.getMatch("Neomezený tarif vyprší</td><td><strong>([0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4} - [0-9]{1,2}:[0-9]{1,2})</strong>");
            if (expires != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expires, "dd.MM.yy - HH:mm", Locale.ENGLISH));
            }
        }
        if (realTraffic > 0l) {
            /**
             * Max simultan downloads (higher than 1) only works if you got any
             */
            ai.setStatus("Premium User");
            account.setType(AccountType.PREMIUM);
            final String maxSimultanDownloads = br.getRegex("<td>Max\\. počet paralelních stahování: </td><td>(\\d+) <a href").getMatch(0);
            if (maxSimultanDownloads != null) {
                account.setMaxSimultanDownloads(Integer.parseInt(maxSimultanDownloads));
            } else {
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            }
            account.setConcurrentUsePossible(true);
        } else {
            ai.setStatus("Registered (free) User");
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "informace/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* Account only */
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            login(account, false);
            br.getPage(downloadLink.getDownloadURL());
            checkOffline();
            dllink = downloadLink.getDownloadURL().replace("/file/", "/file/download/");
        }
        logger.info("Final downloadlink = " + dllink);
        br.setFollowRedirects(true);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        } catch (final PluginException e) {
            // Link; 6150801113541.log; 388414; jdlog://6150801113541
            // they have redirection issues which are infinite loops... this is the best way to address
            if (StringUtils.endsWithCaseInsensitive(e.getErrorMessage(), "Redirectloop")) {
                final String redirect = br.getRedirectLocation();
                if (redirect.matches(".+/\\?error=2$")) {
                    // unknown error message type...
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Error Handling, Pleae report issues");
                }
            }
        }
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 400) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 400", 5 * 60 * 1000l);
            }
            br.followConnection();
            if (br.containsHTML("(was not found on this server|No htmlCode read)")) {
                /** Show other errormessage if free account was used */
                if (account.getType() == AccountType.FREE) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error: Maybe this file cannot be downloaded as a freeuser: Buy traffic or try again later", 60 * 60 * 1000);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000);
            } else if (this.br.getURL().contains("/panel/credit")) {
                logger.info("Traffic empty / Not enough traffic to download this file");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNot enough traffic available!", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                setBrowserExclusive();
                br.setFollowRedirects(false);
                br.setDebug(true);
                prepBr(this.br);
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
                br.setFollowRedirects(true);
                br.getPage(MAINPAGE + "login");
                final String lang = System.getProperty("user.language");
                final Form form = this.br.getFormbyKey("password");
                if (form == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                form.put("email", Encoding.urlEncode(account.getUser()));
                form.put("password", Encoding.urlEncode(account.getPass()));
                form.put("remember", "yes");
                br.submitForm(form);
                if (!br.containsHTML("class=\"fa fa-power}\\-off\"") && this.br.getCookie(MAINPAGE, "RMT") == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /** Save cookies */
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

    private void prepBr(final Browser br) throws IOException {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0");
        br.setCustomCharset("UTF-8");
        /* Set english language */
        br.getPage("http://file-share.top/lang/set/gb");
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}