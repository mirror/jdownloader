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
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileboom.me" }, urls = { "https?://(www\\.)?(fboom|fileboom)\\.me/file/[a-z0-9]{13,}" })
public class FileBoomMe extends K2SApi {
    private final String MAINPAGE = "https://fboom.me";

    public FileBoomMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/premium.html");
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/page/terms.html";
    }

    @Override
    protected boolean useAPI() {
        final boolean use_api = this.getPluginConfig().getBooleanProperty(getUseAPIPropertyID(), isUseAPIDefaultEnabled());
        return use_api;
    }

    @Override
    protected boolean enforcesHTTPS() {
        return true;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // link cleanup, but respect users protocol choosing.
        link.setUrlDownload(link.getDownloadURL().replaceFirst("^https?://", getProtocol()));
        link.setUrlDownload(link.getDownloadURL().replace("fileboom.me/", "fboom.me/"));
    }

    @Override
    public String[] siteSupportedNames() {
        // keep2.cc no dns
        return new String[] { "fileboom.me", "fboom.me" };
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null) {
            return "fileboom.me";
        }
        for (final String supportedName : siteSupportedNames()) {
            if (supportedName.equals(host)) {
                return "fileboom.me";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    protected String getUseAPIPropertyID() {
        return super.getUseAPIPropertyID();
    }

    @Override
    protected boolean isUseAPIDefaultEnabled() {
        return super.isUseAPIDefaultEnabled();
    }

    /* K2SApi setters */
    /**
     * sets domain the API will use!
     */
    @Override
    protected String getDomain() {
        return "fboom.me";
    }

    @Override
    public long getVersion() {
        return (Math.max(super.getVersion(), 0) * 100000) + getAPIRevision();
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        // for multihosters which call this method directly.
        if (useAPI()) {
            return super.requestFileInformation(link);
        }
        correctDownloadLink(link);
        this.setBrowserExclusive();
        super.prepBrowserForWebsite(br);
        getPage(link.getDownloadURL());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.followRedirect();
        final String filename = br.getRegex("<i class=\"icon-download\"></i>([^<>\"]*?)</").getMatch(0);
        final String filesize = br.getRegex(">File size: ([^<>\"]*?)</").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        setConstants(null);
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        if (useAPI()) {
            super.handleDownload(link, null);
        } else {
            super.handleDownloadWebsite(link, null);
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
                        logger.info("Login via cached cookies successful:" + account.getType() + "|CookieAge:" + cookieAge);
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
            } catch (final PluginException e) {
                throw e;
            }
            getPage("/site/profile.html");
            ai.setUnlimitedTraffic();
            final String expire = br.getRegex("Premium expires:[\t\n\r ]+<b>([^<>\"]*?)</b>").getMatch(0);
            if ("LifeTime".equalsIgnoreCase(expire)) {
                ai.setStatus("Premium Account(LifeTime)");
                ai.setValidUntil(-1);
                account.setType(AccountType.PREMIUM);
            } else if (expire == null) {
                ai.setStatus("Free Account");
                account.setType(AccountType.FREE);
            } else {
                final long expireTimeStamp = TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH);
                if (expireTimeStamp == -1) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                ai.setValidUntil(expireTimeStamp + (24 * 60 * 60 * 1000l));
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
        if (account.getType() == AccountType.FREE) {
            if (checkShowFreeDialog(getHost())) {
                showFreeDialog(getHost());
            }
        }
        if (useAPI()) {
            super.handleDownload(link, account);
        } else {
            requestFileInformation(link);
            login(account, false, "https://" + Browser.getHost(link.getPluginPatternMatcher()));
            br.setFollowRedirects(false);
            getPage(link.getDownloadURL());
            if (account.getType() == AccountType.FREE) {
                super.handleDownloadWebsite(link, account);
            } else {
                String dllink = br.getRedirectLocation();
                if (inValidate(dllink) && br.toString().startsWith("{")) {
                    /* 2017-04-27: Strange - accessing just the downloadurl in premium mode, the website returns json (sometimes?). */
                    dllink = PluginJSonUtils.getJson(br, "url");
                }
                if (inValidate(dllink)) {
                    /* Maybe user has direct downloads disabled */
                    dllink = getDllink();
                    if (inValidate(dllink)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                logger.info("dllink = " + dllink);
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
                if (!isValidDownloadConnection(dl.getConnection())) {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                    if (br.containsHTML("Download of file will start in")) {
                        dllink = br.getRegex("document\\.location\\.href\\s*=\\s*'(https?://.*?)'").getMatch(0);
                    } else {
                        dllink = null;
                    }
                    if (dllink == null) {
                        logger.warning("The final dllink seems not to be a file!");
                        handleGeneralServerErrors(account, link);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
                    if (!isValidDownloadConnection(dl.getConnection())) {
                        dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                        logger.warning("The final dllink seems not to be a file!");
                        br.followConnection();
                        handleGeneralServerErrors(account, link);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
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
            dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
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
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        super.resetLink(link);
    }

    @Override
    protected String getReCaptchaV2WebsiteKey() {
        return "6LcYcN0SAAAAABtMlxKj7X0hRxOY8_2U86kI1vbb";
    }
}