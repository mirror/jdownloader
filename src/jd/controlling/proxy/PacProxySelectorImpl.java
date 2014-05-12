package jd.controlling.proxy;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import jd.http.Request;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.ProxyData;

import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.pac.PacScriptSource;
import com.btr.proxy.selector.pac.UrlPacScriptSource;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogBackEnd;
import com.btr.proxy.util.Logger.LogLevel;

public class PacProxySelectorImpl extends AbstractProxySelectorImpl {

    private String                    pacUrl;

    private LogSource                 logger;

    private PacProxySelector          selector;

    private HashMap<String, ExtProxy> cacheMap;

    public PacProxySelectorImpl(String url, String user, String pass) {
        this.pacUrl = url;
        this.user = user;
        this.password = pass;
    }

    public PacProxySelectorImpl(ProxyData proxyData) {

        logger = LogController.getInstance().getLogger(PacProxySelectorImpl.class.getName());
        this.pacUrl = proxyData.getProxy().getAddress();
        this.user = proxyData.getProxy().getUsername();
        this.password = proxyData.getProxy().getPassword();
        setProxyRotationEnabled(proxyData.isProxyRotationEnabled());
        setPreferNativeImplementation(proxyData.getProxy().isPreferNativeImplementation());
        setFilter(proxyData.getFilter());
        // Setup ProxyVole Logger
        Logger.setBackend(new LogBackEnd() {

            @Override
            public void log(final Class<?> arg0, final LogLevel arg1, final String arg2, final Object... arg3) {

                logger.log(Level.ALL, arg2, arg3);
            }

            @Override
            public boolean isLogginEnabled(final LogLevel arg0) {

                return true;
            }
        });

        cacheMap = new HashMap<String, ExtProxy>();

    }

    @Override
    public List<HTTPProxy> getProxiesByUrl(String urlOrDomain) {
        updatePacScript();
        if (selector == null) return null;
        ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();

        URL url = null;
        try {
            url = new URL(urlOrDomain);
        } catch (MalformedURLException e) {
            try {
                url = new URL("http://" + urlOrDomain);
            } catch (MalformedURLException e1) {
                return null;
            }
        }
        try {

            List<Proxy> result = selector.select(url.toURI());
            if (result != null) {
                synchronized (this) {

                    for (Proxy p : result) {
                        try {
                            ExtProxy cached = cacheMap.get(p.toString());
                            if (cached == null) {
                                HTTPProxy httpProxy = null;
                                switch (p.type()) {
                                case DIRECT:
                                    if (p.address() == null) {
                                        httpProxy = new HTTPProxy(TYPE.NONE);
                                    } else {

                                        httpProxy = new HTTPProxy(((InetSocketAddress) p.address()).getAddress());
                                    }
                                    break;
                                case HTTP:
                                    httpProxy = new HTTPProxy(TYPE.HTTP, ((InetSocketAddress) p.address()).getHostString(), ((InetSocketAddress) p.address()).getPort());
                                    break;
                                case SOCKS:

                                    httpProxy = new HTTPProxy(TYPE.SOCKS5, ((InetSocketAddress) p.address()).getHostString(), ((InetSocketAddress) p.address()).getPort());
                                }

                                cached = new ExtProxy(this, httpProxy);
                                cacheMap.put(p.toString(), cached);
                            }

                            cached.setPreferNativeImplementation(isPreferNativeImplementation());
                            if (!cached.isNone()) {
                                cached.setPass(getPassword());
                                cached.setUser(getUser());
                            }
                            ret.add(cached);
                        } catch (Throwable e) {
                            logger.log(e);
                        }
                    }
                }
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        // final ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();
        //
        // final ProxySelector myProxySelector = proxySearch.getProxySelector();
        // List<Proxy> proxy = myProxySelector.select(new URL("http://www.youtube.com/").toURI());
        // System.out.println(proxy);
        // proxy = myProxySelector.select(new URL("http://google.com/").toURI());

        return ret;
    }

    private void updatePacScript() {
        PacScriptSource pacSource = new UrlPacScriptSource(pacUrl);
        if (pacSource.isScriptValid()) {
            selector = new PacProxySelector(pacSource);
        } else {
            this.selector = null;
        }
    }

    public void setType(Type value) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    public String toString() {
        String ret = "AutoProxy Script: " + getPACUrl();

        if (StringUtils.isNotEmpty(getUser())) {

        return getUser() + "@" + ret.toString(); }
        return ret;
    }

    @Override
    public List<HTTPProxy> listProxies() {
        return null;
    }

    public ProxyData toProxyData() {
        ProxyData ret = new ProxyData();
        ret.setProxyRotationEnabled(this.isProxyRotationEnabled());
        HTTPProxyStorable proxy = new HTTPProxyStorable();
        proxy.setUsername(getUser());
        proxy.setPassword(getPassword());
        proxy.setAddress(getPACUrl());
        proxy.setPreferNativeImplementation(isPreferNativeImplementation());
        ret.setProxy(proxy);
        ret.setFilter(getFilter());
        ret.setPac(true);
        ret.setID(this.ID);

        return ret;
    }

    private String user;
    private String password;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        if (StringUtils.equals(user, this.user)) return;
        this.user = user;
        // reset banlist
        banList = new ArrayList<ProxyBan>();
    }

    public void setPassword(String password) {
        if (StringUtils.equals(password, this.password)) return;
        this.password = password;
        // reset banlist
        banList = new ArrayList<ProxyBan>();

    }

    public String getPassword() {
        return password;
    }

    @Override
    public Type getType() {
        return Type.PAC;
    }

    public String getPACUrl() {
        return pacUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != PacProxySelectorImpl.class) return false;
        return StringUtils.equalsIgnoreCase(pacUrl, ((PacProxySelectorImpl) obj).getPACUrl());
    }

    @Override
    public int hashCode() {
        if (pacUrl == null) return "".hashCode();
        return pacUrl.hashCode();
    }

    public void setPACUrl(String value) {
        this.pacUrl = value;
    }

    @Override
    public String toExportString() {
        return getPACUrl();
    }

    @Override
    public boolean isPreferNativeImplementation() {
        return false;
    }

    @Override
    public void setPreferNativeImplementation(boolean preferNativeImplementation) {

    }

    @Override
    public boolean setRotationEnabled(ExtProxy p, boolean enabled) {
        if (isProxyRotationEnabled() == enabled) return false;

        setProxyRotationEnabled(enabled);
        return true;
    }

    @Override
    protected boolean isLocal() {
        return false;
    }

    @Override
    public boolean updateProxy(Request request, int retryCounter) {
        return ProxyController.getInstance().updateProxy(this, request, retryCounter);
    }

    public void setTempAuth(HTTPProxy usedProxy, String user2, String pass) {
        if (!cacheMap.containsKey(usedProxy.toString())) return;
        usedProxy.setUser(user2 == null ? getUser() : user2);
        usedProxy.setPass(pass == null ? getPassword() : pass);

    }
}
