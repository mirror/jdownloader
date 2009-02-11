package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import jd.http.requests.Request;

public interface URLConnectionAdapter {

    Map getHeaderFields();

    String getHeaderField(String string);

    URL getURL();

    int getContentLength();
    long getLongContentLength();

    void setInstanceFollowRedirects(boolean followRedirects);

    void setReadTimeout(int readTimeout);

    void setConnectTimeout(int connectTimeout);

    void setRequestProperty(String key, String string);

    InputStream getInputStream()throws IOException;

    void setRequestMethod(String string)throws ProtocolException;

    void setDoOutput(boolean b);

    void connect() throws IOException;



    OutputStream getOutputStream()throws IOException;

    Map<String, List<String>> getRequestProperties();

    boolean getDoOutput();



    boolean isOK();

    long[] getRange();

    String getResponseMessage() throws IOException;

    int getResponseCode()  throws IOException;

    void disconnect();

    String getContentType();

    boolean isConnected();

    String getRequestProperty(String string);

    boolean isContentDisposition();

    void setRequest(Request request);
    public Request getRequest();

    

}
