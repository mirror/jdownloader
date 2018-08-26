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
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
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

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "publish2.me" }, urls = { "https?://(www\\.)?publish2\\.me/file/[a-z0-9]{13,}" })
public class Publish2Me extends K2SApi {
    private final String MAINPAGE = "http://publish2.me";

    public Publish2Me(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/#premium");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/page/terms.html";
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "publish2.me" };
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // link cleanup, but respect users protocol choosing.
        link.setUrlDownload(link.getDownloadURL().replaceFirst("^https?://", getProtocol()));
    }

    /* K2SApi setters */
    /**
     * sets domain the API will use!
     */
    @Override
    protected String getDomain() {
        return "publish2.me";
    }

    @Override
    public long getVersion() {
        return (Math.max(super.getVersion(), 0) * 100000) + getAPIRevision();
    }

    @Override
    protected String getFUID(final DownloadLink downloadLink) {
        return new Regex(downloadLink.getDownloadURL(), "/file/([a-z0-9]+)").getMatch(0);
    }

    /**
     * easiest way to set variables, without the need for multiple declared references
     *
     * @param account
     */
    private void setConstants(final Account account) {
        if (account != null) {
            if (account.getType() == AccountType.FREE) {
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

    /* end of K2SApi stuff */
    private void setConfigElements() {
        final ConfigEntry cfgapi = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), getUseAPIPropertyID(), "Use API (recommended!)").setDefaultValue(isUseAPIDefaultEnabled());
        getConfig().addEntry(cfgapi);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), EXPERIMENTALHANDLING, "Enable reconnect workaround (only for API mode!)?").setDefaultValue(default_eh).setEnabledCondidtion(cfgapi, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), super.CUSTOM_REFERER, "Set custom Referer here (only non NON-API mode!)").setDefaultValue(null).setEnabledCondidtion(cfgapi, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, "Use Secure Communication over SSL (HTTPS://)").setDefaultValue(default_SSL_CONNECTION));
    }

    public String getFUID(final String link) {
        return new Regex(link, "/file/([a-z0-9]+)$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        // for multihosters which call this method directly.
        if (useAPI()) {
            return super.requestFileInformation(link);
        }
        correctDownloadLink(link);
        this.setBrowserExclusive();
        super.prepBrowserForWebsite(this.br);
        getPage(link.getDownloadURL());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("class=\"icon-download-alt\" style=\"\"></i>([^<>\"]*?)</div>").getMatch(0);
        final String filesize = br.getRegex(">File size: ([^<>\"]*?)</").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        if (useAPI()) {
            super.handleDownload(downloadLink, null);
        } else {
            requestFileInformation(downloadLink);
            doFree(downloadLink, null);
        }
    }

    private final String freeAccConLimit = "Free account does not allow to download more than one file at the same time";
    private final String reCaptcha       = "api\\.recaptcha\\.net|google\\.com/recaptcha/api/";
    private final String formCaptcha     = "/file/captcha\\.html\\?v=[a-z0-9]+";

    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        String dllink = downloadLink.getStringProperty(directlinkproperty, null);
        // because opening the link to test it, uses up the availability, then reopening it again = too many requests too quickly issue.
        if (!inValidate(dllink)) {
            final Browser obr = br.cloneBrowser();
            logger.info("Reusing cached finallink!");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
            if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getLongContentLength() == -1 || dl.getConnection().getResponseCode() == 401) {
                br.followConnection();
                handleGeneralServerErrors(account, downloadLink);
                // we now want to restore!
                br = obr;
                dllink = null;
                downloadLink.setProperty(directlinkproperty, Property.NULL);
            }
        }
        // if above has failed, dllink will be null
        if (inValidate(dllink)) {
            dllink = getDllink();
            if (dllink == null) {
                if (br.containsHTML(">\\s*This file is available<br>only for premium members\\.\\s*</div>")) {
                    premiumDownloadRestriction("This file can only be downloaded by premium users");
                }
                final String id = br.getRegex("data-slow-id=\"([a-z0-9]+)\"").getMatch(0);
                if (id == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                postPage(br.getURL(), "slow_id=" + id);
                if (br.containsHTML("Free user can't download large files")) {
                    premiumDownloadRestriction("This file can only be downloaded by premium users");
                } else if (br.containsHTML(freeAccConLimit)) {
                    // could be shared network or a download hasn't timed out yet or user downloading in another program?
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Connection limit reached", 10 * 60 * 60 * 1001);
                }
                if (br.containsHTML(">Downloading is not possible<")) {
                    final Regex waittime = br.getRegex("Please wait (\\d{2}):(\\d{2}):(\\d{2}) to download this");
                    String tmphrs = waittime.getMatch(0);
                    String tmpmin = waittime.getMatch(1);
                    String tmpsec = waittime.getMatch(2);
                    if (tmphrs == null && tmpmin == null && tmpsec == null) {
                        logger.info("Waittime regexes seem to be broken");
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
                    } else {
                        int minutes = 0, seconds = 0, hours = 0;
                        if (tmphrs != null) {
                            hours = Integer.parseInt(tmphrs);
                        }
                        if (tmpmin != null) {
                            minutes = Integer.parseInt(tmpmin);
                        }
                        if (tmpsec != null) {
                            seconds = Integer.parseInt(tmpsec);
                        }
                        int totalwaittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                        logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, totalwaittime);
                    }
                }
                dllink = getDllink();
                if (dllink == null) {
                    final Browser cbr = br.cloneBrowser();
                    String captcha = null;
                    final int repeat = 4;
                    for (int i = 1; i <= repeat; i++) {
                        if (br.containsHTML(reCaptcha)) {
                            final Recaptcha rc = new Recaptcha(br, this);
                            rc.findID();
                            rc.load();
                            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                            final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                            postPage(br.getURL(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&free=1&freeDownloadRequest=1&uniqueId=" + id);
                            if (br.containsHTML(reCaptcha) && i + 1 != repeat) {
                                continue;
                            } else if (br.containsHTML(reCaptcha) && i + 1 == repeat) {
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            } else {
                                break;
                            }
                        } else if (br.containsHTML(formCaptcha)) {
                            if (captcha == null) {
                                captcha = br.getRegex(formCaptcha).getMatch(-1);
                                if (captcha == null) {
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                }
                            }
                            String code = getCaptchaCode(captcha, downloadLink);
                            postPage(br.getURL(), "CaptchaForm%5BverifyCode%5D=" + code + "&free=1&freeDownloadRequest=1&uniqueId=" + id);
                            if (br.containsHTML(formCaptcha) && i + 1 != repeat) {
                                getPage(cbr, "/file/captcha.html?refresh=1&_=" + System.currentTimeMillis());
                                captcha = cbr.getRegex("\"url\":\"([^<>\"]*?)\"").getMatch(0);
                                if (captcha != null) {
                                    captcha = captcha.replace("\\", "");
                                }
                                continue;
                            } else if (br.containsHTML(formCaptcha) && i + 1 == repeat) {
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            } else {
                                break;
                            }
                        }
                    }
                    int wait = 30;
                    final String waittime = br.getRegex("class=\"tik-tak\"[\t\r\n ]{0,}>(\\d+)</div>").getMatch(0);
                    if (waittime != null) {
                        wait = Integer.parseInt(waittime);
                    }
                    this.sleep(wait * 1001l, downloadLink);
                    postPage(br.getURL(), "free=1&uniqueId=" + id);
                    if (br.containsHTML(freeAccConLimit)) {
                        // could be shared network or a download hasn't timed out yet or user downloading in another program?
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Connection limit reached", 10 * 60 * 60 * 1001);
                    }
                    if (br.containsHTML("Download count files exceed!<")) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download count files exceed!", 10 * 60 * 60 * 1001);
                    }
                    dllink = getDllink();
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            logger.info("dllink = " + dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                handleGeneralServerErrors(account, downloadLink);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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

    private boolean isCaptchaInvalid(Browser br) {
        return br.containsHTML(">Invalid reCAPTCHA<") || br.containsHTML(">Please pass reCAPTCHA<") || br.containsHTML("The verification code is incorrect.");
    }

    private boolean handleLoginCaptcha(final Account account, Browser br, Form login) throws Exception {
        final String captchaLink = login.getRegex("\"(/auth/captcha\\.html\\?v=[a-z0-9]+)\"").getMatch(0);
        if (captchaLink != null) {
            final DownloadLink dummyLink = new DownloadLink(this, "Account", account.getHoster(), "http://" + account.getHoster(), true);
            final String code = getCaptchaCode("https://" + br.getHost() + captchaLink, dummyLink);
            if (code == null) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            login.put("LoginForm%5BverifyCode%5D=", Encoding.urlEncode(code));
            return true;
        } else if (login.containsHTML("class=\"g-recaptcha\"")) {
            // recapthav2
            final DownloadLink original = this.getDownloadLink();
            if (original == null) {
                this.setDownloadLink(new DownloadLink(this, "Account", getHost(), "http://" + br.getRequest().getURL().getHost(), true));
            }
            try {
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                if (recaptchaV2Response == null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                login.put("LoginForm%5BverifyCode%5D", Encoding.urlEncode(recaptchaV2Response));
            } finally {
                if (original == null) {
                    this.setDownloadLink(null);
                }
            }
            return true;
        }
        return false;
    }

    private void login(final Account account, final boolean force, final String MAINPAGE) throws Exception {
        synchronized (account) {
            try {
                // clear cookies/headers etc. this should nullify redirects to /file/
                br = newWebBrowser(true);
                // reduce cpu cycles, do not enter and do evaluations when they are not needed.
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    final String cookieAge = TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - account.getCookiesTimeStamp(""), 0);
                    br.setCookies(MAINPAGE, cookies);
                    getPage(MAINPAGE + "/site/profile.html");
                    if (!br._getURL().getFile().equals("/login.html")) {
                        if (br.containsHTML("Your Premium account has expired")) {
                            account.setType(Account.AccountType.FREE);
                        }
                        logger.info("Login via ached cookies successful:" + account.getType() + "|CookieAge:" + cookieAge);
                        account.saveCookies(br.getCookies(MAINPAGE), "");
                        return;
                    }
                    logger.info("Login via cached cookies failed:" + account.getType() + "|CookieAge:" + cookieAge);
                    // dump session
                    br = newWebBrowser(true);
                }
                getPage(this.MAINPAGE + "/login.html");
                Form login = br.getFormbyActionRegex("/login.html");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("LoginForm%5Busername%5D", Encoding.urlEncode(account.getUser()));
                login.put("LoginForm%5Bpassword%5D", Encoding.urlEncode(account.getPass()));
                boolean hasCaptcha = handleLoginCaptcha(account, br, login);
                sendForm(login);
                if (!hasCaptcha && isCaptchaInvalid(br)) {
                    login = br.getFormbyActionRegex("/login.html");
                    if (login == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    login.put("LoginForm%5Busername%5D", Encoding.urlEncode(account.getUser()));
                    login.put("LoginForm%5Bpassword%5D", Encoding.urlEncode(account.getPass()));
                    hasCaptcha = handleLoginCaptcha(account, br, login);
                    if (!hasCaptcha) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    sendForm(login);
                }
                if (isCaptchaInvalid(br)) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else if (br.containsHTML("Incorrect username or password")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (br.containsHTML(">We have a suspicion that your account was stolen, this is why we")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account temporär gesperrt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account temporarily blocked!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (br.containsHTML(">Please fill in the form with your login credentials")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (br.containsHTML(">Password cannot be blank.<")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Passwortfeld darf nicht leer sein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Password field cannot be empty!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (br.getFormbyActionRegex("/login.html") != null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (br.containsHTML("Your Premium account has expired")) {
                    account.setType(Account.AccountType.FREE);
                }
                logger.info("Fresh login!");
                account.saveCookies(br.getCookies(MAINPAGE), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
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
            try {
                login(account, true, MAINPAGE);
            } catch (PluginException e) {
                account.setValid(false);
                throw e;
            }
            getPage("/site/profile.html");
            ai.setUnlimitedTraffic();
            final String expire = br.getRegex("Premium expires:[\t\n\r ]+<b>([^<>\"]*?)</b>").getMatch(0);
            if (expire == null) {
                ai.setStatus("Free Account");
                account.setType(AccountType.FREE);
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH));
                ai.setStatus("Premium Account");
                account.setType(AccountType.PREMIUM);
            }
            final String trafficleft = br.getRegex("Available traffic \\(today\\):[\t\n\r ]+<b><a href=\"/user/statistic\\.html\">([^<>\"]*?)</a>").getMatch(0);
            if (trafficleft != null) {
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            }
            setAccountLimits(account);
            account.setValid(true);
        }
        return ai;
    }

    @Override
    protected void setAccountLimits(Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            maxPrem.set(1);
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            maxPrem.set(20);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account);
        if (useAPI()) {
            super.handleDownload(link, account);
        } else {
            requestFileInformation(link);
            login(account, false, "https://" + Browser.getHost(link.getPluginPatternMatcher()));
            br.setFollowRedirects(false);
            getPage(link.getDownloadURL());
            if (account.getType() == AccountType.FREE) {
                doFree(link, account);
            } else {
                String dllink = br.getRedirectLocation();
                /* Maybe user has direct downloads disabled */
                if (inValidate(dllink)) {
                    dllink = getDllink();
                    if (inValidate(dllink)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), resumes, chunks);
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

    private String getDllink() throws Exception {
        String dllink = br.getRegex("(\"|')(/file/url\\.html\\?file=[a-z0-9]+)\\1").getMatch(1);
        if (dllink != null) {
            getPage(dllink);
            dllink = br.getRegex("\"url\":\"(https?[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRedirectLocation();
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
        }
        return dllink;
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}