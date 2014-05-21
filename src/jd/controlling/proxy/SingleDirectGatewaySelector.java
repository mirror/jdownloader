package jd.controlling.proxy;

import jd.http.Request;

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
        return obj == this || obj != null && obj.getClass().equals(SingleDirectGatewaySelector.class) && getProxy().equals(((SingleDirectGatewaySelector) obj).getProxy());
    }

    @Override
    public void setUser(String user) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    public void setPassword(String password) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    public boolean updateProxy(Request request, int retryCounter) {
        return false;
    }

    @Override
    public int hashCode() {
        return SingleBasicProxySelectorImpl.class.hashCode();
    }

    @Override
    public void setPort(int port) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    public void setHost(String value) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    public void setType(Type value) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    protected boolean isLocal() {
        return true;
    }

    @Override
    public boolean isReconnectSupported() {
        return false;
    }

    @Override
    public Type getType() {
        if (HTTPProxy.TYPE.DIRECT.equals(getProxy().getType())) {
            return Type.DIRECT;
        } else {
            throw new IllegalStateException();
        }
    }

}
