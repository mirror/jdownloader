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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.parser.Regex;

public class HTTPConnection extends sun.net.www.protocol.http.HttpURLConnection {

    public static final int HTTP_NOT_IMPLEMENTED = HttpURLConnection.HTTP_NOT_IMPLEMENTED;

    private String postData;

    private HashMap<String, List<String>> requestProperties = null;
    private long[] ranges;
    private boolean connected = false;

    private JDProxy proxy = null;

    public boolean isConnected() {
        return connected;
    }

    public HTTPConnection(URL url, Proxy p, sun.net.www.protocol.http.Handler handler) {

        super(url, p, handler);
        this.proxy = (JDProxy) p;
        requestProperties = new HashMap<String, List<String>>();

        Map<String, List<String>> tmp = getRequestProperties();
        Iterator<Entry<String, List<String>>> set = tmp.entrySet().iterator();
        while (set.hasNext()) {
            Entry<String, List<String>> next = set.next();
            requestProperties.put(next.getKey(), next.getValue());
        }
    }

    /**
     * Returns the proxy that has been used for this connection
     * 
     * @return
     */
    public JDProxy getProxy() {
        return proxy;
    }

    public void connect() throws IOException {
        this.connected = true;
        super.connect();

    }

    public long getLongContentLength() {
        if (getHeaderField("content-length") == null) { return -1; }

        return Long.parseLong(getHeaderField("content-length"));

    }

    public int getContentLength() {

        return Integer.parseInt(getHeaderField("content-length"));

    }

    public String getContentType() {
        String type = super.getContentType();
        if (type == null) return "unknown";
        return type;
    }

    public HttpURLConnection getHTTPURLConnection() {
        return this;

    }

    public InputStream getInputStream() throws IOException {

        // DO NOT CALL getResponseCode() here!
        if (responseCode != 404) {
            return super.getInputStream();
        } else {
            return super.getErrorStream();
        }
    }

    public String getPostData() {
        // AuthenticationInfo d = new
        // sun.net.www.protocol.http.AuthenticationInfo();
        // this.setDefaultAuthenticator(a);
        return postData;
    }

    public Map<String, List<String>> getRequestProperties() {
    
        return requestProperties;

    }

    public void post(byte[] parameter) throws IOException {
        BufferedOutputStream wr = new BufferedOutputStream(getOutputStream());
        if (parameter != null) {
            wr.write(parameter);
        }

        postData = "binary";
        wr.flush();
        wr.close();

    }

    public void post(String parameter) throws IOException {
        OutputStreamWriter wr = new OutputStreamWriter(getOutputStream());
        if (parameter != null) {
            wr.write(parameter);
        }

        postData = parameter;
        wr.flush();
        wr.close();

    }

    public void postGzip(String parameter) throws IOException {

        OutputStreamWriter wr = new OutputStreamWriter(getOutputStream());
        if (parameter != null) {
            wr.write(parameter);
        }
        postData = parameter;
        wr.flush();
        wr.close();

    }

    public void setRequestProperty(String key, String value) {
        LinkedList<String> l = new LinkedList<String>();
        l.add(value);
        requestProperties.put(key, l);
        super.setRequestProperty(key, value);

    }

    public boolean isOK() {
        try {
            if (getResponseCode() > -2 && getResponseCode() < 400)
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
            super.disconnect();
        }

    }

    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("-->" + this.getURL() + "\r\n");
  
   
 
        sb.append("----------------Request------------------\r\n");
        
        
        
        sb.append(getRequestMethod() + " " + getURL().getPath() + (getURL().getQuery() != null ? "?" + getURL().getQuery() : "") + " HTTP/1.1\r\n");
        sb.append("Host: " + getURL().getHost() + (":" + getURL().getPort()) + "\r\n");
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

        if (this.postData != null) {
            sb.append(this.postData);
            sb.append(new char[] { '\r', '\n' });
        }
        sb.append("----------------Response------------------\r\n");

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
}
