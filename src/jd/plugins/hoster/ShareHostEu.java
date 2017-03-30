//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharehost.eu" }, urls = { "https?://(www\\.)?sharehost\\.eu/[^<>\"]*?/(.*)" })
public class ShareHostEu extends PluginForHost {
    public ShareHostEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sharehost.eu/premium.html");
        // this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://sharehost.eu/tos.html";
    }

    private int           MAXCHUNKSFORFREE          = 1;
    private int           MAXCHUNKSFORPREMIUM       = 0;
    private String        MAINPAGE                  = "http://sharehost.eu";
    private String        PREMIUM_DAILY_TRAFFIC_MAX = "20 GB";
    private String        FREE_DAILY_TRAFFIC_MAX    = "10 GB";
    private static Object LOCK                      = new Object();

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        // API CALL: /fapi/fileInfo
        // Input:
        // url* - link URL (required)
        // filePassword - password for file (if exists)
        br.postPage(MAINPAGE + "/fapi/fileInfo", "url=" + downloadLink.getDownloadURL());
        // Output:
        // fileName - file name
        // filePath - file path
        // fileSize - file size in bytes
        // fileAvailable - true/false
        // fileDescription - file description
        // Errors:
        // emptyUrl
        // invalidUrl - wrong url format, too long or contauns invalid characters
        // fileNotFound
        // filePasswordNotMatch - podane hasło dostępu do pliku jest niepoprawne
        final String success = PluginJSonUtils.getJsonValue(br, "success");
        if ("false".equals(success)) {
            final String error = PluginJSonUtils.getJsonValue(br, "error");
            if ("fileNotFound".equals(error)) {
                return AvailableStatus.FALSE;
            } else if ("emptyUrl".equals(error)) {
                return AvailableStatus.UNCHECKABLE;
            } else {
                return AvailableStatus.UNCHECKED;
            }
        }
        String fileName = PluginJSonUtils.getJsonValue(br, "fileName");
        // String filePath = PluginJSonUtils.getJsonValue(br, "filePath");
        String fileSize = PluginJSonUtils.getJsonValue(br, "fileSize");
        String fileAvailable = PluginJSonUtils.getJsonValue(br, "fileAvailable");
        // String fileDescription = PluginJSonUtils.getJsonValue(br, "fileDescription");
        if ("true".equals(fileAvailable)) {
            if (fileName == null || fileSize == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            fileName = Encoding.htmlDecode(fileName.trim());
            downloadLink.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
            downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
            downloadLink.setAvailable(true);
        } else {
            downloadLink.setAvailable(false);
        }
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        String accountResponse;
        try {
            accountResponse = login(account, true);
        } catch (PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.setProperty("cookies", Property.NULL);
                final String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Maintenance")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, e.getMessage(), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE, e).localizedMessage(e.getLocalizedMessage());
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login failed: " + errorMessage, PluginException.VALUE_ID_PREMIUM_DISABLE, e);
                }
            } else {
                throw e;
            }
        }
        // API CALL: /fapi/userInfo
        // Input:
        // login*
        // pass*
        // Output:
        // userLogin - login
        // userEmail - e-mail
        // userIsPremium - true/false
        // userPremiumExpire - premium expire date
        // userTrafficToday - daily traffic left (bytes)
        // Errors:
        // emptyLoginOrPassword
        // userNotFound
        String userIsPremium = PluginJSonUtils.getJsonValue(accountResponse, "userIsPremium");
        String userPremiumExpire = PluginJSonUtils.getJsonValue(accountResponse, "userPremiumExpire");
        String userTrafficToday = PluginJSonUtils.getJsonValue(accountResponse, "userTrafficToday");
        long userTrafficLeft = Long.parseLong(userTrafficToday);
        ai.setTrafficLeft(userTrafficLeft);
        if ("true".equals(userIsPremium)) {
            ai.setProperty("premium", "true");
            // ai.setTrafficMax(PREMIUM_DAILY_TRAFFIC_MAX); // Compile error
            ai.setTrafficMax(SizeFormatter.getSize(PREMIUM_DAILY_TRAFFIC_MAX));
            ai.setStatus(getPhrase("PREMIUM"));
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
            try {
                if (userPremiumExpire != null) {
                    Date date = dateFormat.parse(userPremiumExpire);
                    ai.setValidUntil(date.getTime());
                }
            } catch (final Exception e) {
                logger.log(e);
            }
        } else {
            ai.setProperty("premium", "false");
            // ai.setTrafficMax(FREE_DAILY_TRAFFIC_MAX); // Compile error
            ai.setTrafficMax(SizeFormatter.getSize(FREE_DAILY_TRAFFIC_MAX));
            ai.setStatus(getPhrase("FREE"));
        }
        account.setValid(true);
        return ai;
    }

    private String login(final Account account, final boolean force) throws Exception, PluginException {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                // API CALL: /fapi/userInfo
                // Input:
                // login*
                // pass*
                br.postPage(MAINPAGE + "/fapi/userInfo", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                // Output:
                // userLogin - login
                // userEmail - e-mail
                // userIsPremium - true/false
                // userPremiumExpire - premium expire date
                // userTrafficToday - daily traffic left (bytes)
                // Errors:
                // emptyLoginOrPassword
                // userNotFound
                final String response = br.toString();
                if ("false".equals(PluginJSonUtils.getJsonValue(br, "success"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("INVALID_LOGIN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if ("true".equals(PluginJSonUtils.getJsonValue(br, "userIsPremium"))) {
                    account.setProperty("premium", "true");
                } else {
                    account.setProperty("premium", "false");
                }
                br.setFollowRedirects(true);
                br.postPage(MAINPAGE + "/index.php", "v=files%7Cmain&c=aut&f=login&friendlyredir=1&usr_login=" + Encoding.urlEncode(account.getUser()) + "&usr_pass=" + Encoding.urlEncode(account.getPass()));
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("cookies", cookies);
                return response;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        setMainPage(downloadLink.getDownloadURL());
        br.setCookiesExclusive(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setAcceptLanguage("pl-PL,pl;q=0.9,en;q=0.8");
        br.setFollowRedirects(false);
        String fileId = br.getRegex("<a href='/\\?v=download_free&fil=(\\d+)'>Wolne pobieranie</a>").getMatch(0);
        if (fileId == null) {
            fileId = br.getRegex("<a href='/\\?v=download_free&fil=(\\d+)' class='btn btn_gray'>Wolne pobieranie</a>").getMatch(0);
        }
        br.getPage(MAINPAGE + "/?v=download_free&fil=" + fileId);
        Form dlForm = br.getFormbyAction("http://sharehost.eu/");
        String dllink = "";
        for (int i = 0; i < 3; i++) {
            String waitTime = br.getRegex("var iFil=" + fileId + ";[\r\t\n ]+const DOWNLOAD_WAIT=(\\d+)").getMatch(0);
            sleep(Long.parseLong(waitTime) * 1000l, downloadLink);
            String captchaId = br.getRegex("<img src='/\\?c=aut&amp;f=imageCaptcha&amp;id=(\\d+)' id='captcha_img'").getMatch(0);
            String code = getCaptchaCode(MAINPAGE + "/?c=aut&f=imageCaptcha&id=" + captchaId, downloadLink);
            dlForm.put("cap_key", code);
            // br.submitForm(dlForm);
            br.postPage(MAINPAGE + "/", "v=download_free&c=dwn&f=getWaitedLink&cap_id=" + captchaId + "&fil=" + fileId + "&cap_key=" + code);
            if (br.containsHTML("Nie możesz w tej chwili pobrać kolejnego pliku")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("DOWNLOAD_LIMIT"), 5 * 60 * 1000l);
            } else if (br.containsHTML("Wpisano nieprawidłowy kod z obrazka")) {
                logger.info("wrong captcha");
            } else {
                dllink = br.getRedirectLocation();
                break;
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Can't find final download link/Captcha errors!", -1l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, MAXCHUNKSFORFREE);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<ul><li>Brak wolnych slotów do pobrania tego pliku. Zarejestruj się, aby pobrać plik.</li></ul>")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("NO_FREE_SLOTS"), 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    void setLoginData(final Account account) throws Exception {
        br.getPage(MAINPAGE);
        br.setCookiesExclusive(true);
        final Object ret = account.getProperty("cookies", null);
        final HashMap<String, String> cookies = (HashMap<String, String>) ret;
        if (account.isValid()) {
            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                final String key = cookieEntry.getKey();
                final String value = cookieEntry.getValue();
                this.br.setCookie(MAINPAGE, key, value);
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String downloadUrl = downloadLink.getPluginPatternMatcher();
        setMainPage(downloadUrl);
        br.setFollowRedirects(true);
        String loginInfo = login(account, false);
        boolean isPremium = "true".equals(account.getProperty("premium"));
        // long userTrafficleft;
        // try {
        // userTrafficleft = Long.parseLong(getJson(loginInfo, "userTrafficToday"));
        // } catch (Exception e) {
        // userTrafficleft = 0;
        // }
        // if (isPremium || (!isPremium && userTrafficleft > 0)) {
        requestFileInformation(downloadLink);
        if (isPremium) {
            // API CALLS: /fapi/fileDownloadLink
            // INput:
            // login* - login użytkownika w serwisie
            // pass* - hasło użytkownika w serwisie
            // url* - adres URL pliku w dowolnym z formatów generowanych przez serwis
            // filePassword - hasło dostępu do pliku (o ile właściciel je ustanowił)
            // Output:
            // fileUrlDownload (link valid for 24 hours)
            br.postPage(MAINPAGE + "/fapi/fileDownloadLink", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&url=" + downloadLink.getDownloadURL());
            // Errors
            // emptyLoginOrPassword - nie podano w parametrach loginu lub hasła
            // userNotFound - użytkownik nie istnieje lub podano błędne hasło
            // emptyUrl - nie podano w parametrach adresu URL pliku
            // invalidUrl - podany adres URL jest w nieprawidłowym formacie, zawiera nieprawidłowe znaki lub jest zbyt długi
            // fileNotFound - nie znaleziono pliku dla podanego adresu URL
            // filePasswordNotMatch - podane hasło dostępu do pliku jest niepoprawne
            // userAccountNotPremium - konto użytkownika nie posiada dostępu premium
            // userCanNotDownload - użytkownik nie może pobrać pliku z innych powodów (np. brak transferu)
            String success = PluginJSonUtils.getJsonValue(br, "success");
            if ("false".equals(success)) {
                if ("userCanNotDownload".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No traffic left!");
                } else if (("fileNotFound".equals(PluginJSonUtils.getJsonValue(br, "error")))) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            String finalDownloadLink = PluginJSonUtils.getJsonValue(br, "fileUrlDownload");
            setLoginData(account);
            String dllink = finalDownloadLink.replace("\\", "");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, MAXCHUNKSFORPREMIUM);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!" + "Response: " + dl.getConnection().getResponseMessage() + ", code: " + dl.getConnection().getResponseCode() + "\n" + dl.getConnection().getContentType());
                br.followConnection();
                logger.warning("br returns:" + br.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            MAXCHUNKSFORPREMIUM = 1;
            setLoginData(account);
            handleFree(downloadLink);
        }
    }

    private static AtomicBoolean yt_loaded = new AtomicBoolean(false);

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    void setMainPage(String downloadUrl) {
        if (downloadUrl.contains("https://")) {
            MAINPAGE = "https://sharehost.eu";
        } else {
            MAINPAGE = "http://sharehost.eu";
        }
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
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
        {
            put("INVALID_LOGIN", "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
            put("PREMIUM", "Premium User");
            put("FREE", "Free (Registered) User");
            put("LOGIN_FAILED_NOT_PREMIUM", "Login failed or not Premium");
            put("LOGIN_ERROR", "ShareHost.Eu: Login Error");
            put("LOGIN_FAILED", "Login failed!\r\nPlease check your Username and Password!");
            put("NO_TRAFFIC", "No traffic left");
            put("DOWNLOAD_LIMIT", "You can only download 1 file per 15 minutes");
            put("CAPTCHA_ERROR", "Wrong Captcha code in 3 trials!");
            put("NO_FREE_SLOTS", "No free slots for downloading this file");
        }
    };
    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
        {
            put("INVALID_LOGIN", "\r\nNieprawidłowy login/hasło!\r\nCzy jesteś pewien, że poprawnie wprowadziłeś nazwę użytkownika i hasło? Sugestie:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź nazwę użytkownika/hasło ręcznie, bez użycia funkcji Kopiuj i Wklej.");
            put("PREMIUM", "Użytkownik Premium");
            put("FREE", "Użytkownik zarejestrowany (darmowy)");
            put("LOGIN_FAILED_NOT_PREMIUM", "Nieprawidłowe konto lub konto nie-Premium");
            put("LOGIN_ERROR", "ShareHost.Eu: Błąd logowania");
            put("LOGIN_FAILED", "Logowanie nieudane!\r\nZweryfikuj proszę Nazwę Użytkownika i Hasło!");
            put("NO_TRAFFIC", "Brak dostępnego transferu");
            put("DOWNLOAD_LIMIT", "Można pobrać maksymalnie 1 plik na 15 minut");
            put("CAPTCHA_ERROR", "Wprowadzono 3-krotnie nieprawiłowy kod Captcha!");
            put("NO_FREE_SLOTS", "Brak wolnych slotów do pobrania tego pliku");
        }
    };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
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