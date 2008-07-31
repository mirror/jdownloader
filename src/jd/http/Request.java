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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import jd.config.Configuration;
import jd.parser.Regex;
import jd.plugins.HTTPConnection;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public abstract class Request {
    public static int MAX_REDIRECTS = 30;

    /**
     * Gibt eine Hashmap mit allen key:value pairs im query zurück
     * 
     * @param query
     *            kann ein reines query ein (&key=value) oder eine url mit query
     * @return
     */
    public static HashMap<String, String> parseQuery(String query) {
        HashMap<String, String> ret = new HashMap<String, String>();
        if (query.toLowerCase().trim().startsWith("http")) {
            try {
                query = new URL(query).getQuery();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return ret;
            }
        }

        if (query == null) {
            return ret;
        }
        try {
            StringTokenizer st = new StringTokenizer(query, "&=");
            while (st.hasMoreTokens()) {
                ret.put(st.nextToken().trim(), st.nextToken().trim());
            }

        } catch (NoSuchElementException e) {
            // ignore
        }

        return ret;

    }

    private int connectTimeout;
    private HashMap<String, String> cookies = null;
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

    public Request(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        readTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 100000);

        connectTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 100000);

        initDefaultHeader();

    }

    private void collectCookiesFromConnection() {
        Collection<String> cookieHeaders = httpConnection.getHeaderFields().get("Set-Cookie");
        if (cookieHeaders == null) {
            return;
        }
        if (cookies == null) {
            cookies = new HashMap<String, String>();
        }
        ;

        for (String header : cookieHeaders) {
            try {
                StringTokenizer st = new StringTokenizer(header, ";=");
                while (st.hasMoreTokens()) {
                    cookies.put(st.nextToken().trim(), st.nextToken().trim());
                }
            } catch (NoSuchElementException e) {
                // ignore
            }
        }

    }

    public Request connect() throws IOException {
        requested = true;
        openConnection();
        postRequest(httpConnection);

        collectCookiesFromConnection();
        while (followRedirects && httpConnection.getHeaderField("Location") != null && httpConnection.getHeaderField("Location").length()>8) {
            followCounter++;
            if (followCounter >= MAX_REDIRECTS) { throw new IOException("Connection redirects too often. Max (" + MAX_REDIRECTS + ")");

            }
            url = new URL(httpConnection.getHeaderField("Location"));
            openConnection();
            postRequest(httpConnection);
        }
        return this;
    }

    public boolean containsHTML(String html) {
        if (htmlCode == null) {
            return false;
        }
        return htmlCode.contains(html);
    }

    /*
     * private void setCookies(HashMap<String, String> cookies) { this.cookies =
     * cookies; }
     * 
     * private void setCookieString(String cookieString) {
     * 
     * cookies = new HashMap<String, String>(); StringTokenizer st = new
     * StringTokenizer(cookieString, ";="); while (st.hasMoreTokens())
     * cookies.put(st.nextToken().trim(), st.nextToken().trim()); }
     */
    public String followRedirect() throws IOException {
        if (getLocation() == null) {
            return null;
        }
        try {
            url = new URL(getLocation());

            return load();
        } catch (MalformedURLException e) {

            e.printStackTrace();
        }
        return null;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public long getContentLength() {

        if (httpConnection == null) {
            return -1;
        }
        return httpConnection.getContentLength();
    }

    public HashMap<String, String> getCookies() {
        if (cookies == null) {
            cookies = new HashMap<String, String>();
        }
        return cookies;
    }

    public String getCookieString() {
        {
            if (!hasCookies()) {
                return null;
            }

            StringBuffer buffer = new StringBuffer();
            boolean first = true;
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(";");
                }
                buffer.append(entry.getKey());
                buffer.append("=");
                buffer.append(entry.getValue());
            }
            return buffer.toString();
        }
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
        if (httpConnection == null) {
            return null;
        }
        return httpConnection.getHeaderField("Location");
    }

    public long getReadTime() {
        return readTime;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public RequestInfo getRequestInfo() throws IOException {
        RequestInfo requestInfo;

        requestInfo = new RequestInfo(htmlCode, httpConnection.getHeaderField("Location"), getCookieString(), httpConnection.getHeaderFields(), httpConnection.getResponseCode());
        requestInfo.setRequest(this);
        requestInfo.setConnection(httpConnection);
        return requestInfo;

    }

    public long getRequestTime() {
        return requestTime;
    }

    public String getResponseHeader(String key) {
        if (httpConnection == null) {
            return null;
        }
        return httpConnection.getHeaderField(key);
    }

    public Map<String, List<String>> getResponseHeaders() {
        if (httpConnection == null) {
            return null;
        }
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

    public void openConnection() {
        try {
            long tima = System.currentTimeMillis();
            httpConnection = new HTTPConnection(url.openConnection());
            requestTime = System.currentTimeMillis() - tima;
            httpConnection.setReadTimeout(readTimeout);
            httpConnection.setConnectTimeout(connectTimeout);

            httpConnection.setInstanceFollowRedirects(false);
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
        } catch (IOException e) {
            requestTime = -1;
            e.printStackTrace();
        }

    }

    public abstract void postRequest(HTTPConnection httpConnection) throws IOException;

    abstract public void preRequest(HTTPConnection httpConnection) throws IOException;

    public String read() throws IOException {
        long tima = System.currentTimeMillis();
        BufferedReader rd;
        if (httpConnection.getHeaderField("Content-Encoding") != null && httpConnection.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {

            rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(httpConnection.getInputStream())));

        } else {
            rd = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        }
        String line;
        StringBuffer htmlCode = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            htmlCode.append(line + "\r\n");
        }
        rd.close();
        readTime = System.currentTimeMillis() - tima;
        this.htmlCode = htmlCode.toString();
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

    public void setProxy(String ip, String port) {
        proxyip = ip;
        proxyport = port;
        try {
            url = new URL("http", proxyip, Integer.parseInt(proxyport), url.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public String toString() {
        if (!requested) {
            return "Request not sent yet";
        }
        if (htmlCode == null || htmlCode.length() == 0) {
            if (getLocation() != null) { return "Not HTML Code. Redirect to: " + getLocation(); }
            return "No htmlCode read";
        }
        return htmlCode;
    }
}
