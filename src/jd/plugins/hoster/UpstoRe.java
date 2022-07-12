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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
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
    private final String                   MAINPAGE                      = "http://upstore.net";
    private final String                   INVALIDLINKS                  = "https?://[^/]+/(faq|privacy|terms|d/|aff|login|account|dmca|imprint|message|panel|premium|contacts)";
    private static String[]                IPCHECK                       = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };
    private final String                   EXPERIMENTALHANDLING          = "EXPERIMENTALHANDLING";
    private Pattern                        IPREGEX                       = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static AtomicReference<String> lastIP                        = new AtomicReference<String>();
    private static AtomicReference<String> currentIP                     = new AtomicReference<String>();
    private static Map<String, Long>       blockedIPsMap                 = new HashMap<String, Long>();
    private static Object                  CTRLLOCK                      = new Object();
    private String                         PROPERTY_LASTIP               = "UPSTORE_PROPERTY_LASTIP";
    private static final String            PROPERTY_LASTDOWNLOAD         = "UPSTORE_lastdownload_timestamp";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("upsto.re/", "upstore.net/").replace("http://", "https://"));
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        getPage(link.getPluginPatternMatcher());
        if (isOffline1()) {
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
        if (filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private boolean isOffline1() {
        return br.containsHTML("(?i)>\\s*File not found<|>File was deleted by owner or due to a violation of service rules\\.|not found|>SmartErrors powered by");
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final String directurlproperty = "freelink";
        final boolean resume = false;
        final int maxchunks = 1;
        if (this.attemptStoredDownloadurlDownload(link, directurlproperty, resume, maxchunks)) {
            logger.info("Re-using stored directurl");
        } else {
            requestFileInformation(link);
            handleErrorsHTML();
            currentIP.set(this.getIP());
            synchronized (CTRLLOCK) {
                /* Load list of saved IPs + timestamp of last download */
                final Object lastdownloadmap = this.getPluginConfig().getProperty(PROPERTY_LASTDOWNLOAD);
                if (lastdownloadmap != null && lastdownloadmap instanceof HashMap && blockedIPsMap.isEmpty()) {
                    blockedIPsMap = (Map<String, Long>) lastdownloadmap;
                }
            }
            {
                final Form f = br.getFormBySubmitvalue("Slow+download");
                if (f != null) {
                    final Browser br2 = br.cloneBrowser();
                    sleep(6000, link);
                    br2.getPage("/main/acceptterms/?s=" + f.getInputField("s").getValue() + "&ajax=1");
                    final String t = br2.getRegex("name=t\\]'\\)\\.val\\('(\\d+)").getMatch(0);
                    if (t == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        f.put("t", t);
                    }
                    final String h = br2.getRegex("\\+'(.*?)'").getMatch(0);
                    if (h == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        final InputField oldH = f.getInputField("h");
                        final String oldHValue = oldH.getValue();
                        f.put("h", oldHValue.substring(0, Math.min(10, oldHValue.length())) + h);
                    }
                    sleep(6000, link);
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
            // captcha form
            final Form captchaForm = br.getFormBySubmitvalue("Get+download+link");
            // Waittime can be skipped
            final long timeBefore = System.currentTimeMillis();
            final String waittimeStr = br.getRegex("var sec = (\\d+)").getMatch(0);
            final int waitFull = Integer.parseInt(waittimeStr);
            int wait = waitFull;
            logger.info("Detected total waittime: " + wait);
            if (containsHCaptcha(this.br)) {
                final CaptchaHelperHostPluginHCaptcha hCaptcha = new CaptchaHelperHostPluginHCaptcha(this, br);
                final String captchaResponse = hCaptcha.getToken();
                final int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000);
                wait -= passedTime - 11;
                if (wait > 0) {
                    sleep(wait * 1000l, link);
                }
                captchaForm.put("kpw", "spam");
                captchaForm.put("antispam", "spam");
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(captchaResponse));
                captchaForm.put("h-captcha-response", Encoding.urlEncode(captchaResponse));
            } else {
                logger.warning("No captchaForm present at all");
            }
            if (captchaForm != null) {
                submitForm(captchaForm);
            }
            if (br.containsHTML("limit for today|several files recently")) {
                setDownloadStarted(link, 0);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 3 * 60 * 60 * 1000l);
            }
            String dllink = br.getRegex("<div style=\"margin: 10px auto 20px\" class=\"center\">\\s*<a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://d\\d+\\.upstore\\.net/[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                final String reconnectWait = br.getRegex("Please wait (\\d+) minutes before downloading next file").getMatch(0);
                if (reconnectWait != null) {
                    final long waitmillis = Long.parseLong(reconnectWait) * 60 * 1000l;
                    setDownloadStarted(link, waitmillis);
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitmillis + FREE_RECONNECTWAIT_ADDITIONAL);
                }
                final String error = br.getRegex("<span class=\"error\"[^>]*>([^<]+)</span>").getMatch(0);
                if (error != null) {
                    if (error.matches("(?i)No slots for free users in your area at the moment, try again later")) {
                        /* 2021-07-02 */
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, error, 10 * 60 * 1000l);
                    } else {
                        /* Unknown error */
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 5 * 60 * 1000l);
                    }
                }
                handleErrorsJson();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            /*
             * The download attempt already triggers reconnect waittime! Save timestamp here to calculate correct remaining waittime later!
             */
            setDownloadStarted(link, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                handleErrorsHTML();
                handleServerErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directurlproperty, dllink);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directurlproperty, final boolean resume, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(directurlproperty);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    private void handleServerErrors() throws PluginException {
        if (br.containsHTML("(?i)not found")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'not found'", 30 * 60 * 1000l);
        }
    }

    private void handleErrorsJson() throws PluginException {
        /*
         * Example error json:
         * {"errors":["Sorry, download server with your file is temporary unavailable... Try again later or contact support."]}
         */
        if (this.br.containsHTML("(?i)Sorry, download server with your file is temporarily unavailable")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        }
    }

    private void handleErrorsHTML() throws PluginException {
        /* Example: "<span class="error">File size is larger than 2 GB. Unfortunately, it can be downloaded only with premium</span>" */
        if (isOffline1()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("(?i)File size is larger than|it can be downloaded only with premium")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (br.containsHTML("(?i)>\\s*This file is available only for Premium users")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (br.containsHTML("(?i)>\\s*Server for free downloads is overloaded<")) {
            /* Here some errors that should only happen in free(account) mode: */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Server for free downloads is overloaded'", 30 * 60 * 1000l);
        } else if (br.containsHTML("(?i)>\\s*Server with file not found<")) {
            // Same server error (displayed differently) also exists for premium users
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Server with file not found'", 60 * 60 * 1000l);
        }
        /* 2021-11-22: New attempt */
        final String otherError = br.getRegex("<span class=\"error\">([^<>\"]+)</span>").getMatch(0);
        if (otherError != null) {
            /* 2021-11-22 e.g. <span class="error">Under maintenance. Free downloads are not available at the moment.</span> */
            if (otherError.matches("(?i)Under maintenance.*Free downloads are not available at the moment.*")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, otherError, 30 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, otherError, 5 * 60 * 1000l);
            }
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

    private void login(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setCookie(getHost(), "lang", "en");
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(MAINPAGE, cookies);
                    getPage("https://" + this.getHost());
                    if (!this.isLoggedinHTML(br)) {
                        logger.info("Cookie login failed");
                        br.clearCookies(MAINPAGE);
                    } else {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(MAINPAGE), "");
                        return;
                    }
                }
                logger.info("Full login required");
                // dump previous set user-agent
                synchronized (agent) {
                    agent.remove(getHost());
                }
                if (!isMail(account.getUser())) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your mailadress in the 'username' field!\r\nBitte gib deine E-Mail Adresse in das 'Benutzername' Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // goto first page
                br.setCookie(getHost(), "lang", "en");
                getPage("https://" + this.getHost());
                getPage("https://" + this.getHost() + "/account/login/");
                final Form login = br.getFormbyActionRegex(".+/login.*");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("email", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                if (login.containsHTML(regexLoginCaptcha)) {
                    final String cap = br.getRegex(regexLoginCaptcha).getMatch(-1);
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), MAINPAGE, true);
                    final String code = getCaptchaCode(cap, dummyLink);
                    if (code == null) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else {
                        login.put("captcha", Encoding.urlEncode(code));
                    }
                }
                submitForm(login);
                if (br.containsHTML("(?i)>\\s*Wrong email or password\\.\\s*<")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.containsHTML(regexLoginCaptcha)) {
                    // incorrect captcha, or form values changed
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else if (!this.isLoggedinHTML(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Save cookies
                account.saveCookies(br.getCookies(MAINPAGE), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
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

    /**
     * Array containing all required premium cookies!
     *
     * @return
     */
    private String[] getLoginCookies() {
        return new String[] { "usid", "upst" };
    }

    private boolean isMail(final String parameter) {
        return parameter.matches(".+@.+");
    }

    // please note: this might cause cookies to go out of session and errors else where...
    // effectively 4 times a day!
    private final String regexLoginCaptcha = "/captcha/\\?\\d+";

    private long getPremiumTill(Browser br) {
        long result = -1;
        String expire = br.getRegex("(?i)premium till\\s*(\\d{1,2}/\\d{1,2}/\\d{2})").getMatch(0);
        if (expire != null) {
            result = TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", null);
        }
        expire = br.getRegex("(?i)premium till\\s*([a-zA-Z.]+\\s*\\d{1,2}\\s*,\\s*(\\d{4}|\\d{2}))").getMatch(0);
        if (expire != null && result == -1) {
            result = TimeFormatter.getMilliSeconds(expire, "MMMM dd','yyyy", Locale.ENGLISH);
        }
        if (result > 0) {
            // expire at the end of the day, not the beginning
            result = result + (24 * 60 * 60 * 1000l);
            final long offset = TimeZone.getDefault().getOffset(result);
            logger.info("Apply Timezone offset:" + offset);
            result += offset;
        }
        return result;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        this.login(account, true);
        // Make sure that the language is correct
        getPage((br.getHttpConnection() == null ? MAINPAGE.replace("http://", "https://") : "") + "/?lang=en");
        getPage((br.getHttpConnection() == null ? MAINPAGE.replace("http://", "https://") : "") + "/stat/download/?lang=en");
        // Check for never-ending premium accounts
        if (!br.containsHTML(lifetimeAccount)) {
            final long validUntil = getPremiumTill(br);
            if (validUntil == -1) {
                if (br.containsHTML("(?i)unlimited premium")) {
                    ai.setValidUntil(-1);
                    ai.setStatus("Unlimited Premium Account");
                } else {
                    throw new AccountUnavailableException("\r\nFree Accounts are not supported for this host!\r\nKostenlose Accounts dieses Hosters werden nicht unterstützt!", 5 * 60 * 1000l);
                }
            } else {
                ai.setValidUntil(validUntil);
            }
        }
        // traffic is not unlimited they have 20GiB/day fair use. see ticket HZI-220-58438
        // ai.setUnlimitedTraffic();
        // this is in MiB, more accurate than the top rounded figure
        // final String trafficUsed = br.getRegex(">Total:</td>\\s*<td>([\\d+\\.]+)<").getMatch(0);
        final String traffic[] = br.getRegex("(?i)Downloaded in last \\d+ hours: ([\\d+\\.]+) of ([\\d+\\.]+) GB").getRow(0);
        final long trafficDaily = SizeFormatter.getSize(traffic[1] + "GiB");
        final long trafficLeft = trafficDaily - SizeFormatter.getSize(traffic[0] + "GiB");
        ai.setTrafficLeft(trafficLeft);
        ai.setTrafficMax(trafficDaily);
        ai.setStatus("Premium Account");
        return ai;
    }

    // lifetime account
    private String lifetimeAccount = "eternal premium";

    private boolean isLoggedinHTML(final Browser br) {
        if (br.containsHTML("account/logout/?\"")) {
            return true;
        } else {
            return false;
        }
    }

    private final String premDlLimit = "It is strange, but you have reached a download limit for today";

    private AccountInfo trafficLeft(Account account) throws PluginException {
        synchronized (account) {
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
        this.login(account, false);
        br.setFollowRedirects(false);
        getPage(link.getPluginPatternMatcher());
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
            dllink = br.getRegex("\"ok\"\\s*:\\s*\"(https?:[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            handleErrorsJson();
            if (!this.isLoggedinHTML(br)) {
                throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
            }
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink).replace("\\", ""), true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            handleErrorsHTML();
            this.handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
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
        } else if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        } else {
            return false;
        }
    }
}