package jd.controlling.proxy;

import java.net.URI;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class GenericConnectExceptionBan extends AuthExceptionGenericBan {

    public GenericConnectExceptionBan(AbstractProxySelectorImpl selector, HTTPProxy orgReference, URI url) {
        super(selector, orgReference, url);
        created = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        final URI uri = getURI();
        if (uri != null) {
            return _JDT.T.ConnectExceptionInPluginBan(Browser.getHostFromURI(uri).concat(":").concat(Integer.toString(getPort(uri))));
        }
        final HTTPProxy proxy = getProxy();
        return _JDT.T.ConnectExceptionInPluginBan(proxy == null ? "" : proxy.toString());
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URI uri, Plugin pluginFromThread, boolean ignoreConnectBans) {
        return !ignoreConnectBans && super.isProxyBannedByUrlOrPlugin(orgReference, uri, pluginFromThread, ignoreConnectBans);
    }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate, boolean ignoreConnectionBans) {
        return !ignoreConnectionBans && super.isSelectorBannedByPlugin(candidate, ignoreConnectionBans);
    }

    @Override
    public boolean canSwallow(ConnectionBan ban) {
        if (!super.canSwallow(ban)) {
            return false;
        }
        if (ban instanceof GenericConnectExceptionBan) {
            created = Math.max(((GenericConnectExceptionBan) ban).created, created);
            return true;
        }
        return false;
    }

    private volatile long created;

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() - created > 15 * 60 * 1000l || super.isExpired();
    }

}
