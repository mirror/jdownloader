package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

import jd.nutils.encoding.Encoding;

public class HTTPProxyHTTPConnection extends HTTPConnection {

    public HTTPProxyHTTPConnection(URL url, HTTPProxy p) {
        super(url, p);
    }

    /* TODO: proxy to ssl and ssl proxy */
    @Override
    public void connect() throws IOException {
        if (proxy == null || !proxy.getType().equals(HTTPProxy.TYPE.HTTP)) { throw new IOException("HTTPProxyHTTPConnection: invalid HTTP Proxy!"); }
        if (proxy.getPass() == null || proxy.getUser() == null) { throw new IOException("HTTPProxyHTTPConnection: invalid auth info"); }
        if (proxy.getPass().length() > 0 || proxy.getUser().length() > 0) {
            /* add proxy auth */
            requestProperties.put("Proxy-Authorization", "Basic " + Encoding.Base64Encode(proxy.getUser() + ":" + proxy.getPass()));
        }
        if (isConnected()) return;/* oder fehler */
        httpSocket = createSocket();
        httpSocket.setSoTimeout(readTimeout);
        httpResponseCode = -1;
        InetAddress host = Inet4Address.getByName(proxy.getHost());
        long startTime = System.currentTimeMillis();
        httpSocket.connect(new InetSocketAddress(host, proxy.getPort()), connectTimeout);
        requestTime = System.currentTimeMillis() - startTime;
        httpPath = httpURL.toString();
        /* now send Request */
        StringBuilder sb = new StringBuilder();
        sb.append(httpMethod.name()).append(' ').append(httpPath).append(" HTTP/1.1\r\n");
        for (String key : this.requestProperties.keySet()) {
            if (requestProperties.get(key) == null) continue;
            sb.append(key).append(": ").append(requestProperties.get(key)).append("\r\n");
        }
        sb.append("\r\n");
        httpSocket.getOutputStream().write(sb.toString().getBytes("UTF-8"));
        httpSocket.getOutputStream().flush();
        if (httpMethod != METHOD.POST) {
            outputClosed = true;
            connectInputStream();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        connectInputStream();
        if ((getResponseCode() == 407)) proxy.setStatus(HTTPProxy.STATUS.INVALIDAUTH);
        if ((getResponseCode() >= 200 && getResponseCode() <= 400) || getResponseCode() == 404) {
            return inputStream;
        } else {
            throw new IOException(getResponseCode() + " " + getResponseMessage());
        }
    }
}
