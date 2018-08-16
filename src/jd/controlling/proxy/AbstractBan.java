package jd.controlling.proxy;

import java.lang.ref.WeakReference;
import java.net.URL;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;

public abstract class AbstractBan implements ConnectionBan {
    protected final AbstractProxySelectorImpl selector;
    protected final WeakReference<HTTPProxy>  proxy;
    protected final String                    auth;

    public AbstractBan(HTTPProxy proxy, AbstractProxySelectorImpl proxySelector) {
        this.selector = proxySelector;
        this.proxy = new WeakReference<HTTPProxy>(proxy);
        this.auth = getAuth(proxy);
    }

    public AbstractProxySelectorImpl getSelector() {
        return selector;
    }

    protected HTTPProxy getProxy() {
        return proxy.get();
    }

    protected String getAuth() {
        return auth;
    }

    @Override
    public boolean isExpired() {
        final HTTPProxy proxy = getProxy();
        return proxy == null || !StringUtils.equals(getAuth(), getAuth(proxy));
    }

    protected int getPort(URL uri) {
        final int ret = uri.getPort();
        if (ret == -1) {
            final String scheme = uri.getProtocol();
            if (StringUtils.equalsIgnoreCase(scheme, "http")) {
                return 80;
            } else if (StringUtils.equalsIgnoreCase(scheme, "https")) {
                return 443;
            } else if (StringUtils.equalsIgnoreCase(scheme, "ftp")) {
                return 21;
            } else {
                return uri.getDefaultPort();
            }
        } else {
            return ret;
        }
    }

    protected String getAuth(HTTPProxy proxy) {
        if (proxy != null) {
            return Hash.getSHA256(proxy.getUser() + ":" + proxy.getPass());
        } else {
            return null;
        }
    }

    public boolean stringEquals(String a, String b) {
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
