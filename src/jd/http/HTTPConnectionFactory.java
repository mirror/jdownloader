package jd.http;

import java.net.URL;

public class HTTPConnectionFactory {

    static URLConnectionAdapter createHTTPConnection(URL url, HTTPProxy proxy) {
        if (proxy == null) { return new HTTPConnection(url); }
        if (proxy.getType().equals(HTTPProxy.TYPE.DIRECT)) { return new HTTPConnection(url, proxy); }
        if (proxy.getType().equals(HTTPProxy.TYPE.SOCKS5)) { return new Socks5HTTPConnection(url, proxy); }
        if (proxy.getType().equals(HTTPProxy.TYPE.HTTP)) { return new HTTPProxyHTTPConnection(url, proxy); }
        throw new RuntimeException("unsupported proxy type: " + proxy.getType().name());
    }
}
