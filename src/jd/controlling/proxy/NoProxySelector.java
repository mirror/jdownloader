package jd.controlling.proxy;

import jd.http.Request;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.updatev2.ProxyData;

public class NoProxySelector extends SingleBasicProxySelectorImpl {

    public NoProxySelector() {
        super(HTTPProxy.NONE);

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
        switch (getProxy().getType()) {

        case NONE:
            return Type.NONE;

        default:
            throw new IllegalStateException();
        }
    }

    @Override
    public String toExportString() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != NoProxySelector.class) {
            return false;
        }
        return getProxy().equals(((SingleDirectGatewaySelector) obj).getProxy());
    }

    @Override
    protected boolean isLocal() {
        return true;
    }

    @Override
    public boolean updateProxy(Request request, int retryCounter) {
        return false;
    }

}
