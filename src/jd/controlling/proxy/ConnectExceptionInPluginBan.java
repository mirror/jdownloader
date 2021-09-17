package jd.controlling.proxy;

import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.utils.Time;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class ConnectExceptionInPluginBan extends PluginRelatedConnectionBan {
    public ConnectExceptionInPluginBan(Plugin plg, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy) {
        super(plg, proxySelector, proxy);
    }

    @Override
    public String toString() {
        final HTTPProxy proxy = getProxy();
        return _JDT.T.ConnectExceptionInPluginBan_plugin(proxy == null ? "" : proxy.toString(), getHost());
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        return !ignoreConnectBans && super.isProxyBannedByUrlOrPlugin(orgReference, url, pluginFromThread, ignoreConnectBans);
    }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate, boolean ignoreConnectBans) {
        return !ignoreConnectBans;
    }

    private volatile long created = Time.systemIndependentCurrentJVMTimeMillis();

    @Override
    public boolean canSwallow(ConnectionBan ban) {
        if (!super.canSwallow(ban)) {
            return false;
        } else if (ban instanceof ConnectExceptionInPluginBan) {
            created = Math.max(((ConnectExceptionInPluginBan) ban).getCreated(), getCreated());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isExpired() {
        return Time.systemIndependentCurrentJVMTimeMillis() - getCreated() > 15 * 60 * 1000l || super.isExpired();
    }

    public long getCreated() {
        return created;
    }
}
