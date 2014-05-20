package jd.controlling.proxy;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ExtProxy extends HTTPProxy {

    private AbstractProxySelectorImpl selector;

    public ExtProxy(AbstractProxySelectorImpl factory, HTTPProxy proxy) {
        this.selector = factory;
        cloneProxy(proxy);

    }

    @Override
    public HTTPProxy clone() {
        return new ExtProxy(selector, this);
    }

    public AbstractProxySelectorImpl getSelector() {
        return selector;
    }
}
