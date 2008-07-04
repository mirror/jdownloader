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
import java.util.HashMap;
import java.util.Map;

import jd.parser.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.utils.JDUtilities;

public class Browser {
    private URL currentURL;
    private Request request;
    private int limit = 500 * 1024 * 1025;
    private boolean doRedirects = true;
    private HashMap<String, String> headers;
    public static HashMap<String, HashMap<String, String>> COOKIES = new HashMap<String, HashMap<String, String>>();

    public Browser() {

    }

    public String postPage(String url, String post) {

        return postPage(url, Request.parseQuery(post));
    }

    public String postPage(String url, HashMap<String, String> post) {
        try {
            if (currentURL == null) this.currentURL = new URL(url);
            PostRequest request = new PostRequest(url);
            request.setFollowRedirects(doRedirects);

            forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            request.getPostData().putAll(post);
            if (headers != null) request.getHeaders().putAll(this.headers);
            request.connect();
            String ret = null;
            if (request.getHttpConnection().getHeaderField("Content-Length") == null || (limit > 0 && Integer.parseInt(request.getHttpConnection().getHeaderField("Content-Length")) <= limit)) {
                ret = request.read();
            }

            updateCookies(request);
            this.request = request;
            this.currentURL = new URL(url);
            return ret;

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
    private HTTPConnection openPostConnection(String url, HashMap<String, String> post) {
        try {
            if (currentURL == null) this.currentURL = new URL(url);
            PostRequest request = new PostRequest(url);
            request.setFollowRedirects(doRedirects);

            forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            request.getPostData().putAll(post);
            if (headers != null) request.getHeaders().putAll(this.headers);
            request.connect();
          
            this.request = request;
            this.currentURL = new URL(url);
            return request.getHttpConnection();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

    public HTTPConnection openGetConnection(String string) {
        try {
            if (currentURL == null) this.currentURL = new URL(string);
            GetRequest request = new GetRequest(string);
            request.setFollowRedirects(doRedirects);
            forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            if (headers != null) request.getHeaders().putAll(this.headers);
            request.connect();

            updateCookies(request);
            this.request = request;
            this.currentURL = new URL(string);
            return request.getHttpConnection();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

    public String getPage(String string) {
        try {
            if (currentURL == null) this.currentURL = new URL(string);
            GetRequest request = new GetRequest(string);
            request.setFollowRedirects(doRedirects);
            forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            if (headers != null) request.getHeaders().putAll(this.headers);
            request.connect();
            String ret = null;
            if (request.getHttpConnection().getHeaderField("Content-Length") == null || Integer.parseInt(request.getHttpConnection().getHeaderField("Content-Length")) <= limit) {
                ret = request.read();
            }

            updateCookies(request);
            this.request = request;
            this.currentURL = new URL(string);
            return ret;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

    public static void forwardCookies(Request request) {
        if (request == null) return;
        String host = getHost(request.getUrl());
        HashMap<String, String> cookies = COOKIES.get(host);
        if (cookies == null) return;
        request.getCookies().putAll(cookies);

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    public static void updateCookies(Request request) {
        if (request == null) return;
        String host = getHost(request.getUrl());
        HashMap<String, String> cookies = COOKIES.get(host);
        if (cookies == null) {
            cookies = new HashMap<String, String>();
            COOKIES.put(host, cookies);
        }
        cookies.putAll(request.getCookies());

    }

    public Request getRequest() {
        // TODO Auto-generated method stub
        return request;
    }

    public void setCurrentURL(String string) {
        try {
            currentURL = new URL(string);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void setLoadLimit(int i) {
        this.limit = i;

    }

    public void setFollowRedirects(boolean b) {
        this.doRedirects = b;

    }

    public void setHeaders(HashMap<String, String> h) {
        this.headers = h;

    }

    public HashMap<String, String> getHeaders() {
        if (headers == null) headers = new HashMap<String, String>();
        return headers;
    }

    public static String getCookie(String url, String string) {
        String host;

        host = getHost(url);

        HashMap<String, String> cookies = COOKIES.get(host);
        return cookies.get(string);

    }

    public String getRedirectLocation() {
        if (request == null) return null;
        return request.getLocation();

    }

    public Form[] getForms() {
        try {
            return Form.getForms(getRequest().getRequestInfo());
        } catch (Exception e) {
            return null;
        }

    }

    public String submitForm(Form form) {

        String action = form.getAction();
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuffer stbuffer = new StringBuffer();
            boolean first = true;
            for (Map.Entry<String, String> entry : form.vars.entrySet()) {
                if (first)
                    first = false;
                else
                    stbuffer.append("&");
                stbuffer.append(entry.getKey());
                stbuffer.append("=");
                stbuffer.append(JDUtilities.urlEncode(entry.getValue()));
            }
            String varString = stbuffer.toString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+"))
                    action += "&";
                else if (action.matches("[^\\?]*")) action += "?";
                action += varString;
            }
            return this.getPage(action);

        case Form.METHOD_POST:

            return this.postPage(action, form.vars);
        }
        return null;
    }

    public String getPage(DownloadLink downloadLink) {
       return getPage(downloadLink.getDownloadURL());
    }

    public HTTPConnection openFormConnection(Form form) {
        String action = form.getAction();
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuffer stbuffer = new StringBuffer();
            boolean first = true;
            for (Map.Entry<String, String> entry : form.vars.entrySet()) {
                if (first)
                    first = false;
                else
                    stbuffer.append("&");
                stbuffer.append(entry.getKey());
                stbuffer.append("=");
                stbuffer.append(JDUtilities.urlEncode(entry.getValue()));
            }
            String varString = stbuffer.toString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+"))
                    action += "&";
                else if (action.matches("[^\\?]*")) action += "?";
                action += varString;
            }
            return this.openGetConnection(action);

        case Form.METHOD_POST:

            return this.openPostConnection(action, form.vars);
        }
        return null;
        
    }

 
}
