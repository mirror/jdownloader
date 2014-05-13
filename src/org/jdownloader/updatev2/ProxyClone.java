package org.jdownloader.updatev2;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ProxyClone extends HTTPProxy {

    private HTTPProxy orgReference;

    public HTTPProxy getOrgReference() {
        return orgReference;
    }

    public ProxyClone(HTTPProxy p) {
        this.cloneProxy(p);
        this.orgReference = p;
    }

}
