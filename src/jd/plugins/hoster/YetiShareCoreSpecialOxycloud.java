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

import java.io.File;
import java.io.IOException;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/** TODO: Move to JDownloader/src/jdownloader/plugins/components */
public class YetiShareCoreSpecialOxycloud extends YetiShareCore {
    public YetiShareCoreSpecialOxycloud(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * other: Special <br />
     */
    @Override
    protected boolean usesNewYetiShareVersion() {
        return true;
    }

    @Override
    protected String getAccountNameSpaceHome() {
        return "/account";
    }

    @Override
    protected String getAccountNameSpaceUpgrade() {
        return "/upgrade";
    }

    @Override
    protected boolean isPremiumAccount(final Account account, final Browser br) {
        return false;
    }

    @Override
    protected void loginWebsite(final Account account, boolean force) throws Exception {
        loginWebsiteSpecial(account, force);
    }

    /**
     * @return true: Cookies were validated</br>
     *         false: Cookies were not validated
     */
    public boolean loginWebsiteSpecial(final Account account, boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(this.br, account.getHoster());
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !force) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        return false;
                    }
                    logger.info("Verifying login-cookies");
                    getPage(this.getMainPage() + "/upgrade");
                    if (this.isLoggedin()) {
                        logger.info("Successfully logged in via cookies");
                        /* Set/Update account-type */
                        if (this.isPremiumAccount(account, br)) {
                            setAccountLimitsByType(account, AccountType.PREMIUM);
                        } else {
                            setAccountLimitsByType(account, AccountType.FREE);
                        }
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Failed to login via cookies");
                    }
                }
                logger.info("Performing full login");
                getPage(this.getProtocol() + this.getHost() + "/account/login");
                Form loginform;
                if (br.containsHTML("flow\\-login\\.js") && !enforce_old_login_method()) {
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
                        /* E.g. crazyshare.cc */
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
                } else {
                    /* Old login method - rare case! Example: udrop.net --> 2020-08-07: Website is dead */
                    logger.info("Using old login method");
                    loginform = br.getFormbyProperty("id", "form_login");
                    if (loginform == null) {
                        loginform = br.getFormbyKey("loginUsername");
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
                        /* 2020-04-30: E.g. filemia.com */
                        loginform.put("email", Encoding.urlEncode(account.getUser()));
                        loginform.put("password", Encoding.urlEncode(account.getPass()));
                    } else {
                        loginform.put("username", Encoding.urlEncode(account.getUser()));
                        loginform.put("password", Encoding.urlEncode(account.getPass()));
                    }
                    /* 2019-07-31: At the moment only this older login method supports captchas. Examplehost: uploadship.com */
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        /* Handle login-captcha if required */
                        DownloadLink dlinkbefore = this.getDownloadLink();
                        try {
                            final DownloadLink dl_dummy;
                            if (dlinkbefore != null) {
                                dl_dummy = dlinkbefore;
                            } else {
                                dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                                this.setDownloadLink(dl_dummy);
                            }
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
                            final String code = getCaptchaCode("solvemedia", cf, dl_dummy);
                            final String chid = sm.getChallenge(code);
                            loginform.put("adcopy_challenge", chid);
                            loginform.put("adcopy_response", "manual_challenge");
                        } finally {
                            if (dlinkbefore != null) {
                                this.setDownloadLink(dlinkbefore);
                            }
                        }
                    }
                    submitForm(loginform);
                    if (br.containsHTML(">\\s*Your username and password are invalid<") || !isLoggedin()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
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
    protected boolean isLoggedin() {
        return this.isLoggedinSpecial();
    }

    public boolean isLoggedinSpecial() {
        boolean loggedIN = super.isLoggedin();
        if (!loggedIN) {
            /*
             * Traits depend on where user currently is: Case 1: For whenever logout button is visible (e.g. account overview) | Case 2:
             * When logout button is not visible e.g. on "/upgrade" page.
             */
            loggedIN = br.containsHTML("/account/logout\"") || br.containsHTML("/account\"");
        }
        return loggedIN;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (this.supports_api()) {
            this.handleDownloadAPI(link, account);
        } else {
            requestFileInformation(link, account, true);
            loginWebsite(account, false);
            this.handleDownloadWebsite(link, account);
        }
    }

    @Override
    public void handleDownloadWebsite(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* 2020-11-13: Anonymous- & Free Account- and premium account download works the same way. */
        br.setFollowRedirects(true);
        String dllink = checkDirectLink(link, account);
        if (dllink != null) {
            logger.info("Continuing with stored directURL");
        } else {
            logger.info("Generating new directURL");
            final URLConnectionAdapter con = br.openGetConnection(link.getPluginPatternMatcher());
            if (this.looksLikeDownloadableContent(con)) {
                dllink = con.getURL().toString();
            } else {
                br.followConnection();
                br.setFollowRedirects(false);
                Form pwProtected = getPasswordProtectedForm();
                if (pwProtected != null) {
                    /* File is password protected --> Totally different download-way */
                    String passCode = link.getDownloadPassword();
                    if (passCode == null) {
                        passCode = getUserInput("Password?", link);
                    }
                    pwProtected.put("filePassword", Encoding.urlEncode(passCode));
                    this.submitForm(pwProtected);
                    if (!this.isDownloadlink(br.getRedirectLocation()) || this.getPasswordProtectedForm() != null) {
                        /* Assume that entered password is wrong! */
                        link.setDownloadPassword(null);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                    } else {
                        /* Entered password is correct - we can start the download. */
                        dllink = br.getRedirectLocation();
                        link.setDownloadPassword(passCode);
                    }
                } else {
                    String internalFileID = this.getInternalFileID(link, this.br);
                    if (internalFileID == null) {
                        /* Check for redirects before this step. E.g. letsupload.io */
                        final String continueURL = this.getContinueLink();
                        if (continueURL == null) {
                            checkErrorsLastResort(link, account);
                        }
                        this.getPage(continueURL);
                        internalFileID = this.getInternalFileID(link, this.br);
                        if (internalFileID == null) {
                            /* Dead end */
                            checkErrorsLastResort(link, account);
                        } else {
                            /* Save for the next time. This ID should never change! */
                            link.setProperty(PROPERTY_INTERNAL_FILE_ID, internalFileID);
                        }
                    }
                    if (internalFileID == null) {
                        this.checkErrors(link, account);
                        checkErrorsLastResort(link, account);
                    }
                    br.getPage("/account/direct_download/" + internalFileID);
                    dllink = br.getRedirectLocation();
                }
            }
        }
        if (dllink == null) {
            this.checkErrors(link, account);
            checkErrorsLastResort(link, account);
        }
        final boolean resume = this.isResumeable(link, account);
        final int maxchunks = this.getMaxChunks(account);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        link.setProperty(getDownloadModeDirectlinkProperty(account), dl.getConnection().getURL().toString());
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            checkErrors(link, account);
            /*
             * Do not check for logged-out state because we could easily get other errorpages here and we do not want to temp. disable
             * accounts by mistake!
             */
            // checkErrorsLastResort(link, account);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error", 5 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private Form getPasswordProtectedForm() {
        return br.getFormbyKey("filePassword");
    }

    @Override
    protected boolean isOfflineWebsite(final DownloadLink link) throws Exception {
        boolean offline = super.isOfflineWebsite(link);
        if (!offline) {
            offline = isOfflineSpecial();
        }
        return offline;
    }

    protected boolean isOfflineSpecial() {
        return br.containsHTML(">\\s*File has been removed|>\\s*File not found");
    }

    @Override
    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        super.checkErrors(link, account);
        /* 2020-10-12 */
        final String waittimeBetweenDownloadsStr = br.getRegex(">\\s*You must wait (\\d+) minutes? between downloads").getMatch(0);
        if (waittimeBetweenDownloadsStr != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait between downloads", Integer.parseInt(waittimeBetweenDownloadsStr) * 60 * 1001l);
        } else if (isOfflineSpecial()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    protected String getInternalFileID(final DownloadLink link, final Browser br) throws PluginException {
        String internalFileID = link.getStringProperty(PROPERTY_INTERNAL_FILE_ID);
        if (internalFileID == null) {
            internalFileID = br.getRegex("showFileInformation\\((\\d+)\\);").getMatch(0);
        }
        return internalFileID;
    }
}