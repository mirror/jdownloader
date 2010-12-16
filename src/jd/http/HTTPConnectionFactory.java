package jd.http;

import java.net.URL;

public class HTTPConnectionFactory {

    static URLConnectionAdapter createHTTPConnection(URL url, HTTPProxy proxy) {
        if (proxy == null) { return new HTTPConnection(url); }
        if (proxy.getType() == HTTPProxy.TYPE.DIRECT) { return new HTTPConnection(url, proxy); }
        if (proxy.getType() == HTTPProxy.TYPE.SOCKS5) { return new Socks5HTTPConnection(url, proxy); }
        throw new RuntimeException("unsupported proxy type: " + proxy.getType().name());
    }
}
