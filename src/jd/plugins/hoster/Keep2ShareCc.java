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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keep2share.cc" }, urls = { "http://keep2sharedecrypted\\.cc/file/[a-z0-9]+" }, flags = { 2 })
public class Keep2ShareCc extends K2SApi {

    public Keep2ShareCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/premium.html");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/page/terms.html";
    }

    private final String DOWNLOADPOSSIBLE = ">To download this file with slow speed, use";
    public final String  MAINPAGE         = "http://k2s.cc";
    private final String DOMAINS_PLAIN    = "((keep2share|k2s|k2share|keep2s|keep2)\\.cc)";
    private final String DOMAINS_HTTP     = "(https?://(www\\.)?" + DOMAINS_PLAIN + ")";

    /* abstract K2SApi class setters */

    /**
     * sets domain the API will use!
     */
    @Override
    protected String getDomain() {
        return "keep2share.cc";
    }

    @Override
    public long getVersion() {
        return (Math.max(super.getVersion(), 0) * 100000) + getAPIRevision();
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr) {
        prepADB(prepBr);
        prepBr.setConnectTimeout(90 * 1000);
        return prepBr;
    }

    /**
     * easiest way to set variables, without the need for multiple declared references
     *
     * @param account
     */
    private void setConstants(final Account account) {
        if (account != null) {
            if (account.getBooleanProperty("free", false)) {
                // free account
                chunks = 1;
                resumes = true;
                isFree = true;
                directlinkproperty = "freelink2";
            } else {
                // premium account
                chunks = 0;
                resumes = true;
                isFree = false;
                directlinkproperty = "premlink";
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = true;
            isFree = true;
            directlinkproperty = "freelink1";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // link cleanup, but respect users protocol choosing.
        link.setUrlDownload(link.getDownloadURL().replaceFirst("^https?://", getProtocol()));
        link.setUrlDownload(link.getDownloadURL().replace("keep2sharedecrypted.cc/", "k2s.cc/"));
        link.setUrlDownload(link.getDownloadURL().replace("keep2share.cc/", "k2s.cc/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.containsHTML("<title>Keep2Share\\.cc - Error</title>")) {
            link.getLinkStatus().setStatusText("Cannot check status - unknown error state");
            return AvailableStatus.UNCHECKABLE;
        }
        final String filename = getFileName();
        final String filesize = getFileSize();

        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            /* Remove spaces to support such inputs: 1 000.0 MB */
            link.setDownloadSize(SizeFormatter.getSize(filesize.trim().replace(" ", "")));
        }
        if (br.containsHTML("Downloading blocked due to")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Downloading blocked: No JD bug, please contact the keep2share support", 10 * 60 * 1000l);
        }
        // you can set filename for offline links! handling should come here!
        if (br.containsHTML("Sorry, an error occurred while processing your request|File not found or deleted|>Sorry, this file is blocked or deleted\\.</h5>|class=\"empty\"|>Displaying 1")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (isPremiumOnly()) {
            link.getLinkStatus().setStatusText("Only downloadable for premium users");
        }
        return AvailableStatus.TRUE;
    }

    public String getFileName() {
        String filename = null;
        // This might not be needed anymore but keeping it doesn't hurt either
        if (br.containsHTML(DOWNLOADPOSSIBLE)) {
            filename = br.getRegex(">Downloading file:</span><br>[\t\n\r ]+<span class=\"c2\">.*?alt=\"\" style=\"\">([^<>\"]*?)</span>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("File: <span>([^<>\"]*?)</span>").getMatch(0);
            if (filename == null) {
                // offline/deleted
                filename = br.getRegex("File name:</b>(.*?)<br>").getMatch(0);
            }
        }
        return filename;
    }

    public String getFileSize() {
        String filesize = null;
        if (br.containsHTML(DOWNLOADPOSSIBLE)) {
            filesize = br.getRegex("File size ([^<>\"]*?)</div>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex(">Size: ([^<>\"]*?)</div>").getMatch(0);
            if (filesize == null) {
                // offline/deleted
                filesize = br.getRegex("<b>File size:</b>(.*?)<br>").getMatch(0);
            }
        }
        return filesize;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        checkShowFreeDialog(Browser.getHost(MAINPAGE));
        if (useAPI()) {
            super.handleDownload(downloadLink, null);
        } else {
            requestFileInformation(downloadLink);
            doFree(downloadLink, null);
        }
    }

    private void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        handleGeneralErrors(account);
        br.setFollowRedirects(false);
        if (isPremiumOnly()) {
            premiumDownloadRestriction("This file is only available to premium members");
        }
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            if (br.containsHTML(DOWNLOADPOSSIBLE)) {
                dllink = getDllink();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                if (br.containsHTML("Traffic limit exceed!<br>|Download count files exceed!<br>")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
                }
                final String uniqueID = br.getRegex("name=\"slow_id\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (uniqueID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                postPage(br.getURL(), "yt0=&slow_id=" + uniqueID);
                if (br.containsHTML("Free user can't download large files")) {
                    premiumDownloadRestriction("This file is only available to premium members");
                }
                Browser br2 = br.cloneBrowser();
                // domain not transferable!
                getPage(br2, getProtocol() + "static.k2s.cc/ext/evercookie/evercookie.swf");
                // can be here also, raztoki 20130521!
                dllink = getDllink();
                if (dllink == null) {
                    handleFreeErrors();
                    if (br.containsHTML("Free account does not allow to download more than one file at the same time")) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                    }
                    if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                        logger.info("Detected captcha method \"Re Captcha\" for this host");
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                        if (id == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        rc.setId(id);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode(cf, downloadLink);
                        postPage(br.getURL(), "CaptchaForm%5Bcode%5D=&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&free=1&freeDownloadRequest=1&yt0=&uniqueId=" + uniqueID);
                        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                    } else {
                        final String captchaLink = br.getRegex("\"(/file/captcha\\.html\\?[^\"]+)\"").getMatch(0);
                        if (captchaLink == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final String code = getCaptchaCode(captchaLink, downloadLink);
                        postPage(br.getURL(), "CaptchaForm%5Bcode%5D=" + code + "&free=1&freeDownloadRequest=1&uniqueId=" + uniqueID);
                        if (br.containsHTML(">The verification code is incorrect|/site/captcha.html")) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                    }
                    /** Skippable */
                    int wait = 30;
                    final String waittime = br.getRegex("<div id=\"download-wait-timer\">[\t\n\r ]+(\\d+)[\t\n\r ]+</div>").getMatch(0);
                    if (waittime != null) {
                        wait = Integer.parseInt(waittime);
                    }
                    sleep(wait * 1001l, downloadLink);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    postPage(br.getURL(), "free=1&uniqueId=" + uniqueID);
                    handleFreeErrors();
                    br.getHeaders().put("X-Requested-With", null);
                    dllink = getDllink();
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        logger.info("dllink = " + dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(br.toString());
            dllink = br.getRegex("\"url\":\"(https?:[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                handleGeneralServerErrors(account, downloadLink);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
            // Try again...
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
        }
        downloadLink.setProperty("directlink", dllink);
        // add download slot
        controlSlot(+1, account);
        try {
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }
    }

    private void handleFreeErrors() throws PluginException {
        if (br.containsHTML("\">Downloading is not possible<")) {
            int hours = 0, minutes = 0, seconds = 0;
            final Regex waitregex = br.getRegex("Please wait (\\d{2}):(\\d{2}):(\\d{2}) to download this file");
            final String hrs = waitregex.getMatch(0);
            if (hrs != null) {
                hours = Integer.parseInt(hrs);
            }
            final String mins = waitregex.getMatch(1);
            if (mins != null) {
                minutes = Integer.parseInt(mins);
            }
            final String secs = waitregex.getMatch(2);
            if (secs != null) {
                seconds = Integer.parseInt(secs);
            }
            final long totalwait = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000l) + (seconds * 1000l);
            if (totalwait > 0) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, totalwait + 10000l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);

        }
    }

    private boolean isPremiumOnly() {
        return br.containsHTML("File size to large!<") || br.containsHTML("Only <b>Premium</b> access<br>") || br.containsHTML("only for premium members");
    }

    private String getDllink() throws PluginException {
        String dllink = br.getRegex("('|\")(/file/url\\.html\\?file=[a-z0-9]+)\\1").getMatch(1);
        if (dllink != null) {
            dllink = new Regex(br.getURL(), DOMAINS_HTTP).getMatch(0) + dllink;
        }
        return dllink;
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, String> login(final Account account, final boolean force, AtomicBoolean validateCookie) throws Exception {
        synchronized (ACCLOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && (!force || (validateCookie != null && validateCookie.get() == true))) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        if (validateCookie != null) {
                            getPage(MAINPAGE + "/site/profile.html");
                            if (force == false || !br.getURL().contains("login.html")) {
                                return cookies;
                            }
                        } else {
                            return cookies;
                        }
                    }
                }
                if (validateCookie != null) {
                    validateCookie.set(false);
                }
                getPage(MAINPAGE + "/login.html");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                String postData = "LoginForm%5BrememberMe%5D=0&LoginForm%5BrememberMe%5D=1&LoginForm%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass());
                // Handle stupid login captcha
                final String captchaLink = br.getRegex("\"(/auth/captcha\\.html\\?v=[a-z0-9]+)\"").getMatch(0);
                if (captchaLink != null) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "keep2share.cc", "http://keep2share.cc", true);
                    final String code = getCaptchaCode("http://k2s.cc" + captchaLink, dummyLink);
                    postData += "&LoginForm%5BverifyCode%5D=" + Encoding.urlEncode(code);
                } else {
                    if (br.containsHTML("recaptcha/api/challenge") || br.containsHTML("Recaptcha.create")) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", "keep2share.cc", "http://keep2share.cc", true);
                        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        String challenge = br.getRegex("recaptcha/api/challenge\\?k=(.*?)\"").getMatch(0);
                        if (challenge == null) {
                            challenge = br.getRegex("Recaptcha.create\\('(.*?)'").getMatch(0);
                        }
                        rc.setId(challenge);
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, dummyLink);
                        postData = postData + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c);
                    }
                }
                postPage(MAINPAGE + "/login.html", postData);
                if (br.containsHTML("Incorrect username or password")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML("The verification code is incorrect.")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login Captcha ungültig!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid login captcha!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML(">We have a suspicion that your account was stolen, this is why we")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account temporär gesperrt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account temporarily blocked!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML(">Please fill in the form with your login credentials")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (br.containsHTML(">Password cannot be blank.<")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Passwortfeld darf nicht leer sein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Password field cannot be empty!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getHeaders().put("X-Requested-With", null);
                String url = br.getRegex("url\":\"(.*?)\"").getMatch(0);
                if (url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                return cookies;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || !account.getUser().contains("@")) {
            account.setValid(false);
            ai.setStatus("Please use E-Mail as login/name!\r\nBitte E-Mail Adresse als Benutzername benutzen!");
            return ai;
        }
        if (useAPI()) {
            ai = super.fetchAccountInfo(account);
        } else {
            AtomicBoolean validateCookie = new AtomicBoolean(true);
            try {
                login(account, true, validateCookie);
            } catch (final PluginException e) {
                account.setValid(false);
                throw e;
            }
            if (validateCookie.get() == false) {
                getPage(MAINPAGE + "/site/profile.html");
            }
            account.setValid(true);
            if (br.containsHTML("class=\"free\">Free</a>")) {
                account.setProperty("free", true);
                ai.setStatus("Registered Free User");
            } else {
                account.setProperty("free", false);
                String availableTraffic = br.getRegex("Available traffic(.*?\\(today\\))?:.*?<a href=\"/user/statistic\\.html\">(.*?)</a>").getMatch(1);
                if (availableTraffic != null) {
                    ai.setTrafficLeft(SizeFormatter.getSize(availableTraffic));
                } else {
                    ai.setUnlimitedTraffic();
                }
                String expire = br.getRegex("class=\"premium\">Premium:[\t\n\r ]+(\\d{4}\\.\\d{2}\\.\\d{2})").getMatch(0);
                if (expire == null) {
                    expire = br.getRegex("Premium expires:\\s*?<b>(\\d{4}\\.\\d{2}\\.\\d{2})").getMatch(0);
                }
                if (expire == null && br.containsHTML(">Premium:[\t\n\r ]+LifeTime")) {
                    ai.setStatus("Premium Lifetime User");
                    ai.setValidUntil(-1);
                } else if (expire == null) {
                    ai.setStatus("Premium User");
                    ai.setValidUntil(-1);
                } else {
                    ai.setStatus("Premium User");
                    // Expired but actually we still got one day ('today')
                    if (br.containsHTML("\\(1 day\\)")) {
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH) + 24 * 60 * 60 * 1000l);
                    } else {
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH));
                    }
                }
            }
        }
        setAccountLimits(account);
        account.setValid(true);
        return ai;
    }

    @Override
    protected void setAccountLimits(Account account) {
        if (account != null && account.getBooleanProperty("free", false)) {
            maxPrem.set(1);
        } else if (account != null && !account.getBooleanProperty("free", false)) {
            maxPrem.set(20);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account);
        if (account.getBooleanProperty("free", false)) {
            checkShowFreeDialog(Browser.getHost(MAINPAGE));
        }
        if (useAPI()) {
            super.handleDownload(link, account);
        } else {
            requestFileInformation(link);
            boolean fresh = false;
            Object after = null;
            synchronized (ACCLOCK) {
                Object before = account.getProperty("cookies", null);
                after = login(account, false, null);
                fresh = before != after;
            }
            getPage(MAINPAGE + "/site/profile.html");
            if (br.getURL().contains("login.html")) {
                logger.info("Redirected to login page, seems cookies are no longer valid!");
                synchronized (ACCLOCK) {
                    if (after == account.getProperty("cookies", null)) {
                        account.setProperty("cookies", Property.NULL);
                    }
                    if (fresh) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            }
            if (account.getBooleanProperty("free", false)) {
                setConstants(account);
                getPage(link.getDownloadURL());
                doFree(link, account);
            } else {
                br.setFollowRedirects(false);
                getPage(link.getDownloadURL());
                handleGeneralErrors(account);
                // Set cookies for other domain if it is changed via redirect
                String currentDomain = MAINPAGE.replace("http://", "");
                String newDomain = null;
                String dllink = br.getRedirectLocation();
                if (dllink == null) {
                    dllink = getDllinkPremium();
                }
                String possibleDomain = getDomain(dllink);
                if (dllink != null && possibleDomain != null && !possibleDomain.contains(currentDomain)) {
                    newDomain = getDomain(dllink);
                } else if (!br.getURL().contains(currentDomain)) {
                    newDomain = getDomain(br.getURL());
                }
                if (newDomain != null) {
                    resetCookies(account, currentDomain, newDomain);
                    if (dllink == null) {
                        getPage(link.getDownloadURL().replace(currentDomain, newDomain));
                        dllink = br.getRedirectLocation();
                        if (dllink == null) {
                            dllink = getDllinkPremium();
                        }
                    }
                    currentDomain = newDomain;
                }

                if (dllink == null) {
                    if (br.containsHTML("Traffic limit exceed!<")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    synchronized (ACCLOCK) {
                        if (after == account.getProperty("cookies", null)) {
                            account.setProperty("cookies", Property.NULL);
                        }
                        if (fresh) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        }
                    }
                }
                dllink = Encoding.htmlDecode(dllink);
                logger.info("dllink = " + dllink);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumes, chunks);
                if (dl.getConnection().getContentType().contains("html")) {
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    handleGeneralServerErrors(account, link);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // add download slot
                controlSlot(+1, account);
                try {
                    dl.startDownload();
                } finally {
                    // remove download slot
                    controlSlot(-1, account);
                }
            }
        }
    }

    private void handleGeneralErrors(final Account account) throws PluginException {
        if (br.containsHTML("<title>Keep2Share\\.cc - Error</title>")) {
            if (br.containsHTML("<li>Sorry, our store is not available, please try later")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Store is temporarily unavailable'", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
        }
    }

    private String getDllinkPremium() {
        return br.getRegex("(\\'|\")(/file/url\\.html\\?file=[a-z0-9]+)\\1").getMatch(1);
    }

    private String getDomain(final String link) {
        if (link == null) {
            return null;
        }
        return new Regex(link, "https?://(www\\.)?([A-Za-z0-9\\-\\.]+)/").getMatch(1);
    }

    @SuppressWarnings("unchecked")
    private boolean resetCookies(final Account account, String oldDomain, String newDomain) {
        oldDomain = "http://" + oldDomain;
        newDomain = "http://" + newDomain;
        br.clearCookies(oldDomain);
        final Object ret = account.getProperty("cookies", null);
        final HashMap<String, String> cookies = (HashMap<String, String>) ret;
        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
            final String key = cookieEntry.getKey();
            final String value = cookieEntry.getValue();
            this.br.setCookie(newDomain, key, value);
        }
        return true;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
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

    /**
     * because stable is lame!
     * */
    public void setBrowser(final Browser ibr) {
        this.br = ibr;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), USE_API, JDL.L("plugins.hoster.Keep2ShareCc.useAPI", "Use API (recommended!)")).setDefaultValue(default_USE_API));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, JDL.L("plugins.hoster.Keep2ShareCc.preferSSL", "Use Secure Communication over SSL (HTTPS://)")).setDefaultValue(default_SSL_CONNECTION));
    }

}