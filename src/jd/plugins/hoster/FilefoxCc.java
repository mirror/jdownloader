//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FilefoxCc extends XFileSharingProBasic {
    public FilefoxCc(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: premium untested, set FREE account limits<br />
     * captchatype-info: 2019-06-06: reCaptchaV2<br />
     * other:<br />
     */
    private static String[] domains = new String[] { "filefox.cc" };

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -4;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 8;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = new Regex(correctedBR, "<p>([^<>\"]+)</p>\\s*?<p class=\"file\\-size\"").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    protected String regexWaittime() {
        /* 2019-06-06: Special */
        String waitStr = super.regexWaittime();
        if (StringUtils.isEmpty(waitStr)) {
            waitStr = new Regex(correctedBR, "class=\"time\\-remain\">(\\d+)</span>").getMatch(0);
        }
        return waitStr;
    }

    @Override
    protected String getDllink(final DownloadLink downloadLink, final Account account, final Browser br, String src) {
        /* 2019-06-06: Special */
        String dllink = super.getDllink(downloadLink, account, br, src);
        if (StringUtils.isEmpty(dllink)) {
            /* E.g. "https://sXX.filefox.cc/<hash>/<filename>" */
            dllink = new Regex(correctedBR, "class\\s*=\\s*\"btn btn-default\"\\s*href\\s*=\\s*\"(https?[^\"]+)\"").getMatch(0);
        }
        return dllink;
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2019-06-06: Special */
        super.checkErrors(link, account, checkAll);
        if (new Regex(correctedBR, "You have reached the daily download limit").matches()) {
            if (account != null) {
                throw new AccountUnavailableException("You have reached the daily download limit", 1 * 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached the daily download limit", 1 * 60 * 60 * 1000l);
            }
        } else if (new Regex(correctedBR, "class\\s*=\\s*\"paid-only\"").matches()) {
            logger.info("Only downloadable via premium");
            throw new AccountRequiredException();
        } else if (new Regex(correctedBR, "You have reached your download limit per").matches()) {
            /* 2018-02-09: Special: Account trafficlimit reached */
            /*
             * E.g.
             * "You have reached your download limit per 3 days: 32000 MB <br>You can <a href='/premium' target='_blank'>Extend your Premium account</a> to reset download limits."
             */
            logger.info("Premium trafficlimit reached");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (new Regex(correctedBR, "You\\'ve tried to download from \\d+ different IPs").matches()) {
            /* 2018-02-09: Special: Accountsharing block message */
            /*
             * E.g. "<p class="
             * normal">You've tried to download from 2 different IPs in the last 3 hours. You will not be able to download for 3 hours.</p>"
             */
            logger.info("Premium account temporarily blocked because of too many IPs");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        final String reconnectWait = new Regex(correctedBR, "Wait [^<>\"]+ or <a href=\\'[^\"\\']+\\' id=\\'tariff\\-scroll\\'>buy Premium</a> and download now").getMatch(-1);
        if (reconnectWait != null) {
            /* adjust this regex to catch the wait time string for COOKIE_HOST */
            String tmphrs = new Regex(reconnectWait, "\\s+(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = new Regex(reconnectWait, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = new Regex(reconnectWait, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(reconnectWait, "\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                logger.info("Waittime regexes seem to be broken");
                if (account != null) {
                    throw new AccountUnavailableException("Download limit reached", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
                }
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (tmpmin != null) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (tmpsec != null) {
                    seconds = Integer.parseInt(tmpsec);
                }
                if (tmpdays != null) {
                    days = Integer.parseInt(tmpdays);
                }
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                /* Not enough wait time to reconnect -> Wait short and retry */
                if (waittime < 180000) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until new downloads can be started", waittime);
                }
                if (account != null) {
                    throw new AccountUnavailableException("Download limit reached", waittime);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
                }
            }
        }
    }

    @Override
    public String getLoginURL() {
        return super.getMainPage();
    }

    @Override
    public boolean loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedInViaCookies = false;
                if (cookies != null) {
                    br.setCookies(getMainPage(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !force) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust cookies without checking as they're still fresh");
                        return false;
                    }
                    logger.info("Verifying login-cookies");
                    getPage(getMainPage() + "/");
                    loggedInViaCookies = isLoggedin();
                }
                if (loggedInViaCookies) {
                    /* No additional check required --> We know cookies are valid and we're logged in --> Done! */
                    logger.info("Successfully logged in via cookies");
                } else {
                    logger.info("Performing full login");
                    br.clearCookies(getMainPage());
                    getPage(getLoginURL());
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        /* Required for some XFS setups. */
                        getPage(getMainPage() + "/login");
                    }
                    Form loginform = findLoginform(this.br);
                    if (loginform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("email", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    /* Handle login-captcha if required */
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    final DownloadLink dl_dummy;
                    if (dlinkbefore != null) {
                        dl_dummy = dlinkbefore;
                    } else {
                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                        this.setDownloadLink(dl_dummy);
                    }
                    handleCaptcha(dl_dummy, loginform);
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                    submitForm(loginform);
                    if (!isLoggedin()) {
                        if (correctedBR.contains("op=resend_activation")) {
                            /* User entered correct logindata but has not activated his account ... */
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour account has not yet been activated!\r\nActivate it via the URL you should have received via E-Mail and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    // /* 2017-07-25: Special */
                    // if (!br.getURL().contains("/profile")) {
                    // getPage("/profile");
                    // }
                }
                account.saveCookies(br.getCookies(getMainPage()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public boolean isLoggedin() {
        /* 2019-06-13: Special */
        boolean isLoggedin = super.isLoggedin();
        final boolean loginError = correctedBR.contains("Incorrect Login or Password");
        if (isLoggedin && loginError) {
            /*
             * Special case: Login redirects to error-site so login-form is not present --> Upper code may think that we're logged-in -->
             * Correct this
             */
            isLoggedin = false;
        }
        return isLoggedin;
    }

    @Override
    public Form findFormF1Premium() throws Exception {
        /* 2019-06-13: Special */
        checkForSpecialCaptcha();
        return super.findFormF1Premium();
    }

    public Form findFormDownload1() throws Exception {
        /* 2019-06-13: Special */
        checkForSpecialCaptcha();
        return super.findFormDownload1();
    }

    @Override
    protected void getPage(String page) throws Exception {
        super.getPage(page);
        checkForSpecialCaptcha();
    }

    private void checkForSpecialCaptcha() throws Exception {
        if (br.getURL() != null && br.getURL().contains("op=captcha&id=")) {
            /*
             * 2019-01-23: Special - this may also happen in premium mode! This will only happen when accessing downloadurl. It gets e.g.
             * triggered when accessing a lot of different downloadurls in a small timeframe.
             */
            /* Tags: XFS_IP_CHECK /ip_check/ */
            final Form specialCaptchaForm = br.getFormbyProperty("name", "F1");
            if (specialCaptchaForm != null) {
                logger.info("Handling specialCaptchaForm");
                final boolean redirectSetting = br.isFollowingRedirects();
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                specialCaptchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                br.setFollowRedirects(true);
                super.submitForm(specialCaptchaForm);
                br.setFollowRedirects(redirectSetting);
            }
        }
    }

    public static String[] getAnnotationNames() {
        return domains;
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (int i = 0; i < domains.length; i++) {
            if (i == 0) {
                /* Match all URLs on first (=current) domain */
                ret.add("https?://(?:www\\.)?" + getHostsPatternPart() + XFileSharingProBasic.getDefaultAnnotationPatternPart());
            } else {
                ret.add("");
            }
        }
        return ret.toArray(new String[0]);
    }

    /** Returns '(?:domain1|domain2)' */
    public static String getHostsPatternPart() {
        final StringBuilder pattern = new StringBuilder();
        pattern.append("(?:");
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        pattern.append(")");
        return pattern.toString();
    }
}