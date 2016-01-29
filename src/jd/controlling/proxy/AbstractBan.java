package jd.controlling.proxy;

import java.net.URI;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;

public abstract class AbstractBan implements ConnectionBan {

    protected final AbstractProxySelectorImpl selector;

    public AbstractBan(AbstractProxySelectorImpl proxySelector) {
        this.selector = proxySelector;
    }

    public AbstractProxySelectorImpl getSelector() {
        return selector;
    }

    protected int getPort(URI uri) {
        final int ret = uri.getPort();
        if (ret == -1) {
            final String scheme = uri.getScheme();
            if (StringUtils.equalsIgnoreCase(scheme, "http")) {
                return 80;
            } else if (StringUtils.equalsIgnoreCase(scheme, "https")) {
                return 443;
            } else if (StringUtils.equalsIgnoreCase(scheme, "ftp")) {
                return 21;
            }
        }
        return ret;
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
