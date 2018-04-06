package jd.controlling.proxy;

import java.net.URL;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class EndPointConnectExceptionBan extends AbstractBan {
    private final URL url;

    public String getAuth() {
        return auth;
    }

    protected URL getURL() {
        return url;
    }

    public EndPointConnectExceptionBan(AbstractProxySelectorImpl selector, HTTPProxy proxy, URL url) {
        super(proxy, selector);
        this.url = url;
        created = System.currentTimeMillis();
    }

    protected String getHost() {
        return Browser.getHost(getURL());
    }

    @Override
    public String toString() {
        return _JDT.T.ConnectExceptionInPluginBan(getProtocol() + "://" + getHost().concat(":").concat(Integer.toString(getPort())));
    }

    protected int getPort() {
        return getPort(getURL());
    }

    protected String getProtocol() {
        return getURL().getProtocol();
    }

    protected long getExpireTimeout() {
        return 15 * 60 * 1000l;
    }

    @Override
    public boolean isSelectorBannedByPlugin(final Plugin plugin, final boolean ignoreConnectBans) {
        final String host = plugin.getHost();
        return !ignoreConnectBans && StringUtils.containsIgnoreCase(getHost(), host);
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(final HTTPProxy proxy, final URL url, final Plugin pluginFromThread, boolean ignoreConnectBans) {
        if (!ignoreConnectBans && proxyEquals(getProxy(), proxy)) {
            final boolean ret = getPort() == getPort(url) && StringUtils.containsIgnoreCase(getHost(), Browser.getHost(url)) && StringUtils.equals(getProtocol(), url.getProtocol());
            return ret;
        }
        return false;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() - getCreated() > getExpireTimeout() || super.isExpired();
    }

    private volatile long created = System.currentTimeMillis();

    protected long getCreated() {
        return created;
    }

    @Override
    public boolean canSwallow(ConnectionBan ban) {
        if (ban instanceof EndPointConnectExceptionBan) {
            final EndPointConnectExceptionBan other = (EndPointConnectExceptionBan) ban;
            if (proxyEquals(getProxy(), other.getProxy()) && getPort() == other.getPort() && StringUtils.equals(getHost(), other.getHost()) && StringUtils.equals(getProtocol(), other.getProtocol())) {
                created = Math.max(other.getCreated(), getCreated());
                return true;
            }
        }
        return false;
    }
}
