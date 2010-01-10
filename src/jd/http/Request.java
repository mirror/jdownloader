//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import jd.nutils.encoding.Encoding;
import jd.parser.Regex;

public abstract class Request {
    // public static int MAX_REDIRECTS = 30;

    /**
     * Gibt eine Hashmap mit allen key:value pairs im query zurück
     * 
     * @param query
     *            kann ein reines query ein (&key=value) oder eine url mit query
     * @return
     * @throws MalformedURLException
     */

    public static LinkedHashMap<String, String> parseQuery(String query) throws MalformedURLException {
        if (query == null) { return null; }
        LinkedHashMap<String, String> ret = new LinkedHashMap<String, String>();
        if (query.toLowerCase().trim().startsWith("http")) {
            query = new URL(query).getQuery();
        }

        if (query == null) { return ret; }
        String[][] split = new Regex(query.trim(), "&?(.*?)=(.*?)($|&(?=.*?=.+))").getMatches();
        if (split != null) {
            for (int i = 0; i < split.length; i++) {
                ret.put(split[i][0], split[i][1]);
            }
        }
        return ret;
    }

    /*
     * default timeouts, because 0 is infinite and BAD, if we need 0 then we
     * have to set it manually
     */
    private int connectTimeout = 30000;
    private int readTimeout = 60000;
    private Cookies cookies = null;
    private int followCounter = 0;
    private boolean followRedirects = false;

    private RequestHeader headers;
    private String htmlCode;
    protected URLConnectionAdapter httpConnection;

    private long readTime = -1;
    private boolean requested = false;
    private long requestTime = -1;

    private URL url;
    private JDProxy proxy;
    private URL orgURL;
    private String customCharset = null;

    private static String http2JDP(String string) {
        if (string.startsWith("http")) { return ("jdp" + string.substring(4)); }
        return string;
    }

    private static String jdp2http(String string) {
        if (string.startsWith("jdp")) { return ("http" + string.substring(3)); }
        return string;
    }

    public void setCustomCharset(String charset) {
        this.customCharset = charset;
    }

    public Request(String url) throws MalformedURLException {

        this.url = new URL(Encoding.urlEncode_light(http2JDP(url)));
        this.orgURL = new URL(jdp2http(url));
        initDefaultHeader();

    }

    public void setProxy(JDProxy proxy) {
        this.proxy = proxy;

    }

    public JDProxy getProxy() {
        return proxy;
    }

    public String printHeaders() {
        return httpConnection.toString();
    }

    public Request(URLConnectionAdapter con) {
        httpConnection = con;
        collectCookiesFromConnection();
    }

    @SuppressWarnings("unchecked")
    private void collectCookiesFromConnection() {

        List<String> cookieHeaders = (List<String>) httpConnection.getHeaderFields().get("Set-Cookie");
        String Date = httpConnection.getHeaderField("Date");
        if (cookieHeaders == null) { return; }
        if (cookies == null) {
            cookies = new Cookies();
        }

        String host = Browser.getHost(httpConnection.getURL());

        for (int i = cookieHeaders.size() - 1; i >= 0; i--) {
            String header = cookieHeaders.get(i);
            cookies.add(Cookies.parseCookies(header, host, Date));
        }
    }

    /**
     * DO NEVER call this method directly... use browser.connect
     */
    protected Request connect() throws IOException {
        requested = true;
        openConnection();
        postRequest(httpConnection);
        try {
            collectCookiesFromConnection();
        } catch (NullPointerException e) {
            throw new IOException("Malformed url?", e);
        }
        // while (followRedirects && httpConnection.getHeaderField("Location")
        // != null ) {
        // followCounter++;
        // if (followCounter >= MAX_REDIRECTS) { throw new
        // IOException("Connection redirects too often. Max (" + MAX_REDIRECTS +
        // ")");
        //
        // }
        // url = new URL(httpConnection.getHeaderField("Location"));
        // openConnection();
        // postRequest(httpConnection);
        // }

        return this;
    }

    public boolean containsHTML(String html) {
        if (htmlCode == null) { return false; }
        return htmlCode.contains(html);
    }

    public void setCookies(Cookies cookies) {
        this.cookies = cookies;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public long getContentLength() {
        if (httpConnection == null) { return -1; }
        return httpConnection.getLongContentLength();
    }

    public Cookies getCookies() {
        if (cookies == null) {
            cookies = new Cookies();
        }
        return cookies;
    }

    // public static boolean isExpired(String cookie) {
    // if (cookie == null) return false;
    //
    // try {
    // return (new Date().compareTo()) > 0;
    // } catch (Exception e) {
    // return false;
    // }
    // }

    public String getCookieString() {
        return getCookieString(cookies);
    }

    public static String getCookieString(Cookies cookies) {
        if (cookies == null) { return null; }

        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        LinkedList<Cookie> cookies2 = new LinkedList<Cookie>(cookies.getCookies());
        for (Cookie cookie : cookies2) {
            // Pfade sollten verarbeitet werden...TODO
            if (cookie.isExpired()) {
                continue;
            }

            if (first) {
                first = false;
            } else {
                buffer.append("; ");
            }
            buffer.append(cookie.getKey());
            buffer.append("=");
            buffer.append(cookie.getValue());
        }
        return buffer.toString();
    }

    public int getFollowCounter() {
        return followCounter;
    }

    public RequestHeader getHeaders() {
        return headers;
    }

    public String getHtmlCode() {

        return htmlCode;
    }

    public URLConnectionAdapter getHttpConnection() {
        return httpConnection;
    }

    public String getLocation() {
        if (httpConnection == null) { return null; }
        String red = httpConnection.getHeaderField("Location");
        String encoding = httpConnection.getHeaderField("Content-Type");
        if (red == null || red.length() == 0) return null;
        if (encoding != null && encoding.contains("UTF-8")) red = Encoding.UTF8Decode(red, "ISO-8859-1");
        try {
            new URL(red);
        } catch (Exception e) {
            String path = this.getHttpConnection().getURL().getFile();
            if (!path.endsWith("/")) {

                int lastSlash = path.lastIndexOf("/");
                if (lastSlash > 0) {

                    path = path.substring(0, path.lastIndexOf("/"));
                } else {
                    path = "";
                }

            }
            red = "http://" + this.getHttpConnection().getURL().getHost() + (red.charAt(0) == '/' ? red : path + "/" + red);
        }
        return Browser.correctURL(Encoding.urlEncode_light(red));

    }

    public long getReadTime() {
        return readTime;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public String getResponseHeader(String key) {
        if (httpConnection == null) { return null; }
        return httpConnection.getHeaderField(key);
    }

    @SuppressWarnings("unchecked")
    public Map<String, ArrayList<String>> getResponseHeaders() {
        if (httpConnection == null) { return null; }
        return httpConnection.getHeaderFields();
    }

    public URL getUrl() {
        return orgURL;
    }

    public URL getJDPUrl() {
        return url;
    }

    private boolean hasCookies() {

        return cookies != null && !cookies.isEmpty();
    }

    private void initDefaultHeader() {

        headers = new RequestHeader();
        headers.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "de, en-gb;q=0.9, en;q=0.8");
        headers.put("Accept-Encoding", "gzip");
        headers.put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");

        headers.put("Cache-Control", "no-cache");
        headers.put("Pragma", "no-cache");
        headers.put("Connection", "close");

    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public boolean isRequested() {
        return requested;
    }

    public String load() throws IOException {
        requestConnection();
        return htmlCode;
    }

    public boolean matches(Pattern pat) {
        return new Regex(htmlCode, pat).matches();
    }

    public boolean matches(String pat) {
        return new Regex(htmlCode, pat).matches();
    }

    private void openConnection() throws IOException {

        // if (request.getHttpConnection().getResponseCode() == 401 &&
        // logins.containsKey(request.getUrl().getHost())) {
        // this.getHeaders().put("Authorization", "Basic " +
        // Encoding.Base64Encode(logins.get(request.getUrl().getHost())[0] + ":"
        // + logins.get(request.getUrl().getHost())[1]));
        //
        // request.getHttpConnection().disconnect();
        // return this.getPage(string);
        //
        // }

        long tima = System.currentTimeMillis();

        if (!headers.contains("Host")) {
            if (url.getPort() != 80 && url.getPort() > 0) {
                headers.setAt(0, "Host", url.getHost() + ":" + url.getPort());
            } else {
                headers.setAt(0, "Host", url.getHost());
            }

        }
        if (proxy != null) {
            httpConnection = (URLConnectionAdapter) url.openConnection(proxy);
        } else {
            httpConnection = (URLConnectionAdapter) url.openConnection();
        }
        httpConnection.setRequest(this);
        httpConnection.setInstanceFollowRedirects(followRedirects);
        requestTime = System.currentTimeMillis() - tima;
        httpConnection.setReadTimeout(readTimeout);
        httpConnection.setConnectTimeout(connectTimeout);

        if (headers != null) {

            for (int i = 0; i < headers.size(); i++) {

                httpConnection.setRequestProperty(headers.getKey(i), headers.getValue(i));
            }
        }
        preRequest(httpConnection);
        if (hasCookies()) {
            httpConnection.setRequestProperty("Cookie", getCookieString());
        }

    }

    public abstract void postRequest(URLConnectionAdapter httpConnection) throws IOException;

    abstract public void preRequest(URLConnectionAdapter httpConnection) throws IOException;

    public String read() throws IOException {
        long tima = System.currentTimeMillis();
        httpConnection.setCharset(this.customCharset);
        this.htmlCode = read(httpConnection);
        readTime = System.currentTimeMillis() - tima;
        return htmlCode;
    }

    public static String read(URLConnectionAdapter con) throws IOException {
        BufferedReader rd;
        InputStreamReader isr;
        InputStream is = null;
        if (con.getHeaderField("Content-Encoding") != null && con.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            if (con.getInputStream() != null) is = new GZIPInputStream(con.getInputStream());
        } else {
            if (con.getInputStream() != null) is = con.getInputStream();
        }
        if (is == null) return null;
        String cs = con.getCharset();
        if (cs == null) {
            /* default encoding ist ISO-8859-1, falls nicht anders angegeben */
            isr = new InputStreamReader(is, "ISO-8859-1");
        } else {
            cs = cs.toUpperCase();
            try {
                isr = new InputStreamReader(is, cs);
            } catch (Exception e) {
                // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,
                // "Could not Handle Charset " + cs, e);
                try {
                    isr = new InputStreamReader(is, cs.replace("-", ""));
                } catch (Exception e2) {
                    // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,
                    // "Could not Handle Charset " + cs, e);
                    isr = new InputStreamReader(is);
                }
            }
        }
        rd = new BufferedReader(isr);
        String line;
        StringBuilder htmlCode = new StringBuilder();
        /* workaround for premature eof */
        try {
            while ((line = rd.readLine()) != null) {
                htmlCode.append(line + "\r\n");
            }
        } catch (EOFException e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Try workaround for ", e);
        } catch (IOException e) {
            if (e.toString().contains("end of ZLIB") || e.toString().contains("Premature")) {
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Try workaround for ", e);
            } else
                throw e;
        } finally {
            try {
                rd.close();
            } catch (Exception e) {
            }
        }
        return htmlCode.toString();
    }

    private void requestConnection() throws IOException {
        connect();
        htmlCode = read();

    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    // public void setProxy(String ip, String port) throws
    // NumberFormatException, MalformedURLException {
    // proxyip = ip;
    // proxyport = port;
    // if (ip == null || port == null) return;
    // url = new URL("http", proxyip, Integer.parseInt(proxyport),
    // url.toString());
    //
    // }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    // @Override
    public String toString() {
        if (!requested) { return "Request not sent yet"; }

        if (htmlCode == null || htmlCode.length() == 0) {
            if (getLocation() != null) { return "Not HTML Code. Redirect to: " + getLocation(); }
            return "No htmlCode read";
        }

        return htmlCode;
    }

    public void setHtmlCode(String htmlCode) {
        this.htmlCode = htmlCode;
    }

    public Request toHeadRequest() throws MalformedURLException {
        Request ret = new Request(this.getUrl() + "") {
            // @Override
            public void postRequest(URLConnectionAdapter httpConnection) throws IOException {
            }

            // @Override
            public void preRequest(URLConnectionAdapter httpConnection) throws IOException {
                httpConnection.setRequestMethod("HEAD");
            }
        };
        ret.connectTimeout = this.connectTimeout;
        ret.cookies = new Cookies(this.getCookies());
        ret.followRedirects = this.followRedirects;
        ret.headers = (RequestHeader) this.getHeaders().clone();
        ret.setProxy(proxy);
        ret.readTime = this.readTimeout;
        ret.httpConnection = this.httpConnection;
        return ret;
    }

    public Request cloneRequest() {
        return null;
    }

}
