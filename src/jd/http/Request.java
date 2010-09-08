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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import jd.http.URLConnectionAdapter.METHOD;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;

import org.appwork.utils.logging.Log;

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
        final LinkedHashMap<String, String> ret = new LinkedHashMap<String, String>();
        if (query.toLowerCase().trim().startsWith("http")) {
            query = new URL(query).getQuery();
        }

        if (query == null) { return ret; }
        final String[][] split = new Regex(query.trim(), "&?(.*?)=(.*?)($|&(?=.*?=.+))").getMatches();
        if (split != null) {
            final int splitLength = split.length;
            for (int i = 0; i < splitLength; i++) {
                ret.put(split[i][0], split[i][1]);
            }
        }
        return ret;
    }

    /*
     * default timeouts, because 0 is infinite and BAD, if we need 0 then we
     * have to set it manually
     */
    private int                    connectTimeout = 30000;
    private int                    readTimeout    = 60000;
    private Cookies                cookies        = null;

    private RequestHeader          headers;
    private String                 htmlCode;
    protected URLConnectionAdapter httpConnection;

    private long                   readTime       = -1;
    private boolean                requested      = false;

    private JDProxy                proxy;
    private URL                    orgURL;
    private String                 customCharset  = null;
    private byte[]                 byteArray;
    private BufferedImage          image;

    public void setCustomCharset(final String charset) {
        this.customCharset = charset;
    }

    public Request(final String url) throws MalformedURLException {
        this.orgURL = new URL(url);
        initDefaultHeader();
    }

    public void setProxy(final JDProxy proxy) {
        this.proxy = proxy;
    }

    public JDProxy getProxy() {
        return proxy;
    }

    public String printHeaders() {
        return httpConnection.toString();
    }

    public Request(final URLConnectionAdapter con) {
        httpConnection = con;
        collectCookiesFromConnection();
    }

    private void collectCookiesFromConnection() {
        final List<String> cookieHeaders = (List<String>) httpConnection.getHeaderFields().get("Set-Cookie");
        final String date = httpConnection.getHeaderField("Date");
        if (cookieHeaders == null) { return; }
        if (cookies == null) {
            cookies = new Cookies();
        }

        final String host = Browser.getHost(httpConnection.getURL());

        for (int i = cookieHeaders.size() - 1; i >= 0; i--) {
            final String header = cookieHeaders.get(i);
            cookies.add(Cookies.parseCookies(header, host, date));
        }
    }

    /**
     * DO NEVER call this method directly... use browser.connect
     */
    protected Request connect() throws IOException {
        requested = true;
        openConnection();
        postRequest(httpConnection);
        httpConnection.connect();
        try {
            collectCookiesFromConnection();
        } catch (NullPointerException e) {
            throw new IOException("Malformed url?", e);
        }
        return this;
    }

    public boolean containsHTML(final String html) throws CharacterCodingException {
        return (getHtmlCode() == null) ? false : getHtmlCode().contains(html);
    }

    public void setCookies(final Cookies cookies) {
        this.cookies = cookies;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public long getContentLength() {
        return (httpConnection == null) ? -1 : httpConnection.getLongContentLength();
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

    public static String getCookieString(final Cookies cookies) {
        if (cookies == null) { return null; }

        final StringBuilder buffer = new StringBuilder();
        boolean first = true;
        final LinkedList<Cookie> cookies2 = new LinkedList<Cookie>(cookies.getCookies());
        for (final Cookie cookie : cookies2) {
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

    public RequestHeader getHeaders() {
        return headers;
    }

    /**
     * Will replace #getHtmlCode() with next release
     */
    public String getResponseText() throws CharacterCodingException {
        return getHtmlCode();
    }

    public String getHtmlCode() throws CharacterCodingException {
        String ct = httpConnection.getContentType();
        /* check for image content type */
        if (ct != null && Pattern.compile("images?/\\w*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(ct).matches()) throw new IllegalStateException("Content-Type: " + ct);
        if (htmlCode == null && byteArray != null) {
            /* use custom charset or charset from httpconnection */
            String useCS = customCharset == null ? this.httpConnection.getCharset() : customCharset;
            try {
                try {
                    try {
                        if (useCS != null) {
                            /* try to use wanted charset */
                            htmlCode = new String(byteArray, useCS.toUpperCase());
                            byteArray = null;
                            return htmlCode;
                        }
                    } catch (Exception e) {
                    }
                    htmlCode = new String(byteArray, "ISO-8859-1");
                    byteArray = null;
                    return htmlCode;
                } catch (Exception e) {
                    Log.getLogger().severe("could neither charset: " + useCS + " nor default charset");
                    /* fallback to default charset in error case */
                    htmlCode = new String(byteArray);
                    byteArray = null;
                    return htmlCode;
                }
            } catch (Exception e) {
                /* in case of error we do not reset byteArray */
            }
        }
        return htmlCode;
    }

    public URLConnectionAdapter getHttpConnection() {
        return httpConnection;
    }

    public String getLocation() {
        if (httpConnection == null) { return null; }

        String red = httpConnection.getHeaderField("Location");
        final String encoding = httpConnection.getHeaderField("Content-Type");
        if (red == null || red.length() == 0) return null;
        if (encoding != null && encoding.contains("UTF-8")) {
            red = Encoding.UTF8Decode(red, "ISO-8859-1");
        }
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
        return (httpConnection == null) ? -1 : httpConnection.getRequestTime();
    }

    public String getResponseHeader(final String key) {
        return (httpConnection == null) ? null : httpConnection.getHeaderField(key);
    }

    public Map<String, List<String>> getResponseHeaders() {
        return (httpConnection == null) ? null : httpConnection.getHeaderFields();
    }

    public URL getUrl() {
        return orgURL;
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

    public boolean isRequested() {
        return requested;
    }

    public String load() throws IOException {
        requestConnection();
        return getHtmlCode();
    }

    private void openConnection() throws IOException {

        if (!headers.contains("Host")) {
            if (orgURL.getPort() != 80 && orgURL.getPort() > 0) {
                headers.setAt(0, "Host", orgURL.getHost() + ":" + orgURL.getPort());
            } else {
                headers.setAt(0, "Host", orgURL.getHost());
            }
        }

        httpConnection = new HTTPConnection(orgURL, proxy);
        httpConnection.setRequest(this);
        httpConnection.setReadTimeout(readTimeout);
        httpConnection.setConnectTimeout(connectTimeout);

        if (headers != null) {
            final int headersSize = headers.size();
            for (int i = 0; i < headersSize; i++) {
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

    public Request read() throws IOException {
        long tima = System.currentTimeMillis();
        httpConnection.setCharset(this.customCharset);
        byteArray = read(httpConnection);
        readTime = System.currentTimeMillis() - tima;
        return this;
    }

    public static byte[] read(final URLConnectionAdapter con) throws IOException {
        BufferedInputStream is = null;
        if (con.getInputStream() != null) {
            if (con.getHeaderField("Content-Encoding") != null && con.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
                is = new BufferedInputStream(new GZIPInputStream(con.getInputStream()));
            } else {
                is = new BufferedInputStream(con.getInputStream());
            }
        }
        if (is == null) {
            // TODO: check if we have t close con here
            return null;
        }
        ByteArrayOutputStream tmpOut;
        long contentLength = con.getContentLength();
        if (contentLength != -1) {
            int length = contentLength > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) contentLength;
            tmpOut = new ByteArrayOutputStream(length);
        } else {
            tmpOut = new ByteArrayOutputStream(16384);
        }
        byte[] preRead = con.preReadBytes();
        if (preRead != null) {
            tmpOut.write(preRead);
        }
        /* added "Corrupt GZIP trailer" for CamWinsCom */
        try {
            final byte[] b = new byte[1024];
            int len;
            while ((len = is.read(b)) != -1) {
                tmpOut.write(b, 0, len);
            }
        } catch (EOFException e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Try workaround for ", e);
        } catch (IOException e) {
            if (e.toString().contains("end of ZLIB") || e.toString().contains("Premature") || e.toString().contains("Corrupt GZIP trailer")) {
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Try workaround for ", e);
            } else
                throw e;
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
            try {
                tmpOut.close();
            } catch (Exception e) {
            }
            try {
                /* disconnect connection */
                con.disconnect();
            } catch (Exception e) {
            }
        }
        return tmpOut.toByteArray();
    }

    private void requestConnection() throws IOException {
        connect();
        read();
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
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

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

    // @Override
    public String toString() {
        if (!requested) { return "Request not sent yet"; }
        try {
            getHtmlCode();
            if (htmlCode == null || htmlCode.length() == 0) {
                if (getLocation() != null) { return "Not HTML Code. Redirect to: " + getLocation(); }
                return "No htmlCode read";
            }
        } catch (Exception e) {
            return "NOTEXT: " + e.getMessage();
        }
        return this.htmlCode;
    }

    public void setHtmlCode(final String htmlCode) {
        // set bytebuffer to null... user works with htmlcode
        this.byteArray = null;
        this.htmlCode = htmlCode;
    }

    public Request toHeadRequest() throws MalformedURLException {
        final Request ret = new Request(this.getUrl() + "") {
            // @Override
            public void postRequest(URLConnectionAdapter httpConnection) throws IOException {
            }

            // @Override
            public void preRequest(URLConnectionAdapter httpConnection) throws IOException {
                httpConnection.setRequestMethod(METHOD.HEAD);
            }
        };
        ret.connectTimeout = this.connectTimeout;
        ret.cookies = new Cookies(this.getCookies());
        ret.headers = (RequestHeader) this.getHeaders().clone();
        ret.setProxy(proxy);
        ret.readTime = this.readTimeout;
        ret.httpConnection = this.httpConnection;
        return ret;
    }

    public Request cloneRequest() {
        return null;
    }

    /**
     * Returns the loaded bytes
     * 
     * @return
     */
    public byte[] getResponseBytes() {
        if (byteArray != null) return byteArray.clone();
        return null;
    }

    /**
     * tries to generate an image out of the loaded bytes
     * 
     * @return
     */
    public Image getResponseImage() {
        String ct = httpConnection.getContentType();
        /* check for image content */
        if (ct != null && !Pattern.compile("images?/\\w*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(ct).matches()) { throw new IllegalStateException("Content-Type: " + ct); }
        // TODO..this is just quick and dirty.. may result in memory leaks
        if (image == null && byteArray != null) {
            InputStream fake = new ByteArrayInputStream(byteArray);
            try {
                image = ImageIO.read(fake);
                // BasicWindow.showImage(image);
                // its an immage;
                byteArray = null;
            } catch (Exception e) {
                Log.exception(e);
            }
        }
        return image;
    }

}
