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
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
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
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class OxycloudCom extends YetiShareCore {
    public OxycloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-10-12: null<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "oxycloud.com", "beta.oxycloud.com" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
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
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean requires_WWW() {
        /* 2020-10-12: Disabled because this website is currently running ob subdomain "beta.". */
        return false;
    }

    @Override
    protected boolean isOfflineWebsite(final DownloadLink link) throws Exception {
        boolean offline = super.isOfflineWebsite(link);
        if (!offline) {
            offline = isOfflineSpecial();
        }
        return offline;
    }

    private boolean isOfflineSpecial() {
        return br.containsHTML(">\\s*File has been removed|>\\s*File not found");
    }

    @Override
    public String[] scanInfo(final DownloadLink link, final String[] fileInfo) {
        super.scanInfo(link, fileInfo);
        if (supports_availablecheck_over_info_page(link)) {
            /* 2020-10-12: Special */
            final String betterFilesize = br.getRegex("Filesize\\s*:\\s*</span>\\s*<span>([^<>\"]+)<").getMatch(0);
            if (!StringUtils.isEmpty(betterFilesize)) {
                fileInfo[1] = betterFilesize;
            }
        }
        return fileInfo;
    }

    @Override
    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        super.checkErrors(link, account);
        /* 2020-10-12: Special */
        final String waittimeBetweenDownloadsStr = br.getRegex(">\\s*You must wait (\\d+) minutes? between downloads").getMatch(0);
        if (waittimeBetweenDownloadsStr != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait between downloads", Integer.parseInt(waittimeBetweenDownloadsStr) * 60 * 1001l);
        }
        if (isOfflineSpecial()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*Musisz być użytkownikiem premium aby pobrać ten plik")) {
            /* 2020-10-21 */
            throw new AccountRequiredException();
        }
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        if (br.getURL() == null || !br.getURL().contains("/upgrade")) {
            getPage("/upgrade");
        }
        if (!isPremiumAccount(br)) {
            logger.info("Looks like we have a free account");
            setAccountLimitsByType(account, AccountType.FREE);
        } else {
            /* Daily traffic (with expiredate?) > package traffic --> See possible packages here: https://oxycloud.com/upgrade */
            final String premiumAccountPackagesText = br.getRegex("<td class=\"text-right\"><strong>Reverts To Free Account</strong></td>\\s*<td>(.*?)</td>").getMatch(0);
            final Regex dailyTrafficRegex = br.getRegex("Codzienny transfer odnawialny\\s*:\\s*(\\d+\\.\\d{2} [A-Za-z]+)/(\\d+\\.\\d{2} [A-Za-z]+)");
            final String dailyTrafficLeftStr = dailyTrafficRegex.getMatch(0);
            final String dailyTrafficMaxStr = dailyTrafficRegex.getMatch(1);
            boolean foundPremiumTrait = true;
            String expireStr = br.getRegex("Reverts To Free Account.*?(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            // final String expireStr = br.getRegex("Period premium\\s*:\\s*(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})")
            // .getMatch(0); /* Fits for: /account/edit */
            final String trafficStr = br.getRegex("Transfer package\\s*:\\s*(\\d+[^<>\"]+)<").getMatch(0);
            if (dailyTrafficLeftStr != null && dailyTrafficMaxStr != null) {
                logger.info("Premium with daily trafficlimit");
                setAccountLimitsByType(account, AccountType.PREMIUM);
                ai.setTrafficLeft(SizeFormatter.getSize(dailyTrafficLeftStr));
                ai.setTrafficMax(SizeFormatter.getSize(dailyTrafficMaxStr));
                ai.setStatus("Premium time with daily traffic limit");
            } else if (trafficStr != null) {
                /* 2020-10-15: Hmm traffic package ... but we have no idea how much traffic is left?! */
                ai.setStatus("Premium traffic package");
                setAccountLimitsByType(account, AccountType.PREMIUM);
                ai.setTrafficLeft(SizeFormatter.getSize(trafficStr));
            } else {
                foundPremiumTrait = false;
            }
            /* User can have multiple packages. User can e.g. have daily traffic, extra traffic and expire date. */
            if (expireStr != null) {
                logger.info("Found premium expiredate");
                long expire_milliseconds = parseExpireTimeStamp(account, expireStr);
                /* If the premium account is expired we'll simply accept it as a free account. */
                if (expire_milliseconds < System.currentTimeMillis()) {
                    /* Expired premium -> FREE --> This should never happen! */
                    setAccountLimitsByType(account, AccountType.FREE);
                } else {
                    ai.setStatus("Premium time with no data limit");
                    foundPremiumTrait = true;
                    ai.setValidUntil(expire_milliseconds, this.br);
                    setAccountLimitsByType(account, AccountType.PREMIUM);
                }
            }
            if (!foundPremiumTrait) {
                /* This should never happen */
                logger.info("WTF unknown premium account type??");
                setAccountLimitsByType(account, AccountType.PREMIUM);
                ai.setUnlimitedTraffic();
                ai.setStatus("Premium time with unknown limits (possible JD plugin failure)");
            } else {
                /* This is cosmetic only: Try to display users' bought packages in account status */
                if (premiumAccountPackagesText != null) {
                    final String[] premiumPackages = premiumAccountPackagesText.split("<br>");
                    if (premiumPackages.length > 1) {
                        ai.setStatus(premiumPackages.length + " premium packages:\r\n" + premiumAccountPackagesText.replace("<br>", "\r\n"));
                    }
                }
            }
        }
        return ai;
    }

    private boolean isPremiumAccount(Browser br) {
        return br.containsHTML("Typ Konta</strong></td>\\s*<td>Premium</td>");
    }

    @Override
    protected void loginWebsite(final Account account, boolean force) throws Exception {
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
                        return;
                    }
                    logger.info("Verifying login-cookies");
                    getPage(this.getMainPage() + "/upgrade");
                    if (isLoggedinOnUpgradePage()) {
                        logger.info("Successfully logged in via cookies");
                        /* Set/Update account-type */
                        if (this.isPremiumAccount(br)) {
                            setAccountLimitsByType(account, AccountType.PREMIUM);
                        } else {
                            setAccountLimitsByType(account, AccountType.FREE);
                        }
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
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
        boolean loggedIN = super.isLoggedin();
        if (!loggedIN) {
            loggedIN = br.containsHTML("/logout");
        }
        return loggedIN;
    }

    /** Login-check for "/upgrade" page */
    private boolean isLoggedinOnUpgradePage() {
        boolean loggedIN = super.isLoggedin();
        if (!loggedIN) {
            loggedIN = br.containsHTML("/account\"");
        }
        return loggedIN;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (this.supports_api()) {
            this.handleDownloadAPI(link, account);
        } else {
            if (account.getType() == AccountType.FREE) {
                super.handlePremium(link, account);
            } else {
                requestFileInformation(link, account, true);
                loginWebsite(account, false);
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
                        final String internalFileID = br.getRegex("showFileInformation\\((\\d+)\\);").getMatch(0);
                        if (internalFileID == null) {
                            this.checkErrors(link, account);
                            checkErrorsLastResort(link, account);
                        }
                        br.setFollowRedirects(false);
                        br.getPage("/account/direct_download/" + internalFileID);
                        dllink = br.getRedirectLocation();
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
                if (!isDownloadableContent(dl.getConnection())) {
                    try {
                        br.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    checkErrors(link, account);
                    checkErrorsLastResort(link, account);
                }
                dl.startDownload();
            }
        }
    }
}