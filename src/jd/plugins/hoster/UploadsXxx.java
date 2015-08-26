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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "storbit.net", "streambit.tv", "uploads.xxx" }, urls = { "https?://(www\\.)?(?:uploads\\.xxx|streambit\\.tv|storbit\\.net)/(?:video|file)/[A-Za-z0-9\\-_]+/", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32424", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32424" }, flags = { 0, 0, 0 })
public class UploadsXxx extends PluginForHost {

    @SuppressWarnings("deprecation")
    public UploadsXxx(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://storbit.net/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://streambit.tv/rules/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(uploads\\.xxx|streambit\\.tv)/(?:video|file)/", "storbit.net/file/"));
    }

    @Override
    public String rewriteHost(String host) {
        if ("uploads.xxx".equals(getHost()) || "streambit.tv".equals(getHost())) {
            if (host == null || "uploads.xxx".equals(host) || "streambit.tv".equals(host)) {
                return "storbit.net";
            }
        }
        return super.rewriteHost(host);
    }

    private static Object        LOCK              = new Object();
    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 1;
    private static String        MAINPAGE          = "http://storbit.net/";

    // private static String PREMIUM_DAILY_TRAFFIC_MAX = "20 GB";
    // private static String FREE_DAILY_TRAFFIC_MAX = "10 GB";

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Correct old urls */
        correctDownloadLink(link);
        this.setBrowserExclusive();
        this.br.setCookie(this.getHost(), "xxx_lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">404 - File not found|>Sorry, but the specified file may have been deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("h1 title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            String format = br.getRegex(">Format: <b>([^<>\"]*?)</b>").getMatch(0);
            filename = br.getRegex("class=\"title\">[\t\n\r ]+<h\\d+ title=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null || format == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename += "." + format;
        }
        String filesize = br.getRegex(">Size: <b>([^<>\"]+)<").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"size\">([^<>\"]*?)<").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings({ "deprecation" })
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Z0-9\\-_]+)/$").getMatch(0);
            br.getPage("/play/" + fid + "/");
            final String streamlink = br.getRegex("file: \\'(http://[^<>\"\\']*?)\\'").getMatch(0);
            /* video/xxx.mp4 = conversion in progress, video/lock.mp4 = IP_blocked */
            if (streamlink == null || streamlink.contains("video/xxx.mp4") || streamlink.contains("video/lock.mp4")) {
                // if (br.containsHTML("/img/streaming\\.jpg")) {
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Converting video in progress ...");
                // }
                this.br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                this.br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("/ajax.php?a=getDownloadForFree", "id=" + fid + "&_go=");
                if (br.containsHTML("\"message\":\"error\"")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                }
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId("6Lc4YwgTAAAAAPoZXXByh65cUKulPwDN31HlV1Wp");
                for (int i = 0; i < 5; i++) {
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    br.postPage("/ajax.php?a=getDownloadLink", "captcha1=" + Encoding.urlEncode(rc.getChallenge()) + "&captcha2=" + Encoding.urlEncode(c) + "&id=" + fid + "&_go=");
                    if (br.containsHTML("\"message\":\"error\"")) {
                        rc.reload();
                        continue;
                        // throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    break;
                }
                dllink = getJson("location");
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("CAPTCHA_ERROR"), 1 * 60 * 1000l);
                }
                if (dllink.contains("http://.streambit.tv/")) {
                    /* We get crippled downloadlinks if the user enters "" as captcha response */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            } else {
                /* Prefer streams as we can avoid the captcha though the quality does not match the originally uploaded content. */
                dllink = streamlink;
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("FINAL_LINK_ERROR"));
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
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

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        boolean premium = false;
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            ai.setStatus(getPhrase("LOGIN_FAILED_NOT_PREMIUM"));
            UserIO.getInstance().requestMessageDialog(0, getPhrase("LOGIN_ERROR"), getPhrase("INVALID_LOGIN"));
            // account.setValid(false);
            account.setError(AccountError.INVALID, getPhrase("LOGIN_FAILED_NOT_PREMIUM"));
            return ai;
        }

        br.setCookie(MAINPAGE, "xxx_lang", "en");
        br.getPage(MAINPAGE);
        if (br.containsHTML("Account: <b>Premium")) {
            account.setProperty("PREMIUM", "TRUE");
            ai.setStatus(getPhrase("PREMIUM"));
            premium = true;
        } else {
            account.setProperty("PREMIUM", "FALSE");
            ai.setStatus(getPhrase("FREE"));
        }

        // Not tested - some users reported 20GB daily traffic limit, but there's no info on the page
        /*
         * final String dailyLimitLeftUsed = br.getRegex("<p>Pobrano dzisiaj</p>[\r\t\n ]+<p><strong>(.*)</strong> z " +
         * PREMIUM_DAILY_TRAFFIC_MAX + "</p>").getMatch(0); if (dailyLimitLeftUsed != null) { long trafficLeft =
         * SizeFormatter.getSize(PREMIUM_DAILY_TRAFFIC_MAX) - SizeFormatter.getSize(dailyLimitLeftUsed);
         * ai.setTrafficMax(SizeFormatter.getSize(PREMIUM_DAILY_TRAFFIC_MAX)); ai.setTrafficLeft(trafficLeft); } else {
         * ai.setUnlimitedTraffic(); }
         */
        ai.setUnlimitedTraffic();

        if (premium) {

            String expire = br.getRegex(">Account: <b>Premium \\((\\d+) [days]+\\)</b></a>").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setError(AccountError.INVALID, getPhrase("LOGIN_FAILED_NOT_PREMIUM"));
                // account.setValid(false);
                return ai;
            }
            ai.setExpired(false);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, Integer.parseInt(expire));
            Date date = calendar.getTime();
            ai.setValidUntil(TimeFormatter.getMilliSeconds(sdf.format(date), "yyyy-MM-dd", Locale.ENGLISH));
        }
        account.setValid(true);
        if (premium) {
            try {
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }
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
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.postPage(MAINPAGE + "/ajax.php?a=getUserLogin", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&remember=0&_go=");

                String success = getJson("message");
                if (!"success".equals(success)) {
                    logger.warning("Couldn't determine premium status or account is Free not Premium!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("LOGIN_FAILED_NOT_PREMIUM"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
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

    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        br.setCookie(MAINPAGE, "xxx_lang", "en");
        login(account, true);
        String downloadURL = downloadLink.getDownloadURL();

        br.getPage(downloadURL);
        if ("FALSE".equals(account.getProperty("PREMIUM"))) {
            doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
        }
        String dllink = checkDirectLink(downloadLink, "premium_directlink");
        if (dllink == null) {
            // <a href="http://b7.storbit.net/download/4682043b4c17918ed170ace0860d2f082f6a3e82/ffeverx2pld.mkv" class="btnd1"
            // target="_blank">Download PREMIUM</a>

            dllink = new Regex(br, "<a href=\"(http://[A-z0-9]+?\\.storbit\\.net/download/[^<>\"]+?/[^<>\"]+?)\" class=\"btnd1\" target=\"_blank\">Download PREMIUM</a>").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("FINAL_LINK_ERROR"));
            }
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning(getPhrase("FINAL_LINK_ERROR"));
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("FINAL_LINK_ERROR"));
        }
        downloadLink.setProperty("premium_directlink", dllink);
        dl.startDownload();

    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
        {
            put("INVALID_LOGIN", "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
            put("PREMIUM", "Premium User");
            put("FREE", "Free (Registered) User");
            put("LOGIN_FAILED_NOT_PREMIUM", "Login failed or not Premium");
            put("LOGIN_ERROR", "StorBit.net/Uploads.xxx/Streambit.tv: Login Error");
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
            put("LOGIN_ERROR", "StorBit.net/Uploads.xxx/Streambit.tv: Błąd logowania");
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