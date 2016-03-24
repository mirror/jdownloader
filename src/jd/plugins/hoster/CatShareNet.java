//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.http.Cookies;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "catshare.net" }, urls = { "http://(?:www\\.)?catshare\\.net/[A-Za-z0-9]{15,16}" }, flags = { 2 })
public class CatShareNet extends PluginForHost {

    private String        brbefore = "";
    private String        HOSTER   = "http://catshare.net";
    private static Object lock     = new Object();

    // DEV NOTES
    // captchatype: recaptcha
    // non account: 1 * 1

    public CatShareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(HOSTER + "/login");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        final String downloadURL = link.getDownloadURL();
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadURL);
        doSomething();
        if (br.containsHTML("<title>Error 404</title>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fileName = new Regex(brbefore, "<h3 class=\"pull-left\" style=\"margin-left: 10px;\">(.*)</h3>[ \t\n\r\f]+<h3 class=\"pull-right\"").getMatch(0);
        String fileSize = new Regex(brbefore, "<h3 class=\"pull-right\" style=\"margin-right: 10px;\">(.+?)</h3>").getMatch(0);

        if (fileName == null || fileSize == null) {
            logger.warning("For link: " + downloadURL + ", final filename or filesize is null!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, getPhrase("NO_LINK_DATA"));
        }

        link.setName(fileName.trim());
        link.setDownloadSize(SizeFormatter.getSize(fileSize));
        // setting this prevents from setting incorrect (shortened) filename from the request header
        link.setFinalFileName(link.getName());
        return AvailableStatus.TRUE;
    }

    public void checkErrors(DownloadLink theLink, boolean beforeRecaptcha) throws NumberFormatException, PluginException {
        // Some waittimes...
        if (beforeRecaptcha) {
            if (br.containsHTML("<h4 style=\"margin-right: 10px;\">Odczekaj <big id=\"counter\"></big> lub kup")) {
                // possible waiitime after last download
                String waitTime = br.getRegex("<script>[ \t\n\r\f]+var count = ([0-9]+);").getMatch(0);
                logger.warning("Waittime detected for link " + theLink.getDownloadURL());
                Long waitTimeSeconds = Long.parseLong(waitTime);
                if (waitTimeSeconds != 60l) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, getPhrase("WAITTIME"), (waitTimeSeconds + 5) * 1000L);
                }
            }
        }

    }

    // never got one, but left this for future usage
    public void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(brbefore, Pattern.compile("No file", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("SERVER_ERROR"));
        }
        if (new Regex(brbefore, "(Not Found|<h1>(404 )?Not Found</h1>)").matches()) {
            logger.warning("Server says link offline, please recheck that!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("Twój dzienny limit transferu")) {
            UserIO.getInstance().requestMessageDialog(0, getPhrase("PREMIUM_ERROR"), getPhrase("DAILY_LIMIT") + "\r\n" + getPhrase("PREMIUM_DISABLED"));
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (br.containsHTML("<input type=\"submit\" class=\"btn btn-large btn-inverse\" style=\"font-size:30px; font-weight: bold; padding:30px\" value=\"Pobierz szybko\" />")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("LINK_BROKEN"));
        }

    }

    public void doFreeWebsite(DownloadLink downloadLink, boolean resumable, int maxChunks) throws Exception, PluginException {
        String passCode = null;
        checkErrors(downloadLink, true);

        String dllink = null;
        long timeBefore = System.currentTimeMillis();
        boolean password = false;
        boolean skipWaittime = false;

        // only ReCaptcha
        Form dlForm = new Form();
        if (new Regex(brbefore, "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
            dlForm = br.getForm(0);
            if (dlForm == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("NO_RECAPTCHA_FORM"));
            }

            logger.info("Detected captcha method \"Re Captcha\" for this host");
            final Recaptcha rc = new Recaptcha(br, this);
            rc.setForm(dlForm);
            String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            rc.setId(id);
            for (int i = 0; i < 5; i++) {
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode("recaptcha", cf, downloadLink);
                Form rcform = rc.getForm();
                rcform.put("recaptcha_challenge_field", rc.getChallenge());
                rcform.put("recaptcha_response_field", Encoding.urlEncode(c));
                logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
                dlForm = rc.getForm();
                // waittime is often skippable for reCaptcha handling
                // skipWaittime = true;
                br.submitForm(dlForm);
                logger.info("Submitted DLForm");
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    rc.reload();
                    continue;
                }
                break;
            }

        } else {
            logger.warning("Unknown ReCaptcha method for: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("UNKNOWN_RECAPTCHA"));
        }

        /* Captcha END */
        // if (password) passCode = handlePassword(passCode, dlForm, downloadLink);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            logger.info("5 reCaptcha tryouts for <" + downloadLink.getDownloadURL() + "> were incorrect");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("RECAPTCHA_ERROR"), 1 * 60 * 1000l);
        }

        doSomething();
        checkErrors(downloadLink, false);
        dllink = getDllink();
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("REGEX_ERROR"));
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            doSomething();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("FINAL_LINK_ERROR"));
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    // Removed fake messages which can kill the plugin
    public void doSomething() throws NumberFormatException, PluginException {
        brbefore = br.toString();
        ArrayList<String> someStuff = new ArrayList<String>();
        ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String lolz[] = br.getRegex(aRegex).getColumn(0);
            if (lolz != null) {
                for (String dingdang : lolz) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (String fun : someStuff) {
            brbefore = brbefore.replace(fun, "");
        }
    }

    @Override
    public String getAGBLink() {
        return HOSTER + "/regulamin";
    }

    public String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(brbefore, "Download: <a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = new Regex(brbefore, "(https?://(\\w+\\.)?catshare\\.net/dl/(\\d+/){4}[^\"]+)").getMatch(0);
                if (dllink == null) {
                    dllink = new Regex(brbefore, "(https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/catshare/\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/dl/(\\d+/){4}[^\"]+)").getMatch(0);
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFreeWebsite(downloadLink, false, 1);
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        boolean hours = false;
        try {
            loginWebsite(account, true);
        } catch (PluginException e) {
            ai.setStatus(getPhrase("LOGIN_ERROR"));
            UserIO.getInstance().requestMessageDialog(0, "Catshare.net: " + getPhrase("LOGIN_ERROR"), getPhrase("LOGIN_FAILED"));
            account.setValid(false);
            return ai;
        }

        if ("true".equals(account.getProperty("premium"))) {
            final String dailyLimitLeft = br.getRegex("<li><a href=\"/premium\">([^<>\"\\']+)</a></li>").getMatch(0);
            if (dailyLimitLeft != null) {
                ai.setTrafficMax(SizeFormatter.getSize("20 GB"));
                ai.setTrafficLeft(SizeFormatter.getSize(dailyLimitLeft, true, true));
            } else {
                ai.setUnlimitedTraffic();
            }

            String expire = br.getRegex(">Konto premium ważne do : <strong>(\\d{4}\\-\\d+{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})<").getMatch(0);
            if (expire == null) {
                expire = br.getRegex("(\\d{4}\\-\\d+{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
                if (expire == null) {
                    // for the last day of the premium period
                    if (br.containsHTML("Konto premium ważne do : <strong>-</strong></span>")) {
                        // 0 days left
                        expire = br.getRegex("<a href=\"/premium\">(Konto:[\t\n\r ]+)*Premium \\(<b>(\\d) dni</b>\\)+[ \t\n\r]+</a>").getMatch(1);
                        if (expire == null) {
                            expire = br.getRegex("(Konto:[\r\t\n ]+)+Premium \\(<b><span style=\"color: red\">(\\d+) godzin</span></b>\\)").getMatch(1);
                            hours = true;
                        }
                    }
                    if (expire == null) {
                        ai.setExpired(true);
                        return ai;
                    }
                }
            }
            if (expire.equals("0") && (dailyLimitLeft != null)) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String dateNow = formatter.format(Calendar.getInstance().getTime());
                dateNow = dateNow + " 23:59:59";
                ai.setValidUntil(TimeFormatter.getMilliSeconds(dateNow, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
            } else {
                if (hours) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new Date());
                    cal.add(Calendar.HOUR_OF_DAY, Integer.parseInt(expire));
                    String dateExpire = formatter.format(cal.getTime());

                    ai.setValidUntil(TimeFormatter.getMilliSeconds(dateExpire, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));

                } else {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
                }
            }
            try {
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                // not available in old Stable 0.9.581
            }

            ai.setStatus(getPhrase("PREMIUM"));
        } else {
            ai.setStatus(getPhrase("FREE"));
        }
        return ai;
    }

    private void loginWebsite(Account account, boolean force) throws Exception {
        synchronized (lock) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.getPage("http://" + this.getHost() + "/login");
                Form login = br.getForm(0);
                if (login == null) {
                    logger.warning("Couldn't find login form");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("NO_LOGIN_FORM"));
                }
                login.put("user_email", Encoding.urlEncode(account.getUser()));
                login.put("user_password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                br.getPage("/");
                if (br.containsHTML("(Konto:[\r\t\n ]+)*Darmowe")) {
                    account.setType(AccountType.FREE);
                } else if ((br.containsHTML("(Konto:[\r\t\n ]+)*Premium \\(<b>\\d+ dni</b>\\)")) || (br.containsHTML("(Konto:[\r\t\n ]+)+Premium \\(<b><span style=\"color: red\">\\d+ godzin</span></b>\\)"))) {
                    account.setType(AccountType.PREMIUM);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("LOGIN_ERROR"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        String passCode = null;
        requestFileInformation(downloadLink);
        loginWebsite(account, true);
        br.getPage(downloadLink.getDownloadURL());

        if (account.getType() == AccountType.FREE) {
            doFreeWebsite(downloadLink, false, 1);
            return;
        }

        br.getPage(downloadLink.getDownloadURL());
        doSomething();
        String dllink = getDllink();
        if (dllink == null) {
            if (br.containsHTML("Twój dzienny limit transferu")) {
                UserIO.getInstance().requestMessageDialog(0, getPhrase("PREMIUM_ERROR"), getPhrase("DAILY_LIMIT") + "\r\n" + getPhrase("PREMIUM_DISABLED"));
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.containsHTML("Plik chwilowo niedostępny z powodu awarii")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("LINK_BROKEN"), 60 * 60 * 1000l);
            } else if (br.containsHTML("<h2>System wykrył naruszenie regulaminu w zakresie dostępu do konta.</h2>")) {
                UserIO.getInstance().requestMessageDialog(0, getPhrase("PREMIUM_ERROR"), getPhrase("ACCOUNT_SHARED") + "\r\n" + getPhrase("PREMIUM_DISABLED"));
                String[][] blockDateTime = new Regex(br, "konto zostało tymczasowo zablokowane i zostanie odblokowane (\\d{2}-\\d{2}-\\d{4}) o godzinie (\\d{2}:\\d{2})</h3>").getMatches();
                if (blockDateTime.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("ACCOUNT_SHARED"), 2 * 60 * 60 * 1000l);
                } else {

                    final DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
                    final Date waitUntil = df.parse(blockDateTime[0][0] + " " + blockDateTime[0][1] + ":00");
                    final long difference = waitUntil.getTime() - (new Date()).getTime();
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("ACCOUNT_SHARED"), difference);
                }
            } else {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("REGEX_ERROR"));
            }
        }

        logger.info("Final downloadlink = " + dllink + " starting the download...");
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, getMaxChunksPremiumDownload());
        } catch (Exception e) {
            logger.info("Downloading of: " + dllink + " throws exception: " + e.getMessage());
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, e.getMessage());
        }

        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            doSomething();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("FINAL_LINK_ERROR"));
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /* limit for number of chunks, more cause "service temporarily unavailable" */
    public int getMaxChunksPremiumDownload() {
        return -4;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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
        return false;
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("WAITTIME", "Waittime detected");
                                                      put("PREMIUM_ERROR", "CatShare.net Premium Error");
                                                      put("DAILY_LIMIT", "Daily Limit exceeded!");
                                                      put("SERVER_ERROR", "Server error");
                                                      put("PREMIUM_DISABLED", "Premium disabled, will continue downloads as anonymous user");
                                                      put("LINK_BROKEN", "Link is broken at the server side");
                                                      put("NO_RECAPTCHA_FORM", "no reCaptcha form!");
                                                      put("UNKNOWN_RECAPTCHA", "Unknown ReCaptcha method!");
                                                      put("RECAPTCHA_ERROR", "reCaptcha error or server doesn't accept reCaptcha challenges");
                                                      put("REGEX_ERROR", "Regex didn't match - no final link found");
                                                      put("FINAL_LINK_ERROR", "The final dllink seems not to be a file!");
                                                      put("NO_LINK_DATA", "filename or filesize not found!");
                                                      put("LOGIN_ERROR", "Login Error");
                                                      put("LOGIN_FAILED", "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct?\r\nSome hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
                                                      put("PREMIUM", "Premium User");
                                                      put("FREE", "Free User");
                                                      put("NO_LOGIN_FORM", "no login form");
                                                      put("ACCOUNT_SHARED", "System detected account violation - account is shared.");
                                                  }
                                              };
    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("WAITTIME", "Wykryto czas oczekiwania");
                                                      put("PREMIUM_ERROR", "CatShare.net Błąd Konta Premium");
                                                      put("DAILY_LIMIT", "Wyczerpano dzienny limit!");
                                                      put("SERVER_ERROR", "Błąd serwera");
                                                      put("PREMIUM_DISABLED", "Konto Premium zostanie wyłączone, pobierania będą kontynuowane jako anonimowe");
                                                      put("LINK_BROKEN", "Plik jest uszkodzony na serwerze");
                                                      put("NO_RECAPTCHA_FORM", "brak formularza reCaptcha!");
                                                      put("UNKNOWN_RECAPTCHA", "Nieznany typ ReCaptcha!");
                                                      put("RECAPTCHA_ERROR", "błąd reCaptcha lub serwer nie akceptuje prób wprowadzenia kodu reCaptcha");
                                                      put("REGEX_ERROR", "Wyrażenie regularne nie znalazło finalnego linku");
                                                      put("FINAL_LINK_ERROR", "Finałowy link jest nieprawidłowy");
                                                      put("NO_LINK_DATA", "Brak nazwy i rozmiaru pliku!");
                                                      put("LOGIN_ERROR", "Błąd logowania");
                                                      put("LOGIN_FAILED", "\r\nNieprawidłowy login/hasło!\r\nCzy jesteś pewien, że poprawnie wprowadziłeś nazwę użytkownika i hasło?\r\nSugestie:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź nazwę użytkownika/hasło ręcznie, bez użycia funkcji Kopiuj i Wklej.");
                                                      put("PREMIUM", "Użytkownik Premium");
                                                      put("FREE", "Użytkownik darmowy");
                                                      put("NO_LOGIN_FORM", "brak formularza logowania");
                                                      put("ACCOUNT_SHARED", "System wykrył naruszenie regulaminu w zakresie dostępu do konta.");
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