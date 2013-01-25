package org.jdownloader.updatev2;

import java.util.List;

import org.appwork.storage.Storable;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;

/*this class is for */
public class ProxyData implements Storable {

    public ProxyData() {
        // required by Storable
    }

    private boolean           proxyRotationEnabled = true;
    private boolean           defaultProxy         = false;
    private HTTPProxyStorable proxy                = null;
    private String            ID                   = null;
    private List<String>      permitDenyList       = null;
    private boolean           resumeIsAllowed      = false;

    public String getID() {
        return ID;
    }

    public List<String> getPermitDenyList() {
        return this.permitDenyList;
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

    public void setPermitDenyList(final List<String> permitDenyList) {
        this.permitDenyList = permitDenyList;
    }

    /**
     * @return the proxy
     */
    public HTTPProxyStorable getProxy() {
        return proxy;
    }

    public void setResumeIsAllowed(boolean b) {
        resumeIsAllowed = b;
    }

    public boolean isResumeAllowed() {
        return resumeIsAllowed;
    }

}
