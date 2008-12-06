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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import jd.config.Configuration;
import jd.parser.Regex;
import jd.utils.JDUtilities;

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

    public static HashMap<String, String> parseQuery(String query) throws MalformedURLException {
        if (query == null) { return null; }
        HashMap<String, String> ret = new HashMap<String, String>();
        if (query.toLowerCase().trim().startsWith("http")) {

            query = new URL(query).getQuery();

        }

        if (query == null) { return ret; }

        String[] split = query.trim().split("[\\&|=]");
        int i = 0;
        while (true) {
            String key = null;
            String value = null;
            if (split.length > i) key = split[i++];
            if (split.length > i) value = split[i++];

            if (key != null) {
                ret.put(key, value);
            } else {
                break;
            }

        }

        return ret;

    }

    private int connectTimeout;
    private ArrayList<Cookie> cookies = null;
    private int followCounter = 0;
    private boolean followRedirects = false;

    private HashMap<String, String> headers;
    private String htmlCode;
    protected HTTPConnection httpConnection;
    private String proxyip;
    private String proxyport;
    private long readTime = -1;
    private int readTimeout;
    private boolean requested = false;
    private long requestTime = -1;

    private URL url;

    public Request(String url) throws MalformedURLException {

        this.url = new URL(Encoding.urlEncode_light(url));

        readTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 100000);

        connectTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 100000);

        initDefaultHeader();

    }

    public String printHeaders() {
        return httpConnection.toString();
    }

    public Request(HTTPConnection con) {
        httpConnection = con;
        collectCookiesFromConnection();
    }

    public static ArrayList<Cookie> parseCookies(String cookieString, String host, String Date) {
        ArrayList<Cookie> cookies = new ArrayList<Cookie>();

        String header = cookieString;

        String path = null;
        String expires = null;
        String domain = null;
        HashMap<String, String> tmp = new HashMap<String, String>();
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
            StringTokenizer st2 = new StringTokenizer(cookieelement, "=");
            if (st2.hasMoreTokens()) key = st2.nextToken().trim();
            if (st2.hasMoreTokens()) value = st2.nextToken().trim();

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
            cookie.setTimeDifferece(Date);
        }

        return cookies;

    }

    private void collectCookiesFromConnection() {
        List<String> cookieHeaders = httpConnection.getHeaderFields().get("Set-Cookie");
        String Date = httpConnection.getHeaderField("Date");
        if (cookieHeaders == null) { return; }
        if (cookies == null) {
            cookies = new ArrayList<Cookie>();
        }

        String host = httpConnection.getURL().getHost();

        for (int i = cookieHeaders.size() - 1; i >= 0; i--) {
            String header = cookieHeaders.get(i);

            cookies.addAll(parseCookies(header, host, Date));
        }

    }

    public Request connect() throws IOException {
        requested = true;
        openConnection();
        postRequest(httpConnection);

        collectCookiesFromConnection();
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

    public void setCookies(ArrayList<Cookie> cookies) {
        this.cookies = cookies;
    }

    public String followRedirect() throws IOException {
        if (getLocation() == null) { return null; }

        url = new URL(getLocation());

        return load();

    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public long getContentLength() {

        if (httpConnection == null) { return -1; }
        return httpConnection.getContentLength();
    }

    public ArrayList<Cookie> getCookies() {
        if (cookies == null) {
            cookies = new ArrayList<Cookie>();
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

    public static String getCookieString(HashMap<String, Cookie> cookies) {
        if (cookies == null) { return null; }

        StringBuilder buffer = new StringBuilder();
        boolean first = true;

        for (Iterator<Entry<String, Cookie>> it = cookies.entrySet().iterator(); it.hasNext();) {
            Cookie cookie = it.next().getValue();

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

    public static String getCookieString(ArrayList<Cookie> cookies) {
        if (cookies == null) { return null; }

        StringBuilder buffer = new StringBuilder();
        boolean first = true;

        for (Cookie cookie : cookies) {

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

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public String getHtmlCode() {

        return htmlCode;
    }

    public HTTPConnection getHttpConnection() {
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
                path = path.substring(0, path.lastIndexOf("/"));
            }
            red = "http://" + this.getHttpConnection().getURL().getHost() + (red.charAt(0) == '/' ? red : path + "/" + red);
        }
        return Encoding.urlEncode_light(red);

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

    public Map<String, List<String>> getResponseHeaders() {
        if (httpConnection == null) { return null; }
        return httpConnection.getHeaderFields();
    }

    public URL getUrl() {
        return url;
    }

    private boolean hasCookies() {

        return cookies != null && !cookies.isEmpty();
    }

    private void initDefaultHeader() {
        headers = new HashMap<String, String>();
        headers.put("Accept-Language", "de, en-gb;q=0.9, en;q=0.8");
        headers.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        headers.put("Connection", "close");
        headers.put("Cache-Control", "no-cache");
        headers.put("Pragma", "no-cache");

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

    public void openConnection() throws IOException {

        long tima = System.currentTimeMillis();
        httpConnection = new HTTPConnection(url.openConnection());
        httpConnection.setInstanceFollowRedirects(followRedirects);
        requestTime = System.currentTimeMillis() - tima;
        httpConnection.setReadTimeout(readTimeout);
        httpConnection.setConnectTimeout(connectTimeout);

        if (headers != null) {
            Set<String> keys = headers.keySet();
            Iterator<String> iterator = keys.iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();

                httpConnection.setRequestProperty(key, headers.get(key));
            }
        }
        preRequest(httpConnection);
        if (hasCookies()) {
            httpConnection.setRequestProperty("Cookie", getCookieString());
        }

    }

    public abstract void postRequest(HTTPConnection httpConnection) throws IOException;

    abstract public void preRequest(HTTPConnection httpConnection) throws IOException;

    public String read() throws IOException {
        long tima = System.currentTimeMillis();
        this.htmlCode = read(httpConnection);
        readTime = System.currentTimeMillis() - tima;

        return htmlCode.toString();
    }

    public static String read(HTTPConnection con) throws IOException {
        BufferedReader rd;
        if (con.getHeaderField("Content-Encoding") != null && con.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {

            rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(con.getInputStream())));

        } else {
            rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
        }
        String line;
        StringBuilder htmlCode = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            htmlCode.append(line + "\r\n");
        }
        rd.close();

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

    public void setProxy(String ip, String port) throws NumberFormatException, MalformedURLException {
        proxyip = ip;
        proxyport = port;
        if (ip == null || port == null) return;
        url = new URL("http", proxyip, Integer.parseInt(proxyport), url.toString());

    }

    public String getProxyip() {
        return proxyip;
    }

    public String getProxyport() {
        return proxyport;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
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

    @SuppressWarnings("unchecked")
    public Request toHeadRequest() throws MalformedURLException {
        Request ret = new Request(this.getUrl() + "") {

            @Override
            public void postRequest(HTTPConnection httpConnection) throws IOException {
            }

            @Override
            public void preRequest(HTTPConnection httpConnection) throws IOException {
                httpConnection.setRequestMethod("HEAD");
            }

        };
        ret.connectTimeout = this.connectTimeout;

        ret.cookies = (ArrayList<Cookie>) this.getCookies().clone();
        ret.followRedirects = this.followRedirects;
        ret.headers = (HashMap<String, String>) this.getHeaders().clone();
        ret.setProxy(proxyip, proxyport);
        ret.readTime = this.readTimeout;

        ret.httpConnection = this.httpConnection;

        return ret;

    }

    public Request cloneRequest() {
        // TODO Auto-generated method stub
        return null;
    }

}
