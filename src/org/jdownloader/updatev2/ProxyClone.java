package org.jdownloader.updatev2;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ProxyClone extends HTTPProxy {

    private final HTTPProxy orgReference;

    public HTTPProxy getOrgReference() {
        return orgReference;
    }

    public ProxyClone(HTTPProxy p) {
        super(p);
        this.orgReference = p;
    }

}
