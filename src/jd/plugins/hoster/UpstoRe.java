//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.CaptchaException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "upstore.net", "upsto.re" }, urls = { "https?://(www\\.)?(upsto\\.re|upstore\\.net)/[A-Za-z0-9]+", "ejnz905rj5o0jt69pgj50ujz0zhDELETE_MEew7th59vcgzh59prnrjhzj0" })
public class UpstoRe extends antiDDoSForHost {
    public UpstoRe(PluginWrapper wrapper) {
        super(wrapper);
        if ("upstore.net".equals(getHost())) {
            this.enablePremium("http://upstore.net/premium/");
        }
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://upstore.net/terms/";
    }

    @Override
    public String rewriteHost(String host) {
        if ("upsto.re".equals(getHost())) {
            if (host == null || "upsto.re".equals(host)) {
                return "upstore.net";
            }
        }
        return super.rewriteHost(host);
    }

    /* Constants (limits) */
    private static final long              FREE_RECONNECTWAIT            = 1 * 60 * 60 * 1000L;
    private static final long              FREE_RECONNECTWAIT_ADDITIONAL = 60 * 1000l;
    private static Object                  LOCK                          = new Object();
    private final String                   MAINPAGE                      = "http://upstore.net";
    private final String                   INVALIDLINKS                  = "https?://[^/]+/(faq|privacy|terms|d/|aff|login|account|dmca|imprint|message|panel|premium|contacts)";
    private static String[]                IPCHECK                       = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };
    private final String                   EXPERIMENTALHANDLING          = "EXPERIMENTALHANDLING";
    private Pattern                        IPREGEX                       = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static AtomicReference<String> lastIP                        = new AtomicReference<String>();
    private static AtomicReference<String> currentIP                     = new AtomicReference<String>();
    private static HashMap<String, Long>   blockedIPsMap                 = new HashMap<String, Long>();
    private static Object                  CTRLLOCK                      = new Object();
    private String                         PROPERTY_LASTIP               = "UPSTORE_PROPERTY_LASTIP";
    private static final String            PROPERTY_LASTDOWNLOAD         = "UPSTORE_lastdownload_timestamp";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("upsto.re/", "upstore.net/").replace("http://", "https://"));
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    /**
     * defines custom browser requirements
     *
     * @author raztoki
     */
    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setCookie("http://upstore.net/", "lang", "en");
        }
        return prepBr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.setFollowRedirects(true);
        if (link.getDownloadURL().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found<|>File was deleted by owner or due to a violation of service rules\\.|not found|>SmartErrors powered by")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.containsHTML("name=\"hash\"") && !this.br.containsHTML("class=\"features (minus|plus)\"")) {
            /* Probably not a file url. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex fileInfo = br.getRegex("<h2 style=\"margin:0\">([^<>\"]*?)</h2>[\t\n\r ]+<div class=\"comment\">([^<>\"]*?)</div>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Download file ([^<>\"]*?) \\&mdash; Upload, store \\& share your files on").getMatch(0);
        }
        String filesize = fileInfo.getMatch(1);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleErrorsHTML();
        currentIP.set(this.getIP());
        synchronized (CTRLLOCK) {
            /* Load list of saved IPs + timestamp of last download */
            final Object lastdownloadmap = this.getPluginConfig().getProperty(PROPERTY_LASTDOWNLOAD);
            if (lastdownloadmap != null && lastdownloadmap instanceof HashMap && blockedIPsMap.isEmpty()) {
                blockedIPsMap = (HashMap<String, Long>) lastdownloadmap;
            }
        }
        String dllink = checkDirectLink(downloadLink, "freelink");
        if (dllink == null) {
            {
                final Form f = br.getFormBySubmitvalue("Slow+download");
                if (f != null) {
                    submitForm(f);
                }
            }
            handleErrorsHTML();
            /**
             * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached!
             */
            if (this.getPluginConfig().getBooleanProperty(EXPERIMENTALHANDLING, default_eh)) {
                /*
                 * If the user starts a download in free (unregistered) mode the waittime is on his IP. This also affects free accounts if
                 * he tries to start more downloads via free accounts afterwards BUT nontheless the limit is only on his IP so he CAN
                 * download using the same free accounts after performing a reconnect!
                 */
                long lastdownload = getPluginSavedLastDownloadTimestamp();
                long passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload;
                if (passedTimeSinceLastDl < FREE_RECONNECTWAIT) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, FREE_RECONNECTWAIT - passedTimeSinceLastDl);
                }
            }
            // USE FORMS !!!!
            // captcha form
            final Form captcha = br.getFormBySubmitvalue("Get+download+link");
            // Waittime can be skipped
            final long timeBefore = System.currentTimeMillis();
            final String rcID = br.getRegex("Recaptcha\\.create\\('([^<>\"]*?)'").getMatch(0);
            if (rcID != null && captcha != null) {
                final Recaptcha rc = new Recaptcha(br, this);
                rc.setId(rcID);
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = null;
                try {
                    c = getCaptchaCode("recaptcha", cf, downloadLink);
                } catch (final Throwable e) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                int wait = 60;
                String waittime = br.getRegex("var sec = (\\d+)").getMatch(0);
                if (waittime != null) {
                    wait = Integer.parseInt(waittime);
                }
                int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
                wait -= passedTime;
                if (wait > 0) {
                    sleep(wait * 1000l, downloadLink);
                }
                final String kpw = br.getRegex("\\(\\{'type':'hidden','name':'(\\w+)'\\}\\).val\\(window\\.antispam").getMatch(0);
                captcha.put("recaptcha_challenge_field", Encoding.urlEncode(rc.getChallenge()));
                captcha.put("recaptcha_response_field", Encoding.urlEncode(c));
                // some javascript crapola
                final String antispam = Encoding.urlEncode(getSoup());
                captcha.put("antispam", antispam);
                captcha.put((kpw != null ? kpw : "kpw"), antispam);
                submitForm(captcha);
                if (br.containsHTML("limit for today|several files recently")) {
                    setDownloadStarted(downloadLink, 0);
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 3 * 60 * 60 * 1000l);
                }
            }
            dllink = br.getRegex("<div style=\"margin: 10px auto 20px\" class=\"center\">\\s*<a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://d\\d+\\.upstore\\.net/[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                final String reconnectWait = br.getRegex("Please wait (\\d+) minutes before downloading next file").getMatch(0);
                if (reconnectWait != null) {
                    final long waitmillis = Long.parseLong(reconnectWait) * 60 * 1000l;
                    setDownloadStarted(downloadLink, waitmillis);
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitmillis + FREE_RECONNECTWAIT_ADDITIONAL);
                }
                if (br.containsHTML("Recaptcha\\.create\\('([^<>\"]*?)'")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                handleErrorsJson();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            sleep(3000l, downloadLink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        /* The download attempt already triggers reconnect waittime! Save timestamp here to calculate correct remaining waittime later! */
        setDownloadStarted(downloadLink, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        if (br.containsHTML("not found")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'not found'", 30 * 60 * 1000l);
        }
    }

    private void handleErrorsJson() throws PluginException {
        /*
         * Example error json:
         * {"errors":["Sorry, download server with your file is temporary unavailable... Try again later or contact support."]}
         */
        if (this.br.containsHTML("Sorry, download server with your file is temporary unavailable")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
    }

    private void handleErrorsHTML() throws PluginException {
        /* Example: "<span class="error">File size is larger than 2 GB. Unfortunately, it can be downloaded only with premium</span>" */
        if (this.br.containsHTML("File size is larger than|it can be downloaded only with premium")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (br.containsHTML(">This file is available only for Premium users<")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        /* Here some errors that should only happen in free(account) mode: */
        if (br.containsHTML(">Server for free downloads is overloaded<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Server for free downloads is overloaded'", 30 * 60 * 1000l);
        }
        // Same server error (displayed differently) also exists for premium users
        if (br.containsHTML(">Server with file not found<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Server with file not found'", 60 * 60 * 1000l);
        }
    }

    private String getSoup() {
        final Random r = new Random();
        final String soup = "()_+-=:;?.,ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String v = "";
        for (int i = 0; i < 20; i++) {
            v = v + soup.charAt(r.nextInt(soup.length()));
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setCookie(getHost(), "lang", "en");
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        // re-use same agent from cached session.
                        final String ua = account.getStringProperty("ua", null);
                        if (ua != null && !ua.equals(userAgent.get())) {
                            // cloudflare routine sets user-agent on first request.
                            userAgent.set(ua);
                        }
                        br.setCookie(getHost(), "lang", "en");
                        return;
                    }
                }
                // dump previous set user-agent
                if (userAgent.get() != null) {
                    userAgent.set(null);
                }
                if (!isMail(account.getUser())) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your mailadress in the 'username' field!\r\nBitte gib deine E-Mail Adresse in das 'Benutzername' Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // goto first page
                br.setCookie(getHost(), "lang", "en");
                getPage("https://upstore.net/");
                // getPage("/account/soclogin/?url=https%3A%2F%2Fupstore.net%2F");
                postPage("/account/login/", "url=https%253A%252F%252Fupstore.net%252F&send=Login&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                // some times they force captcha
                final String cap = br.getRegex(regexLoginCaptcha).getMatch(-1);
                if (cap != null) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), MAINPAGE, true);
                    String code = null;
                    try {
                        code = getCaptchaCode(cap, dummyLink);
                    } catch (Throwable e) {
                        if (e instanceof CaptchaException) {
                            // JD2 reference to skip button we should abort!
                            throw (CaptchaException) e;
                        }
                    }
                    if (code == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nCaptcha required and wasn't provided, account disabled!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    postPage(br.getURL(), "url=http%253A%252F%252Fupstore.net%252F&send=sign+in&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&captcha=" + Encoding.urlEncode(code));
                    if (br.containsHTML(regexLoginCaptcha)) {
                        // incorrect captcha, or form values changed
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nIncorrect catpcha, account disabled!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (!browserCookiesMatchLoginCookies(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Save cookies
                final HashMap<String, String> cookies = getBrowsersLoginCookies(br);
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                account.setProperty("ua", br.getHeaders().get("User-Agent"));
                account.setProperty("lastlogin", System.currentTimeMillis());
            } catch (final PluginException e) {
                dumpCachedLoginSession(account);
                throw e;
            }
        }
    }

    private void dumpCachedLoginSession(final Account account) {
        account.setProperty("cookies", Property.NULL);
        account.setProperty("lastlogin", Property.NULL);
        account.setProperty("ua", Property.NULL);
        userAgent.set(null);
    }

    /**
     * saves cookies to HashMap from provided browser
     *
     * @author raztoki
     * @param br
     * @return
     */
    private HashMap<String, String> getBrowsersLoginCookies(Browser br) {
        final HashMap<String, String> cookies = new HashMap<String, String>();
        final Cookies add = br.getCookies(MAINPAGE);
        for (final Cookie c : add.getCookies()) {
            if (cookieContainsLoginKey(c)) {
                cookies.put(c.getKey(), c.getValue());
            }
        }
        return cookies;
    }

    /**
     * returns true if provided Cookie contains keyname is contained within getLoginCookies()
     *
     * @author raztoki
     * @param c
     * @return
     */
    private boolean cookieContainsLoginKey(final Cookie c) {
        if (c != null) {
            for (final String cookieKey : getLoginCookies()) {
                if (c.getKey().equalsIgnoreCase(cookieKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMail(final String parameter) {
        return parameter.matches(".+@.+");
    }

    // please note: this might cause cookies to go out of session and errors else where...
    // effectively 4 times a day!
    private final String regexLoginCaptcha = "/captcha/\\?\\d+";

    private long getPremiumTill(Browser br) {
        long result = -1;
        String expire = br.getRegex("premium till\\s*(\\d{1,2}/\\d{1,2}/\\d{2})").getMatch(0);
        if (expire != null) {
            result = TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", null);
        }
        expire = br.getRegex("premium till\\s*([a-zA-Z.]+\\s*\\d{1,2}\\s*,\\s*(\\d{4}|\\d{2}))").getMatch(0);
        if (expire != null && result == -1) {
            result = TimeFormatter.getMilliSeconds(expire, "MMMM dd','yyyy", Locale.ENGLISH);
        }
        return result;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        areWeStillLoggedIn(account);
        // Make sure that the language is correct
        getPage((br.getHttpConnection() == null ? MAINPAGE.replace("http://", "https://") : "") + "/?lang=en");
        getPage((br.getHttpConnection() == null ? MAINPAGE.replace("http://", "https://") : "") + "/stat/download/?lang=en");
        // Check for never-ending premium accounts
        if (!br.containsHTML(lifetimeAccount)) {
            final long validUntil = getPremiumTill(br);
            if (validUntil == -1) {
                if (br.containsHTML("unlimited premium")) {
                    ai.setValidUntil(-1);
                    ai.setStatus("Unlimited Premium Account");
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree Accounts are not supported for this host!\r\nKostenlose Accounts dieses Hosters werden nicht unterstützt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(validUntil);
            }
        }
        // traffic is not unlimited they have 20GiB/day fair use. see ticket HZI-220-58438
        // ai.setUnlimitedTraffic();
        // this is in MiB, more accurate than the top rounded figure
        // final String trafficUsed = br.getRegex(">Total:</td>\\s*<td>([\\d+\\.]+)<").getMatch(0);
        final String traffic[] = br.getRegex("Downloaded in last \\d+ hours: ([\\d+\\.]+) of ([\\d+\\.]+) GB").getRow(0);
        final long trafficDaily = SizeFormatter.getSize(traffic[1] + "GiB");
        final long trafficLeft = trafficDaily - SizeFormatter.getSize(traffic[0] + "GiB");
        ai.setTrafficLeft(trafficLeft);
        ai.setTrafficMax(trafficDaily);
        ai.setStatus("Premium Account");
        account.setValid(true);
        return ai;
    }

    // lifetime account
    private String lifetimeAccount = "eternal premium";

    /**
     * Method to determine if current cookie session is still valid.
     *
     * @author raztoki
     * @param account
     * @return
     * @throws Exception
     */
    private boolean areWeStillLoggedIn(Account account) throws Exception {
        synchronized (LOCK) {
            boolean isFollowingRedirects = br.isFollowingRedirects();
            try {
                br = new Browser();
                br.setFollowRedirects(true);
                login(account, false);
                if (br.getHttpConnection() != null) {
                    // full login just happened and verified as current! otherwise exception would have been thrown!
                    return true;
                }
                // send a get page to mainpage to see if premium cookie is cleared.
                getPage(MAINPAGE);
                // upstore doesn't remove invalid cookies, so we need to also check against account types!
                if (browserCookiesMatchLoginCookies(br) && (br.containsHTML(this.lifetimeAccount) || br.containsHTML("unlimited premium") || getPremiumTill(br) > 0)) {
                    // save these incase they changed value.
                    final HashMap<String, String> cookies = getBrowsersLoginCookies(br);
                    account.setProperty("cookies", cookies);
                } else {
                    dumpCachedLoginSession(account);
                    br = new Browser();
                    login(account, false);
                }
                // there is no false as exception should be thrown, or full login performed we are always true!
                return true;
            } finally {
                br.setFollowRedirects(isFollowingRedirects);
            }
        }
    }

    /**
     * Array containing all required premium cookies!
     *
     * @return
     */
    private String[] getLoginCookies() {
        return new String[] { "usid" };
    }

    /**
     * If default browser contains ALL cookies within 'loginCookies' array, it will return true<br />
     * <br />
     * NOTE: loginCookies[] can only contain true names! Remove all dead names from array!
     *
     * @author raztoki
     */
    private boolean browserCookiesMatchLoginCookies(final Browser br) {
        final Cookies cookies = br.getCookies(MAINPAGE);
        // simple math logic here
        int i = 0;
        if (cookies != null) {
            for (String loginCookie : getLoginCookies()) {
                for (final Cookie cookie : cookies.getCookies()) {
                    if (cookie.getKey().equalsIgnoreCase(loginCookie) && (cookie.getValue() != null || cookie.getValue().length() != 0)) {
                        i++;
                    }
                }
            }
        }
        if (i != getLoginCookies().length) {
            return false;
        } else {
            return true;
        }
    }

    private final String premDlLimit = "It is strange, but you have reached a download limit for today";

    private AccountInfo trafficLeft(Account account) throws PluginException {
        synchronized (LOCK) {
            AccountInfo ai = account.getAccountInfo();
            String maxLimit = br.getRegex(premDlLimit + " \\((\\d+ (MB|GB|TB))\\)").getMatch(0);
            if (maxLimit != null) {
                ai.setTrafficMax(SizeFormatter.getSize(maxLimit));
            }
            ai.setTrafficLeft(0);
            account.setAccountInfo(ai);
        }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Downloadlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        areWeStillLoggedIn(account);
        br.setFollowRedirects(false);
        getPage(link.getDownloadURL());
        if (br.containsHTML(premDlLimit)) {
            trafficLeft(account);
        }
        // Directdownload enabled?
        String dllink = br.getRedirectLocation();
        // No directdownload? Let's "click" on download
        if (dllink == null) {
            postPage("//upstore.net/load/premium/", "js=1&hash=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
            if (br.containsHTML(premDlLimit)) {
                trafficLeft(account);
            }
            dllink = br.getRegex("\"ok\":\"(https?:[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            handleErrorsJson();
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink).replace("\\", ""), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            this.handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = prepBrowser(br.cloneBrowser(), Browser.getHost(dllink));
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    private void setDownloadStarted(final DownloadLink dl, final long remaining_reconnect_wait) throws Exception {
        synchronized (CTRLLOCK) {
            final long timestamp_download_started;
            if (remaining_reconnect_wait > 0) {
                /*
                 * FREE_RECONNECTWAIT minus remaining wait = We know when the user started his download - we want to get the timestamp. Add
                 * 1 minute to make sure that we wait long enough!
                 */
                long timePassed = FREE_RECONNECTWAIT - remaining_reconnect_wait - FREE_RECONNECTWAIT_ADDITIONAL;
                /* Errorhandling for invalid values */
                if (timePassed < 0) {
                    timePassed = 0;
                }
                timestamp_download_started = System.currentTimeMillis() - timePassed;
            } else {
                /*
                 * Nothing given unknown starttime, wrong inputvalue 'remaining_reconnect_wait' or user has started the download just now.
                 */
                timestamp_download_started = System.currentTimeMillis();
            }
            blockedIPsMap.put(currentIP.get(), timestamp_download_started);
            setIP(dl, null);
            getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD, blockedIPsMap);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean setIP(final DownloadLink link, final Account account) throws Exception {
        synchronized (IPCHECK) {
            if (currentIP.get() != null && !new Regex(currentIP.get(), IPREGEX).matches()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (ipChanged(link) == false) {
                // Static IP or failure to reconnect! We don't change lastIP
                logger.warning("Your IP hasn't changed since last download");
                return false;
            } else {
                String lastIP = currentIP.get();
                link.setProperty(PROPERTY_LASTIP, lastIP);
                UpstoRe.lastIP.set(lastIP);
                getPluginConfig().setProperty(PROPERTY_LASTIP, lastIP);
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
    }

    private long getPluginSavedLastDownloadTimestamp() {
        long lastdownload = 0;
        synchronized (blockedIPsMap) {
            final Iterator<Entry<String, Long>> it = blockedIPsMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Long> ipentry = it.next();
                final String ip = ipentry.getKey();
                final long timestamp = ipentry.getValue();
                if (System.currentTimeMillis() - timestamp >= FREE_RECONNECTWAIT) {
                    /* Remove old entries */
                    it.remove();
                }
                if (ip.equals(currentIP.get())) {
                    lastdownload = timestamp;
                }
            }
        }
        return lastdownload;
    }

    private boolean ipChanged(final DownloadLink link) throws Exception {
        String currIP = null;
        if (currentIP.get() != null && new Regex(currentIP.get(), IPREGEX).matches()) {
            currIP = currentIP.get();
        } else {
            currIP = getIP();
        }
        if (currIP == null) {
            return false;
        }
        String lastIP = link.getStringProperty(PROPERTY_LASTIP, null);
        if (lastIP == null) {
            lastIP = UpstoRe.lastIP.get();
        }
        if (lastIP == null) {
            lastIP = this.getPluginConfig().getStringProperty(PROPERTY_LASTIP, null);
        }
        return !currIP.equals(lastIP);
    }

    private String getIP() throws Exception {
        Browser ip = new Browser();
        String currentIP = null;
        ArrayList<String> checkIP = new ArrayList<String>(Arrays.asList(IPCHECK));
        Collections.shuffle(checkIP);
        Exception exception = null;
        for (String ipServer : checkIP) {
            if (currentIP == null) {
                try {
                    ip.getPage(ipServer);
                    currentIP = ip.getRegex(IPREGEX).getMatch(0);
                    if (currentIP != null) {
                        break;
                    }
                } catch (Exception e) {
                    if (exception == null) {
                        exception = e;
                    }
                }
            }
        }
        if (currentIP == null) {
            if (exception != null) {
                throw exception;
            }
            logger.warning("firewall/antivirus/malware/peerblock software is most likely is restricting accesss to JDownloader IP checking services");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return currentIP;
    }

    private final boolean default_eh = false;

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EXPERIMENTALHANDLING, "Activate reconnect workaround for freeusers: Prevents having to enter additional captchas in between downloads.").setDefaultValue(default_eh));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
}