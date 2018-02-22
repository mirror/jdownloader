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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidu.net" }, urls = { "https?://rapidu\\.(net|pl)/(\\d+)(/)?" })
public class RapiduNet extends PluginForHost {
    private String       userAgent        = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.149 Safari/537.36 OPR/20.0.1387.77";
    // some how related to storbit.net(cloudflare errors), but streambit.tv domain redirects to rapidu.net
    // requested by admin of the hoster due to high traffic
    private int          MAXCHUNKSFORFREE = 1;
    private int          MAXCHUNKSFORPREMIUM;
    private final String PREFER_RECONNECT = "PREFER_RECONNECT";

    public RapiduNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://rapidu.net/premium/");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://rapidu.net/rules/";
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        // correct link stuff goes here, stable is lame!
        for (DownloadLink link : urls) {
            if (link.getProperty("FILEID") == null) {
                String downloadUrl = link.getDownloadURL();
                String fileID;
                if (downloadUrl.contains("https")) {
                    fileID = new Regex(downloadUrl, "https://rapidu\\.(net|pl)/([0-9]+)/?").getMatch(1);
                } else {
                    fileID = new Regex(downloadUrl, "http://rapidu\\.(net|pl)/([0-9]+)/?").getMatch(1);
                }
                link.setProperty("FILEID", fileID);
            }
        }
        try {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setFollowRedirects(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 49) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                boolean first = true;
                for (final DownloadLink dl : links) {
                    if (!first) {
                        sb.append(",");
                    }
                    sb.append(dl.getProperty("FILEID"));
                    first = false;
                }
                br.postPage("https://rapidu.net/api/getFileDetails/", "id=" + sb.toString());
                // (int) [fileStatus] - Status pliku - 1 - plik poprawny, 0 - plik usunięty lub zawiera błędy
                // (int) [fileId] - Identyfikator pliku
                // (string) [fileName] - Nazwa pliku
                // (string) [fileDesc] - Opis pliku
                // (int) [fileSize] - Rozmiar pliku ( w bajtach )
                // (string) [fileUrl] - Adres url pliku
                String response = br.toString();
                int fileNumber = 0;
                for (final DownloadLink dllink : links) {
                    String source = new Regex(response, "\"" + fileNumber + "\":\\{(.+?)\\}").getMatch(0);
                    String fileStatus = getJson("fileStatus", source);
                    String fileName = getJson("fileName", source);
                    String fileUrl = getJson("fileUrl", source);
                    if (fileName == null && fileUrl != null) {
                        fileUrl = fileUrl.replace("\\", "");
                        fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                    }
                    if (fileName == null) {
                        fileName = dllink.getName();
                    }
                    String fileSize = getJson("fileSize", source);
                    if (fileStatus.equals("0")) {
                        dllink.setAvailable(false);
                        logger.warning("Linkchecker returns not available for: " + getHost() + " and link: " + dllink.getDownloadURL());
                    } else {
                        fileName = Encoding.htmlDecode(fileName.trim());
                        fileName = Encoding.unicodeDecode(fileName);
                        dllink.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
                        dllink.setDownloadSize(SizeFormatter.getSize(fileSize));
                        dllink.setAvailable(true);
                    }
                    fileNumber++;
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, "without_account");
    }

    public void doFree(final DownloadLink downloadLink, final String directlinkproperty) throws Exception, PluginException {
        setMainPage(downloadLink.getDownloadURL());
        br.setCookiesExclusive(true);
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setAcceptLanguage("pl-PL,pl;q=0.9,en;q=0.8");
            // br.getHeaders().put("User-Agent", userAgent);
            br.postPage(MAINPAGE + "/ajax.php?a=getLoadTimeToDownload", "_go=");
            String response = br.toString();
            if (response == null) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Host busy!", 1 * 60l * 1000l);
            }
            Date actualDate = new Date();
            String timeToDownload = getJson("timeToDownload", response);
            if (timeToDownload.equals("stop")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP Blocked!", 10 * 60l * 1000l);
            }
            Date eventDate = new Date(Long.parseLong(timeToDownload) * 1000l);
            long timeLeft = eventDate.getTime() - actualDate.getTime();
            if (timeLeft > 600 * 1000) {
                long seconds = timeLeft / 1000l;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                seconds = (long) Math.floor(seconds % 60);
                minutes = (long) Math.floor(minutes % 60);
                hours = (long) Math.floor(hours);
                logger.info("Waittime =" + hours + " hours, " + minutes + " minutes, " + seconds + " seconds!");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait time!", seconds * 1000l + minutes * 60l * 1000l + hours * 3600l * 1000l);
            } else if (timeLeft > 0) {
                sleep(timeLeft, downloadLink);
            }
            final String fileID = downloadLink.getProperty("FILEID").toString();
            Form dlForm = new Form();
            dlForm.put("ajax", "1");
            dlForm.put("cachestop", "0.7658682554028928");
            final boolean usesReCaptchaV2 = true;
            // id for the hoster
            final String reCaptchaV1ID = "6Ld12ewSAAAAAHoE6WVP_pSfCdJcBQScVweQh8Io";
            /* 2018-02-22: Hardcoded rcV2ID */
            final String reCaptchaV2ID = "6LcOuQkUAAAAAF8FPp423qz-U2AXon68gJSdI_W4";
            for (int i = 0; i < 5; i++) {
                if (usesReCaptchaV2) {
                    /* 218-02-22 */
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaV2ID).getToken();
                    br.postPage(MAINPAGE + "/ajax.php?a=getCheckCaptcha", "captcha1=" + Encoding.urlEncode(recaptchaV2Response) + "&fileId=" + fileID + "&_go=");
                } else {
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.setForm(dlForm);
                    rc.setId(reCaptchaV1ID);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    br.postPage(MAINPAGE + "/ajax.php?a=getCheckCaptcha", "captcha1=" + rc.getChallenge() + "&captcha2=" + Encoding.urlEncode(c) + "&fileId=" + fileID + "&_go=");
                }
                response = br.toString();
                final String message = checkForErrors(br.toString(), "error");
                if (message == null || message.equals("error")) {
                    /* 2018-02-22: This should not happen anymore as they're using reCaptchaV2 now. */
                    logger.info("RapiduNet: ReCaptcha challenge reports: " + response);
                    continue;
                } else if (message.equals("success")) {
                    dllink = getJson("url", response);
                }
                break;
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Can't find final download link/Captcha errors!", -1l);
            }
            dllink = dllink.replace("\\", "");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, MAXCHUNKSFORFREE);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleDownloadServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Store directlink and re-use it later! */
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private final void handleDownloadServerErrors() throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private void setLoginData(final Account account) throws Exception {
        br.getPage("https://rapidu.net/");
        br.setCookiesExclusive(true);
        final Object ret = account.loadCookies("");
        final HashMap<String, String> cookies = (HashMap<String, String>) ret;
        if (account.isValid()) {
            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                final String key = cookieEntry.getKey();
                final String value = cookieEntry.getValue();
                this.br.setCookie("http://" + account.getHoster() + "/", key, value);
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        boolean retry;
        setMainPage(downloadLink.getDownloadURL());
        String response = "";
        // loop because wrong handling of
        // LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE when Free user has waittime for the next
        // downloads
        do {
            br.setFollowRedirects(true);
            retry = false;
            requestFileInformation(downloadLink);
            String loginInfo = login(account, false);
            // (string) login - Login użytkownika
            // (string) password - Hasło
            // (string) id - Identyfikator pliku ( np: 7464459120 )
            int userPremium = Integer.parseInt(getJson("userPremium", loginInfo));
            // requested by hoster admin
            if (userPremium == 0) {
                MAXCHUNKSFORPREMIUM = 1;
            } else {
                // API method to get download limits
                try {
                    br.postPage("https://rapidu.net/api/getServerLimit/", "");
                } catch (Exception e) {
                }
                // returns:
                // {"filesConnLimit":int,"filesDownloadLimit":int}
                // filesConnLimit = number of chunks
                // filesDownloadLimit = number of simultanous downloads
                String downloadLimit = getJson("filesConnLimit", br.toString());
                if (downloadLimit == null) {
                    MAXCHUNKSFORPREMIUM = -3;
                } else {
                    MAXCHUNKSFORPREMIUM = -1 * Integer.parseInt(downloadLimit);
                }
            }
            br.setFollowRedirects(true);
            br.postPage(MAINPAGE + "/api/getFileDownload/", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&id=" + downloadLink.getProperty("FILEID"));
            // response
            // (string) [fileLocation] - Lokalizacja pliku ( link ważny jest 24h od momentu wygenerowania )
            response = br.toString();
            String errors = checkForErrors(response, "error");
            // errorEmptyLoginOrPassword - Brak parametru Login lub Password
            // errorAccountNotFound - Konto użytkownika nie zostało znalezione
            // errorAccountBan - Konto użytkownika zostało zbanowane i nie ma możliwości zalogowania się na nie
            // errorAccountNotHaveDayTransfer - Użytkownik nie posiada wystarczającej ilości transferu dziennego, aby pobrać dany plik
            // errorDateNextDownload - Czas, po którym użytkownik Free może pobrać kolejny plik
            // errorEmptyFileId - Brak parametru id lub parametr jest pusty
            // errorFileNotFound - Nie znaleziono pliku
            //
            if (errors != null) {
                // probably it won't happen after changes in the API -
                // getFileDownload now supports also Free Registered Users
                if (errors.contains("errorAccountFree")) {
                    setLoginData(account);
                    doFree(downloadLink, "account_free");
                    return;
                } else {
                    if (errors.contains("errorDateNextDownload")) {
                        String nextDownload = checkForErrors(response, "errorDateNextDownload");
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        Date newStartDate = df.parse(nextDownload);
                        Date actualDate = new Date();
                        long leftToWait = newStartDate.getTime() - actualDate.getTime();
                        if (leftToWait > 0) {
                            final boolean preferReconnect = this.getPluginConfig().getBooleanProperty("PREFER_RECONNECT", false);
                            // doesn't work correctly
                            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, leftToWait); }
                            // temporary solution
                            if (preferReconnect) {
                                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, leftToWait * 60 * 1001l);
                            }
                            sleep(leftToWait, downloadLink);
                            retry = true;
                        }
                    } else if (errors.contains("errorAccountNotHaveDayTransfer")) {
                        logger.info("Hoster: RapiduNet reports: No traffic left!");
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No traffic left!");
                    } else {
                        logger.info("Hoster: RapiduNet reports:" + errors + " with link: " + downloadLink.getDownloadURL());
                        if (errors.contains("errorFileNotFound")) {
                            // API incorrectly informs when the server is in maintenance mode
                            br.getPage(downloadLink.getDownloadURL());
                            if (br.containsHTML("Trwają prace techniczne")) {
                                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster in Maintenance Mode", 1 * 60 * 1000l);
                            } else {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                        } else {
                            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, errors);
                        }
                    }
                }
            }
        } while (retry);
        String fileLocation = getJson("fileLocation", response);
        if (fileLocation == null) {
            logger.info("Hoster: RapiduNet reports: filelocation not found with link: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, "filelocation not found");
        }
        setLoginData(account);
        String dllink = fileLocation.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, MAXCHUNKSFORPREMIUM);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.warning("The final dllink seems not to be a file!" + "Response: " + dl.getConnection().getResponseMessage() + ", code: " + dl.getConnection().getResponseCode() + "\n" + dl.getConnection().getContentType());
            handleDownloadServerErrors();
            logger.warning("br returns:" + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private boolean default_prefer_reconnect = false;

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), PREFER_RECONNECT, JDL.L("plugins.hoster.rapidunet.preferreconnect", getPhrase("PREFER_RECONNECT"))).setDefaultValue(default_prefer_reconnect));
    }

    private String        MAINPAGE = "https://rapidu.net";
    private static Object LOCK     = new Object();

    private String login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            br.setCookiesExclusive(true);
            // final Object ret = account.getProperty("cookies", null);
            br.postPage(MAINPAGE + "/api/getAccountDetails/", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            String response = br.toString();
            String error = checkForErrors(response, "error");
            if (error == null && response.contains("Trwaja prace techniczne")) {
                error = "Hoster in Maintenance Mode";
            }
            if (error != null) {
                logger.info("Hoster RapiduNet reports: " + error);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, error);
            }
            br.postPage(MAINPAGE + "/ajax.php?a=getUserLogin", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&remember=1&_go=");
            account.saveCookies(br.getCookies(br.getURL()), "");
            return response;
        }
    }

    private String checkForErrors(String source, String searchString) {
        if (source.contains("message")) {
            String errorMessage = getJson(searchString, source);
            if (errorMessage == null) {
                errorMessage = getJson("message", source);
            }
            return errorMessage;
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        String accountResponse;
        try {
            accountResponse = login(account, true);
        } catch (PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
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
        //
        // (string) [userLogin] - Login użytkownika
        // (string) [userEmail] - Adres email
        // (int) [userPremium] - 1 - Użytkownik Premium, 0 - Użytkownik Free
        // (datetime) [userPremiumDateEnd] - Data wygaśnięcia konta premium
        // (string) [userHostingPlan] - Aktualny plan hostingowy
        // (float) [userAmount] - Środki zgromadzone w programie partnerskim
        // (int) [userFileNum] - Łączna liczba plików
        // (int) [userFileSize] - Łączny rozmiar plików ( w bajtach )
        // (int) [userDirectDownload] - 1 - Directdownload włączony, 0 - Directdownload wyłączony
        // (int) [userTrafficDay] - Dostępny transfer (w ramach dziennego limitu transferu)
        // (int) [userTraffic] - Dostępny, maksymalny transfer w ciągu dnia
        // (datetime) [userDateRegister] - Data rejestracji
        final int userPremium = Integer.parseInt(getJson("userPremium", accountResponse));
        final String userPremiumDateEnd = getJson("userPremiumDateEnd", accountResponse);
        final String userTrafficDay = getJson("userTrafficDay", accountResponse);
        final long userTraffic = Long.parseLong(getJson("userTraffic", accountResponse));
        ai.setTrafficLeft(userTrafficDay);
        // available daily user traffic
        ai.setTrafficMax(userTraffic);
        if (userPremium == 0) {
            ai.setStatus("Registered (free) user");
            account.setType(AccountType.FREE);
        } else {
            ai.setStatus("Premium user");
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
            Date date;
            try {
                date = dateFormat.parse(userPremiumDateEnd);
                ai.setValidUntil(date.getTime());
            } catch (final Exception e) {
                logger.log(e);
            }
            // set max simult. downloads using API method
            account.setMaxSimultanDownloads(checkMaxSimultanPremiumDowloadNum());
            account.setType(AccountType.PREMIUM);
        }
        account.setValid(true);
        return ai;
    }

    private int checkMaxSimultanPremiumDowloadNum() {
        int limit = 2;
        Browser br2 = new Browser();
        br2.setCookiesExclusive(true);
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.setFollowRedirects(true);
        // API method to get download limits
        try {
            br2.postPage(MAINPAGE + "/api/getServerLimit/", "");
        } catch (Exception e) {
        }
        // returns:
        // {"filesConnLimit":int,"filesDownloadLimit":int}
        // filesConnLimit = number of chunks
        // filesDownloadLimit = number of simultanous downloads
        String downloadLimit = getJson("filesDownloadLimit", br2.toString());
        if (downloadLimit == null) {
            return limit;
        } else {
            limit = Integer.parseInt(downloadLimit);
        }
        return limit;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        // requested by the hoster admin
        return checkMaxSimultanPremiumDowloadNum();
    }

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

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":[{]?\"(.+?)\"[}]?").getMatch(0);
        }
        return result;
    }

    void setMainPage(String downloadUrl) {
        if (downloadUrl.contains("https://")) {
            MAINPAGE = "https://rapidu.net";
        } else {
            MAINPAGE = "http://rapidu.net";
        }
    }

    /**
     * Check if a stored directlink exists under property 'property' and if so, check if it is still valid (leads to a downloadable content
     * [NOT html]).
     */
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
                                                      put("PREFER_RECONNECT", "Prefer Reconnect if the wait time is detected");
                                                  }
                                              };
    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("PREFER_RECONNECT", "Wybierz Ponowne Połaczenie, jeśli wykryto czas oczekiwania na kolejne pobieranie");
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