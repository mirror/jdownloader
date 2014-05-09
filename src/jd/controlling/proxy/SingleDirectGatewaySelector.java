package jd.controlling.proxy;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.updatev2.ProxyData;

public class SingleDirectGatewaySelector extends SingleBasicProxySelectorImpl {

    public SingleDirectGatewaySelector(ProxyData proxyData) {
        super(proxyData);
    }

    public SingleDirectGatewaySelector(HTTPProxy proxyData) {
        super(proxyData);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != SingleDirectGatewaySelector.class) return false;
        return getProxy().equals(((SingleDirectGatewaySelector) obj).getProxy());
    }

    @Override
    public int hashCode() {
        return getProxy().hashCode();
    }

    public void setType(Type value) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    protected boolean isLocal() {
        return true;
    }

    @Override
    public Type getType() {
        switch (getProxy().getType()) {

        case DIRECT:
            return Type.DIRECT;

        default:
            throw new IllegalStateException();
        }
    }

}
