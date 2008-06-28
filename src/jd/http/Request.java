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
    private HashMap<String, String> cookies = null;
    private boolean followRedirects = false;
    private int connectTimeout;
    private int readTimeout;
    private URL url;

    protected HTTPConnection httpConnection;
    private long requestTime = -1;
    private HashMap<String, String> headers;
    private long readTime = -1;
    private int followCounter = 0;
    private String htmlCode;
    private boolean requested = false;
    private String proxyip;
    private String proxyport;

    public Request(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        readTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 60000);

        connectTimeout = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 60000);

        initDefaultHeader();

    }
    public void setProxy(String ip, String port) {
        this.proxyip=ip;
        this.proxyport=port;
        try {
            url=new URL("http",proxyip,Integer.parseInt(proxyport),url.toString());
        } catch (Exception e) {         
            e.printStackTrace();
        } 
        
    }
    private void initDefaultHeader() {
        headers = new HashMap<String, String>();
        headers.put("Accept-Language", "de, en-gb;q=0.9, en;q=0.8");
        headers.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
    }

    public String getHtmlCode() {

        return htmlCode;
    }

    public RequestInfo getRequestInfo() throws IOException {
        RequestInfo requestInfo;

        requestInfo = new RequestInfo(htmlCode, httpConnection.getHeaderField("Location"), this.getCookieString(), httpConnection.getHeaderFields(), httpConnection.getResponseCode());
        requestInfo.setRequest(this);
        requestInfo.setConnection(httpConnection);
        return requestInfo;

    }

    public Request connect() throws IOException {
        this.requested = true;
        this.openConnection();
        postRequest(this.httpConnection);

        collectCookiesFromConnection();
        while (this.followRedirects && httpConnection.getHeaderField("Location") != null) {
            this.followCounter++;
            if (followCounter >= MAX_REDIRECTS) { throw new IOException("Connection redirects too often. Max (" + MAX_REDIRECTS + ")");

            }
            url = new URL(httpConnection.getHeaderField("Location"));
            openConnection();
            postRequest(this.httpConnection);
        }
        return this;
    }

    public long getContentLength() {
        if (this.httpConnection == null) return -1;
        return (long) httpConnection.getContentLength();
    }

    private void requestConnection() throws IOException {
        connect();
        htmlCode = read();

    }

    public String load() throws IOException {
        requestConnection();
        return htmlCode;
    }

    public String toString() {
        if (!requested) return "Request not sent yet";
        if (htmlCode == null || htmlCode.length() == 0) {
            if (getLocation() != null) { return "Not HTML Code. Redirect to: " + getLocation(); }
            return "No htmlCode read";
        }
        return htmlCode;
    }

    public abstract void postRequest(HTTPConnection httpConnection) throws IOException;
/*
    private void setCookies(HashMap<String, String> cookies) {
        this.cookies = cookies;
    }

    private void setCookieString(String cookieString) {

        cookies = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(cookieString, ";=");
        while (st.hasMoreTokens())
            cookies.put(st.nextToken().trim(), st.nextToken().trim());
    }
*/
    public String followRedirect() throws IOException {
        if (getLocation() == null) return null;
        try {
            this.url = new URL(getLocation());

            return this.load();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public String getCookieString() {
        {
            if (!hasCookies()) return null;

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

    public HashMap<String, String> getCookies() {
        if (cookies == null) cookies = new HashMap<String, String>();
        return cookies;
    }

    public boolean containsHTML(String html) {
        if (htmlCode == null) return false;
        return this.htmlCode.contains(html);
    }

    public boolean matches(String pat) {
        return new Regex(htmlCode, pat).matches();
    }

    public boolean matches(Pattern pat) {
        return new Regex(htmlCode, pat).matches();
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public String read() throws IOException {
        long tima = System.currentTimeMillis();
        BufferedReader rd;
        if (this.httpConnection.getHeaderField("Content-Encoding") != null && httpConnection.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {

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
        this.readTime = System.currentTimeMillis() - tima;
        this.htmlCode=htmlCode.toString();
        return htmlCode.toString();
    }

    public void openConnection() {
        try {
            long tima = System.currentTimeMillis();
            httpConnection = new HTTPConnection(url.openConnection());
            this.requestTime = System.currentTimeMillis() - tima;
            httpConnection.setReadTimeout(this.readTimeout);
            httpConnection.setConnectTimeout(this.connectTimeout);
      
            httpConnection.setInstanceFollowRedirects(false);
            if (this.headers != null) {
                Set<String> keys = headers.keySet();
                Iterator<String> iterator = keys.iterator();
                String key;
                while (iterator.hasNext()) {
                    key = iterator.next();

                    httpConnection.setRequestProperty(key, headers.get(key));
                }
            }
            preRequest(httpConnection);
            if (hasCookies()) httpConnection.setRequestProperty("Cookie", this.getCookieString());
        } catch (IOException e) {
            this.requestTime = -1;
            e.printStackTrace();
        }

    }

    abstract public void preRequest(HTTPConnection httpConnection) throws IOException;

    private boolean hasCookies() {

        return this.cookies != null && !this.cookies.isEmpty();
    }

    private void collectCookiesFromConnection() {
        Collection<String> cookieHeaders = httpConnection.getHeaderFields().get("Set-Cookie");
        if (cookieHeaders == null) return;
        if (this.cookies == null) cookies = new HashMap<String, String>();
        ;

        for (String header : cookieHeaders) {
            try {
                StringTokenizer st = new StringTokenizer(header, ";=");
                while (st.hasMoreTokens())
                    cookies.put(st.nextToken().trim(), st.nextToken().trim());
            } catch (NoSuchElementException e) {
                // ignore
            }
        }

    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public long getReadTime() {
        return readTime;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public int getFollowCounter() {
        return followCounter;
    }

    public URL getUrl() {
        return url;
    }

    public boolean isRequested() {
        return requested;
    }

    public String getLocation() {
        if (httpConnection == null) return null;
        return httpConnection.getHeaderField("Location");
    }

    public HTTPConnection getHttpConnection() {
        return httpConnection;
    }
    public Map<String, List<String>> getResponseHeaders() {
        if(httpConnection==null)return null;
          return httpConnection.getHeaderFields();
      }
      public String getResponseHeader(String key){
          if(httpConnection==null)return null;
          return httpConnection.getHeaderField(key);
      }
}
