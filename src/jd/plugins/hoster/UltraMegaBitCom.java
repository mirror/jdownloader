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
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadto.us", "ultramegabit.com" }, urls = { "https?://(www\\.)?(ultramegabit\\.com|uploadto\\.us)/file/details/[A-Za-z0-9\\-_]+", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2, 0 })
public class UltraMegaBitCom extends PluginForHost {

    public UltraMegaBitCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE);
        /* Needed for premium, servers are a bit slow */
        this.setStartIntervall(20 * 1000l);
    }

    @Override
    public boolean isPremiumEnabled() {
        return "uploadto.us".equals(getHost());
    }

    @Override
    public String getAGBLink() {
        return "http://uploadto.us/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        /* remove www., force https */
        link.setUrlDownload("https://uploadto.us/file/details/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9\\-_]+)$").getMatch(0));
    }

    public Boolean rewriteHost(DownloadLink link) {
        if (isPremiumEnabled()) {
            if (link != null && "ultramegabit.com".equals(link.getHost())) {
                link.setHost("uploadto.us");
                return true;
            }
            return false;
        }
        return null;
    }

    public String rewriteHost(String host) {
        if ("ultramegabit.com".equals(getHost())) {
            if (host == null || "ultramegabit.com".equals(host)) {
                return "uploadto.us";
            }
        }
        return null;
    }

    public Boolean rewriteHost(Account acc) {
        if (isPremiumEnabled()) {
            if (acc != null && "ultramegabit.com".equals(acc.getHoster())) {
                acc.setHoster("uploadto.us");
                return true;
            }
            return false;
        }
        return null;
    }

    private static AtomicReference<String> agent   = new AtomicReference<String>(null);

    private static AtomicInteger           maxPrem = new AtomicInteger(1);

    private Browser prepBrowser(final Browser prepBr) {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("uploadto.us/folder/add/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>uploadto\\.us - ([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h4>(<img[^>]+>)?(.*?) \\(([^\\)]+)\\)</h4>").getMatch(1);
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        String filesize = br.getRegex("data-toggle=\"modal\">Download \\(([^<>\"]*?)\\) <span").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("id=\"download_button\" value=\"Free download \\(([^<>\"]*?)\\)\"").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("<h4>(<img[^>]+>)?(.*?) \\(([^\\)]+)\\)</h4>").getMatch(2);
            }
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        // place offline content here, can contain filenames/filesize this is useful to users.
        if (br.containsHTML(">File not found<|>File restricted<|>File not available")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Deleted because of inactivity
        if (br.containsHTML(">File has been deleted|>We're sorry\\. This file has been deleted due to inactivity\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // now throw ERROR_PLUGIN_DEFECT
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        final long timeBefore = System.currentTimeMillis();
        if (br.containsHTML(">Premium members only<|The owner of this file has decided to only allow premium members to download it")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        // Only seen in a log
        if (br.containsHTML(">Download slot limit reached<")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        final String rcid = br.getRegex("\\?k=([^<>\"]*?)\"").getMatch(0);
        Form dlform = null;
        for (final Form form : br.getForms()) {
            if (form.containsHTML("uploadto\\.us/file/download")) {
                dlform = form;
            }
        }
        if (rcid == null || dlform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        // final String encode = dlform.getInputField("encode").getValue();
        final String csrf_token = dlform.getInputField("csrf_token").getValue();
        br.setCookie(MAINPAGE, "csrf_cookie", csrf_token);
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcid);
        rc.load();
        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        final String c = getCaptchaCode(cf, downloadLink);
        dlform.put("recaptcha_response_field", c);
        dlform.put("recaptcha_challenge_field", rc.getChallenge());
        dlform.remove(null);
        waitTime(timeBefore, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlform, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleServerError();
            handleErrors(account);
            if (br.containsHTML(">Download limit exceeded<|<div id=\"file_delay_carousel\"")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            } else if (br.containsHTML("guests are only able to download 1 file every")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
            } else if (br.containsHTML(">Account limitation notice|files smaller than")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) {
                        throw (PluginException) e;
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
            } else if (br.containsHTML("<h3 id=\"download_delay\">Please wait\\.\\.\\.</h3>")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitSum());
            } else if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/|>The ReCAPTCHA field is required)")) {
                try {
                    invalidateLastChallengeResponse();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                try {
                    validateLastChallengeResponse();
                } catch (final Throwable e) {
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    /**
     * Shared server error handling method between free and premium.
     * 
     * @throws PluginException
     */
    private void handleServerError() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Hoster Issue, Please contact hoster for resolution");
        } else if (dl.getConnection().getResponseCode() == 404 && br.getURL().endsWith("/file/oops")) {
            // hoster throwing 404, treat as server error or file not found??
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (dl.getConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500");
        } else if (br.containsHTML("<b>Fatal error</b>:|<h4>A PHP Error was encountered</h4>")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Fatal server error");
        }
    }

    /**
     * Shared errors handling method between free and premium
     * 
     * @param account
     * @throws PluginException
     */
    private void handleErrors(final Account account) throws PluginException {
        // some reason this shows up for accounts also... go figure they lock useage ip
        if (br.containsHTML("<h4>File access denied by file owner</h4>") && (br.containsHTML("<p>We're sorry\\. This owner of this file has imposed additional access limitations to his original content") || br.containsHTML("\\s*You have exceeded the number of downloads this file owner allows from a single IP in 24 hours\\.<"))) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        }
    }

    private void waitTime(long timeBefore, final DownloadLink downloadLink) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        final String ttt = "30";
        if (ttt != null) {
            int wait = Integer.parseInt(ttt);
            wait -= passedTime;
            logger.info("[Seconds] Waittime on the page: " + ttt);
            logger.info("[Seconds] Passed time: " + passedTime);
            logger.info("[Seconds] Total time to wait: " + wait);
            if (wait > 0) {
                sleep(wait * 1000l, downloadLink);
            }
        }
    }

    private static final String MAINPAGE = "http://uploadto.us";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private AccountInfo login(final Account account, final boolean force, final AccountInfo AI) throws Exception {
        AccountInfo ai = AI;
        if (ai == null) {
            ai = new AccountInfo();
        }
        synchronized (LOCK) {
            try {
                // lets try and prevent hoster redirects caused by browser history.
                br = new Browser();
                // Load cookies
                br.setCookiesExclusive(true);
                prepBrowser(br);
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
                        return ai;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("https://uploadto.us/login");
                final String token = br.getRegex("name=\"csrf_token\" value=\"([^<>\"]*?)\"").getMatch(0);
                final String lang = System.getProperty("user.language");
                if (token == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage("/login", "csrf_token=" + token + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML(">Form validation errors found<|>Invalid username or password<") || br.getURL().contains("uploadto.us/login")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
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
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
            // need todo this here otherwise login from dl method will not be able to determine "free" status.
            br.getPage("/user/details");
            String space = br.getRegex("<span class=\"glyphicon glyphicon-hdd\"></span> ([\\d\\.]+ [A-Za-z]+)").getMatch(0);
            if (space == null) {
                space = br.getRegex("<li title=\"Quota\"[^\r\n]+\">([\\d\\.]+ [A-Za-z]+) / [^\r\n]+</li>").getMatch(0);
            }
            if (space != null) {
                ai.setUsedSpace(SizeFormatter.getSize(space));
            }
            String filesNum = br.getRegex("<span class=\"glyphicon glyphicon-file\"></span> ([\\d]+)").getMatch(0);
            if (filesNum != null) {
                ai.setFilesNum(Long.parseLong(filesNum));
            }
            ai.setUnlimitedTraffic();
            br.getPage("/user/subscription");

            final boolean ispremium = (br.containsHTML("\"Premium Member\"") || br.containsHTML("premium subscription</h5>"));
            // some premiums have no expiration date, page shows only: Account status: Premium
            String expire = br.getRegex("<h5>Next rebill at (\\d+:\\d+(am|pm) \\d+/\\d+/\\d+)</h5>").getMatch(0);
            if (expire == null) {
                expire = br.getRegex("<h5>Account expires at (\\d+:\\d+(am|pm) \\d+/\\d+/\\d+)</h5>").getMatch(0);
            }
            if (expire == null && !ispremium) {
                // "Member"
                maxPrem.set(1);
                account.setProperty("free", true);
                try {
                    account.setType(AccountType.FREE);
                    account.setMaxSimultanDownloads(maxPrem.get());
                    account.setConcurrentUsePossible(false);
                } catch (final Throwable e) {
                    /* not available in old Stable 0.9.581 */
                }
                ai.setStatus("Free Account");
                account.setValid(true);
                if (AI == null) {
                    account.setAccountInfo(ai);
                }
                return ai;
            } else if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "h:mma dd/MM/yyyy", Locale.ENGLISH));
                if (ai.isExpired()) {
                    ai.setValidUntil(-1);
                    maxPrem.set(1);
                    account.setProperty("free", true);
                    try {
                        account.setType(AccountType.FREE);
                        account.setMaxSimultanDownloads(maxPrem.get());
                        account.setConcurrentUsePossible(false);
                    } catch (final Throwable e) {
                        /* not available in old Stable 0.9.581 */
                    }
                    ai.setStatus("Free Account");
                    account.setValid(true);
                    if (AI == null) {
                        account.setAccountInfo(ai);
                    }
                    return ai;
                }
            }
            account.setProperty("free", false);
            account.setValid(true);
            maxPrem.set(20);
            try {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Premium Account");
            if (AI == null) {
                account.setAccountInfo(ai);
            }
            return ai;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            if (System.getProperty("jd.revision.jdownloaderrevision") == null && System.getProperty("java.version").matches("1\\.[7-9].+")) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                String lng = System.getProperty("user.language");
                                String message = null;
                                String title = null;
                                boolean xSystem = CrossSystem.isOpenBrowserSupported();
                                title = "JDownloader 2 Dependancy";
                                message = "In order to use this plugin you will need to use JDownloader 2.\r\n";
                                if (xSystem) {
                                    message += "JDownloader 2 install instructions and download link: Click -OK- (open in browser)\r\n ";
                                } else {
                                    message += "JDownloader 2 install instructions and download link:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                                }
                                int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                                if (xSystem && JOptionPane.OK_OPTION == result) {
                                    CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
                                }
                            } catch (Throwable e) {
                            }
                        }
                    });
                } catch (Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                return login(account, true, ai);
            }
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }

    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        // Force login or download will fail
        login(account, true, null);
        if (account.getBooleanProperty("free", false)) {
            br.getPage(link.getDownloadURL());
            doFree(link, account);
        } else {
            final String token = br.getCookie(MAINPAGE, "csrf_cookie");
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setFollowRedirects(false);
            br.postPage("https://uploadto.us/file/download", "csrf_token=" + token + "&encode=" + new Regex(link.getDownloadURL(), "([A-Za-z0-9\\-_]+)$").getMatch(0));
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, finallink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                handleServerError();
                handleErrors(account);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            }
            link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
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

    private long waitSum() {
        // time into the future
        String test = br.getRegex("ts = \\((\\d+)").getMatch(0);
        // current time
        long ct = System.currentTimeMillis();
        // ms wait
        long wait1 = Integer.parseInt(test);
        wait1 += 3600;
        wait1 = wait1 * 1000;
        long result = wait1 - ct;
        return result;
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