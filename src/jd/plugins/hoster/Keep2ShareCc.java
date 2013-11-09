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

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keep2share.cc" }, urls = { "http://(www\\.)?(keep2share|k2s|k2share|keep2s|keep2)\\.cc/file/[a-z0-9]+" }, flags = { 2 })
public class Keep2ShareCc extends PluginForHost {

    public Keep2ShareCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://keep2share.cc/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://k2s.cc/page/terms.html";
    }

    private final String           DOWNLOADPOSSIBLE = ">To download this file with slow speed, use";
    private final String           MAINPAGE         = "http://keep2s.cc";
    private final String           DOMAINS          = "(https?://(www\\.)?(keep2share|k2s|k2share|keep2s|keep2)\\.cc)";

    private static Object          LOCK             = new Object();

    private static StringContainer agent            = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("://[a-zA-Z0-9\\.]+\\.cc/", "://keep2s.cc/"));
    }

    private Browser prepBrowser(final Browser prepBr) {
        // define custom browser headers and language settings.
        if (agent.string == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent.string);
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Pragma", null);
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
                            if (JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + domain + "&freedialog"));
                        }
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        String filename = null, filesize = null;
        // This might not be needed anymore but keeping it doesn't hurt either
        if (br.containsHTML(DOWNLOADPOSSIBLE)) {
            filename = br.getRegex(">Downloading file:</span><br>[\t\n\r ]+<span class=\"c2\">.*?alt=\"\" style=\"\">([^<>\"]*?)</span>").getMatch(0);
            filesize = br.getRegex("File size ([^<>\"]*?)</div>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("File: <span>([^<>\"]*?)</span>").getMatch(0);
            if (filename == null) {
                // offline/deleted
                filename = br.getRegex("File name:</b>(.*?)<br>").getMatch(0);
            }
        }
        if (filesize == null) {
            filesize = br.getRegex(">Size: ([^<>\"]*?)</div>").getMatch(0);
            if (filesize == null) {
                // offline/deleted
                filesize = br.getRegex("<b>File size:</b>(.*?)<br>").getMatch(0);
            }
        }
        if (filename != null) link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        if (br.containsHTML("Downloading blocked due to")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Downloading blocked", 10 * 60 * 1000l);
        // you can set filename for offline links! handling should come here!
        if (br.containsHTML("Sorry, an error occurred while processing your request|File not found or deleted|>Sorry, this file is blocked or deleted\\.</h5>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        checkShowFreeDialog();
        br.setFollowRedirects(false);
        if (br.containsHTML("File size to large\\!<") || br.containsHTML("Only <b>Premium</b> access<br>") || br.containsHTML("only for premium members")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to premium members");
        }
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (br.containsHTML(DOWNLOADPOSSIBLE)) {
                dllink = getDllink();
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                if (br.containsHTML("Traffic limit exceed\\!<br>|Download count files exceed!<br>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
                final String uniqueID = br.getRegex("name=\"slow_id\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (uniqueID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.postPage(br.getURL(), "yt0=&slow_id=" + uniqueID);
                if (br.containsHTML("Free user can't download large files")) throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to premium members");
                Browser br2 = br.cloneBrowser();
                // domain not transferable!
                br2.getPage("http://static.keep2share.cc/ext/evercookie/evercookie.swf");
                // can be here also, raztoki 20130521!
                dllink = getDllink();
                if (dllink == null) {
                    handleFreeErrors();
                    if (br.containsHTML("Free account does not allow to download more than one file at the same time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                    if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                        logger.info("Detected captcha method \"Re Captcha\" for this host");
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                        if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        rc.setId(id);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode(cf, downloadLink);
                        br.postPage(br.getURL(), "CaptchaForm%5Bcode%5D=&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&free=1&freeDownloadRequest=1&yt0=&uniqueId=" + uniqueID);
                        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else {
                        final String captchaLink = br.getRegex("\"(/file/captcha\\.html\\?[^\"]+)\"").getMatch(0);
                        if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        final String code = getCaptchaCode(new Regex(br.getURL(), DOMAINS).getMatch(0) + captchaLink, downloadLink);
                        br.postPage(br.getURL(), "CaptchaForm%5Bcode%5D=" + code + "&free=1&freeDownloadRequest=1&uniqueId=" + uniqueID);
                        if (br.containsHTML(">The verification code is incorrect|/site/captcha.html")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
                    }
                    /** Skippable */
                    int wait = 30;
                    final String waittime = br.getRegex("<div id=\"download\\-wait\\-timer\">[\t\n\r ]+(\\d+)[\t\n\r ]+</div>").getMatch(0);
                    if (waittime != null) wait = Integer.parseInt(waittime);
                    sleep(wait * 1001l, downloadLink);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.postPage(br.getURL(), "free=1&uniqueId=" + uniqueID);
                    handleFreeErrors();
                    br.getHeaders().put("X-Requested-With", null);
                    dllink = getDllink();
                    if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        logger.warning("dllink = " + dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info(br.toString());
            dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = dllink.replace("\\", "");
            // Try again...
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private void handleFreeErrors() throws PluginException {
        if (br.containsHTML("\">Downloading is not possible<")) {
            int hours = 0, minutes = 0, seconds = 0;
            final Regex waitregex = br.getRegex("Please wait (\\d{2}):(\\d{2}):(\\d{2}) to download this file");
            final String hrs = waitregex.getMatch(0);
            if (hrs != null) hours = Integer.parseInt(hrs);
            final String mins = waitregex.getMatch(1);
            if (mins != null) minutes = Integer.parseInt(mins);
            final String secs = waitregex.getMatch(2);
            if (secs != null) seconds = Integer.parseInt(secs);
            final long totalwait = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000l) + (seconds * 1000l);
            if (totalwait > 0) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, totalwait + 10000l);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);

        }
    }

    private String getDllink() throws PluginException {
        String dllink = br.getRegex("(\\'|\")(/file/url\\.html\\?file=[a-z0-9]+)(\\'|\")").getMatch(1);
        if (dllink != null) dllink = new Regex(br.getURL(), DOMAINS).getMatch(0) + dllink;
        return dllink;
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
    private HashMap<String, String> login(final Account account, final boolean force, AtomicBoolean validateCookie) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBrowser(br);
                br.setFollowRedirects(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && (!force || (validateCookie != null && validateCookie.get() == true))) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        if (validateCookie != null) {
                            br.getPage(MAINPAGE + "/site/profile.html");
                            if (force == false || !br.getURL().contains("login.html")) { return cookies; }
                        } else {
                            return cookies;
                        }
                    }
                }
                if (validateCookie != null) validateCookie.set(false);
                br.getPage(MAINPAGE + "/login.html");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                String postData = "LoginForm%5BrememberMe%5D=0&LoginForm%5BrememberMe%5D=1&LoginForm%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass());
                // Handle stupid login captcha
                final String captchaLink = br.getRegex("\"(/auth/captcha\\.html\\?v=[a-z0-9]+)\"").getMatch(0);
                if (captchaLink != null) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "keep2share.cc", "http://keep2share.cc", true);
                    final String code = getCaptchaCode("http://keep2share.cc" + captchaLink, dummyLink);
                    postData += "&LoginForm%5BverifyCode%5D=" + Encoding.urlEncode(code);
                } else {
                    if (br.containsHTML("recaptcha/api/challenge") || br.containsHTML("Recaptcha.create")) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", "keep2share.cc", "http://keep2share.cc", true);
                        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        String challenge = br.getRegex("recaptcha/api/challenge\\?k=(.*?)\"").getMatch(0);
                        if (challenge == null) challenge = br.getRegex("Recaptcha.create\\('(.*?)'").getMatch(0);
                        rc.setId(challenge);
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, dummyLink);
                        postData = postData + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c);
                    }
                }
                br.postPage(MAINPAGE + "/login.html", postData);
                if (br.containsHTML("Incorrect username or password")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Incorrect username or password", PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (br.containsHTML("The verification code is incorrect.")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "LoginCaptcha invalid", PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (br.containsHTML(">We have a suspicion that your account was stolen, this is why we")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account temporarily blocked", PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (br.containsHTML(">Please fill in the form with your login credentials")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account invalid", PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.getHeaders().put("X-Requested-With", null);
                String url = br.getRegex("url\":\"(.*?)\"").getMatch(0);
                if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        AtomicBoolean validateCookie = new AtomicBoolean(true);
        try {
            login(account, true, validateCookie);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (validateCookie.get() == false) br.getPage(MAINPAGE + "/site/profile.html");
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
            if (expire == null) expire = br.getRegex("Premium expires:\\s*?<b>(\\d{4}\\.\\d{2}\\.\\d{2})").getMatch(0);
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
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        HashMap<String, String> cookies = login(account, false, null);
        br.getPage(MAINPAGE + "/site/profile.html");
        if (br.getURL().contains("login.html")) {
            logger.info("Redirected to login page, seems cookies are no longer valid!");
            synchronized (LOCK) {
                if (cookies == account.getProperty("cookies", null)) {
                    account.setProperty("cookies", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("class=\"free\">Free</a>")) {
            br.getPage(link.getDownloadURL());
            doFree(link);
        } else {
            br.setFollowRedirects(false);
            br.getPage(link.getDownloadURL());
            // Set cookies for other domain if it is changed via redirect
            String currentDomain = MAINPAGE.replace("http://", "");
            String newDomain = null;
            String dllink = br.getRedirectLocation();
            if (dllink == null) dllink = br.getRegex("(\\'|\")(/file/url\\.html\\?file=[a-z0-9]+)(\\'|\")").getMatch(1);
            if (dllink != null && !dllink.contains(currentDomain))
                newDomain = new Regex(dllink, "https?://([^<>\"/]*?)/").getMatch(0);
            else if (!br.getURL().contains(currentDomain)) {
                newDomain = new Regex(br.getURL(), "https?://([^<>\"/]*?)/").getMatch(0);
            }
            if (newDomain != null) {
                resetCookies(account, currentDomain, newDomain);
                br.getPage(link.getDownloadURL().replace(currentDomain, newDomain));
                dllink = br.getRedirectLocation();
                if (dllink == null) dllink = br.getRegex("(\\'|\")(/file/url\\.html\\?file=[a-z0-9]+)(\\'|\")").getMatch(1);
            }
            if (newDomain != null) currentDomain = newDomain;
            if (dllink == null) {
                if (br.containsHTML("Traffic limit exceed\\!<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                synchronized (LOCK) {
                    if (cookies == account.getProperty("cookies", null) && !account.getBooleanProperty("retried_once", false)) {
                        account.setProperty("cookies", Property.NULL);
                        account.setProperty("retried_once", true);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(dllink);
            if (!dllink.startsWith("http")) {
                dllink = "http://" + currentDomain + dllink;
            }
            logger.info("dllink = " + dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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
}