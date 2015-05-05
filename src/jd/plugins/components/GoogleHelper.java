package jd.plugins.components;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class GoogleHelper {

    private static final String COOKIES2                                      = "googleComCookies";
    private static final String META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39 = "<meta\\s+http-equiv=\"refresh\"\\s+content\\s*=\\s*\"(\\d+)\\s*;\\s*url\\s*=\\s*([^\"]+)";
    private Browser             br;
    private boolean             cacheEnabled                                  = true;

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public GoogleHelper(Browser ytbr) {
        this.br = ytbr;

    }

    public void login(String type) {

        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(type);
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {

                    try {

                        this.login(n);
                        if (n.isValid()) {
                            return;
                        }
                    } catch (final Exception e) {

                        n.setValid(false);
                        return;
                    }

                }
            }
        }
        return;
    }

    private void postPageFollowRedirects(Browser br, String url, LinkedHashMap<String, String> post) throws IOException, InterruptedException {
        boolean before = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        int wait = 0;
        try {
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
                if (url == null || new URL(url).getHost().toLowerCase(Locale.ENGLISH).contains(getService().serviceName)) {
                    break;
                }
                if (wait > 0) {
                    Thread.sleep(wait);
                }

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

    public boolean login(Account account) throws Exception {

        try {
            this.br.setDebug(true);
            this.br.setCookiesExclusive(true);
            // delete all cookies
            this.br.clearCookies(null);

            br.setCookie("http://google.com", "PREF", "hl=en-GB");
            if (isCacheEnabled() && account.getProperty(COOKIES2) != null) {

                @SuppressWarnings("unchecked")
                HashMap<String, String> cookies = (HashMap<String, String>) account.getProperty(COOKIES2);

                if (cookies != null) {
                    if (cookies.containsKey("SID") && cookies.containsKey("HSID")) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("google.com", key, value);
                        }
                        if (hasBeenValidatedRecently(account)) {
                            return true;
                        }
                        getPageFollowRedirects(br, "https://accounts.google.com/CheckCookie?hl=en&checkedDomains=" + Encoding.urlEncode(getService().serviceName) + "&checkConnection=" + Encoding.urlEncode(getService().checkConnectionString) + "&pstMsg=1&chtml=LoginDoneHtml&service=" + Encoding.urlEncode(getService().serviceName) + "&continue=" + Encoding.urlEncode(getService().continueAfterCheckCookie) + "&gidl=CAA");
                        if (br.containsHTML("accounts/SetSID")) {
                            validate(account);
                            return true;
                        }
                    }
                }
            }

            this.br.setFollowRedirects(true);
            /* first call to google */

            getPageFollowRedirects(br, "https://accounts.google.com/ServiceLogin?uilel=3&service=" + Encoding.urlEncode(getService().serviceName) + "&passive=true&continue=" + Encoding.urlEncode(getService().continueAfterServiceLogin) + "&hl=en_US&ltmpl=sso");

            LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();

            post.put("GALX", br.getCookie("http://google.com", "GALX"));
            post.put("continue", getService().continueAfterServiceLoginAuth);
            post.put("service", getService().serviceName);
            post.put("hl", "en");
            post.put("utf8", "☃");
            post.put("pstMsg", "1");
            post.put("dnConn", "");
            post.put("checkConnection", getService().checkConnectionString);

            post.put("checkedDomains", getService().serviceName);
            post.put("Email", account.getUser());
            post.put("Passwd", account.getPass());
            post.put("signIn", "Sign in");
            post.put("PersistentCookie", "yes");
            post.put("rmShown", "1");

            postPageFollowRedirects(br, "https://accounts.google.com/ServiceLoginAuth", post);
            Form form = br.getFormBySubmitvalue("Verify");

            if (form != null && "SecondFactor".equals(form.getAction())) {
                handle2FactorAuthSms(form);
            }
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies cYT = this.br.getCookies("google.com");
            for (final Cookie c : cYT.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty(COOKIES2, cookies);
            if (br.containsHTML("accounts/SetSID")) {
                validate(account);
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            account.setProperty(COOKIES2, null);
            throw e;
        }

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

    private void handle2FactorAuthSms(Form form) throws Exception {
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
        InputDialog d = new InputDialog(0, _JDT._.Google_helper_2factor_sms_dialog_title(), _JDT._.Google_helper_2factor_sms_dialog_msg(number.trim()), null, new AbstractIcon(IconKey.ICON_TEXT, 32), null, null);
        InputDialogInterface handler = UIOManager.I().show(InputDialogInterface.class, d);
        handler.throwCloseExceptions();
        InputField smsUserPin = form.getInputFieldByName("smsUserPin");
        smsUserPin.setValue(handler.getText());
        InputField persistentCookie = form.getInputFieldByName("PersistentCookie");
        persistentCookie.setValue("on");
        form.remove("smsSend");
        form.remove("retry");
        submitForm(br, form);

        handleIntersitial();
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

}
