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
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bigfile.to" }, urls = { "https?://(?:www\\.)?(uploadable\\.ch|bigfile\\.to)/file/[A-Za-z0-9]+" })
public class BigfileTo extends PluginForHost {

    public BigfileTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(baseDomain + "/extend.php");
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return baseDomain + "/terms.php";
    }

    private static final long   FREE_SIZELIMIT          = 2 * 1073741824l;
    private static final String PREMIUM_UNLIMITEDCHUNKS = "PREMIUM_UNLIMITEDCHUNKS";
    /* Last updated: 2016-02-22 */
    private static final String recaptchaid             = "6LfZ0RETAAAAAOjhYT7V9ukeCT3wWccw98uc50vu";
    private static final String baseDomain              = "https://www.bigfile.to";

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        /* Forced https */
        link.setUrlDownload(link.getDownloadURL().replaceFirst("https?://(?:www\\.)?uploadable\\.ch/", baseDomain + "/"));
    }

    @Override
    public String rewriteHost(final String host) {
        final String currentHost = getHost();
        if ("bigfile.to".equals(currentHost)) {
            if (host == null || "uploadable.ch".equals(host)) {
                return "bigfile.to";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            prepBr(br);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("urls=");
                for (final DownloadLink dl : links) {
                    sb.append(dl.getDownloadURL());
                    sb.append("%0A");
                }
                br.postPage(baseDomain + "/check.php", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = new Regex(dllink.getDownloadURL(), "/file/([A-Za-z0-9]+)$").getMatch(0);
                    final String linkinfo = br.getRegex("href=\"\">(https?://(www\\.)?(?:uploadable\\.ch|bigfile\\.to)/file/" + fid + "</a></div>.*?)</li>").getMatch(0);
                    if (linkinfo == null) {
                        logger.warning("Mass-Linkchecker broken");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (linkinfo.contains(">Not Available<")) {
                        dllink.setAvailable(false);
                    } else {
                        final String name = new Regex(linkinfo, "class=\"col2\">([^<>\"]*?)</div>").getMatch(0);
                        final String size = new Regex(linkinfo, "class=\"col3\">([^<>\"]*?)</div>").getMatch(0);
                        dllink.setAvailable(true);
                        dllink.setName(Encoding.htmlDecode(name.trim()));
                        dllink.setDownloadSize(SizeFormatter.getSize(size));
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

    private static final AtomicReference<String> agent = new AtomicReference<String>(null);

    private Browser prepBr(final Browser br) {
        if (agent.get() == null) {
            agent.set(UserAgents.stringUserAgent(BrowserName.Chrome));
        }
        br.getHeaders().put("User-Agent", agent.get());
        return br;
    }

    /** Don't use mass-linkchecker here as it may return wrong/outdated information. */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBr(this.br);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not available<|>This file is no longer available.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        handleErrorsWebsite();
        final String filename = br.getRegex("id=\"file_name\" title=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("class=\"filename_normal\">\\(([^<>\"]*?)\\)</span>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        final long fsize = SizeFormatter.getSize(filesize);
        link.setDownloadSize(fsize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        br.setFollowRedirects(false);
        final String directlinkproperty = "directlink";
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            String reCaptchaPublicKey = br.getRegex("var reCAPTCHA_publickey=\\'([^<>\"\\']+)\\';").getMatch(0);
            if (reCaptchaPublicKey == null) {
                /* Fallback to our statically stored recaptchaid */
                reCaptchaPublicKey = recaptchaid;
            }
            final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            final String postLink = br.getURL();
            {
                final Browser json = br.cloneBrowser();
                json.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                json.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                json.getPage("/now.php");
            }
            {
                final Browser json = br.cloneBrowser();
                json.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                json.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                json.postPage(postLink, "downloadLink=wait");
                int wait = 90;
                final String waittime = json.getRegex("\"waitTime\":(\\d+)").getMatch(0);
                if (waittime != null) {
                    wait = Integer.parseInt(waittime);
                }

                /* 2017-03-21: Added 5 seconds extra waittime to prevent possible issues due to too short waittime. */
                sleep((wait + 5) * 1001l, downloadLink);
            }
            {
                final Browser json = br.cloneBrowser();
                json.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                json.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                json.postPage(postLink, "checkDownload=check");
                if (json.containsHTML("\"fail\":\"timeLimit\"")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 20 * 60 * 1001l);
                }
            }
            final Recaptcha rc = new Recaptcha(br, this);
            rc.setId(reCaptchaPublicKey);
            rc.load();
            for (int i = 1; i <= 5; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                {
                    final Browser json = br.cloneBrowser();
                    json.getHeaders().put("Accept", "*/*");
                    json.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    json.postPage("/checkReCaptcha.php", "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&recaptcha_shortencode_field=" + fid);
                    if (json.containsHTML("\"success\":0") || json.toString().trim().equals("[]")) {
                        if (i + 1 == 5) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        rc.reload();
                        continue;
                    }
                }
                break;
            }
            {
                final Browser json = br.cloneBrowser();
                json.getHeaders().put("Accept", "*/*");
                json.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                json.postPage("/file/" + fid, "downloadLink=show");
                if ("fail".equals(json.toString())) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                }
            }
            br.postPage("/file/" + fid, "download=normal");
            final String reconnect_mins = br.getRegex(">Please wait for (\\d+) minutes  to download the next file").getMatch(0);
            if (reconnect_mins != null) {
                logger.info("Reconnect limit detected");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnect_mins) * 60 * 1001l);
            }
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                handleErrorsWebsite();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            checkDirectlinkFailed(downloadLink, directlinkproperty);
            checkResponseCodeErrors(dl.getConnection());
            logger.info("Finallink does not lead to a file, continuing...");
            br.followConnection();
            this.handleErrorsWebsite();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private void handleErrorsWebsite() throws PluginException {

        /* FREE-only errors */
        if (this.br.containsHTML("Captcha error")) {
            /* 2017-01-06 */
            /*
             * E.g.
             * "<h1>Captcha error.<br>To enjoy maximum download speeds and unlimited parallel downloads,</h1> <a class="buyPremiumLink" href="
             * /premium.php">upgrade to Premium account now</a>"
             */
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        /* Account-only errors */
        if (this.br.containsHTML("For security measures, we ask you to update your password")) {
            /* 2017-01-06 */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Website says: 'For security measures, we ask you to update your password.'", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (this.br.containsHTML("You have exceeded your file size download limit")) {
            /* 2017-01-06 */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Website says: 'You have exceeded your file size download limit'", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }

        /* Other-errors */
        /*
         * <h1>File is not available<br>We are sorry...<br/>The page you requested cannot be displayed right now. The file may have removed
         * by the uploader or expired.</h1>
         */
        if (this.br.containsHTML(">File is not available<|The file may have removed by the uploader or expired")) {
            /* Typically file offline after download attempt. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        /* GENERAL-errors (via errorcode) */
        /* Error-links: http://www.bigfile.to/l-error.php?error_code=ERRORCODE OR http://www.bigfile.to//landing-1406.html */
        String errorcode_str = new Regex(this.br.getURL(), "error_code=(\\d+)").getMatch(0);
        if (errorcode_str == null) {
            errorcode_str = new Regex(this.br.getURL(), "landing\\-(\\d+)\\.html").getMatch(0);
        }
        int errorcode = 0;
        if (errorcode_str != null) {
            errorcode = Integer.parseInt(errorcode_str);
            switch (errorcode) {
            case 617:
                /* This typically happens in availablecheck. */
                /* >File is not available. Please check your link again.< */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 1406:
                /* Probably this is some kinda expired session. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error '406 - Cookie not found'", 3 * 60 * 1000l);
            case 1702:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Your download link has expired'", 1 * 60 * 1000l);
            case 1703:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error '406 - Cookie not found'", 3 * 60 * 1000l);
            default:
                break;
            }
        }
    }

    /**
     * Use this if a download fails on final download attempt. In case a previously generated directurl has been re-used, this will simply
     * retry and set the directlinkproperty to NULL to avoid unhandled errorcases.
     */
    private void checkDirectlinkFailed(final DownloadLink dl, final String directlinkproperty) throws PluginException {
        final String directlink = dl.getStringProperty(directlinkproperty, null);
        if (directlink != null) {
            dl.setProperty(directlinkproperty, Property.NULL);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Directlink failed/expired");
        }
    }

    /** Handles all kinds of error-responsecodes! */
    private void checkResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con == null) {
            return;
        }
        final long responsecode = con.getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 10 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 10 * 60 * 1000l);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || con.getResponseCode() == 404) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings({ "deprecation" })
    private AccountInfo login(final Account account, final boolean force, final AccountInfo ac) throws Exception {
        synchronized (LOCK) {
            final AccountInfo ai = ac != null ? ac : new AccountInfo();
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBr(br);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedin = false;
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    // lets do a check - this will also avoid unnerving login captchas!
                    br.getPage(baseDomain + "/indexboard.php");
                    if (!isNotLoggedIn(br, account)) {
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        loggedin = true;
                    } else {
                        loggedin = false;
                    }
                }
                if (!loggedin) {
                    /* Forced https! */
                    br.getPage(baseDomain + "/login.php");
                    Form loginform = br.getFormbyProperty("id", "loginForm");
                    if (loginform == null) {
                        loginform = br.getForm(0);
                    }
                    if (loginform == null) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    if (StringUtils.contains(account.getUser(), "@")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "You cannot login via e-mail. Please use your username!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    loginform.put("userName", Encoding.urlEncode(account.getUser()));
                    loginform.put("userPassword", Encoding.urlEncode(account.getPass()));
                    loginform.remove("autoLogin");
                    loginform.put("autoLogin", "on");
                    loginform.remove("action__login");
                    loginform.put("action__login", "normalLogin");
                    if (loginform.hasInputFieldByName("recaptcha_response_field")) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", getHost(), baseDomain, true);
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.setId(recaptchaid);
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("recaptcha", cf, dummyLink);
                        loginform.put("recaptcha_response_field", Encoding.urlEncode(c));
                        loginform.put("recaptcha_challenge_field", Encoding.urlEncode(rc.getChallenge()));
                    }
                    this.br.submitForm(loginform);
                    if (isNotLoggedIn(br, account)) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder login Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                br.getPage("/indexboard.php");
                final String space = br.getRegex(">Storage</div>[\t\r\n ]+<div class=\"b_blue_type\">\\s*([^\"/]*?)\\s*</span>").getMatch(0);
                if (space != null) {
                    ai.setUsedSpace(space.trim().replace("<span>", ""));
                }
                final String expiredate = br.getRegex("lass=\"grey_type\">[\r\n\t ]+Until\\s*([^<>\"]*?)\\s*</div>").getMatch(0);
                if (expiredate == null) {
                    // free accounts can still have captcha.
                    account.setMaxSimultanDownloads(1);
                    account.setConcurrentUsePossible(false);
                    account.setType(AccountType.FREE);
                    ai.setStatus("Free Account");
                } else {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate.trim(), "dd MMM yyyy", Locale.ENGLISH) + (24 * 60 * 60 * 1000l));
                    account.setMaxSimultanDownloads(20);
                    account.setConcurrentUsePossible(true);
                    account.setType(AccountType.PREMIUM);
                    ai.setStatus("Premium Account");
                }
                ai.setUnlimitedTraffic();
                account.setValid(true);

                account.saveCookies(br.getCookies(this.getHost()), "");
                if (ac == null) {
                    account.setAccountInfo(ai);
                }
                return ai;
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void doesPasswordNeedChanging(final Browser br, final Account account) throws IOException, PluginException {
        if (account == null) {
            return;
        }
        // test to confirm that user password doesn't need changing
        if (StringUtils.endsWithCaseInsensitive(br.getRedirectLocation(), "/account.php")) {
            br.getPage(br.getRedirectLocation());
            if (br.containsHTML("<div>For security measures, we ask you to update your password\\.</div>")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Service provider asks that you update your passsword.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown error! Please report to JDownloader Development Team.");
        }
    }

    private boolean isNotLoggedIn(final Browser br, final Account account) throws IOException, PluginException {
        doesPasswordNeedChanging(br, account);
        return !br.containsHTML("class=\"icon logout\"");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        try {
            return login(account, true, new AccountInfo());
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false, null);
        if (account.getType() == AccountType.FREE) {
            br.getPage(link.getDownloadURL());
            doFree(link, account);
        } else {
            br.setFollowRedirects(false);
            final String directlinkproperty = "directlink_premium";
            String dllink = checkDirectLink(link, directlinkproperty);
            if (dllink == null) {
                br.postPage(link.getDownloadURL(), "download=premium");
                /*
                 * Full message: You have exceeded your download limit. Please verify your email address to continue downloading.
                 */
                if (br.containsHTML("You have exceeded your download limit")) {
                    if (br.containsHTML("Please verify your email address to continue")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please verify your email address to continue downloading", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Downloadlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                }
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    handleErrorsWebsite();
                    logger.warning("Final link is null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            int maxchunks = 1;
            if (this.getPluginConfig().getBooleanProperty(PREMIUM_UNLIMITEDCHUNKS, false)) {
                logger.info("User is allowed to use more than 1 chunk in premiummode");
                maxchunks = 0;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                checkDirectlinkFailed(link, directlinkproperty);
                checkResponseCodeErrors(dl.getConnection());
                br.followConnection();
                handleErrorsWebsite();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dllink);
            dl.startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if ((account == null || account.getType() == AccountType.FREE) && downloadLink.getDownloadSize() > FREE_SIZELIMIT) {
            return false;
        } else {
            return true;
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

    private void setConfigElements() {
        this.getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), PREMIUM_UNLIMITEDCHUNKS, JDL.L("plugins.hoster.uploadablech.allowPremiumUnlimitedChunks", "Allow unlimited (=20) chunks for premium mode [may cause issues]?")).setDefaultValue(false));
    }
}