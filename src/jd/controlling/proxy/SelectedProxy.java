package jd.controlling.proxy;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class SelectedProxy extends HTTPProxy {

    protected final AbstractProxySelectorImpl selector;

    public SelectedProxy(AbstractProxySelectorImpl selector, HTTPProxy proxy) {
        super(proxy);
        this.selector = selector;
    }

    @Override
    public HTTPProxy clone() {
        return new SelectedProxy(getSelector(), this);
    }

    public AbstractProxySelectorImpl getSelector() {
        return selector;
    }
}
