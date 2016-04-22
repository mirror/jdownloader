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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "devilshare.net" }, urls = { "https?://devilshare\\.net/view/([A-z0-9]+)" }, flags = { 2 })
public class DevilShareNet extends PluginForHost {

    public DevilShareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://devilshare.net/premium");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://devilshare.net/premium";
    }

    private int             MAXCHUNKSFORFREE             = 1;
    private int             MAXCHUNKSFORPREMIUM          = 0;
    private String          PREMIUM_DAILY_TRAFFIC_MAX    = "30GB";
    private String          REGISTERED_DAILY_TRAFFIC_MAX = "30GB";
    private String          MAINPAGE                     = "http://devilshare.net";
    protected final String  USE_API                      = "USE_API";
    protected final boolean default_USE_API              = true;
    private static Object   LOCK                         = new Object();

    private Account         currentAccount;

    private Account getCurrentAccount() {
        return currentAccount;
    }

    private void setCurrentAccount(Account currentAccount) {
        this.currentAccount = currentAccount;
    }

    private void prepareBrowser(Browser br) {
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "pl-PL,pl;q=0.8,en-US;q=0.6,en;q=0.4");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate, lzma");
        br.setReadTimeout(3 * 60 * 1000);
    }

    private String getFileId(String downloadURL) {
        String fileId = new Regex(downloadURL, MAINPAGE + "/view/([A-z0-9]+)/*?").getMatch(0);
        return fileId;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        final String dwnlURL = downloadLink.getPluginPatternMatcher();
        final boolean useAPI = getUseAPI();
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("devilshare.net");
        Account account = AccountController.getInstance().getValidAccount(hosterPlugin);
        // if called from handlePremium then
        // use the same account which is chosen by account usage rules
        Account curAccount = getCurrentAccount();
        if ((curAccount != null) && (!account.equals(curAccount))) {
            account = curAccount;
        }
        if (useAPI && account != null) {
            String fileId = getFileId(dwnlURL);

            // API CALL: http://devilshare.net/api/file/get/info
            // Input:
            // (string) token - User's API Token
            // (string) link - File's Link (identifier between the domain and name of the file url)

            // http://devilshare.net/view/55db799be65d4/Ballers.2015.S01E01.PL.HDTV.XviD-KiT.avi
            br.postPage(MAINPAGE + "/api/file/get/info", "token=" + getUserAPIToken(account) + "&link=" + fileId);
            // Output:
            // array
            // (
            // (string) name
            // (int) size - in bytes
            // (boolean) deleted
            // )
            // Errors:
            // array('type' => 'error', 'message' => X)
            // messages:
            // Token not found
            // File not found
            String error = getJson("error");
            if (error != null) {
                String errorMsg = getJson("message");
                return AvailableStatus.FALSE;
            } else {

                String fileName = getJson("name");
                String deleted = getJson("deleted");
                String fileSize = getJson("size");

                if ("true".equals(deleted)) {
                    downloadLink.setAvailable(false);
                    return AvailableStatus.FALSE;
                } else {
                    fileName = Encoding.htmlDecode(fileName.trim());
                    fileName = unescape(fileName);
                    downloadLink.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
                    downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
                    downloadLink.setAvailable(true);
                }

                if (!downloadLink.isAvailabilityStatusChecked()) {
                    return AvailableStatus.UNCHECKED;
                }
                if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            }
        } else {
            br.getPage(dwnlURL);
            if (this.br.containsHTML(">File Not Found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            String[][] fileInfo = new Regex(br, "<div class=\"large-12\" id=\"download\">[ \t\n\r\f]+?<h1>(.*)</h1>[ \t\n\r\f]+?<span> (.* [TGM]B)</span>[ \t\n\r\f]<div class=\"clear\"></div>").getMatches();
            if (fileInfo == null) {
                return AvailableStatus.UNCHECKABLE;
            } else {
                String fileName = fileInfo[0][0];
                String fileSize = fileInfo[0][1];
                downloadLink.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
                downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
                downloadLink.setAvailable(true);
                return AvailableStatus.TRUE;
            }
        }
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
                final String errorMessage = e.getErrorMessage();
                if (errorMessage != null && errorMessage.contains("Maintenance")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("LOGIN_ERROR"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                throw e;
            }
        }
        String userIsPremium = account.getProperty("premium").toString();
        boolean isPremium = "true".equals(userIsPremium);

        if (isPremium) {
            String userPremiumExpire = getJson(accountResponse, "expiration_date");
            if (userPremiumExpire == null) {
                userPremiumExpire = getJsonSub("expiration_date", "date", accountResponse);
            }
            String userTrafficToday = getJson(accountResponse, "limit");
            long userTrafficLeft = Long.parseLong(userTrafficToday);
            ai.setTrafficLeft(userTrafficLeft);

            ai.setProperty("premium", "true");
            ai.setTrafficMax(SizeFormatter.getSize(PREMIUM_DAILY_TRAFFIC_MAX));
            ai.setStatus(getPhrase("PREMIUM"));
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
            Date date;
            try {
                date = dateFormat.parse(userPremiumExpire);
                ai.setValidUntil(date.getTime());
            } catch (final Exception e) {
                logger.log(e);
            }
        } else {
            ai.setProperty("premium", "false");
            if (getUseAPI()) {
                String userTrafficToday = getJson(accountResponse, "limit");
                long userTrafficLeft = Long.parseLong(userTrafficToday);
                ai.setTrafficLeft(userTrafficLeft);

                ai.setTrafficMax(SizeFormatter.getSize(REGISTERED_DAILY_TRAFFIC_MAX));
            }
            ai.setStatus(getPhrase("FREE"));
        }
        account.setValid(true);
        return ai;
    }

    private String findAPIToken() throws Exception {
        br.getPage(MAINPAGE + "/settings");
        String APIToken = new Regex(br, "messageWindowTitle.html\\('Your API Token'\\);[ \t\n\r\f]+?messageWindowContent.html\\('([0-9A-z]+?)'\\);").getMatch(0);
        return APIToken;
    }

    private String login(final Account account, final boolean force) throws Exception, PluginException {

        synchronized (LOCK) {
            try {
                boolean forceLogin = force;
                String response = "";
                String userAPIToken = "";
                br.setCookiesExclusive(true);
                prepareBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                if (!forceLogin) {
                    userAPIToken = getUserAPIToken(account);
                    if (userAPIToken == null) {
                        forceLogin = true;
                    }
                }
                if (forceLogin) {

                    // first: Normal login to get User's API Token
                    br.getPage(MAINPAGE);
                    Form form = br.getFormbyAction("/login_check");
                    if (form == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("INVALID_FORM"));
                    }
                    // final InputField loginTokenInputField = form.getInputField("login%5B_token%5D");
                    final String loginToken = form.getInputField("login%5B_token%5D").getValue();
                    br.followConnection();
                    br.postPage(MAINPAGE + "/login_check", "_username=" + Encoding.urlEncode(account.getUser()) + "&_password=" + Encoding.urlEncode(account.getPass()) + "&login%5Blogin%5D=Login" + "&login%5B_token%5D=" + loginToken);
                    br.getPage(br.getRedirectLocation());
                    if (br.containsHTML("<span id=\"error-window-content\">Bad credentials.</span>")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("LOGIN_FAILED"));

                    }
                    userAPIToken = findAPIToken();
                    if (userAPIToken == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("LOGIN_FAILED"));
                    }
                } else {
                    userAPIToken = account.getProperty("userAPIToken").toString();
                }
                if (getUseAPI()) {
                    // API CALL: http://devilshare.net/api/user/get/info
                    // Input:
                    // (string) token - User's API Token
                    br.postPage(MAINPAGE + "/api/user/get/info", "token=" + userAPIToken);
                    // Output:
                    // array
                    // (
                    // (string) email
                    // (string) type - User's Account Type
                    // (datetime) expiration_date - User's Account Type Expiration Date
                    // (string) affiliation - User's Affiliation Type
                    // (float) income
                    // (int) files - User's Files Count
                    // (int) limit - User's Remaining Download Limit (bytes)
                    // (datetime) registration_date
                    // )
                    // Errors:
                    // array('type' => 'error', 'message' => X)
                    // messages:
                    // Token not found
                    // User not found
                    response = br.toString();
                    if ("error".equals(getJson(response, "type"))) {
                        throw new Exception(getJson(response, "message"));
                    }

                } else {
                    // {"email":"majsterjd@wp.pl","type":"premium","expiration_date":{"date":"2015-09-24 10:01:33.000000","timezone_type":3,
                    // "timezone":"Europe\/Warsaw"},"affiliation":"download","income":0,"files":"0","limit":"28517343442","registration_date":{"date":"2015-08-24
                    // 10:02:50.000000","timezone_type":3,"timezone":"Europe\/Warsaw"}}
                    String[][] accountInfo = new Regex(br, "<li>Logged as: <span>(.*?)</span></li>[ \t\n\r\f]+?<li>Account Type: <b>(.*?)</b>[ \t\n\r\f]+?\\([ \t\n\r\f]+?(\\d+) day[s]*?[ \t\n\r\f]+?\\)[ \t\n\r\f]+?</li>[ \t\n\r\f]+?<li>Transfer Limit: <b>(.*?)</b></li>").getMatches();
                    String type = "";
                    String expireDate = "";
                    String limit = "";
                    if (accountInfo.length == 0) {
                        accountInfo = new Regex(br, "<li>Logged as: <span>(.*?)</span></li>[ \t\n\r\f]+?<li>Account Type: <b>(.*?)</b>[ \t\n\r\f]+?</li>[ \t\n\r\f]+?<li>").getMatches();
                        type = accountInfo[0][1].toLowerCase();
                        // limit = REGISTERED_DAILY_TRAFFIC_MAX;
                        response = "{\"type\":\"" + type + "\"}";
                    } else {
                        type = accountInfo[0][1].toLowerCase();
                        expireDate = accountInfo[0][2];
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(new Date());
                        calendar.add(Calendar.DATE, Integer.parseInt(expireDate));
                        String reportDate = sdf.format(calendar.getTime());
                        limit = accountInfo[0][3];
                        response = "{\"type\":\"" + type + "\",\"expiration_date\":{\"date\":\"" + reportDate + " 00:00:00.000000\"}" + ",\"limit\":\"" + SizeFormatter.getSize(limit) + "\"}";
                    }

                }
                if ("free".equals(getJson(response, "type"))) {
                    account.setProperty("premium", "false");

                } else if ("premium".equals(getJson(response, "type"))) {
                    account.setProperty("premium", "true");
                } else {
                    account.setProperty("premium", "unknown");
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("userAPIToken", userAPIToken);
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
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String downloadUrl = downloadLink.getPluginPatternMatcher();
        setMainPage(downloadUrl);
        prepareBrowser(br);
        br.clearCookies(downloadUrl);
        String fileId = getFileId(downloadUrl);
        requestFileInformation(downloadLink);
        String dllink = "";
        String response = "";
        String error = "";
        String errorMessage = "";

        // check if we can use API with free account
        Account account = getCurrentAccount();
        if (account == null) {
            final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("devilshare.net");
            account = AccountController.getInstance().getValidAccount(hosterPlugin);
            // if called from handlePremium then
            // use the same account which is chosen by account usage rules
            Account curAccount = getCurrentAccount();
            if ((curAccount != null) && (!account.equals(curAccount))) {
                account = curAccount;
            }
        }
        if (getUseAPI() && account != null) {
            // API Calls the same as in premium
            br.postPage(MAINPAGE + "/api/file/download/prepare", "token=" + getUserAPIToken(account) + "&link=" + fileId + "&type=download");

            error = getJson("error");
            if (error != null) {
                errorMessage = getJson("message");
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, errorMessage);
            }

            try {
                br.postPage(MAINPAGE + "/api/file/download", "token=" + getUserAPIToken(account) + "&link=" + fileId);
            } catch (Exception e) {
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, e.getCause().getMessage());
            }

            dllink = br.getRedirectLocation();

        } else {
            // 1.check link
            if (account == null) {
                br.getPage(MAINPAGE + "/download/check/" + fileId + "/guest");
            } else {
                br.getPage(MAINPAGE + "/download/check/" + fileId + "/free");
            }
            response = br.toString();
            String errorType = getJson("type");
            if ("error".equals(errorType)) {
                errorMessage = getJson("message");
                if ("login".equals(errorMessage)) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("PREMIUM_REQUIRED"), -1l);
                }
            }
            error = getJson("test");
            if ("waiting_time".equals(error)) {
                String waitTime = getJson("message");
                Long waitTimeSeconds = Long.parseLong(waitTime);
                sleep(waitTimeSeconds * 1000l, downloadLink);
            }
            // 2. get download URL
            br.getPage(MAINPAGE + "/download/" + fileId);
            dllink = br.getRedirectLocation();

        }

        if (dllink == null) {
            error = getJson("type");
            if ("error".equals(error)) {
                errorMessage = getJson(error);
                if (errorMessage != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMessage, -1l);
                }
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("NO_FINAL_DLLINK"), -1l);
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

    private void setLoginData(final Account account) throws Exception {
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

    private String getUserAPIToken(Account account) {
        return account.getProperty("userAPIToken").toString();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {

        String loginInfo = login(account, true);

        boolean isPremium = "true".equals(account.getProperty("premium"));
        setCurrentAccount(account);

        if (isPremium) {
            String downloadUrl = downloadLink.getPluginPatternMatcher();
            String fileId = getFileId(downloadUrl);
            setMainPage(downloadUrl);
            br.setFollowRedirects(false);

            requestFileInformation(downloadLink);
            String finalDownloadLink = "";

            if (getUseAPI()) {
                // Download prepare: http://devilshare.net/api/file/download/prepare
                // INput:
                // (string) token - User's API Token
                // (string) link - File's Link (identifier between the domain and name of the file url)
                // (string) type - Download Type
                // Output:
                // array('type' => 'pass')
                //
                // Errors: array('type' => 'error', 'message' => X)
                // messages: Token not found, Invalid link, Invalid type, Proxy not found, No permission, Downloader does not exist,
                // Download
                // limit exceeded
                // Time to next download
                // (unlikely) register, login, upgrade
                //
                br.postPage(MAINPAGE + "/api/file/download/prepare", "token=" + getUserAPIToken(account) + "&link=" + fileId + "&type=download");

                String error = getJson("error");
                if (error != null) {
                    String errorMessage = getJson("message");
                    throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, errorMessage);
                }

                // Download: http://devilshare.net/api/file/download
                // Input:
                // (string) token - User's API Token
                // (string) link - File's Link (identifier between the domain and name of the file url)
                // Output:
                // A redirect response to file url
                // Errors:
                // array('type' => 'error', 'message' => X)
                // messages:
                // Token not found
                // Invalid link
                // No permission
                // Retry limit exceeded
                // No lock
                // Invalid lock

                try {
                    br.postPage(MAINPAGE + "/api/file/download", "token=" + getUserAPIToken(account) + "&link=" + fileId);
                } catch (Exception e) {
                    throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, e.getCause().getMessage());
                }

                finalDownloadLink = br.getRedirectLocation();
            } else {
                br.getPage(MAINPAGE + "/download/check/" + fileId + "/premium");
                String error = getJson("error");
                if (error != null) {
                    String errorMessage = getJson("message");
                    throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, errorMessage);
                }
                br.getPage(MAINPAGE + "/download/" + fileId);

                finalDownloadLink = br.getRedirectLocation();
            }
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

    private String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (!yt_loaded.getAndSet(true)) {
            JDUtilities.getPluginForHost("youtube.com");
        }
        return jd.nutils.encoding.Encoding.unescapeYoutube(s);
    }

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

    /*
     * *
     * Wrapper<br/> Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    private void setMainPage(String downloadUrl) {
        if (downloadUrl.contains("https://")) {
            MAINPAGE = "https://devilshare.net";
        } else {
            MAINPAGE = "http://devilshare.net";
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
            put("USE_API", "Use API (recommended!)");
            put("INVALID_LOGIN", "Can't find Login Form");
            put("INVALID_LOGIN", "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
            put("PREMIUM", "Premium User");
            put("FREE", "Free (Registered) User");
            put("LOGIN_FAILED_NOT_PREMIUM", "Login failed or not Premium");
            put("LOGIN_ERROR", "DevilShare.Net: Login Error");
            put("LOGIN_FAILED", "Login failed!\r\nPlease check your Username and Password!");
            put("NO_TRAFFIC", "No traffic left");
            put("DOWNLOAD_LIMIT", "You can only download 1 file per 15 minutes");
            put("CAPTCHA_ERROR", "Wrong Captcha code in 3 trials!");
            put("NO_FREE_SLOTS", "No free slots for downloading this file");
            put("PREMIUM_REQUIRED", "Free account can't no longer download files - you need Premium account");
            put("NO_FINAL_DLLINK", "Can't find final download link!");
        }
    };

    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
        {
            put("USE_API", "Używaj API (zalecane!)");
            put("INVALID_LOGIN", "Nie znaleziono formularza logowania!");
            put("INVALID_LOGIN", "\r\nNieprawidłowy login/hasło!\r\nCzy jesteś pewien, że poprawnie wprowadziłeś nazwę użytkownika i hasło? Sugestie:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź nazwę użytkownika/hasło ręcznie, bez użycia funkcji Kopiuj i Wklej.");
            put("PREMIUM", "Użytkownik Premium");
            put("FREE", "Użytkownik zarejestrowany (darmowy)");
            put("LOGIN_FAILED_NOT_PREMIUM", "Nieprawidłowe konto lub konto nie-Premium");
            put("LOGIN_ERROR", "DevilShare.Net: Błąd logowania");
            put("LOGIN_FAILED", "Logowanie nieudane!\r\nZweryfikuj proszę Nazwę Użytkownika i Hasło!");
            put("NO_TRAFFIC", "Brak dostępnego transferu");
            put("DOWNLOAD_LIMIT", "Można pobrać maksymalnie 1 plik na 15 minut");
            put("CAPTCHA_ERROR", "Wprowadzono 3-krotnie nieprawiłowy kod Captcha!");
            put("NO_FREE_SLOTS", "Brak wolnych slotów do pobrania tego pliku");
            put("PREMIUM_REQUIRED", "Nie można pobrać więcej plików na koncie darmowym - wymagane jest konto Premium");
            put("/", "Nie udało się określić linku pobierania!");
        }
    };

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), USE_API, JDL.L("plugins.hoster.DevilShareNet.useAPI", getPhrase("USE_API"))).setDefaultValue(default_USE_API));
    }

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

    private String getJsonSub(final String parameter, final String subParameter, final String source) {
        String result = "";
        try {
            result = new Regex(source, "\"" + parameter + "\":\\{\"" + subParameter + "\":\"(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\.\\d{6}\"").getMatch(0);
        } catch (Exception e) {
            // TODO: handle exception
            logger.info(e.getMessage());
        }
        return result;
    }

    private boolean getUseAPI() {
        return this.getPluginConfig().getBooleanProperty("USE_API", false);
    }
}