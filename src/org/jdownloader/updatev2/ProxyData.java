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

    public boolean isRangeRequestsSupported() {
        return rangeRequestsSupported;
    }

    // public String getID() {
    // return ID;
    // }
    //
    // public void setID(String iD) {
    // ID = iD;
    // }

    /**
     * @return the enabled
     */

    /**
     * @param enabled
     *            the enabled to set
     */
    @Deprecated
    public void setProxyRotationEnabled(boolean enabled) {
        setEnabled(enabled);
        // setUseForPremiumEnabled(enabled);
    }

    private boolean enabled;

    // private boolean useForFreeEnabled;
    //
    // public boolean isUseForFreeEnabled() {
    // return useForFreeEnabled;
    // }
    //
    // public void setUseForFreeEnabled(boolean useForFreeEnabled) {
    // this.useForFreeEnabled = useForFreeEnabled;
    // }
    //
    // public boolean isUseForPremiumEnabled() {
    // return useForPremiumEnabled;
    // }
    //
    // public void setUseForPremiumEnabled(boolean useForPremiumEnabled) {
    // this.useForPremiumEnabled = useForPremiumEnabled;
    // }
    //
    // private boolean useForPremiumEnabled;

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
