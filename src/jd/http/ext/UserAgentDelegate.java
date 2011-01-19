package jd.http.ext;

import java.net.URL;
import java.security.Policy;

import jd.http.Cookie;
import jd.http.Cookies;

import org.lobobrowser.html.HttpRequest;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLScriptElementImpl;

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
        return browser.getBrowserEnviroment().getAppCodeName();

    }

    public String getAppMinorVersion() {
        return browser.getBrowserEnviroment().getAppMinorVersion();

    }

    public String getAppName() {
        return browser.getBrowserEnviroment().getAppName();

    }

    public String getAppVersion() {
        // TODO Auto-generated method stub
        return browser.getBrowserEnviroment().getAppVersion();

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
        return browser.getBrowserEnviroment().getPlatform();
    }

    public String getProduct() {
        return browser.getBrowserEnviroment().getProduct();
    }

    public int getScriptingOptimizationLevel() {
        return 9;
    }

    /**
     * Returns <code>null</code>. This method must be overridden if JavaScript
     * code is untrusted.
     */
    public Policy getSecurityPolicy() {
        return browser.getBrowserEnviroment().getSecurityPolicy();
    }

    public String getUserAgent() {
        // TODO Auto-generated method stub
        return browser.getCommContext().getRequest().getHeaders().get("User-Agent");
    }

    public String getVendor() {
        return browser.getBrowserEnviroment().getVendor();
    }

    public boolean isCookieEnabled() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public boolean isExternalCSSEnabled() {
        // TODO Auto-generated method stub
        return browser.getBrowserEnviroment().isExternalCSSEnabled();
    }

    public boolean isMedia(String arg0) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public boolean isScriptingEnabled() {
        // TODO Auto-generated method stub
        return browser.getBrowserEnviroment().isScriptingEnabled();
    }

    /**
     * gets called if js sets a cookie
     */
    public void setCookie(URL arg0, String arg1) {
        // date is null..since js time is localtime anyway
        Cookies cookies = Cookies.parseCookies(arg1, arg0.getHost(), null);
        // cookies = cookies;
        browser.getCommContext().getCookies(arg0.toString()).add(cookies);

    }

    public boolean isInternalCSSEnabled() {
        // TODO Auto-generated method stub
        return browser.getBrowserEnviroment().isInternalCSSEnabled();
    }

    public String doScriptFilter(HTMLScriptElementImpl htmlScriptElementImpl, String text) {
        // TODO Auto-generated method stub
        return browser.getBrowserEnviroment().doScriptFilter(htmlScriptElementImpl, text);
    }

}
