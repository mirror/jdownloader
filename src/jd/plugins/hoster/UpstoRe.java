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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.CaptchaException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upstore.net", "upsto.re" }, urls = { "http://(www\\.)?(upsto\\.re|upstore\\.net)/[A-Za-z0-9]+", "ejnz905rj5o0jt69pgj50ujz0zhDELETE_MEew7th59vcgzh59prnrjhzj0" }, flags = { 2, 0 })
public class UpstoRe extends PluginForHost {

    public UpstoRe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://upstore.net/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://upstore.net/terms/";
    }

    public boolean isPremiumEnabled() {
        return "upstore.net".equals(getHost());
    }

    @Override
    public Boolean rewriteHost(Account acc) {
        if ("upstore.net".equals(getHost())) {
            if (acc != null && "upsto.re".equals(acc.getHoster())) {
                acc.setHoster("upstore.net");
                return true;
            }
            return false;
        }
        return null;
    }

    private static Object LOCK         = new Object();
    private final String  MAINPAGE     = "http://upstore.net";
    private final String  INVALIDLINKS = "http://(www\\.)?(upsto\\.re|upstore\\.net)/(faq|privacy|terms|d/|aff|login|account|dmca|imprint|message|panel|premium|contacts)";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("upsto.re/", "upstore.net/"));
    }

    private final boolean                        useRUA    = true;
    private static final AtomicReference<String> userAgent = new AtomicReference<String>(null);

    /**
     * defines custom browser requirements
     *
     * @author raztoki
     * */
    private Browser prepBrowser(final Browser prepBr) {
        if (useRUA) {
            if (userAgent.get() == null) {
                /* we first have to load the plugin, before we can reference it */
                JDUtilities.getPluginForHost("mediafire.com");
                userAgent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
            }
            prepBr.getHeaders().put("User-Agent", userAgent.get());
        }
        prepBr.setCookie("http://upstore.net/", "lang", "en");

        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        if (link.getDownloadURL().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found<|>File was deleted by owner or due to a violation of service rules\\.|not found|>SmartErrors powered by")) {
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

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "freelink");
        if (dllink == null) {
            final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            br.postPage(downloadLink.getDownloadURL(), "free=Slow+download&hash=" + fid);
            if (br.containsHTML(">This file is available only for Premium users<")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) {
                        throw (PluginException) e;
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
            }
            if (br.containsHTML(">Server for free downloads is overloaded<")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server overloaded", 30 * 60 * 1000l);
            }
            // Same server error (displayed differently) also exists for premium users
            if (br.containsHTML(">Server with file not found<")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            }
            // Waittime can be skipped
            final long timeBefore = System.currentTimeMillis();
            final String rcID = br.getRegex("Recaptcha\\.create\\(\\'([^<>\"]*?)\\'").getMatch(0);
            if (rcID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = null;
            try {
                c = getCaptchaCode(cf, downloadLink);
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
            br.postPage(downloadLink.getDownloadURL(), "free=Get+download+link&hash=" + fid + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
            if (br.containsHTML("limit for today|several files recently")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 3 * 60 * 60 * 1000l);
            }
            dllink = br.getRegex("<div style=\"margin: 10px auto 20px\" class=\"center\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(http://d\\d+\\.upstore\\.net/[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                final String reconnectWait = br.getRegex("Please wait (\\d+) minutes before downloading next file").getMatch(0);
                if (reconnectWait != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 60 * 1001l);
                }
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            sleep(3000l, downloadLink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("not found")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                prepBrowser(br);
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        account.getProperty("ua", userAgent.get());
                        return;
                    }
                }
                if (!isMail(account.getUser())) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your mailadress in the 'username' field!\r\nBitte gib deine E-Mail Adresse in das 'Benutzername' Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.postPage("http://upstore.net/account/login/", "url=http%253A%252F%252Fupstore.net%252F&send=Login&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
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
                    br.postPage(br.getURL(), "url=http%253A%252F%252Fupstore.net%252F&send=sign+in&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&captcha=" + Encoding.urlEncode(code));
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
    private final long   useLoginIndividual = 6 * 3480000l;
    private final String regexLoginCaptcha  = "/captcha/\\?\\d+";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        try {
            areWeStillLoggedIn(account);
        } catch (PluginException e) {
            throw e;
        }
        // Make sure that the language is correct
        br.getPage((br.getHttpConnection() == null ? MAINPAGE : "") + "/?lang=en");
        ai.setUnlimitedTraffic();
        // Check for never-ending premium accounts
        if (!br.containsHTML(lifetimeAccount)) {
            final String expire = br.getRegex(premiumTilAccount).getMatch(0);
            if (expire == null) {
                if (br.containsHTML("unlimited premium")) {
                    ai.setValidUntil(-1);
                    ai.setStatus("Unlimited Premium User");
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nFree Accounts are not supported for this host!\r\nKostenlose Accounts dieses Hosters werden nicht unterstützt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yy", null));
            }
        }
        ai.setStatus("Premium User");
        account.setValid(true);

        return ai;
    }

    // lifetime account
    private String lifetimeAccount   = "eternal premium";
    // expiring type account
    private String premiumTilAccount = "premium till (\\d{2}/\\d{2}/\\d{2})";

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
                br.getPage(MAINPAGE);
                // upstore doesn't remove invalid cookies, so we need to also check against account types!
                if (browserCookiesMatchLoginCookies(br) && br.containsHTML(this.lifetimeAccount + "|" + this.premiumTilAccount)) {
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
     * */
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
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(premDlLimit)) {
            trafficLeft(account);
        }
        // Directdownload enabled?
        String dllink = br.getRedirectLocation();
        // No directdownload? Let's "click" on download
        if (dllink == null) {
            br.postPage("http://upstore.net/load/premium/", "js=1&hash=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
            if (br.containsHTML(premDlLimit)) {
                trafficLeft(account);
            }
            dllink = br.getRegex("\"ok\":\"(http:[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink).replace("\\", ""), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            // Same server error (displayed differently) also exists for free
            // users
            if (br.containsHTML("not found")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            }
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
                Browser br2 = br.cloneBrowser();
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