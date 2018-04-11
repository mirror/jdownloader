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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.gui.UserIO;
import jd.http.Browser;
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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "catshare.net" }, urls = { "https?://(?:www\\.)?catshare\\.net/[A-Za-z0-9]{15,16}" })
public class CatShareNet extends antiDDoSForHost {
    private String          brbefore               = "";
    private String          HOSTER                 = "http://catshare.net";
    private static Object   lock                   = new Object();
    protected final String  USE_API                = "USE_API_35559";
    private final boolean   defaultUSE_API         = true;
    private final boolean   use_api_availablecheck = true;
    public static final int api_status_all_ok      = -1;
    // private final boolean useAPI = true;
    private String          apiSession             = null;

    // DEV NOTES
    // captchatype: reCaptchaV1
    @Override
    public String getAGBLink() {
        return HOSTER + "/regulamin";
    }

    public CatShareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(HOSTER + "/login");
        this.setConfigElements();
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.getHeaders().put("User-Agent", "JDownloader");
        }
        return prepBr;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            int tempcounter = 0;
            while (true) {
                /* Reset tempcounter */
                tempcounter = 0;
                links.clear();
                while (true) {
                    /* we test 10 links at once (max = 10) */
                    if (index == urls.length || links.size() > 9) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("links=");
                for (final DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    if (tempcounter < links.size() - 1) {
                        sb.append(Encoding.urlEncode("\n"));
                    }
                    tempcounter++;
                }
                /* Reset tempcounter */
                tempcounter = 0;
                postPage(getAPIProtocol() + this.getHost() + "/download/json_check", sb.toString());
                LinkedHashMap<String, Object> entries = null;
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                final String[] json_workaround_array = br.toString().split("\\},\\{");
                for (final DownloadLink dl : links) {
                    final String state;
                    final String filename;
                    final long filesize;
                    if (tempcounter <= json_workaround_array.length - 1) {
                        entries = (LinkedHashMap<String, Object>) ressourcelist.get(tempcounter);
                        state = (String) entries.get("status");
                        filename = (String) entries.get("filename");
                        filesize = JavaScriptEngineFactory.toLong(entries.get("filesize"), 0);
                    } else {
                        state = null;
                        filename = null;
                        filesize = 0;
                    }
                    if ("offline".equals(state) || state == null) {
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    /* Trust API - offline urls can still have their filename- and size information available */
                    if (filename != null) {
                        dl.setFinalFileName(filename);
                    }
                    dl.setDownloadSize(filesize);
                    tempcounter++;
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (use_api_availablecheck) {
            checkLinks(new DownloadLink[] { downloadLink });
            if (!downloadLink.isAvailabilityStatusChecked()) {
                return AvailableStatus.UNCHECKED;
            }
            if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } else {
            return requestFileInformationWebsite(downloadLink);
        }
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        final String downloadURL = link.getDownloadURL();
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        getPage(downloadURL);
        doSomething();
        if (br.containsHTML("<title>Error 404</title>") || br.getHttpConnection().getResponseCode() == 404) {
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

    @SuppressWarnings("deprecation")
    public void checkErrorsWebsite(DownloadLink theLink, boolean beforeRecaptcha) throws NumberFormatException, PluginException {
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

    public String getDllinkWebsite() {
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

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (getUseAPI()) {
            handleDownloadAPI(downloadLink, true, 0, false, "freedirectlink");
        } else {
            if (use_api_availablecheck) {
                getPage(downloadLink.getDownloadURL());
                doSomething();
            }
            doFreeWebsite(downloadLink, false, 1);
        }
    }

    /** Handles Free, Free-Account and Premium-Account download via API */
    public void handleDownloadAPI(final DownloadLink downloadLink, final boolean resumable, final int maxChunks, final boolean premium, final String directlinkproperty) throws Exception, PluginException {
        String passCode = downloadLink.getDownloadPassword();
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            String download_post_data = "linkid=" + getLinkid(downloadLink) + "&challenge=" + System.currentTimeMillis();
            /*
             * Premium users could skip this step but I left it in so in case a Premium Account just switches to Free or the admin wants
             * premium users to enter captchas/wait too --> Everything is possible ;)
             */
            postPageAPI(getAPIProtocol() + this.getHost() + "/download/json_wait", "");
            /* E.g. response for premium users: {"wait_time":0,"key":null} */
            long wait = 0;
            String wait_str = PluginJSonUtils.getJsonValue(br, "wait_time");
            if (wait_str != null) {
                wait = Long.parseLong(wait_str) * 1000;
                if (wait > System.currentTimeMillis()) {
                    /* Change from timestamp of current time + wait TO Remaining wait */
                    wait -= System.currentTimeMillis();
                }
                checkForLongWait(wait);
            }
            /* 2016-05-19: json_challenge-call is not needed anymore as json_wait will return waittime AND reCaptcha key */
            // postPageAPI("/download/json_challenge", "");
            final long timeBefore = System.currentTimeMillis();
            final String rcID = PluginJSonUtils.getJsonValue(br, "key");
            if (rcID != null && rcID.length() > 6) {
                /* Usually free users do have to enter captchas */
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, rcID).getToken();
                download_post_data += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
            }
            waitTime(downloadLink, timeBefore, wait);
            postPageAPI("/download/json_download", download_post_data);
            dllink = PluginJSonUtils.getJsonValue(br, "downloadUrl");
            if (dllink == null || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API issue");
            }
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleErrorsAPI();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server issue");
        }
        if (passCode != null) {
            downloadLink.setDownloadPassword(passCode);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private void checkForLongWait(final long wait) throws PluginException {
        if (wait > 240000l) {
            /* Reconnect wait */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait);
        }
    }

    public static String getAPIProtocol() {
        return "https://";
    }

    /** 2018-04-11: Outdated! */
    public void doFreeWebsite(final DownloadLink downloadLink, final boolean resumable, final int maxChunks) throws Exception, PluginException {
        checkErrorsWebsite(downloadLink, true);
        String dllink = null;
        boolean password = false;
        // only ReCaptcha
        Form dlForm = new Form();
        if (new Regex(brbefore, "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
            dlForm = br.getForm(0);
            final String id = br.getRegex("data\\-sitekey=\"([^<>\"]+)\"").getMatch(0);
            String waittime_seconds = br.getRegex("var count = (\\d+);").getMatch(0);
            if (waittime_seconds == null) {
                /* Fallback */
                waittime_seconds = "60";
            }
            if (dlForm == null || id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("NO_RECAPTCHA_FORM"));
            }
            long wait = Long.parseLong(waittime_seconds) * 1000l;
            checkForLongWait(wait);
            final long timeBefore = System.currentTimeMillis();
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, id).getToken();
            dlForm.put("g-recaptcha-response", recaptchaV2Response);
            waitTime(downloadLink, timeBefore, 60000);
            submitForm(dlForm);
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
        checkErrorsWebsite(downloadLink, false);
        dllink = getDllinkWebsite();
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
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    private void waitTime(final DownloadLink downloadLink, final long timeBefore, long waittime) throws PluginException {
        long passedTime = (System.currentTimeMillis() - timeBefore) - 1000;
        waittime -= passedTime;
        if (waittime > 0) {
            logger.info("Waiting waittime: " + waittime);
            this.sleep(waittime, downloadLink);
        } else {
            logger.info("Waited long enough due to captcha solving --> No wait required anymore");
        }
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai;
        try {
            login(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        if (getUseAPI()) {
            ai = this.fetchAccountInfoAPI(account);
        } else {
            ai = this.fetchAccountInfoWebsite(account);
        }
        return ai;
    }

    private AccountInfo fetchAccountInfoAPI(final Account account) {
        final AccountInfo ai = new AccountInfo();
        final String type = PluginJSonUtils.getJsonValue(br, "type");
        final String expireTime = PluginJSonUtils.getJsonValue(br, "expireTime");
        final String traffic = PluginJSonUtils.getJsonValue(br, "traffic");
        long expire_long = 0;
        if (expireTime != null) {
            expire_long = Long.parseLong(expireTime) * 1000l;
        }
        if ("Free".equals(type) || expire_long < System.currentTimeMillis()) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(1);
            ai.setStatus(getPhrase("FREE"));
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus(getPhrase("PREMIUM"));
        }
        if (traffic != null) {
            ai.setTrafficLeft(Long.parseLong(traffic));
        }
        if (expire_long > System.currentTimeMillis()) {
            ai.setValidUntil(Long.parseLong(expireTime) * 1000l);
        }
        return ai;
    }

    private AccountInfo fetchAccountInfoWebsite(final Account account) {
        final AccountInfo ai = new AccountInfo();
        boolean hours = false;
        if (account.getType() == AccountType.PREMIUM) {
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
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            ai.setStatus(getPhrase("PREMIUM"));
        } else {
            account.setMaxSimultanDownloads(1);
            ai.setStatus(getPhrase("FREE"));
        }
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        if (getUseAPI()) {
            loginAPI(account);
        } else {
            loginWebsite(account, force);
        }
    }

    private void loginAPI(final Account account) throws Exception {
        synchronized (lock) {
            try {
                br.setCookiesExclusive(true);
                /* session_id might be stored for API usage */
                final Cookies cookies = account.loadCookies("");
                restoreSession(account);
                if (cookies != null && this.apiSession != null) {
                    br.setCookies(this.getHost(), cookies);
                    getPageAPI(getAPIProtocol() + this.getHost() + "/login/json_checkLogin");
                    if (this.getAPIStatuscode(this.br) == api_status_all_ok) {
                        logger.info("Successfully re-used previous session");
                        return;
                    }
                    logger.info("Failed to re-use previous session --> Performing full login");
                }
                postPageAPI(getAPIProtocol() + this.getHost() + "/login/json", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                this.apiSession = PluginJSonUtils.getJsonValue(br, "session");
                if (this.apiSession == null) {
                    /* Actually this should not happen as errorhandling is done inside postPageAPI() */
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
                saveSession(account);
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void saveSession(final Account acc) {
        acc.setProperty("apisession", this.apiSession);
    }

    private void restoreSession(final Account acc) {
        this.apiSession = acc.getStringProperty("apisession", null);
    }

    private void loginWebsite(Account account, boolean force) throws Exception {
        synchronized (lock) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    return;
                }
                getPage("http://" + this.getHost() + "/login");
                Form login = br.getForm(0);
                if (login == null) {
                    logger.warning("Couldn't find login form");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("NO_LOGIN_FORM"));
                }
                login.put("user_email", Encoding.urlEncode(account.getUser()));
                login.put("user_password", Encoding.urlEncode(account.getPass()));
                submitForm(login);
                getPage("/");
                if (br.containsHTML("(Konto:[\r\t\n ]+)*Darmowe")) {
                    account.setType(AccountType.FREE);
                } else if ((br.containsHTML("(Konto:[\r\t\n ]+)*Premium \\(<b>\\d+ dni</b>\\)")) || (br.containsHTML("(Konto:[\r\t\n ]+)+Premium \\(<b><span style=\"color: red\">\\d+ godzin</span></b>\\)"))) {
                    account.setType(AccountType.PREMIUM);
                } else {
                    /* Unknown account type */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("LOGIN_ERROR"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (getUseAPI()) {
            handlePremiumAPI(downloadLink, account);
        } else {
            handlePremiumWebsite(downloadLink, account);
        }
    }

    public void handlePremiumAPI(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        loginAPI(account);
        if (account.getType() == AccountType.FREE) {
            handleDownloadAPI(downloadLink, true, 1, false, "directlink_freeaccount");
        } else {
            handleDownloadAPI(downloadLink, true, -4, true, "directlink_premiumaccount");
        }
    }

    @SuppressWarnings("deprecation")
    public void handlePremiumWebsite(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        loginWebsite(account, true);
        getPage(downloadLink.getDownloadURL());
        if (account.getType() == AccountType.FREE) {
            doFreeWebsite(downloadLink, true, 1);
            return;
        }
        getPage(downloadLink.getDownloadURL());
        doSomething();
        String dllink = getDllinkWebsite();
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -4);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            doSomething();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("FINAL_LINK_ERROR"));
        }
        dl.startDownload();
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

    private void getPageAPI(String url) throws Exception {
        if (apiSession != null) {
            if (!url.contains("?")) {
                url += "?";
            } else {
                url += "&";
            }
            url += "session=" + Encoding.urlEncode(apiSession);
        }
        getPage(url);
        handleErrorsAPI();
    }

    @SuppressWarnings("deprecation")
    private void postPageAPI(final String url, String postData) throws Exception {
        if (apiSession != null) {
            if (postData == null || postData.equals("")) {
                postData = "";
            } else {
                postData += "&";
            }
            postData += "session=" + Encoding.urlEncode(apiSession);
        }
        postPage(url, postData);
        handleErrorsAPI();
    }

    private void handleErrorsAPI() throws PluginException {
        final int code = getAPIStatuscode(this.br);
        String msg = PluginJSonUtils.getJsonValue(this.br, "msg");
        if (msg == null) {
            msg = "Unknown API error";
        }
        switch (code) {
        case api_status_all_ok:
            /* Everything ok */
            break;
        case 0:
            /* Fatal API error - this should never happen! */
            throw new PluginException(LinkStatus.ERROR_FATAL, msg);
        case 1:
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        case 2:
            /* Folderlink does not exist --> This errorcode should only happen inside jd.plugins.decrypter.CatShareNetFolder !! */
            if (StringUtils.containsIgnoreCase(br.getURL(), "/login/")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, msg);
        case 8:
            /* Daily downloadlimit reached (usually happens for premium accounts) */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        case 9:
            /* Not enough traffic available */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, msg, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        case 10:
            /* WTF - "U didnt follow the rules!" */
            throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL API error: " + msg);
        case 11:
            /* Wrong captcha input */
            throw new PluginException(LinkStatus.ERROR_CAPTCHA, msg);
        case 12:
            /* "You have to wait" --> (Free) limit reached */
            long wait = 3 * 60 * 60 * 1001l;
            final String wait_str = PluginJSonUtils.getJsonValue(this.br, "wait_time");
            /* wait_time can also be of type Boolean! */
            if (wait_str != null && wait_str.matches("\\d+")) {
                wait = 1001 * Long.parseLong(wait_str);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, msg, wait);
        case 13:
            /* File offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, msg);
        case 14:
            /*
             * Too many links (if someone tried to check more than X(see CheckLinks mass-linkchecker-function) links per request - this
             * should NEVER happen!
             */
            throw new PluginException(LinkStatus.ERROR_FATAL, msg);
        default:
            throw new PluginException(LinkStatus.ERROR_FATAL, msg);
        }
    }

    public static int getAPIStatuscode(final Browser br) {
        int code = -1;
        final String code_str = PluginJSonUtils.getJsonValue(br, "code");
        if (code_str != null) {
            code = Integer.parseInt(code_str);
        }
        return code;
    }

    @SuppressWarnings("deprecation")
    private String getLinkid(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]{15,16})$").getMatch(0);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
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
        if (acc.getType() == AccountType.FREE) {
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
                                                      put("USE_API", "Use API (recommended!)");
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
                                                      put("USE_API", "Używaj API (rekomendowane!)");
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

    private void setConfigElements() {
        /*
         * 2016-06-29: Disabled this setting on admin request - if there are API issues in the future, this can be re-enabled! Keep in mind
         * that website might change so the website handling might be broken and needs fixing first before it can be used!!
         */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), USE_API, JDL.L("plugins.hoster.CatShareNet.useAPI", getPhrase("USE_API"))).setDefaultValue(defaultUSE_API).setEnabled(false));
    }

    private boolean getUseAPI() {
        return this.getPluginConfig().getBooleanProperty(USE_API, defaultUSE_API);
    }

    // for the decrypter, so we have only one session of antiddos
    public void postPage(final String url, final String arg) throws Exception {
        super.postPage(url, arg);
    }
}