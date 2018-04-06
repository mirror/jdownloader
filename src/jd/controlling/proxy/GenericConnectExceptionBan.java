package jd.controlling.proxy;

import java.net.URL;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class GenericConnectExceptionBan extends AuthExceptionGenericBan {
    public GenericConnectExceptionBan(AbstractProxySelectorImpl selector, HTTPProxy orgReference, URL url) {
        super(selector, orgReference, url);
    }

    protected long getExpireTimeout() {
        return 15 * 60 * 1000l;
    }

    @Override
    public String toString() {
        final URL url = getURL();
        if (url != null) {
            return _JDT.T.ConnectExceptionInPluginBan(Browser.getHost(url).concat(":").concat(Integer.toString(getPort(url))));
        }
        final HTTPProxy proxy = getProxy();
        return _JDT.T.ConnectExceptionInPluginBan(proxy == null ? "" : proxy.toString());
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        return !ignoreConnectBans && super.isProxyBannedByUrlOrPlugin(orgReference, url, pluginFromThread, ignoreConnectBans);
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
            created = Math.max(((GenericConnectExceptionBan) ban).getCreated(), getCreated());
            return true;
        }
        return false;
    }

    private volatile long created = System.currentTimeMillis();

    protected long getCreated() {
        return created;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() - getCreated() > getExpireTimeout() || super.isExpired();
    }
}
