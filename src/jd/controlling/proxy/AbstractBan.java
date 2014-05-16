package jd.controlling.proxy;

public abstract class AbstractBan implements ConnectionBan {

    private AbstractProxySelectorImpl selector;

    public AbstractBan(AbstractProxySelectorImpl proxySelector) {
        this.selector = proxySelector;
    }

    public AbstractProxySelectorImpl getSelector() {
        return selector;
    }

}
