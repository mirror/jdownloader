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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;

public class Browser {
    public static HashMap<String, HashMap<String, String>> COOKIES = new HashMap<String, HashMap<String, String>>();

    public static void clearCookies(String string) {
        COOKIES.put(string, null);

    }

    public static void forwardCookies(Request request) {
        if (request == null) { return; }
        String host = Browser.getHost(request.getUrl());
        HashMap<String, String> cookies = COOKIES.get(host);
        if (cookies == null) { return; }

        if (cookies.containsKey("expires") && Request.isExpired(cookies.get("expires"))) { return; }
        for (Iterator<Entry<String, String>> it = cookies.entrySet().iterator(); it.hasNext();) {
            Entry<String, String> cookie = it.next();

            // Pfade sollten verarbeitet werden...TODO
            if (cookie.getKey().equalsIgnoreCase("path") || cookie.getKey().equalsIgnoreCase("expires") || cookie.getKey().equalsIgnoreCase("domain")) {
                continue;
            }
            request.getCookies().put(cookie.getKey(), cookie.getValue());
        }

    }

    public static void forwardCookies(HTTPConnection con) {
        if (con == null) { return; }
        String host = Browser.getHost(con.getURL().toString());
        HashMap<String, String> cookies = COOKIES.get(host);
        String cs = Request.getCookieString(cookies);
        if (cs != null && cs.trim().length() > 0) con.setRequestProperty("Cookie", cs);
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

    private boolean doRedirects = false;

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

            return Form.getForms(this);
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

    public String getPage(String string) {
        string = getURL(string);
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
            red = "http://" + request.getHttpConnection().getURL().getHost() + (red.charAt(0) == '/' ? red : "/" + red);
        }
        return red;
    }

    public Request getRequest() {

        return request;
    }

    public HTTPConnection openFormConnection(Form form) {

        String base = null;
        if (request != null) base = request.getUrl().toString();
        String action = form.getAction(base);
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuffer stbuffer = new StringBuffer();
            boolean first = true;
            for (Map.Entry<String, String> entry : form.getVars().entrySet()) {
                if (first) {
                    first = false;
                } else {
                    stbuffer.append("&");
                }
                stbuffer.append(entry.getKey());
                stbuffer.append("=");
                stbuffer.append(Encoding.urlEncode(entry.getValue()));
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

            return this.openPostConnection(action, form.getVars());
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
            string = "http://" + request.getHttpConnection().getURL().getHost() + (string.charAt(0) == '/' ? string : "/" + string);
        }
        return string;
    }

    private HTTPConnection openPostConnection(String url, HashMap<String, String> post) {
        url = getURL(url);
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
        url = getURL(url);
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
        String base = null;
        if (request != null) base = request.getUrl().toString();
        String action = form.getAction(base);
        switch (form.method) {

        case Form.METHOD_GET:
            StringBuffer stbuffer = new StringBuffer();
            boolean first = true;
            for (Map.Entry<String, String> entry : form.getVars().entrySet()) {
                if (first) {
                    first = false;
                } else {
                    stbuffer.append("&");
                }
                stbuffer.append(entry.getKey());
                stbuffer.append("=");
                stbuffer.append(Encoding.urlEncode(entry.getValue()));
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

            return this.postPage(action, form.getVars());

        case Form.METHOD_FILEPOST:

            HTTPPost up = new HTTPPost(action, doRedirects);
            up.doUpload();
            up.connect();

            up.getConnection().setRequestProperty("Accept", "*/*");
            up.getConnection().setRequestProperty("Accept-Language", acceptLanguage);
            up.getConnection().setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
            forwardCookies(up.getConnection());
            up.getConnection().setRequestProperty("Referer", currentURL.toString());
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                up.getConnection().setRequestProperty(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, String> entry : form.getVars().entrySet()) {
                up.sendVariable(entry.getKey(), Encoding.urlEncode(entry.getValue()));
            }
            up.sendFile(form.getFileToPost().toString(), form.getFiletoPostName());

            // Dummy request um das ganze kompatibel zu machen
            Request request = new Request(up.getConnection()) {

                @Override
                public void postRequest(HTTPConnection httpConnection) throws IOException {

                }

                @Override
                public void preRequest(HTTPConnection httpConnection) throws IOException {

                }

            };
            request.getHeaders().putAll(headers);
            request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
            request.setFollowRedirects(doRedirects);
            Browser.forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            String ret = null;
            try {
                ret = request.read();
            } catch (IOException e) {

                e.printStackTrace();
            }
            Browser.updateCookies(request);
            this.request = request;
            try {
                currentURL = new URL(action);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            up.close();
            return ret;

        }
        return null;
    }

    @Override
    public String toString() {
        if (request == null) { return "Browser. no request yet"; }
        return request.toString();
    }

    public Regex getRegex(String string) {
        return new Regex(this, string);
    }

    public Regex getRegex(Pattern compile) {
        return new Regex(this, compile);
    }

    public boolean containsHTML(String fileNotFound) {
        return new Regex(this, fileNotFound).matches();
    }

    public String loadConnection(HTTPConnection con) {
        try {
            if (con == null) return request.read();
            if (con == null) return null;
            return Request.read(con);
        } catch (IOException e) {
            return null;
        }

    }

    public HTTPConnection getHttpConnection() {
        if (request == null) return null;
        return request.getHttpConnection();
    }

    /**
     * Lädt über eine URLConnection eine datei ehrunter. Zieldatei ist file.
     * 
     * @param file
     * @param con
     * @return Erfolg true/false
     */
    public static boolean download(File file, HTTPConnection con) {
        try {
            if (file.isFile()) {
                if (!file.delete()) {
                    System.out.println("Konnte Datei nicht überschreiben " + file);
                    return false;
                }
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            BufferedInputStream input = new BufferedInputStream(con.getInputStream());
            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean downloadBinary(String filepath, String fileurl) {

        try {
            fileurl = Encoding.urlEncode(fileurl.replaceAll("\\\\", "/"));
            File file = new File(filepath);
            if (file.isFile()) {
                if (!file.delete()) {
                    System.out.println("Konnte Datei nicht löschen " + file);
                    return false;
                }

            }

            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            fileurl = URLDecoder.decode(fileurl, "UTF-8");

            URL url = new URL(fileurl);
            HTTPConnection con = new HTTPConnection(url.openConnection());

            BufferedInputStream input = new BufferedInputStream(con.getInputStream());

            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();

            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;

        }

    }
    public boolean downloadFile(File file, String urlString) {
        try {
            urlString = URLDecoder.decode(urlString, "UTF-8");
           
            HTTPConnection con = this.openGetConnection(urlString);
            con.setInstanceFollowRedirects(true);
            return Browser.download(file, con);
        } catch (Exception e) {
            e.printStackTrace();
        } 
        return false;
    }

    /**
     * Lädt eine url lokal herunter
     * 
     * @param file
     * @param urlString
     * @return Erfolg true/false
     */
    public static boolean download(File file, String urlString) {
        try {
            urlString = URLDecoder.decode(urlString, "UTF-8");
            URL url = new URL(urlString);
            HTTPConnection con = new HTTPConnection(url.openConnection());
            con.setInstanceFollowRedirects(true);
            return Browser.download(file, con);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Form getForm(int i) {
        Form[] forms = getForms();
        if (forms.length <= i) return null;
        return forms[i];
    }

    public String getHost() {
        if (request == null) return null;
        return request.getUrl().getHost();

    }

    public Browser cloneBrowser() {
        Browser br = new Browser();
        br.acceptLanguage = acceptLanguage;
        br.connectTimeout = connectTimeout;
        br.currentURL = currentURL;
        br.doRedirects = doRedirects;
        br.getHeaders().putAll(headers);
        br.limit = limit;
        br.readTimeout = readTimeout;
        br.request = request;
        return br;
    }

    public Form[] getForms(String downloadURL) {
       this.getPage(downloadURL);
       return this.getForms();
    
    }

    public HTTPConnection openFormConnection(int i) {
        return openFormConnection(getForm(i));
        
    }

}
