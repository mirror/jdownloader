//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.net.Proxy;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;

/**
 * Diese Klasse verwendet Das delegate/Adapter Designpattern.
 * 
 * @author coalado
 * 
 */
public class HTTPsConnection extends HTTPConnection {

    private HttpsURLConnectionImpl delegate;

    @SuppressWarnings("unchecked")
    public HTTPsConnection(URLConnection openConnection, Proxy p) {
        super(openConnection.getURL(), p, null);
        delegate = (HttpsURLConnectionImpl) openConnection;
        requestProperties = new HashMap<String, List<String>>();

        Map<String, List<String>> tmp = delegate.getRequestProperties();
        Iterator<Entry<String, List<String>>> set = tmp.entrySet().iterator();
        while (set.hasNext()) {
            Entry<String, List<String>> next = set.next();
            requestProperties.put(next.getKey(), next.getValue());
        }
    }

    public void connect() throws IOException {
        this.connectionnEstabilished = true;
        delegate.connect();

    }

    public long getLongContentLength() {
        if (delegate.getHeaderField("content-length") == null) { return -1; }
        return Long.parseLong(delegate.getHeaderField("content-length"));

    }

    public int getContentLength() {
        if (delegate.getHeaderField("content-length") == null) { return -1; }
        return Integer.parseInt(delegate.getHeaderField("content-length"));
    }

    public String getContentType() {
        String type = delegate.getContentType();
        if (type == null) return "unknown";
        return type;
    }

    public InputStream getInputStream() throws IOException {

        if (delegate.getResponseCode() != 404) {
            return delegate.getInputStream();
        } else {
            return delegate.getErrorStream();
        }
    }

    public Map<String, List<String>> getRequestProperties() {

        return requestProperties;

    }

    // public void post(byte[] parameter) throws IOException {
    // BufferedOutputStream wr = new
    // BufferedOutputStream(delegate.getOutputStream());
    // if (parameter != null) {
    // wr.write(parameter);
    // }
    //
    // postData = "binary";
    // wr.flush();
    // wr.close();
    //
    // }
    //
    // public void post(String parameter) throws IOException {
    // OutputStreamWriter wr = new
    // OutputStreamWriter(delegate.getOutputStream());
    // if (parameter != null) {
    // wr.write(parameter);
    // }
    //
    // postData = parameter;
    // wr.flush();
    // wr.close();
    //
    // }

    public void setDoOutput(boolean b) {

        delegate.setDoOutput(b);
    }

    //
    // public void postGzip(String parameter) throws IOException {
    //
    // OutputStreamWriter wr = new
    // OutputStreamWriter(delegate.getOutputStream());
    // if (parameter != null) {
    // wr.write(parameter);
    // }
    // postData = parameter;
    // wr.flush();
    // wr.close();
    //
    // }

    public void setRequestProperty(String key, String value) {
        LinkedList<String> l = new LinkedList<String>();
        l.add(value);
        requestProperties.put(key, l);
        delegate.setRequestProperty(key, value);

    }

    public boolean isOK() {
        try {
            if (delegate.getResponseCode() > -2 && delegate.getResponseCode() < 400)
                return true;
            else
                return false;
        } catch (IOException e) {
            return false;
        }
    }

    public long[] getRange() {
        String range;
        if (ranges != null) return ranges;
        if ((range = delegate.getHeaderField("Content-Range")) == null) return null;
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

        return delegate.getHeaderField("Content-Disposition") != null;
    }

    public void disconnect() {
        if (isConnected()) {
            delegate.disconnect();
            this.connectionnEstabilished = false;
        }

    }

    @SuppressWarnings("unchecked")
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("-->" + this.getURL() + "\r\n");

        sb.append("----------------Request------------------\r\n");

        sb.append(getRequestMethod() + " " + getURL().getPath() + (getURL().getQuery() != null ? "?" + getURL().getQuery() : "") + " HTTP/1.1\r\n");
        // if (getURL().getPort() > 0 && getURL().getPort() != 80) {
        // sb.append("Host: " + getURL().getHost() + (":" + getURL().getPort())
        // + "\r\n");
        // } else {
        // sb.append("Host: " + getURL().getHost() + "\r\n");
        // }
        for (Iterator<Entry<String, List<String>>> it = this.getRequestProperties().entrySet().iterator(); it.hasNext();) {
            Entry<String, List<String>> next = it.next();
            StringBuilder value = new StringBuilder();
            for (String v : next.getValue()) {
                value.append(';');
                value.append(v);
            }
            String v = value.toString();
            if (v.length() > 0) v = v.substring(1);
            sb.append(next.getKey());
            sb.append(new char[] { ':', ' ' });
            sb.append(v);
            sb.append(new char[] { '\r', '\n' });
        }
        sb.append(new char[] { '\r', '\n' });

        if (this.getRequest() != null) {
            if (getRequest() instanceof PostRequest) {
                sb.append(((PostRequest) getRequest()).getPostDataString());
                sb.append(new char[] { '\r', '\n' });

            } else if (getRequest() instanceof PostFormDataRequest) {
                sb.append(((PostFormDataRequest) getRequest()).getPostDataString());
                sb.append(new char[] { '\r', '\n' });
            }

        }

        sb.append("----------------Response------------------\r\n");

        for (Iterator<Entry<String, List<String>>> it = delegate.getHeaderFields().entrySet().iterator(); it.hasNext();) {
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

    public OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    public InputStream getErrorStream() {
        return delegate.getErrorStream();
    }

    public boolean usingProxy() {
        return delegate.usingProxy();
    }

    /**
     * Gets a header field by name. Returns null if not known.
     * 
     * @param name
     *            the name of the header field
     */
    public String getHeaderField(String name) {
        return delegate.getHeaderField(name);
    }

    /**
     * Returns an unmodifiable Map of the header fields. The Map keys are
     * Strings that represent the response-header field names. Each Map value is
     * an unmodifiable List of Strings that represents the corresponding field
     * values.
     * 
     * @return a Map of header fields
     * @since 1.4
     */
    @SuppressWarnings("rawtypes")
    public Map getHeaderFields() {
        return delegate.getHeaderFields();
    }

    /**
     * Gets a header field by index. Returns null if not known.
     * 
     * @param n
     *            the index of the header field
     */
    public String getHeaderField(int n) {
        return delegate.getHeaderField(n);

    }

    /**
     * Gets a header field by index. Returns null if not known.
     * 
     * @param n
     *            the index of the header field
     */
    public String getHeaderFieldKey(int n) {
        return delegate.getHeaderFieldKey(n);
    }

    /**
     * Adds a general request property specified by a key-value pair. This
     * method will not overwrite existing values associated with the same key.
     * 
     * @param key
     *            the keyword by which the request is known (e.g., "
     *            <code>accept</code>").
     * @param value
     *            the value associated with it.
     * @see #getRequestProperties(java.lang.String)
     * @since 1.4
     */
    public void addRequestProperty(String key, String value) {
        delegate.addRequestProperty(key, value);
    }

    public String getRequestProperty(String key) {
        return delegate.getRequestProperty(key);
    }

    public void setConnectTimeout(int timeout) {
        delegate.setConnectTimeout(timeout);
    }

    /**
     * Returns setting for connect timeout.
     * <p>
     * 0 return implies that the option is disabled (i.e., timeout of infinity).
     * 
     * @return an <code>int</code> that indicates the connect timeout value in
     *         milliseconds
     * @see java.net.URLConnection#setConnectTimeout(int)
     * @see java.net.URLConnection#connect()
     * @since 1.5
     */
    public int getConnectTimeout() {
        return delegate.getConnectTimeout();
    }

    /**
     * Sets the read timeout to a specified timeout, in milliseconds. A non-zero
     * value specifies the timeout when reading from Input stream when a
     * connection is established to a resource. If the timeout expires before
     * there is data available for read, a java.net.SocketTimeoutException is
     * raised. A timeout of zero is interpreted as an infinite timeout.
     * 
     * <p>
     * Some non-standard implementation of this method ignores the specified
     * timeout. To see the read timeout set, please call getReadTimeout().
     * 
     * @param timeout
     *            an <code>int</code> that specifies the timeout value to be
     *            used in milliseconds
     * @throws IllegalArgumentException
     *             if the timeout parameter is negative
     * 
     * @see java.net.URLConnectiongetReadTimeout()
     * @see java.io.InputStream#read()
     * @since 1.5
     */
    public void setReadTimeout(int timeout) {
        delegate.setReadTimeout(timeout);
    }

    /**
     * Returns setting for read timeout. 0 return implies that the option is
     * disabled (i.e., timeout of infinity).
     * 
     * @return an <code>int</code> that indicates the read timeout value in
     *         milliseconds
     * 
     * @see java.net.URLConnection#setReadTimeout(int)
     * @see java.io.InputStream#read()
     * @since 1.5
     */
    public int getReadTimeout() {
        return delegate.getReadTimeout();
    }

}
