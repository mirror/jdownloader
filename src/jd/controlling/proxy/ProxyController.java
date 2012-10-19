package jd.controlling.proxy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jd.config.SubConfiguration;
import jd.controlling.IOEQ;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.update.updateclient.UpdateHttpClientOptions;
import org.appwork.update.updateclient.UpdaterConstants;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.DefaultEventSender;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.InternetConnectionSettings;

public class ProxyController {

    private static final ProxyController              INSTANCE      = new ProxyController();

    private java.util.List<ProxyInfo>                 proxies       = new ArrayList<ProxyInfo>();
    private java.util.List<ProxyInfo>                 directs       = new ArrayList<ProxyInfo>();
    private ProxyInfo                                 defaultproxy  = null;
    private ProxyInfo                                 none          = null;

    private DefaultEventSender<ProxyEvent<ProxyInfo>> eventSender   = null;

    private InternetConnectionSettings                config;

    private UpdateHttpClientOptions                   updaterConfig = null;

    private GeneralSettings                           generalConfig = null;

    private final Object                              LOCK          = new Object();

    public static final ProxyController getInstance() {
        return INSTANCE;
    }

    public DefaultEventSender<ProxyEvent<ProxyInfo>> getEventSender() {
        return eventSender;
    }

    private ProxyController() {
        eventSender = new DefaultEventSender<ProxyEvent<ProxyInfo>>();
        /* init needed configs */
        config = JsonConfig.create(InternetConnectionSettings.class);
        generalConfig = JsonConfig.create(GeneralSettings.class);
        updaterConfig = JsonConfig.create(UpdateHttpClientOptions.class);
        /* init our NONE proxy */
        none = new ProxyInfo(HTTPProxy.NONE);
        loadProxySettings();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                saveProxySettings();
            }

            @Override
            public String toString() {
                return "ProxyController: save config";
            }
        });

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                exportUpdaterConfig();
            }

            @Override
            public String toString() {
                return "ProxyController: export important settings to updaterConfig";
            }
        });
        eventSender.addListener(new DefaultEventListener<ProxyEvent<ProxyInfo>>() {
            DelayedRunnable asyncSaving = new DelayedRunnable(IOEQ.TIMINGQUEUE, 5000l, 60000l) {

                                            @Override
                                            public void delayedrun() {
                                                saveProxySettings();
                                            }

                                        };

            public void onEvent(ProxyEvent<ProxyInfo> event) {
                asyncSaving.resetAndStart();
            }
        });
    }

    private void exportUpdaterConfig() {
        updaterConfig.setConnectTimeout(generalConfig.getHttpConnectTimeout());
        updaterConfig.setReadTimeout(generalConfig.getHttpReadTimeout());
        exportUpdaterProxy();
    }

    private void exportUpdaterProxy() {
        ProxyInfo ldefaultproxy = defaultproxy;
        if (ldefaultproxy != null && !ldefaultproxy.isNone()) {
            HTTPProxyStorable storable = HTTPProxy.getStorable(ldefaultproxy);
            updaterConfig.setProxy(storable);
        } else {
            updaterConfig.setProxy(null);
        }
    }

    public static List<HTTPProxy> autoConfig() {
        java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        try {
            if (CrossSystem.isWindows()) { return HTTPProxy.getWindowsRegistryProxies(); }
            /* we enable systemproxies to query them for a test getPage */
            System.setProperty("java.net.useSystemProxies", "true");
            List<Proxy> l = null;
            try {
                l = ProxySelector.getDefault().select(new URI("http://www.appwork.org"));
            } finally {
                System.setProperty("java.net.useSystemProxies", "false");
            }
            for (final Proxy p : l) {
                final SocketAddress ad = p.address();
                if (ad != null && ad instanceof InetSocketAddress) {
                    final InetSocketAddress isa = (InetSocketAddress) ad;
                    if (StringUtils.isEmpty(isa.getHostName())) {
                        continue;
                    }
                    switch (p.type()) {
                    case HTTP: {
                        HTTPProxy pd = new HTTPProxy(HTTPProxy.TYPE.HTTP);
                        pd.setHost(isa.getHostName());
                        pd.setPort(isa.getPort());
                        ret.add(pd);
                    }
                        break;
                    case SOCKS: {
                        HTTPProxy pd = new HTTPProxy(HTTPProxy.TYPE.SOCKS5);
                        pd.setHost(isa.getHostName());
                        pd.setPort(isa.getPort());
                        ret.add(pd);
                    }
                        break;
                    }
                }
            }
        } catch (final Throwable e1) {
            Log.exception(e1);
        }
        return ret;
    }

    private List<HTTPProxy> getAvailableDirects() {
        List<InetAddress> ips = HTTPProxyUtils.getLocalIPs();
        LogController.CL().info(ips.toString());
        java.util.List<HTTPProxy> directs = new ArrayList<HTTPProxy>(ips.size());
        if (ips.size() > 1) {
            // we can use non if we have only one WAN ips anyway
            for (InetAddress ip : ips) {
                directs.add(new HTTPProxy(ip));
            }
        }
        return directs;
    }

    private void saveProxySettings() {
        ProxyInfo ldefaultproxy = defaultproxy;
        {
            /* use own scope */
            ArrayList<ProxyData> ret = new ArrayList<ProxyData>(proxies.size());
            java.util.List<ProxyInfo> lproxies = proxies;
            for (ProxyInfo proxy : lproxies) {
                ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }
            config.setCustomProxyList(ret);
        }
        {
            /* use own scope */
            ArrayList<ProxyData> ret = new ArrayList<ProxyData>();
            java.util.List<ProxyInfo> ldirects = directs;
            for (ProxyInfo proxy : ldirects) {
                ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }
            config.setDirectGatewayList(ret);
        }
        config.setNoneDefault(none == ldefaultproxy);
        config.setNoneRotationEnabled(none.isProxyRotationEnabled());
    }

    private List<HTTPProxy> restoreFromOldConfig() {
        java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        SubConfiguration oldConfig = SubConfiguration.getConfig("DOWNLOAD", true);
        if (oldConfig.getBooleanProperty(UpdaterConstants.USE_PROXY, false)) {
            /* import old http proxy settings */
            final String host = oldConfig.getStringProperty(UpdaterConstants.PROXY_HOST, "");
            final int port = oldConfig.getIntegerProperty(UpdaterConstants.PROXY_PORT, 8080);
            final String user = oldConfig.getStringProperty(UpdaterConstants.PROXY_USER, "");
            final String pass = oldConfig.getStringProperty(UpdaterConstants.PROXY_PASS, "");
            if (!StringUtils.isEmpty(host)) {
                final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, host, port);
                if (!StringUtils.isEmpty(user)) {
                    pr.setUser(user);
                }
                if (!StringUtils.isEmpty(pass)) {
                    pr.setPass(pass);
                }
                ret.add(pr);
            }
        }
        if (oldConfig.getBooleanProperty(UpdaterConstants.USE_SOCKS, false)) {
            /* import old socks5 settings */
            final String user = oldConfig.getStringProperty(UpdaterConstants.PROXY_USER_SOCKS, "");
            final String pass = oldConfig.getStringProperty(UpdaterConstants.PROXY_PASS_SOCKS, "");
            final String host = oldConfig.getStringProperty(UpdaterConstants.SOCKS_HOST, "");
            final int port = oldConfig.getIntegerProperty(UpdaterConstants.SOCKS_PORT, 1080);
            if (!StringUtils.isEmpty(host)) {
                final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, host, port);
                if (!StringUtils.isEmpty(user)) {
                    pr.setUser(user);
                }
                if (!StringUtils.isEmpty(pass)) {
                    pr.setPass(pass);
                }
                ret.add(pr);
            }
        }
        return ret;
    }

    private void loadProxySettings() {
        ProxyInfo newDefaultProxy = null;
        boolean rotCheck = false;
        java.util.List<ProxyInfo> proxies = new ArrayList<ProxyInfo>();
        java.util.List<ProxyInfo> directs = new ArrayList<ProxyInfo>();
        java.util.List<HTTPProxy> dupeCheck = new ArrayList<HTTPProxy>();
        ProxyInfo proxy = null;
        {
            /* restore customs proxies */
            /* use own scope */
            java.util.List<ProxyData> ret = config.getCustomProxyList();
            if (ret != null) {
                /* config available */
                restore: for (ProxyData proxyData : ret) {
                    try {
                        HTTPProxy proxyTemplate = HTTPProxy.getHTTPProxy(proxyData.getProxy());
                        if (proxyTemplate != null) {
                            proxy = new ProxyInfo(proxyData, proxyTemplate);
                            for (HTTPProxy p : dupeCheck) {
                                if (p.sameProxy(proxy)) {
                                    /* proxy already got restored */
                                    continue restore;
                                }
                            }
                            dupeCheck.add(proxy);
                            proxies.add(proxy);
                            if (proxyData.isDefaultProxy()) {
                                newDefaultProxy = proxy;
                            }
                            if (proxy.isProxyRotationEnabled()) rotCheck = true;
                        }
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
            } else {
                /* convert from old system */
                List<HTTPProxy> reto = restoreFromOldConfig();
                restore: for (HTTPProxy proxyData : reto) {
                    try {
                        proxy = new ProxyInfo(proxyData);
                        for (HTTPProxy p : dupeCheck) {
                            if (p.sameProxy(proxy)) {
                                /* proxy already got restored */
                                continue restore;
                            }
                        }
                        dupeCheck.add(proxy);
                        proxies.add(proxy);
                        /* in old system we only had one possible proxy */
                        newDefaultProxy = proxy;
                        if (proxy.isProxyRotationEnabled()) rotCheck = true;
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
            }
            /* import proxies from system properties */
            List<HTTPProxy> sproxy = HTTPProxy.getFromSystemProperties();
            restore: for (HTTPProxy proxyData : sproxy) {
                try {
                    proxy = new ProxyInfo(proxyData);
                    for (HTTPProxy p : dupeCheck) {
                        if (p.sameProxy(proxy)) {
                            /* proxy already got restored */
                            continue restore;
                        }
                    }
                    dupeCheck.add(proxy);
                    proxies.add(proxy);
                    /* in old system we only had one possible proxy */
                    newDefaultProxy = proxy;
                    if (proxy.isProxyRotationEnabled()) rotCheck = true;
                } catch (final Throwable e) {
                    Log.exception(e);
                }
            }
        }
        {
            /* use own scope */
            List<HTTPProxy> availableDirects = getAvailableDirects();
            java.util.List<ProxyData> ret = config.getDirectGatewayList();
            if (ret != null) {
                // restore directs
                restore: for (ProxyData proxyData : ret) {
                    /* check if the local IP is still avilable */
                    try {
                        HTTPProxy proxyTemplate = HTTPProxy.getHTTPProxy(proxyData.getProxy());
                        if (proxyTemplate != null) {
                            proxy = new ProxyInfo(proxyData, proxyTemplate);
                            for (HTTPProxy p : dupeCheck) {
                                if (p.sameProxy(proxy)) {
                                    /* proxy already got restored */
                                    continue restore;
                                }
                            }
                            boolean localIPAvailable = false;
                            for (HTTPProxy p : availableDirects) {
                                if (p.sameProxy(proxy)) {
                                    localIPAvailable = true;
                                    break;
                                }
                            }
                            if (localIPAvailable == false) {
                                /* local ip no longer available */
                                continue restore;
                            }
                            dupeCheck.add(proxy);
                            directs.add(proxy);
                            if (proxyData.isDefaultProxy()) {
                                newDefaultProxy = proxy;
                            }
                            if (proxy.isProxyRotationEnabled()) rotCheck = true;
                        }
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
            }
        }

        if (config.isNoneDefault()) {
            /* check if the NONE Proxy is our default proxy */
            if (newDefaultProxy != null) {
                Log.L.severe("NONE default but already got different default?!");
            }
            newDefaultProxy = none;
        }
        /* is NONE Proxy included in rotation */
        none.setProxyRotationEnabled(config.isNoneRotationEnabled());
        if (none.isProxyRotationEnabled()) rotCheck = true;
        if (!rotCheck) {
            // we need at least one rotation
            none.setProxyRotationEnabled(true);
            config.setNoneRotationEnabled(true);
        }
        if (newDefaultProxy == null || newDefaultProxy == none) config.setNoneDefault(true);
        setDefaultProxy(newDefaultProxy);
        /* set new proxies live */
        this.directs = directs;
        this.proxies = proxies;
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(ProxyController.this, ProxyEvent.Types.REFRESH, null));
    }

    /**
     * returns a copy of current proxy list
     * 
     * @return
     */
    public java.util.List<ProxyInfo> getList() {
        java.util.List<ProxyInfo> ret = new ArrayList<ProxyInfo>(directs.size() + proxies.size() + 1);
        ret.add(none);
        ret.addAll(directs);
        ret.addAll(proxies);
        return ret;
    }

    /**
     * returns the default proxy for all normal browser activity as well as for premium usage
     * 
     * @return
     */
    public ProxyInfo getDefaultProxy() {
        return defaultproxy;
    }

    /**
     * sets current default proxy
     * 
     * @param def
     */
    public void setDefaultProxy(ProxyInfo def) {
        if (def != null && defaultproxy == def) return;
        if (def == null) {
            defaultproxy = none;
        } else {
            defaultproxy = def;
        }
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.REFRESH, null));
        exportUpdaterProxy();
    }

    /**
     * add proxy to proxylist in case its not in it yet
     * 
     * @param proxy
     */
    public void addProxy(HTTPProxy proxy) {
        if (proxy == null) return;
        ProxyInfo ret = null;
        synchronized (LOCK) {
            java.util.List<ProxyInfo> nproxies = new ArrayList<ProxyInfo>(proxies);
            for (ProxyInfo info : nproxies) {
                /* duplicate check */
                if (info.sameProxy(proxy)) return;
            }
            nproxies.add(ret = new ProxyInfo(proxy));
            proxies = nproxies;
        }
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.ADDED, ret));
    }

    public void addProxy(List<HTTPProxy> proxy) {
        if (proxy == null || proxy.size() == 0) return;
        int changes = 0;
        synchronized (LOCK) {
            java.util.List<ProxyInfo> nproxies = new ArrayList<ProxyInfo>(proxies);
            changes = nproxies.size();
            main: for (HTTPProxy newP : proxy) {
                if (newP == null) continue;
                for (ProxyInfo info : nproxies) {
                    /* duplicate check */
                    if (info.sameProxy(newP)) continue main;
                }
                nproxies.add(new ProxyInfo(newP));

            }
            proxies = nproxies;
            if (changes != nproxies.size()) changes = -1;
        }
        if (changes == -1) {
            eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.ADDED, null));
            eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.REFRESH, null));
        }
    }

    /**
     * enable/disable given proxy, enables none-proxy in case no proxy would be enabled anymore
     * 
     * @param proxy
     * @param enabled
     */
    public void setproxyRotationEnabled(ProxyInfo proxy, boolean enabled) {
        if (proxy == null) return;
        if (proxy.isProxyRotationEnabled() == enabled) return;
        proxy.setProxyRotationEnabled(enabled);
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.REFRESH, null));
    }

    /** removes given proxy from proxylist */
    public void remove(ProxyInfo proxy) {
        if (proxy == null) return;
        boolean removed = false;
        synchronized (LOCK) {
            java.util.List<ProxyInfo> nproxies = new ArrayList<ProxyInfo>(proxies);
            if (nproxies.remove(proxy)) {
                removed = true;
                if (proxy == defaultproxy) {
                    setDefaultProxy(none);
                }
            }
            proxies = nproxies;
        }
        if (removed) eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.REMOVED, proxy));
    }

    public ProxyInfo getProxyForDownload(PluginForHost plugin, DownloadLink link, Account acc, boolean byPassMaxSimultanDownload) {
        final String host = link.getHost();
        final int maxactive = plugin.getMaxSimultanDownload(link, acc);
        if (acc != null) {
            /* an account must be used or waittime must be over */
            /*
             * only the default proxy may use accounts, to prevent accountblocks because of simultan ip's using it
             */
            ProxyInfo ldefaultProxy = defaultproxy;
            int active = ldefaultProxy.activeDownloadsbyHosts(host);
            if (byPassMaxSimultanDownload || active < maxactive) return ldefaultProxy;
            return null;
        }
        if (none.isProxyRotationEnabled()) {
            /* only use enabled proxies */
            if (none.getHostBlockedTimeout(host) == null && none.getHostIPBlockTimeout(host) == null) {
                /* active downloads must be less than allowed download */
                int active = none.activeDownloadsbyHosts(host);
                if (byPassMaxSimultanDownload || active < maxactive) {
                    if (none.isHostAllowed(host)) {
                        if (link.getChunksProgress() == null || none.isResumeAllowed()) return none;
                    }
                }
            }
        }
        java.util.List<ProxyInfo> ldirects = directs;
        for (ProxyInfo info : ldirects) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                if (info.getHostBlockedTimeout(host) == null && info.getHostIPBlockTimeout(host) == null) {
                    /* active downloads must be less than allowed download */
                    int active = info.activeDownloadsbyHosts(host);
                    if (byPassMaxSimultanDownload || active < maxactive) {
                        /* connection to hoster must be allowed */
                        if (info.isHostAllowed(host)) {
                            /* proxy must allow resume, if selected */
                            if (link.getChunksProgress() == null || info.isResumeAllowed()) return info;
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
        java.util.List<ProxyInfo> lproxies = proxies;
        for (ProxyInfo info : lproxies) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                if (info.getHostBlockedTimeout(host) == null && info.getHostIPBlockTimeout(host) == null) {
                    /* active downloads must be less than allowed download */
                    int active = info.activeDownloadsbyHosts(host);
                    if (byPassMaxSimultanDownload || active < maxactive) {
                        /* connection to hoster must be allowed */
                        if (info.isHostAllowed(host)) {
                            /* proxy must allow resume, if selected */
                            if (link.getChunksProgress() == null || info.isResumeAllowed()) return info;
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
        return null;
    }

    public ProxyBlock getHostIPBlockTimeout(final String host) {
        ProxyBlock ret = null;
        if (none.isProxyRotationEnabled()) {
            ret = none.getHostIPBlockTimeout(host);
        }
        if (ret == null) return null;
        java.util.List<ProxyInfo> lproxies = proxies;
        for (ProxyInfo info : lproxies) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                ProxyBlock ret2 = info.getHostIPBlockTimeout(host);
                if (ret2 == null) {
                    return null;
                } else if (ret == null || ret2.getBlockedUntil() < ret.getBlockedUntil()) {
                    ret = ret2;
                }

            }
        }
        java.util.List<ProxyInfo> ldirects = directs;
        for (ProxyInfo info : ldirects) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                ProxyBlock ret2 = info.getHostIPBlockTimeout(host);
                if (ret2 == null) {
                    return null;
                } else if (ret == null || ret2.getBlockedUntil() < ret.getBlockedUntil()) {
                    ret = ret2;
                }
            }
        }
        return ret;
    }

    /* optimize for speed */
    public ProxyBlock getHostBlockedTimeout(final String host) {
        ProxyBlock ret = null;
        if (none.isProxyRotationEnabled()) {
            ret = none.getHostBlockedTimeout(host);
        }
        if (ret == null) return null;
        java.util.List<ProxyInfo> lproxies = proxies;
        for (ProxyInfo info : lproxies) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                ProxyBlock ret2 = info.getHostBlockedTimeout(host);
                if (ret2 == null) {
                    return null;
                } else if (ret == null || ret2.getBlockedUntil() < ret.getBlockedUntil()) {
                    ret = ret2;
                }

            }
        }
        java.util.List<ProxyInfo> ldirects = directs;
        for (ProxyInfo info : ldirects) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                ProxyBlock ret2 = info.getHostBlockedTimeout(host);
                if (ret2 == null) {
                    return null;
                } else if (ret == null || ret2.getBlockedUntil() < ret.getBlockedUntil()) {
                    ret = ret2;
                }
            }
        }
        return ret;
    }

    public boolean hasIPBlock(final String host) {
        if (none.isProxyRotationEnabled()) {
            if (none.getHostIPBlockTimeout(host) != null) return true;
        }
        java.util.List<ProxyInfo> lproxies = proxies;
        for (ProxyInfo info : lproxies) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                if (info.getHostIPBlockTimeout(host) != null) return true;
            }
        }
        java.util.List<ProxyInfo> ldirects = directs;
        for (ProxyInfo info : ldirects) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                if (info.getHostIPBlockTimeout(host) != null) return true;
            }
        }
        return false;
    }

    public boolean hasHostBlocked(final String host) {
        if (none.isProxyRotationEnabled()) {
            if (none.getHostBlockedTimeout(host) != null) return true;
        }
        java.util.List<ProxyInfo> lproxies = proxies;
        for (ProxyInfo info : lproxies) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                if (info.getHostBlockedTimeout(host) != null) return true;
            }
        }
        java.util.List<ProxyInfo> ldirects = directs;
        for (ProxyInfo info : ldirects) {
            if (info.isProxyRotationEnabled()) {
                /* only use enabled proxies */
                if (info.getHostBlockedTimeout(host) != null) return true;
            }
        }
        return false;
    }

    public void removeHostBlockedTimeout(final String host, boolean onlyLocal) {
        none.removeHostBlockedWaittime(host);
        java.util.List<ProxyInfo> ldirects = directs;
        for (ProxyInfo info : ldirects) {
            info.removeHostBlockedWaittime(host);
        }
        if (!onlyLocal) {
            java.util.List<ProxyInfo> lproxies = proxies;
            for (ProxyInfo info : lproxies) {
                // if (onlyLocal && info.getProxy().isRemote()) continue;
                info.removeHostBlockedWaittime(host);
            }
        }
    }

    public void removeIPBlockTimeout(final String host, boolean onlyLocal) {
        none.removeHostIPBlockTimeout(host);
        java.util.List<ProxyInfo> ldirects = directs;
        for (ProxyInfo info : ldirects) {
            info.removeHostIPBlockTimeout(host);
        }
        if (!onlyLocal) {
            java.util.List<ProxyInfo> lproxies = proxies;
            for (ProxyInfo info : lproxies) {
                info.removeHostIPBlockTimeout(host);
            }
        }
    }

    public boolean hasRotation() {
        if (none.isProxyRotationEnabled()) return true;
        java.util.List<ProxyInfo> ldirects = directs;
        for (ProxyInfo pi : ldirects) {
            if (pi.isProxyRotationEnabled()) return true;
        }
        java.util.List<ProxyInfo> lproxies = proxies;
        for (ProxyInfo pi : lproxies) {
            if (pi.isProxyRotationEnabled()) return true;
        }
        return false;
    }

    public ProxyInfo getNone() {
        return none;
    }

}
