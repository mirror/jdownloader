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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vipfile.in" }, urls = { "https?://(www\\.)?vipfile\\.in/[A-Za-z0-9]+" }, flags = { 2 })
public class VipfileIn extends PluginForHost {

    public VipfileIn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mainpage + "/upgrade." + type);
    }

    // For sites which use this script: http://www.yetishare.com/
    // YetiShareBasic Version 0.4.3-psp
    // mods:
    // limit-info: premium tested, seems like they only have premium accounts
    // protocol: no https
    // captchatype: null recaptcha
    // other:

    @Override
    public String getAGBLink() {
        return mainpage + "/terms." + type;
    }

    /* Basic constants */
    private final String         mainpage                                     = "http://vipfile.in";
    private final String         domains                                      = "(vipfile\\.in)";
    private final String         type                                         = "html";
    private static final int     wait_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT = 10;
    private static final int     additional_WAIT_SECONDS                      = 3;
    private static final int     directlinkfound_WAIT_SECONDS                 = 10;
    private static final boolean supportshttps                                = false;
    private static final boolean supportshttps_FORCED                         = false;
    /* In case there is no information when accessing the main link */
    private static final boolean available_CHECK_OVER_INFO_PAGE               = false;
    /* Known errors */
    private static final String  url_ERROR_SIMULTANDLSLIMIT                   = "e=You+have+reached+the+maximum+concurrent+downloads";
    private static final String  url_ERROR_SERVER                             = "e=Error%3A+Could+not+open+file+for+reading.";
    private static final String  url_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT       = "e=You+must+wait+";
    private static final String  url_ERROR_PREMIUMONLY                        = "e=You+must+register+for+a+premium+account+to+download+files+of+this+size";
    /* Texts for the known errors */
    private static final String  errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT = "You must wait between downloads!";
    private static final String  errortext_ERROR_SERVER                       = "Server error";
    private static final String  errortext_ERROR_PREMIUMONLY                  = "This file can only be downloaded by premium (or registered) users";
    private static final String  errortext_ERROR_SIMULTANDLSLIMIT             = "Max. simultan downloads limit reached, wait to start more downloads from this host";

    /* Connection stuff */
    private static final boolean free_RESUME                                  = true;
    private static final int     free_MAXCHUNKS                               = 0;
    private static final int     free_MAXDOWNLOADS                            = 20;
    private static final boolean account_FREE_RESUME                          = true;
    private static final int     account_FREE_MAXCHUNKS                       = 0;
    private static final int     account_FREE_MAXDOWNLOADS                    = 20;
    private static final boolean account_PREMIUM_RESUME                       = true;
    private static final int     account_PREMIUM_MAXCHUNKS                    = 0;
    private static final int     account_PREMIUM_MAXDOWNLOADS                 = 20;

    private static AtomicInteger MAXPREM                                      = new AtomicInteger(1);

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing or forced protocol */
        if (!supportshttps) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://"));
        } else if (supportshttps && supportshttps_FORCED) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("http://", "https://"));
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename;
        String filesize;
        if (available_CHECK_OVER_INFO_PAGE) {
            br.getPage(link.getDownloadURL() + "~i");
            if (!br.getURL().contains("~i")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("Filename:[\t\n\r ]+</td>[\t\n\r ]+<td(?: class=\"responsiveInfoTable\")?>([^<>\"]*?)<").getMatch(0);
            if (filename == null || inValidate(Encoding.htmlDecode(filename).trim()) || Encoding.htmlDecode(filename).trim().equals("  ")) {
                /* Filename might not be available here either */
                filename = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            }
            filesize = br.getRegex("Filesize:[\t\n\r ]+</td>[\t\n\r ]+<td(?: class=\"responsiveInfoTable\")?>([^<>\"]*?)<").getMatch(0);
        } else {
            br.getPage(link.getDownloadURL());
            if (br.getURL().contains(url_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT)) {
                link.setName(getFID(link));
                link.getLinkStatus().setStatusText(errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT);
                return AvailableStatus.TRUE;
            } else if (br.getURL().contains(url_ERROR_SERVER)) {
                link.setName(getFID(link));
                link.getLinkStatus().setStatusText(errortext_ERROR_SERVER);
                return AvailableStatus.TRUE;
            } else if (br.getURL().contains(url_ERROR_PREMIUMONLY) || br.getURL().contains("/upgrade.html")) {
                link.getLinkStatus().setStatusText(errortext_ERROR_PREMIUMONLY);
                return AvailableStatus.TRUE;
            }
            handleErrors();
            if (br.getURL().contains("/error." + type) || br.getURL().contains("/index." + type) || (!br.containsHTML("class=\"downloadPageTable(V2)?\"") && !br.containsHTML("class=\"download\\-timer\""))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex fInfo = br.getRegex("<strong>([^<>\"]*?) \\((\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))\\)<");
            filename = fInfo.getMatch(0);
            filesize = fInfo.getMatch(1);
            if (filename == null || filesize == null) {
                /* Get piece of the page which usually contains filename- and size */
                final String page_piece = br.getRegex("(<div class=\"contentPageWrapper\">.*?class=\"link btn\\-free\")").getMatch(0);
                if (page_piece != null) {
                    final String endings = jd.plugins.hoster.DirectHTTP.ENDINGS;
                    if (filename == null) {
                        filename = new Regex(page_piece, "([^<>/\r\n\t:\\?\"]+" + endings + "[^<>/\r\n\t:\\?\"]*)").getMatch(0);
                    }
                    if (filesize == null) {
                        filesize = new Regex(page_piece, "(\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))").getMatch(0);
                    }
                }
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

    public void doFree(final DownloadLink downloadLink, final boolean resume, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String continue_link = null;
        boolean captcha = false;
        try {
            continue_link = checkDirectLink(downloadLink, directlinkproperty);
            if (continue_link != null) {
                /*
                 * Let the server 'calm down' otherwise it will thing that we tried to open two connections as we checked the directlink
                 * before and return an error.
                 */
                sleep(directlinkfound_WAIT_SECONDS * 1000l, downloadLink);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, continue_link, resume, maxchunks);
            } else {
                if (available_CHECK_OVER_INFO_PAGE) {
                    br.getPage(downloadLink.getDownloadURL());
                }
                handleErrors();
                /* Handle up to 3 pre-download pages before the (eventually existing) captcha */
                for (int i = 1; i <= 5; i++) {
                    logger.info("Handling pre-download page #" + i);
                    continue_link = getContinueLink();
                    if (continue_link == null) {
                        logger.info("No continue_link available, stepping out of pre-download loop");
                        break;
                    } else {
                        logger.info("Found continue_link, continuing...");
                    }
                    final String waittime = br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
                    if (waittime != null) {
                        logger.info("Found waittime, waiting (seconds): " + waittime + " + " + additional_WAIT_SECONDS + " additional seconds");
                        sleep((Integer.parseInt(waittime) + additional_WAIT_SECONDS) * 1001l, downloadLink);
                    } else {
                        logger.info("Current pre-download page has no waittime");
                    }
                    final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
                    if (rcID == null) {
                        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, continue_link, resume, maxchunks);
                    } else {
                        captcha = true;
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                        rc.setId(rcID);
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        String c = getCaptchaCode(cf, downloadLink);
                        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, continue_link, "submit=continue&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, resume, maxchunks);
                    }
                    if (dl.getConnection().isContentDisposition()) {
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
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                handleErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final BrowserException e) {
            downloadLink.setProperty(directlinkproperty, Property.NULL);
            if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 429) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 'Too many requests'", 2 * 60 * 1000l);
            }
            throw e;
        }
        continue_link = dl.getConnection().getURL().toString();
        downloadLink.setProperty(directlinkproperty, continue_link);
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
        } else if (br.getURL().contains(url_ERROR_PREMIUMONLY) || br.getURL().contains("/upgrade.html")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, errortext_ERROR_PREMIUMONLY);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
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

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
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
                br.setFollowRedirects(true);
                br.getPage(this.getProtocol() + "www." + this.getHost() + "/login." + type);
                final String loginstart = new Regex(br.getURL(), "(https?://(www\\.)?)").getMatch(0);
                final boolean oldLogin = true;
                final String lang = System.getProperty("user.language");
                if (oldLogin) {
                    br.postPage("http://vipfile.in/login.html", "submit=Login&submitme=1&loginUsername=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML(">Your username and password are invalid<") || !br.containsHTML("/logout\\.html\">logout \\(")) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                } else {
                    final String loginpostpage = loginstart + this.getHost() + "/ajax/_account_login.ajax.php";
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    br.postPage(loginpostpage, "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (!br.containsHTML("\"login_status\":\"success\"")) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                if (!br.getURL().contains("/upgrade")) {
                    br.getPage(loginstart + this.getHost() + "/upgrade." + type);
                }
                final String type = br.getRegex("Account Type:[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]*?)<").getMatch(0);
                if (type == null || !type.contains("Paid")) {
                    account.setProperty("free", true);
                } else {
                    account.setProperty("free", false);
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(mainpage);
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        MAXPREM.set(1);
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (account.getBooleanProperty("free", false)) {
            try {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(account_FREE_MAXDOWNLOADS);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            MAXPREM.set(account_FREE_MAXDOWNLOADS);
            ai.setStatus("Registered (free) user");
        } else {
            br.getPage("http://" + this.getHost() + "/index." + type);
            /* If the premium account is expired we'll simply accept it as a free account. */
            final String expire = br.getRegex("<p>Account is preimum until : ([^<>\"]*?)</p>").getMatch(0);
            if (expire == null) {
                account.setValid(false);
                return ai;
            }
            long expire_milliseconds = 0;
            expire_milliseconds = TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            if ((expire_milliseconds - System.currentTimeMillis()) <= 0) {
                account.setProperty("free", true);
                try {
                    account.setType(AccountType.FREE);
                    account.setMaxSimultanDownloads(account_FREE_MAXDOWNLOADS);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                }
                MAXPREM.set(account_FREE_MAXDOWNLOADS);
                ai.setStatus("Registered (free) user");
            } else {
                ai.setValidUntil(expire_milliseconds);
                try {
                    account.setType(AccountType.PREMIUM);
                    account.setMaxSimultanDownloads(account_PREMIUM_MAXDOWNLOADS);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                }
                MAXPREM.set(account_PREMIUM_MAXDOWNLOADS);
                ai.setStatus("Premium User");
            }
        }
        account.setValid(true);
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (account.getBooleanProperty("free", false)) {
            if (!available_CHECK_OVER_INFO_PAGE) {
                br.getPage(link.getDownloadURL());
            }
            doFree(link, account_FREE_RESUME, account_FREE_MAXCHUNKS, "free_acc_directlink");
        } else {
            String dllink = link.getDownloadURL();
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, account_PREMIUM_RESUME, account_PREMIUM_MAXCHUNKS);
            if (!dl.getConnection().isContentDisposition()) {
                logger.warning("The final dllink seems not to be a file, checking for errors...");
                br.followConnection();
                handleErrors();
                logger.info("Found no errors, let's see if we can find the dllink now...");
                dllink = this.getDllink();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, account_PREMIUM_RESUME, account_PREMIUM_MAXCHUNKS);
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
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return MAXPREM.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}