package jd.controlling.proxy;

import java.lang.ref.WeakReference;
import java.net.URI;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class EndPointConnectExceptionBan extends AbstractBan {

    private final WeakReference<HTTPProxy> proxy;
    private final URI                      uri;

    protected URI getURI() {
        return uri;
    }

    public EndPointConnectExceptionBan(AbstractProxySelectorImpl selector, HTTPProxy proxy, URI uri) {
        super(selector);
        this.proxy = new WeakReference<HTTPProxy>(proxy);
        this.uri = uri;
    }

    protected HTTPProxy getProxy() {
        return proxy.get();
    }

    protected String getHost() {
        return Browser.getHostFromURI(getURI());
    }

    @Override
    public String toString() {
        return _JDT.T.ConnectExceptionInPluginBan(getScheme() + "://" + getHost().concat(":").concat(Integer.toString(getPort())));
    }

    protected int getPort() {
        return getPort(getURI());
    }

    protected String getScheme() {
        return getURI().getScheme();
    }

    @Override
    public boolean isSelectorBannedByPlugin(final Plugin plugin, final boolean ignoreConnectBans) {
        final String host = plugin.getHost();
        return !ignoreConnectBans && StringUtils.containsIgnoreCase(getHost(), host);
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(final HTTPProxy proxy, final URI uri, final Plugin pluginFromThread, boolean ignoreConnectBans) {
        if (!ignoreConnectBans && proxyEquals(getProxy(), proxy)) {
            final boolean ret = getPort() == getPort(uri) && StringUtils.containsIgnoreCase(getHost(), Browser.getHostFromURI(uri)) && StringUtils.equals(getScheme(), uri.getScheme());
            return ret;
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
            if (proxyEquals(getProxy(), other.getProxy()) && getPort() == other.getPort() && StringUtils.equals(getHost(), other.getHost()) && StringUtils.equals(getScheme(), other.getScheme())) {
                return true;
            }
        }
        return false;
    }
}
