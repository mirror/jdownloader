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
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.JavaScript;
import jd.parser.Regex;
import jd.utils.JDUtilities;
import jd.utils.SnifferException;
import jd.utils.Sniffy;

public class Browser {
    public class BrowserException extends IOException {

        /**
         * 
         */
        private static final long serialVersionUID = 1509988898224037320L;

        public BrowserException(String string) {
            super(string);
        }

    }

    public static HashMap<String, HashMap<String, Cookie>> COOKIES = new HashMap<String, HashMap<String, Cookie>>();
    public HashMap<String, HashMap<String, Cookie>> cookies = new HashMap<String, HashMap<String, Cookie>>();

    public void clearCookies(String string) {
        getCookies().put(string, null);

    }

    public void forwardCookies(Request request) {
        if (request == null) { return; }
        String host = Browser.getHost(request.getUrl());
        HashMap<String, Cookie> cookies = getCookies().get(host);
        if (cookies == null) { return; }

        for (Iterator<Entry<String, Cookie>> it = cookies.entrySet().iterator(); it.hasNext();) {
            Cookie cookie = it.next().getValue();

            // Pfade sollten verarbeitet werden...TODO
            if (cookie.isExpired()) {
                continue;
            }
            request.getCookies().add(cookie);
        }

    }

    public void forwardCookies(HTTPConnection con) {
        if (con == null) { return; }
        String host = Browser.getHost(con.getURL().toString());
        HashMap<String, Cookie> cookies = getCookies().get(host);
        String cs = Request.getCookieString(cookies);
        if (cs != null && cs.trim().length() > 0) con.setRequestProperty("Cookie", cs);
    }

    public String getCookie(String url, String string) {
        String host;

        host = Browser.getHost(url);

        HashMap<String, Cookie> cookies = getCookies().get(host);
        return cookies.get(string).getValue();

    }

    public HashMap<String, HashMap<String, Cookie>> getCookies() {

        if (this.cookiesExclusive) return cookies;
        return COOKIES;
    }

    public void setCookie(String url, String key, String value) {
        String host;

        host = Browser.getHost(url);
        HashMap<String, Cookie> cookies;
        if (!getCookies().containsKey(host) || (cookies = getCookies().get(host)) == null) {
            cookies = new HashMap<String, Cookie>();
            getCookies().put(host, cookies);
        }

        Cookie cookie = new Cookie();
        cookie.setHost(host);
        cookie.setKey(key);
        cookie.setValue(value);
        cookies.put(key.trim(), cookie);

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

    public void updateCookies(Request request) {
        if (request == null) { return; }
        String host = Browser.getHost(request.getUrl());
        HashMap<String, Cookie> cookies = getCookies().get(host);
        if (cookies == null) {
            cookies = new HashMap<String, Cookie>();
            getCookies().put(host, cookies);
        }
        for (Cookie cookie : request.getCookies()) {
            cookies.put(cookie.getKey(), cookie);
        }

    }

    private String acceptLanguage = "de, en-gb;q=0.9, en;q=0.8";
    private int connectTimeout = -1;
    private URL currentURL;

    private boolean doRedirects = false;

    private HashMap<String, String> headers;

    private int limit = 1 * 1024 * 1024;

    private int readTimeout = -1;

    private Request request;
    private boolean snifferDetection = false;
    private boolean cookiesExclusive;

    public Browser() {

    }

    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public Form[] getForms() {

        return Form.getForms(this);

    }

    public Form getForm(String name) {
        for (Form f : getForms()) {
            if (f.hasSubmitValue(name)) return f;
        }
        return null;
    }

    public HashMap<String, String> getHeaders() {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        return headers;
    }

    private boolean snifferCheck() throws SnifferException {
        if (!snifferDetection) return false;
        if (Sniffy.hasSniffer()) {
            JDUtilities.getLogger().severe("Sniffer Software detected");
            throw new SnifferException();
        }
        return false;
    }

    public String getPage(String string) throws IOException {
        string = getURL(string);

        if (currentURL == null) {
            currentURL = new URL(string);
        }

        if (snifferCheck()) {
            // throw new SnifferException();
        }
        GetRequest request = new GetRequest(string);
        request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        forwardCookies(request);
        request.getHeaders().put("Referer", currentURL.toString());
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }

        request.connect();
        String ret = null;

        checkContentLengthLimit(request);
        ret = request.read();

        updateCookies(request);
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            ret = this.getPage(null);
        } else {

            currentURL = new URL(string);
        }
        return ret;

    }

    private void checkContentLengthLimit(Request request) throws BrowserException {
        if (request == null || request.getHttpConnection() == null || request.getHttpConnection().getHeaderField("Content-Length") == null) return;
        if (Integer.parseInt(request.getHttpConnection().getHeaderField("Content-Length")) > limit) { throw new BrowserException("Content-length too big"); }

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

    public HTTPConnection openFormConnection(Form form) throws IOException {
        if (form == null) return null;
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

    public HTTPConnection openGetConnection(String string) throws IOException {
        string = getURL(string);

        if (currentURL == null) {
            currentURL = new URL(string);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        GetRequest request = new GetRequest(string);
        if (connectTimeout > 0) {
            request.setConnectTimeout(connectTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        forwardCookies(request);
        request.getHeaders().put("Referer", currentURL.toString());
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }

        request.connect();

        updateCookies(request);
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            this.openGetConnection(null);
        } else {

            currentURL = new URL(string);
        }
        return this.request.getHttpConnection();

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

    private HTTPConnection openPostConnection(String url, HashMap<String, String> post) throws IOException {
        url = getURL(url);

        if (currentURL == null) {
            currentURL = new URL(url);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostRequest request = new PostRequest(url);
        request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        if (connectTimeout > 0) {
            request.setConnectTimeout(connectTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        forwardCookies(request);
        request.getHeaders().put("Referer", currentURL.toString());
        if (post != null) {
            request.getPostData().putAll(post);
        }
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }
        request.connect();

        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            this.openGetConnection(null);
        } else {

            currentURL = new URL(url);
        }
        return this.request.getHttpConnection();

    }

    public HTTPConnection openPostConnection(String url, String post) throws IOException {

        return openPostConnection(url, Request.parseQuery(post));
    }

    public String postPage(String url, HashMap<String, String> post) throws IOException {
        url = getURL(url);

        if (currentURL == null) {
            currentURL = new URL(url);
        }
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
        PostRequest request = new PostRequest(url);
        request.getHeaders().put("ACCEPT-LANGUAGE", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        if (connectTimeout > 0) {
            request.setConnectTimeout(connectTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        forwardCookies(request);
        request.getHeaders().put("Referer", currentURL.toString());
        if (post != null) request.getPostData().putAll(post);
        if (headers != null) {
            request.getHeaders().putAll(headers);
        }

        String ret = null;

        request.connect();
        checkContentLengthLimit(request);
        ret = request.read();

        updateCookies(request);
        this.request = request;
        if (this.doRedirects && request.getLocation() != null) {
            ret = this.getPage(null);
        } else {

            currentURL = new URL(url);
        }
        return ret;

    }

    public JavaScript getJavaScript() throws IOException {
        if (request == null) return null;
        String data = toString();
        String url = request.getUrl().toString();
        String basename = "";
        String host = "";
        LinkedList<String> set = new LinkedList<String>();
        Pattern[] basePattern = new Pattern[] { Pattern.compile("(?s)<[ ]?base[^>]*?href='(.*?)'", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?base[^>]*?href=\"(.*?)\"", Pattern.CASE_INSENSITIVE), Pattern.compile("(?s)<[ ]?base[^>]*?href=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE), };
        Matcher m;
        for (Pattern element : basePattern) {
            m = element.matcher(data);
            if (m.find()) {
                url = Encoding.htmlDecode(m.group(1));
                break;
            }
        }
        if (url != null) {
            url = url.replace("http://", "");
            int dot = url.lastIndexOf('/');
            if (dot != -1) {
                basename = url.substring(0, dot + 1);
            } else {
                basename = "http://" + url + "/";
            }
            dot = url.indexOf('/');
            if (dot != -1) {
                host = "http://" + url.substring(0, dot);
            } else {
                host = "http://" + url;
            }
            url = "http://" + url;
        } else {
            url = "";
        }
        String[][] reg = new Regex(data, "<[ ]?script(.*?)>(.*?)<[ ]?/script>").getMatches();
        StringBuffer buff = new StringBuffer();
        // buff.append("var document[];\r\n");
        for (int i = 0; i < reg.length; i++) {
            if (reg[i][0].toLowerCase().contains("javascript")) {
                if (reg[i][1].length() > 0) buff.append(reg[i][1] + "\r\n");
                Pattern[] linkAndFormPattern = new Pattern[] { Pattern.compile(".*?src=\"(.*?)\"", Pattern.CASE_INSENSITIVE), Pattern.compile(".*?src='(.*?)'", Pattern.CASE_INSENSITIVE), Pattern.compile(".*?src=([^'\"][^\\s]*)", Pattern.CASE_INSENSITIVE) };
                for (Pattern element : linkAndFormPattern) {
                    m = element.matcher(reg[i][0]);
                    while (m.find()) {
                        String link = Encoding.htmlDecode(m.group(1));
                        if (link.length() > 6 && link.matches("(?is)https?://.*")) {
                            ;
                        } else if (link.length() > 0) {
                            if (link.length() > 2 && link.substring(0, 3).equals("www")) {
                                link = "http://" + link;
                            }
                            if (link.charAt(0) == '/') {
                                link = host + link;
                            } else if (link.charAt(0) == '#') {
                                link = url + link;
                            } else {
                                link = basename + link;
                            }
                        }
                        if (!set.contains(link)) {
                            set.add(link);
                        }
                    }
                }
            }

        }

        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            String string = (String) iter.next();
            String page = this.cloneBrowser().getPage(string);
            buff.append(page + "\r\n");
        }
        String ret = buff.toString();
        // TODO document ersetzen
        // ret.replaceAll("document\\.([^\\s;=]*)", "");
        return new JavaScript(ret);
    }

    public String postPage(String url, String post) throws IOException {

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

    public String getBaseURL() {
        if (request == null) return null;

        String base = request.getUrl().toString();
        if (base.matches("http://.*/.*")) {
            return base.substring(0, base.lastIndexOf("/")) + "/";
        } else {
            return base + "/";
        }

    }

    public String submitForm(Form form) throws IOException {
        String base = null;
        if (snifferCheck()) {
            // throw new IOException("Sniffer found");
        }
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
            forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            String ret = null;
            checkContentLengthLimit(request);
            ret = request.read();

            updateCookies(request);
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

    public String loadConnection(HTTPConnection con) throws IOException {
        checkContentLengthLimit(request);
        if (con == null) return request.read();
        return Request.read(con);

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
            fileurl = fileurl.replaceAll(" ", "%20");
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
        br.getHeaders().putAll(getHeaders());
        br.limit = limit;
        br.readTimeout = readTimeout;
        br.request = request;
        return br;
    }

    public Form[] getForms(String downloadURL) throws IOException {
        this.getPage(downloadURL);
        return this.getForms();

    }

    public HTTPConnection openFormConnection(int i) throws IOException {
        return openFormConnection(getForm(i));

    }

    public String getMatch(String string) {
        return getRegex(string).getMatch(0);

    }

    public boolean isSnifferDetection() {
        return snifferDetection;
    }

    public void setSnifferDetection(boolean snifferDetection) {
        this.snifferDetection = snifferDetection;
    }

    public String getURL() {
        if (request == null) { return null; }
        return request.getUrl().toString();

    }

    public void setCookiesExclusive(boolean b) {
        if(cookiesExclusive==b)return;
        this.cookiesExclusive = b;
        if (b) {
            this.cookies.clear();

            for (Iterator<Entry<String, HashMap<String, Cookie>>> it = COOKIES.entrySet().iterator(); it.hasNext();) {
                Entry<String, HashMap<String, Cookie>> next = it.next();
                HashMap<String, Cookie> tmp;
                cookies.put(next.getKey(), tmp = new HashMap<String, Cookie>());
                tmp.putAll(next.getValue());

            }

        } else {
            this.cookies.clear();
        }

    }

    public boolean isCookiesExclusive() {
        return cookiesExclusive;
    }

    public String followConnection() {
        String ret = null;
        try {
            if(request.getHtmlCode()!=null){
                JDUtilities.getLogger().warning("Request has already been read");
                return null;
            }
        checkContentLengthLimit(request);
       
            ret = request.read();
        } catch (IOException e) {
        
            e.printStackTrace();
            return null;
        }    
      
      
        return ret;
        
    }

}
