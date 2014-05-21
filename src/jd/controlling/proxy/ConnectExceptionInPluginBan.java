package jd.controlling.proxy;

import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class ConnectExceptionInPluginBan extends PluginRelatedConnectionBan {

    public ConnectExceptionInPluginBan(Plugin plg, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy) {
        super(plg, proxySelector, proxy);
        created = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        HTTPProxy proxy = getProxy();
        return _JDT._.ConnectExceptionInPluginBan_plugin(proxy == null ? "" : proxy.toString(), getHost());
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        return !ignoreConnectBans && super.isProxyBannedByUrlOrPlugin(orgReference, url, pluginFromThread, ignoreConnectBans);
    }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate, boolean ignoreConnectBans) {
        return !ignoreConnectBans;
    }

    private volatile long created;

    @Override
    public boolean canSwallow(ConnectionBan ban) {
        if (!super.canSwallow(ban)) {
            return false;
        }
        if (ban instanceof ConnectExceptionInPluginBan) {
            created = Math.max(((ConnectExceptionInPluginBan) ban).created, created);
            return true;
        }
        return false;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() - created > 15 * 60 * 1000l || super.isExpired();
    }

    public long getCreated() {
        return created;
    }

}
