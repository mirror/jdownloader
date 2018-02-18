//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import java.util.Locale;

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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freedisc.pl" }, urls = { "https?://(www\\.)?freedisc\\.pl/(#(!|%21))?[A-Za-z0-9\\-_]+,f-\\d+" })
public class FreeDiscPl extends PluginForHost {

    public FreeDiscPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://freedisc.pl/");
        this.setStartIntervall(1000);
        try {
            Browser.setRequestIntervalLimitGlobal("freedisc.pl", 250, 20, 60000);
        } catch (final Throwable e) {
        }
    }

    @Override
    public String getAGBLink() {
        return "http://freedisc.pl/regulations";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/#!", "/"));
    }

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
    private static final String  KNOWN_EXTENSIONS             = "asf|avi|flv|m4u|m4v|mov|mkv|mp4|mpeg4?|mpg|ogm|vob|wmv|webm";

    protected static Cookies     botSafeCookies               = new Cookies();

    private Browser prepBR(final Browser br) {
        prepBRStatic(br);

        synchronized (botSafeCookies) {
            if (!botSafeCookies.isEmpty()) {
                br.setCookies(this.getHost(), botSafeCookies);
            }
        }

        return br;
    }

    public static Browser prepBRStatic(final Browser br) {
        br.setAllowedResponseCodes(410);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        prepBR(this.br);
        br.getPage(link.getDownloadURL());
        if (isBotBlocked(this.br)) {
            return AvailableStatus.UNCHECKABLE;
        } else if (br.getRequest().getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Ten plik został usunięty przez użytkownika lub administratora|Użytkownik nie posiada takiego pliku|<title>404 error") || !br.getURL().contains(",f")) {
            /* Check this last as botBlocked also contains 404. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Handle no public files as offline
        if (br.containsHTML("Ten plik nie jest publicznie dostępny")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)</h2>").getMatch(0);
        // itemprop="name" style=" font-size: 17px; margin-top: 6px;">Alternatywne Metody Analizy technicznej .pdf</h1>
        if (filename == null) {
            filename = br.getRegex("itemprop=\"name\"( style=\"[^<>\"/]+\")?>([^<>\"]*?)</h1>").getMatch(1);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final String fpat = "\\s*([0-9]+(?:[\\.,][0-9]+)?\\s*[A-Z]{1,2})";
        String filesize = br.getRegex("class='frameFilesSize'>Rozmiar pliku</div>[\t\n\r ]+<div class='frameFilesCountNumber'>" + fpat).getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("Rozmiar pliku</div>[\t\n\r ]+<div class='frameFilesCountNumber'>" + fpat).getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("</i> Rozmiar pliku</div><div class='frameFilesCountNumber'>" + fpat).getMatch(0);
                if (filesize == null) {
                    filesize = br.getRegex("class='frameFilesCountNumber'>" + fpat).getMatch(0);
                    if (filesize == null) {
                        filesize = br.getRegex("<i class=\"icon-hdd\"></i>\\s*Rozmiar\\s*</div>\\s*<div class='value'>" + fpat).getMatch(0);
                    }
                }
            }
        }
        final String storedfileName = link.getName();
        String storedExt = "";
        if (storedfileName != null) {
            storedExt = storedfileName.substring(storedfileName.lastIndexOf(".") + 1);
        }
        if (link.getName() == null || (storedExt != null && !storedExt.matches(KNOWN_EXTENSIONS))) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (isBotBlocked(this.br)) {
            this.handleAntiBot(this.br);
            /* Important! Check status if we were blocked before! */
            requestFileInformation(downloadLink);
        }
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        final boolean isvideo = downloadLink.getBooleanProperty("isvideo", false);
        if (dllink != null) {
            /* Check if directlinks comes from a stream */
            if (isvideo) {
                /* Stream-downloads always have no limits! */
                resumable = true;
                maxchunks = 0;
            }
        } else {
            final boolean videostreamIsAvailable = br.containsHTML("rel=\"video_src\"");
            final String videoEmbedUrl = br.getRegex("<iframe src=\"(https?://freedisc\\.pl/embed/video/\\d+[^<>\"]*?)\"").getMatch(0);
            resumable = true;
            maxchunks = 0;
            final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
            postPageRaw("//freedisc.pl/download/payment_info", "{\"item_id\":\"" + fid + "\",\"item_type\":1,\"code\":\"\",\"file_id\":" + fid + ",\"no_headers\":1,\"menu_visible\":0}");
            br.getRequest().setHtmlCode(Encoding.unicodeDecode(this.br.toString()));
            if (br.containsHTML("Pobranie plików większych jak [0-9\\.]+ (MB|GB|TB), wymaga opłacenia kosztów transferu")) {
                logger.info("File is premiumonly --> Maybe stream download is possible!");
                /* Premiumonly --> But maybe we can download the video-stream */
                if (videostreamIsAvailable && videoEmbedUrl != null) {
                    logger.info("Seems like a stream is available --> Trying to find downloadlink");
                    /* Stream-downloads always have no limits! */
                    resumable = true;
                    maxchunks = 0;
                    getPage(videoEmbedUrl);
                    dllink = br.getRegex("data-video-url=\"(https?://[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("player\\.swf\\?file=(https?://[^<>\"]*?)\"").getMatch(0);
                    }
                    if (dllink != null) {
                        logger.info("Stream download handling seems to have worked successfully");
                        String ext = null;
                        final String currentFname = downloadLink.getName();
                        if (currentFname.contains(".")) {
                            ext = currentFname.substring(currentFname.lastIndexOf("."));
                        }
                        if (ext == null || (ext != null && ext.length() <= 5)) {
                            downloadLink.setFinalFileName(downloadLink.getName() + ".mp4");
                        } else if (ext.length() > 5) {
                            ext = dllink.substring(dllink.lastIndexOf(".") + 1);
                            if (ext.matches(KNOWN_EXTENSIONS)) {
                                downloadLink.setFinalFileName(downloadLink.getName() + "." + ext);
                            }
                        }
                        downloadLink.setProperty("isvideo", true);
                    } else {
                        logger.info("Stream download handling seems to have failed");
                    }
                }
                if (dllink == null) {
                    /* We failed to find a stream downloadlink so the file must be premium/free-account only! */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                }
            } else {
                String downloadUrlJson = PluginJSonUtils.getJsonNested(br, "download_data");
                dllink = PluginJSonUtils.getJsonValue(downloadUrlJson, "download_url") + PluginJSonUtils.getJsonValue(downloadUrlJson, "item_id") + "/" + PluginJSonUtils.getJsonValue(downloadUrlJson, "time");
                // dllink = "http://freedisc.pl/download/" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
                downloadLink.setProperty("isvideo", false);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (this.br.containsHTML("Ten plik jest chwilowo niedos")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            if (br.getURL().contains("freedisc.pl/pierrw,f-")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String server_filename = getFileNameFromDispositionHeader(dl.getConnection());
        if (server_filename != null) {
            server_filename = Encoding.htmlDecode(server_filename);
            downloadLink.setFinalFileName(server_filename);
        } else {
            final String urlName = getFileNameFromURL(dl.getConnection().getURL());
            if (downloadLink.getFinalFileName() == null && urlName != null && org.appwork.utils.Files.getExtension(urlName) != null) {
                downloadLink.setFinalFileName(downloadLink.getName() + org.appwork.utils.Files.getExtension(urlName));
            }
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    public static boolean isBotBlocked(final Browser br) {
        return br.containsHTML("Przez roboty internetowe nasze serwery się gotują|g-recaptcha");
    }

    private void getPage(final String url) throws Exception {
        br.getPage(url);
        handleAntiBot(this.br);
    }

    private void postPageRaw(final String url, final String parameters) throws Exception {
        this.br.postPageRaw(url, parameters);
        handleAntiBot(this.br);
    }

    private void handleAntiBot(final Browser br) throws Exception {
        if (isBotBlocked(this.br)) {
            /* Process anti-bot captcha */
            logger.info("Login captcha / spam protection detected");
            final DownloadLink originalDownloadLink = this.getDownloadLink();
            final DownloadLink downloadlinkToUse;
            try {
                if (originalDownloadLink != null) {
                    downloadlinkToUse = originalDownloadLink;
                } else {
                    /* E.g. for login process */
                    downloadlinkToUse = new DownloadLink(this, "Account Login " + this.getHost(), this.getHost(), MAINPAGE, true);
                }
                this.setDownloadLink(downloadlinkToUse);
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                br.postPage(br.getURL(), "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
                if (isBotBlocked(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Anti-Bot block", 5 * 60 * 1000l);
                }
            } finally {
                if (originalDownloadLink != null) {
                    this.setDownloadLink(originalDownloadLink);
                }
            }

            // save the session!
            synchronized (botSafeCookies) {
                botSafeCookies = br.getCookies(this.getHost());
            }
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://freedisc.pl";
    private static Object       LOCK     = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBR(br);
                br.setFollowRedirects(false);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* Always try to re-use cookies. */
                    br.setCookies(this.getHost(), cookies);
                    br.getPage("http://" + this.getHost() + "/");
                    if (br.containsHTML("id=\"btnLogout\"")) {
                        return;
                    }

                }
                Browser br = prepBR(new Browser());
                br.getPage("http://" + this.getHost() + "/");
                // this is done via ajax!
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Content-Type", "application/json");
                br.getHeaders().put("Cache-Control", null);
                br.postPageRaw("/account/signin_set", "{\"email_login\":\"" + account.getUser() + "\",\"password_login\":\"" + account.getPass() + "\",\"remember_login\":1,\"provider_login\":\"\"}");
                if (br.getCookie(MAINPAGE, "login_remember") == null && br.getCookie(MAINPAGE, "cookie_login_remember") == null) {
                    final String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędna nazwa użytkownika/hasło lub nie obsługiwany typ konta!\r\nSzybka pomoc:\r\nJesteś pewien, że poprawnie wprowadziłeś użytkownika/hasło?\r\nJeśli twoje hasło zawiera niektóre specjalne znaki - proszę zmień je (usuń) i wprowadź dane ponownie!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                handleAntiBot(this.br);
                /* Only free accounts are supported */
                account.setType(AccountType.FREE);
                account.saveCookies(br.getCookies(this.getHost()), "");
                // reload into standard browser
                this.br.setCookies(this.getHost(), account.loadCookies(""));
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        if (account.getType() == AccountType.FREE) {
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        } else {
            final String expire = br.getRegex("").getMatch(0);
            if (expire == null) {
                final String lang = System.getProperty("user.language");
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder nicht unterstützter Account Typ!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if ("pl".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędna nazwa użytkownika/hasło lub nie obsługiwany typ konta!\r\nSzybka pomoc:\r\nJesteś pewien, że poprawnie wprowadziłeś użytkownika/hasło?\r\nJeśli twoje hasło zawiera niektóre specjalne znaki - proszę zmień je (usuń) i wprowadź dane ponownie!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or unsupported account type!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        // br.getPage(link.getDownloadURL());
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                dllink = br.getRegex("").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String server_filename = getFileNameFromHeader(dl.getConnection());
            if (server_filename != null) {
                server_filename = Encoding.htmlDecode(server_filename);
                link.setFinalFileName(server_filename);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}