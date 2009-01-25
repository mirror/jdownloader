package jd.http;

import java.net.Proxy;
import java.net.SocketAddress;

public class JDProxy extends Proxy {

    public JDProxy(Type type, SocketAddress sa) {
        super(type, sa);

    }

}
