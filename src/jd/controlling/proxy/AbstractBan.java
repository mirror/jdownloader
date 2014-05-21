package jd.controlling.proxy;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;

public abstract class AbstractBan implements ConnectionBan {

    private AbstractProxySelectorImpl selector;

    public AbstractBan(AbstractProxySelectorImpl proxySelector) {
        this.selector = proxySelector;
    }

    public AbstractProxySelectorImpl getSelector() {
        return selector;
    }

    public boolean stringEquals(String a, String b) {
        if (a == null) {
            a = "";
        }
        if (b == null) {
            b = "";
        }
        return StringUtils.equals(a, b);
    }

    public boolean proxyEquals(HTTPProxy a, HTTPProxy b) {
        if (a == b) {
            return true;
        }
        if (a != null && b != null) {
            return a.getType() == b.getType() && stringEquals(a.getHost(), b.getHost()) && a.getPort() == b.getPort();
        }
        return false;
    }

}
