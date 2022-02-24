package org.jdownloader.updatev2;

import org.appwork.storage.Storable;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;

/*this class is for */
public class ProxyData implements Storable {
    public ProxyData() {
        // required by Storable
    }

    private HTTPProxyStorable proxy                  = null;
    // private String ID = null;
    private boolean           rangeRequestsSupported = true;
    private FilterList        filter;
    private boolean           pac                    = false;
    private boolean           reconnectSupported     = false;

    public boolean isReconnectSupported() {
        return reconnectSupported;
    }

    public void setReconnectSupported(boolean reconnectSupported) {
        this.reconnectSupported = reconnectSupported;
    }

    public boolean isRangeRequestsSupported() {
        return rangeRequestsSupported;
    }

    /**
     * @return the enabled
     */
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public void setRangeRequestsSupported(boolean b) {
        rangeRequestsSupported = b;
    }

    public void setFilter(FilterList filter) {
        this.filter = filter;
    }

    public FilterList getFilter() {
        return filter;
    }

    public void setPac(boolean b) {
        pac = b;
    }

    public boolean isPac() {
        return pac;
    }
}
