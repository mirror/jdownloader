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

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.UserIO;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidu.net" }, urls = { "http://rapidu\\.net/(\\d+)(/)?" }, flags = { 2 })
public class RapiduNet extends PluginForHost {

    private String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.149 Safari/537.36 OPR/20.0.1387.77";

    public RapiduNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rapidu.net/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://rapidu.net/rules/";
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        // correct link stuff goes here, stable is lame!
        for (DownloadLink link : urls) {
            if (link.getProperty("FILEID") == null) {
                String downloadUrl = link.getDownloadURL();
                String fileID = new Regex(downloadUrl, MAINPAGE + "/([0-9]+)").getMatch(0);
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
                br.postPage(MAINPAGE + "/api/getFileDetails/", "id=" + sb.toString());

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
                    if (fileName == null) fileName = dllink.getName();
                    String fileSize = getJson("fileSize", source);

                    if (fileStatus.equals("0")) {
                        dllink.setAvailable(false);
                        logger.warning("Linkchecker returns not available for: " + getHost() + " and link: " + dllink.getDownloadURL());
                    } else {

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
        if (!downloadLink.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKED; }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;

    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {

        br.setCookiesExclusive(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setAcceptLanguage("pl-PL,pl;q=0.9,en;q=0.8");
        br.getHeaders().put("User-Agent", userAgent);
        br.postPage(MAINPAGE + "/ajax.php?a=getLoadTimeToDownload", "_go=");
        String response = br.toString();
        Date actualDate = new Date();

        Date eventDate = new Date(Long.parseLong(getJson("timeToDownload", response)) * 1000l);

        long pozostalyCzas = eventDate.getTime() - actualDate.getTime();

        if (pozostalyCzas > 0) {
            long seconds = pozostalyCzas / 1000l;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            seconds = (long) Math.floor(seconds % 60);
            minutes = (long) Math.floor(minutes % 60);
            hours = (long) Math.floor(hours);

            logger.info("Waittime =" + hours + " godzin, " + minutes + " minut, " + seconds + " sekund");

            sleep(seconds * 1000l + minutes * 60l * 1000l + hours * 3600l * 1000l, downloadLink);

        }
        String fileID = downloadLink.getProperty("FILEID").toString();
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        Form dlForm = new Form();

        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        dlForm.put("ajax", "1");
        dlForm.put("cachestop", "0.7658682554028928");
        rc.setForm(dlForm);
        // id for the hoster
        String id = "6Ld12ewSAAAAAHoE6WVP_pSfCdJcBQScVweQh8Io";

        rc.setId(id);
        String dllink = null;
        for (int i = 0; i < 5; i++) {
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            br.postPage(MAINPAGE + "/ajax.php?a=getCheckCaptcha", "captcha1=" + rc.getChallenge() + "&captcha2=" + Encoding.urlEncode(c) + "&fileId=" + fileID + "&_go=");
            response = br.toString();
            String message = checkForErrors(br.toString(), "error");
            if (message.equals("error")) {

                rc.reload();
                continue;

            } else if (message.equals("success")) {
                dllink = getJson("url", response).replace("\\", "");

            }
            break;
        }

        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Can't find final download link/Captcha errors!", -1l); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        boolean retry;
        String response = "";

        // loop because wrong handling of
        // LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE when Free user has waittime for the next
        // downloads
        do {
            retry = false;
            requestFileInformation(downloadLink);
            String loginInfo = login(account, false);
            // (string) login - Login użytkownika
            // (string) password - Hasło
            // (string) id - Identyfikator pliku ( np: 7464459120 )

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
            // (Premium
            // 30Gb/dzień, Free 5Gb/dzień)
            // errorDateNextDownload - Czas, po którym użytkownik Free może pobrać kolejny plik
            // errorEmptyFileId - Brak parametru id lub parametr jest pusty
            // errorFileNotFound - Nie znaleziono pliku
            //

            if (errors != null) {
                // probably it won't happen after changes in the API -
                // getFileDownload now supports also Free Registered Users
                if (errors.contains("errorAccountFree")) {
                    setLoginData(account);
                    handleFree(downloadLink);
                    return;
                } else {
                    if (errors.contains("errorDateNextDownload")) {
                        String nextDownload = checkForErrors(response, "errorDateNextDownload");

                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        Date newStartDate = df.parse(nextDownload);
                        Date actualDate = new Date();
                        long leftToWait = newStartDate.getTime() - actualDate.getTime();
                        if (leftToWait > 0) {
                            // doesn't work correctly
                            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, leftToWait); }
                            // temporary solution
                            sleep(leftToWait, downloadLink);
                            retry = true;
                        }
                    } else if (errors.contains("errorAccountNotHaveDayTransfer")) {
                        logger.info("Hoster: RapiduNet reports: No traffic left!");
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No traffic left!");
                    } else {

                        logger.info("Hoster: RapiduNet reports:" + errors + " with link: " + downloadLink.getDownloadURL());
                        if (errors.contains("errorFileNotFound"))
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        else
                            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, errors);
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

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        dl.startDownload();

    }

    private static final String MAINPAGE = "http://rapidu.net";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private String login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);

            br.postPage(MAINPAGE + "/api/getAccountDetails/", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            String response = br.toString();
            String error = checkForErrors(response, "error");
            if (error != null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account is invalid: " + error);

            }
            br.postPage(MAINPAGE + "/ajax.php?a=getUserLogin", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&remember=1&_go=");

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
            ai.setStatus("Login failed");
            UserIO.getInstance().requestMessageDialog(0, "RapiduNet Login Error", "Login failed!\r\nPlease check your Username and Password!");
            account.setValid(false);
            account.setProperty("cookies", Property.NULL);
            return ai;

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
        // (int) [userTraffic] - Dostępny transfer (w ramach dziennego limitu transferu)
        // (datetime) [userDateRegister] - Data rejestracji

        int userPremium = Integer.parseInt(getJson("userPremium", accountResponse));
        String userPremiumDateEnd = getJson("userPremiumDateEnd", accountResponse);
        String userTraffic = getJson("userTrafficDay", accountResponse);
        ai.setTrafficLeft(userTraffic);
        if (userPremium == 0) {
            ai.setStatus("Registered (free) user");
        } else {
            ai.setStatus("Premium user");

            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
            Date date;
            try {
                date = dateFormat.parse(userPremiumDateEnd);
                ai.setValidUntil(date.getTime());
            } catch (final Exception e) {
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            }
        }

        account.setValid(true);

        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":[{]?\"(.+?)\"[}]?").getMatch(0);
        return result;
    }
}