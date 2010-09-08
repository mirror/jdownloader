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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import sun.net.www.MessageHeader;
import sun.net.www.http.ChunkedInputStream;

import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;
import jd.parser.Regex;

public class HTTPConnection implements URLConnectionAdapter {

    protected LinkedHashMap<String, String>     requestProperties    = null;
    protected long[]                            ranges;

    protected Request                           request;

    protected String                            customcharset        = null;

    protected Socket                            httpSocket           = null;
    protected URL                               httpURL              = null;
    protected JDProxy                           proxy                = null;
    protected String                            httpPath             = null;

    private METHOD                              httpMethod           = METHOD.GET;
    private LinkedHashMap<String, List<String>> headers              = null;
    private int                                 httpResponseCode     = -1;
    private String                              httpResponseMessage  = "";
    private int                                 readTimeout          = 30000;
    private int                                 connectTimeout       = 30000;
    private long                                requestTime          = -1;
    private InputStream                         inputStream          = null;
    private boolean                             inputStreamConnected = false;
    private String                              httpHeader           = null;
    private byte[]                              preReadBytes         = null;

    public boolean isConnected() {
        if (httpSocket != null && httpSocket.isConnected()) return true;
        return false;
    }

    public HTTPConnection(URL url) {
        this(url, null);
    }

    public HTTPConnection(URL url, JDProxy p) {
        httpURL = url;
        proxy = p;
        requestProperties = new LinkedHashMap<String, String>();
        headers = new LinkedHashMap<String, List<String>>();
    }

    public void connect() throws IOException {
        if (isConnected()) return;/* oder fehler */
        if (httpURL.getProtocol().startsWith("https")) {
            SocketFactory socketFactory = SSLSocketFactory.getDefault();
            httpSocket = socketFactory.createSocket();
        } else {
            httpSocket = createSocket();
        }
        httpSocket.setSoTimeout(readTimeout);
        httpResponseCode = -1;
        InetAddress host = Inet4Address.getByName(httpURL.getHost());
        int port = httpURL.getPort();
        if (port == -1) port = httpURL.getDefaultPort();
        long startTime = System.currentTimeMillis();
        if (proxy != null) {
            /* http://de.wikipedia.org/wiki/SOCKS */
            throw new RuntimeException("proxy support not done yet");
        } else {
            httpSocket.connect(new InetSocketAddress(host, port), connectTimeout);
        }
        requestTime = System.currentTimeMillis() - startTime;
        httpPath = new Regex(httpURL.toString(), "https?://.*?(/.+)").getMatch(0);
        if (httpPath == null) httpPath = "/";
        /* now send Request */
        StringBuilder sb = new StringBuilder();
        sb.append(httpMethod.name() + " " + httpPath + " HTTP/1.1\r\n");
        for (String key : this.requestProperties.keySet()) {
            sb.append(key + ": " + requestProperties.get(key) + "\r\n");
        }
        sb.append("\r\n");
        httpSocket.getOutputStream().write(sb.toString().getBytes("UTF-8"));
        httpSocket.getOutputStream().flush();
        if (httpMethod != METHOD.POST) {
            connectInputStream();
        }
    }

    protected synchronized void connectInputStream() throws IOException {
        if (inputStreamConnected) return;
        inputStreamConnected = true;
        /* first read http header */
        ByteBuffer header = HTTPConnectionUtils.readheader(httpSocket.getInputStream(), true);
        byte[] bytes = new byte[header.limit()];
        header.get(bytes);
        httpHeader = new String(bytes, "UTF-8").trim();
        /* parse response code/message */
        if (httpHeader.startsWith("HTTP")) {
            String code = new Regex(httpHeader, "HTTP.*? (\\d+)").getMatch(0);
            if (code != null) httpResponseCode = Integer.parseInt(code);
            httpResponseMessage = new Regex(httpHeader, "HTTP.*? \\d+ (.+)").getMatch(0);
            if (httpResponseMessage == null) httpResponseMessage = "";
        } else {
            preReadBytes = bytes;
            httpHeader = "unknown HTTP response";
            httpResponseCode = 200;
            httpResponseMessage = "unknown HTTP response";
            inputStream = httpSocket.getInputStream();
            return;
        }
        /* read rest of http headers */
        header = HTTPConnectionUtils.readheader(httpSocket.getInputStream(), false);
        bytes = new byte[header.limit()];
        header.get(bytes);
        String temp = new String(bytes, "UTF-8");
        String[] headerStrings = temp.split("\r\n");
        temp = null;
        for (int i = 1; i <= headerStrings.length - 1; i++) {
            String line = headerStrings[i];
            String key = null;
            String value = null;
            if (line.indexOf(": ") > 0) {
                key = line.substring(0, line.indexOf(": "));
                value = line.substring(line.indexOf(": ") + 2);
            } else {
                key = null;
                value = line;
            }
            List<String> list = headers.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                headers.put(key, list);
            }
            list.add(value);
        }
        headerStrings = null;
        List<String> chunked = headers.get("Transfer-Encoding");
        if (chunked != null && chunked.size() > 0 && "chunked".equalsIgnoreCase(chunked.get(0))) {
            /* TODO: write own chunkedinputstream */
            inputStream = new ChunkedInputStream(httpSocket.getInputStream(), new MyHttpClient(), new MessageHeader());
        } else {
            inputStream = httpSocket.getInputStream();
        }
    }

    public long getLongContentLength() {
        String length = getHeaderField("Content-Length");
        if (length == null) { return -1; }
        return Long.parseLong(length);
    }

    public int getContentLength() {
        return (int) getLongContentLength();
    }

    public String getContentType() {
        String type = getHeaderField("Content-Type");
        if (type == null) return "unknown";
        return type;
    }

    public InputStream getInputStream() throws IOException {
        connect();
        connectInputStream();
        return inputStream;
    }

    public Map<String, String> getRequestProperties() {
        return requestProperties;
    }

    public void setRequestProperty(String key, String value) {
        requestProperties.put(key, value);
    }

    public boolean isOK() {
        if (getResponseCode() > -2 && getResponseCode() < 400) return true;
        return false;
    }

    public long[] getRange() {
        String range;
        if (ranges != null) return ranges;
        if ((range = this.getHeaderField("Content-Range")) == null) return null;
        // bytes 174239-735270911/735270912
        String[] ranges = new Regex(range, ".*?(\\d+).*?-.*?(\\d+).*?/.*?(\\d+)").getRow(0);
        if (ranges == null) {
            System.err.print(this + "");
            return null;
        }
        this.ranges = new long[] { Long.parseLong(ranges[0]), Long.parseLong(ranges[1]), Long.parseLong(ranges[2]) };
        return this.ranges;
    }

    public boolean isContentDisposition() {
        return this.getHeaderField("Content-Disposition") != null;
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                httpSocket.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("-->" + this.getURL() + "\r\n");

        sb.append("----------------Request------------------\r\n");

        sb.append(httpMethod.toString() + " " + getURL().getPath() + (getURL().getQuery() != null ? "?" + getURL().getQuery() : "") + " HTTP/1.1\r\n");

        for (String key : this.getRequestProperties().keySet()) {
            String v = this.getRequestProperties().get(key);
            sb.append(key);
            sb.append(new char[] { ':', ' ' });
            sb.append(v);
            sb.append(new char[] { '\r', '\n' });
        }
        sb.append(new char[] { '\r', '\n' });

        if (this.getRequest() != null) {
            if (getRequest() instanceof PostRequest) {
                if (((PostRequest) getRequest()).getPostDataString() != null) sb.append(((PostRequest) getRequest()).getPostDataString());
                sb.append(new char[] { '\r', '\n' });

            } else if (getRequest() instanceof PostFormDataRequest) {
                if (((PostFormDataRequest) getRequest()).getPostDataString() != null) sb.append(((PostFormDataRequest) getRequest()).getPostDataString());
                sb.append(new char[] { '\r', '\n' });
            }

        }

        sb.append("----------------Response------------------\r\n");
        sb.append(httpHeader + "\r\n");
        for (Iterator<Entry<String, List<String>>> it = getHeaderFields().entrySet().iterator(); it.hasNext();) {
            Entry<String, List<String>> next = it.next();
            // Achtung cookie reihenfolge ist wichtig!!!
            for (int i = next.getValue().size() - 1; i >= 0; i--) {
                if (next.getKey() == null) {
                    sb.append(next.getValue().get(i));
                    sb.append(new char[] { '\r', '\n' });
                } else {
                    sb.append(next.getKey());
                    sb.append(new char[] { ':', ' ' });
                    sb.append(next.getValue().get(i));
                    sb.append(new char[] { '\r', '\n' });
                }
            }
        }
        sb.append(new char[] { '\r', '\n' });

        return sb.toString();

    }

    public void setRequest(Request request) {
        this.request = request;

    }

    public Request getRequest() {
        return request;
    }

    public String getCharset() {
        int i;
        if (customcharset != null) return customcharset;
        return (getContentType() != null && (i = getContentType().toLowerCase().indexOf("charset=")) > 0) ? getContentType().substring(i + 8).trim() : null;
    }

    public void setCharset(String Charset) {
        this.customcharset = Charset;
    }

    public String getHeaderField(String string) {
        List<String> ret = headers.get(string);
        if (ret == null || ret.size() == 0) return null;
        return ret.get(0);
    }

    public OutputStream getOutputStream() throws IOException {
        connect();
        return httpSocket.getOutputStream();
    }

    public String getRequestProperty(String string) {
        return requestProperties.get(string);
    }

    public int getResponseCode() {
        return this.httpResponseCode;
    }

    public String getResponseMessage() {
        return this.httpResponseMessage;
    }

    public URL getURL() {
        return httpURL;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setRequestMethod(METHOD method) {
        this.httpMethod = method;
    }

    public Map<String, List<String>> getHeaderFields() {
        return headers;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public METHOD getRequestMethod() {
        return this.httpMethod;
    }

    public byte[] preReadBytes() {
        byte[] ret = preReadBytes;
        preReadBytes = null;
        return ret;
    }

    public void postDataSend() throws IOException {
        if (!this.isConnected()) return;
        connectInputStream();
    }

    public Socket createSocket() throws IOException {
        return new Socket();
    }
}
