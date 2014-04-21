//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "asfile.com" }, urls = { "http://(www\\.)?asfile\\.com/file/[A-Za-z0-9]+" }, flags = { 2 })
public class AsFileCom extends PluginForHost {

    public AsFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.enablePremium("http://asfile.com/en/index/pay");
    }

    @Override
    public String getAGBLink() {
        return "http://asfile.com/en/page/offer";
    }

    private static Object       LOCK     = new Object();
    private static final String MAINPAGE = "http://asfile.com";

    private Browser prepBrowser(final Browser prepBr) {
        // required for native cloudflare support, without the need to repeat requests.
        try {
            /* not available in old stable */
            prepBr.setAllowedResponseCodes(new int[] { 503 });
        } catch (Throwable e) {
        }
        prepBr.getHeaders().put("Accept-Language", "en-EN");
        HashMap<String, String> map = null;
        synchronized (cloudflareCookies) {
            map = new HashMap<String, String>(cloudflareCookies);
            if (!map.isEmpty()) {
                for (final Map.Entry<String, String> cookieEntry : map.entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    prepBr.setCookie(this.getHost(), key, value);
                }
            }
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>ASfile\\.com</title>|>Page not found<|Delete Reason:|No htmlCode read)") || br.getURL().contains("/file_is_unavailable/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename;
        if (br.getURL().contains("/password/")) {
            filename = br.getRegex("This file ([^<>\"]*?) is password protected.").getMatch(0);
            link.getLinkStatus().setStatusText("This link is password protected!");
        } else {
            filename = br.getRegex("<meta name=\"title\" content=\"Free download ([^<>\"\\']+)\"").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>Free download ([^<>\"\\']+)</title>").getMatch(0);
            if (filename == null) filename = br.getRegex(">Download:</div><div class=\"div_variable\"><strong>(.*?)</strong>").getMatch(0);
            String filesize = br.getRegex(">File size:</div><div class=\"div_variable\">([^<>\"]*?)<").getMatch(0);
            if (filesize == null) filesize = br.getRegex("File size: (.*?)</div>").getMatch(0);
            if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        String passCode = null;
        String dllink = downloadLink.getStringProperty("directFree", null);
        if (dllink != null) {
            prepBrowser(br);
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                /* direct link no longer valid */
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                } finally {
                    dllink = null;
                }
            }
            if (dllink != null) {
                /* direct link still valid */
                downloadLink.setProperty("direct", dllink);
                dl.startDownload();
                return;
            }
        }
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        // Password handling
        if (br.getURL().contains("/password/")) {
            passCode = downloadLink.getStringProperty("pass", null);
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            br.postPage(br.getURL(), "password=" + passCode);
            if (br.getURL().contains("/password/")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
        }

        if (br.containsHTML("This file is available only to premium users")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium only");
            }
        }
        long totalReconnectWait = 0;
        final String waitMin = br.getRegex("class=\"orange\">(\\d+)</span>[\t\n\r ]+<span id=\"measure\">[\t\n\r ]+minutes").getMatch(0);
        if (waitMin != null) totalReconnectWait += Long.parseLong(waitMin) * 60 * 1001l;
        final String waitSec = br.getRegex("class=\"orange\">(\\d+)</span>[\t\n\r ]+<span id=\"measure\">[\t\n\r ]+seconds").getMatch(0);
        // waitSe is always there so only add it if we also have minutes
        if (waitSec != null && waitMin != null) totalReconnectWait += Long.parseLong(waitSec) * 1001l;
        if (totalReconnectWait > 0) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, totalReconnectWait);
        final String fileID = new Regex(downloadLink.getDownloadURL(), "asfile\\.com/file/(.+)").getMatch(0);
        final long timeBefore = System.currentTimeMillis();
        // Captcha waittime can be skipped
        waitTime(timeBefore, downloadLink, true);
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
        if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        rc.setId(id);
        rc.load();
        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        final String c = getCaptchaCode(cf, downloadLink);
        postPage(br.getURL(), "recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c));
        if (!br.containsHTML("/free\\-download/file/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        getPage("http://asfile.com/en/free-download/file/" + fileID);
        if (br.containsHTML("You have exceeded the download limit for today")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        if (br.containsHTML(">This file TEMPORARY unavailable")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Temporarily unavailable due technical problems", 60 * 60 * 1000l);
        final String hash = br.getRegex("hash: \\'([a-z0-9]+)\\'").getMatch(0);
        final String storage = br.getRegex("storage: \\'([^<>\"\\']+)\\'").getMatch(0);
        if (hash == null || storage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        waitTime(System.currentTimeMillis(), downloadLink, false);
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.postPage("http://asfile.com/en/index/convertHashToLink", "hash=" + hash + "&path=" + fileID + "&storage=" + Encoding.urlEncode(storage) + "&name=" + Encoding.urlEncode(downloadLink.getName()));
        final String correctedBR = brc.toString().replace("\\", "");
        dllink = new Regex(correctedBR, "\"url\":\"(http:[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(http://s\\d+\\.asfile\\.com/file/free/[a-z0-9]+/\\d+/[A-Za-z0-9]+/[^<>\"\\'/]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(2000, downloadLink);
        /*
         * resume no longer possible? at least with a given password it does not work
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directFree", dllink);
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        dl.startDownload();
    }

    private void waitTime(long timeBefore, final DownloadLink downloadLink, boolean skip) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        final String waittime = br.getRegex("class=\"orange\">(\\d+)</span>[\t\n\r ]+<span id=\"measure\">[\t\n\r ]+seconds").getMatch(0);
        int wait = 60;
        if (waittime != null) wait = Integer.parseInt(waittime);
        if (wait > 180) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        wait -= passedTime;
        logger.info("Waittime detected, waiting " + wait + " - " + passedTime + " seconds from now on...");
        if (wait > 0 && !skip) sleep(wait * 1000l, downloadLink);
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        prepBrowser(br);
        br.setReadTimeout(3 * 60 * 1000);
        synchronized (LOCK) {
            // Load cookies
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                String user = account.getUser();
                if (user == null || user.trim().length() == 0) {
                    /* passCode only */
                    br.setCookie(MAINPAGE, "code", account.getPass());
                    return;
                }
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                postPage(MAINPAGE + "/en/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                getPage(MAINPAGE + "/en/");
                final String lang = System.getProperty("user.language");
                if (br.containsHTML(">Fail login<") || !br.containsHTML("logout\">Logout ")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage("http://asfile.com/en/profile");
                if (br.containsHTML("Your account is: FREE<br")) {
                    logger.info("Free accounts are not supported!");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (!br.containsHTML("(<p>Your account:<strong> premium|Your account is: PREMIUM<br />)")) {
                    logger.info("This is an unsupported accounttype!");

                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        String user = account.getUser();
        if (user != null && user.trim().length() > 0) {
            String expire = br.getRegex("premium </strong>\\(to (\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2})\\)</p>").getMatch(0);
            if (expire == null) {
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd hh:mm", null));
            }
            ai.setStatus("Premium User");
        } else {
            getPage("http://asfile.com/en/index/pay");
            String expire = br.getRegex("You have got the premium access to: (\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2})</p>").getMatch(0);
            if (expire != null) ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy/MM/dd hh:mm", null));
            ai.setStatus("Passcode User");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, false);
        /* try direct link */
        String dllink = link.getStringProperty("direct", null);
        if (dllink != null) {
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                /* direct link no longer valid */
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                } finally {
                    dllink = null;
                }
            }
            if (dllink != null) {
                /* direct link still valid */
                link.setProperty("direct", dllink);
                dl.startDownload();
                return;
            }
        }
        String uid = new Regex(link.getDownloadURL(), "asfile\\.com/file/(.+)").getMatch(0);
        if (uid == null) {
            logger.warning("Couldn't find 'uid' value");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        try {
            br.getPage("http://asfile.com/en/premium-download/file/" + uid);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("500")) {
                logger.severe("500 error->account seems invalid!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (dllink == null) dllink = getDllink();
        if (dllink == null) {
            br.getPage("http://asfile.com/en/count_files/" + uid);
            dllink = getDllink();
            if (dllink == null) {
                logger.warning("Couldn't find 'dllink' value");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            link.setProperty("direct", null);
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("direct", dllink);
        dl.startDownload();
    }

    private String getDllink() throws PluginException {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("(https?://s\\d+\\.asfile\\.com/file/premium/[a-z0-9]+/\\d+/(\\w+/)?/?[A-Za-z0-9]+/[^<>\"\\']+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<p><a href=\"(http://[^<>\"\\'/]+)\"").getMatch(0);
                if (dllink == null) {
                    if (br.containsHTML("You have exceeded the download limit for today")) {
                        logger.info("You have exceeded the download limit for today");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    // 'extend link' is present on every page! thus disables
                    // account constantly when ddlink == null
                    if (!br.containsHTML("Your account is: PREMIUM<")) {
                        logger.info("Seems the account is no longer 'Premium'");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                }
            }
        }
        return dllink;
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

    private static HashMap<String, String> cloudflareCookies = new HashMap<String, String>();

    /**
     * Gets page <br />
     * - natively supports silly cloudflare anti DDoS crapola
     * 
     * @author raztoki
     */
    private void getPage(final String page) throws Exception {
        if (page == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        try {
            br.getPage(page);
        } catch (Exception e) {
            if (e instanceof PluginException) throw (PluginException) e;
            // should only be picked up now if not JD2
            if (br.getHttpConnection().getResponseCode() == 503 && br.getHttpConnection().getHeaderFields("server").contains("cloudflare-nginx")) {
                logger.warning("Cloudflare anti DDoS measures enabled, your version of JD can not support this. In order to go any further you will need to upgrade to JDownloader 2");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Cloudflare anti DDoS measures enabled");
            } else
                throw e;
        }
        // prevention is better than cure
        if (br.getHttpConnection().getResponseCode() == 503 && br.getHttpConnection().getHeaderFields("server").contains("cloudflare-nginx")) {
            String host = new Regex(page, "https?://([^/]+)(:\\d+)?/").getMatch(0);
            Form cloudflare = br.getFormbyProperty("id", "ChallengeForm");
            if (cloudflare == null) cloudflare = br.getFormbyProperty("id", "challenge-form");
            if (cloudflare != null) {
                String math = br.getRegex("\\$\\('#jschl_answer'\\)\\.val\\(([^\\)]+)\\);").getMatch(0);
                if (math == null) math = br.getRegex("a\\.value = ([\\d\\-\\.\\+\\*/]+);").getMatch(0);
                if (math == null) {
                    String variableName = br.getRegex("(\\w+)\\s*=\\s*\\$\\(\'#jschl_answer\'\\);").getMatch(0);
                    if (variableName != null) variableName = variableName.trim();
                    math = br.getRegex(variableName + "\\.val\\(([^\\)]+)\\)").getMatch(0);
                }
                if (math == null) {
                    logger.warning("Couldn't find 'math'");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // use js for now, but change to Javaluator as the provided string doesn't get evaluated by JS according to Javaluator
                // author.
                ScriptEngineManager mgr = new ScriptEngineManager();
                ScriptEngine engine = mgr.getEngineByName("JavaScript");
                cloudflare.put("jschl_answer", String.valueOf(((Object) engine.eval("(" + math + ") + " + host.length()))));
                Thread.sleep(5500);
                br.submitForm(cloudflare);
                if (br.getFormbyProperty("id", "ChallengeForm") != null || br.getFormbyProperty("id", "challenge-form") != null) {
                    logger.warning("Possible plugin error within cloudflare handling");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // lets save cloudflare cookie to reduce the need repeat cloudFlare()
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    if (new Regex(c.getKey(), "(cfduid|cf_clearance)").matches()) cookies.put(c.getKey(), c.getValue());
                }
                synchronized (cloudflareCookies) {
                    cloudflareCookies.clear();
                    cloudflareCookies.putAll(cookies);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void postPage(String page, final String postData) throws Exception {
        if (page == null || postData == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // stable sucks
        if (isJava7nJDStable() && page.startsWith("https")) page = page.replaceFirst("https://", "http://");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        try {
            br.postPage(page, postData);
        } finally {
            br.getHeaders().put("Content-Type", null);
        }
    }

    private void sendForm(final Form form) throws Exception {
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // stable sucks && lame to the max, lets try and send a form outside of desired protocol. (works with oteupload)
        if (Form.MethodType.POST.equals(form.getMethod())) {
            // if the form doesn't contain an action lets set one based on current br.getURL().
            if (form.getAction() == null || form.getAction().equals("")) form.setAction(br.getURL());
            if (isJava7nJDStable() && (form.getAction().contains("https://") || /* relative path */(!form.getAction().startsWith("http")))) {
                if (!form.getAction().startsWith("http") && br.getURL().contains("https://")) {
                    // change relative path into full path, with protocol correction
                    String basepath = new Regex(br.getURL(), "(https?://.+)/[^/]+$").getMatch(0);
                    String basedomain = new Regex(br.getURL(), "(https?://[^/]+)").getMatch(0);
                    String path = form.getAction();
                    String finalpath = null;
                    if (path.startsWith("/"))
                        finalpath = basedomain.replaceFirst("https://", "http://") + path;
                    else if (!path.startsWith("."))
                        finalpath = basepath.replaceFirst("https://", "http://") + path;
                    else {
                        // lacking builder for ../relative paths. this will do for now.
                        logger.info("Missing relative path builder. Must abort now... Try upgrading to JDownloader 2");
                        throw new PluginException(LinkStatus.ERROR_FATAL);
                    }
                    form.setAction(finalpath);
                } else
                    form.setAction(form.getAction().replaceFirst("https?://", "http://"));
                if (!stableSucks.get()) showSSLWarning(this.getHost());
            }
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        }
        try {
            br.submitForm(form);
        } finally {
            br.getHeaders().put("Content-Type", null);
        }
    }

    private boolean isJava7nJDStable() {
        if (System.getProperty("jd.revision.jdownloaderrevision") == null && System.getProperty("java.version").matches("1\\.[7-9].+"))
            return true;
        else
            return false;
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
                            if (xSystem)
                                message += "JDownloader 2 Installationsanleitung und Downloadlink: Klicke -OK- (per Browser oeffnen)\r\n ";
                            else
                                message += "JDownloader 2 Installationsanleitung und Downloadlink:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                        } else if ("es".equalsIgnoreCase(lng)) {
                            title = domain + " :: Java 7+ && HTTPS Solicitudes Post.";
                            message = "Debido a un bug en Java 7+, al utilizar esta versión de JDownloader, no se puede enviar correctamente las solicitudes Post en HTTPS\r\n";
                            message += "Por ello, hemos añadido una solución alternativa para que pueda seguir utilizando esta versión de JDownloader...\r\n";
                            message += "Tenga en cuenta que las peticiones Post de HTTPS se envían como HTTP. Utilice esto a su propia discreción.\r\n";
                            message += "Si usted no desea enviar información o datos desencriptados, por favor utilice JDownloader 2!\r\n";
                            if (xSystem)
                                message += " Las instrucciones para descargar e instalar Jdownloader 2 se muestran a continuación: Hacer Click en -Aceptar- (El navegador de internet se abrirá)\r\n ";
                            else
                                message += " Las instrucciones para descargar e instalar Jdownloader 2 se muestran a continuación, enlace :\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                        } else {
                            title = domain + " :: Java 7+ && HTTPS Post Requests.";
                            message = "Due to a bug in Java 7+ when using this version of JDownloader, we can not successfully send HTTPS Post Requests.\r\n";
                            message += "We have added a work around so you can continue to use this version of JDownloader...\r\n";
                            message += "Please be aware that HTTPS Post Requests are sent as HTTP. Use at your own discretion.\r\n";
                            message += "If you do not want to send unecrypted data, please upgrade to JDownloader 2!\r\n";
                            if (xSystem)
                                message += "Jdownloader 2 install instructions and download link: Click -OK- (open in browser)\r\n ";
                            else
                                message += "JDownloader 2 install instructions and download link:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                        }
                        int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                        if (xSystem && JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
                        stableSucks.set(true);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
    }

}