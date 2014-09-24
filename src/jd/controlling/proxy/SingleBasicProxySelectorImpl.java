package jd.controlling.proxy;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jd.http.Request;
import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.updatev2.ProxyData;

public class SingleBasicProxySelectorImpl extends AbstractProxySelectorImpl {

    private final SelectedProxy   proxy;
    private final List<HTTPProxy> list;

    @Override
    public String toString() {
        String ret = proxy.toString();
        if (StringUtils.isNotEmpty(getUser())) {
            return getUser() + "@" + ret.toString();
        }
        return ret;
    }

    private String username;
    private String password;
    private String tempUser;
    private String tempPass;

    public ProxyData toProxyData() {
        ProxyData ret = super.toProxyData();
        HTTPProxyStorable storable = HTTPProxy.getStorable(getProxy());
        storable.setUsername(username);
        storable.setPassword(password);
        storable.setPreferNativeImplementation(isPreferNativeImplementation());
        ret.setProxy(storable);
        return ret;
    }

    @Override
    public boolean updateProxy(Request request, int retryCounter) {
        return ProxyController.getInstance().updateProxy(this, request, retryCounter);
    }

    public SingleBasicProxySelectorImpl(ProxyData proxyData) {
        proxy = new SelectedProxy(this, HTTPProxy.getHTTPProxy(proxyData.getProxy()));
        setFilter(proxyData.getFilter());
        proxy.setConnectMethodPrefered(proxyData.getProxy().isConnectMethodPrefered());
        setResumeAllowed(proxyData.isRangeRequestsSupported());
        setEnabled(proxyData.isEnabled());
        username = proxy.getUser();
        password = proxy.getPass();
        ArrayList<HTTPProxy> list = new ArrayList<HTTPProxy>();
        list.add(proxy);
        this.list = Collections.unmodifiableList(list);
    }

    public SingleBasicProxySelectorImpl(HTTPProxy rawProxy) {
        proxy = new SelectedProxy(this, rawProxy);
        username = proxy.getUser();
        password = proxy.getPass();
        ArrayList<HTTPProxy> list = new ArrayList<HTTPProxy>();
        list.add(proxy);
        this.list = Collections.unmodifiableList(list);
    }

    @Override
    public List<HTTPProxy> getProxiesByUrl(String url) {

        for (SelectProxyByUrlHook hook : selectProxyByUrlHooks) {
            hook.onProxyChoosen(url, list);
        }

        return list;
    }

    public SelectedProxy getProxy() {
        return proxy;
    }

    @Override
    public Type getType() {
        switch (getProxy().getType()) {
        case HTTP:
            return Type.HTTP;
        case SOCKS4:
            return Type.SOCKS4;
        case SOCKS5:
            return Type.SOCKS5;
        default:
            throw new IllegalStateException();
        }
    }

    public void setType(Type value) {
        switch (value) {
        case HTTP:
            proxy.setType(TYPE.HTTP);
        case SOCKS4:
            proxy.setType(TYPE.SOCKS4);
        case SOCKS5:
            proxy.setType(TYPE.SOCKS5);
        default:
            throw new IllegalStateException("Illegal Operation");
        }
    }

    public void setUser(String user) {
        final SelectedProxy proxy = getProxy();
        if (!StringUtils.equals(user, proxy.getUser())) {
            proxy.setUser(user);
            username = user;
            tempUser = null;
            tempPass = null;
            clearBanList();
        }
    }

    public void setPassword(String pass) {
        final SelectedProxy proxy = getProxy();
        if (!StringUtils.equals(pass, proxy.getPass())) {
            proxy.setPass(pass);
            password = pass;
            tempUser = null;
            tempPass = null;
            clearBanList();
        }
    }

    public String getPassword() {
        String ret = tempPass;
        if (ret != null) {
            return "(Temp)" + ret;
        }
        return password;
    }

    private String _getPassword() {
        String ret = tempPass;
        if (ret != null) {
            return ret;
        }
        return password;
    }

    private String _getUsername() {
        String ret = tempUser;
        if (ret != null) {
            return ret;
        }
        return username;
    }

    public String getUser() {
        String ret = tempUser;
        if (ret != null) {
            return "(Temp)" + ret;
        }
        return username;
    }

    @Override
    public boolean isPreferNativeImplementation() {
        return getProxy().isPreferNativeImplementation();
    }

    @Override
    public void setPreferNativeImplementation(boolean preferNativeImplementation) {
        if (isPreferNativeImplementation() != preferNativeImplementation) {
            getProxy().setPreferNativeImplementation(preferNativeImplementation);
            clearBanList();
        }
    }

    public int getPort() {
        return getProxy().getPort();
    }

    public void setPort(int port) {
        final SelectedProxy proxy = getProxy();
        if (proxy.getPort() != port) {
            proxy.setPort(port);
            clearBanList();
        }
    }

    public void setHost(String value) {
        final SelectedProxy proxy = getProxy();
        if (!StringUtils.equals(value, proxy.getHost())) {
            proxy.setHost(value);
            clearBanList();
        }
    }

    public String getHost() {
        return getProxy().getHost();
    }

    @Override
    public String toExportString() {
        StringBuilder sb = new StringBuilder();
        boolean hasUSerInfo = false;
        SelectedProxy proxy = getProxy();
        switch (proxy.getType()) {
        case HTTP:
            sb.append("http://");
            break;
        case SOCKS4:
            sb.append("socks4://");
            break;
        case SOCKS5:
            sb.append("socks5://");
            break;
        case DIRECT:
            if (proxy.getLocalIP() != null) {
                sb.append("direct://");
                final String ip = proxy.getLocalIP().getHostAddress();
                sb.append(ip);
                return sb.toString();
            }
            return null;
        default:
            return null;
        }
        final String username = _getUsername();
        if (!StringUtils.isEmpty(username)) {
            sb.append(username);
            hasUSerInfo = true;
        }
        final String password = _getPassword();
        if (!StringUtils.isEmpty(password)) {
            if (hasUSerInfo) {
                sb.append(":");
            }
            hasUSerInfo = true;
            sb.append(password);
        }
        if (hasUSerInfo) {
            sb.append("@");
        }
        sb.append(getHost());
        if (getPort() > 0) {
            sb.append(":");
            sb.append(getPort());
        }
        return sb.toString();

    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass().equals(SingleBasicProxySelectorImpl.class) && getProxy().equals(((SingleBasicProxySelectorImpl) obj).getProxy());
    }

    @Override
    public int hashCode() {
        return SingleBasicProxySelectorImpl.class.hashCode();
    }

    @Override
    protected boolean isLocal() {
        return false;
    }

    @Override
    public boolean isProxyBannedFor(HTTPProxy orgReference, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        // can orgRef be null? I doubt that. TODO:ensure
        if (!getProxy().equals(orgReference)) {
            return false;
        }
        return super.isProxyBannedFor(orgReference, url, pluginFromThread, ignoreConnectBans);
    }

    public void setTempAuth(String user, String pass) {
        final SelectedProxy proxy = getProxy();
        proxy.setUser(user == null ? username : user);
        proxy.setPass(pass == null ? password : pass);
        this.tempUser = user;
        this.tempPass = pass;
        if (user != null || pass != null) {
            clearBanList();
        }
    }

}
