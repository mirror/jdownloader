package jd.controlling.proxy;

import java.util.ArrayList;
import java.util.List;

import jd.http.Request;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.updatev2.ProxyData;

public class SingleBasicProxySelectorImpl extends AbstractProxySelectorImpl {

    private ExtProxy        proxy;
    private List<HTTPProxy> list;

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
        ProxyData ret = new ProxyData();
        ret.setProxyRotationEnabled(this.isProxyRotationEnabled());
        ret.setFilter(getFilter());
        HTTPProxyStorable storable = HTTPProxy.getStorable(proxy);
        storable.setUsername(username);
        storable.setPassword(password);
        ret.setProxy(storable);
        ret.setID(this.ID);
        ret.setRangeRequestsSupported(isResumeAllowed());
        return ret;
    }

    @Override
    public boolean updateProxy(Request request, int retryCounter) {

        return ProxyController.getInstance().updateProxy(this, request, retryCounter);
    }

    public SingleBasicProxySelectorImpl(ProxyData proxyData) {

        ID = proxyData.getID();
        proxy = new ExtProxy(this, HTTPProxy.getHTTPProxy(proxyData.getProxy()));
        setFilter(proxyData.getFilter());
        proxy.setConnectMethodPrefered(proxyData.getProxy().isConnectMethodPrefered());
        setResumeAllowed(proxyData.isRangeRequestsSupported());
        setProxyRotationEnabled(proxyData.isProxyRotationEnabled());
        username = proxy.getUser();
        password = proxy.getPass();
        if (ID == null) {

            ID = proxy.getType().name() + IDs.incrementAndGet() + "_" + System.currentTimeMillis();

        }
        list = new ArrayList<HTTPProxy>();
        list.add(proxy);

    }

    public SingleBasicProxySelectorImpl(HTTPProxy rawProxy) {
        proxy = new ExtProxy(this, rawProxy);
        username = proxy.getUser();
        password = proxy.getPass();
        this.ID = proxy.getType().name() + IDs.incrementAndGet() + "_" + System.currentTimeMillis();
        list = new ArrayList<HTTPProxy>();
        list.add(proxy);

    }

    @Override
    public List<HTTPProxy> getProxiesByUrl(String url) {

        return list;
    }

    public ExtProxy getProxy() {
        return proxy;
    }

    @Override
    public Type getType() {
        switch (proxy.getType()) {

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
        if (StringUtils.equals(user, proxy.getUser()))
            return;
        username = user;
        tempUser = null;
        tempPass = null;
        proxy.setUser(user);
        // reset banlist
        banList = new ArrayList<ProxyBan>();
    }

    public void setPassword(String password) {
        if (StringUtils.equals(password, proxy.getPass()))
            return;
        proxy.setPass(password);
        this.password = password;
        tempUser = null;
        tempPass = null;
        // reset banlist
        banList = new ArrayList<ProxyBan>();

    }

    public String getPassword() {
        if (tempPass != null)
            return "(Temp)" + tempPass;
        return password;
    }

    public String getUser() {
        if (tempUser != null)
            return "(Temp)" + tempUser;
        return username;
    }

    @Override
    public boolean isPreferNativeImplementation() {
        return proxy.isPreferNativeImplementation();
    }

    @Override
    public void setPreferNativeImplementation(boolean preferNativeImplementation) {

        proxy.setPreferNativeImplementation(preferNativeImplementation);
    }

    @Override
    public boolean setRotationEnabled(ExtProxy p, boolean enabled) {
        if (isProxyRotationEnabled() == enabled)
            return false;

        setProxyRotationEnabled(enabled);
        return true;
    }

    public int getPort() {
        return proxy.getPort();
    }

    public void setPort(int port) {
        proxy.setPort(port);
    }

    public void setHost(String value) {
        proxy.setHost(value);
    }

    public String getHost() {
        return proxy.getHost();
    }

    @Override
    public String toExportString() {
        StringBuilder sb = new StringBuilder();
        boolean hasUSerInfo = false;
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
        default:
            return null;
        }
        if (!StringUtils.isEmpty(getUser())) {
            sb.append(getUser());
            hasUSerInfo = true;
        }
        if (!StringUtils.isEmpty(getPassword())) {
            if (hasUSerInfo)
                sb.append(":");
            hasUSerInfo = true;
            sb.append(getPassword());
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
        if (obj == null || obj.getClass() != SingleBasicProxySelectorImpl.class)
            return false;
        return proxy.equals(((SingleBasicProxySelectorImpl) obj).getProxy());
    }

    @Override
    public int hashCode() {
        return proxy.hashCode();
    }

    @Override
    public List<HTTPProxy> listProxies() {
        return list;
    }

    @Override
    protected boolean isLocal() {
        return false;
    }

    public void setTempAuth(String user, String pass) {

        proxy.setUser(user == null ? username : user);
        proxy.setPass(pass == null ? password : pass);
        tempUser = user;
        this.tempPass = pass;
    }

    @Override
    protected void onBanListUpdate() {
        if (isBanned(proxy)) {
            // empty
            list = new ArrayList<HTTPProxy>();
        } else {
            ArrayList<HTTPProxy> tmp = new ArrayList<HTTPProxy>();
            tmp.add(proxy);
            list = tmp;
        }
    }

}
