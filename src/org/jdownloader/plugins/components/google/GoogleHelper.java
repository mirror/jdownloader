package org.jdownloader.plugins.components.google;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.TextComponentInterface;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.dialogs.NewPasswordDialog;
import org.jdownloader.dialogs.NewPasswordDialogInterface;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.config.GoogleConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.translate._JDT;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jd.controlling.accountchecker.AccountCheckerThread;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInvalidException;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.GoogleService;

public class GoogleHelper {
    // private static final String COOKIES2 = "googleComCookies";
    private static final String META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39 = "<meta\\s+http-equiv=\"refresh\"\\s+content\\s*=\\s*\"(\\d+)\\s*;\\s*url\\s*=\\s*([^\"]+)";
    private static final String PROPERTY_ACCOUNT_user_agent                   = "user_agent";
    private Browser             br;
    private boolean             cacheEnabled                                  = true;

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    LogInterface logger = null;

    public LogInterface getLogger() {
        return logger;
    }

    public void setLogger(final LogInterface logger) {
        this.logger = logger;
    }

    private void log(String str) {
        LogInterface logger = getLogger();
        if (logger != null) {
            logger.info(str);
        }
    }

    public GoogleHelper(final Browser browser) {
        init(browser, null);
    }

    public GoogleHelper(final Browser browser, final LogInterface logger) {
        init(browser, logger);
    }

    private void init(final Browser browser, final LogInterface logger) {
        this.br = browser;
        Thread thread = Thread.currentThread();
        boolean forceUpdateAndBypassCache = thread instanceof AccountCheckerThread && ((AccountCheckerThread) thread).getJob().isForce();
        cacheEnabled = !forceUpdateAndBypassCache;
        this.logger = logger;
    }

    private void postPageFollowRedirects(Browser br, String url, UrlQuery post) throws IOException, InterruptedException {
        boolean before = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        int wait = 0;
        try {
            log("Google Login: POST " + url + " Data: " + post);
            br.postPage(url, post);
            url = null;
            if (br.getRedirectLocation() != null) {
                url = br.getRedirectLocation();
            }
            String[] redirect = br.getRegex(META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39).getRow(0);
            if (redirect != null) {
                url = Encoding.htmlDecode(redirect[1]);
                wait = Integer.parseInt(redirect[0]) * 1000;
            }
        } finally {
            br.setFollowRedirects(before);
        }
        if (url != null) {
            if (wait > 0) {
                Thread.sleep(wait);
            }
            getPageFollowRedirects(br, url);
        }
    }

    private void getPageFollowRedirects(Browser br, String url) throws IOException, InterruptedException {
        boolean before = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        try {
            int max = 20;
            int wait = 0;
            while (max-- > 0) {
                url = breakRedirects(url);
                if (url == null) {
                    break;
                }
                if (wait > 0) {
                    Thread.sleep(wait);
                }
                log("Google Login: GET " + url);
                br.getPage(url);
                url = null;
                if (br.getRedirectLocation() != null) {
                    url = br.getRedirectLocation();
                    continue;
                }
                String[] redirect = br.getRegex(META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39).getRow(0);
                if (redirect != null) {
                    url = Encoding.htmlDecode(redirect[1]);
                    wait = Integer.parseInt(redirect[0]) * 1000;
                }
            }
        } finally {
            br.setFollowRedirects(before);
        }
    }

    protected String breakRedirects(String url) throws MalformedURLException, IOException {
        if (StringUtils.isEmpty(url) || new URL(url).getHost().toLowerCase(Locale.ENGLISH).contains(getService().serviceName)) {
            return null;
        }
        return url;
    }

    private Browser prepBR(final Browser br) {
        br.setCookie("https://google.com", "PREF", "hl=en-GB");
        return br;
    }

    public static String getUserAgent() {
        final String cfgUserAgent = PluginJsonConfig.get(GoogleConfig.class).getUserAgent();
        if (StringUtils.isEmpty(cfgUserAgent) || cfgUserAgent.equalsIgnoreCase("JDDEFAULT")) {
            /* Return default */
            /*
             * 2020-06-19: Firefox Users will get their User-Agent via "Flag Cookies" addon on cookie import but Opera & Chrome users won't
             * which is why we'll use a Chrome User-Agent as default
             */
            /* Last updated: 2020-06-19 */
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 Safari/537.36";
        } else {
            /* Return user selection */
            return cfgUserAgent;
        }
    }

    private Thread showCookieLoginInformation(final String host) {
        final String serviceNameForUser;
        final String realhost;
        if (host.equals("youtube.com")) {
            realhost = "youtube.com";
            serviceNameForUser = "YouTube";
        } else if (host.equals("drive.google.com")) {
            /* For GoogleDrive downloads */
            realhost = "drive.google.com";
            serviceNameForUser = "Google Drive";
        } else {
            realhost = "google.com";
            serviceNameForUser = "Google";
        }
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String help_article_url = "https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions";
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = serviceNameForUser + " - Login";
                        message += "Hallo liebe(r) " + serviceNameForUser + " NutzerIn\r\n";
                        message += "Um deinen " + serviceNameForUser + " Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "Öffne " + realhost + " in deinem Browser und folge dieser Anleitung:\r\n";
                        message += help_article_url;
                    } else {
                        title = serviceNameForUser + " - Login";
                        message += "Hello dear " + serviceNameForUser + " user\r\n";
                        message += "Open " + realhost + " in your browser and follow these instructions:\r\n";
                        message += help_article_url;
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(help_article_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public void login(final Account account, final boolean forceLoginValidation) throws Exception {
        synchronized (account) {
            try {
                /*
                 * User-Agent handling (by priority): Prefer last saved User-Agent given via user cookies --> "Fallback to" User-Agent from
                 * user given cookies --> Fallback to User defined User-Agent via plugin setting
                 */
                final String userDefinedUserAgent = getUserAgent();
                this.br.setDebug(true);
                this.br.setCookiesExclusive(true);
                /* TODO: Do we still need this? */
                this.br.setCookie("https://google.com", "PREF", "hl=en-GB");
                /* 2020-06-19: Enable this if login is only possible via exported cookies and NOT via username & password! */
                /* 2020-06-19: Enabled cookie-only-login! */
                final boolean cookieLoginOnly = true;
                final Cookies userCookies = account.loadUserCookies();
                final Cookies lastSavedCookies = account.loadCookies("");
                if (cookieLoginOnly && userCookies == null) {
                    showCookieLoginInformation(account.getHoster());
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                /* Check stored cookies */
                if (lastSavedCookies != null || userCookies != null) {
                    if (userCookies != null) {
                        logger.info("Attempting to perform login with user cookies");
                        br.setCookies(userCookies);
                        /* No User-Agent given in users' cookies? Add User selected User-Agent */
                        if (!StringUtils.isEmpty(userCookies.getUserAgent())) {
                            logger.info("Using User-Agent given in user cookies: " + userCookies.getUserAgent());
                            /* Save User-Agent so it gets re-used next time */
                            account.setProperty(PROPERTY_ACCOUNT_user_agent, userCookies.getUserAgent());
                            /* No need to do this - User-Agent is already set above via setCookies! */
                            // br.getHeaders().put("User-Agent", userCookies.getUserAgent());
                        } else {
                            logger.info("Using user defined User-Agent: " + userDefinedUserAgent);
                            br.getHeaders().put("User-Agent", userDefinedUserAgent);
                        }
                    } else {
                        logger.info("Attempting to login with stored cookies");
                        br.setCookies(lastSavedCookies);
                        /*
                         * TODO: Handle this similar to loadCookies so that this property will return null if user changes his account
                         * credentials(?)
                         */
                        final String lastSavedUserAgent = account.getStringProperty(PROPERTY_ACCOUNT_user_agent, null);
                        if (userCookies != null && !StringUtils.isEmpty(lastSavedUserAgent)) {
                            logger.info("Using last saved User-Agent: " + lastSavedUserAgent);
                            br.getHeaders().put("User-Agent", lastSavedUserAgent);
                        } else {
                            logger.info("Using user defined User-Agent: " + userDefinedUserAgent);
                            br.getHeaders().put("User-Agent", userDefinedUserAgent);
                        }
                    }
                    if (isCacheEnabled() && hasBeenValidatedRecently(account) && !forceLoginValidation) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    br.setAllowedResponseCodes(new int[] { 400 });
                    if (validateCookies(account)) {
                        logger.info("Login with cookies successful");
                        validate(account);
                        /* Only store cookies if username via username + password has been used. */
                        if (userCookies != null) {
                            synchronized (account) {
                                displayAdditionalCookieLoginInformation(account);
                            }
                        } else {
                            account.saveCookies(br.getCookies(br.getHost()), "");
                        }
                        return;
                    } else {
                        logger.info("Login with stored cookies failed");
                        if (userCookies != null) {
                            /* Give up. We only got these cookies so login via username and password is not possible! */
                            logger.info("Login failed --> No password available but only cookies --> Give up");
                            account.removeProperty(PROPERTY_ACCOUNT_user_agent);
                            errorAccountInvalid(account);
                        }
                    }
                }
                /* Full login */
                /* TODO: Check if ANY if this code still works */
                logger.info("Attempting full login via website");
                /* Clear old cookies & headers */
                this.br.clearAll();
                prepBR(this.br);
                this.br.setFollowRedirects(true);
                logger.info("Using user defined User-Agent: " + userDefinedUserAgent);
                br.setHeader("User-Agent", userDefinedUserAgent);
                /* first call to google */
                getPageFollowRedirects(br, "https://accounts.google.com/ServiceLogin?uilel=3&service=" + Encoding.urlEncode(getService().serviceName) + "&passive=true&continue=" + Encoding.urlEncode(getService().continueAfterServiceLogin) + "&hl=en_US&ltmpl=sso");
                // Set-Cookie: GAPS=1:u14pnu_cVhnJlNpZ_xhGBJLeS1FDxA:R-JYyKg6DETne8XP;Path=/;Expires=Fri, 23-Jun-2017 13:04:05
                // GMT;Secure;HttpOnly;Priority=HIGH
                UrlQuery post = new UrlQuery();
                post.appendEncoded("GALX", br.getCookie("http://google.com", "GALX"));
                post.appendEncoded("continue", getService().continueAfterServiceLoginAuth);
                post.appendEncoded("service", getService().serviceName);
                post.appendEncoded("hl", "en");
                post.appendEncoded("utf8", "☃");
                post.appendEncoded("pstMsg", "1");
                post.appendEncoded("dnConn", "");
                post.appendEncoded("checkConnection", (getService().checkConnectionString));
                post.appendEncoded("checkedDomains", (getService().serviceName));
                post.appendEncoded("Email", (account.getUser()));
                post.appendEncoded("Passwd", (account.getPass()));
                post.appendEncoded("signIn", "Sign in");
                post.appendEncoded("PersistentCookie", "yes");
                post.appendEncoded("rmShown", "1");
                postPageFollowRedirects(br, "https://accounts.google.com/ServiceLoginAuth", post);
                main: while (true) {
                    Form[] forms = br.getForms();
                    String error = br.getRegex("<span color=\"red\">(.*?)</span>").getMatch(0);
                    if (StringUtils.isNotEmpty(error)) {
                        UIOManager.I().showErrorMessage(_JDT.T.google_error(error));
                    }
                    if (br.containsHTML("Please change your password")) {
                        Form changePassword = br.getFormbyAction("https://accounts.google.com/ChangePassword");
                        if (changePassword != null) {
                            CrossSystem.openURL("http://www.google.com/support/accounts/bin/answer.py?answer=46526");
                            NewPasswordDialog d = new NewPasswordDialog(UIOManager.LOGIC_COUNTDOWN, _JDT.T.google_password_change_title(), _JDT.T.google_password_change_message(account.getUser()), null, _GUI.T.lit_continue(), null);
                            d.setTimeout(5 * 60 * 1000);
                            NewPasswordDialogInterface handler = UIOManager.I().show(NewPasswordDialogInterface.class, d);
                            try {
                                handler.throwCloseExceptions();
                                changePassword.getInputField("Passwd").setValue(Encoding.urlEncode(handler.getPassword()));
                                changePassword.getInputField("PasswdAgain").setValue(Encoding.urlEncode(handler.getPasswordVerification()));
                                submitForm(br, changePassword);
                                if (!br.containsHTML("Please change your password")) {
                                    account.setPass(handler.getPassword());
                                }
                                continue;
                            } catch (DialogNoAnswerException e) {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Verify it's you: Email", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                            }
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Password change required", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        }
                    }
                    Form verifyItsYouByEmail = br.getFormByInputFieldKeyValue("challengetype", "RecoveryEmailChallenge");
                    if (verifyItsYouByEmail != null) {
                        String example = br.getRegex("<label.*?id=\"RecoveryEmailChallengeLabel\">.*?<span.*?>([^<]+)</span>.*?</label>").getMatch(0);
                        if (example == null) {
                            CrossSystem.openURL(br.getURL());
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Verify it's you: Email", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        } else {
                            InputDialog d = new InputDialog(0, _JDT.T.google_email_verification_title(), _JDT.T.google_email_verification_message(example.trim()), null, null, _GUI.T.lit_continue(), null) {
                                @Override
                                protected int getPreferredWidth() {
                                    return 400;
                                }
                            };
                            InputDialogInterface handler = UIOManager.I().show(InputDialogInterface.class, d);
                            try {
                                handler.throwCloseExceptions();
                                String email = handler.getText();
                                verifyItsYouByEmail.getInputField("emailAnswer").setValue(Encoding.urlEncode(email));
                                submitForm(br, verifyItsYouByEmail);
                                continue;
                            } catch (DialogNoAnswerException e) {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Verify it's you: Email", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                            }
                        }
                    }
                    if (br.containsHTML("privacyreminder")) {
                        // google wants you to accept the new privacy policy
                        CrossSystem.openURL("https://accounts.google.com/ServiceLogin?uilel=3&service=" + Encoding.urlEncode(getService().serviceName) + "&passive=true&continue=" + Encoding.urlEncode(getService().continueAfterServiceLogin) + "&hl=en_US&ltmpl=sso");
                        if (!UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _JDT.T.google_helper_privacy_update_title(), _JDT.T.google_helper_privacy_update_message(account.getUser()), null, _GUI.T.lit_continue(), null)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Privacy Reminder Required", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        }
                        while (true) {
                            postPageFollowRedirects(br, "https://accounts.google.com/ServiceLoginAuth", post);
                            if (br.containsHTML("privacyreminder")) {
                                CrossSystem.openURL("https://accounts.google.com/ServiceLogin?uilel=3&service=" + Encoding.urlEncode(getService().serviceName) + "&passive=true&continue=" + Encoding.urlEncode(getService().continueAfterServiceLogin) + "&hl=en_US&ltmpl=sso");
                                if (!UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _JDT.T.google_helper_privacy_update_title(), _JDT.T.google_helper_privacy_update_message_retry(account.getUser()), null, _GUI.T.lit_continue(), null)) {
                                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Privacy Reminder Required", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                                }
                            } else {
                                continue main;
                            }
                        }
                    }
                    Form form = this.br.getFormBySubmitvalue("Verify");
                    if (form == null) {
                        for (Form f : forms) {
                            if (f.getAction().startsWith("/signin/challenge") && !f.getAction().startsWith("/signin/challenge/skip")) {
                                form = f;
                            }
                        }
                    }
                    if (form != null) {
                        if ("SecondFactor".equals(form.getAction())) {
                            handle2FactorAuthSmsDeprecated(form);
                            continue;
                        } else if ("/signin/challenge".equals(form.getAction())) {
                            handle2FactorAuthSmsNew(form);
                            continue;
                        } else if (form.getAction().startsWith("/signin/challenge/")) {
                            handle2FactorAuthSmsNew2(form);
                            continue;
                        }
                    }
                    form = br.getFormByInputFieldKeyValue("Page", "PasswordSeparationSignIn");
                    if (form != null) {
                        form.put("Email", Encoding.urlEncode(account.getUser()));
                        form.put("Passwd", Encoding.urlEncode(account.getPass()));
                        form.put("hl", "en");
                        submitForm(br, form);
                        continue;
                    }
                    if (StringUtils.isNotEmpty(error)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, error, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    break;
                }
                // if (!br.getURL().matches("https?\\:\\/\\/accounts\\.google\\.com\\/CheckCookie\\?.*")) {
                //
                // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                //
                // }
                if (validateSuccessOLD()) {
                    account.saveCookies(br.getCookies(br.getURL()), "");
                    validate(account);
                    return;
                } else {
                    errorAccountInvalid(account);
                }
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private final String PROPERTY_HAS_SHOWN_ADDITIONAL_COOKIE_LOGIN_INFORMATION = "has_shown_additional_cookie_login_information";

    private void displayAdditionalCookieLoginInformation(final Account account) {
        if (account.getBooleanProperty(PROPERTY_HAS_SHOWN_ADDITIONAL_COOKIE_LOGIN_INFORMATION, false) == false) {
            final Thread thread = new Thread() {
                public void run() {
                    try {
                        String message = "";
                        final String title;
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            title = "Google Login - wichtige Information zum Cookie Login";
                            message += "! ! ! WICHTIGE INFORMATION ZUM GOOGLE COOKIE LOGIN ! ! !";
                            message += "\r\nWir empfehlen, die Session/Cookies, die du gerade in JDownloader eingegeben hast nicht weiter im Browser zu verwenden.";
                            message += "\r\nSolltest du diesen Hinweis missachten, werden deine Google Cookies in JDownloader höchstwahrscheinlich nach sehr kurzer Zeit ungültig.";
                            message += "\r\nWir empfehlen, vor dem Export der Cookies eine neue Session zu eröffnen, die danach nicht mehr im Browser verwendet wird z.B. durch Login in einem Inkognitofenster oder in einem separaten Browserprofil.";
                        } else {
                            title = "Google Login - additional cookie login information";
                            message += "! ! ! IMPORTANT INFORMATION ABOUT GOOGLE COOKIE LOGIN ! ! !";
                            message += "\r\nWe recommend not to use the same Google session/cookies which you've just entered in JDownloader in your browser!";
                            message += "\r\nIf you ignore this recommendation, the cookies in JDownloader may expire within a very short time.";
                            message += "\r\nWe recommend creating a separate Google session in a private browser window or separate browser profile which you are not actively using afterwards!";
                        }
                        final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                        dialog.setTimeout(300 * 1000);
                        final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                        ret.throwCloseExceptions();
                    } catch (final Throwable e) {
                        getLogger().log(e);
                    }
                };
            };
            thread.setDaemon(true);
            thread.start();
            account.setProperty(PROPERTY_HAS_SHOWN_ADDITIONAL_COOKIE_LOGIN_INFORMATION, true);
        }
    }

    public static void errorAccountInvalid(final Account account) throws AccountInvalidException {
        final Cookies userCookies = account.loadUserCookies();
        if (userCookies != null && !userCookies.isEmpty()) {
            account.removeProperty(PROPERTY_ACCOUNT_user_agent);
            if (account.hasEverBeenValid()) {
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
            } else {
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
            }
        } else {
            throw new AccountInvalidException();
        }
    }

    public boolean validateCookies(final Account account) throws IOException, InterruptedException, AccountInvalidException {
        logger.info("Validating cookies");
        /*
         * 2020-09-07: psp: I was unable to just use the google.com cookies for youtube so basically we now expect the user to import the
         * correct cookies for the service they want to use so usually either "google.com" or "youtube.com" coookies.
         */
        if (account.getHoster().equals("youtube.com")) {
            br.getPage("https://www.youtube.com/");
            br.followRedirect(true);
            boolean ret = br.containsHTML("\"key\"\\s*:\\s*\"logged_in\"\\s*,\\s*\"value\"\\s*:\\s*\"1\"");
            ret |= br.containsHTML("\"LOGGED_IN\"\\s*:\\s*true");
            return ret;
        } else if (account.getHoster().equals("drive.google.com")) {
            validateCookiesGoogleDrive(br, account);
            return true;
        } else {
            final boolean useTwoLoginValidations = false;
            if (useTwoLoginValidations) {
                /* Old check */
                getPageFollowRedirects(br, "https://accounts.google.com/CheckCookie?hl=en&checkedDomains=" + Encoding.urlEncode(getService().serviceName) + "&checkConnection=" + Encoding.urlEncode(getService().checkConnectionString) + "&pstMsg=1&chtml=LoginDoneHtml&service=" + Encoding.urlEncode(getService().serviceName) + "&continue=" + Encoding.urlEncode(getService().continueAfterCheckCookie) + "&gidl=CAA");
                if (validateSuccessOLD()) {
                    return true;
                } else {
                    logger.info("First cookie validation failed --> 2nd validation ...");
                    getPageFollowRedirects(br, "https://www.google.com/?gws_rd=ssl");
                    if (br.containsHTML("accounts\\.google\\.com/logout")) {
                        return true;
                    }
                }
            } else {
                /* New check */
                getPageFollowRedirects(br, "https://www.google.com/?gws_rd=ssl");
                if (br.containsHTML("accounts\\.google\\.com/logout")) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     *
     * @throws AccountInvalidException
     * @throws IOException
     */
    private void validateCookiesGoogleDrive(final Browser br, final Account account) throws AccountInvalidException, IOException {
        final boolean oldFollowRedirects = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            /** Important! Some URLs will redirect to accounts.google.com when not logged in or login cookies expired, others will not. */
            // br.getPage("https://drive.google.com/");
            br.getPage("https://drive.google.com/drive/my-drive");
            if (br.getURL().contains("accounts.google.com")) {
                logger.warning("Looks like GDrive login failed");
                GoogleHelper.errorAccountInvalid(account);
            } else {
                logger.info("Looks like GDrive login was successful");
            }
        } finally {
            br.setFollowRedirects(oldFollowRedirects);
        }
    }

    /**
     * Validates login via e.g.
     * https://accounts.google.com/CheckCookie?hl=en&checkedDomains=youtube&checkConnection=youtube%3A210%3A1&pstMsg
     * =1&chtml=LoginDoneHtml&service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue&gidl=CAA
     */
    protected boolean validateSuccessOLD() {
        return br.containsHTML("accounts/SetSID");
    }

    protected void validate(Account account) {
        account.setProperty("LAST_VALIDATE_" + getService().name(), System.currentTimeMillis());
    }

    protected boolean hasBeenValidatedRecently(Account account) {
        long lastValidated = account.getLongProperty("LAST_VALIDATE_" + getService().name(), -1);
        if (lastValidated > 0 && System.currentTimeMillis() - lastValidated < getValidatedCacheTimeout()) {
            return true;
        }
        return false;
    }

    protected long getValidatedCacheTimeout() {
        return 1 * 60 * 60 * 1000l;
    }

    private void handle2FactorAuthSmsDeprecated(Form form) throws Exception {
        // //*[@id="verifyText"]
        if (br.containsHTML("idv-delivery-error-container")) {
            // <div class="infobanner">
            // <p class="error-msg infobanner-content"
            // id="idv-delivery-error-container">
            // You seem to be having trouble getting your verification code.
            // Please try again later.
            // </p>
            // </div>
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "You seem to be having trouble getting your sms verification code.  Please try again later.");
        }
        String number = br.getRegex("<span\\s+class\\s*=\\s*\"twostepphone\".*?>(.*?)</span>").getMatch(0);
        if (number != null) {
            InputDialog d = new InputDialog(0, _JDT.T.Google_helper_2factor_sms_dialog_title(), _JDT.T.Google_helper_2factor_sms_dialog_msg(number.trim()), null, new AbstractIcon(IconKey.ICON_TEXT, 32), null, null);
            InputDialogInterface handler = UIOManager.I().show(InputDialogInterface.class, d);
            handler.throwCloseExceptions();
            InputField smsUserPin = form.getInputFieldByName("smsUserPin");
            smsUserPin.setValue(Encoding.urlEncode(handler.getText()));
            InputField persistentCookie = form.getInputFieldByName("PersistentCookie");
            persistentCookie.setValue(Encoding.urlEncode("on"));
            form.remove("smsSend");
            form.remove("retry");
            submitForm(br, form);
        } else {
            // new version implemented on 31th july 2015
            number = br.getRegex("<span\\s+class\\s*=\\s*\"twostepphone\".*?>(.*?)</span>").getMatch(0);
            InputDialog d = new InputDialog(0, _JDT.T.Google_helper_2factor_sms_dialog_title(), _JDT.T.Google_helper_2factor_sms_dialog_msg(number.trim()), null, new AbstractIcon(IconKey.ICON_TEXT, 32), null, null);
            InputDialogInterface handler = UIOManager.I().show(InputDialogInterface.class, d);
            handler.throwCloseExceptions();
            InputField smsUserPin = form.getInputFieldByName("smsUserPin");
            smsUserPin.setValue(Encoding.urlEncode(handler.getText()));
            InputField persistentCookie = form.getInputFieldByName("PersistentCookie");
            persistentCookie.setValue(Encoding.urlEncode("on"));
            form.remove("smsSend");
            form.remove("retry");
            submitForm(br, form);
        }
        handleIntersitial();
    }

    private void handle2FactorAuthSmsNew2(Form form) throws Exception {
        // //*[@id="verifyText"]
        if (br.containsHTML("idv-delivery-error-container")) {
            // <div class="infobanner">
            // <p class="error-msg infobanner-content"
            // id="idv-delivery-error-container">
            // You seem to be having trouble getting your verification code.
            // Please try again later.
            // </p>
            // </div>
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "You seem to be having trouble getting your sms verification code.  Please try again later.");
        }
        final String number = getPhonenumberCensored();
        InputDialog d = new InputDialog(0, _JDT.T.Google_helper_2factor_sms_dialog_title(), _JDT.T.Google_helper_2factor_sms_dialog_msg(number), null, new AbstractIcon(IconKey.ICON_TEXT, 32), null, null) {
            @Override
            protected void initFocus(JComponent focus) {
                // super.initFocus(focus);
            }

            @Override
            protected TextComponentInterface getSmallInputComponent() {
                final ExtTextField ttx = new ExtTextField();
                final String TEXT_NOT_TO_TOUCH = "G-";
                ttx.addKeyListener(this);
                ttx.addMouseListener(this);
                ttx.setText(TEXT_NOT_TO_TOUCH);
                ((AbstractDocument) ttx.getDocument()).setDocumentFilter(new DocumentFilter() {
                    @Override
                    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                        if (offset < TEXT_NOT_TO_TOUCH.length()) {
                            return;
                        }
                        super.insertString(fb, offset, string, attr);
                    }

                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                        if (offset < TEXT_NOT_TO_TOUCH.length()) {
                            length = Math.max(0, length - TEXT_NOT_TO_TOUCH.length());
                            offset = TEXT_NOT_TO_TOUCH.length();
                        }
                        super.replace(fb, offset, length, text, attrs);
                    }

                    @Override
                    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                        if (offset < TEXT_NOT_TO_TOUCH.length()) {
                            length = Math.max(0, length + offset - TEXT_NOT_TO_TOUCH.length());
                            offset = TEXT_NOT_TO_TOUCH.length();
                        }
                        if (length > 0) {
                            super.remove(fb, offset, length);
                        }
                    }
                });
                return ttx;
            }
        };
        InputDialogInterface handler = UIOManager.I().show(InputDialogInterface.class, d);
        handler.throwCloseExceptions();
        InputField smsUserPin = form.getInputFieldByName("Pin");
        String txt = handler.getText();
        if (txt.startsWith("G-")) {
            txt = txt.substring(2);
        }
        smsUserPin.setValue(Encoding.urlEncode(txt));
        InputField persistentCookie = form.getInputFieldByName("TrustDevice");
        persistentCookie.setValue(Encoding.urlEncode("on"));
        form.remove("smsSend");
        form.remove("retry");
        submitForm(br, form);
        handleIntersitial();
    }

    private void handle2FactorAuthSmsNew(Form form) throws Exception {
        // //*[@id="verifyText"]
        if (br.containsHTML("idv-delivery-error-container")) {
            // <div class="infobanner">
            // <p class="error-msg infobanner-content"
            // id="idv-delivery-error-container">
            // You seem to be having trouble getting your verification code.
            // Please try again later.
            // </p>
            // </div>
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "You seem to be having trouble getting your sms verification code.  Please try again later.");
        }
        final String number = getPhonenumberCensored();
        InputDialog d = new InputDialog(0, _JDT.T.Google_helper_2factor_sms_dialog_title(), _JDT.T.Google_helper_2factor_sms_dialog_msg(number), null, new AbstractIcon(IconKey.ICON_TEXT, 32), null, null) {
            @Override
            protected void initFocus(JComponent focus) {
                // super.initFocus(focus);
            }

            @Override
            protected TextComponentInterface getSmallInputComponent() {
                final ExtTextField ttx = new ExtTextField();
                final String TEXT_NOT_TO_TOUCH = "G-";
                ttx.addKeyListener(this);
                ttx.addMouseListener(this);
                ttx.setText(TEXT_NOT_TO_TOUCH);
                ((AbstractDocument) ttx.getDocument()).setDocumentFilter(new DocumentFilter() {
                    @Override
                    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                        if (offset < TEXT_NOT_TO_TOUCH.length()) {
                            return;
                        }
                        super.insertString(fb, offset, string, attr);
                    }

                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                        if (offset < TEXT_NOT_TO_TOUCH.length()) {
                            length = Math.max(0, length - TEXT_NOT_TO_TOUCH.length());
                            offset = TEXT_NOT_TO_TOUCH.length();
                        }
                        super.replace(fb, offset, length, text, attrs);
                    }

                    @Override
                    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                        if (offset < TEXT_NOT_TO_TOUCH.length()) {
                            length = Math.max(0, length + offset - TEXT_NOT_TO_TOUCH.length());
                            offset = TEXT_NOT_TO_TOUCH.length();
                        }
                        if (length > 0) {
                            super.remove(fb, offset, length);
                        }
                    }
                });
                return ttx;
            }
        };
        InputDialogInterface handler = UIOManager.I().show(InputDialogInterface.class, d);
        handler.throwCloseExceptions();
        String txt = handler.getText();
        if (txt.startsWith("G-")) {
            txt = txt.substring(2);
        }
        form.remove("pin");
        form.put("pin", Encoding.urlEncode(txt));
        final InputField persistentCookie = form.getInputFieldByName("TrustDevice");
        if (persistentCookie != null) {
            persistentCookie.setValue(Encoding.urlEncode("on"));
        } else {
            form.put("TrustDevice", "on");
        }
        form.remove("smsSend");
        form.remove("retry");
        submitForm(br, form);
        handleIntersitial();
    }

    private String getPhonenumberCensored() {
        String number = br.getRegex("<b dir=\"ltr\">(.*?)</b>").getMatch(0);
        if (number == null) {
            number = this.br.getRegex("<b dir=\"ltr\" class=\"[^<>\"]+\">([^<>\"]*?)</b>").getMatch(0);
        }
        if (number != null) {
            number = number.trim();
        }
        return number;
    }

    protected void handleIntersitial() throws Exception {
        // Form[] forms = br.getForms();
        Form remind = br.getFormBySubmitvalue("Remind+me+later");
        if (remind != null && "SmsAuthInterstitial".equals(remind.getAction())) {
            remind.remove("addBackupPhone");
            submitForm(br, remind);
        }
    }

    private void submitForm(Browser br, Form form) throws Exception {
        boolean before = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        int wait = 0;
        String url = null;
        try {
            br.submitForm(form);
            if (br.getRedirectLocation() != null) {
                url = br.getRedirectLocation();
            }
            String[] redirect = br.getRegex(META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39).getRow(0);
            if (redirect != null) {
                url = Encoding.htmlDecode(redirect[1]);
                wait = Integer.parseInt(redirect[0]) * 1000;
            }
        } finally {
            br.setFollowRedirects(before);
        }
        if (url != null) {
            if (wait > 0) {
                Thread.sleep(wait);
            }
            getPageFollowRedirects(br, url);
        }
    }

    private String getText(Document doc, XPath xPath, String string) throws XPathExpressionException {
        Node n = (Node) xPath.evaluate(string, doc, XPathConstants.NODE);
        return (n != null ? n.getFirstChild().getTextContent().trim() : null);
    }

    private GoogleService service = GoogleService.YOUTUBE;

    public GoogleService getService() {
        return service;
    }

    public void setService(GoogleService service) {
        this.service = service;
    }

    private boolean isCacheEnabled() {
        return cacheEnabled;
    }
    // public void followRedirect() throws IOException, InterruptedException {
    // int wait = 0;
    // String url = null;
    // if (br.getRedirectLocation() != null) {
    // url = br.getRedirectLocation();
    //
    // }
    //
    // String[] redirect = br.getRegex(META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39).getRow(0);
    // if (redirect != null) {
    // url = Encoding.htmlDecode(redirect[1]);
    // wait = Integer.parseInt(redirect[0]) * 1000;
    // }
    //
    // if (url != null) {
    // if (wait > 0) {
    // Thread.sleep(wait);
    // }
    // getPageFollowRedirects(br, url, false);
    //
    // }
    // }

    /**
     * See https://stackoverflow.com/questions/16907352/reverse-engineering-javascript-behind-google-button
     *
     * @throws PluginException
     */
    public static String getSAPISidHash(final Browser br, final String url) throws PluginException {
        String sapisid = null;
        /* Get that cookie from any domain [most commonly google.com or youtube.com]. */
        final HashMap<String, Cookies> cookiemap = br.getCookies();
        final Iterator<Entry<String, Cookies>> iterator = cookiemap.entrySet().iterator();
        itloop: while (iterator.hasNext()) {
            final Entry<String, Cookies> entry = iterator.next();
            final Cookies cookies = entry.getValue();
            for (final Cookie cookie : cookies.getCookies()) {
                if (cookie.getKey().equalsIgnoreCase("SAPISID")) {
                    sapisid = cookie.getValue();
                    break itloop;
                }
            }
        }
        if (sapisid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final long timeMillis = System.currentTimeMillis() / 1000;
            return timeMillis + "_" + JDHash.getSHA1(timeMillis + " " + sapisid + " " + url);
        }
    }
}
