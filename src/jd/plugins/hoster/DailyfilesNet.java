//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dailyfiles.net" }, urls = { "https?://dailyfiles\\.net/([A-Za-z0-9]+)/?" })
public class DailyfilesNet extends antiDDoSForHost {

    private String  MAINPAGE = "http://dailyfiles.net/";
    private String  accountResponse;
    private Account currentAccount;

    public DailyfilesNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://dailyfiles.net/upgrade.html");
    }

    @Override
    public String getAGBLink() {
        return "http://dailyfiles.net/terms.html";
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);

            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setFollowRedirects(true);
            for (final DownloadLink dllink : urls) {
                String fileID = new Regex(dllink.getDownloadURL(), "https?://dailyfiles\\.net/([A-Za-z0-9]+)/?").getMatch(0);
                dllink.setLinkID(fileID);
                // API CALL
                // http://dailyfiles.net/API/apidownload.php?request=filecheck&url=http://dailyfiles.net/eacc7758a35bd234/FileUploader.exe

                getPage(br, MAINPAGE + "API/apidownload.php?request=filecheck&url=" + Encoding.urlEncode(dllink.getDownloadURL()));

                // output: output: filename, filesize
                String response = br.toString();
                final String status = PluginJSonUtils.getJsonValue(response, "status");
                final String error = checkForErrors(response, "error");
                // {"error":"File doesn't exists"}
                if (error == null && !"offline".equals(status)) {
                    String fileName = PluginJSonUtils.getJsonValue(response, "fileName");
                    String fileSize = PluginJSonUtils.getJsonValue(response, "fileSize");
                    if (fileName != null) {
                        fileName = Encoding.htmlDecode(fileName.trim());
                        dllink.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
                    }
                    if (fileSize != null) {
                        dllink.setDownloadSize(SizeFormatter.getSize(fileSize));
                    }
                    dllink.setAvailable(true);
                } else {
                    dllink.setAvailable(false);
                    logger.warning("Linkchecker returns: " + error + " for: " + getHost() + " and link: " + dllink.getDownloadURL());
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        final boolean checked = checkLinks(new DownloadLink[] { downloadLink });
        // we can't throw exception in checklinks! This is needed to prevent multiple captcha events!
        if (!checked && hasAntiddosCaptchaRequirement()) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (!checked || !downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(downloadLink);

    }

    private AvailableStatus getAvailableStatus(final DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String downloadURL = downloadLink.getPluginPatternMatcher();
        setMainPage(downloadURL);
        br.setCookiesExclusive(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setAcceptLanguage("pl-PL,pl;q=0.9,en;q=0.8");

        br.setFollowRedirects(false);
        requestFileInformation(downloadLink);
        Account currAccount = getCurrentAccount();
        final String getURL = MAINPAGE + "API/apidownload.php?request=getfile&url=" + Encoding.urlEncode(downloadURL) + (currAccount != null ? "&username=" + Encoding.urlEncode(currAccount.getUser()) + "&password=" + Encoding.urlEncode(currAccount.getPass()) : "");
        getPage(getURL);
        String response = br.toString();

        String error = checkForErrors(response, "error");
        /*
         * output: wait (waiting time in seconds - unregistered 120, free user 60), downloadlink • File doesn't exists • Not authenticated -
         * if username is set, and authentication failed • You have reached your bandwidth limit • Could not save token - internal error •
         * Token doesn't exists - internal error • Could not generate download URL - internal error • File is too large for free users
         */

        if (error != null) {
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, error);

        } else {
            long watiTime = Long.parseLong(PluginJSonUtils.getJsonValue(response, "wait"));
            sleep(watiTime * 100l, downloadLink);

        }
        final String fileLocation = PluginJSonUtils.getJsonValue(response, "downloadlink");
        if (fileLocation == null) {
            logger.info("filelocation not found with link: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, getPhrase("DOWNLOADLINK_ERROR"));

        }
        response = br.toString();
        if (response == null) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Host busy!", 1 * 60l * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, fileLocation, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleDownloadServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private final void handleDownloadServerErrors() throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String downloadURL = downloadLink.getPluginPatternMatcher();
        setMainPage(downloadURL);
        String response = "";

        br.setFollowRedirects(false);
        requestFileInformation(downloadLink);
        String loginInfo = login(account, false);
        // (string) login - Login użytkownika
        // (string) password - Hasło
        // (string) id - Identyfikator pliku ( np: 7464459120 )
        String userType = PluginJSonUtils.getJsonValue(loginInfo, "typ");
        if (!"premium".equals(userType)) {
            // setLoginData(account);
            setCurrentAccount(account);
            handleFree(downloadLink);
            return;
        }

        String getURL = MAINPAGE + "API/apidownload.php?request=getfile&url=" + Encoding.urlEncode(downloadURL) + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
        getPage(getURL);
        response = br.toString();

        String error = checkForErrors(response, "error");
        /*
         * output: wait (waiting time in seconds - unregistered 120, free user 60), downloadlink • File doesn't exists • Not authenticated -
         * if username is set, and authentication failed • You have reached your bandwidth limit • Could not save token - internal error •
         * Token doesn't exists - internal error • Could not generate download URL - internal error • File is too large for free users
         */

        if (error != null) {
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, error);
        }

        String fileLocation = PluginJSonUtils.getJsonValue(br, "downloadlink");
        if (fileLocation == null) {
            logger.info("filelocation not found with link: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, getPhrase("DOWNLOADLINK_ERROR"));

        }
        // setLoginData(account);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, fileLocation, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.warning("The final dllink seems not to be a file!" + "Response: " + dl.getConnection().getResponseMessage() + ", code: " + dl.getConnection().getResponseCode() + "\n" + dl.getConnection().getContentType());
            handleDownloadServerErrors();
            logger.warning("br returns:" + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        dl.startDownload();
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    private String login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            br.setCookiesExclusive(true);
            // API Call: /API/apidownload.php?request=usercheck&username=xxx&password=xxx
            getPage(MAINPAGE + "/API/apidownload.php?request=usercheck&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            String response = br.toString();

            String error = checkForErrors(response, "error");
            if (error != null) {
                logger.info(error);
                if ("Not authenticated".equals(error)) {
                    error = getPhrase("NOT_AUTHENTICATED");
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, error);

            }
            postPage(MAINPAGE + "/ajax/_account_login.ajax.php", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));

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
        }
    }

    private String checkForErrors(String source, String searchString) {
        if (source.contains("error")) {
            final String errorMessage = PluginJSonUtils.getJsonValue(source, searchString);
            return errorMessage;
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            accountResponse = login(account, true);
        } catch (PluginException e) {
            account.setProperty("cookies", Property.NULL);
            throw e;
        }
        // output
        // premium:
        // {
        // type: "premium",
        // expires: "2017-03-09 00:00:00",
        // traffic: "107374182400"
        // }
        // free:
        // {
        // type: "free",
        // expires: "0000-00-00 00:00:00"
        // }
        String userType = PluginJSonUtils.getJsonValue(accountResponse, "typ");
        if ("premium".equals(userType)) {
            String userPremiumDateEnd = PluginJSonUtils.getJsonValue(accountResponse, "expires");
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
            Date date;
            try {
                date = dateFormat.parse(userPremiumDateEnd);
                ai.setValidUntil(date.getTime());
            } catch (final Exception e) {
                logger.log(e);
            }

            long userTraffic = Long.parseLong(PluginJSonUtils.getJsonValue(accountResponse, "traffic"));
            ai.setTrafficLeft(userTraffic);
            ai.setStatus(getPhrase("PREMIUM_USER"));
        } else {
            ai.setStatus(getPhrase("FREE_USER"));
        }
        account.setValid(true);
        return ai;
    }

    /*
     * @Override public int getMaxSimultanPremiumDownloadNum() { }
     */
    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // requested by the hoster admin
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    void setMainPage(String downloadUrl) {
        if (downloadUrl.contains("https://")) {
            MAINPAGE = "https://dailyfiles.net/";
        } else {
            MAINPAGE = "http://dailyfiles.net/";
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

            put("LOGIN_ERROR", "Login Error");
            put("PREMIUM_USER", "Premium Account");
            put("FREE_USER", "Free Account");
            put("NOT_AUTHENTICATED", "Not Authenticated");
            put("DOWNLOADLINK_ERROR", "Downloadlink error");
        }
    };

    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
        {

            put("LOGIN_ERROR", "Błąd logowania");
            put("PREMIUM_USER", "Użytkownik Premium");
            put("FREE_USER", "Zarejestrowany użytkownik darmowy");
            put("NOT_AUTHENTICATED", "Nazwa użytkownika lub hasło jest niepoprawne");
            put("DOWNLOADLINK_ERROR", "Serwer nie zwrócił linku pobierania");

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

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(Account currentAccount) {
        this.currentAccount = currentAccount;
    }
}