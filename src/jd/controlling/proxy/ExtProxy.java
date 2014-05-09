package jd.controlling.proxy;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ExtProxy extends HTTPProxy {

    public ExtProxy(AbstractProxySelectorImpl factory, HTTPProxy proxy) {

        cloneProxy(proxy);

    }
}
