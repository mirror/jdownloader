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
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keep2share.cc" }, urls = { "http://keep2sharedecrypted\\.cc/file/[a-z0-9]+" }, flags = { 2 })
public class Keep2ShareCc extends PluginForHost {

    public Keep2ShareCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://k2s.cc/premium.html");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://k2s.cc/page/terms.html";
    }

    private final String                   DOWNLOADPOSSIBLE             = ">To download this file with slow speed, use";
    private final String                   MAINPAGE                     = "http://k2s.cc";
    private final String                   DOMAINS_PLAIN                = "((keep2share|k2s|k2share|keep2s|keep2)\\.cc)";
    private final String                   DOMAINS_HTTP                 = "(https?://(www\\.)?" + DOMAINS_PLAIN + ")";

    private static final String            USE_API                      = "USE_API";
    private final static String            SSL_CONNECTION               = "SSL_CONNECTION";

    private static Object                  LOCK                         = new Object();

    private static AtomicReference<String> agent                        = new AtomicReference<String>(null);
    private boolean                        prepBrSet                    = false;

    /* Connection stuff */
    private static final boolean           FREE_RESUME                  = true;
    private static final int               FREE_MAXCHUNKS               = 1;
    private static final int               FREE_MAXDOWNLOADS            = 1;
    private static final boolean           ACCOUNT_FREE_RESUME          = true;
    private static final int               ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int               ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("keep2sharedecrypted.cc/", "k2s.cc/"));
        link.setUrlDownload(link.getDownloadURL().replace("keep2share.cc/", "k2s.cc/"));
    }

    private Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        // required for native cloudflare support, without the need to repeat requests.
        try {
            /* not available in old stable */
            prepBr.setAllowedResponseCodes(new int[] { 503, 522 });
        } catch (Throwable e) {
        }
        synchronized (antiDDoSCookies) {
            if (!antiDDoSCookies.isEmpty()) {
                for (final Map.Entry<String, String> cookieEntry : antiDDoSCookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    prepBr.setCookie(this.getHost(), key, value);
                }
            }
        }
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Pragma", null);
        prepBr.setConnectTimeout(90 * 1000);
        prepBrSet = true;
        return prepBr;
    }

    private void checkShowFreeDialog() {
        SubConfiguration config = null;
        try {
            config = getPluginConfig();
            if (config.getBooleanProperty("premAdShown", Boolean.FALSE) == false) {
                if (config.getProperty("premAdShown2") == null) {
                    File checkFile = JDUtilities.getResourceFile("tmp/k2c");
                    if (!checkFile.exists()) {
                        checkFile.mkdirs();
                        showFreeDialog();
                    }
                } else {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (final Throwable e) {
        } finally {
            if (config != null) {
                config.setProperty("premAdShown", Boolean.TRUE);
                config.setProperty("premAdShown2", "shown");
                config.save();
            }
        }
    }

    private void showFreeDialog() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String domain = "keep2share.cc";
                        String message = null;
                        String title = null;
                        String tab = "                        ";
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " Free Download";
                            message = "Du lädst im kostenlosen Modus von " + domain + ".\r\n";
                            message += "Wie bei allen anderen Hostern holt JDownloader auch hier das Beste für dich heraus!\r\n\r\n";
                            message += tab + "  Falls du allerdings mehrere Dateien\r\n" + "          - und das möglichst mit Fullspeed und ohne Unterbrechungen - \r\n" + "             laden willst, solltest du dir den Premium Modus anschauen.\r\n\r\nUnserer Erfahrung nach lohnt sich das - Aber entscheide am besten selbst. Jetzt ausprobieren?  ";
                        } else {
                            title = domain + " Free Download";
                            message = "You are using the " + domain + " Free Mode.\r\n";
                            message += "JDownloader always tries to get the best out of each hoster's free mode!\r\n\r\n";
                            message += tab + "   However, if you want to download multiple files\r\n" + tab + "- possibly at fullspeed and without any wait times - \r\n" + tab + "you really should have a look at the Premium Mode.\r\n\r\nIn our experience, Premium is well worth the money. Decide for yourself, though. Let's give it a try?   ";
                        }
                        if (CrossSystem.isOpenBrowserSupported()) {
                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
                            if (JOptionPane.OK_OPTION == result) {
                                CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                            }
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
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
        requestFileInformation(downloadLink);
        handle_Free(downloadLink, null);
    }

    private void handle_Free(final DownloadLink downloadLink, final Account account) throws Exception {
        checkShowFreeDialog();
        String dllink = checkDirectLink(downloadLink, "k2sfreedirectlink");
        if (dllink == null) {
            if (this.apiEnabled()) {
                dllink = api_HandleFree(downloadLink, account);
            } else {
                dllink = site_doFree(downloadLink, account);
            }
        }
        download_free(downloadLink, account, dllink);
    }

    private void download_free(final DownloadLink downloadLink, final Account account, String dllink) throws Exception {
        dllink = fixLinkSSL(dllink);
        logger.info("dllink = " + dllink);
        boolean resume = FREE_RESUME;
        int maxchunks = FREE_MAXDOWNLOADS;
        if (account != null) {
            resume = ACCOUNT_FREE_RESUME;
            maxchunks = ACCOUNT_FREE_MAXCHUNKS;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(br.toString());
            dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                handleGeneralServerErrors(account);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
            // Try again...
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        }
        downloadLink.setProperty("k2sfreedirectlink", dllink);
        dl.startDownload();
    }

    private String site_doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        handleGeneralErrors(account);
        br.setFollowRedirects(false);
        if (br.containsHTML("File size to large\\!<") || br.containsHTML("Only <b>Premium</b> access<br>") || br.containsHTML("only for premium members")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to premium members");
        }
        String dllink;
        if (br.containsHTML(DOWNLOADPOSSIBLE)) {
            dllink = getDllink();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            if (br.containsHTML("Traffic limit exceed\\!<br>|Download count files exceed!<br>")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
            }
            final String uniqueID = br.getRegex("name=\"slow_id\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (uniqueID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            postPage(br.getURL(), "yt0=&slow_id=" + uniqueID);
            if (br.containsHTML("Free user can't download large files")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to premium members");
            }
            Browser br2 = br.cloneBrowser();
            // domain not transferable!
            getPage2(br2, "http://static.k2s.cc/ext/evercookie/evercookie.swf");
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
                    final String code = getCaptchaCode(new Regex(this.fixLinkSSL(br.getURL()), DOMAINS_HTTP).getMatch(0) + captchaLink, downloadLink);
                    postPage(br.getURL(), "CaptchaForm%5Bcode%5D=" + code + "&free=1&freeDownloadRequest=1&uniqueId=" + uniqueID);
                    if (br.containsHTML(">The verification code is incorrect|/site/captcha.html")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                }
                /** Skippable */
                int wait = 30;
                final String waittime = br.getRegex("<div id=\"download\\-wait\\-timer\">[\t\n\r ]+(\\d+)[\t\n\r ]+</div>").getMatch(0);
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
        return dllink;
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

    private String getDllink() throws PluginException {
        String dllink = br.getRegex("('|\")(/file/url\\.html\\?file=[a-z0-9]+)\\1").getMatch(1);
        if (dllink != null) {
            dllink = new Regex(br.getURL(), DOMAINS_HTTP).getMatch(0) + dllink;
        }
        return dllink;
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+|null|true|false)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || con.getResponseCode() == 401) {
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

    @SuppressWarnings("unchecked")
    private HashMap<String, String> site_login(final Account account, final boolean force, AtomicBoolean validateCookie) throws Exception {
        synchronized (LOCK) {
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
                            getPage2(this.br, MAINPAGE + "/site/profile.html");
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
            ai.setStatus("Please use E-Mail as login/name!");
            return ai;
        }
        if (this.apiEnabled()) {
            ai = api_fetchAccountInfo(account);
        } else {
            ai = site_fetchAccountInfo(account);
        }
        account.setValid(true);
        return ai;
    }

    private AccountInfo site_fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
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
        return ai;
    }

    private String api_getAuthToken(final Account acc) {
        return acc.getStringProperty("authtoken");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (this.apiEnabled()) {
            api_handleAccountDownload(link, account);
        } else {
            site_handleAccountDownload(link, account);
        }
    }

    private void site_handleAccountDownload(final DownloadLink link, final Account account) throws Exception {
        boolean fresh = false;
        Object after = null;
        synchronized (LOCK) {
            Object before = account.getProperty("cookies", null);
            after = site_login(account, false, null);
            fresh = before != after;
        }
        getPage(MAINPAGE + "/site/profile.html");
        if (br.getURL().contains("login.html")) {
            logger.info("Redirected to login page, seems cookies are no longer valid!");
            synchronized (LOCK) {
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
        if (br.containsHTML("class=\"free\">Free</a>")) {
            getPage(link.getDownloadURL());
            handle_Free(link, account);
        } else {
            br.setFollowRedirects(false);
            getPage2(this.br, link.getDownloadURL());
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
                    getPage2(this.br, link.getDownloadURL().replace(currentDomain, newDomain));
                    dllink = br.getRedirectLocation();
                    if (dllink == null) {
                        dllink = getDllinkPremium();
                    }
                }
                currentDomain = newDomain;
            }

            if (dllink == null) {
                if (br.containsHTML("Traffic limit exceed\\!<")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                synchronized (LOCK) {
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
            if (!dllink.startsWith("http")) {
                dllink = "http://" + currentDomain + dllink;
            }
            download_premium(link, account, dllink);
        }
    }

    private void download_premium(final DownloadLink link, final Account account, final String dllink) throws Exception {
        logger.info("dllink = " + dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleGeneralServerErrors(account);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleGeneralErrors(final Account account) throws PluginException {
        if (br.containsHTML("<title>Keep2Share\\.cc - Error</title>")) {
            if (br.containsHTML("<li>Sorry, our store is not available, please try later")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Store is temporarily unavailable'", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
        }
    }

    private void handleGeneralServerErrors(final Account account) throws PluginException {
        final String alreadyDownloading = "Your current tariff doesn't allow to download more files then you are downloading now\\.";
        if ((account == null || account.getBooleanProperty("free", false)) && br.containsHTML(alreadyDownloading)) {
            // found from jdlog://4140408642041 also note: ISP seems to have transparent proxy!
            // should only happen to free.
            // We also only have 1 max free sim currently, if we go higher we need to track current transfers against
            // connection_candidate(proxy|direct) IP address, and reduce max sim by one.
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, alreadyDownloading, 10 * 60 * 1000);
        } else if (dl.getConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404 || br.containsHTML(">Not Found<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        }
    }

    private String getDllinkPremium() {
        return br.getRegex("(\\'|\")(/file/url\\.html\\?file=[a-z0-9]+)(\\'|\")").getMatch(1);
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

    private String api_HandleFree(final DownloadLink downloadLink, final Account account) throws Exception {
        final String fid = getFID(downloadLink);
        postPageRaw(this.br, "http://keep2share.cc/api/v1/requestcaptcha", "");
        final String challenge = getJson(br.toString(), "challenge");
        final String captcha_url = getJson(br.toString(), "captcha_url").replace("\\", "");
        final String code = getCaptchaCode(captcha_url, downloadLink);
        postPageRaw(this.br, "http://keep2share.cc/api/v1/geturl", "{\"file_id\":\"" + fid + "\",\"free_download_key\":null,\"captcha_challenge\":\"" + challenge + "\",\"captcha_response\":\"" + code + "\"}");
        if (!"success".equals(getJson(br.toString(), "status"))) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final String free_download_key = getJson(br.toString(), "free_download_key");
        final int wait = Integer.parseInt(getJson(br.toString(), "time_wait"));
        this.sleep(wait * 1001l, downloadLink);
        postPageRaw(this.br, "http://keep2share.cc/api/v1/geturl", "{\"file_id\":\"" + fid + "\",\"free_download_key\":\"" + free_download_key + "\",\"captcha_challenge\":null,\"captcha_response\":null}");
        String dllink = getJson(br.toString(), "url");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        return dllink;
    }

    /* TODO: Implement */
    private AccountInfo api_fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        // postPageRaw(this.br, "http://keep2share.cc/api/v1/GetFilesInfo", "{\"ids\"=[\"02cb9384ecf26\"]}");
        api_login(account, true);
        postPageRaw(this.br, "http://keep2share.cc/api/v1/accountinfo", "{\"auth_token\":\"" + api_getAuthToken(account) + "\"}");
        final String available_traffic = getJson(br.toString(), "available_traffic");
        final String account_expires = getJson(br.toString(), "account_expires");
        if (account_expires.equals("false")) {
            account.setProperty("free", true);
            ai.setStatus("Free User");
        } else {
            account.setProperty("free", false);
            ai.setValidUntil(Long.parseLong(account_expires) * 1000l);
            ai.setStatus("Premium User");
        }
        ai.setTrafficLeft(Long.parseLong(available_traffic));
        return ai;
    }

    private void api_login(final Account acc, final boolean force) throws IOException, PluginException {
        String authtoken = api_getAuthToken(acc);
        if (authtoken == null || force) {
            boolean failed = false;
            try {
                postPageRaw(this.br, "http://keep2share.cc/api/v1/login", "{\"username\":\"" + acc.getUser() + "\",\"password\":\"" + acc.getPass() + "\"}");
            } catch (final BrowserException e) {
                if (br.getHttpConnection().getResponseCode() == 406) {
                    failed = true;
                } else {
                    throw e;
                }
            }
            authtoken = getJson(br.toString(), "auth_token");
            if (authtoken == null || failed) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            acc.setProperty("authtoken", authtoken);
        }
    }

    private void api_handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.api_login(account, false);
        postPageRaw(this.br, "http://keep2share.cc/api/v1/geturl", "{\"auth_token\":\"" + api_getAuthToken(account) + "\",\"file_id\":\"" + getFID(link) + "\",\"free_download_key\":null,\"captcha_challenge\":null,\"captcha_response\":null}");
        String dllink = getJson(br.toString(), "url");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        download_premium(link, account, dllink);
    }

    private void api_handleAccountDownload(final DownloadLink link, final Account account) throws Exception {
        if (account.getBooleanProperty("free", false)) {
            api_HandleFree(link, account);
        } else {
            api_handlePremium(link, account);
        }
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
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
        return 1;
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

    /** Gets page where "getPage()" fown below is not used - is there to make sure the SSL setting is used correctly for all browser calls */
    public void getPage2(final Browser br, String page) throws IOException {
        page = fixLinkSSL(page);
        br.getPage(page);
    }

    /**
     * Gets page <br />
     * - natively supports silly cloudflare anti DDoS crapola
     *
     * @author raztoki
     */
    public void getPage(String page) throws Exception {
        if (page == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        page = fixLinkSSL(page);
        if (!prepBrSet) {
            prepBrowser(br);
        }
        final boolean follows_redirects = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            try {
                br.getPage(page);
            } catch (Exception e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
                // should only be picked up now if not JD2
                if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 503 && br.getHttpConnection().getHeaderField("server") != null && br.getHttpConnection().getHeaderField("server").toLowerCase(Locale.ENGLISH).contains("cloudflare-nginx")) {
                    logger.warning("Cloudflare anti DDoS measures enabled, your version of JD can not support this. In order to go any further you will need to upgrade to JDownloader 2");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Cloudflare anti DDoS measures enabled");
                } else {
                    throw e;
                }
            }
            antiDDoS();
        } finally {
            br.setFollowRedirects(follows_redirects);
        }
    }

    public void postPage(String page, final String postData) throws Exception {
        if (page == null || postData == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        page = fixLinkSSL(page);
        if (!prepBrSet) {
            prepBrowser(br);
        }
        // stable sucks
        if (isJava7nJDStable() && page.startsWith("https")) {
            page = page.replaceFirst("https://", "http://");
        }
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        try {
            br.postPage(page, postData);
            antiDDoS();
        } finally {
            br.getHeaders().put("Content-Type", null);
        }
    }

    public static void postPageRaw(final Browser br, String page, final String postData) throws IOException {
        page = fixLinkSSL(page);
        // stable sucks
        if (isJava7nJDStable() && page.startsWith("https")) {
            page = page.replaceFirst("https://", "http://");
        }
        br.postPageRaw(page, postData);
    }

    public void sendForm(final Form form) throws Exception {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!prepBrSet) {
            prepBrowser(br);
        }
        // stable sucks && lame to the max, lets try and send a form outside of desired protocol. (works with oteupload)
        if (Form.MethodType.POST.equals(form.getMethod())) {
            // if the form doesn't contain an action lets set one based on current br.getURL().
            if (form.getAction() == null || form.getAction().equals("")) {
                form.setAction(br.getURL());
            }
            if (isJava7nJDStable() && (form.getAction().contains("https://") || /* relative path */(!form.getAction().startsWith("http")))) {
                if (!form.getAction().startsWith("http") && br.getURL().contains("https://")) {
                    // change relative path into full path, with protocol correction
                    String basepath = new Regex(br.getURL(), "(https?://.+)/[^/]+$").getMatch(0);
                    String basedomain = new Regex(br.getURL(), "(https?://[^/]+)").getMatch(0);
                    String path = form.getAction();
                    String finalpath = null;
                    if (path.startsWith("/")) {
                        finalpath = basedomain.replaceFirst("https://", "http://") + path;
                    } else if (!path.startsWith(".")) {
                        finalpath = basepath.replaceFirst("https://", "http://") + path;
                    } else {
                        // lacking builder for ../relative paths. this will do for now.
                        logger.info("Missing relative path builder. Must abort now... Try upgrading to JDownloader 2");
                        throw new PluginException(LinkStatus.ERROR_FATAL);
                    }
                    form.setAction(finalpath);
                } else {
                    form.setAction(form.getAction().replaceFirst("https?://", "http://"));
                }
                if (!stableSucks.get()) {
                    showSSLWarning(this.getHost());
                }
            }
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        }
        try {
            br.submitForm(form);
            antiDDoS();
        } finally {
            br.getHeaders().put("Content-Type", null);
        }
    }

    private static HashMap<String, String> antiDDoSCookies = new HashMap<String, String>();

    /**
     * Performs Cloudflare and Incapsula requirements.<br />
     * Auto fill out the required fields and updates antiDDoSCookies session.<br />
     * Always called after Browser Request!
     *
     * @version 0.02
     * @author raztoki
     **/
    private void antiDDoS() throws Exception {
        if (br == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final HashMap<String, String> cookies = new HashMap<String, String>();
        if (br.getHttpConnection() != null) {
            final String URL = br.getURL();
            if (requestHeadersHasKeyNValueContains("server", "cloudflare-nginx")) {
                Form cloudflare = br.getFormbyProperty("id", "ChallengeForm");
                if (cloudflare == null) {
                    cloudflare = br.getFormbyProperty("id", "challenge-form");
                }
                if (br.getHttpConnection().getResponseCode() == 403 && cloudflare != null) {
                    // new method seems to be within 403
                    if (cloudflare.hasInputFieldByName("recaptcha_response_field")) {
                        // they seem to add multiple input fields which is most likely meant to be corrected by js ?
                        // we will manually remove all those
                        while (cloudflare.hasInputFieldByName("recaptcha_response_field")) {
                            cloudflare.remove("recaptcha_response_field");
                        }
                        while (cloudflare.hasInputFieldByName("recaptcha_challenge_field")) {
                            cloudflare.remove("recaptcha_challenge_field");
                        }
                        // this one is null, needs to be ""
                        if (cloudflare.hasInputFieldByName("message")) {
                            cloudflare.remove("message");
                            cloudflare.put("messsage", "\"\"");
                        }
                        // recaptcha bullshit
                        String apiKey = cloudflare.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                        if (apiKey == null) {
                            apiKey = br.getRegex("/recaptcha/api/(?:challenge|noscript)\\?k=([A-Za-z0-9%_\\+\\- ]+)").getMatch(0);
                            if (apiKey == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        final DownloadLink dllink = new DownloadLink(null, "antiDDoS Provider 'Clouldflare' requires Captcha", MAINPAGE, MAINPAGE, true);
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.setId(apiKey);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String response = getCaptchaCode(cf, dllink);
                        cloudflare.put("recaptcha_challenge_field", rc.getChallenge());
                        cloudflare.put("recaptcha_response_field", Encoding.urlEncode(response));
                        br.submitForm(cloudflare);
                        if (br.getFormbyProperty("id", "ChallengeForm") != null || br.getFormbyProperty("id", "challenge-form") != null) {
                            logger.warning("Possible plugin error within cloudflare handling");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                } else if (br.getHttpConnection().getResponseCode() == 503 && cloudflare != null) {
                    // 503 response code with javascript math section
                    String host = new Regex(URL, "https?://([^/]+)(:\\d+)?/").getMatch(0);
                    String math = br.getRegex("\\$\\('#jschl_answer'\\)\\.val\\(([^\\)]+)\\);").getMatch(0);
                    if (math == null) {
                        math = br.getRegex("a\\.value = ([\\d\\-\\.\\+\\*/]+);").getMatch(0);
                    }
                    if (math == null) {
                        String variableName = br.getRegex("(\\w+)\\s*=\\s*\\$\\('#jschl_answer'\\);").getMatch(0);
                        if (variableName != null) {
                            variableName = variableName.trim();
                        }
                        math = br.getRegex(variableName + "\\.val\\(([^\\)]+)\\)").getMatch(0);
                    }
                    if (math == null) {
                        logger.warning("Couldn't find 'math'");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    // use js for now, but change to Javaluator as the provided string doesn't get evaluated by JS according to Javaluator
                    // author.
                    ScriptEngineManager mgr = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
                    ScriptEngine engine = mgr.getEngineByName("JavaScript");
                    final long value = ((Number) engine.eval("(" + math + ") + " + host.length())).longValue();
                    cloudflare.put("jschl_answer", value + "");
                    Thread.sleep(5500);
                    br.submitForm(cloudflare);
                    if (br.getFormbyProperty("id", "ChallengeForm") != null || br.getFormbyProperty("id", "challenge-form") != null) {
                        logger.warning("Possible plugin error within cloudflare handling");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else {
                    // nothing wrong, or something wrong (unsupported format)....
                    // commenting out return prevents caching of cookies per request
                    // return;
                }
                // get cookies we want/need.
                // refresh these with every getPage/postPage/submitForm?
                final Cookies add = br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    if (new Regex(c.getKey(), "(cfduid|cf_clearance)").matches()) {
                        cookies.put(c.getKey(), c.getValue());
                    }
                }
            }
            // save the session!
            synchronized (antiDDoSCookies) {
                antiDDoSCookies.clear();
                antiDDoSCookies.putAll(cookies);
            }
        }
    }

    /**
     *
     * @author raztoki
     * */
    private boolean requestHeadersHasKeyNValueStartsWith(final String k, final String v) {
        if (k == null || v == null) {
            return false;
        }
        for (HTTPHeader s : br.getRequest().getHeaders()) {
            if (s.getKey().startsWith(k) && s.getValue().startsWith(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @author raztoki
     * */
    private boolean requestHeadersHasKeyNValueContains(final String k, final String v) {
        if (k == null || v == null) {
            return false;
        }
        for (HTTPHeader s : br.getRequest().getHeaders()) {
            if (s.getKey().contains(k) && s.getValue().contains(v)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkSsl() {
        return SubConfiguration.getConfig("keep2share.cc").getBooleanProperty(SSL_CONNECTION, false);
    }

    private static String fixLinkSSL(String link) {
        if (checkSsl()) {
            link = link.replace("http://", "https://");
        } else {
            link = link.replace("https://", "http://");
        }
        return link;
    }

    private boolean apiEnabled() {
        return this.getPluginConfig().getBooleanProperty(USE_API, false);
    }

    private void setConfigElements() {
        // this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), USE_API,
        // JDL.L("plugins.hoster.Keep2ShareCc.useAPI", "Use API (recommended!)")).setDefaultValue(false));
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, JDL.L("plugins.hoster.Keep2ShareCc.preferSSL", "Use Secure Communication over SSL (HTTPS://)")).setDefaultValue(false));
    }

    private static boolean isJava7nJDStable() {
        if (System.getProperty("jd.revision.jdownloaderrevision") == null && System.getProperty("java.version").matches("1\\.[7-9].+")) {
            return true;
        } else {
            return false;
        }
    }

    private static AtomicBoolean stableSucks = new AtomicBoolean(false);

    public static void showSSLWarning(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        boolean xSystem = CrossSystem.isOpenBrowserSupported();
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " :: Java 7+ && HTTPS Post Requests.";
                            message = "Wegen einem Bug in in Java 7+ in dieser JDownloader version koennen wir keine HTTPS Post Requests ausfuehren.\r\n";
                            message += "Wir haben eine Notloesung ergaenzt durch die man weiterhin diese JDownloader Version nutzen kann.\r\n";
                            message += "Bitte bedenke, dass HTTPS Post Requests als HTTP gesendet werden. Nutzung auf eigene Gefahr!\r\n";
                            message += "Falls du keine unverschluesselten Daten versenden willst, update bitte auf JDownloader 2!\r\n";
                            if (xSystem) {
                                message += "JDownloader 2 Installationsanleitung und Downloadlink: Klicke -OK- (per Browser oeffnen)\r\n ";
                            } else {
                                message += "JDownloader 2 Installationsanleitung und Downloadlink:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                            }
                        } else if ("es".equalsIgnoreCase(lng)) {
                            title = domain + " :: Java 7+ && HTTPS Solicitudes Post.";
                            message = "Debido a un bug en Java 7+, al utilizar esta versión de JDownloader, no se puede enviar correctamente las solicitudes Post en HTTPS\r\n";
                            message += "Por ello, hemos añadido una solución alternativa para que pueda seguir utilizando esta versión de JDownloader...\r\n";
                            message += "Tenga en cuenta que las peticiones Post de HTTPS se envían como HTTP. Utilice esto a su propia discreción.\r\n";
                            message += "Si usted no desea enviar información o datos desencriptados, por favor utilice JDownloader 2!\r\n";
                            if (xSystem) {
                                message += " Las instrucciones para descargar e instalar Jdownloader 2 se muestran a continuación: Hacer Click en -Aceptar- (El navegador de internet se abrirá)\r\n ";
                            } else {
                                message += " Las instrucciones para descargar e instalar Jdownloader 2 se muestran a continuación, enlace :\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                            }
                        } else {
                            title = domain + " :: Java 7+ && HTTPS Post Requests.";
                            message = "Due to a bug in Java 7+ when using this version of JDownloader, we can not successfully send HTTPS Post Requests.\r\n";
                            message += "We have added a work around so you can continue to use this version of JDownloader...\r\n";
                            message += "Please be aware that HTTPS Post Requests are sent as HTTP. Use at your own discretion.\r\n";
                            message += "If you do not want to send unecrypted data, please upgrade to JDownloader 2!\r\n";
                            if (xSystem) {
                                message += "Jdownloader 2 install instructions and download link: Click -OK- (open in browser)\r\n ";
                            } else {
                                message += "JDownloader 2 install instructions and download link:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                            }
                        }
                        int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                        if (xSystem && JOptionPane.OK_OPTION == result) {
                            CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
                        }
                        stableSucks.set(true);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

}