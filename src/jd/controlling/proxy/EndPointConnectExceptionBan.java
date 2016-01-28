package jd.controlling.proxy;

import java.lang.ref.WeakReference;
import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class EndPointConnectExceptionBan extends AbstractBan {

    private final WeakReference<HTTPProxy> proxy;
    private final URL                      url;

    protected URL getURL() {
        return url;
    }

    public EndPointConnectExceptionBan(AbstractProxySelectorImpl selector, HTTPProxy proxy, URL url) {
        super(selector);
        this.proxy = new WeakReference<HTTPProxy>(proxy);
        this.url = url;
    }

    protected HTTPProxy getProxy() {
        return proxy.get();
    }

    protected String getHost() {
        return getURL().getHost();
    }

    @Override
    public String toString() {
        return _JDT._.ConnectExceptionInPluginBan(getHost().concat(":").concat(Integer.toString(getPort())));
    }

    protected int getPort() {
        return getPort(getURL());
    }

    protected int getPort(URL url) {
        final int ret = url.getPort();
        if (ret == -1) {
            return url.getDefaultPort();
        } else {
            return ret;
        }
    }

    @Override
    public boolean isSelectorBannedByPlugin(final Plugin plugin, final boolean ignoreConnectBans) {
        final String host = plugin.getHost();
        return !ignoreConnectBans && StringUtils.containsIgnoreCase(getHost(), host);
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy proxy, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        if (!ignoreConnectBans && proxyEquals(getProxy(), proxy)) {
            return StringUtils.containsIgnoreCase(getHost(), url.getHost()) && getPort() == getPort(url);
        }
        return false;
    }

    @Override
    public boolean isExpired() {
        return getProxy() == null;
    }

    @Override
    public boolean canSwallow(ConnectionBan ban) {
        if (ban instanceof EndPointConnectExceptionBan) {
            final EndPointConnectExceptionBan other = (EndPointConnectExceptionBan) ban;
            if (proxyEquals(getProxy(), other.getProxy()) && StringUtils.equals(getHost(), other.getHost()) && getPort() == other.getPort()) {
                return true;
            }
        }
        return false;
    }
}
