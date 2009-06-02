package jd.http;

import java.util.LinkedList;

public class Cookies {

    private LinkedList<Cookie> cookies = new LinkedList<Cookie>();

    public Cookies() {

    }

    public Cookies(Cookies cookies) {
        add(cookies);
    }

    public void add(Cookies newcookies) {
        synchronized (cookies) {
            for (Cookie cookie : newcookies.getCookies()) {
                add(cookie);
            }
        }
    }

    public void add(Cookie cookie) {
        synchronized (cookies) {
            for (Cookie cookie2 : cookies) {
                if (cookie2.equals(cookie)) {
                    cookie2 = cookie;
                    return;
                }
            }
            cookies.add(cookie);
        }
    }

    public void remove(Cookie cookie) {
        synchronized (cookies) {
            boolean b = cookies.remove(cookie);
            if (b == false) {
                Cookie del = null;
                for (Cookie cookie2 : cookies) {
                    if (cookie2.equals(cookie)) {
                        del = cookie2;
                        break;
                    }
                }
                if (del != null) cookies.remove(del);
            }
        }
    }

    public void clear() {
        synchronized (cookies) {
            cookies.clear();
        }
    }

    public Cookie get(String key) {
        if (key == null) return null;
        synchronized (cookies) {
            for (Cookie cookie : cookies) {
                if (cookie.getKey().equalsIgnoreCase(key)) return cookie;
            }
            return null;
        }
    }

    public LinkedList<Cookie> getCookies() {
        return cookies;
    }

    public boolean isEmpty() {
        return cookies.isEmpty();
    }

}
