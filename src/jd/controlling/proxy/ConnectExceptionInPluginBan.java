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
        return _JDT._.ConnectExceptionInPluginBan_plugin(proxy.toString(), plugin.getHost());
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        if (ignoreConnectBans) {
            return false;
        }
        return super.isProxyBannedByUrlOrPlugin(orgReference, url, pluginFromThread, ignoreConnectBans);
    }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate, boolean ignoreConnectBans) {
        if (ignoreConnectBans) {
            return false;
        }
        // auth is always a ban reason
        return true;
    }

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

    private long created;

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() - created > 15 * 60 * 1000l;
    }

    public long getCreated() {
        return created;
    }

}
