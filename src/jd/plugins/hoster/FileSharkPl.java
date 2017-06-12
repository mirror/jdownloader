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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshark.pl" }, urls = { "https?://(www\\.)?fileshark\\.pl/pobierz/(\\d+)/(.+)" })
public class FileSharkPl extends PluginForHost {
    public FileSharkPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.fileshark.pl/premium/kup");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://www.fileshark.pl/strona/regulamin";
    }

    private static final String DAILY_LIMIT                     = "30 GB";
    private static final String POLAND_ONLY                     = ">Strona jest dostępna wyłącznie dla użytkowników znajdujących się na terenie Polski<";
    protected final String      USE_API                         = "USE_API";
    protected final boolean     default_USE_API                 = true;
    private static final short  API_CALL_ACCOUNT_GET_DETAILS    = 0;
    private static final short  API_CALL_FILE_GET_DETAILS       = 1;
    private static final short  API_CALL_FILE_GET_DOWNLOAD_LINK = 2;
    private Account             currentAccount;

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(Account currentAccount) {
        this.currentAccount = currentAccount;
    }

    private long checkForErrors() throws PluginException {
        if (br.containsHTML("Osiągnięto maksymalną liczbę sciąganych jednocześnie plików.")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("MAX_DOWNLOAD"), 60 * 60 * 1000l);
        }
        if (br.containsHTML("Plik nie został odnaleziony w bazie danych.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("<li>Trwa pobieranie pliku. Możesz pobierać tylko jeden plik w tym samym czasie.</li>")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("OTHER_FILE_DOWNLOAD"), 5 * 60 * 1000l);
        }
        String dailyLimitWarning = br.getRegex("<p class=\"lead text-center alert alert-warning\">(Dzienny limi[t]*? nie pozwala na pobranie pliku. )").getMatch(0);
        if (dailyLimitWarning != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Hoster reports: " + dailyLimitWarning, 60 * 60 * 1000l);
        }
        if (br.containsHTML("Kolejne pobranie możliwe za") || br.containsHTML("Proszę czekać. Pobieranie będzie możliwe za")) {
            String waitTime = br.getRegex("Kolejne pobranie możliwe za <span id=\"timeToDownload\">(\\d+)</span>").getMatch(0);
            if (waitTime == null) {
                waitTime = br.getRegex("Pobieranie będzie możliwe za <span id=\"timeToDownload\">(\\d+)</span>").getMatch(0);
            }
            if (waitTime != null) {
                return Long.parseLong(waitTime) * 1000l;
            }
        }
        return 0l;
    }

    private String getErrorMessage(String source, int errorCode) {
        if (errorCode > 0) {
            //
            // 2 => 'Wrong method',
            // 10 => 'Missing login or password',
            // 11 => 'User is not authenticated',
            // 20 => 'Missing file id, token or name',
            // 21 => 'No file found',
            // 22 => 'Missing file id or token',
            // 23 => 'File is deleted',
            // 24 => 'DMCA',
            // 30 => 'Only for users in selected countries',
            // 31 => 'Free download available only for countries: Poland',
            // 32 => 'You must wait for new download',
            // 33 => 'Daily limit has been reached',
            // 34 => 'Max number of active downloads has been reached'
            return PluginJSonUtils.getJsonValue(source, "errorMessage");
        }
        return "Unknown error";
    }

    /*
     * callType = 0 - user info 1 - get file details 2 - get download link
     */
    boolean getAPICall(DownloadLink downloadLink, short callType, String userName, String userPassword) throws IOException {
        if (callType > API_CALL_ACCOUNT_GET_DETAILS) {
            String fileData[][] = new Regex(downloadLink.getPluginPatternMatcher(), "https?://(www\\.)?fileshark\\.pl/pobierz/((\\d+)/([0-9a-zA-Z]+)/?)").getMatches();
            if (fileData.length > 0) {
                String fileId = fileData[0][2];
                String fileToken = fileData[0][3];
                if (fileId == null || fileToken == null) {
                    return (false);
                } else {
                    if (callType == API_CALL_FILE_GET_DETAILS) {
                        br.postPage("https://fileshark.pl/api/file/getDetails", "id=" + fileId + "&token=" + fileToken);
                    } else {
                        if (userName != null && userPassword != null) {
                            br.postPage("https://fileshark.pl/api/file/getDownloadLink", "id=" + fileId + "&token=" + fileToken + "&username=" + userName + "&password=" + userPassword);
                        } else {
                            br.postPage("https://fileshark.pl/api/file/getDownloadLink", "id=" + fileId + "&token=" + fileToken);
                        }
                    }
                }
            } else {
                return (false);
            }
        } else {
            br.postPage("https://fileshark.pl/api/account/getDetails", "username=" + userName + "&password=" + userPassword);
        }
        if (br.containsHTML("404 Nie znaleziono strony:")) {
            return (false);
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String fileName = "";
        String fileSize = "";
        boolean useAPI = getUseAPI();
        if (useAPI) {
            // API CALL - file info: fileshark.pl/api/file/getDetails?id={id}&token={token}
            // POST
            // http://www.fileshark.pl/pobierz/{id}/{token}/
            // {id} - file id
            // {token} - file token
            useAPI = getAPICall(link, API_CALL_FILE_GET_DETAILS, null, null);
            if (useAPI) {
                // Output (json):
                // success: (bool)
                // data: (array)
                // fileName: (string) : file name
                // isDownloadable: (bool) : available for download
                // isDeleted: (bool) : file deleted
                // isDmcaRequested: (bool) : DMCA registered
                // fileSize: (int) : file siez (bytes)
                // fileUrl: (string) : download URL
                // isPremium: (bool) : Available only for Premium
                // createdAt: (datetime) : added date
                // contentType: (string) : file type
                if ("true".equals(PluginJSonUtils.getJsonValue(br, "success"))) {
                    fileName = PluginJSonUtils.getJsonValue(br, "fileName");
                    fileSize = PluginJSonUtils.getJsonValue(br, "fileSize");
                    String isDownloadable = PluginJSonUtils.getJsonValue(br, "isDownloadable");
                    String isDeleted = PluginJSonUtils.getJsonValue(br, "isDeleted");
                    String isDmcaRequested = PluginJSonUtils.getJsonValue(br, "isDmcaRequested");
                    String isPremium = PluginJSonUtils.getJsonValue(br, "isPremium");
                    if ("false".equals(isDownloadable)) {
                        link.getLinkStatus().setStatusText(getPhrase("NOT_DOWNLOADABLE"));
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if ("true".equals(isDeleted)) {
                        link.getLinkStatus().setStatusText(getPhrase("FILE_DELETED"));
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if ("true".equals(isDmcaRequested)) {
                        link.getLinkStatus().setStatusText(getPhrase("DMCA_REQUEST"));
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if ("true".equals(isPremium)) {
                        link.setProperty("PREMIUM", "TRUE");
                    } else {
                        link.setProperty("PREMIUM", "FALSE");
                    }
                } else
                    // try without API
                {
                    useAPI = false;
                }
            }
        }
        if (!useAPI) {
            // bug at the server side:
            // if the user finished downloads then the next download link doesn't
            // display info about the link but message with the time for the next download
            // So checking the online stat simply requires login to the site! (just like
            // checking for Premium user)
            // This is stupid because after user puts the link from LinkGrabber -> Downloads
            // then again it is checked (for Premium user) and again log procedure is
            // required to set cookies for downloads...
            // So the correct names and filesizes are set at the download time...
            // Informed them about this bug, hope they will correct it, next download
            // time should be displayed just when the user tries to start the download (button)
            // not at the time when the link is displayed.
            // for Premium only ! Read description above!
            final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("fileshark.pl");
            Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
            // if called from handlePremium then
            // use the same account which is chosen by account usage rules
            Account curAccount = getCurrentAccount();
            if ((curAccount != null) && (!aa.equals(curAccount))) {
                aa = curAccount;
            }
            if (aa != null) {
                try {
                    login(aa, true);
                } catch (Exception e) {
                }
            }
            br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            if (br.containsHTML(POLAND_ONLY)) {
                link.getLinkStatus().setStatusText(getPhrase("POLAND_ONLY"));
                return AvailableStatus.UNCHECKABLE;
            } else if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("warning\">Strona jest")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            fileName = br.getRegex("<h2[ \n\t\t\f]+class=\"name-file\">([^<>\"]*?)</h2>").getMatch(0);
            fileSize = br.getRegex("<p class=\"size-file\">Rozmiar: <strong>(.*?)</strong></p>").getMatch(0);
            if (fileName == null || fileSize == null) {
                long waitTime = checkForErrors();
                if (waitTime != 0) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("WAITTIME"), waitTime);
                }
            }
        }
        link.setName(Encoding.htmlDecode(fileName.trim()));
        link.setDownloadSize(SizeFormatter.getSize(fileSize));
        link.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        AvailableStatus as = requestFileInformation(downloadLink);
        if (as != AvailableStatus.FALSE) {
            if (!getUseAPI()) {
                br.setFollowRedirects(false);
                br.getPage(downloadLink.getDownloadURL());
                if (br.containsHTML(POLAND_ONLY)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("POLAND_ONLY"));
                }
            }
            doFree(downloadLink);
        }
    }

    public static void saveCaptchaImage(final File file, final byte[] data) throws IOException {
        if (file.isFile()) {
            if (file.exists() && !file.delete()) {
                throw new IOException("Could not overwrite file: " + file);
            }
        }
        final File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        file.createNewFile();
        FileOutputStream fos = null;
        InputStream input = null;
        boolean okay = false;
        try {
            fos = new FileOutputStream(file, false);
            final int length = data.length;
            fos.write(data, 0, length);
            okay = length > 0;
        } finally {
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            if (okay == false) {
                file.delete();
            }
        }
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String dllink = "";
        boolean useAPI = getUseAPI();
        if (useAPI) {
            boolean waitTimeDetected = true;
            byte trials = 0;
            do {
                useAPI = getAPICall(downloadLink, API_CALL_FILE_GET_DOWNLOAD_LINK, null, null);
                String response = br.toString();
                if ("false".equals(PluginJSonUtils.getJsonValue(response, "success"))) {
                    int errorCode = Integer.parseInt(PluginJSonUtils.getJsonValue(response, "errorCode"));
                    String errorMessage = getErrorMessage(response, errorCode);
                    if (errorCode == 32) {
                        if (trials < 1) {
                            sleep(60 * 1000l, downloadLink);
                        }
                        trials++;
                        // useAPI = getAPICall(downloadLink, 1, null, null);
                        if (trials > 1) {
                            useAPI = false;
                        }
                    } else if (errorCode == 34) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMessage, 60 * 60 * 1000l);
                    } else if (errorCode == 31) {
                        // Free download available only for countries: countryName
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMessage, PluginException.VALUE_ID_PREMIUM_ONLY);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FATAL, errorMessage);
                    }
                } else {
                    dllink = PluginJSonUtils.getJsonValue(response, "downloadLink");
                    waitTimeDetected = false;
                }
            } while (waitTimeDetected && (trials < 2));
        }
        if (!useAPI) {
            if (br.containsHTML(">If you want to download this file, buy")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) {
                        throw (PluginException) e;
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("PREMIUM_ONLY"));
            }
            String downloadURL = downloadLink.getDownloadURL();
            String fileId = new Regex(downloadURL, "https?://(www\\.)?fileshark.pl/pobierz/" + "(\\d+/[0-9a-zA-Z]+/?)").getMatch(1);
            br.getPage(MAINPAGE + "pobierz/normal/" + fileId);
            String redirect = br.getRedirectLocation();
            if (redirect != null) {
                br.getPage(redirect);
            }
            long waitTime = checkForErrors();
            if (waitTime != 0) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("WAITTIME"), waitTime);
            }
            Form dlForm = new Form();
            br.setCookie(downloadURL, "file", fileId);
            // captcha handling
            // the image is encoded into the page, so first we need to store
            // it to hdd and the display as challenge to solve
            for (int i = 0; i < 5; i++) {
                dlForm = br.getForm(0);
                String token = dlForm.getInputFieldByName("form%5B_token%5D").getValue();
                File cf = getLocalCaptchaFile();
                String imageDataEncoded = new Regex(dlForm.getHtmlCode(), "<img src=\"data:image/jpeg;base64,(.*)\" title=\"").getMatch(0);
                byte[] imageData = Base64.decode(imageDataEncoded);
                saveCaptchaImage(cf, imageData);
                String c = getCaptchaCode(cf, downloadLink);
                br.postPage(MAINPAGE + "pobierz/normal/" + fileId, "&form%5Bcaptcha%5D=" + c + "&form%5Bstart%5D=&form%5B_token%5D=" + token);
                logger.info("Submitted DLForm");
                if (br.containsHTML("class=\"error\">Błędny kod")) {
                    continue;
                }
                waitTime = checkForErrors();
                if (waitTime != 0) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("WAITTIME"), waitTime);
                }
                break;
            }
            if (br.containsHTML("class=\"error\">Błędny kod")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("WRONG_CAPTCHA"), 5 * 60 * 1000l);
            }
            dllink = br.getRedirectLocation();
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    void setTrafficLeft(AccountInfo accountInfo, String dailyLimitLeftUsed, boolean isAPI) {
        long trafficLeft = 0;
        if (dailyLimitLeftUsed != null) {
            if (!isAPI) {
                trafficLeft = SizeFormatter.getSize(DAILY_LIMIT) - SizeFormatter.getSize(dailyLimitLeftUsed);
            } else {
                trafficLeft = Long.parseLong(dailyLimitLeftUsed);
            }
            accountInfo.setTrafficMax(SizeFormatter.getSize(DAILY_LIMIT));
            accountInfo.setTrafficLeft(trafficLeft);
        } else {
            accountInfo.setUnlimitedTraffic();
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        boolean useAPI = getUseAPI();
        boolean isPremium = false;
        AccountInfo ai = new AccountInfo();
        boolean hours = false;
        try {
            login(account, useAPI ? false : true);
        } catch (PluginException e) {
            ai.setStatus(getPhrase("LOGIN_FAILED"));
            UserIO.getInstance().requestMessageDialog(0, "FileShark: " + getPhrase("LOGIN_ERROR"), getPhrase("LOGIN_FAILED") + "!\r\n" + getPhrase("VERIFY_LOGIN"));
            account.setValid(false);
            return ai;
        }
        String dailyLimitLeftUsed = "";
        String expire = "";
        if (useAPI) {
            // API CALL - get user Info: fileshark.pl/api/account/getDetails?username={username}&password={pass}
            // params (POST):
            // {username} - user name
            // {pass} - password
            useAPI = getAPICall(null, API_CALL_ACCOUNT_GET_DETAILS, Encoding.urlEncode(account.getUser()), Encoding.urlEncode(account.getPass()));
            // br.postPage("http://fileshark.pl/api/account/getDetails", "username=" + userName + "&password=" + userPassword);
            // output (json)
            // success: (bool)
            // data: (array)
            // userLogin: (string) : User name
            // userEmail: (string) : User e-mail
            // userPremium: (bool) : is user Premium
            // userPremiumDateEnd: (datetime) : expiry date
            // userAmount: (float) User state?
            // userFilesCnt: (int) : Number of files on user's account
            // userDayTrafficLeft: (int) : Daily traffic limit (bytes)
            // registrationDate: (datetime) : Registration date
            if (useAPI) {
                String response = br.toString();
                final String success = PluginJSonUtils.getJsonValue(response, "success");
                if ("true".equals(success)) {
                    final String isUserPremium = PluginJSonUtils.getJsonValue(response, "userPremium");
                    dailyLimitLeftUsed = PluginJSonUtils.getJsonValue(response, "userDayTrafficLeft");
                    setTrafficLeft(ai, dailyLimitLeftUsed, true);
                    if ("true".equals(isUserPremium)) {
                        String userPremiumDateEnd = PluginJSonUtils.getJsonNested(response, "userPremiumDateEnd");
                        expire = PluginJSonUtils.getJsonValue(userPremiumDateEnd, "date");
                        if (expire == null) {
                            ai.setExpired(true);
                            account.setValid(false);
                            return ai;
                        }
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
                        isPremium = true;
                    }
                } else {
                    UserIO.getInstance().requestMessageDialog(0, "FileShark: " + getPhrase("LOGIN_ERROR"), getPhrase("LOGIN_FAILED") + "!\r\n" + getPhrase("VERIFY_LOGIN"));
                    account.setValid(false);
                    ai.setStatus(getPhrase("LOGIN_FAILED"));
                    return ai;
                }
            }
        }
        if (!useAPI) {
            dailyLimitLeftUsed = br.getRegex("<p>Pobrano dzisiaj</p>[\r\t\n ]+<p><strong>(.*)</strong> z " + DAILY_LIMIT + "</p>").getMatch(0);
            setTrafficLeft(ai, dailyLimitLeftUsed, false);
            String accountType = br.getRegex("<p class=\"type-account\">Rodzaj konta <strong>([A-Za-z]+)</strong></p>").getMatch(0);
            if ("Standardowe".equals(accountType)) {
                isPremium = false;
            } else {
                expire = br.getRegex(">Rodzaj konta <strong>Premium <span title=\"(\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})\">\\(do").getMatch(0);
                if (expire == null) {
                    ai.setExpired(true);
                    account.setValid(false);
                    return ai;
                }
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH));
                isPremium = true;
            }
        }
        account.setValid(true);
        if (isPremium) {
            try {
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
            ai.setStatus(getPhrase("PREMIUM_USER"));
            account.setProperty("PREMIUM", "TRUE");
        } else {
            ai.setStatus(getPhrase("REGISTERED_USER"));
            account.setProperty("PREMIUM", "FALSE");
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
        }
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
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
                            this.br.setCookie("https://fileshark.pl", key, value);
                        }
                        return;
                    }
                }
                br.getPage("https://fileshark.pl/zaloguj");
                Form login = br.getForm(0);
                if (login == null) {
                    logger.warning("Couldn't find login form");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("_username", Encoding.urlEncode(account.getUser()));
                login.put("_password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                br.getPage("/");
                /*
                 * if (!br.containsHTML("Rodzaj konta <strong>Premium")) {
                 * logger.warning("Couldn't determine premium status or account is Free not Premium!"); throw new
                 * PluginException(LinkStatus.ERROR_PREMIUM, "Premium Account is invalid: it's free or not recognized!",
                 * PluginException.VALUE_ID_PREMIUM_DISABLE); }
                 */
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("https://fileshark.pl/");
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

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        boolean useAPI = getUseAPI();
        if (useAPI) {
            login(account, false);
        } else {
            login(account, true);
        }
        setCurrentAccount(account);
        if ("FALSE".equals(account.getProperty("PREMIUM"))) {
            account.setMaxSimultanDownloads(1);
            handleFree(downloadLink);
            return;
        }
        requestFileInformation(downloadLink);
        String downloadURL = downloadLink.getDownloadURL();
        String dllink = "";
        if (useAPI) {
            // API CALL: POST fileshark.pl/api/file/getDownloadLink?id={id}&token={token}&username={username}&password={password}
            String generatedLink = checkDirectLink(downloadLink, "generatedLinkFileSharkPl");
            if (generatedLink == null) {
                useAPI = getAPICall(downloadLink, API_CALL_FILE_GET_DOWNLOAD_LINK, Encoding.urlEncode(account.getUser()), Encoding.urlEncode(account.getPass()));
                if (useAPI) {
                    String response = br.toString();
                    if ("true".equals(PluginJSonUtils.getJsonValue(response, "success"))) {
                        // parameters
                        // {id} - file id
                        // {token} - file token
                        // optional (for Premium)
                        // {username} - user name
                        // {password} - password
                        // Output = json
                        // success: (bool)
                        // data: (array)
                        // downloadLink: (string) : download URL
                        // fileId: (int) : file id
                        // fileName: (string) : filename
                        // linkValidTo: (datetime) : Link valid date
                        dllink = PluginJSonUtils.getJsonValue(response, "downloadLink");
                        if (dllink == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        String linkValidTo = PluginJSonUtils.getJsonValue(PluginJSonUtils.getJsonNested(response, "linkValidTo"), "date");
                        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
                        long dateValid;
                        try {
                            dateValid = dateFormat.parse(linkValidTo).getTime();
                        } catch (final Exception e) {
                            logger.log(e);
                            dateValid = System.currentTimeMillis();
                        }
                        downloadLink.setProperty("generatedLinkFileSharkPl", dllink);
                        downloadLink.setProperty("generatedLinkFileSharkPlDate", dateValid);
                    } else {
                        useAPI = false;
                    }
                }
            } else {
                dllink = generatedLink;
            }
        }
        if (!useAPI) {
            br.getPage(downloadURL);
            String fileId = new Regex(downloadURL, "https?://(www\\.)?fileshark.pl/pobierz/" + "(\\d+/[0-9a-zA-Z]+/?)").getMatch(1);
            if (fileId == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setFollowRedirects(false);
            br.getPage("https://fileshark.pl/pobierz/start/" + fileId);
            long waitTime = checkForErrors();
            dllink = br.getRedirectLocation();
        } else {
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "https://fileshark.pl/";
    private static Object       LOCK     = new Object();

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
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
        link.setProperty("generatedLinkFileSharkPl", Property.NULL);
        link.setProperty("generatedLinkFileSharkPlDate", Property.NULL);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), USE_API, JDL.L("plugins.hoster.FileSharkPl.useAPI", getPhrase("USE_API"))).setDefaultValue(default_USE_API));
    }

    private String getPhrase(String key) {
        if ("pl".equals(System.getProperty("user.language")) && phrasesPL.containsKey(key)) {
            return phrasesPL.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
        {
            put("USE_API", "Use API (recommended!)");
            put("PREMIUM_USER", "Premium User");
            put("REGISTERED_USER", "Registered User");
            put("POLAND_ONLY", "This service is only available in Poland");
            put("NOT_DOWNLOADABLE", "File is unavailable for download");
            put("DMCA_REQUEST", "File requested by DMCA");
            put("FILE_DELETED", "File deleted");
            put("OTHER_FILE_DOWNLOAD", "Other file is downloading!");
            put("PREMIUM_ONLY", "This file can only be downloaded by premium users");
            put("WRONG_CAPTCHA", "Wrong Captcha!");
            put("LOGIN_FAILED", "Login failed");
            put("VERIFY_LOGIN", "Please check your Username and Password!");
            put("LOGIN_ERROR", "Login Error");
            put("WAITTIME", "You must wait for new download");
            put("MAX_DOWNLOAD", "Reached max number of simultaneously downloaded files.");
        }
    };
    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
        {
            put("USE_API", "Używaj API (zalecane!)");
            put("PREMIUM_USER", "Użytkownik Premium");
            put("REGISTERED_USER", "Uzytkownik zarejestrowany");
            put("POLAND_ONLY", "Dostęp do serwisu wyłącznie dla adresów z terenu Polski");
            put("NOT_DOWNLOADABLE", "Plik niedostępny do pobrania");
            put("DMCA_REQUEST", "Plik zgłoszony przez DMCA");
            put("FILE_DELETED", "Plik usunięty");
            put("OTHER_FILE_DOWNLOAD", "Inny plik jest pobierany!");
            put("PREMIUM_ONLY", "Plik możliwy do pobierania wyłącznie dla użytkowników Premium");
            put("WRONG_CAPTCHA", "Błędny kod Captcha!");
            put("LOGIN_FAILED", "Błędny login/hasło");
            put("VERIFY_LOGIN", "Proszę zweryfikuj swoją nazwę użytkownika i hasło!");
            put("LOGIN_ERROR", "Błąd logowania");
            put("WAITTIME", "Musisz odczekać do kolejnego pobierania");
            put("MAX_DOWNLOAD", "Osiągnięto maksymalną liczbę sciąganych jednocześnie plików.");
        }
    };

    private boolean getUseAPI() {
        return this.getPluginConfig().getBooleanProperty("USE_API", true);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            String dllValidDate = downloadLink.getStringProperty(property + "Date");
            if (dllValidDate != null) {
                if (System.currentTimeMillis() > Long.parseLong(dllValidDate.toString())) {
                    downloadLink.setProperty(property, Property.NULL);
                    downloadLink.setProperty(property + "Date", Property.NULL);
                    return null;
                }
            }
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    downloadLink.setProperty(property + "Date", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                downloadLink.setProperty(property + "Date", Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }
}