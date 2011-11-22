package jd.controlling.proxy;

import org.appwork.storage.Storable;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;

/*this class is for */
public class ProxyData implements Storable {

    public ProxyData() {
        // required by Storable
    }

    public static enum StatusID {
        OK,
        OFFLINE,
        INVALIDAUTH
    }

    /**
     * @return the status
     */
    public StatusID getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(StatusID status) {
        this.status = status;
    }

    private StatusID          status               = StatusID.OK;
    private boolean           proxyRotationEnabled = true;
    private boolean           defaultProxy;
    private HTTPProxyStorable proxy                = null;

    public boolean isDefaultProxy() {
        return defaultProxy;
    }

    public void setDefaultProxy(boolean defaultProxy) {
        this.defaultProxy = defaultProxy;
    }

    /**
     * @return the enabled
     */
    public boolean isProxyRotationEnabled() {
        return proxyRotationEnabled;
    }

    /**
     * @param enabled
     *            the enabled to set
     */
    public void setProxyRotationEnabled(boolean enabled) {
        this.proxyRotationEnabled = enabled;
    }

    /**
     * @param proxy
     *            the proxy to set
     */
    public void setProxy(HTTPProxyStorable proxy) {
        this.proxy = proxy;
    }

    /**
     * @return the proxy
     */
    public HTTPProxyStorable getProxy() {
        return proxy;
    }

}
