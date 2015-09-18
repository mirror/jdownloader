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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "5fantastic.pl" }, urls = { "http://(?:www\\.)?5fantastic\\.pl/[^/]+(/|-)\\d+\\-\\d+[^/]+" }, flags = { 0 })
public class FiveFantasticPl extends PluginForHost {
    private static Object        LOCK                         = new Object();
    private String               HOSTER                       = "http://www.5fantastic.pl";
    private final String         FINAL_FILENAME               = "FINAL_FILENAME";

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = -1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;

    public FiveFantasticPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(HOSTER + "/index.aspx");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return HOSTER + "/strona.aspx?gidart=4558";
    }

    private boolean default_final_filename = true;

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), FINAL_FILENAME, JDL.L("plugins.hoster.5fantasticpl.finalfilename", getPhrase("FINAL_FILENAME"))).setDefaultValue(default_final_filename));
    }

    private void prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate, lzma, sdch");
        br.setAllowedResponseCodes(400);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBR(this.br);
        final String downloadUrl = link.getDownloadURL();

        try {
            br.getPage(downloadUrl);
        } catch (Exception e) {
            final String errorMessage = e.getCause().getMessage();
            final String newName = getPhrase("LINK_UNCHECKABLE");
            if (!link.getName().contains(newName)) {
                link.setName(link.getName() + " " + newName);
            }
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);

            if (errorMessage.contains("Content-length too big")) {
                try {
                    link.setComment(getPhrase("FOLDERS_NOT_SUPPORTED"));
                } catch (final Throwable t) {
                }
                logger.severe("FiveFantastic: the link: " + downloadUrl + " seems to be folder. Folders are not supported!");
                throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("FOLDERS_NOT_SUPPORTED"));
            }
            try {
                link.setComment(getPhrase("TRYING_TO_GET_PAGE") + downloadUrl + getPhrase("GOT_ERROR") + errorMessage);
            } catch (final Throwable t) {
            }

            logger.severe("FiveFantastic: trying to get page for link: " + downloadUrl + ",  got error: " + errorMessage);
            throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("ERROR") + errorMessage);
        }

        /* Search page --> File offline */
        if (this.br.containsHTML("bliczna/wyszukaj\\.png")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        /* DB issue / offline */
        if (this.br.getHttpConnection().getResponseCode() == 400) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        if (br.containsHTML("Plik jest plikiem prywatnym.")) {
            // check if the link is user's private file
            // only logged user has access to his private files
            List<Account> accs = AccountController.getInstance().getValidAccounts(this.getHost());
            boolean isPremium = false;
            Account premiumAccount = null;
            if (accs == null || accs.size() == 0) {
                logger.info("No account present.");

            } else {
                for (Account account : accs) {
                    if (account.getProperty("Premium") == "T") {
                        premiumAccount = account;
                        isPremium = true;
                        break;
                    }
                }
            }

            if (isPremium) {
                try {
                    login(premiumAccount, true);
                } catch (Exception e) {
                    logger.info("5Fantastic Premium Error: Login failed or not Premium!");

                }
            }
            br.getPage(downloadUrl);

            if (br.containsHTML("Plik jest plikiem prywatnym.")) {
                try {
                    link.setComment(getPhrase("PRIVATE_FILE"));
                } catch (final Throwable t) {
                }
                logger.info("FiveFantastic: " + link.getDownloadURL() + " is private file - download impossible!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, getPhrase("FILE_ERROR"));
            }
        }

        if (br.containsHTML("<title>[\t\n\r ]+5fantastic\\.pl \\- najlepszy darmowy dysk internetowy \\- pliki udostępnione przez klubowiczów[\t\n\r ]+</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, getPhrase("FILE_ERROR"));
        }
        final String filename = br.getRegex("<title>([^<>\"]*?) \\- 5fantastic\\.pl</title>").getMatch(0);
        final String filesize = br.getRegex(">Rozmiar: </span><span style=\"[^\"]+\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        final boolean preferFinalFilename = this.getPluginConfig().getBooleanProperty("FINAL_FILENAME", true);
        if (!preferFinalFilename) {
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        } else {
            Form freeform = this.br.getFormByInputFieldKeyValue("free", "true");
            if (freeform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.submitForm(freeform);
            if (this.br.getURL().contains("error-08.html")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            this.br.cloneBrowser().getPage("/account/cookie");
            freeform = this.br.getFormByInputFieldKeyValue("free", "true");
            if (this.br.getFormByInputFieldKeyValue("free", "false") != null) {
                /* Hmmm either premiumonly-file or reconnect needed */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            if (freeform == null) {
                String waitTime = new Regex(br, "Za darmo możesz pobierać tylko raz na godzinę, czekaj (\\d+) minut[y]*?, lub skorzystaj z PREMIUM").getMatch(0);
                if (waitTime != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("ONLY_ONE_PER_HOUR"), Integer.parseInt(waitTime) * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            int seconds = 600;
            final String seconds_regexed = this.br.getRegex("id=\"timer\">(\\d+)<").getMatch(0);
            if (seconds_regexed != null) {
                seconds = Integer.parseInt(seconds_regexed);
            }
            this.sleep(seconds * 1001l, downloadLink);
            this.br.setFollowRedirects(true);
            // this.br.submitForm(freeform);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, freeform, FREE_RESUME, FREE_MAXCHUNKS);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            /* TODO: Check if these errormessages still exist */
            if (br.containsHTML("Z Twojego numeru IP jest już pobierany plik. Poczekaj na zwolnienie zasobów, albo ściągnij plik za punkty")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("DOWNLOAD_DETECTED"), 5 * 60 * 1000l);
            }
            if (br.containsHTML("jest aktualnie pobierany inny plik")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("DOWNLOAD_DETECTED"), 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dl.getConnection().getURL().toString());
        fixFilename(downloadLink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
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
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }

        final String remaining_traffic_mb = getJson("PointsCount");

        account.setValid(true);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);

        ai.setTrafficLeft(Long.parseLong(remaining_traffic_mb) * 1024 * 1024);
        // save property for checking availability of private files
        account.setProperty("Premium", "T");
        /* Set account type based on traffic left - I have no idea whether there might be a better way to do this ... */
        if (ai.getTrafficLeft() == 0) {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
        }
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        final String user = Encoding.urlEncode(account.getUser());
        final String pwd = Encoding.urlEncode(account.getPass());
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBR(this.br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = user.equals(account.getStringProperty("name", user));
                if (acmatch) {
                    acmatch = pwd.equals(account.getStringProperty("pass", pwd));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(HOSTER, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);

                this.br.getPage("http://5fantastic.pl/account/loginajax?t=" + System.currentTimeMillis());
                final String token = this.br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (token == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                this.br.postPage("/account/loginajax", "login=" + user + "&password=" + pwd + "&__RequestVerificationToken=" + token);
                if (this.br.getCookie(HOSTER, ".ASPXAUTH") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final String loggedin = getJson("IsLogged");
                this.br.postPage("/account/user", "");
                if ("false".equals(loggedin)) {
                    /* Free accounts are not yet supported */
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(HOSTER);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", user);
                account.setProperty("pass", pwd);
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    /** TODO: Fix premium download! */
    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        login(account, true);
        this.br.setFollowRedirects(true);
        final String dllink = checkDirectLink(downloadLink, "directlink_account_premium");
        if (dllink != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        } else {
            this.br.getPage(downloadLink.getDownloadURL());
            final String fileId = get_fileId(downloadLink);
            final String userId = get_userId(downloadLink);
            final String postData = "fileId=" + fileId + "&userId=" + userId + "&free=false";

            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, this.br.getURL(), postData, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("jest aktualnie pobierany inny plik")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("DOWNLOAD_DETECTED"), 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        downloadLink.setProperty("directlink_account_premium", dl.getConnection().getURL().toString());
        fixFilename(downloadLink);
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    private String get_userId(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "5fantastic\\.pl/klubowicze(?:\\-|/)(\\d+)\\-(\\d+).+").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    private String get_fileId(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "5fantastic\\.pl/klubowicze(?:\\-|/)(\\d+)\\-(\\d+).+").getMatch(1);
    }

    private void fixFilename(final DownloadLink dlink) {
        /* Hoster tags filenames --> Remove that! */
        String server_filename = getFileNameFromHeader(dl.getConnection());
        server_filename = server_filename.replace("._5fantastic.pl_", "");
        dlink.setFinalFileName(server_filename);
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
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("FOLDERS_NOT_SUPPORTED", "The link seems to be folder. Folders are not supported.");
                                                      put("LINK_UNCHECKABLE", "(Link uncheckable - read Comment column)");
                                                      put("TRYING_TO_GET_PAGE", "FiveFantastic: trying to get page for link:");
                                                      put("GOT_ERROR", ",  got error: ");
                                                      put("ERROR", "Error: ");
                                                      put("PRIVATE_FILE", "This is private file - download impossible");
                                                      put("FILE_ERROR", "File not found or private!");
                                                      put("TEMPORARY_UNAVAILABLE", "Link temporary unavailable!");
                                                      put("DOWNLOAD_DETECTED", "Download in progress detected from your IP!");
                                                      put("PREMIUM_ERROR", "5Fantastic Premium Error");
                                                      put("LOGIN_FAILED", "Login failed or not Premium!\r\nPlease check your Username and Password!");
                                                      put("PREMIUM_INVALID", "Premium Account is invalid or not recognized!");
                                                      put("NO_TRAFFIC_LEFT", "No Premium traffic left!");
                                                      put("NO_PREMIUM", "Login failed or not Premium!");
                                                      put("PREMIUM_USER", "Premium User with traffic limit: ");
                                                      put("FINAL_FILENAME", "Use default final filename (ON) or use final filename from the web page (off)");
                                                      put("ONLY_ONE_PER_HOUR", "With free mode you can only download one file per hour");
                                                  }
                                              };

    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("FOLDERS_NOT_SUPPORTED", "Wybrany link jest folderem. Foldery nie są obsługiwane");
                                                      put("LINK_UNCHECKABLE", "(Link nieweryfikowalny - sprawdź kolumnę Komentarz)");
                                                      put("TRYING_TO_GET_PAGE", "FiveFantastic: próba pobrania strony dla linku:");
                                                      put("GOT_ERROR", ",  zwrócony błąd: ");
                                                      put("ERROR", "Błąd: ");
                                                      put("PRIVATE_FILE", "Plik prywatny - pobieranie niemożliwe");
                                                      put("FILE_ERROR", "Plik nie znaleziony lub plik prywatny!");
                                                      put("TEMPORARY_UNAVAILABLE", "Link chwilowo niedostępny!");
                                                      put("DOWNLOAD_DETECTED", "Wykryto trwające pobieranie z twojego adresu IP!");
                                                      put("PREMIUM_ERROR", "5Fantastic: Błąd Konta Premium");
                                                      put("LOGIN_FAILED", "Błąd logowania lub konto nie jest Premium!\r\nSprawdź dane logowania: login/hasło!");
                                                      put("PREMIUM_INVALID", "Nieprawidłowe lub nierozpoznane konto Premium!");
                                                      put("NO_TRAFFIC_LEFT", "Brak dostępnego transferu Premium!");
                                                      put("NO_PREMIUM", "Błędny login lub brak Premium!");
                                                      put("PREMIUM_USER", "Użytkownik Premium z limitem: ");
                                                      put("FINAL_FILENAME", "Użyj domyślnej finalnej nazwy pliku (WŁ) lub ustaw finalną nazwę pliku na podstawie nazwy ze strony www (WYŁ)");
                                                      put("ONLY_ONE_PER_HOUR", "W trybie free możesz pobrać tylko 1 plik na godzinę");
                                                  }
                                              };

    /**
     * Returns a Polish/English translation of a phrase. We don't use the JDownloader translation framework since we need only Polish and
     * English.
     * 
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("pl".equals(System.getProperty("user.language")) && phrasesPL.containsKey(key)) {
            return phrasesPL.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }
}