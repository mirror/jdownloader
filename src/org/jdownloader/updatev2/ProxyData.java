package org.jdownloader.updatev2;

import jd.controlling.proxy.FilterList;

import org.appwork.storage.Storable;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;

/*this class is for */
public class ProxyData implements Storable {

    public ProxyData() {
        // required by Storable
    }

    private boolean           proxyRotationEnabled   = true;
    private boolean           defaultProxy           = false;
    private HTTPProxyStorable proxy                  = null;
    private String            ID                     = null;

    private boolean           rangeRequestsSupported = true;

    private FilterList        filter;
    private boolean           pac                    = false;

    public boolean isRangeRequestsSupported() {
        return rangeRequestsSupported;
    }

    public String getID() {
        return ID;
    }

    public void setID(String iD) {
        ID = iD;
    }

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
