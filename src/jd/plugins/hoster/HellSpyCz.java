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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hellspy.cz" }, urls = { "https?://(?:www\\.|porn\\.)?hellspy\\.(?:cz|com|sk)/(?:soutez/|sutaz/)?[a-z0-9\\-]+/\\d+" })
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
    private static final boolean ALL_PREMIUMONLY              = true;
    private static final String  HTML_IS_STREAM               = "section\\-videodetail\"";

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
        final String linkpart = new Regex(link.getPluginPatternMatcher(), "hellspy\\.[a-z0-9]+/(.+)").getMatch(0);
        if (linkpart != null) {
            link.setPluginPatternMatcher("https://www.hellspy.cz/" + linkpart);
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        String linkid_main = new Regex(link.getPluginPatternMatcher(), "hellspy\\.[a-z0-9]+/[^/]+/(\\d+)").getMatch(0);
        if (linkid_main != null) {
            final String linkid_archive = new Regex(link.getPluginPatternMatcher(), "relatedDownloadControl\\-(\\d+)").getMatch(0);
            if (linkid_archive != null) {
                linkid_main += "_" + linkid_archive;
            }
            return getHost() + "://" + linkid_main;
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
        final boolean isFile = br.containsHTML("lass=\"left section section\\-filedetail\"");
        final boolean isVideoStream = br.containsHTML("class=\"snippet--playerSn\"|do=play");
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!isFile && !isVideoStream) {
            /* No downloadable content --> Offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isPartFile(link.getPluginPatternMatcher())) {
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

    public static boolean isPartFile(final String url) {
        return new Regex(url, "[^<>\"]*pa?r?t?.?[0-9]+-rar").matches();
    }

    private boolean isVideoStream() {
        return br.containsHTML("class=\\\"snippet--playerSn\\\"|do=play|section-videodetail");
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
        if (this.isGEOBlocked()) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "We are sorry, but Hellspy is unavailable in your country", 4 * 60 * 60 * 1000l);
        }
        dllink = checkDirectLink(downloadLink, "free_directlink");
        if (dllink == null) {
            if (isVideoStream()) {
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
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        downloadLink.setProperty("free_directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                return null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return null;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        int maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        String dllink = checkDirectLink(link, "account_premium_directlink");
        if (dllink == null) {
            requestFileInformation(link);
            if (isGEOBlocked()) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "We are sorry, but HellSpy is unavailable in your country", 4 * 60 * 60 * 1000l);
            }
            login(account, false);
            br.getPage(link.getPluginPatternMatcher());
            if (br.containsHTML(HTML_IS_STREAM)) {
                dllink = getStreamDirectlink();
                /* No chunklimit for streams */
                maxchunks = 0;
            } else {
                final String filedownloadbutton;
                if (isPartFile(link.getPluginPatternMatcher())) {
                    final Regex urlRegex = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/[^/]+/(\\d+).+relatedDownloadControl\\-(\\d+).*?\\-uri=([^=\\&]+)");
                    final String idOfPart = urlRegex.getMatch(1);
                    final String urlStringOfPart = urlRegex.getMatch(2);
                    if (idOfPart == null || urlStringOfPart == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    filedownloadbutton = String.format("https://www.%s/%s/%s?download=1&iframe_view=popup+download_iframe_related_popup", this.getHost(), urlStringOfPart, idOfPart);
                } else {
                    final Regex urlRegex = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/([^/]+)/(\\d+)");
                    final String urlStringOfMainPart = urlRegex.getMatch(0);
                    final String idOfMainPart = urlRegex.getMatch(1);
                    if (urlStringOfMainPart == null || idOfMainPart == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    filedownloadbutton = String.format("https://www.%s/%s/%s?download=1&iframe_view=popup+download_iframe_related_popup", this.getHost(), urlStringOfMainPart, idOfMainPart);
                }
                final URLConnectionAdapter con = br.openGetConnection(filedownloadbutton);
                try {
                    if (!looksLikeDownloadableContent(con)) {
                        br.followConnection();
                        dllink = br.getRegex("launchFullDownload\\(\\'(https?[^<>\"\\']+)\\'\\);\"").getMatch(0);
                    } else {
                        dllink = filedownloadbutton;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                dllink = dllink.replaceAll("\\\\", "");
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, maxchunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty("account_premium_directlink", dllink);
        dl.startDownload();
    }

    private boolean isGEOBlocked() {
        return br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 502;
    }

    private String getStreamDirectlink() throws PluginException, IOException {
        String play_url = br.getRegex("\"(/[^<>\"]*?do=play)\"").getMatch(0);
        if (play_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        play_url = Encoding.htmlDecode(play_url);
        this.br.getPage(play_url);
        this.br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        String dllink = br.getRegex("url\\s*:\\s*\"(https?://[^\"\\']+\\.mp4[^\"\\']*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return dllink;
        }
    }

    /** TODO: Maybe add support for time-accounts */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("/ucet/");
        String traffic_left = br.getRegex("<td>Zakoupený:</td><td>(\\d+ MB)</td>").getMatch(0);
        if (traffic_left == null) {
            traffic_left = br.getRegex("<strong>Kreditů: </strong>([^<>\"]*?) \\&ndash; <a").getMatch(0);
        }
        if (traffic_left == null) {
            ai.setStatus("Invalid/Unknown");
        } else {
            ai.setTrafficLeft(SizeFormatter.getSize(traffic_left));
            ai.setValidUntil(-1);
            ai.setStatus("Account with credits");
        }
        return ai;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    return;
                }
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.setDebug(true);
                br.getPage("https://www." + account.getHoster() + "/?do=loginBox-loginpopup");
                String login_action = br.getRegex("\"(http://(?:www\\.)?[^/]+/user/login/\\?do=apiLoginForm-submit[^<>\"]*?)\"").getMatch(0);
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
                final String permLogin = br.getCookie(getHost(), "permlogin");
                if (permLogin == null || br.containsHTML("zadal jsi špatné uživatelské")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
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
        if (acc.getType() == AccountType.FREE) {
            /* free accounts can also have captchas */
            return true;
        }
        return false;
    }
}