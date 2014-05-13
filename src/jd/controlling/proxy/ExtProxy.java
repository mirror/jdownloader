package jd.controlling.proxy;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ExtProxy extends HTTPProxy {

    private AbstractProxySelectorImpl factory;

    public ExtProxy(AbstractProxySelectorImpl factory, HTTPProxy proxy) {
        this.factory = factory;
        cloneProxy(proxy);

    }

    @Override
    public HTTPProxy clone() {
        return new ExtProxy(factory, this);
    }

    public AbstractProxySelectorImpl getFactory() {
        return factory;
    }
}
