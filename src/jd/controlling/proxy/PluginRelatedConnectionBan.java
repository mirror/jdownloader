package jd.controlling.proxy;

import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class PluginRelatedConnectionBan extends AbstractBan {
    private final String host;

    public PluginRelatedConnectionBan(Plugin plugin, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy) {
        super(proxy, proxySelector);
        host = plugin.getHost();
    }

    protected String getHost() {
        return host;
    }

    @Override
    public String toString() {
        final HTTPProxy proxy = getProxy();
        return _JDT.T.AuthExceptionGenericBan_toString_plugin(proxy == null ? "" : proxy.toString(), getHost());
    }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate, boolean ignoreConnectBans) {
        // auth is always a ban reason
        return true;
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL uri, Plugin pluginFromThread, boolean ignoreConnectBans) {
        final HTTPProxy proxy = getProxy();
        return proxy != null && proxy.equals(orgReference);
    }

    @Override
    public boolean canSwallow(ConnectionBan ban) {
        if (!(ban instanceof PluginRelatedConnectionBan)) {
            return false;
        }
        if (!proxyEquals(((PluginRelatedConnectionBan) ban).getProxy(), getProxy())) {
            return false;
        }
        // actually not really required. of one plugin is banned, all are banned
        if (!StringUtils.equalsIgnoreCase(((PluginRelatedConnectionBan) ban).getHost(), getHost())) {
            return false;
        }
        return true;
    }
}
