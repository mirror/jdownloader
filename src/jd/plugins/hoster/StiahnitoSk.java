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
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stiahnito.sk" }, urls = { "http://(www\\.)?stiahnito\\.sk/(sutaz/)?[a-z0-9\\-]+/\\d+" }, flags = { 2 })
public class StiahnitoSk extends PluginForHost {

    public StiahnitoSk(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.stiahnito.sk/ucet/credit");
    }

    @Override
    public String getAGBLink() {
        return "http://www.stiahnito.sk/terms-and-conditions";
    }

    /*
     * Sister sites: hellshare.cz, (and their other domains), hellspy.cz (and their other domains), using same dataservers but slightly
     * different script
     */

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = -5;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -5;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private static final String  COOKIE_HOST                  = "http://stiahnito.sk";
    private static final boolean ALL_PREMIUMONLY              = true;
    private static final String  HTML_IS_STREAM               = "section\\-videodetail\"";
    private static Object        LOCK                         = new Object();

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String filename = null;
        String ext = null;
        String filesize = null;
        setBrowserExclusive();
        br.setCustomCharset("utf-8");
        this.br.setDebug(true);
        try {
            br.getPage(link.getDownloadURL());
        } catch (final BrowserException e) {
            if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 502) {
                link.getLinkStatus().setStatusText("We are sorry, but HellSpy is unavailable in your country");
                return AvailableStatus.UNCHECKABLE;
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = br.getRegex("<h1 title=\"([^<>\"]*?)\"").getMatch(0);
        if (br.containsHTML(HTML_IS_STREAM)) {
            /* Force extension */
            ext = ".mp4";
        } else {
            /* Filesize is only given for non-streams */
            filesize = br.getRegex("class=\"file\\-size\">([^<>\"]*?)<").getMatch(0);
            if (filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        if (ext != null) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        int maxchunks = FREE_MAXCHUNKS;
        String dllink = null;
        requestFileInformation(downloadLink);
        if (br.getHttpConnection().getResponseCode() == 502) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "We are sorry, but HellShare is unavailable in your country", 4 * 60 * 60 * 1000l);
        }
        dllink = checkDirectLink(downloadLink, "free_directlink");
        if (dllink == null) {
            if (br.containsHTML(HTML_IS_STREAM)) {
                dllink = getStreamDirectlink();
                /* No chunklimit for streams */
                maxchunks = 0;
            } else {
                /* No handling available for file links yet as all of them seem to be premium only! */
            }
        }
        if (dllink == null && ALL_PREMIUMONLY) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        br.setFollowRedirects(true);
        br.setReadTimeout(120 * 1000);
        /* Resume & unlimited chunks is definitly possible for stream links! */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, maxchunks);
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("Received html code insted of file");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("free_directlink", dllink);
        dl.startDownload();
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
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
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.setDebug(true);
                br.getPage("http://www.stiahnito.sk/?do=loginBox-loginpopup");
                String login_action = br.getRegex("\"(http://(?:www\\.)?hell\\-share\\.com/user/login/\\?do=apiLoginForm-submit[^<>\"]*?)\"").getMatch(0);
                if (login_action == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                login_action = Encoding.htmlDecode(login_action);
                br.postPage(login_action, "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&permanent_login=on&submit_login=P%C5%99ihl%C3%A1sit+se&login=1&redir_url=http%3A%2F%2Fwww.stiahnito.sk%2F%3Fdo%3DloginBox-login");
                String permLogin = br.getCookie(br.getURL(), "permlogin");
                if (permLogin == null || br.containsHTML("zadal jsi špatné uživatelské")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
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
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        final String trafficleft = br.getRegex("href=\"/ucet/credit\">[\t\n\r ]+(\\d+[\t\n\r ]+MB)[\t\n\r ]+</a>").getMatch(0);
        if (trafficleft == null) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        try {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            /* not available in old Stable 0.9.581 */
        }
        ai.setStatus("Premium Account");
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        int maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        requestFileInformation(link);
        login(account, false);
        String dllink = this.checkDirectLink(link, "premium_directlink");
        if (dllink == null) {
            if (br.containsHTML(HTML_IS_STREAM)) {
                dllink = getStreamDirectlink();
                /* No chunklimit for streams */
                maxchunks = 0;
            } else {
                br.getPage(link.getDownloadURL());
                br.setFollowRedirects(false);
                final String filedownloadbutton = br.getURL() + "?download=1&iframe_view=popup+download_iframe_detail_popup";
                br.getPage(filedownloadbutton);
                dllink = br.getRedirectLocation();
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replaceAll("\\\\", "");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    private String getStreamDirectlink() throws PluginException, IOException {
        String play_url = br.getRegex("\"(/[^<>\"]*?do=play)\"").getMatch(0);
        if (play_url == null) {
            logger.warning("Stream-play-url is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        play_url = Encoding.htmlDecode(play_url);
        this.br.getPage(play_url);
        this.br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        String dllink = br.getRegex("url: \"(http://stream\\d+\\.helldata\\.com[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Stream-finallink is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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