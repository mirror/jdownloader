package jd.controlling.proxy;

import java.util.ArrayList;
import java.util.List;

import jd.http.Request;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.jdownloader.updatev2.ProxyData;

public class NoProxySelector extends AbstractProxySelectorImpl {

    private ExtProxy        proxy;
    private List<HTTPProxy> list;

    public NoProxySelector() {

        proxy = new ExtProxy(this, new HTTPProxy(TYPE.NONE));
        // setFilter(proxyData.getFilter());
        list = new ArrayList<HTTPProxy>();
        list.add(proxy);

    }

    public ProxyData toProxyData() {
        ProxyData ret = new ProxyData();
        ret.setProxyRotationEnabled(this.isProxyRotationEnabled());
        ret.setProxy(HTTPProxy.getStorable(proxy));
        ret.setFilter(getFilter());
        ret.setID(this.ID);
        ret.setRangeRequestsSupported(isResumeAllowed());
        return ret;
    }

    @Override
    public List<HTTPProxy> getProxiesByUrl(String url) {

        return list;
    }

    public ExtProxy getProxy() {
        return proxy;
    }

    @Override
    public String toString() {
        return proxy.toString();
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
    public boolean isPreferNativeImplementation() {
        return proxy.isPreferNativeImplementation();
    }

    @Override
    public void setPreferNativeImplementation(boolean preferNativeImplementation) {

        proxy.setPreferNativeImplementation(preferNativeImplementation);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != NoProxySelector.class) return false;
        return proxy.equals(((NoProxySelector) obj).getProxy());
    }

    @Override
    public int hashCode() {
        return proxy.hashCode();
    }

    @Override
    public List<HTTPProxy> listProxies() {
        return list;
    }

    @Override
    public boolean setRotationEnabled(ExtProxy p, boolean enabled) {
        if (isProxyRotationEnabled() == enabled) return false;

        setProxyRotationEnabled(enabled);
        return true;
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
