//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloudlocker.biz" }, urls = { "https?://(?:www\\.)?cloudlocker\\.biz/[A-Za-z0-9]+" })
public class CloudLockerBiz extends antiDDoSForHost {

    public CloudLockerBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mainpage + "/upgrade." + type);
    }

    /**
     * For sites which use this script: http://www.yetishare.com/<br />
     * YetiShareBasic Version 0.7.2-psp<br />
     * mods:<br />
     * limit-info: premium untested, set FREE limits<br />
     * protocol: no https<br />
     * captchatype: reCaptchaV2<br />
     * other:<br />
     */

    @Override
    public String getAGBLink() {
        return mainpage + "/terms." + type;
    }

    /* Basic constants */
    private final String         mainpage                                     = "http://cloudlocker.biz";
    private final String         domains                                      = "(cloudlocker\\.biz)";
    private final String         type                                         = "html";
    private static final int     wait_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT = 10;
    private static final int     additional_WAIT_SECONDS                      = 3;
    private static final int     directlinkfound_WAIT_SECONDS                 = 10;
    private static final boolean supportshttps                                = false;
    private static final boolean supportshttps_FORCED                         = false;
    /* In case there is no information when accessing the main link */
    private static final boolean available_CHECK_OVER_INFO_PAGE               = true;
    private static final boolean useOldLoginMethod                            = false;
    /* Known errors */
    private static final String  url_ERROR_SIMULTANDLSLIMIT                   = "e=You+have+reached+the+maximum+concurrent+downloads";
    private static final String  url_ERROR_SERVER                             = "e=Error%3A+Could+not+open+file+for+reading.";
    private static final String  url_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT       = "e=You+must+wait+";
    /* E.g. You+must+register+for+a+premium+account+to+download+files+of+this+size */
    /* E.g. You+must+register+for+a+premium+account+to+see+or+download+files.+Please+use+the+links+above+to+register+or+login. */
    private static final String  url_ERROR_PREMIUMONLY                        = "e=You+must+register+for+a+premium+account+to";
    /* Texts for the known errors */
    private static final String  errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT = "You must wait between downloads!";
    private static final String  errortext_ERROR_SERVER                       = "Server error";
    private static final String  errortext_ERROR_PREMIUMONLY                  = "This file can only be downloaded by premium (or registered) users";
    private static final String  errortext_ERROR_SIMULTANDLSLIMIT             = "Max. simultan downloads limit reached, wait to start more downloads from this host";

    /* Connection stuff */
    private static final boolean free_RESUME                                  = true;
    private static final int     free_MAXCHUNKS                               = 1;
    private static final int     free_MAXDOWNLOADS                            = 1;
    private static final boolean account_FREE_RESUME                          = true;
    private static final int     account_FREE_MAXCHUNKS                       = 1;
    private static final int     account_FREE_MAXDOWNLOADS                    = 1;
    private static final boolean account_PREMIUM_RESUME                       = true;
    private static final int     account_PREMIUM_MAXCHUNKS                    = 1;
    private static final int     account_PREMIUM_MAXDOWNLOADS                 = 1;

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing or forced protocol */
        if (!supportshttps) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://"));
        } else if (supportshttps && supportshttps_FORCED) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("http://", "https://"));
        }
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.addAllowedResponseCodes(429);
        }
        return prepBr;
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        link.setLinkID(fid);
        String filename;
        String filesize;
        if (available_CHECK_OVER_INFO_PAGE) {
            getPage(link.getDownloadURL() + "~i");
            if (!br.getURL().contains("~i") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] tableData = this.br.getRegex("class=\"responsiveInfoTable\">([^<>\"/]*?)<").getColumn(0);
            /* Sometimes we get crippled results with the 2nd RegEx so use this one first */
            filename = this.br.getRegex("data\\-animation\\-delay=\"\\d+\">Information about ([^<>\"]*?)</div>").getMatch(0);
            if (filename == null) {
                filename = this.br.getRegex("(?:Filename|Dateiname|اسم الملف):[\t\n\r ]*?</td>[\t\n\r ]*?<td(?: class=\"responsiveInfoTable\")?>([^<>\"]*?)<").getMatch(0);
            }
            filesize = br.getRegex("(?:Filesize|Dateigröße|حجم الملف):[\t\n\r ]*?</td>[\t\n\r ]*?<td(?: class=\"responsiveInfoTable\")?>([^<>\"]*?)<").getMatch(0);
            try {
                /* Language-independant attempt ... */
                if (filename == null) {
                    filename = tableData[0];
                }
                if (filesize == null) {
                    filesize = tableData[1];
                }
            } catch (final Throwable e) {
            }
            if (filename == null || inValidate(Encoding.htmlDecode(filename).trim()) || Encoding.htmlDecode(filename).trim().equals("  ")) {
                /* Filename might not be available here either */
                filename = fid;
            }
        } else {
            getPage(link.getDownloadURL());
            if (br.getURL().contains(url_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT)) {
                link.setName(getFID(link));
                link.getLinkStatus().setStatusText(errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT);
                return AvailableStatus.TRUE;
            } else if (br.getURL().contains(url_ERROR_SERVER)) {
                link.setName(getFID(link));
                link.getLinkStatus().setStatusText(errortext_ERROR_SERVER);
                return AvailableStatus.TRUE;
            } else if (br.getURL().contains(url_ERROR_PREMIUMONLY)) {
                link.getLinkStatus().setStatusText(errortext_ERROR_PREMIUMONLY);
                return AvailableStatus.TRUE;
            }
            handleErrors();
            if (br.getURL().contains("/error." + type) || br.getURL().contains("/index." + type) || (!br.containsHTML("class=\"downloadPageTable(V2)?\"") && !br.containsHTML("class=\"download\\-timer\"")) || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex fInfo = br.getRegex("<strong>([^<>\"]*?) \\((\\d+(?:,\\d+)?(?:\\.\\d+)? (?:KB|MB|GB))\\)<");
            filename = fInfo.getMatch(0);
            filesize = fInfo.getMatch(1);
            if (filesize == null) {
                filesize = br.getRegex("(\\d+(?:,\\d+)?(\\.\\d+)? (?:KB|MB|GB))").getMatch(0);
            }
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename).trim());
        link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize.replace(",", "")).trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, free_RESUME, free_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink link, final boolean resume, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        boolean skipWaittime = false;
        String continue_link = null;
        boolean captcha = false;
        boolean success = false;
        final long timeBeforeDirectlinkCheck = System.currentTimeMillis();
        long timeBeforeCaptchaInput;
        continue_link = checkDirectLink(link, directlinkproperty);
        if (continue_link != null) {
            /*
             * Let the server 'calm down' (if it was slow before) otherwise it will thing that we tried to open two connections as we
             * checked the directlink before and return an error.
             */
            if ((System.currentTimeMillis() - timeBeforeDirectlinkCheck) > 1500) {
                sleep(directlinkfound_WAIT_SECONDS * 1000l, link);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, continue_link, resume, maxchunks);
        } else {
            if (available_CHECK_OVER_INFO_PAGE) {
                getPage(link.getDownloadURL());
            }
            handleErrors();
            /* Passwords are usually before waittime. */
            handlePassword(link);
            /* Handle up to 3 pre-download pages before the (eventually existing) captcha */;
            for (int i = 1; i <= 5; i++) {
                logger.info("Handling pre-download page #" + i);
                timeBeforeCaptchaInput = System.currentTimeMillis();
                continue_link = getContinueLink();
                if (i == 1 && continue_link == null) {
                    logger.info("No continue_link available, plugin broken");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (continue_link == null) {
                    logger.info("No continue_link available, stepping out of pre-download loop");
                    break;
                } else {
                    logger.info("Found continue_link, continuing...");
                }
                final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
                if (isDownloadlink(continue_link)) {
                    /*
                     * If we already found a downloadlink let's try to download it because html can still contain captcha html --> We don't
                     * need a captcha in this case for sure! E.g. host '3rbup.com'.
                     */
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, continue_link, resume, maxchunks);
                } else if (br.containsHTML("data\\-sitekey=")) {
                    captcha = true;
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    success = true;
                    if (!skipWaittime) {
                        waitTime(link, timeBeforeCaptchaInput);
                    }
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, continue_link, "submit=Submit&submitted=1&d=1&capcode=false&g-recaptcha-response=" + recaptchaV2Response, resume, maxchunks);
                } else if (rcID != null) {
                    captcha = true;
                    success = false;
                    final Recaptcha rc = new Recaptcha(this.br, this);
                    rc.setId(rcID);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, link);
                    if (!skipWaittime) {
                        waitTime(link, timeBeforeCaptchaInput);
                    }
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, continue_link, "submit=continue&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, resume, maxchunks);
                } else if (br.containsHTML("solvemedia\\.com/papi/")) {
                    captcha = true;
                    success = false;
                    logger.info("Detected captcha method \"solvemedia\" for this host");
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    if (br.containsHTML("api\\-secure\\.solvemedia\\.com/")) {
                        sm.setSecure(true);
                    }
                    File cf = null;
                    try {
                        cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    } catch (final Exception e) {
                        if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                        }
                        throw e;
                    }
                    final String code = getCaptchaCode("solvemedia", cf, link);
                    final String chid = sm.getChallenge(code);
                    if (!skipWaittime) {
                        waitTime(link, timeBeforeCaptchaInput);
                    }
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, continue_link, "submit=continue&submitted=1&g-recaptcha-response=1&d=1&adcopy_challenge=" + Encoding.urlEncode(chid) + "&adcopy_response=" + Encoding.urlEncode(code), resume, maxchunks);
                } else {
                    success = true;
                    if (!skipWaittime) {
                        waitTime(link, timeBeforeCaptchaInput);
                    }
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, continue_link, resume, maxchunks);
                }
                checkResponseCodeErrors(dl.getConnection());
                if (dl.getConnection().isContentDisposition()) {
                    success = true;
                    break;
                }
                br.followConnection();
                handleErrors();
                if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    logger.info("Wrong captcha");
                    continue;
                }
            }
        }
        checkResponseCodeErrors(dl.getConnection());
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (captcha && !success) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        continue_link = dl.getConnection().getURL().toString();
        link.setProperty(directlinkproperty, continue_link);
        dl.startDownload();
    }

    private String getContinueLink() {
        String continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        if (continue_link == null) {
            continue_link = br.getRegex("class=\\'btn btn\\-free\\' href=\\'(https?://[^<>\"]*?)\\'>").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("(?:\"|\\')(https?://(www\\.)?" + domains + "/[^<>\"]*?pt=[^<>\"]*?)(?:\"|\\')").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = getDllink();
        }
        return continue_link;
    }

    private String getDllink() {
        return br.getRegex("\"(https?://(www\\.)?(?:[A-Za-z0-9\\.]+\\.)?" + domains + "/[^<>\"\\?]*?\\?download_token=[A-Za-z0-9]+)\"").getMatch(0);
    }

    private boolean isDownloadlink(final String url) {
        final boolean isdownloadlink = url.contains("download_token=");
        return isdownloadlink;
    }

    private void handlePassword(final DownloadLink dl) throws Exception {
        if (br.getURL().contains("/file_password.html")) {
            logger.info("Current link is password protected");
            String passCode = dl.getStringProperty("pass", null);
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", dl);
                if (passCode == null || passCode.equals("")) {
                    logger.info("User has entered blank password, exiting handlePassword");
                    dl.setProperty("pass", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                }
                dl.setProperty("pass", passCode);
            }
            postPage(br.getURL(), "submit=access+file&submitme=1&file=" + this.getFID(dl) + "&filePassword=" + Encoding.urlEncode(passCode));
            if (br.getURL().contains("/file_password.html")) {
                logger.info("User entered incorrect password --> Retrying");
                dl.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            logger.info("User entered correct password --> Continuing");
        }
    }

    /** Handles pre download (pre-captcha) waittime. */
    private void waitTime(final DownloadLink downloadLink, final long timeBefore) throws PluginException {
        int wait = 0;
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /* Ticket Time */
        final String ttt = this.br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
        if (ttt != null) {
            wait = Integer.parseInt(ttt) + additional_WAIT_SECONDS;
        }
        wait -= passedTime;
        if (wait > 0) {
            sleep(wait * 1000l, downloadLink);
        }
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML("Error: Too many concurrent download requests")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
        } else if (br.getURL().contains(url_ERROR_SIMULTANDLSLIMIT)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errortext_ERROR_SIMULTANDLSLIMIT, 1 * 60 * 1000l);
        } else if (br.getURL().contains("error.php?e=Error%3A+Could+not+open+file+for+reading")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        } else if (br.getURL().contains(url_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT)) {
            final String wait_minutes = new Regex(br.getURL(), "wait\\+(\\d+)\\+minutes?").getMatch(0);
            if (wait_minutes != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT, Integer.parseInt(wait_minutes) * 60 * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT, wait_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT * 60 * 1001l);
        } else if (br.getURL().contains(url_ERROR_PREMIUMONLY)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, errortext_ERROR_PREMIUMONLY);
        } else if (br.getURL().contains("You+have+reached+the+maximum+permitted+downloads+in")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 3 * 60 * 60 * 1001l);
        } else if (br.toString().equals("unknown user")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Unknown user'", 30 * 60 * 1000l);
        }
        checkResponseCodeErrors(this.br.getHttpConnection());
    }

    /** Handles all kinds of error-responsecodes! */
    private void checkResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con == null) {
            return;
        }
        final long responsecode = con.getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
        } else if (responsecode == 429) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 429 connection limit reached, please contact our support!", 5 * 60 * 1000l);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            final Browser br2 = this.br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
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
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    private String getProtocol() {
        if ((this.br.getURL() != null && this.br.getURL().contains("https://")) || supportshttps_FORCED) {
            return "https://";
        } else {
            return "http://";
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_MAXDOWNLOADS;
    }

    private static final Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, boolean force) throws Exception {
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
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(mainpage, key, value);
                        }
                        return;
                    }
                }
                getPage(this.getProtocol() + this.getHost() + "/");
                final String lang = System.getProperty("user.language");
                final String loginstart = new Regex(br.getURL(), "(https?://(www\\.)?)").getMatch(0);
                if (useOldLoginMethod) {
                    postPage(this.getProtocol() + this.getHost() + "/login." + type, "submit=Login&submitme=1&loginUsername=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML(">Your username and password are invalid<") || !br.containsHTML("/logout\\.html\">logout \\(")) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                } else {
                    getPage(this.getProtocol() + this.getHost() + "/login." + type);
                    final String loginpostpage = loginstart + this.getHost() + "/ajax/_account_login.ajax.php";
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    postPage(loginpostpage, "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (!br.containsHTML("\"login_status\":\"success\"")) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                getPage(loginstart + this.getHost() + "/account_home." + type);
                if (!br.containsHTML("class=\"badge badge\\-success\">PAID USER</span>")) {
                    account.setProperty("free", true);
                } else {
                    account.setProperty("free", false);
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(mainpage));
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
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (account.getBooleanProperty("free", false)) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(account_FREE_MAXDOWNLOADS);
            /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        } else {
            getPage("http://" + this.getHost() + "/upgrade." + type);
            /* If the premium account is expired we'll simply accept it as a free account. */
            final String expire = br.getRegex("Reverts To Free Account:[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expire == null) {
                account.setValid(false);
                return ai;
            }
            long expire_milliseconds = 0;
            expire_milliseconds = TimeFormatter.getMilliSeconds(expire, "MM/dd/yyyy hh:mm:ss", Locale.ENGLISH);
            if ((expire_milliseconds - System.currentTimeMillis()) <= 0) {
                account.setProperty("free", true);
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(account_FREE_MAXDOWNLOADS);
                /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
                account.setConcurrentUsePossible(false);
                ai.setStatus("Free Account");
            } else {
                ai.setValidUntil(expire_milliseconds);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(account_PREMIUM_MAXDOWNLOADS);
                ai.setStatus("Premium Account");
            }
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (account.getBooleanProperty("free", false)) {
            if (!available_CHECK_OVER_INFO_PAGE) {
                getPage(link.getDownloadURL());
            }
            doFree(link, account_FREE_RESUME, account_FREE_MAXCHUNKS, "free_acc_directlink");
        } else {
            String dllink = link.getDownloadURL();
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, account_PREMIUM_RESUME, account_PREMIUM_MAXCHUNKS);
            checkResponseCodeErrors(dl.getConnection());
            if (!dl.getConnection().isContentDisposition()) {
                logger.warning("The final dllink seems not to be a file, checking for errors...");
                br.followConnection();
                handleErrors();
                logger.info("Found no errors, let's see if we can find the dllink now...");
                handlePassword(link);
                dllink = this.getDllink();
                if (dllink == null) {
                    handleErrors();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, account_PREMIUM_RESUME, account_PREMIUM_MAXCHUNKS);
                checkResponseCodeErrors(dl.getConnection());
            }
            if (!dl.getConnection().isContentDisposition()) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                handleErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }

}