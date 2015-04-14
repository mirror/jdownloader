//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DummyScriptEnginePlugin.ThrowingRunnable;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datafile.com" }, urls = { "https?://(www\\.)?datafile\\.com/d/[A-Za-z0-9]+(/[^<>\"/]+)?" }, flags = { 2 })
public class DataFileCom extends PluginForHost {

    public DataFileCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://www.datafile.com/getpremium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.datafile.com/terms.html";
    }

    /* Connection stuff */
    private static final boolean           FREE_RESUME                  = false;
    private static final int               FREE_MAXCHUNKS               = 1;
    /*
     * How multiple free downloads are possible: Start first download & save timestamp of downloadstart. Next download can be started one
     * hour later - does not matter if the first one still runs. Tested up to 4 but more must be possible ;)
     */
    private static final int               FREE_MAXDOWNLOADS            = 20;
    private static final int               ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean           ACCOUNT_PREMIUM_RESUME       = true;
    private static final int               ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int               ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private final String                   PREMIUMONLY                  = "(\"Sorry\\. Only premium users can download this file\"|>This file can be downloaded only by users with<br />Premium account\\!<|>You can download files up to)";
    private final boolean                  SKIPRECONNECTWAITTIME        = true;
    private final boolean                  SKIPWAITTIME                 = true;

    private final String[]                 IPCHECK                      = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };
    private static final String            PROPERTY_LASTDOWNLOAD        = "datafilecom_lastdownload_timestamp";
    private final String                   PROPERTY_LASTIP              = "PROPERTY_LASTIP";
    private static AtomicReference<String> lastIP                       = new AtomicReference<String>();
    private static AtomicReference<String> currentIP                    = new AtomicReference<String>();
    private static HashMap<String, Long>   blockedIPsMap                = new HashMap<String, Long>();
    private static Object                  CTRLLOCK                     = new Object();
    private final Pattern                  IPREGEX                      = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static final long              FREE_RECONNECTWAIT           = 3610000L;

    private Account                        currAcc                      = null;
    private DownloadLink                   currDownloadLink             = null;

    // Connection Management
    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20]
    private static final AtomicInteger     totalMaxSimultanFreeDownload = new AtomicInteger(FREE_MAXDOWNLOADS);

    @SuppressWarnings({ "deprecation" })
    public void correctDownloadLink(final DownloadLink link) {
        final String unneededPart = new Regex(link.getDownloadURL(), "datafile\\.com/d/[A-Za-z0-9]+(/[^<>\"/]+)").getMatch(0);
        if (unneededPart != null) {
            final String urlfilename = new Regex(unneededPart, "/([^<>\"/]+)").getMatch(0);
            link.setUrlDownload(link.getDownloadURL().replace(unneededPart, ""));
            link.setProperty("urlfilename", Encoding.htmlDecode(urlfilename.trim()));
        }
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    /**
     * They have a linkchecker but it doesn't show filenames if they're not included in the URL: http://www.datafile.com/linkchecker.html
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        // Offline links should also have nice filenames
        final String urlFileName = link.getStringProperty("urlfilename", null);
        this.setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        redirectAntiDDos(br);
        br.setFollowRedirects(false);
        String filesize = null;
        // Limit reached -> Let's use their linkchecker to at least find the filesize and onlinestatus
        if (br.getURL().contains("code=7") || br.getURL().contains("code=9")) {
            final Browser br2 = br.cloneBrowser();
            br2.postPage("http://www.datafile.com/linkchecker.html", "btn=&links=" + Encoding.urlEncode(link.getDownloadURL()));
            filesize = br2.getRegex("title=\"File size\">([^<>\"]*?)</td>").getMatch(0);
            if (filesize != null) {
                link.getLinkStatus().setStatusText("Cannot show filename when a downloadlimit is reached");
                link.setDownloadSize(SizeFormatter.getSize(filesize));
                return AvailableStatus.TRUE;
            } else if (!br2.containsHTML(">Link<") && !br2.containsHTML(">Status<") && !br2.containsHTML(">File size<")) {
                // Maybe no table --> Link offline
                if (urlFileName != null) {
                    link.setName(urlFileName);
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* No results -> Unckeckable because if the limit */
            link.getLinkStatus().setStatusText("Cannot check available status when a downloadlimit is reached");
            if (urlFileName != null) {
                link.setName(urlFileName);
            }
            return AvailableStatus.UNCHECKABLE;
        }
        /* Invalid link */
        if (br.containsHTML("<div class=\"error\\-msg\">")) {
            if (urlFileName != null) {
                link.setName(urlFileName);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Deleted file */
        if (br.containsHTML(">Sorry but this file has been deleted") || br.getHttpConnection().getResponseCode() == 404) {
            if (urlFileName != null) {
                link.setName(urlFileName);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("ErrorCode 7: Download file count limit")) {
            if (urlFileName != null) {
                link.setName(urlFileName);
            }
            return AvailableStatus.UNCHECKABLE;
        }
        final String urlfilename = link.getStringProperty("urlfilename", null);
        final String decrypterfilename = link.getStringProperty("decrypterfilename", null);
        String sitefilename = br.getRegex("class=\"file\\-name\">([^<>\"]*?)</div>").getMatch(0);
        filesize = br.getRegex(">Filesize:<span class=\"lime\">([^<>\"]*?)</span>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex(">Filesize: <span class=\"lime\">([^<>\"]*?)</span>").getMatch(0);
        }
        if (sitefilename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        sitefilename = Encoding.htmlDecode(sitefilename.trim());
        if (decrypterfilename != null) {
            /* Folder-filenames are complete - filenames shown when accessing single links are sometimes cut! */
            link.setName(decrypterfilename);
        } else if (urlfilename != null && (urlfilename.length() > sitefilename.length())) {
            /* URL-filenames are complete - filenames shown when accessing single links are sometimes cut! */
            link.setName(urlfilename);
        } else {
            link.setName(sitefilename);
        }
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (br.containsHTML(PREMIUMONLY)) {
            link.getLinkStatus().setStatusText("This file can only be downloaded by premium users");
        }
        return AvailableStatus.TRUE;
    }

    public static class ScriptEnv {
        public static String atob(String string) {
            String ret = Encoding.Base64Decode(string);
            return ret;
        }

    }

    private void redirectAntiDDos(Browser br) throws Exception {
        String js = br.getRegex("<style type=\"text/css\">a\\{color\\: white\\;\\}</style><script language=\"JavaScript\">(.*)</script>").getMatch(0);
        if (js != null) {
            ScriptEngineManager mgr = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
            final ScriptEngine engine = mgr.getEngineByName("JavaScript");
            // history.length<1){document.body.innerHTML=''
            engine.eval("document={};document.body={};");
            engine.eval("window={};window.location={};");
            engine.eval("history=[];");
            // load java environment trusted
            DummyScriptEnginePlugin.runTrusted(new ThrowingRunnable<ScriptException>() {

                @Override
                public void run() throws ScriptException {
                    // atob requires String to be loaded for its parameter and return type
                    engine.eval("var string=" + String.class.getName() + ";");
                    engine.eval("var scriptEnv=Packages." + ScriptEnv.class.getName() + ";");

                    // create the atob function and redirect it to our java function
                    engine.eval("atob=function(str){return Packages." + ScriptEnv.class.getName() + ".atob(str)+\"\";}");
                    // cleanup
                    engine.eval("delete java;");
                    engine.eval("delete jd;");
                }

            });

            engine.eval(js);

            Object redirect = engine.eval("window.location.href");
            if (redirect != null) {
                br.getPage(redirect + "");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "AntiDDOS JS failed");
            }

        }

    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null, downloadLink);
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        currentIP.set(this.getIP());
        synchronized (CTRLLOCK) {
            /* Load list of saved IPs + timestamp of last download */
            final Object lastdownloadmap = this.getPluginConfig().getProperty(PROPERTY_LASTDOWNLOAD);
            if (lastdownloadmap != null && lastdownloadmap instanceof HashMap && blockedIPsMap.isEmpty()) {
                blockedIPsMap = (HashMap<String, Long>) lastdownloadmap;
            }
        }
        boolean captchaSuccess = false;
        if (br.containsHTML(PREMIUMONLY)) {
            // not possible to download under handleFree!
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (this.getPluginConfig().getBooleanProperty(ENABLE_FREE_STORED_WAITTIME, defaultENABLE_fREE_PARALLEL_DOWNLOADS)) {
                final long lastdownload = getPluginSavedLastDownloadTimestamp();
                final long passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload;
                /**
                 * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached!
                 */
                if (ipChanged(downloadLink) == false && passedTimeSinceLastDl < FREE_RECONNECTWAIT) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, FREE_RECONNECTWAIT - passedTimeSinceLastDl);
                }
            }
            handleURLErrors(this.br.getURL());
            final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            br.setFollowRedirects(false);
            final Regex waitTime = br.getRegex("class=\"time\">(\\d+):(\\d+):(\\d+)</span>");
            int tmphrs = 0, tmpmin = 0, tmpsecs = 0;
            String tempHours = waitTime.getMatch(0);
            if (tempHours != null) {
                tmphrs = Integer.parseInt(tempHours);
            }
            String tempMinutes = waitTime.getMatch(1);
            if (tempMinutes != null) {
                tmpmin = Integer.parseInt(tempMinutes);
            }
            String tempSeconds = waitTime.getMatch(2);
            if (tempSeconds != null) {
                tmpsecs = Integer.parseInt(tempSeconds);
            }
            final long wait = (tmphrs * 60 * 60 * 1000) + (tmpmin * 60 * 1000) + (tmpsecs * 1001);
            if (wait == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!SKIPRECONNECTWAITTIME && wait > 3601800) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait);
            }
            long timeBeforeCaptcha = System.currentTimeMillis();
            final String rcID = br.getRegex("api/challenge\\?k=([^<>\"]*?)\"").getMatch(0);
            if (rcID == null) {
                logger.warning("rcID is null");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            rc.load();
            for (int i = 1; i <= 5; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                if (!SKIPWAITTIME || i > 1) {
                    waitTime(timeBeforeCaptcha, downloadLink, wait);
                }
                // Validation phase, return token that need to be added to getFileDownloadLink call
                postPage("http://www.datafile.com/files/ajax.html", "doaction=validateCaptcha&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&fileid=" + fid);

                String token = br.getRegex("\\{\"success\":1,\"token\":\"(.*)\"\\}").getMatch(0);
                if (token == null || br.containsHTML("\"success\":0")) {
                    rc.reload();
                    continue;
                }
                postPage("http://www.datafile.com/files/ajax.html", "doaction=getFileDownloadLink&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&fileid=" + fid + "&token=" + token);
                captchaSuccess = true;
                break;
            }
            /*
             * Collection of possible "msg" values: "Please type the two words from picture", "The two words is not valid!<br \/>Please try
             * again."
             */
            if (!captchaSuccess) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            dllink = br.getRegex("\"link\":\"([^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
            if (dllink.startsWith("/")) {
                dllink = "http://datafile.com" + dllink;
            }
            /* Sometimes a limitmessage/errorcode is inside the link - handle that! */
            this.handleURLErrors(dllink);
        }
        try {

            /*
             * The download attempt already triggers reconnect waittime! Save timestamp here to calculate correct remaining waittime later!
             */
            try {
                logger.info("Downloadstart was attempted --> Setting timestamps in plugin config/account");
                synchronized (CTRLLOCK) {
                    blockedIPsMap.put(currentIP.get(), System.currentTimeMillis());
                    this.getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD, blockedIPsMap);
                    if (account != null) {
                        /* Storing on account is not (yet) needed but let's do it anyways - it might be useful in the future! */
                        account.setProperty(PROPERTY_LASTDOWNLOAD, System.currentTimeMillis());
                    }
                    this.setIP(downloadLink, account);
                }
            } catch (final Throwable e) {
            }

            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
                }
                br.followConnection();
                handleGeneralErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(getServerFilename());
            downloadLink.setProperty("directlink", dllink);
            // add download slot
            controlSlot(+1, account);
            try {
                dl.startDownload();
            } finally {
                // remove download slot
                controlSlot(-1, account);
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                logger.info("Retry on ERROR_DOWNLOAD_INCOMPLETE");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private void waitTime(long timeBefore, final DownloadLink downloadLink, long wait) throws PluginException {
        long passedTime = (System.currentTimeMillis() - timeBefore) - 1000;
        /** Ticket Time */
        wait -= passedTime;
        logger.info("Waittime detected, waiting " + wait + " - " + passedTime + " milliseconds from now on...");
        if (wait > 0) {
            sleep(wait, downloadLink);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                try {
                    // @since JD2
                    con = br2.openHeadConnection(dllink);
                } catch (final Throwable t) {
                    con = br2.openGetConnection(dllink);
                }
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    private static final String MAINPAGE = "http://datafile.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
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
                        return;
                    }
                }
                br.setFollowRedirects(true);
                // https is forced here anyways
                String protocol = "https://";
                if (isJava7nJDStable()) {
                    if (!stableSucks.get()) {
                        showSSLWarning(this.getHost());
                    }
                    // https is forced here anyways
                    protocol = "https://";
                }
                br.postPage(protocol + "www.datafile.com/login.html", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=0&remember_me=1&btn=");
                if (br.getCookie(MAINPAGE, "hash") == null || br.getCookie(MAINPAGE, "user") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (useAPI.get()) {
            return fetchAccountInfo_API(account, ai);
        } else {
            try {
                login(account, true);
            } catch (PluginException e) {
                account.setValid(false);
                throw e;
            }
            br.getPage("/profile.html");
            final String filesNum = br.getRegex(">Files: <span class=\"lime\">(\\d+)</span>").getMatch(0);
            if (filesNum != null) {
                ai.setFilesNum(Long.parseLong(filesNum));
            }
            final String space = br.getRegex(">Storage: <span class=\"lime\">([^<>\"]*?)</span>").getMatch(0);
            if (space != null) {
                ai.setUsedSpace(space.trim());
            }
            ai.setUnlimitedTraffic();
            String expire = br.getRegex("([a-zA-Z]{3} \\d{1,2}, \\d{4} \\d{1,2}:\\d{1,2})").getMatch(0);
            if (expire == null) {
                logger.info("JD could not detect account expire time, your account has been determined as a free account");
                account.setProperty("free", true);
                account.setProperty("totalMaxSim", ACCOUNT_FREE_MAXDOWNLOADS);
                ai.setStatus("Registered (free) account");
            } else {
                account.setProperty("free", false);
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMM dd, yyyy HH:mm", Locale.ENGLISH));
                account.setProperty("totalMaxSim", ACCOUNT_PREMIUM_MAXDOWNLOADS);
                ai.setStatus("Premium account");
            }
            account.setValid(true);
        }
        return ai;

    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account, downloadLink);
        if (useAPI.get() && !account.getBooleanProperty("free", false)) {
            /* API + premium */
            handlePremium_API(downloadLink, account);
        } else {
            /* Either disabled API or free account - free (account) download is not possible via API. */
            requestFileInformation(downloadLink);
            login(account, false);
            if (account.getBooleanProperty("free")) {
                br.getPage(downloadLink.getDownloadURL());
                // if the cached cookie expired, relogin.
                if (br.getCookie(MAINPAGE, "hash") == null || br.getCookie(MAINPAGE, "user") == null) {
                    synchronized (LOCK) {
                        account.setProperty("cookies", Property.NULL);
                        // if you retry, it can use another account...
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
                handleGeneralErrors();
                doFree(downloadLink, account);
            } else {
                br.setFollowRedirects(true);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
                if (dl.getConnection().getContentType().contains("html")) {
                    if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
                    }
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    handleGeneralErrors();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                downloadLink.setFinalFileName(getServerFilename());
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

    private String getServerFilename() {
        String finalname = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        final String finalFixedName = new Regex(finalname, "([^<>\"]*?)\"; creation\\-date=").getMatch(0);
        if (finalFixedName != null) {
            finalname = finalFixedName;
        }
        return finalname;
    }

    private void handleGeneralErrors() throws PluginException {
        final String redirect = br.getRedirectLocation();
        String errorCode = br.getRegex("ErrorCode (\\d+):").getMatch(0);
        if ((redirect != null && redirect.contains("error.html?code=")) || errorCode != null) {
            if (errorCode == null) {
                errorCode = new Regex(redirect, "error\\.html\\?code=(\\d+)").getMatch(0);
            }
            if (errorCode != null) {
                freeErrorcodeErrorhandling(errorCode);
            }
            logger.warning("Unknown error");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unknown error", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
    }

    /** Handles errors based on errorcode inside URL */
    private void handleURLErrors(final String url) throws PluginException {
        final String code = new Regex(url, "code=(\\d+)").getMatch(0);
        if (code != null) {
            freeErrorcodeErrorhandling(code);
        }
    }

    private void freeErrorcodeErrorhandling(final String errorcode) throws PluginException {
        if (errorcode.equals("6") || errorcode.equals("9")) {
            errorFreeTooManySimultanDownloads();
        } else if (errorcode.equals("7")) {
            errorDailyDownloadlimitReached();
        } else {
            logger.warning("Unknown error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    /**
     * code = 6 or 9
     *
     * Should only happen for free/free account mode.
     * */
    private void errorFreeTooManySimultanDownloads() throws PluginException {
        logger.info("You are downloading another file at this moment. Please wait for it to complete and then try again.");
        if (this.currAcc != null) {
            final AccountInfo ac = new AccountInfo();
            ac.setTrafficLeft(0);
            this.currAcc.setAccountInfo(ac);
            this.currAcc.setError(AccountError.TEMP_DISABLED, "You are downloading another file at this moment. Please wait for it to complete and then try again.");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "You are downloading another file at this moment. Please wait for it to complete and then try again.", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You are downloading another file at this moment. Please wait for it to complete and then try again.", FREE_RECONNECTWAIT);
        }
    }

    /**
     * code = 7
     *
     * Can happen in any download mode
     *
     * @throws PluginException
     * */
    private void errorDailyDownloadlimitReached() throws PluginException {
        if (this.currAcc == null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily downloadlimit reached", FREE_RECONNECTWAIT);
        } else if (this.currAcc != null && this.currAcc.getBooleanProperty("free", false)) {
            // reached daily download quota
            logger.info("You've reached daily download quota for " + this.currAcc.getUser() + " account");
            logger.info("Free account: Daily downloadlimit reached");
            final AccountInfo ac = new AccountInfo();
            ac.setTrafficLeft(0);
            this.currAcc.setAccountInfo(ac);
            this.currAcc.setError(AccountError.TEMP_DISABLED, "Daily downloadlimit reached");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Trafficlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else {
            logger.info("Premium account: Daily downloadlimit reached");
            final AccountInfo ac = new AccountInfo();
            ac.setTrafficLeft(0);
            this.currAcc.setAccountInfo(ac);
            this.currAcc.setError(AccountError.TEMP_DISABLED, "Daily downloadlimit reached");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Trafficlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
    }

    private void postPage(String url, final String postData) throws IOException {
        if (isJava7nJDStable() && url.toLowerCase().startsWith("https://")) {
            if (!stableSucks.get()) {
                showSSLWarning(this.getHost());
            }
            url = url.replaceFirst("https://", "http://");
        }
        br.postPage(url, postData);
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
    }

    private void prepBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setCookie(MAINPAGE, "lang", "en");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        /*
         * Sometimes saved datafile directlinks cause problems, are very slow or time out so this gives us a higher chance of a working
         * download after a reset.
         */
        link.setProperty("directlink", Property.NULL);
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

    private static AtomicBoolean useAPI = new AtomicBoolean(true);
    private final String         apiURL = "https://api.datafile.com";

    private Browser prepApiBrowser(final Browser ibr) {
        return ibr;
    }

    private synchronized void getPage(final Browser ibr, final String url, final Account account) throws Exception {
        if (account != null) {
            String apiToken = getApiToken(account);
            ibr.getPage(url + (url.matches("(" + apiURL + ")?/[a-zA-Z0-9]+/[a-zA-Z0-9]+\\?[a-zA-Z0-9]+.+") ? "&" : "?") + "token=" + apiToken);
            if (sessionTokenInValid(account, ibr)) {
                apiToken = getApiToken(account);
                if (apiToken != null) {
                    // can't sessionKeyInValid because getApiKey/loginKey return String, and loginKey uses a new Browser.
                    ibr.getPage(url + (url.matches("(" + apiURL + ")?/[a-zA-Z0-9]+/[a-zA-Z0-9]+\\?[a-zA-Z0-9]+.+") ? "&" : "?") + "token=" + apiToken);
                } else {
                    // failure occurred.
                    throw new PluginException(LinkStatus.ERROR_FATAL);
                }
            }
            // account specific errors which could happen at any point in time!
            if (sessionTokenInValid(account, ibr)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + errorMsg(ibr), PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else {
            ibr.getPage(url);
        }
        // error handling for generic errors which could occur at any point in time!
        if (("718".equalsIgnoreCase(getJson(ibr, "code")))) {
            // 718 ERR_API_IP_SUSPENDED The IP Address initiating the request has been suspended
            throw new PluginException(LinkStatus.ERROR_FATAL, "\r\n" + errorMsg(ibr));
        }
    }

    private String errorMsg(final Browser ibr) {
        final String message = getJson(ibr, "message");
        logger.warning(message);
        return message;
    }

    private synchronized String getApiToken(final Account account) throws Exception {
        String apiToken = account.getStringProperty("apiToken", null);
        if (apiToken == null) {
            apiToken = loginToken(account);
        }
        return apiToken;
    }

    private boolean sessionTokenInValid(final Account account, final Browser ibr) {
        final String code = getJson(ibr, "code");
        if (("909".equalsIgnoreCase(code) || "910".equalsIgnoreCase(code))) {
            // 909 Token not valid
            // 910 Token Expired
            account.setProperty("apiToken", Property.NULL);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private AccountInfo fetchAccountInfo_API(final Account account, final AccountInfo ai) throws Exception {
        try {
            loginToken(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        return account.getAccountInfo();
    }

    @SuppressWarnings("deprecation")
    private void handlePremium_API(final DownloadLink downloadLink, final Account account) throws Exception {
        // No API method for linkchecking, but can done based on this request response!
        getPage(br, apiURL + "/files/download?file=" + Encoding.urlEncode(downloadLink.getDownloadURL()), account);
        final String ddlink = getJson("download_url");
        if (ddlink == null) {
            final String code = getJson("code");
            if ("500".equalsIgnoreCase(code)) {
                // 500 MySQL server has gone away
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 10 * 60 * 1000l);
            } else if ("503".equalsIgnoreCase(code)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503: Server ID not found", 5 * 60 * 1000l);
            }
            if ("700".equalsIgnoreCase(code) || "701".equalsIgnoreCase(code)) {
                // 700 File url not valid
                // 701 File removed
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if ("702".equalsIgnoreCase(code) || "703".equalsIgnoreCase(code)) {
                // 702 File blocked
                // 703 File download prohibited
                /*
                 * not sure about this either, blocked..why ?? download prohibited..why ??
                 */
                // set this for now...
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            } else if ("704".equalsIgnoreCase(code)) {
                // 704 Not enough traffic
                /*
                 * This is a problem under current JD frame work.. For example: the file downloaded could be 10GiB, the hoster prevents
                 * because user has only 5GiB traffic left... yet we can't disable the account due to this, because they have 5GB traffic
                 * left. Also could be due to out of sync account stats? we don't have an exception to handle this...
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unhandled Error code or 'download_url' could not be found");
            }
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, ddlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleGeneralErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getServerFilename());
        // add download slot
        controlSlot(+1, account);
        try {
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }

    }

    private String loginToken(final Account account) throws Exception {
        final Browser nbr = new Browser();
        prepApiBrowser(nbr);
        nbr.getPage(apiURL + "/users/auth?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&accesskey=cddce1a5-a6dd-4300-9c08-eb70909de7c6");
        final String apiToken = getJson(nbr, "token");
        final String code = getJson(nbr, "code");
        if (apiToken != null) {
            account.setProperty("apiToken", apiToken);
            // all is good! lets update account info whilst we are at it. It's all here!
            AccountInfo ai = new AccountInfo();
            final String traffic_left = getJson(nbr, "traffic_left");
            final String primium_till = getJson(nbr, "premium_till");
            final String space_left = getJson(nbr, "space_left");
            if (primium_till != null) {
                // premium_till - time when user premium had expired, unix_timestamp, int (0 -no premium access)
                ai.setValidUntil(Long.parseLong(primium_till + "000"));
                if (!"0".equalsIgnoreCase(primium_till) || !ai.isExpired()) {
                    account.setProperty("free", false);
                    account.setProperty("totalMaxSim", ACCOUNT_PREMIUM_MAXDOWNLOADS);
                    ai.setStatus("Premium account");
                } else {
                    account.setProperty("free", true);
                    account.setProperty("totalMaxSim", ACCOUNT_FREE_MAXDOWNLOADS);
                    /* Don't use multiple free accounts at the same time. */
                    account.setConcurrentUsePossible(false);
                    ai.setStatus("Free account");
                    ai.setUnlimitedTraffic();
                    ai.setValidUntil(-1);
                }
            }
            if (traffic_left != null) {
                // traffic_left - user traffic left bytes for download files, int (-1 unlimited, 0 no traffic left)
                ai.setTrafficLeft(Long.parseLong(traffic_left));
            }
            if (space_left != null) {
                // space_left - user storage space left bytes for upload files, int ( -1 unlimited, 0 no space)
                // we only have space used not space left...
                // ai.setUsedSpace(Long.parseLong(space_left));
            }
            account.setAccountInfo(ai);
        } else if ("900".equalsIgnoreCase(code) || "901".equalsIgnoreCase(code) || "902".equalsIgnoreCase(code) || "903".equalsIgnoreCase(code)) {
            // 900 User not found
            // 901 Login can not be empty
            // 902 Password can not be empty
            // 903 User inactive
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + errorMsg(nbr), PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if ("1000".equalsIgnoreCase(code)) {
            // 1000 access key invalid
            useAPI.set(false);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "AccessKey invalid");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return apiToken;
    }

    private String getIP() throws PluginException {
        final Browser ip = new Browser();
        String currentIP = null;
        final ArrayList<String> checkIP = new ArrayList<String>(Arrays.asList(this.IPCHECK));
        Collections.shuffle(checkIP);
        for (final String ipServer : checkIP) {
            if (currentIP == null) {
                try {
                    ip.getPage(ipServer);
                    currentIP = ip.getRegex(this.IPREGEX).getMatch(0);
                    if (currentIP != null) {
                        break;
                    }
                } catch (final Throwable e) {
                }
            }
        }
        if (currentIP == null) {
            this.logger.warning("firewall/antivirus/malware/peerblock software is most likely is restricting accesss to JDownloader IP checking services");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return currentIP;
    }

    private boolean ipChanged(final DownloadLink link) throws PluginException {
        String currIP = null;
        if (currentIP.get() != null && new Regex(currentIP.get(), this.IPREGEX).matches()) {
            currIP = currentIP.get();
        } else {
            currIP = this.getIP();
        }
        if (currIP == null) {
            return false;
        }
        String lastIP = link.getStringProperty(PROPERTY_LASTIP, null);
        if (lastIP == null) {
            lastIP = DataFileCom.lastIP.get();
        }
        return !currIP.equals(lastIP);
    }

    @SuppressWarnings("deprecation")
    private boolean setIP(final DownloadLink link, final Account account) throws PluginException {
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
                DataFileCom.lastIP.set(lastIP);
                getPluginConfig().setProperty(PROPERTY_LASTIP, lastIP);
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
    }

    private long getPluginSavedLastDownloadTimestamp() {
        long lastdownload = 0;
        synchronized (blockedIPsMap) {
            for (Entry<String, Long> ipentry : blockedIPsMap.entrySet()) {
                final String ip = ipentry.getKey();
                final long timestamp = ipentry.getValue();
                if (System.currentTimeMillis() - timestamp >= FREE_RECONNECTWAIT) {
                    /* Remove old entries */
                    blockedIPsMap.remove(ip);
                }
                if (ip.equals(currentIP.get())) {
                    lastdownload = timestamp;
                }
            }
        }
        return lastdownload;
    }

    private static long getLongProperty(final Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    private static final String  ENABLE_FREE_STORED_WAITTIME           = "ENABLE_FREE_STORED_WAITTIME";
    private static final boolean defaultENABLE_fREE_PARALLEL_DOWNLOADS = false;

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), ENABLE_FREE_STORED_WAITTIME, JDL.L("plugins.hoster.datafilecom.enableStoredWaittimeForFreeModes", "Enable saved waittime in between free downloads?\r\nHelps to start more simultaneous downloads with less waittime/captchas in between.")).setDefaultValue(defaultENABLE_fREE_PARALLEL_DOWNLOADS));
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        }
        if (result != null) {
            result = result.replaceAll("\\\\/", "/");
        }
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return getJson(br.toString(), key);
    }

    /**
     * Tries to return value of key from JSon response, from provided Browser.
     *
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return getJson(ibr.toString(), key);
    }

    private static AtomicInteger maxPrem = new AtomicInteger(1);
    private static AtomicInteger maxFree = new AtomicInteger(1);

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlSlot
     *            (+1|-1)
     * @author raztoki
     * */
    private void controlSlot(final int num, final Account account) {
        synchronized (CTRLLOCK) {
            if (account == null) {
                int was = maxFree.get();
                maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
                logger.info("maxFree was = " + was + " && maxFree now = " + maxFree.get());
            } else {
                int was = maxPrem.get();
                maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), account.getIntegerProperty("totalMaxSim", 20)));
                logger.info("maxPrem was = " + was + " && maxPrem now = " + maxPrem.get());
            }
        }
    }

    private boolean isJava7nJDStable() {
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