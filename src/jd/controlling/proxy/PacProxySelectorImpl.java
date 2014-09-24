package jd.controlling.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.plugins.Plugin;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.InternetConnectionSettings;
import org.jdownloader.updatev2.ProxyData;

import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.pac.PacScriptSource;
import com.btr.proxy.selector.pac.UrlPacScriptSource;

public class PacProxySelectorImpl extends AbstractProxySelectorImpl {

    private static final LogSource                  logger           = LogController.getInstance().getLogger(PacProxySelectorImpl.class.getName());

    private volatile String                         pacUrl;
    private final HashMap<String, PacProxySelector> selectors        = new HashMap<String, PacProxySelector>();
    private final AtomicLong                        latestValidation = new AtomicLong(-1);
    private final HashMap<String, HTTPProxy>        cacheMap         = new HashMap<String, HTTPProxy>();
    private final HashMap<String, String[]>         tempAuthMap      = new HashMap<String, String[]>();
    private boolean                                 nativeProxy      = false;

    public PacProxySelectorImpl(String url, String user, String pass) {
        this.pacUrl = url;
        this.user = user;
        this.password = pass;
    }

    public PacProxySelectorImpl(ProxyData proxyData) {
        this.pacUrl = proxyData.getProxy().getAddress();
        this.user = proxyData.getProxy().getUsername();
        this.password = proxyData.getProxy().getPassword();
        setEnabled(proxyData.isEnabled());
        setPreferNativeImplementation(proxyData.getProxy().isPreferNativeImplementation());
        setFilter(proxyData.getFilter());
    }

    @Override
    public List<HTTPProxy> getProxiesByUrl(String urlOrDomain) {
        List<HTTPProxy> ret = getProxyByUrlInternal(urlOrDomain);
        for (SelectProxyByUrlHook hook : selectProxyByUrlHooks) {
            hook.onProxyChoosen(urlOrDomain, ret);
        }
        return ret;
    }

    public List<HTTPProxy> getProxyByUrlInternal(String urlOrDomain) {
        PacProxySelector lSelector = getPacProxySelector();
        if (lSelector == null) {
            return null;
        }
        final ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
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
        if (url != null) {
            try {
                List<Proxy> result = lSelector.select(url.toURI());
                if (result != null) {
                    for (Proxy p : result) {
                        String ID = p.toString();
                        try {
                            HTTPProxy cached = null;
                            synchronized (this) {
                                cached = cacheMap.get(ID);
                                if (cached == null) {
                                    final HTTPProxy httpProxy;
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
                                        break;
                                    default:
                                        continue;
                                    }
                                    cached = new SelectedProxy(this, httpProxy);
                                    cacheMap.put(ID, cached);
                                }
                            }
                            cached.setPreferNativeImplementation(isPreferNativeImplementation());
                            if (cached.isRemote()) {
                                String pw = getPassword();
                                String us = getUser();
                                synchronized (this) {
                                    String[] tmp = tempAuthMap.get(toID(cached));
                                    if (tmp != null) {
                                        if (tmp[0] != null) {
                                            us = tmp[0];
                                        }
                                        if (tmp[1] != null) {
                                            pw = tmp[1];
                                        }
                                    }
                                }
                                cached.setPass(pw);
                                cached.setUser(us);
                            }
                            ret.add(cached);
                        } catch (Throwable e) {
                            logger.log(e);
                        }
                    }
                }
            } catch (Throwable e) {
                logger.log(e);
            }
        }
        return ret;
    }

    private synchronized PacProxySelector getPacProxySelector() {
        final String lPacURL = pacUrl;
        if (lPacURL.startsWith("pac://")) {
            PacScriptSource pacSource = new PacScriptSource() {
                @Override
                public String getScriptContent() throws IOException {
                    if (StringUtils.equalsIgnoreCase(lPacURL, "pac://")) {
                        return JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class).getLocalPacScript();
                    } else {
                        return Encoding.urlDecode(lPacURL.substring(6), false);
                    }
                }

                @Override
                public boolean isScriptValid() {
                    try {
                        String script = getScriptContent();
                        if (script == null || script.trim().length() == 0) {
                            logger.info("PAC script is empty. Skipping script!");
                            return false;
                        }
                        if (script.indexOf("FindProxyForURL") == -1) {
                            logger.info("PAC script entry point FindProxyForURL not found. Skipping script!");
                            return false;
                        }
                        return true;
                    } catch (IOException e) {
                        logger.log(e);
                        return false;
                    }
                }

            };
            if (pacSource.isScriptValid()) {
                return new PacProxySelector(pacSource);
            }
        } else {
            PacProxySelector selector = selectors.get(pacUrl);
            if (selector == null || System.currentTimeMillis() - latestValidation.get() > 15 * 60 * 1000l) {
                tempAuthMap.clear();
                cacheMap.clear();
                selectors.clear();
                latestValidation.set(-1);
                PacScriptSource pacSource = new UrlPacScriptSource(lPacURL);
                if (pacSource.isScriptValid()) {
                    selector = new PacProxySelector(pacSource);
                    latestValidation.set(System.currentTimeMillis());
                    selectors.put(lPacURL, selector);
                } else {
                    selector = null;
                }
            }
            return selector;
        }
        return null;
    }

    @Override
    public void setType(Type value) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    @Override
    public String toString() {
        String ret = "AutoProxy Script: " + getPACUrl();
        if (StringUtils.isNotEmpty(getUser())) {
            return getUser() + "@" + ret.toString();
        }
        return ret;
    }

    public ProxyData toProxyData() {
        ProxyData ret = super.toProxyData();
        HTTPProxyStorable proxy = new HTTPProxyStorable();
        proxy.setUsername(getUser());
        proxy.setPassword(getPassword());
        proxy.setAddress(getPACUrl());
        proxy.setPreferNativeImplementation(isPreferNativeImplementation());
        ret.setProxy(proxy);
        ret.setPac(true);
        return ret;
    }

    private String user;
    private String password;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        if (!StringUtils.equals(user, this.user)) {
            this.user = user;
            clearBanList();
        }
    }

    public void setPassword(String password) {
        if (!StringUtils.equals(password, this.password)) {
            this.password = password;
            clearBanList();
        }
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
        return obj == this || obj != null && obj.getClass().equals(PacProxySelectorImpl.class) && StringUtils.equalsIgnoreCase(getPACUrl(), ((PacProxySelectorImpl) obj).getPACUrl());
    }

    @Override
    public int hashCode() {
        return PacProxySelectorImpl.class.hashCode();
    }

    public void setPACUrl(String value) {
        if (!StringUtils.equals(pacUrl, value)) {
            this.pacUrl = value;
            clearBanList();
        }
    }

    @Override
    public String toExportString() {
        return null;
    }

    @Override
    public boolean isPreferNativeImplementation() {
        return nativeProxy;
    }

    @Override
    public void setPreferNativeImplementation(boolean preferNativeImplementation) {
        if (isPreferNativeImplementation() != preferNativeImplementation) {
            nativeProxy = preferNativeImplementation;
            clearBanList();
        }
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
        synchronized (this) {
            if (user2 == null && pass == null) {
                tempAuthMap.remove(toID(usedProxy));
            } else {
                tempAuthMap.put(toID(usedProxy), new String[] { user2, pass });
            }
        }
        usedProxy.setUser(user2 == null ? getUser() : user2);
        usedProxy.setPass(pass == null ? getPassword() : pass);
    }

    private String toID(HTTPProxy usedProxy) {
        return usedProxy.getHost() + ":" + usedProxy.getPort();
    }

    @Override
    public boolean isProxyBannedFor(HTTPProxy orgReference, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        // can orgRef be null? I doubt that. TODO:ensure
        synchronized (this) {
            if (!cacheMap.containsValue(orgReference)) {
                return false;
            }
        }
        return super.isProxyBannedFor(orgReference, url, pluginFromThread, ignoreConnectBans);
    }

    @Override
    public boolean isSelectorBannedFor(Plugin pluginForHost, boolean ignoreConnectionBans) {
        // actually, we cannot ban a pac selector at all, because there might always be a new proxy available.
        // in most cases however, once the selector is banned for a plugin - it is banned. This should work on most cases.
        // only pac scripts that have some kind of random proxy selection active would fail here
        return super.isSelectorBannedFor(pluginForHost, ignoreConnectionBans);
    }

    public boolean isSelectorBannedFor(URL url) {
        // pac might always have a working proxy, and thus is never blocked by an url only
        return false;
    }
}
