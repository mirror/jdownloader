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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.UpstoReConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UpstoRe extends antiDDoSForHost {
    public UpstoRe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://upstore.net/premium/");
    }

    @Override
    public String getAGBLink() {
        return "https://upstore.net/terms/";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "upstore.net", "upsto.re" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]{2,})(/([^/]+))?");
        }
        return ret.toArray(new String[0]);
    }

    /* Constants (limits) */
    private static final long              FREE_RECONNECTWAIT            = 1 * 60 * 60 * 1000L;
    private static final long              FREE_RECONNECTWAIT_ADDITIONAL = 60 * 1000l;
    private final String                   INVALIDLINKS                  = "(?i)https?://[^/]+/(faq|privacy|terms|d/|aff|login|account|dmca|imprint|message|panel|premium|contacts)";
    private static AtomicReference<String> currentIP                     = new AtomicReference<String>();
    private static Map<String, Long>       blockedIPsMap                 = new HashMap<String, Long>();
    private static Object                  CTRLLOCK                      = new Object();
    private static final String            PROPERTY_last_blockedIPsMap   = "UPSTORE_last_blockedIPsMap";
    /* Don't touch the following! */
    private static final AtomicInteger     freeRunning                   = new AtomicInteger(0);

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    /**
     * Defines custom browser requirements
     *
     * @author raztoki
     */
    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            setDefaultCookies(prepBr);
            final String userDefinedUserAgent = PluginJsonConfig.get(UpstoReConfig.class).getCustomUserAgentHeader();
            if (!StringUtils.isEmpty(userDefinedUserAgent)) {
                /*
                 * Must be relatively recent, shouldn't have the "Ubuntu; " part [of Firefox] or free downloads only 15KiB/s instead of
                 * 75KiB/. RE: https://board.jdownloader.org/showthread.php?t=89506&page=6
                 */
                prepBr.getHeaders().put("User-Agent", userDefinedUserAgent);
            }
        }
        return prepBr;
    }

    private void setDefaultCookies(final Browser br) {
        br.setCookie(this.getHost(), "lang", "en");
    }

    /** Returns the URL used in browser to access content. */
    private String getInternalContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replaceFirst("http://", "https://").replaceFirst("upsto\\.re/", "upstore.net/");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        final String contenturl = getInternalContentURL(link);
        if (!link.isNameSet()) {
            /* Set weak filename */
            final String filenameInsideURL = new Regex(contenturl, this.getSupportedLinks()).getMatch(2);
            if (filenameInsideURL != null) {
                link.setName(Encoding.htmlDecode(filenameInsideURL));
            } else {
                link.setName(fid);
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (contenturl.matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        getPage(contenturl);
        if (isOffline1()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.containsHTML("value=\"" + fid) && !this.br.containsHTML("class=\"features (minus|plus)\"")) {
            /* Probably not a file url. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex fileInfo = br.getRegex("<h2 style=\"margin:0\">([^<>\"]*?)</h2>\\s*<div class=\"comment\">([^<>\"]*?)</div>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) {
            filename = br.getRegex("(?i)<title>Download file ([^<>\"]*?) \\&mdash; Upload, store \\& share your files on").getMatch(0);
        }
        final String filesize = fileInfo.getMatch(1);
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
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            requestFileInformation(link);
            handleErrorsHTML(this.br);
            currentIP.set(new BalancedWebIPCheck(null).getExternalIP().getIP());
            synchronized (CTRLLOCK) {
                /* Load list of saved IPs + timestamp of last download */
                final Object lastdownloadmap = this.getPluginConfig().getProperty(PROPERTY_last_blockedIPsMap);
                if (lastdownloadmap != null && lastdownloadmap instanceof Map && blockedIPsMap.isEmpty()) {
                    blockedIPsMap.putAll((Map<String, Long>) lastdownloadmap);
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
            handleErrorsHTML(this.br);
            /**
             * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached!
             */
            if (PluginJsonConfig.get(UpstoReConfig.class).isActivateReconnectWorkaround()) {
                /*
                 * If the user starts a download in free (unregistered) mode the waittime is on his IP. This also affects free accounts if
                 * he tries to start more downloads via free accounts afterwards BUT nontheless the limit is only on his IP so he CAN
                 * download using the same free accounts after performing a reconnect!
                 */
                final long lastdownload = getPluginSavedLastDownloadTimestamp();
                final long passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload;
                if (passedTimeSinceLastDl < FREE_RECONNECTWAIT) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, FREE_RECONNECTWAIT - passedTimeSinceLastDl);
                }
            }
            final Form captchaForm = br.getFormBySubmitvalue("Get+download+link");
            final long timeBefore = System.currentTimeMillis();
            final String waittimeStr = br.getRegex("var sec = (\\d+)").getMatch(0);
            final int waitFull = Integer.parseInt(waittimeStr);
            int wait = waitFull;
            logger.info("Detected total waittime: " + wait);
            if (containsHCaptcha(this.br)) {
                final CaptchaHelperHostPluginHCaptcha hCaptcha = new CaptchaHelperHostPluginHCaptcha(this, br);
                final String captchaResponse = hCaptcha.getToken();
                final int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000);
                wait -= passedTime;
                /* Make sure we wait long enough */
                wait += 1;
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
            if (br.containsHTML("(?i)limit for today|several files recently")) {
                setDownloadStarted(link, 0);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 3 * 60 * 60 * 1000l);
            }
            dllink = br.getRegex("<div style=\"margin: 10px auto 20px\" class=\"center\">\\s*<a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://d\\d+\\.[^/]+/l/[^<>\"]*?)\"").getMatch(0);
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
            } else if (PluginJsonConfig.get(UpstoReConfig.class).isDowngradeToHTTP()) {
                dllink = dllink.replaceAll("https://", "http://");
            }
        }
        dllink = correctProtocolInFinalDownloadurl(dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            /*
             * The download attempt already triggers reconnect waittime! Save timestamp here to calculate correct remaining waittime later!
             */
            setDownloadStarted(link, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection(true);
                handleErrorsHTML(this.br);
                handleServerErrors(this.br);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        link.setProperty(directurlproperty, dl.getConnection().getURL().toExternalForm());
        try {
            /* Add a download slot */
            controlMaxFreeDownloads(null, link, +1);
            /* Start download */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxFreeDownloads(null, link, -1);
        }
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    /** Handles errors which are returned after accessing final downloadurl. */
    private void handleServerErrors(final Browser br) throws PluginException {
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

    private void handleErrorsHTML(final Browser br) throws PluginException {
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

    private void login(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    getPage("https://" + this.getHost());
                    if (!this.isLoggedinHTML(br)) {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                        setDefaultCookies(br);
                    } else {
                        logger.info("Cookie login successful");
                        /* Save new cookie timestamp */
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    }
                }
                logger.info("Full login required");
                if (!isMail(account.getUser())) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Bitte gib deine E-Mail Adresse in das 'Benutzername' Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail address in the 'username' field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
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
                    final String code = getCaptchaCode(cap, this.getDownloadLink());
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
                    // incorrect login-captcha, or form values changed
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else if (!this.isLoggedinHTML(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
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

    private long getPremiumTill(final Browser br) {
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
        getPage((br.getHttpConnection() == null ? "https://" + this.getHost() : "") + "/?lang=en");
        getPage((br.getHttpConnection() == null ? "https://" + this.getHost() : "") + "/stat/download/?lang=en");
        // Check for never-ending premium accounts
        if (br.containsHTML("(?i)eternal premium")) {
            account.setType(AccountType.LIFETIME);
        } else {
            account.setType(AccountType.PREMIUM);
            final long validUntil = getPremiumTill(br);
            if (validUntil == -1) {
                if (br.containsHTML("(?i)unlimited premium")) {
                    ai.setValidUntil(-1);
                    ai.setStatus("Unlimited Premium Account");
                } else {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new AccountUnavailableException("Kostenlose Accounts dieses Anbieters werden nicht unterstützt!", 5 * 60 * 1000l);
                    } else {
                        throw new AccountUnavailableException("Free Accounts of this host are not supported!", 5 * 60 * 1000l);
                    }
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
        return ai;
    }

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
        getPage(this.getInternalContentURL(link));
        if (br.containsHTML(premDlLimit)) {
            trafficLeft(account);
        }
        // Directdownload enabled?
        String dllink = br.getRedirectLocation();
        // No directdownload? Let's "click" on download
        if (dllink == null) {
            postPage("/load/premium/", "js=1&hash=" + this.getFID(link));
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
        dllink = Encoding.htmlDecode(dllink).replace("\\", "");
        dllink = correctProtocolInFinalDownloadurl(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            handleErrorsHTML(this.br);
            this.handleServerErrors(this.br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String correctProtocolInFinalDownloadurl(final String url) {
        /* 2024-01-16: Temp workaround, see https://board.jdownloader.org/showthread.php?t=95034 */
        return url.replaceFirst("(?i)^https://", "http://");
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
            getPluginConfig().setProperty(PROPERTY_last_blockedIPsMap, blockedIPsMap);
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

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final int maxFree;
        if (PluginJsonConfig.get(UpstoReConfig.class).isAllowMultipleFreeDownloads()) {
            maxFree = 2;
        } else {
            maxFree = 1;
        }
        return Math.min(maxFree, freeRunning.get() + 1);
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* No account, yes we can expect captcha */
            return true;
        } else if (acc.getType() == AccountType.FREE) {
            /* Free accounts also have captchas */
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Class<? extends UpstoReConfig> getConfigInterface() {
        return UpstoReConfig.class;
    }
}