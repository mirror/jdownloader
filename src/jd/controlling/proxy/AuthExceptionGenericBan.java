package jd.controlling.proxy;

import java.lang.ref.WeakReference;
import java.net.URI;

import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class AuthExceptionGenericBan extends AbstractBan {

    private final WeakReference<HTTPProxy> proxy;
    private final URI                      uri;

    public URI getURI() {
        return uri;
    }

    public AuthExceptionGenericBan(AbstractProxySelectorImpl proxySelector, HTTPProxy proxy, URI uri) {
        super(proxySelector);
        this.proxy = new WeakReference<HTTPProxy>(proxy);
        this.uri = uri;
    }

    protected HTTPProxy getProxy() {
        return proxy.get();
    }

    @Override
    public String toString() {
        final HTTPProxy proxy = getProxy();
        return _JDT.T.AuthExceptionGenericBan_toString(proxy == null ? "" : proxy.toString());
    }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate, boolean ignoreConnectionBans) {
        return true;
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URI uri, Plugin pluginFromThread, boolean ignoreConnectBans) {
        final HTTPProxy proxy = getProxy();
        return proxy != null && proxy.equals(orgReference);
    }

    @Override
    public boolean isExpired() {
        return getProxy() == null;
    }

    @Override
    public boolean canSwallow(ConnectionBan ban) {
        if (!(ban instanceof AuthExceptionGenericBan)) {
            return false;
        }
        if (!proxyEquals(((AuthExceptionGenericBan) ban).getProxy(), getProxy())) {
            return false;
        }
        return true;
    }

}
