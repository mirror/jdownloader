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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "storbit.net" }, urls = { "https?://storbit\\.net/file/[A-Za-z0-9]+?/([^<>\"]+)" }, flags = { 2 })
public class StorBitNet extends PluginForHost {

    public StorBitNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://storbit.net/premium/");
        // this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://storbit.net/rules/";
    }

    private int           MAXCHUNKSFORFREE          = 1;
    private int           MAXCHUNKSFORPREMIUM       = 0;
    private String        MAINPAGE                  = "http://storbit.net/";
    private String        PREMIUM_DAILY_TRAFFIC_MAX = "20 GB";
    private String        FREE_DAILY_TRAFFIC_MAX    = "10 GB";
    private static Object LOCK                      = new Object();

    private void prepareBrowser(Browser br) {
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "pl-PL,pl;q=0.8,en-US;q=0.6,en;q=0.4");
        br.setCookie(MAINPAGE, "xxx_lang", "en");
        br.setReadTimeout(3 * 60 * 1000);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        prepareBrowser(br);
        String downloadURL = downloadLink.getPluginPatternMatcher();
        br.getPage(downloadURL);
        if (br.containsHTML(">404 - File not found\\.\\.\\.</b>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String fileName = br.getRegex("div class=\"title\"><h1 title=\"([^<>\"]*?)\">&nbsp;").getMatch(0);

        String fileSize = br.getRegex("<span class=\"size\">(\\d+\\.\\d+ [GMTk]B)</span>").getMatch(0);

        fileName = Encoding.htmlDecode(fileName.trim());
        fileName = unescape(fileName);
        if (fileName == null || fileSize == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        downloadLink.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
        downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
        downloadLink.setAvailable(true);
        String fileID = new Regex(downloadURL, "https?://storbit\\.net/file/([A-Za-z0-9]+?)/[^<>\"]+").getMatch(0);
        downloadLink.setProperty("fileID", fileID);

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String downloadURL = downloadLink.getPluginPatternMatcher();

        br.getPage(downloadURL);
        setMainPage(downloadURL);
        // br.setCookiesExclusive(true);
        prepareBrowser(br);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setFollowRedirects(true);
        String fileID = downloadLink.getProperty("fileID").toString();
        br.postPage("http://storbit.net/ajax.php?a=getDownloadForFree", "id=" + fileID + "&_go=");

        String success = getJson("message");
        // not tested
        if (!"success".equals(success)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("DOWNLOAD_LIMIT"), 60 * 60l * 1000l);
        }
        br.getPage(downloadURL);

        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        Form dlForm = new Form();

        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        dlForm.put("ajax", "1");
        dlForm.put("cachestop", "0.7658682554028928");
        rc.setForm(dlForm);
        // id for the hoster
        String id = "6Lc4YwgTAAAAAPoZXXByh65cUKulPwDN31HlV1Wp";

        rc.setId(id);
        String dllink = null;
        for (int i = 0; i < 5; i++) {
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode("recaptcha", cf, downloadLink);

            br.postPage("http://storbit.net/ajax.php?a=getDownloadLink", "captcha1=" + rc.getChallenge() + "&captcha2=" + Encoding.urlEncode(c) + "&id=" + fileID + "&_go=");
            String message = getJson("message");

            if (message == null || "error".equals(message)) {
                logger.info("StoreBitNet: ReCaptcha challenge reports: " + message);
                rc.reload();
                continue;

            } else if (message.equals("success")) {
                dllink = getJson("location").replace("\\", "");

            }
            break;
        }

        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("CAPTCHA_ERROR"), -1l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, MAXCHUNKSFORFREE);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("FINAL_LINK_ERROR"));
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

    private static AtomicBoolean yt_loaded = new AtomicBoolean(false);

    private String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (!yt_loaded.getAndSet(true)) {
            JDUtilities.getPluginForHost("youtube.com");
        }
        return jd.plugins.hoster.Youtube.unescape(s);
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

    void setMainPage(String downloadUrl) {
        if (downloadUrl.contains("https://")) {
            MAINPAGE = "https://storbit.net/";
        } else {
            MAINPAGE = "http://storbit.net/";
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
                                                      put("LOGIN_ERROR", "StorBit.net: Login Error");
                                                      put("LOGIN_FAILED", "Login failed!\r\nPlease check your Username and Password!");
                                                      put("NO_TRAFFIC", "No traffic left");
                                                      put("DOWNLOAD_LIMIT", "You can only download 1 file per 60 minutes");
                                                      put("CAPTCHA_ERROR", "Wrong Captcha code in 5 trials OR final download link not found!");
                                                      put("FINAL_LINK_ERROR", "Plugin Error: final download link is invalid");
                                                  }
                                              };

    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("INVALID_LOGIN", "\r\nNieprawidłowy login/hasło!\r\nCzy jesteś pewien, że poprawnie wprowadziłeś nazwę użytkownika i hasło? Sugestie:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź nazwę użytkownika/hasło ręcznie, bez użycia funkcji Kopiuj i Wklej.");
                                                      put("PREMIUM", "Użytkownik Premium");
                                                      put("FREE", "Użytkownik zarejestrowany (darmowy)");
                                                      put("LOGIN_FAILED_NOT_PREMIUM", "Nieprawidłowe konto lub konto nie-Premium");
                                                      put("LOGIN_ERROR", "StorBit.net: Błąd logowania");
                                                      put("LOGIN_FAILED", "Logowanie nieudane!\r\nZweryfikuj proszę Nazwę Użytkownika i Hasło!");
                                                      put("NO_TRAFFIC", "Brak dostępnego transferu");
                                                      put("DOWNLOAD_LIMIT", "Można pobrać maksymalnie 1 plik na 60 minut");
                                                      put("CAPTCHA_ERROR", "Wprowadzono 5-krotnie nieprawiłowy kod Captcha LUB nie można określić linku pobierania!");
                                                      put("FINAL_LINK_ERROR", "Błąd wtyczki: finalny link pobierania jest nieprawidłowy");

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