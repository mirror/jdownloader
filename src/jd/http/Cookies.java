//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.http;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import jd.parser.Regex;

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
                    cookie2.update(cookie);
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

    public static Cookies parseCookies(String cookieString, String host, String Date) {
        Cookies cookies = new Cookies();

        String header = cookieString;

        String path = null;
        String expires = null;
        String domain = null;
        LinkedHashMap<String, String> tmp = new LinkedHashMap<String, String>();
        /* einzelne Cookie Elemente */
        StringTokenizer st = new StringTokenizer(header, ";");
        while (true) {

            String key = null;
            String value = null;
            String cookieelement = null;
            if (st.hasMoreTokens()) {
                cookieelement = st.nextToken().trim();
            } else {
                break;
            }
            /* Key and Value */
            String st2[] = new Regex(cookieelement, "(.*?)=(.+)").getRow(0);
            if (st2 == null || st2.length == 0) {
                key = null;
            } else if (st2.length == 1) {
                key = st2[0].trim();
            } else if (st2.length == 2) {
                key = st2[0].trim();
                value = st2[1].trim();
            }

            if (key != null) {
                if (key.equalsIgnoreCase("path")) {
                    path = value;
                    continue;
                }
                if (key.equalsIgnoreCase("expires")) {
                    expires = value;
                    continue;
                }
                if (key.equalsIgnoreCase("domain")) {
                    domain = value;
                    continue;
                }

                tmp.put(key, value);
            } else {
                break;
            }

        }

        for (Iterator<Entry<String, String>> it = tmp.entrySet().iterator(); it.hasNext();) {
            Entry<String, String> next = it.next();
            Cookie cookie = new Cookie();
            /*
             * cookies ohne value sind keine cookies
             */
            if (next.getValue() == null) continue;
            cookies.add(cookie);
            cookie.setHost(host);
            cookie.setPath(path);
            cookie.setDomain(domain);
            cookie.setExpires(expires);
            cookie.setValue(next.getValue());
            cookie.setKey(next.getKey());
            cookie.setHostTime(Date);
        }

        return cookies;

    }

}
