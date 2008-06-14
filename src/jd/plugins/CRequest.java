//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jd.parser.Form;
import jd.parser.Regex;
import jd.utils.JDUtilities;

/**
 * CRequest verwaltet Cookies und Referrer automatisch
 * 
 * @author DwD
 * 
 */
public class CRequest {

    private RequestInfo requestInfo;

    /**
     * ist für getRequest und postRequest
     */
    public boolean withHtmlCode = true;

    /**
     * ob automatischen weiterleitungen gefolgt werden soll
     */
    public boolean redirect = true;

    /**
     * Interner Cookie bestehend aus host, CookieString
     */
    private HashMap<String, HashMap<String, String>> cookie = new HashMap<String, HashMap<String, String>>();

    public RequestInfo getRequestInfo() {
        return requestInfo;
    }

    public CRequest setRequestInfo(Form form) {
        try {
            return setRequestInfo(form.getRequestInfo());
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    /**
     * hier kann man die RequestInfo z.B.
     * cRequest.setRequestInfo(cRequest.getForm().getForm().getRequestInfo())
     * 
     * @param requestInfo
     */
    public CRequest setRequestInfo(RequestInfo requestInfo) {
        this.requestInfo = requestInfo;
        String host = getURL().getHost().replaceFirst("^www\\.", "");
        HashMap<String, String> clist = new HashMap<String, String>();
        try {
            clist.putAll(cookie.get(host));
        } catch (Exception e) {
            // TODO: handle exception
        }

        try {
            String[] bCookie = requestInfo.getCookie().split("; ");
            for (int i = 0; i < bCookie.length; i++) {
                if (!bCookie[i].matches("[\\s]*")) {
                    try {
                        String[] vals = new Regex(bCookie[i], "(.*?\\=)(.*)").getMatches()[0];
                        clist.put(vals[0], vals[1]);
                    } catch (Exception e) {
                        clist.put(bCookie[i], "");
                    }
                }

            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        this.cookie.put(host, clist);
        return this;
    }

    /**
     * getRequest gibt sich selbst aus Cookie und Referrer werden automatisch
     * gesetzt
     * 
     * @param url
     * @return
     */
    public CRequest getRequest(String url) {

        try {
            URL mURL = new URL(url);
            if (withHtmlCode)
                setRequestInfo(HTTP.getRequest(mURL, getCookie(mURL.getHost()), urlToString(), redirect));
            else
                setRequestInfo(HTTP.getRequestWithoutHtmlCode(mURL, getCookie(mURL.getHost()), urlToString(), redirect));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return this;
    }

    /**
     * postRequest gibt sich selbst aus Cookie und Referrer werden automatisch
     * gesetzt
     * 
     * @param url
     * @param parameter
     * @return
     */
    public CRequest postRequest(String url, String parameter) {

        try {
            URL mURL = new URL(url);
            if (withHtmlCode)
                setRequestInfo(HTTP.postRequest(mURL, getCookie(mURL.getHost()), urlToString(), null, parameter, redirect));
            else
                setRequestInfo(HTTP.postRequestWithoutHtmlCode(mURL, getCookie(mURL.getHost()), urlToString(), parameter, redirect));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return this;
    }

    /**
     * gibt die Forms der requestInfo aus
     * 
     * @param pattern
     * @return
     */
    public Form[] getForms() {
        try {
            return requestInfo.getForms();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    /**
     * gibt die erste Form der requestInfo aus
     * 
     * @return
     */
    public Form getForm() {
        try {
            return requestInfo.getForm();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    /**
     * Macht einen Regexp auf die requestInfo
     * 
     * @param pattern
     * @return
     */
    public Regex getRegexp(String pattern) {
        try {
            return requestInfo.getRegexp(pattern);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    /**
     * @return the connection
     */
    public HTTPConnection getConnection() {
        return requestInfo.getConnection();
    }

    /**
     * @return the URL
     */
    public URL getURL() {
        try {
            return getConnection().getURL();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public String getHost() {
        try {
            return getURL().getHost();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public String getLocation() {
        try {
            return requestInfo.getLocation();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    /**
     * @return URL.toString();
     */
    public String urlToString() {
        try {
            return getURL().toString();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    /**
     * gibt den Cookie aus
     * 
     * @return
     */
    public String getCookie() {
        try {
            return getCookie(getURL().getHost());
        } catch (Exception e) {
        }
        try {
            return requestInfo.getCookie();
        } catch (Exception ec) {
        }
        return null;
    }

    /**
     * gibt den Cookie für einen bestimmten Host aus
     * 
     * @param host
     * @return
     */
    public String getCookie(String host) {
        try {
            HashMap<String, String> c = new HashMap<String, String>();
            try {
                if (host.matches(".*?\\..*?\\..*?")) {
                    c.putAll(cookie.get(host.replaceFirst(".*?\\.", "")));
                }
            } catch (Exception e) {
            }
            try {
                c.putAll(cookie.get(host));
            } catch (Exception e) {
                // TODO: handle exception
            }
            String cookie = "";
            boolean last = false;
            for (Map.Entry<String, String> entry : c.entrySet()) {
                cookie += (last ? "; " : "") + entry.getKey() + entry.getValue();
                last = true;
            }
            return cookie;
        } catch (Exception e) {
        }
        return null;
    }

    public String getTitle() {
        try {
            return getRegexp("<title>(.*?)</title>").getFirstMatch();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return "";
    }

    public String getHtmlCode() {
        return toString();
    }

    public String toString() {
        try {
            return requestInfo.getHtmlCode();
        } catch (Exception ec) {
        }
        return null;
    }

    /**
     * sieht den aktuellen Request als CaptchaAdresse an, läd das CaptchaFile
     * herunter und gibt es mit CaptchaCode zurück
     * 
     * @param plugin
     * @return
     */
    public CaptchaInfo<File, String> getCaptchaCode(Plugin plugin) {
        boolean withHtmlCodeBak = withHtmlCode = false;
        HTTPConnection con = getConnection();
        String ct = con.getContentType().toLowerCase();
        if (ct != null && ct.contains("image/")) {
            ct = ct.replaceFirst("image/", "");
            if (ct.equals("jpeg")) ct = "jpg";
        } else {
            ct = "jpg";
        }
        File captchaFile = plugin.getLocalCaptchaFile(plugin, "." + ct);
        JDUtilities.download(captchaFile, con);
        withHtmlCode = withHtmlCodeBak;
        return new CaptchaInfo<File, String>(captchaFile, Plugin.getCaptchaCode(captchaFile, plugin));
    }

    /**
     * läd zur CaptchaAdress das CaptchaFile herunter und gibt es mit
     * CaptchaCode zurück
     * 
     * @param plugin
     * @return
     */
    public CaptchaInfo<File, String> getCaptchaCode(Plugin plugin, String captchaAdress) {
        CRequest re = clone();
        re.withHtmlCode = false;
        re.getRequest(captchaAdress);
        return re.getCaptchaCode(plugin);

    }

    public CRequest clone() {
        CRequest request = new CRequest();
        request.cookie = cookie;
        request.requestInfo = requestInfo;
        request.redirect = redirect;
        request.withHtmlCode = withHtmlCode;
        return request;
    }

    /**
     * CaptchaInfo besteht aus dem captchaFile und captchaCode
     * 
     * @author dwd
     * 
     * @param <captchaFile>
     * @param <captchaCode>
     */
    public class CaptchaInfo<captchaFile, captchaCode> {
        public captchaFile captchaFile;

        public captchaCode captchaCode;

        public CaptchaInfo(captchaFile captchaFile, captchaCode captchaCode) {
            this.captchaFile = captchaFile;
            this.captchaCode = captchaCode;
        }

        public String toString() {
            // TODO Auto-generated method stub
            return captchaCode.toString();
        }
    }
}
