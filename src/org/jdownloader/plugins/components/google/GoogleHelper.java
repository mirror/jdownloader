package org.jdownloader.plugins.components.google;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

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
import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
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

import jd.controlling.AccountController;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.GoogleService;

public class GoogleHelper {
    // private static final String COOKIES2 = "googleComCookies";
    private static final String META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39 = "<meta\\s+http-equiv=\"refresh\"\\s+content\\s*=\\s*\"(\\d+)\\s*;\\s*url\\s*=\\s*([^\"]+)";
    private Browser             br;
    private boolean             cacheEnabled                                  = true;

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    LogInterface logger = null;

    public LogInterface getLogger() {
        return logger;
    }

    public void setLogger(LogInterface logger) {
        this.logger = logger;
    }

    private void log(String str) {
        LogInterface logger = getLogger();
        if (logger != null) {
            logger.info(str);
        }
    }

    public GoogleHelper(Browser ytbr) {
        this.br = ytbr;
        Thread thread = Thread.currentThread();
        boolean forceUpdateAndBypassCache = thread instanceof AccountCheckerThread && ((AccountCheckerThread) thread).getJob().isForce();
        cacheEnabled = !forceUpdateAndBypassCache;
    }

    public boolean login(String type) {
        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(type);
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    try {
                        this.login(n);
                        if (n.isValid()) {
                            return true;
                        }
                    } catch (final Exception e) {
                        n.setValid(false);
                        return false;
                    }
                }
            }
        }
        return false;
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
        if (cfgUserAgent.equalsIgnoreCase("JDDEFAULT")) {
            /* Return default */
            return "JDownloader2";
        } else {
            /* Return user selection */
            return cfgUserAgent;
        }
    }

    public boolean login(Account account) throws Exception {
        /* TODO: Add logger output */
        try {
            /*
             * 2020-02-10: Disabled boolean again! Seems like Google Login is still working for some users.
             * https://svn.jdownloader.org/issues/86318
             */
            final boolean pluginBroken = false;
            if (pluginBroken) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nGoogle login is broken!\r\nA bugfix in the near future is very unlikely.\r\nSee svn.jdownloader.org/issues/86318 ", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String userAgent = getUserAgent();
            br.setHeader("User-Agent", userAgent);
            this.br.setDebug(true);
            this.br.setCookiesExclusive(true);
            // delete all cookies
            this.br.clearCookies(null);
            this.br.setCookie("https://google.com", "PREF", "hl=en-GB");
            final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass());
            /* Check stored cookies */
            /* TODO: Check password change --> When user all of the sudden changes PW field and inputs other cookies! */
            /* TODO: Save- and load cookies of all domains? */
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(cookies);
                if (isCacheEnabled() && hasBeenValidatedRecently(account) || !true) {
                    return true;
                }
                getPageFollowRedirects(br, "https://accounts.google.com/CheckCookie?hl=en&checkedDomains=" + Encoding.urlEncode(getService().serviceName) + "&checkConnection=" + Encoding.urlEncode(getService().checkConnectionString) + "&pstMsg=1&chtml=LoginDoneHtml&service=" + Encoding.urlEncode(getService().serviceName) + "&continue=" + Encoding.urlEncode(getService().continueAfterCheckCookie) + "&gidl=CAA");
                if (validateSuccess()) {
                    validate(account);
                    return true;
                }
            }
            /* If stored cookies fail, try user cookies --> Also important as user could change password(= cookies) at any time */
            if (userCookies != null) {
                if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    /* 2020-01-10: Devs only */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // logger.info("Attempting login via user cookies");
                /*
                 * Work with the cookies the user has provided --> In this case we cannot even login via password as we do not know the
                 * users' password!
                 */
                br.setCookies(userCookies);
                /* TODO: Set previously "maybe added via Cookies-object" User-Agent! */
                if (StringUtils.isEmpty(userCookies.getUserAgent())) {
                    br.getHeaders().put("User-Agent", userAgent);
                }
                if (isCacheEnabled() && hasBeenValidatedRecently(account)) {
                    return true;
                }
                /* TODO: Try to get username/mail and set it as account username in JD */
                getPageFollowRedirects(br, "https://accounts.google.com/CheckCookie?hl=en&checkedDomains=" + Encoding.urlEncode(getService().serviceName) + "&checkConnection=" + Encoding.urlEncode(getService().checkConnectionString) + "&pstMsg=1&chtml=LoginDoneHtml&service=" + Encoding.urlEncode(getService().serviceName) + "&continue=" + Encoding.urlEncode(getService().continueAfterCheckCookie) + "&gidl=CAA");
                /* TODO: Maybe prefer this as validation check? */
                boolean loggedIN = validateSuccess();
                if (!loggedIN) {
                    getPageFollowRedirects(br, "https://www.google.com/?gws_rd=ssl");
                    if (br.containsHTML("accounts\\.google\\.com/logout")) {
                        loggedIN = true;
                    }
                }
                if (loggedIN) {
                    // logger.info("user cookies login successful");
                    validate(account);
                    /* TODO: Save all cookies of all domains */
                    saveCookiesOnAccount(account);
                    return true;
                } else {
                    // logger.info("user login cookies login failed");
                    /* Give up. We only got these cookies so login via username and password is not possible! */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            this.br.clearCookies(null);
            prepBR(this.br);
            this.br.setFollowRedirects(true);
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
            if (validateSuccess()) {
                saveCookiesOnAccount(account);
                validate(account);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            account.clearCookies("");
            throw e;
        }
    }

    private void saveCookiesOnAccount(final Account account) {
        account.saveCookies(br.getCookies(br.getURL()), "");
    }

    /**
     * Validates login via e.g.
     * https://accounts.google.com/CheckCookie?hl=en&checkedDomains=youtube&checkConnection=youtube%3A210%3A1&pstMsg
     * =1&chtml=LoginDoneHtml&service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue&gidl=CAA
     */
    protected boolean validateSuccess() {
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
}
