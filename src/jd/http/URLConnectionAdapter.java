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
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface URLConnectionAdapter {

    public static enum METHOD {
        GET, POST, HEAD
    }

    Map<String, List<String>> getHeaderFields();

    String getHeaderField(String string);

    URL getURL();

    int getContentLength();

    long getLongContentLength();

    void setReadTimeout(int readTimeout);

    void setConnectTimeout(int connectTimeout);

    void setRequestProperty(String key, String string);

    InputStream getInputStream() throws IOException;

    void setRequestMethod(METHOD method);

    METHOD getRequestMethod();

    void connect() throws IOException;

    OutputStream getOutputStream() throws IOException;

    Map<String, String> getRequestProperties();

    boolean isOK();

    long[] getRange();

    String getResponseMessage() ;

    int getResponseCode();

    void disconnect();

    String getContentType();

    boolean isConnected();

    String getRequestProperty(String string);

    boolean isContentDisposition();

    void setRequest(Request request);

    public Request getRequest();

    public String getCharset();

    public void setCharset(String charset);

    public long getRequestTime();

    /**
     * returns bytearray that belongs to content, WARNING will get null after
     * first call
     * 
     * @return
     */
    public byte[] preReadBytes();

    /* needs to get called after postData is send */
    public void postDataSend() throws IOException;
}
