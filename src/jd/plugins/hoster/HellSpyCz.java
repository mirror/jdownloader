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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hellspy.cz" }, urls = { "https?://(?:www\\.|porn\\.)?hellspy\\.(?:cz|com|sk)/(?:soutez/|sutaz/)?[a-z0-9\\-]+/.+" })
public class HellSpyCz extends PluginForHost {
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
    private static final String  COOKIE_HOST                  = "https://www.hellspy.cz";
    private static final boolean ALL_PREMIUMONLY              = true;
    private static final String  HTML_IS_STREAM               = "section\\-videodetail\"";
    private static Object        LOCK                         = new Object();

    public HellSpyCz(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.hellspy.cz/registrace/");
        /* Especially for premium - don't make too many requests in a short time or we'll get 503 responses. */
        this.setStartIntervall(2000l);
    }

    @Override
    public String getAGBLink() {
        return "https://www.hellspy.cz/";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        final String linkpart = new Regex(link.getPluginPatternMatcher(), "hellspy\\.[a-z0-9]+/(.+)").getMatch(1);
        link.setPluginPatternMatcher("https://www.hellspy.cz/" + linkpart);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        String linkid_main = new Regex(link.getPluginPatternMatcher(), "hellspy\\.[a-z0-9]+/[^/]+/(\\d+)").getMatch(0);
        if (linkid_main != null) {
            final String linkid_archive = new Regex(link.getPluginPatternMatcher(), "relatedDownloadControl\\-(\\d+)").getMatch(0);
            if (linkid_archive != null) {
                linkid_main += "_" + linkid_archive;
            }
            return linkid_main;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String filename = null;
        String ext = null;
        String filesize = null;
        setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getHeaders().put("Accept-Language", "en-gb;q=0.9, en;q=0.8");
        br.setFollowRedirects(true);
        this.br.setDebug(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (new Regex(link.getPluginPatternMatcher(), "[^<>\"]*pa?r?t?.?[0-9]+-rar").matches()) {
            /* 2018-11-12: For multi-part-URLs */
            return AvailableStatus.TRUE;
        }
        filename = br.getRegex("<h1 title=\"([^<>\"]*?)\"").getMatch(0);
        if (br.containsHTML(HTML_IS_STREAM)) {
            /* Force extension */
            ext = ".mp4";
        } else {
            /* Filesize is only given for non-streams */
            filesize = br.getRegex("<span class=\"filesize right\">(\\d+(?:\\.\\d+)? <span>[^<>\"]*?)</span>").getMatch(0);
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
        if (filesize != null) {
            filesize = filesize.replace("<span>", "");
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace("&nbsp;", "")));
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) {
        if (br.containsHTML(">Soubor nenalezen<") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
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
        int maxchunks = FREE_MAXCHUNKS;
        String dllink = null;
        requestFileInformation(downloadLink);
        if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 502) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "We are sorry, but Hellspy is unavailable in your country", 4 * 60 * 60 * 1000l);
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
            throw new AccountRequiredException();
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

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        int maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        String dllink = null;
        requestFileInformation(downloadLink);
        if (br.getHttpConnection().getResponseCode() == 502) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "We are sorry, but HellSpy is unavailable in your country", 4 * 60 * 60 * 1000l);
        }
        login(account, false);
        br.getPage(downloadLink.getDownloadURL());
        dllink = checkDirectLink(downloadLink, "account_premium_directlink");
        if (dllink == null) {
            if (br.containsHTML(HTML_IS_STREAM)) {
                dllink = getStreamDirectlink();
                /* No chunklimit for streams */
                maxchunks = 0;
            } else {
                final String filedownloadbutton = br.getURL() + "?download=1&iframe_view=popup+download_iframe_detail_popup";
                URLConnectionAdapter con = openConnection(this.br, filedownloadbutton);
                if (con.getContentType().contains("html")) {
                    br.followConnection();
                    dllink = br.getRegex("launchFullDownload\\(\\'(http[^<>\"]*?)\\'\\);\"").getMatch(0);
                } else {
                    dllink = filedownloadbutton;
                }
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replaceAll("\\\\", "");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, maxchunks);
        if (dl.getConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
        }
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
        downloadLink.setProperty("account_premium_directlink", dllink);
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

    /** TODO: Maybe add support for time-accounts */
    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            ai.setStatus("Login failed");
            account.setValid(false);
            throw e;
        }
        br.getPage("/ucet/");
        String traffic_left = br.getRegex("<td>Zakoupený:</td><td>(\\d+ MB)</td>").getMatch(0);
        if (traffic_left == null) {
            traffic_left = br.getRegex("<strong>Kreditů: </strong>([^<>\"]*?) \\&ndash; <a").getMatch(0);
        }
        if (traffic_left == null) {
            ai.setStatus("Invalid/Unknown");
            account.setValid(false);
            return ai;
        }
        ai.setTrafficLeft(SizeFormatter.getSize(traffic_left));
        ai.setValidUntil(-1);
        ai.setStatus("Account with credits");
        account.setProperty("free", false);
        return ai;
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force) throws Exception {
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
                br.getPage("http://www.hellspy.cz/?do=loginBox-loginpopup");
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
                br.postPage(login_action, "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&permanent_login=on&submit_login=P%C5%99ihl%C3%A1sit+se&login=1&redir_url=http%3A%2F%2Fwww.hellspy.cz%2F%3Fdo%3DloginBox-login");
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

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        con = br.openGetConnection(directlink);
        return con;
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