package jd.http.ext;

import java.net.URL;
import java.security.Policy;

import jd.http.Cookie;
import jd.http.Cookies;

import org.appwork.utils.logging.Log;
import org.lobobrowser.html.HttpRequest;
import org.lobobrowser.html.UserAgentContext;

public class UserAgentDelegate implements UserAgentContext {

    private ExtBrowser browser;

    public UserAgentDelegate(ExtBrowser extBrowser) {
        browser = extBrowser;
    }

    public HttpRequest createHttpRequest() {
        // TODO Auto-generated method stub

        return new ExtHTTPRequest(browser);

    }

    public String getAppCodeName() {
        return browser.getUserAgent().getAppCodeName();

    }

    public String getAppMinorVersion() {
        return browser.getUserAgent().getAppMinorVersion();

    }

    public String getAppName() {
        return browser.getUserAgent().getAppName();

    }

    public String getAppVersion() {
        // TODO Auto-generated method stub
        return browser.getUserAgent().getAppVersion();

    }

    public String getBrowserLanguage() {
        // TODO Auto-generated method stub
        return browser.getCommContext().getAcceptLanguage();
    }

    public String getCookie(URL arg0) {
        // TODO Auto-generated method stub
        Cookies cookies = this.browser.getCommContext().getCookies(arg0 + "");
        StringBuilder c = new StringBuilder();
        boolean b = false;
        for (Cookie cookie : cookies.getCookies()) {
            if (b == true) {
                c.append("; ");
            } else
                b = true;
            c.append(cookie.getKey() + "=" + cookie.getValue());
        }
        return c.toString();
    }

    public String getPlatform() {
        // TODO Auto-generated method stub
        return browser.getUserAgent().getPlatform();
    }

    public String getProduct() {
        return browser.getUserAgent().getProduct();
    }

    public int getScriptingOptimizationLevel() {
        return 0;
    }

    /**
     * Returns <code>null</code>. This method must be overridden if JavaScript
     * code is untrusted.
     */
    public Policy getSecurityPolicy() {
        return browser.getUserAgent().getSecurityPolicy();
    }

    public String getUserAgent() {
        // TODO Auto-generated method stub
        return browser.getCommContext().getHeaders().get("User-Agent");
    }

    public String getVendor() {
        return browser.getUserAgent().getVendor();
    }

    public boolean isCookieEnabled() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public boolean isExternalCSSEnabled() {
        // TODO Auto-generated method stub
        return browser.getUserAgent().isExternalCSSEnabled();
    }

    public boolean isMedia(String arg0) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public boolean isScriptingEnabled() {
        // TODO Auto-generated method stub
        return browser.getUserAgent().isScriptingEnabled();
    }

    public void setCookie(URL arg0, String arg1) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public boolean isInternalCSSEnabled() {
        // TODO Auto-generated method stub
        return browser.getUserAgent().isInternalCSSEnabled();
    }

}
