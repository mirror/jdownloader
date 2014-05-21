package jd.controlling.proxy;

import jd.http.Request;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.updatev2.ProxyData;

public class NoProxySelector extends SingleBasicProxySelectorImpl {

    public NoProxySelector() {
        super(HTTPProxy.NONE);
    }

    public NoProxySelector(HTTPProxy httpData) {
        super(httpData);
    }

    public NoProxySelector(ProxyData proxyData) {
        super(HTTPProxy.NONE);
        setEnabled(proxyData.isEnabled());
        setFilter(proxyData.getFilter());
    }

    public void setType(Type value) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    public Type getType() {
        if (HTTPProxy.TYPE.NONE.equals(getProxy().getType())) {
            return Type.NONE;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public int hashCode() {
        return SingleBasicProxySelectorImpl.class.hashCode();
    }

    @Override
    public String toExportString() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass().equals(NoProxySelector.class) && getProxy().equals(((NoProxySelector) obj).getProxy());
    }

    @Override
    protected boolean isLocal() {
        return true;
    }

    @Override
    public boolean isReconnectSupported() {
        return true;
    }

    @Override
    public boolean updateProxy(Request request, int retryCounter) {
        return false;
    }

}
