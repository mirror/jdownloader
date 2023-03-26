//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.UserAgents;
import jd.plugins.download.HashInfo;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mediafire.com" }, urls = { "https?://(?:www\\.|m\\.)?mediafire\\.com/(download/[a-z0-9]+|(download\\.php\\?|\\?JDOWNLOADER(?!sharekey)|file(?:_premium)?/|file\\?|download/?).*?(?=http:|$|\r|\n))|https?://download\\d+.mediafire(?:cdn)?\\.com/[^/]+/([a-z0-9]+)/([^/]+)" })
public class MediafireCom extends PluginForHost {
    /** Settings stuff */
    private static final String FREE_FORCE_RECONNECT_ON_CAPTCHA = "FREE_FORCE_RECONNECT_ON_CAPTCHA";

    public static String stringUserAgent() {
        return UserAgents.stringUserAgent();
    }

    public static String portableUserAgent() {
        return UserAgents.portableUserAgent();
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "mediafire.com", "mediafire" };
    }

    public static String hbbtvUserAgent() {
        return UserAgents.hbbtvUserAgent();
    }

    private static final String PRIVATEFILE           = JDL.L("plugins.hoster.mediafirecom.errors.privatefile", "Private file: Only downloadable for registered users");
    private static final String PRIVATEFOLDERUSERTEXT = "This is a private folder. Re-Add this link while your account is active to make it work!";
    private static final String TYPE_DIRECT           = "https?://download\\d+.mediafire(?:cdn)?\\.com/[^/]+/([a-z0-9]+)/([^/\"']+)";

    public static abstract class PasswordSolver {
        protected Browser       br;
        protected PluginForHost plg;
        protected DownloadLink  dlink;
        private final int       maxTries;
        private int             currentTry;

        public PasswordSolver(final PluginForHost plg, final Browser br, final DownloadLink downloadLink) {
            this.plg = plg;
            this.br = br;
            this.dlink = downloadLink;
            this.maxTries = 3;
            this.currentTry = 0;
        }

        abstract protected void handlePassword(String password) throws Exception;

        // do not add @Override here to keep 0.* compatibility
        public boolean hasAutoCaptcha() {
            return false;
        }

        abstract protected boolean isCorrect();

        public void run() throws Exception {
            while (this.currentTry++ < this.maxTries) {
                String password = null;
                if ((password = this.dlink.getDownloadPassword()) != null) {
                } else {
                    password = plg.getUserInput(JDL.LF("PasswordSolver.askdialog", "Downloadpassword for %s/%s", this.plg.getHost(), this.dlink.getName()), this.dlink);
                }
                if (password == null) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
                }
                this.handlePassword(password);
                if (!this.isCorrect()) {
                    this.dlink.setDownloadPassword(null);
                    continue;
                } else {
                    this.dlink.setDownloadPassword(password);
                    return;
                }
            }
            throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
    }

    private static AtomicReference<String> agent                      = new AtomicReference<String>(stringUserAgent());
    /**
     * The number of retries to be performed in order to determine if a file is availableor to try captcha/password.
     */
    private int                            max_number_of_free_retries = 3;
    private Browser                        brAPI                      = null;
    private String                         sessionToken               = null;

    /*
     * https://www.mediafire.com/developers/core_api/1.5/getting_started/
     */
    @SuppressWarnings("deprecation")
    public MediafireCom(final PluginWrapper wrapper) {
        super(wrapper);
        /*
         * 2020-09-16: Removed interval for testing purposes. Seems like starting all downloads at once doesn't lead to errors/captchas
         * anymore.
         */
        // this.setStartIntervall(5000);
        this.enablePremium("https://www.mediafire.com/upgrade/");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        final String id;
        if (link.getPluginPatternMatcher().matches(TYPE_DIRECT)) {
            /* 2020-05-29: Correct direct-URL --> Normal URL */
            final Regex dlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_DIRECT);
            id = dlinfo.getMatch(0);
            final String url_filename = dlinfo.getMatch(1);
            final String newURL = String.format("https://www.mediafire.com/file/%s/%s", id, url_filename);
            link.setPluginPatternMatcher(newURL);
        } else {
            id = getFUID(link);
        }
        if (id != null) {
            link.setLinkID("mediafirecom_" + id);
        }
        final String pluginMatcher = link.getPluginPatternMatcher().replaceFirst("https?://media", "https://www.media").replaceFirst("/file_premium/", "/file/");
        link.setPluginPatternMatcher(pluginMatcher);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        Map<String, Object> entries = restoreFromString(this.brAPI.toString(), TypeRef.MAP);
        entries = (Map<String, Object>) entries.get("response");
        entries = (Map<String, Object>) entries.get("user_info");
        /* 2021-04-29: All numbers are given as String -> Use "toLong" method! */
        if (entries.containsKey("used_storage_size")) {
            ai.setUsedSpace(JavaScriptEngineFactory.toLong(entries.get("used_storage_size"), 0));
        }
        if (account.getType() == AccountType.FREE) {
            ai.setStatus("Free Account");
            ai.setUnlimitedTraffic();
            account.setMaxSimultanDownloads(10);
            account.setConcurrentUsePossible(true);
        } else {
            if (entries.containsKey("bandwidth")) {
                ai.setTrafficLeft(JavaScriptEngineFactory.toLong(entries.get("bandwidth"), 0));
            }
            ai.setStatus("Premium Account");
            account.setMaxSimultanDownloads(20);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* 2020-10-01: Changed from 10 to unlimited */
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (link.getBooleanProperty("privatefolder")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, PRIVATEFOLDERUSERTEXT);
        }
        doFree(link, null);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink link, final Account account) throws Exception {
        br = new Browser();
        String finalDownloadurl = null;
        int trycounter = 0;
        boolean captchaCorrect = false;
        if (account == null) {
            br.getHeaders().put("User-Agent", MediafireCom.agent.get());
        }
        do {
            logger.info("Downloadloop number: " + trycounter);
            if (finalDownloadurl != null) {
                break;
            }
            this.requestFileInformation(link);
            if (link.getBooleanProperty("privatefile") && account == null) {
                throw new AccountRequiredException(PRIVATEFILE);
            }
            // Check for direct link
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(link.getDownloadURL());
                if (this.looksLikeDownloadableContent(con)) {
                    finalDownloadurl = con.getURL().toString();
                } else {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            handleNonAPIErrors(link, br);
            if (finalDownloadurl == null) {
                // TODO: This errorhandling is missing for premium users!
                captchaCorrect = false;
                final Form captchaForm = br.getFormbyProperty("name", "form_captcha");
                if (captchaForm != null) {
                    logger.info("Found captchaForm");
                    if (captchaForm.containsHTML("solvemedia.com/papi/")) {
                        logger.info("Detected captcha method \"solvemedia\" for this host");
                        handleExtraReconnectSettingOnCaptcha(account);
                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        String code = getCaptchaCode(cf, link);
                        String chid = sm.getChallenge(code);
                        captchaForm.put("adcopy_challenge", chid);
                        captchaForm.put("adcopy_response", code.replace(" ", "+"));
                        br.submitForm(captchaForm);
                        if (br.getFormbyProperty("name", "form_captcha") != null) {
                            logger.info("solvemedia captcha wrong");
                            continue;
                        }
                    } else if (captchaForm.containsHTML("g-recaptcha-response")) {
                        handleExtraReconnectSettingOnCaptcha(account);
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                        br.submitForm(captchaForm);
                        if (br.getFormbyProperty("name", "form_captcha") != null) {
                            logger.info("recaptchav2 captcha wrong");
                            continue;
                        }
                    } else if (captchaForm.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                        logger.info("Detected captcha method \"Re Captcha\" for this host");
                        handleExtraReconnectSettingOnCaptcha(account);
                        final Recaptcha rc = new Recaptcha(br, this);
                        String id = new Regex(captchaForm.getHtmlCode(), "challenge\\?k=(.+?)\"").getMatch(0);
                        if (id != null) {
                            logger.info("CaptchaID found, Form found " + (captchaForm != null));
                            rc.setId(id);
                            final InputField challenge = new InputField("recaptcha_challenge_field", null);
                            final InputField code = new InputField("recaptcha_response_field", null);
                            captchaForm.addInputField(challenge);
                            captchaForm.addInputField(code);
                            rc.setForm(captchaForm);
                            rc.load();
                            final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                            boolean defect = false;
                            try {
                                final String c = this.getCaptchaCode("recaptcha", cf, link);
                                rc.setCode(c);
                                final Form captchaForm2 = br.getFormbyProperty("name", "form_captcha");
                                id = br.getRegex("challenge\\?k=(.+?)\"").getMatch(0);
                                if (captchaForm2 != null && id == null) {
                                    logger.info("Form found but no ID");
                                    defect = true;
                                    logger.info("PluginError 672");
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                }
                                if (id != null) {
                                    /* captcha wrong */
                                    logger.info("reCaptcha captcha wrong");
                                    continue;
                                }
                            } catch (final PluginException e) {
                                if (defect) {
                                    throw e;
                                }
                                /**
                                 * captcha input timeout run out.. try to reconnect
                                 */
                                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Try reconnect to avoid more captchas", 5 * 60 * 1000l);
                            }
                        }
                    } else if (captchaForm.containsHTML("for=\"customCaptchaCheckbox\"")) {
                        /* Mediafire custom checkbox "captcha" */
                        captchaForm.put("mf_captcha_response", "1");
                        br.submitForm(captchaForm);
                        if (br.getFormbyProperty("name", "form_captcha") != null) {
                            logger.info("custom captcha wrong");
                            continue;
                        }
                    } else {
                        br.submitForm(captchaForm);
                    }
                } else {
                    logger.info("Didn't find captchaForm -> Captcha not needed or already solved");
                }
            }
            captchaCorrect = true;
            if (finalDownloadurl == null) {
                this.handlePW(link);
                finalDownloadurl = br.getRegex("kNO\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
                logger.info("Kno= " + finalDownloadurl);
                if (finalDownloadurl == null) {
                    /* pw protected files can directly redirect to download */
                    finalDownloadurl = br.getRedirectLocation();
                }
                if (finalDownloadurl == null) {
                    finalDownloadurl = br.getRegex("href\\s*=\\s*\"(https?://[^\"]+)\"\\s*id\\s*=\\s*\"downloadButton\"").getMatch(0);
                    if (finalDownloadurl == null) {
                        finalDownloadurl = br.getRegex("(" + TYPE_DIRECT + ")").getMatch(0);
                    }
                }
            }
            trycounter++;
        } while (trycounter < max_number_of_free_retries && finalDownloadurl == null);
        if (finalDownloadurl == null) {
            if (!captchaCorrect) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalDownloadurl, true, -15);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            handleServerErrors();
            logger.info("Error (3)");
            handleNonAPIErrors(link, br);
            if (br.containsHTML("We apologize, but we are having difficulties processing your download request")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Please be patient while we try to repair your download request", 2 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403, ", 30 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404, ", 30 * 60 * 1000l);
        }
    }

    private void handleExtraReconnectSettingOnCaptcha(final Account account) throws PluginException {
        if (this.getPluginConfig().getBooleanProperty(FREE_FORCE_RECONNECT_ON_CAPTCHA, false)) {
            if (account != null) {
                logger.info("Captcha reconnect setting active & free account used --> TEMPORARILY_UNAVAILABLE");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Waiting some time to avoid captcha in free account mode", 30 * 60 * 1000l);
            } else {
                logger.info("Captcha reconnect setting active & NO account used --> IP_BLOCKED");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Reconnecting or waiting some time to avoid captcha in free mode", 30 * 60 * 1000l);
            }
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFUID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFUID(final DownloadLink link) {
        String fileID = new Regex(link.getDownloadURL(), "https?://.*?/(file(?:_premium)?|file\\.php|download|download\\.php)/?\\??([a-zA-Z0-9]+)").getMatch(1);
        if (fileID == null) {
            fileID = new Regex(link.getDownloadURL(), "\\?([a-zA-Z0-9]+)").getMatch(0);
            if (fileID == null) {
                fileID = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            }
        }
        return fileID;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (link.getBooleanProperty("privatefolder")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, PRIVATEFOLDERUSERTEXT);
        }
        login(br, account, false);
        if (account.getType() == AccountType.FREE) {
            doFree(link, account);
        } else {
            apiCommand(account, "file/get_links.php", "link_type=direct_download&quick_key=" + getFUID(link));
            final String url = PluginJSonUtils.getJsonValue(brAPI, "direct_download");
            if (url == null) {
                // you can error under success.....
                // {"response":{"action":"file\/get_links","links":[{"quickkey":"removed","error":"User lacks
                // permissions"}],"result":"Success","current_api_version":"1.5"}}
                if (StringUtils.equalsIgnoreCase(PluginJSonUtils.getJson(brAPI, "error"), "User lacks permissions")) {
                    throw new AccountRequiredException("Incorrect account been used to download this file");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                handleServerErrors();
                logger.info("Error (4)");
                logger.info(dl.getConnection() + "");
                handleNonAPIErrors(link, br);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.setFilenameFix(true);
            dl.startDownload();
        }
    }

    private void handlePW(final DownloadLink link) throws Exception {
        final String label = "aria-labelledby\\s*=\\s*\"passwordmsg\"|class\\s*=\\s*\"passwordPrompt\"";
        if (br.containsHTML(label)) {
            logger.info("Handle possible PW");
            new PasswordSolver(this, br, link) {
                String curPw = null;

                @Override
                protected void handlePassword(final String password) throws Exception {
                    curPw = password;
                    final Form form = getPasswordForm();
                    if (form == null) {
                        logger.warning("Failed to find passwordForm");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    form.put("downloadp", Encoding.urlEncode(curPw));
                    final boolean followRedirect = br.isFollowingRedirects();
                    try {
                        br.setFollowRedirects(false);
                        br.submitForm(form);
                    } finally {
                        br.setFollowRedirects(followRedirect);
                    }
                }

                @Override
                protected boolean isCorrect() {
                    Form form = getPasswordForm();
                    if (form != null) {
                        return false;
                    } else {
                        return true;
                    }
                }

                protected Form getPasswordForm() {
                    Form form = br.getFormByRegex(label);
                    if (form != null) {
                        return form;
                    }
                    form = br.getFormbyProperty("name", "download");
                    if (form != null && !form.containsHTML(label)) {
                        if (form.getInputField("downloadp") != null) {
                            logger.warning("Maybe passwordform(?)");
                            return form;
                        } else {
                            logger.warning("Wrong passwordform(?) --> Returning null");
                            return null;
                        }
                    }
                    return form;
                }
            }.run();
        }
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    public void login(final Browser brLogin, final Account account, boolean force) throws Exception {
        boolean red = brLogin.isFollowingRedirects();
        try {
            // at this stage always trusting cookies
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                brLogin.setCookies(this.getHost(), cookies);
                if (!force) {
                    logger.info("Trust cookie without check");
                    return;
                }
                logger.info("Checking cookie validity");
                try {
                    apiCommand(account, "user/get_info.php", null);
                    final Map<String, Object> entries = restoreFromString(brAPI.toString(), TypeRef.MAP);
                    final String email = (String) JavaScriptEngineFactory.walkJson(entries, "response/user_info/email");
                    if (StringUtils.equalsIgnoreCase(email, account.getUser())) {
                        logger.info("Cookie login successful");
                        account.saveCookies(brLogin.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                } catch (final PluginException ignore) {
                    logger.exception("API returned error -> Full login required", ignore);
                }
            }
            logger.info("Performing full login");
            this.setBrowserExclusive();
            brLogin.setFollowRedirects(true);
            if (!isValidMailAdress(account.getUser())) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte trage deine E-Mail Adresse in das 'Name'-Feld ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the 'Name'-field.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            brLogin.getPage("https://www." + this.getHost() + "/login/");
            // some shit that happens in javascript.
            final Browser brLogin2 = brLogin.cloneBrowser();
            brLogin2.getPage("/templates/login_signup/login_signup.php?dc=loginPath");
            Form form = brLogin2.getFormbyProperty("id", "form_login1");
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (form.getAction() == null) {
                form.setAction("/dynamic/client_login/mediafire.php");
            }
            final String security = brLogin2.getRegex("security:\"([^\"]+)").getMatch(0);
            if (security != null) {
                form.put("security", security);
            }
            /* We want to get long lasting cookies! */
            form.put("login_remember", "true");
            form.put("login_email", Encoding.urlEncode(account.getUser()));
            form.put("login_pass", Encoding.urlEncode(account.getPass()));
            // submit via the same browser
            brLogin2.submitForm(form);
            /* 2021-04-29: This might return an error via json but as long as we get the cookie all is fine! */
            final String cookie = brLogin2.getCookie("http://www.mediafire.com", "user", Cookies.NOTDELETEDPATTERN);
            if (cookie == null || cookie.equals("x")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            // now with session token we can get info about the account from the web-api hybrid
            brLogin.setFollowRedirects(false);
            brLogin.getPage("/myaccount/");
            initApi(account);
            apiCommand(account, "user/get_info.php", null);
            // to determine free | premium this is done via above request
            final String accounType = PluginJSonUtils.getJsonValue(brAPI, "premium");
            if (StringUtils.equalsIgnoreCase(accounType, "no")) {
                account.setType(AccountType.FREE);
            } else {
                account.setType(AccountType.PREMIUM);
            }
            account.saveCookies(brLogin.getCookies(this.getHost()), "");
        } catch (PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        } finally {
            brLogin.setFollowRedirects(red);
        }
    }

    private void initApi(final Account account) throws Exception {
        if (brAPI == null) {
            brAPI = br.cloneBrowser();
        }
        if (sessionToken == null) {
            sessionToken = brAPI.getRegex("parent\\.bqx\\(\"([a-f0-9]+)\"\\)").getMatch(0);
            if (sessionToken == null && br.getRequest() != null) {
                sessionToken = br.getRegex("LoadIframeLightbox\\('/templates/tos\\.php\\?token=([a-f0-9]+)").getMatch(0);
            }
            if (sessionToken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty("session_token", sessionToken);
        }
        apiCommand(account, "device/get_status.php", null);
    }

    private void apiCommand(final Account account, final String command, final String args) throws Exception {
        if (sessionToken == null) {
            if (account != null) {
                sessionToken = account.getStringProperty("session_token", null);
                if (sessionToken == null) {
                    // relogin
                    login(br, account, true);
                    sessionToken = account.getStringProperty("session_token", null);
                }
            }
            if (sessionToken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        brAPI = br.cloneBrowser();
        brAPI.setAllowedResponseCodes(403);
        brAPI.getHeaders().put("Accept", "*/*");
        brAPI.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // website still uses 1.4, api is up to 1.5 at this stage -raztoki20160101
        String a = "https://www.mediafire.com/api/1.4/" + command + "?r=" + getRandomFourLetters() + (args != null ? "&" + args : "") + "&session_token=" + sessionToken + "&response_format=json";
        brAPI.getPage(a);
        // is success ?
        handleApiError(account);
    }

    private void handleApiError(final Account account) throws PluginException {
        // FYI you can have errors even though it's success
        if (!StringUtils.equalsIgnoreCase(PluginJSonUtils.getJsonValue(brAPI, "result"), "Success")) {
            if (StringUtils.equalsIgnoreCase(PluginJSonUtils.getJsonValue(brAPI, "result"), "Error")) {
                // error handling
                final String error = PluginJSonUtils.getJsonValue(brAPI, "error");
                switch (Integer.parseInt(error)) {
                case 105:
                    sessionToken = null;
                    if (account != null) {
                        account.setProperty("session_token", Property.NULL);
                        account.clearCookies("");
                    }
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                // offline file, to file/get_info as a single file... we need to return so the proper
                case 110:
                    // invalid uid
                case 111:
                    return;
                default:
                    // unknown error!
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
    }

    private boolean handleLinkcheckingApiError(final Account account) throws PluginException {
        if (!StringUtils.equalsIgnoreCase(PluginJSonUtils.getJsonValue(brAPI, "result"), "Success")) {
            if (StringUtils.equalsIgnoreCase(PluginJSonUtils.getJsonValue(brAPI, "result"), "Error")) {
                // error handling
                final String error = PluginJSonUtils.getJsonValue(brAPI, "error");
                switch (Integer.parseInt(error)) {
                case 111:// invalid uid
                case 110:// Unknown or Invalid QuickKey
                    return true;
                default:
                    return false;
                }
            }
        }
        return false;
    }

    private String getRandomFourLetters() {
        final Random r = new Random();
        final String soup = "abcdefghijklmnopqrstuvwxyz";
        String v = "";
        for (int i = 0; i < 4; i++) {
            v = v + soup.charAt(r.nextInt(soup.length()));
        }
        return v;
    }

    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        final Account aa = AccountController.getInstance().getValidAccount(this);
        return checkLinks(urls, aa);
    }

    public boolean checkLinks(final DownloadLink[] urls, final Account account) {
        try {
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final Map<String, DownloadLink> linkMap = new HashMap<String, DownloadLink>();
            while (true) {
                links.clear();
                linkMap.clear();
                while (true) {
                    // maximum number of quickkeys allowed is 500.
                    if (links.size() == 100 || index == urls.length) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                sb.append("quick_key=");
                boolean addDelimiter = false;
                for (final DownloadLink dl : links) {
                    if (addDelimiter) {
                        sb.append(",");
                    } else {
                        addDelimiter = true;
                    }
                    final String id = getFUID(dl);
                    linkMap.put(id, dl);
                    sb.append(id);
                }
                if (account != null) {
                    apiCommand(account, "file/get_info.php", sb.toString());
                } else {
                    brAPI = br.cloneBrowser();
                    brAPI.setAllowedResponseCodes(new int[] { 400, 403 });
                    brAPI.getHeaders().put("Accept", "*/*");
                    brAPI.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    brAPI.getPage("https://www.mediafire.com/api/1.5/file/get_info.php" + "?r=" + getRandomFourLetters() + "&" + sb.toString() + "&response_format=json");
                    handleApiError(account);
                }
                final Map<String, Object> apiResponse = restoreFromString(brAPI.toString(), TypeRef.MAP);
                final List<Map<String, Object>> file_infos;
                Object infos = JavaScriptEngineFactory.walkJson(apiResponse, "response/file_infos");
                if (infos == null) {
                    infos = JavaScriptEngineFactory.walkJson(apiResponse, "response/file_info");
                }
                if (infos != null && infos instanceof List) {
                    file_infos = (List<Map<String, Object>>) infos;
                } else if (infos != null && infos instanceof Map) {
                    file_infos = new ArrayList<Map<String, Object>>();
                    file_infos.add((Map<String, Object>) infos);
                } else {
                    if (links.size() == 1) {
                        final DownloadLink dl = links.get(0);
                        // for invalid uid in arraylist.size == 1;
                        if (handleLinkcheckingApiError(account)) {
                            // we know that single result must be false!
                            dl.setAvailableStatus(AvailableStatus.FALSE);
                        } else {
                            // single result....
                            dl.setAvailableStatus(AvailableStatus.TRUE);
                        }
                        return true;
                    } else if (handleLinkcheckingApiError(account) && links.size() > 1) {
                        // all uids teh array are invalid.
                        file_infos = null;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (file_infos != null) {
                    for (final Map<String, Object> file_info : file_infos) {
                        final DownloadLink item = linkMap.remove(file_info.get("quickkey"));
                        if (item != null) {
                            final String name = (String) file_info.get("filename");
                            final Long size = JavaScriptEngineFactory.toLong(file_info.get("size"), -1);
                            final String hash = (String) file_info.get("hash");
                            final String privacy = (String) file_info.get("privacy");
                            final String pass = (String) file_info.get("password_protected");
                            String content_url = (String) JavaScriptEngineFactory.walkJson(file_info, "links/normal_download");
                            final String delete_date = (String) file_info.get("delete_date");
                            if (!StringUtils.isEmpty(name)) {
                                item.setFinalFileName(name);
                            }
                            if (size != null && size >= 0) {
                                item.setVerifiedFileSize(size);
                            }
                            if (!StringUtils.isEmpty(hash)) {
                                item.setHashInfo(HashInfo.parse(hash));
                            }
                            if (!StringUtils.isEmpty(privacy)) {
                                item.setProperty("privacy", privacy);
                            }
                            if (!StringUtils.isEmpty(pass) && PluginJSonUtils.parseBoolean(pass)) {
                                item.setPasswordProtected(true);
                            }
                            if (!StringUtils.isEmpty(content_url)) {
                                /*
                                 * 2020-02-27: This may sometimes fix encoding issues: https://board.jdownloader.org/showthread.php?t=83274
                                 */
                                content_url = content_url.replaceFirst("/file_premium/", "/file/");
                                item.setContentUrl(content_url);
                                item.setUrlDownload(content_url);
                            }
                            /* 2020-06-29: Some files will have all information given bur are deleted if delete_date exists! */
                            if (delete_date != null && delete_date.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                                item.setAvailableStatus(AvailableStatus.FALSE);
                            } else {
                                item.setAvailableStatus(AvailableStatus.TRUE);
                            }
                        }
                    }
                }
                for (final DownloadLink offline : linkMap.values()) {
                    // if some uids are invalid with valid results, invalids just don't return.. we can then set them as offline!
                    offline.setAvailableStatus(AvailableStatus.FALSE);
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            getLogger().log(e);
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        if (!checkLinks(new DownloadLink[] { link }) || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    private void handleNonAPIErrors(final DownloadLink dl, Browser imported) throws PluginException, IOException {
        // imported browser affects br so lets make a new browser just for error checking.
        Browser eBr = new Browser();
        // catch, and prevent a null imported browser
        if (imported == null) {
            imported = br.cloneBrowser();
        }
        if (imported != null) {
            eBr = imported.cloneBrowser();
        } else {
            // prob not required...
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Some errors are only provided if isFollowingRedirects==true. As this isn't always the case throughout the plugin, lets grab the
        // redirect page so we can use .containsHTML
        if (!eBr.isFollowingRedirects()) {
            if (eBr.getRedirectLocation() != null) {
                eBr.getPage(eBr.getRedirectLocation());
            }
        }
        // error checking below!
        if (eBr.getURL().matches(".+/error\\.php\\?errno=3(20|23|78|80|86|88).*?")) {
            // 320 = file is removed by the originating user or MediaFire.
            // 323 = Dangerous File Blocked.
            // 378 = File Removed for Violation (of TOS)
            // 380 = claimed by a copyright holder through a valid DMCA request
            // 386 = File Blocked for Violation.
            // 388 = identified as copyrighted work
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (eBr.getURL().matches(".+/error\\.php\\?errno=394.*?")) {
            /*
             * The file you attempted to download is an archive that is encrypted or password protected. MediaFire does not support
             * unlimited downloads of encrypted or password protected archives and the limit for this file has been reached. MediaFire
             * understands the need for users to transfer encrypted and secured files, we offer this service starting at $1.50 per month. We
             * have informed the owner that sharing of this file has been limited and how they can resolve this issue.
             */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Download not possible, retriction based on uploaders account");
        } else if (eBr.getURL().contains("mediafire.com/error.php?errno=382")) {
            dl.getLinkStatus().setStatusText("File Belongs to Suspended Account.");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (eBr.getURL().contains("mediafire.com/error.php?errno=999") && eBr.containsHTML("<div id=\"privateTitle\">This file is currently set to private.</div>")) {
            // note: error 999 can redirect into other error messages! eg. http://www.mediafire.com/error.php?errno=999 ->
            // http://www.mediafire.com/error.php?errno=320, with the file id it will hold it's place
            // /error.php?errno=999&quickkey=FUID&origin=download.
            // 999 = ...
            // 999 = This file is currently set to private.
            // When a resource is set to private by its owner, only the owner can access it. If you would like to request access to the
            // file, please log in to your account.
            // Link; 8609354739341.log; 47049765; jdlog://8609354739341
            dl.getLinkStatus().setStatusText("File has been set to private, only owner can download.");
            throw new PluginException(LinkStatus.ERROR_FATAL);
        } else if (eBr.containsHTML("class=\"error\\-title\">Temporarily Unavailable</p>")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is temporarily unavailable!", 30 * 60 * 1000l);
        } else if (eBr.containsHTML("class=\"error-title\">This download is currently unavailable<")) {
            // jdlog://7235652095341
            final String time = eBr.getRegex("we will retry your download again in (\\d+) seconds\\.<").getMatch(0);
            long t = ((time != null ? Long.parseLong(time) : 60) * 1000l) + 2;
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is temporarily unavailable!", t);
        }
    }

    private boolean isValidMailAdress(final String value) {
        return value.matches(".+@.+");
    }

    @Override
    public String getDescription() {
        return "JDownloader's mediafire.com plugin helps downloading files from mediafire.com.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FREE_FORCE_RECONNECT_ON_CAPTCHA, JDL.L("plugins.hoster.MediafireCom.FreeForceReconnectOnCaptcha", "Free mode: Reconnect if captcha input needed?\r\n<html><p style=\"color:#F62817\"><b>WARNING: This setting can prevent captchas but it can also lead to an infinite reconnect loop!</b></p></html>")).setDefaultValue(false));
    }

    @Override
    public void reset() {
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

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }
}