package jd.plugins;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.Storable;

import jd.http.Cookie;
import jd.http.Cookies;

public class CookiesStorable implements Storable {
    public String getUserAgent() {
        if (this.cookiesObject != null) {
            return this.cookiesObject.getUserAgent();
        }
        return this.userAgent;
    }

    public void setUserAgent(final String agent) {
        this.userAgent = agent;
    }

    public List<Cookie> getCookies() {
        if (this.cookiesObject != null) {
            return this.cookiesObject.getCookies();
        }
        final List<Cookie> cookiesArray = new ArrayList<Cookie>();
        for (final CookieStorable cookieStorable : cookies) {
            cookiesArray.add(cookieStorable._restore());
        }
        return cookiesArray;
    }

    private String             userAgent = null;
    final List<CookieStorable> cookies   = new ArrayList<CookieStorable>();
    private final Cookies      cookiesObject;

    public CookiesStorable(/* Storable */) {
        this.cookiesObject = null;
    }

    public CookiesStorable(Cookies cookies) {
        this.cookiesObject = cookies;
    }

    public Cookies _restore() {
        final Cookies ret = new Cookies();
        ret.setUserAgent(this.getUserAgent());
        final List<Cookie> cookies = getCookies();
        for (final Cookie cookie : cookies) {
            ret.add(cookie);
        }
        return ret;
    }
}