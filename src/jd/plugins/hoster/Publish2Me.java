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

import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
        getPage(link.getPluginPatternMatcher());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("class=\"icon-download-alt\" style=\"\"></i>([^<>\"]*?)</div>").getMatch(0);
        final String filesize = br.getRegex(">File size: ([^<>\"]*?)</").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
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
            super.handleDownloadWebsite(downloadLink, null);
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

    @Override
    protected String getReCaptchaV2WebsiteKey() {
        return "6LcYcN0SAAAAABtMlxKj7X0hRxOY8_2U86kI1vbb";
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
            ai.setStatus("Please use E-Mail as login/name!\r\nBitte E-Mail Adresse als Benutzername benutzen!");
            return ai;
        }
        if (useAPI()) {
            ai = super.fetchAccountInfo(account);
        } else {
            try {
                login(account, true, MAINPAGE);
            } catch (PluginException e) {
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
            super.handleDownloadWebsite(link, account);
        }
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