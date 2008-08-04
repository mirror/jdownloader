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

package jd.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.utils.JDUtilities;

public class Browser {
    public static HashMap<String, HashMap<String, String>> COOKIES = new HashMap<String, HashMap<String, String>>();
private static final SimpleDateFormat DATE_FORMAT=new SimpleDateFormat("EEE, dd-MMM-yyyy hh:mm:ss z");
    public static void clearCookies(String string) {
        COOKIES.put(string, null);

    }

    public static void forwardCookies(Request request) {
        if (request == null) { return; }
        String host = Browser.getHost(request.getUrl());
        HashMap<String, String> cookies = COOKIES.get(host);
        if (cookies == null) { return; }
        
        if(cookies.containsKey("expires") && isExpired(cookies.get("expires"))){
            return;
        }   
        for(Iterator<Entry<String, String>> it = cookies.entrySet().iterator();it.hasNext();){
            Entry<String, String> cookie = it.next();
            
                     
            //Pfade sollten verarbeitet werden...TODO
            if(cookie.getKey().equalsIgnoreCase("path")||cookie.getKey().equalsIgnoreCase("expires")){
                continue;
            }
            request.getCookies().put(cookie.getKey(),cookie.getValue());
        }
        

    }

    private static boolean isExpired(String cookie) {
        if (cookie == null) return false;
       
        try {            
            return (new Date().compareTo(DATE_FORMAT.parse(cookie))) > 0;
        } catch (Exception e) {          
            return false;
        }
        }

    public static String getCookie(String url, String string) {
        String host;

        host = Browser.getHost(url);

        HashMap<String, String> cookies = COOKIES.get(host);
        return cookies.get(string);

    }

    public static String getHost(Object url) {
        try {
            String ret = new URL(url + "").getHost();
            int id = 0;
            while ((id = ret.indexOf(".")) != ret.lastIndexOf(".")) {
                ret = ret.substring(id + 1);

            }
            return ret;
        } catch (MalformedURLException e) {

            e.printStackTrace();
        }
        return null;

    }

    public static void updateCookies(Request request) {
        if (request == null) { return; }
        String host = Browser.getHost(request.getUrl());
        HashMap<String, String> cookies = COOKIES.get(host);
        if (cookies == null) {
            cookies = new HashMap<String, String>();
            COOKIES.put(host, cookies);
        }
        cookies.putAll(request.getCookies());

    }

    private String acceptLanguage = "de, en-gb;q=0.9, en;q=0.8";
    private int connectTimeout = -1;
    private URL currentURL;

    private boolean doRedirects = true;

    private HashMap<String, String> headers;

    private int limit = 500 * 1024 * 1025;

    private int readTimeout = -1;

    private Request request;

    public Browser() {

    }

    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public Form[] getForms() {
        try {
            return Form.getForms(getRequest().getRequestInfo());
        } catch (Exception e) {
            return null;
        }

    }

    public HashMap<String, String> getHeaders() {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        return headers;
    }

    public String getPage(DownloadLink downloadLink) {
        return getPage(downloadLink.getDownloadURL());
    }

    public String getPage(String string) {
        string=getURL(string);
        try {

            if (currentURL == null) {
                currentURL = new URL(string);
            }
            GetRequest request = new GetRequest(string);
            request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
            request.setFollowRedirects(doRedirects);
            Browser.forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            if (headers != null) {
                request.getHeaders().putAll(headers);
            }
            request.connect();
            String ret = null;
            if (request.getHttpConnection().getHeaderField("Content-Length") == null || Integer.parseInt(request.getHttpConnection().getHeaderField("Content-Length")) <= limit) {
                ret = request.read();
            }

            Browser.updateCookies(request);
            this.request = request;
            currentURL = new URL(string);
            return ret;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public String getRedirectLocation() {
        if (request == null) { return null; }
        String red = request.getLocation();
        if (red == null) return null;
        try {
            new URL(red);
        } catch (Exception e) {
            red = "http://" + request.getHttpConnection().getURL().getHost() + (red.charAt(0)=='/'?red:"/"+red);
        }
        return red;
    }

    public Request getRequest() {

        return request;
    }

    public HTTPConnection openFormConnection(Form form) {
        String action = form.getAction();
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuffer stbuffer = new StringBuffer();
            boolean first = true;
            for (Map.Entry<String, String> entry : form.vars.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    stbuffer.append("&");
                }
                stbuffer.append(entry.getKey());
                stbuffer.append("=");
                stbuffer.append(JDUtilities.urlEncode(entry.getValue()));
            }
            String varString = stbuffer.toString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+")) {
                    action += "&";
                } else if (action.matches("[^\\?]*")) {
                    action += "?";
                }
                action += varString;
            }
            return openGetConnection(action);

        case Form.METHOD_POST:

            return this.openPostConnection(action, form.vars);
        }
        return null;

    }

    public HTTPConnection openGetConnection(String string) {
        string = getURL(string);

        try {
            if (currentURL == null) {
                currentURL = new URL(string);
            }
            GetRequest request = new GetRequest(string);
            if (connectTimeout > 0) {
                request.setConnectTimeout(connectTimeout);
            }
            if (readTimeout > 0) {
                request.setReadTimeout(readTimeout);
            }
            request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
            request.setFollowRedirects(doRedirects);
            Browser.forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            if (headers != null) {
                request.getHeaders().putAll(headers);
            }
            request.connect();

            Browser.updateCookies(request);
            this.request = request;
            currentURL = new URL(string);
            return request.getHttpConnection();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

    private String getURL(String string) {
        if (string == null) string = this.getRedirectLocation();

        try {
            new URL(string);
        } catch (Exception e) {
            if (request == null || request.getHttpConnection() == null) return string;
            string = "http://" + request.getHttpConnection().getURL().getHost() + (string.charAt(0)=='/'?string:"/"+string);
        }
        return string;
    }

    private HTTPConnection openPostConnection(String url, HashMap<String, String> post) {
        url=getURL(url);
        try {
            if (currentURL == null) {
                currentURL = new URL(url);
            }
            PostRequest request = new PostRequest(url);
            request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
            request.setFollowRedirects(doRedirects);
            if (connectTimeout > 0) {
                request.setConnectTimeout(connectTimeout);
            }
            if (readTimeout > 0) {
                request.setReadTimeout(readTimeout);
            }
            Browser.forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            if (post != null) {
                request.getPostData().putAll(post);
            }
            if (headers != null) {
                request.getHeaders().putAll(headers);
            }
            request.connect();

            this.request = request;
            currentURL = new URL(url);
            return request.getHttpConnection();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

    public HTTPConnection openPostConnection(String url, String post) {

        return openPostConnection(url, Request.parseQuery(post));
    }

    public String postPage(String url, HashMap<String, String> post) {
        url=getURL(url);
        try {
            if (currentURL == null) {
                currentURL = new URL(url);
            }
            PostRequest request = new PostRequest(url);
            request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
            request.setFollowRedirects(doRedirects);
            if (connectTimeout > 0) {
                request.setConnectTimeout(connectTimeout);
            }
            if (readTimeout > 0) {
                request.setReadTimeout(readTimeout);
            }
            Browser.forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            request.getPostData().putAll(post);
            if (headers != null) {
                request.getHeaders().putAll(headers);
            }
            request.connect();
            String ret = null;
            if (request.getHttpConnection().getHeaderField("Content-Length") == null || limit > 0 && Integer.parseInt(request.getHttpConnection().getHeaderField("Content-Length")) <= limit) {
                ret = request.read();
            }

            Browser.updateCookies(request);
            this.request = request;
            currentURL = new URL(url);
            return ret;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;

    }

    public String postPage(String url, String post) {

        return postPage(url, Request.parseQuery(post));
    }

    public void setAcceptLanguage(String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setCurrentURL(String string) {
        try {
            currentURL = new URL(string);
        } catch (MalformedURLException e) {

            e.printStackTrace();
        }

    }

    public void setFollowRedirects(boolean b) {
        doRedirects = b;

    }

    public void setHeaders(HashMap<String, String> h) {
        headers = h;

    }

    public void setLoadLimit(int i) {
        limit = i;

    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String submitForm(Form form) {

        String action = form.getAction();
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuffer stbuffer = new StringBuffer();
            boolean first = true;
            for (Map.Entry<String, String> entry : form.vars.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    stbuffer.append("&");
                }
                stbuffer.append(entry.getKey());
                stbuffer.append("=");
                stbuffer.append(JDUtilities.urlEncode(entry.getValue()));
            }
            String varString = stbuffer.toString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+")) {
                    action += "&";
                } else if (action.matches("[^\\?]*")) {
                    action += "?";
                }
                action += varString;
            }
            return this.getPage(action);

        case Form.METHOD_POST:

            return this.postPage(action, form.vars);
        }
        return null;
    }

    @Override
    public String toString() {
        if (request == null) { return "Browser. no rquest yet"; }
        return request.toString();
    }

    public Regex getRegex(String string) {
        return new Regex(this, string);
    }

    public Regex getRegex(Pattern compile) {
        return new Regex(this, compile);
    }

    public boolean containsHTML(String fileNotFound) {
        return new Regex(this,fileNotFound).matches();
    }

}
