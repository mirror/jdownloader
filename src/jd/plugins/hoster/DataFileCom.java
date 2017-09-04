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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
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
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datafile.com" }, urls = { "https?://(www\\.)?datafile\\.com/(d/[A-Za-z0-9]+(/[^<>\"/]+)?|download/.*?i=[A-Za-z0-9]+)" })
public class DataFileCom extends antiDDoSForHost {

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
    private static final boolean           FREE_RESUME               = false;
    private static final int               FREE_MAXCHUNKS            = 1;
    private static final boolean           ACCOUNT_PREMIUM_RESUME    = true;
    private static final int               ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private final String                   accessKey                 = "cddce1a5-a6dd-4300-9c08-eb70909de7c6";
    private final String                   PREMIUMONLY               = "(\"Sorry\\. Only premium users can download this file\"|>This file can be downloaded only by users with<br />Premium account\\!<|>You can download files up to)";
    private final boolean                  SKIPRECONNECTWAITTIME     = true;
    private final boolean                  SKIPWAITTIME              = true;
    private final String[]                 IPCHECK                   = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };
    private static final String            PROPERTY_LASTDOWNLOAD     = "datafilecom_lastdownload_timestamp";
    private final String                   PROPERTY_LASTIP           = "PROPERTY_LASTIP";
    private static AtomicReference<String> lastIP                    = new AtomicReference<String>();
    private static AtomicReference<String> currentIP                 = new AtomicReference<String>();
    private static HashMap<String, Long>   blockedIPsMap             = new HashMap<String, Long>();
    private static Object                  CTRLLOCK                  = new Object();
    private final Pattern                  IPREGEX                   = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static final long              FREE_RECONNECTWAIT        = 3610000L;
    private Account                        currAcc                   = null;

    @SuppressWarnings({ "deprecation" })
    public void correctDownloadLink(final DownloadLink link) {
        final String rewrite = new Regex(link.getDownloadURL(), "datafile\\.com/download/.*?i=([A-Za-z0-9]+)").getMatch(0);
        if (rewrite != null) {
            link.setUrlDownload("https://datafile.com/d/" + rewrite);
        }
        final String unneededPart = new Regex(link.getDownloadURL(), "datafile\\.com/d/[A-Za-z0-9]+(/[^<>\"/]+)").getMatch(0);
        if (unneededPart != null) {
            final String urlfilename = new Regex(unneededPart, "/([^<>\"/]+)").getMatch(0);
            link.setUrlDownload(link.getDownloadURL().replace(unneededPart, ""));
            link.setProperty("urlfilename", Encoding.htmlDecode(urlfilename.trim()));
        }
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
    }

    @Override
    protected long getStartIntervall(DownloadLink downloadLink, Account account) {
        if (account == null || account.getType() == AccountType.FREE) {
            return 5000l;
        }
        return 1000;
    }

    @Override
    protected Browser prepBrowser(Browser prepBr, String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");
            prepBr.setCookie(MAINPAGE, "lang", "en");
        }
        return prepBr;
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
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        br.setFollowRedirects(false);
        String filesize = null;
        // Limit reached -> Let's use their linkchecker to at least find the filesize and onlinestatus
        if (br.getURL().contains("code=7") || br.getURL().contains("code=9")) {
            final Browser br2 = br.cloneBrowser();
            postPage(br2, "https://www.datafile.com/linkchecker.html", "btn=&links=" + Encoding.urlEncode(link.getDownloadURL()));
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

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null, downloadLink);
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
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
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
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
            handleURLErrors(br.getURL());
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
            final Recaptcha rc = new Recaptcha(br, this);
            rc.setId(rcID);
            rc.load();
            for (int i = 1; i <= 5; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                if (!SKIPWAITTIME || i > 1) {
                    waitTime(timeBeforeCaptcha, downloadLink, wait);
                }
                // Validation phase, return token that need to be added to getFileDownloadLink call
                postPage("/files/ajax.html", "doaction=validateCaptcha&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&fileid=" + fid);
                final String token = PluginJSonUtils.getJson(br, "token");
                if (token == null || br.containsHTML("\"success\":0")) {
                    rc.reload();
                    continue;
                }
                postPage("/files/ajax.html", "doaction=getFileDownloadLink&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&fileid=" + fid + "&token=" + token);
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
            dllink = PluginJSonUtils.getJson(br, "link");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
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
            dl.startDownload();
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
        String dllink = downloadLink.getStringProperty(property, null);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable t) {
                }
            }
        }
        return dllink;
    }

    private static final String MAINPAGE = "https://datafile.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
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
                        return;
                    }
                }
                br.setFollowRedirects(true);
                postPage("https://www.datafile.com/login.html", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=0&remember_me=1&btn=");
                if (br._getURL().getPath().equals("/deny.html")) {
                    throw new AccountInvalidException("Can not login/access site from this IP address, You must whitelist the IP to your account, Check your email!");
                }
                if (!containsLoginCookie()) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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
        }
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        getPage(br, "/profile.html");
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
            account.setType(AccountType.FREE);
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(1);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMM dd, yyyy HH:mm", Locale.ENGLISH));
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(20);
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account, downloadLink);
        if (useAPI.get() && account.getType() == AccountType.PREMIUM) {
            /* API + premium */
            handlePremium_API(downloadLink, account);
            return;
        }
        /* Either disabled API or free account - free (account) download is not possible via API. */
        requestFileInformation(downloadLink);
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            getPage(br, downloadLink.getDownloadURL());
            // if the cached cookie expired, relogin.
            if (!containsLoginCookie()) {
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
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, downloadLink.getDownloadURL(), ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors();
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                handleGeneralErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(getServerFilename());
            dl.startDownload();
        }
    }

    private boolean containsLoginCookie() {
        final boolean logincookie = br.getCookie(MAINPAGE, "hash") != null || br.getCookie(MAINPAGE, "user") != null || br.getCookie(MAINPAGE, "shash") != null;
        return logincookie;
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
     */
    private void errorFreeTooManySimultanDownloads() throws PluginException {
        logger.info("You are downloading another file at this moment. Please wait for it to complete and then try again.");
        if (currAcc != null) {
            final AccountInfo ac = new AccountInfo();
            ac.setTrafficLeft(0);
            currAcc.setAccountInfo(ac);
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
     */
    private void errorDailyDownloadlimitReached() throws PluginException {
        if (currAcc == null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily downloadlimit reached", FREE_RECONNECTWAIT);
        } else if (currAcc != null && currAcc.getType() == AccountType.FREE) {
            // reached daily download quota
            logger.info("You've reached daily download quota for " + currAcc.getUser() + " account");
            logger.info("Free account: Daily downloadlimit reached");
            final AccountInfo ac = new AccountInfo();
            ac.setTrafficLeft(0);
            currAcc.setAccountInfo(ac);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Daily downloadlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else {
            logger.info("Premium account: Daily downloadlimit reached");
            final AccountInfo ac = new AccountInfo();
            ac.setTrafficLeft(0);
            currAcc.setAccountInfo(ac);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Daily downloadlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
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
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    private String getIP() throws PluginException {
        final Browser ip = new Browser();
        String currentIP = null;
        final ArrayList<String> checkIP = new ArrayList<String>(Arrays.asList(this.IPCHECK));
        Collections.shuffle(checkIP);
        for (final String ipServer : checkIP) {
            if (currentIP == null) {
                try {
                    getPage(ip, ipServer);
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

    private static final String  ENABLE_FREE_STORED_WAITTIME           = "ENABLE_FREE_STORED_WAITTIME";
    private static final boolean defaultENABLE_fREE_PARALLEL_DOWNLOADS = false;

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), ENABLE_FREE_STORED_WAITTIME, JDL.L("plugins.hoster.datafilecom.enableStoredWaittimeForFreeModes", "Enable saved waittime in between free downloads?\r\nHelps to start more simultaneous downloads with less waittime/captchas in between.")).setDefaultValue(defaultENABLE_fREE_PARALLEL_DOWNLOADS));
    }

    // decrypter wrapper
    public void getPage(final String page) throws Exception {
        super.getPage(br, page);
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        if (useAPI.get()) {
            return checkLinks_API(urls, null);
        }
        return false;
    }

    // api stuff

    private static AtomicBoolean useAPI = new AtomicBoolean(true);
    private final String         apiURL = "https://api.datafile.com";

    private synchronized void getPage(final Browser ibr, final String url, final Account account) throws Exception {
        if (account != null) {
            String apiToken = getApiToken(account);
            getPage(ibr, url + (url.matches("(" + apiURL + ")?/[a-zA-Z0-9]+/[a-zA-Z0-9]+\\?[a-zA-Z0-9]+.+") ? "&" : "?") + "token=" + apiToken);
            if (sessionTokenInValid(account, ibr)) {
                apiToken = getApiToken(account);
                if (apiToken != null) {
                    // can't sessionKeyInValid because getApiKey/loginKey return String, and loginKey uses a new Browser.
                    getPage(ibr, url + (url.matches("(" + apiURL + ")?/[a-zA-Z0-9]+/[a-zA-Z0-9]+\\?[a-zA-Z0-9]+.+") ? "&" : "?") + "token=" + apiToken);
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
            getPage(ibr, url);
        }
        // error handling for generic errors which could occur at any point in time!
        if (("718".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "code")))) {
            // 718 ERR_API_IP_SUSPENDED The IP Address initiating the request has been suspended
            throw new PluginException(LinkStatus.ERROR_FATAL, "\r\n" + errorMsg(ibr));
        }
    }

    private String errorMsg(final Browser ibr) {
        final String message = PluginJSonUtils.getJson(ibr, "message");
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
        final String code = PluginJSonUtils.getJson(ibr, "code");
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

    private boolean checkLinks_API(final DownloadLink[] urls, Account account) {
        if (account == null) {
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
            Account n = null;
            if (accounts != null && accounts.size() != 0) {
                Iterator<Account> it = accounts.iterator();
                while (it.hasNext()) {
                    n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        try {
                            if (getApiToken(n) != null) {
                                account = n;
                                break;
                            } else {
                                n = null;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        // can't link check without token, token is bound to account
        if (account == null) {
            return false;
        }
        try {
            final Browser br = new Browser();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                final StringBuilder sb = new StringBuilder();
                links.clear();
                while (true) {
                    // currently only do one at a time! suckie dollar
                    if (links.size() == 1 || index == urls.length) {
                        break;
                    }
                    links.add(urls[index++]);
                }
                sb.delete(0, sb.capacity());
                sb.append("file=");
                for (final DownloadLink dl : links) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                }
                getPage(br, apiURL + "/files/check?" + sb, account);
                for (final DownloadLink dl : links) {
                    final String filter = PluginJSonUtils.getJsonNested(br, "fileinfo");
                    if (filter == null) {
                        return false;
                    }
                    final String status = PluginJSonUtils.getJson(filter, "status");
                    if (!"online".equalsIgnoreCase(status)) {
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    final String name = PluginJSonUtils.getJson(filter, "filename");
                    final String size = PluginJSonUtils.getJson(filter, "filesize");
                    final String md5 = PluginJSonUtils.getJson(filter, "hash");
                    final String prem = PluginJSonUtils.getJson(filter, "is_premium_only");
                    final String pass = PluginJSonUtils.getJson(filter, "password");
                    if (name != null) {
                        dl.setName(name);
                    }
                    if (size != null) {
                        dl.setVerifiedFileSize(Long.parseLong(size));
                    }
                    if (md5 != null) {
                        dl.setMD5Hash(md5);
                    }
                    if (prem != null) {
                        dl.setProperty("premiumRequired", Boolean.parseBoolean(prem));
                    }
                    if (pass != null) {
                        dl.setProperty("passwordRequired", Boolean.parseBoolean(pass));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    private void handlePremium_API(final DownloadLink downloadLink, final Account account) throws Exception {
        // No API method for linkchecking, but can done based on this request response!
        getPage(br, apiURL + "/files/download?file=" + Encoding.urlEncode(downloadLink.getDownloadURL()), account);
        final String ddlink = PluginJSonUtils.getJson(br, "download_url");
        if (ddlink == null) {
            final String code = PluginJSonUtils.getJson(br, "code");
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
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, ddlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors();
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            handleGeneralErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getServerFilename());
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 402) {
            /* 402 Payment Required happens sometimes for unknown reason... */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 402", 10 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 30 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        }
    }

    private String loginToken(final Account account) throws Exception {
        final Browser nbr = new Browser();
        getPage(nbr, apiURL + "/users/auth?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&accesskey=" + accessKey);
        final String apiToken = PluginJSonUtils.getJson(nbr, "token");
        final String code = PluginJSonUtils.getJson(nbr, "code");
        if (apiToken != null) {
            account.setProperty("apiToken", apiToken);
            // all is good! lets update account info whilst we are at it. It's all here!
            AccountInfo ai = new AccountInfo();
            final String traffic_left = PluginJSonUtils.getJson(nbr, "traffic_left");
            final String primium_till = PluginJSonUtils.getJson(nbr, "premium_till");
            final String space_left = PluginJSonUtils.getJson(nbr, "space_left");
            if (primium_till != null) {
                // premium_till - time when user premium had expired, unix_timestamp, int (0 -no premium access)
                ai.setValidUntil(Long.parseLong(primium_till + "000"));
                if (!"0".equalsIgnoreCase(primium_till) || !ai.isExpired()) {
                    account.setType(AccountType.PREMIUM);
                    account.setConcurrentUsePossible(true);
                    account.setMaxSimultanDownloads(20);
                } else {
                    account.setType(AccountType.FREE);
                    account.setConcurrentUsePossible(true);
                    account.setMaxSimultanDownloads(1);
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
        } else if ("400".equalsIgnoreCase(code)) {
            // {"code":403,"message":"Your IP[123.123.123.123] is not allowed to access. Try adding it to whitelist."}
            throw new AccountInvalidException("Can not login/access site from this IP address, You must whitelist the IP to your account");
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

}