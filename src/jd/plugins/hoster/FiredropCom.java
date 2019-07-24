//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FiredropCom extends YetiShareCore {
    public FiredropCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null solvemedia reCaptchaV2<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "firedrop.com", "frd.li" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String[]> pluginDomains = getPluginDomains();
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?:/file_password\\.html\\?file=[A-Za-z0-9]+|" + YetiShareCore.getDefaultAnnotationPatternPart() + ")");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public String getFUIDFromURL(final String url) {
        /* 2019-07-23: Special */
        String fuid = new Regex(url, "\\?file=([A-Za-z0-9]+)").getMatch(0);
        if (fuid == null) {
            fuid = super.getFUIDFromURL(url);
        }
        return fuid;
    }

    @Override
    protected void login(final Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(this.br, account.getHoster());
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedInViaCookies = false;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !force) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        return;
                    }
                    logger.info("Verifying login-cookies");
                    getPage(this.getMainPage() + "/account_home.html");
                    loggedInViaCookies = br.containsHTML("/logout.html");
                }
                if (loggedInViaCookies) {
                    /* No additional check required --> We know cookies are valid and we're logged in --> Done! */
                    logger.info("Successfully logged in via cookies");
                } else {
                    logger.info("Performing full login");
                    getPage(this.getProtocol() + this.getHost() + "/login.html");
                    Form loginform;
                    if (br.containsHTML("flow\\-login\\.js")) {
                        final String loginstart = new Regex(br.getURL(), "(https?://(www\\.)?)").getMatch(0);
                        /* New (ajax) login method - mostly used - example: iosddl.net */
                        logger.info("Using new login method");
                        /* These headers are important! */
                        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                        loginform = br.getFormbyProperty("id", "form_login");
                        if (loginform == null) {
                            logger.info("Fallback to custom built loginform");
                            loginform = new Form();
                            loginform.put("submitme", "1");
                        }
                        loginform.put("username", Encoding.urlEncode(account.getUser()));
                        loginform.put("password", Encoding.urlEncode(account.getPass()));
                        final String action = loginstart + this.getHost() + "/ajax/_account_login.ajax.php";
                        loginform.setAction(action);
                        if (loginform.containsHTML("class=\"g\\-recaptcha\"")) {
                            final DownloadLink dlinkbefore = this.getDownloadLink();
                            if (dlinkbefore == null) {
                                this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                            }
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            if (dlinkbefore != null) {
                                this.setDownloadLink(dlinkbefore);
                            }
                            loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                        }
                        submitForm(loginform);
                        if (!br.containsHTML("\"login_status\":\"success\"")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    } else if (true) {
                        /* 2019-07-23: TODO: fixme */
                        final String loginstart = new Regex(br.getURL(), "(https?://(www\\.)?)").getMatch(0);
                        /* New (ajax) login method - mostly used - example: iosddl.net */
                        logger.info("Using new SPECIAL login method");
                        /* These headers are important! */
                        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                        loginform = br.getFormbyProperty("id", "form_login");
                        if (loginform == null) {
                            loginform = br.getFormbyProperty("id", "frm-login");
                        }
                        if (loginform == null) {
                            logger.info("Fallback to custom built loginform");
                            loginform = new Form();
                            loginform.put("submitme", "1");
                        } else {
                            /* 2019-07-24: Hmm we have to correct this sometimes! */
                            if (loginform.getMethod() == null || loginform.getMethod() != MethodType.POST) {
                                loginform.setMethod(MethodType.POST);
                            }
                        }
                        loginform.put("email", Encoding.urlEncode(account.getUser()));
                        loginform.put("password", Encoding.urlEncode(account.getPass()));
                        final String action = loginstart + this.getHost() + "/dashboard/ajax/post";
                        loginform.setAction(action);
                        if (loginform.containsHTML("class=\"g\\-recaptcha\"")) {
                            final DownloadLink dlinkbefore = this.getDownloadLink();
                            if (dlinkbefore == null) {
                                this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                            }
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            if (dlinkbefore != null) {
                                this.setDownloadLink(dlinkbefore);
                            }
                            loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                        }
                        submitForm(loginform);
                        if (!br.containsHTML("\"error\":false")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    } else {
                        /* Old login method - rare case! Example: udrop.net */
                        logger.info("Using old login method");
                        loginform = br.getFormbyProperty("id", "form_login");
                        if (loginform == null) {
                            loginform = br.getFormbyKey("loginUsername");
                        }
                        if (loginform == null) {
                            /* 2019-07-23: Example: firedrop.com */
                            loginform = br.getFormbyKey("email");
                        }
                        if (loginform == null) {
                            logger.info("Fallback to custom built loginform");
                            loginform = new Form();
                            loginform.setMethod(MethodType.POST);
                            loginform.put("submit", "Login");
                            loginform.put("submitme", "1");
                        }
                        if (loginform.hasInputFieldByName("loginUsername") && loginform.hasInputFieldByName("loginPassword")) {
                            /* 2019-07-08: Rare case: Example: freaktab.org */
                            loginform.put("loginUsername", Encoding.urlEncode(account.getUser()));
                            loginform.put("loginPassword", Encoding.urlEncode(account.getPass()));
                        } else if (loginform.hasInputFieldByName("email")) {
                            /* 2019-07-23: Example: firedrop.com */
                            loginform.put("email", Encoding.urlEncode(account.getUser()));
                            loginform.put("password", Encoding.urlEncode(account.getPass()));
                        } else {
                            loginform.put("username", Encoding.urlEncode(account.getUser()));
                            loginform.put("password", Encoding.urlEncode(account.getPass()));
                        }
                        submitForm(loginform);
                        // postPage(this.getProtocol() + this.getHost() + "/login.html", "submit=Login&submitme=1&loginUsername=" +
                        // Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()));
                        if (br.containsHTML(">Your username and password are invalid<") || !br.containsHTML("/logout\\.html\">")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }
}