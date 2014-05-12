package jd.controlling.proxy;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jd.config.SubConfiguration;
import jd.http.Request;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.DefaultEventSender;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProxyDialog;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.InternetConnectionSettings;
import org.jdownloader.updatev2.ProxyData;

public class ProxyController {

    private static final ProxyController                              INSTANCE      = new ProxyController();

    private java.util.List<SingleBasicProxySelectorImpl>              proxies       = new ArrayList<SingleBasicProxySelectorImpl>();
    private List<SingleDirectGatewaySelector>                         directs       = new ArrayList<SingleDirectGatewaySelector>();
    private java.util.List<PacProxySelectorImpl>                      pacs          = new ArrayList<PacProxySelectorImpl>();
    private AbstractProxySelectorImpl                                 defaultproxy  = null;
    private final NoProxySelector                                     none;

    private DefaultEventSender<ProxyEvent<AbstractProxySelectorImpl>> eventSender   = null;

    private InternetConnectionSettings                                config;

    private GeneralSettings                                           generalConfig = null;

    private final Object                                              LOCK          = new Object();

    private LogSource                                                 logger;

    public static final ProxyController getInstance() {
        return INSTANCE;
    }

    public DefaultEventSender<ProxyEvent<AbstractProxySelectorImpl>> getEventSender() {
        return eventSender;
    }

    public static final String USE_PROXY        = "USE_PROXY";
    public static final String PROXY_HOST       = "PROXY_HOST";
    public static final String PROXY_PASS       = "PROXY_PASS";
    public static final String PROXY_PASS_SOCKS = "PROXY_PASS_SOCKS";
    public static final String PROXY_PORT       = "PROXY_PORT";
    public static final String PROXY_USER       = "PROXY_USER";
    public static final String PROXY_USER_SOCKS = "PROXY_USER_SOCKS";
    public static final String SOCKS_HOST       = "SOCKS_HOST";
    public static final String SOCKS_PORT       = "SOCKS_PORT";
    public static final String USE_SOCKS        = "USE_SOCKS";

    private ProxyController() {
        eventSender = new DefaultEventSender<ProxyEvent<AbstractProxySelectorImpl>>();
        /* init needed configs */
        config = JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class);
        generalConfig = JsonConfig.create(GeneralSettings.class);
        logger = LogController.getInstance().getLogger(ProxyController.class.getName());
        /* init our NONE proxy */
        none = new NoProxySelector();
        loadProxySettings();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                saveProxySettings();
            }

            @Override
            public String toString() {
                return "ProxyController: save config";
            }
        });

        eventSender.addListener(new DefaultEventListener<ProxyEvent<AbstractProxySelectorImpl>>() {
            DelayedRunnable asyncSaving = new DelayedRunnable(5000l, 60000l) {
                                            @Override
                                            public String getID() {
                                                return "ProxyController";
                                            }

                                            @Override
                                            public void delayedrun() {
                                                saveProxySettings();
                                            }

                                        };

            public void onEvent(ProxyEvent<AbstractProxySelectorImpl> event) {
                asyncSaving.resetAndStart();
            }
        });
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
        AbstractProxySelectorImpl ldefaultproxy = defaultproxy;
        {
            /* use own scope */
            ArrayList<ProxyData> ret = new ArrayList<ProxyData>(proxies.size());
            List<SingleBasicProxySelectorImpl> lproxies = proxies;
            for (AbstractProxySelectorImpl proxy : lproxies) {
                ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }

            List<PacProxySelectorImpl> lPacs = pacs;
            for (PacProxySelectorImpl proxy : lPacs) {
                ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }
            config.setCustomProxyList(ret);
        }
        {
            /* use own scope */
            ArrayList<ProxyData> ret = new ArrayList<ProxyData>();
            List<SingleDirectGatewaySelector> ldirects = directs;
            for (AbstractProxySelectorImpl proxy : ldirects) {
                ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }
            config.setDirectGatewayList(ret);
        }
        config.setNoneDefault(none == ldefaultproxy);
        config.setNoneRotationEnabled(none.isProxyRotationEnabled());
        config.setNoneFilter(none.getFilter());
        config._getStorageHandler().write();
    }

    private List<HTTPProxy> restoreFromOldConfig() {
        java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        SubConfiguration oldConfig = SubConfiguration.getConfig("DOWNLOAD", true);
        if (oldConfig.getBooleanProperty(USE_PROXY, false)) {
            /* import old http proxy settings */
            final String host = oldConfig.getStringProperty(PROXY_HOST, "");
            final int port = oldConfig.getIntegerProperty(PROXY_PORT, 8080);
            final String user = oldConfig.getStringProperty(PROXY_USER, "");
            final String pass = oldConfig.getStringProperty(PROXY_PASS, "");
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
        if (oldConfig.getBooleanProperty(USE_SOCKS, false)) {
            /* import old socks5 settings */
            final String user = oldConfig.getStringProperty(PROXY_USER_SOCKS, "");
            final String pass = oldConfig.getStringProperty(PROXY_PASS_SOCKS, "");
            final String host = oldConfig.getStringProperty(SOCKS_HOST, "");
            final int port = oldConfig.getIntegerProperty(SOCKS_PORT, 1080);
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
        AbstractProxySelectorImpl newDefaultProxy = null;
        boolean rotCheck = false;
        java.util.List<SingleBasicProxySelectorImpl> proxies = new ArrayList<SingleBasicProxySelectorImpl>();
        java.util.List<SingleDirectGatewaySelector> directs = new ArrayList<SingleDirectGatewaySelector>();
        java.util.List<PacProxySelectorImpl> pacs = new ArrayList<PacProxySelectorImpl>();
        HashSet<AbstractProxySelectorImpl> dupeCheck = new HashSet<AbstractProxySelectorImpl>();
        AbstractProxySelectorImpl proxy = null;
        {
            /* restore customs proxies */
            /* use own scope */
            java.util.List<ProxyData> ret = config.getCustomProxyList();
            if (ret != null) {
                /* config available */
                restore: for (ProxyData proxyData : ret) {
                    try {
                        // HTTPProxy proxyTemplate = HTTPProxy.getHTTPProxy(proxyData.getProxy());
                        // if (proxyTemplate != null) {
                        if (proxyData.isPac()) {
                            proxy = new PacProxySelectorImpl(proxyData);
                        } else {
                            ;
                            switch (proxyData.getProxy().getType()) {
                            case DIRECT:
                                proxy = new SingleDirectGatewaySelector(proxyData);
                                break;
                            case HTTP:
                            case SOCKS4:
                            case SOCKS5:
                                proxy = new SingleBasicProxySelectorImpl(proxyData);
                                break;
                            case NONE:

                                System.out.println(1);

                            default:
                                continue;
                            }
                        }
                        if (proxy == null) continue;
                        if (!dupeCheck.add(proxy)) continue restore;
                        dupeCheck.add(proxy);
                        if (proxy instanceof PacProxySelectorImpl) {
                            pacs.add((PacProxySelectorImpl) proxy);
                        } else if (proxy instanceof SingleDirectGatewaySelector) {
                            directs.add((SingleDirectGatewaySelector) proxy);
                        } else if (proxy instanceof SingleBasicProxySelectorImpl) {
                            proxies.add((SingleBasicProxySelectorImpl) proxy);
                        } else {
                            throw new WTFException("Unknown Type: " + proxy.getClass());
                        }

                        if (proxyData.isDefaultProxy()) {
                            newDefaultProxy = proxy;
                        }
                        if (proxy.isProxyRotationEnabled()) rotCheck = true;
                        // }
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
            } else {
                /* convert from old system */
                List<HTTPProxy> reto = restoreFromOldConfig();
                restore: for (HTTPProxy proxyData : reto) {
                    try {
                        switch (proxyData.getType()) {
                        case DIRECT:
                            proxy = new SingleDirectGatewaySelector(proxyData);
                            break;
                        case HTTP:
                        case SOCKS4:
                        case SOCKS5:
                            proxy = new SingleBasicProxySelectorImpl(proxyData);
                            break;

                        default:
                            continue;
                        }
                        if (proxy == null) continue;
                        if (!dupeCheck.add(proxy)) continue restore;

                        if (proxy instanceof SingleBasicProxySelectorImpl) {
                            proxies.add((SingleBasicProxySelectorImpl) proxy);
                        } else {
                            throw new WTFException("Unknown Type: " + proxy.getClass());
                        }

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
                    proxy = new SingleBasicProxySelectorImpl(proxyData);

                    if (!dupeCheck.add(proxy)) continue restore;

                    proxies.add((SingleBasicProxySelectorImpl) proxy);

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

                        proxy = new SingleDirectGatewaySelector(proxyData);
                        if (proxy == null) continue;

                        if (!dupeCheck.add(proxy)) continue restore;
                        dupeCheck.add(proxy);

                        boolean localIPAvailable = false;
                        for (HTTPProxy p : availableDirects) {
                            if (p.equals(((SingleDirectGatewaySelector) proxy).getProxy())) {
                                localIPAvailable = true;
                                break;
                            }
                        }
                        if (localIPAvailable == false) {
                            /* local ip no longer available */
                            continue restore;
                        }

                        directs.add((SingleDirectGatewaySelector) proxy);

                        if (proxyData.isDefaultProxy()) {
                            newDefaultProxy = proxy;
                        }
                        if (proxy.isProxyRotationEnabled()) rotCheck = true;

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
        none.setFilter(config.getNoneFilter());

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
        this.pacs = pacs;
        eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(ProxyController.this, ProxyEvent.Types.REFRESH, null));
    }

    /**
     * returns a copy of current proxy list
     * 
     * @return
     */
    public java.util.List<AbstractProxySelectorImpl> getList() {
        java.util.List<AbstractProxySelectorImpl> ret = new ArrayList<AbstractProxySelectorImpl>(directs.size() + proxies.size() + pacs.size() + 1);
        ret.add(none);

        ret.addAll(directs);
        ret.addAll(proxies);
        ret.addAll(pacs);
        return ret;
    }

    /**
     * returns the default proxy for all normal browser activity as well as for premium usage
     * 
     * @return
     */
    public AbstractProxySelectorImpl getDefaultProxy() {
        return defaultproxy;
    }

    /**
     * sets current default proxy
     * 
     * @param def
     */
    public void setDefaultProxy(AbstractProxySelectorImpl def) {
        if (def != null && defaultproxy == def) return;
        if (def == null) {
            defaultproxy = none;
        } else {
            defaultproxy = def;
        }
        eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.REFRESH, null));
        // exportUpdaterProxy();
    }

    /**
     * add proxy to proxylist in case its not in it yet
     * 
     * @param proxy
     */
    public void addProxy(AbstractProxySelectorImpl proxy) {
        if (proxy == null) return;

        synchronized (LOCK) {

            switch (proxy.getType()) {
            case HTTP:
            case SOCKS4:
            case SOCKS5:
                if (!new HashSet<AbstractProxySelectorImpl>(proxies).add(proxy)) return;
                ArrayList<SingleBasicProxySelectorImpl> nproxies = new ArrayList<SingleBasicProxySelectorImpl>(proxies);
                nproxies.add((SingleBasicProxySelectorImpl) proxy);
                proxies = nproxies;
                break;
            case DIRECT:
                if (!new HashSet<AbstractProxySelectorImpl>(directs).add(proxy)) return;
                ArrayList<SingleDirectGatewaySelector> ndirects = new ArrayList<SingleDirectGatewaySelector>(directs);
                ndirects.add((SingleDirectGatewaySelector) proxy);
                directs = ndirects;
                break;
            case PAC:
                if (!new HashSet<AbstractProxySelectorImpl>(pacs).add(proxy)) return;
                ArrayList<PacProxySelectorImpl> npacs = new ArrayList<PacProxySelectorImpl>(pacs);
                npacs.add((PacProxySelectorImpl) proxy);
                pacs = npacs;
                break;

            default:
                logger.info("Invalid Type " + proxy.getType());
                return;
            }

        }
        eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.ADDED, proxy));
    }

    public void addProxy(List<HTTPProxy> nProxies) {
        if (nProxies == null) return;
        ExtProxy ret = null;
        boolean changes = false;
        synchronized (LOCK) {
            HashSet<AbstractProxySelectorImpl> dupe = new HashSet<AbstractProxySelectorImpl>(proxies);
            ArrayList<SingleBasicProxySelectorImpl> nproxies = new ArrayList<SingleBasicProxySelectorImpl>(proxies);

            for (HTTPProxy proxy : nProxies) {
                AbstractProxySelectorImpl proxyFac = new SingleBasicProxySelectorImpl(proxy);
                if (proxyFac == null || !(proxyFac instanceof SingleBasicProxySelectorImpl) || !dupe.add(proxyFac)) continue;

                changes = true;
                nproxies.add((SingleBasicProxySelectorImpl) proxyFac);
            }

            proxies = nproxies;
        }

        if (changes) {
            eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.ADDED, null));
            eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.REFRESH, null));
        }
    }

    public void setProxyRotationEnabled(AbstractProxySelectorImpl p, boolean enabled) {
        if (p == null) return;

        if (p.isProxyRotationEnabled() == enabled) return;
        p.setProxyRotationEnabled(enabled);

        eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.REFRESH, null));
    }

    /** removes given proxy from proxylist */
    public void remove(AbstractProxySelectorImpl proxy) {
        if (proxy == null) return;
        boolean removed = false;
        synchronized (LOCK) {
            if (proxy instanceof SingleBasicProxySelectorImpl) {

                java.util.List<SingleBasicProxySelectorImpl> nproxies = new ArrayList<SingleBasicProxySelectorImpl>(proxies);
                if (nproxies.remove(proxy)) {
                    removed = true;
                    if (proxy == defaultproxy) {
                        setDefaultProxy(none);
                    }
                }
                proxies = nproxies;
            } else if (proxy instanceof PacProxySelectorImpl) {
                java.util.List<PacProxySelectorImpl> npacs = new ArrayList<PacProxySelectorImpl>(pacs);
                if (npacs.remove(proxy)) {
                    removed = true;
                    if (proxy == defaultproxy) {
                        setDefaultProxy(none);
                    }
                }
                this.pacs = npacs;
            } else {
                throw new WTFException("bad Type: " + proxy.getClass());
            }

        }
        if (removed) eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.REMOVED, proxy));
    }

    public List<AbstractProxySelectorImpl> getPossibleProxies(String host, boolean accountInUse, int maxActive) {
        List<AbstractProxySelectorImpl> ret = new ArrayList<AbstractProxySelectorImpl>();
        host = host.toLowerCase(Locale.ENGLISH);
        if (accountInUse) {
            AbstractProxySelectorImpl ldefaultProxy = defaultproxy;
            if (ldefaultProxy.isAllowedByFilter(host)) {
                int active = ldefaultProxy.activeDownloadsbyHosts(host);
                if (active < maxActive) ret.add(ldefaultProxy);
                return ret;
            }
            // no proxy found. search first one in rotation
            if (none.isProxyRotationEnabled()) {
                if (none.isAllowedByFilter(host)) {
                    int active = none.activeDownloadsbyHosts(host);
                    if (active < maxActive) ret.add(none);
                    return ret;
                }
            }
            List<SingleDirectGatewaySelector> ldirects = directs;

            for (SingleDirectGatewaySelector info : ldirects) {
                if (!info.isAllowedByFilter(host)) continue;
                collect(host, maxActive, ret, info);
                return ret;
            }
            List<PacProxySelectorImpl> lpacs = pacs;
            for (PacProxySelectorImpl info : lpacs) {
                if (!info.isAllowedByFilter(host)) continue;
                collect(host, maxActive, ret, info);
                return ret;
            }

            List<SingleBasicProxySelectorImpl> lProxies = proxies;
            for (SingleBasicProxySelectorImpl info : lProxies) {
                if (!info.isAllowedByFilter(host)) continue;
                collect(host, maxActive, ret, info);
                return ret;
            }
        } else {
            if (none.isProxyRotationEnabled()) {
                if (none.isAllowedByFilter(host)) {
                    int active = none.activeDownloadsbyHosts(host);
                    if (active < maxActive) ret.add(none);
                }
            }
            List<SingleDirectGatewaySelector> ldirects = directs;

            for (SingleDirectGatewaySelector info : ldirects) {
                if (!info.isAllowedByFilter(host)) continue;
                collect(host, maxActive, ret, info);
            }
            List<PacProxySelectorImpl> lpacs = pacs;
            for (PacProxySelectorImpl info : lpacs) {
                if (!info.isAllowedByFilter(host)) continue;
                collect(host, maxActive, ret, info);
            }

            List<SingleBasicProxySelectorImpl> lProxies = proxies;
            for (SingleBasicProxySelectorImpl info : lProxies) {
                if (!info.isAllowedByFilter(host)) continue;
                collect(host, maxActive, ret, info);
            }
        }
        return ret;
    }

    public void collect(String host, int maxActive, List<AbstractProxySelectorImpl> ret, AbstractProxySelectorImpl selector) {
        if (selector.isProxyRotationEnabled()) {
            if (selector.isBanned(host)) return;
            int active = selector.activeDownloadsbyHosts(host);
            if (active < maxActive) ret.add(selector);

        }
    }

    public boolean hasRotation() {
        if (none.isProxyRotationEnabled()) return true;
        List<SingleDirectGatewaySelector> ldirects = directs;
        for (SingleDirectGatewaySelector pi : ldirects) {
            if (pi.isProxyRotationEnabled()) return true;
        }
        List<SingleBasicProxySelectorImpl> lproxies = proxies;
        for (SingleBasicProxySelectorImpl pi : lproxies) {
            if (pi.isProxyRotationEnabled()) return true;
        }

        List<PacProxySelectorImpl> lpacs = pacs;
        for (PacProxySelectorImpl pi : lpacs) {
            if (pi.isProxyRotationEnabled()) return true;
        }
        return false;
    }

    public NoProxySelector getNone() {
        return none;
    }

    public void exportTo(File saveTo) throws UnsupportedEncodingException, IOException {
        ProxyExportImport save = new ProxyExportImport();

        AbstractProxySelectorImpl ldefaultproxy = defaultproxy;
        {
            /* use own scope */
            ArrayList<ProxyData> ret = new ArrayList<ProxyData>(proxies.size());
            List<SingleBasicProxySelectorImpl> lproxies = proxies;
            for (SingleBasicProxySelectorImpl proxy : lproxies) {
                ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }

            List<PacProxySelectorImpl> lpacs = pacs;
            for (PacProxySelectorImpl proxy : lpacs) {
                ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }
            save.setCustomProxyList(ret);
        }

        save.setNoneDefault(none == ldefaultproxy);
        save.setNoneRotationEnabled(none.isProxyRotationEnabled());

        IO.secureWrite(saveTo, JSonStorage.serializeToJson(save).getBytes("UTF-8"));
    }

    public String getLatestProfilePath() {
        return config.getLatestProfile();
    }

    public void importFrom(File selected) throws IOException {
        ProxyExportImport restore = JSonStorage.restoreFromString(IO.readFileToString(selected), new TypeRef<ProxyExportImport>() {
        });
        config.setLatestProfile(selected.getAbsolutePath());
        config.setCustomProxyList(restore.getCustomProxyList());
        config.setNoneDefault(restore.isNoneDefault());
        config.setNoneRotationEnabled(restore.isNoneRotationEnabled());
        loadProxySettings();
    }

    public void setBan(AbstractProxySelectorImpl proxy, String bannedDomain, long proxyHostBanTimeout, String explain) {
        proxy.addBan(new ProxyBan(bannedDomain, proxyHostBanTimeout <= 0 ? -1 : proxyHostBanTimeout + System.currentTimeMillis(), explain));
    }

    public boolean updateProxy(AbstractProxySelectorImpl selector, Request request, int retryCounter) {
        List<String> proxyAuths = request.getHttpConnection().getHeaderFields("proxy-authenticate");
        HTTPProxy usedProxy = request.getProxy();
        URL url;
        try {
            url = new URL(request.getUrl());

            // pd = new ProxyDialog(usedProxy, T.T.TranslationProxyDialogAuthRequired(url.getHost()));
            // pd.setTitle(_AWU.T.proxydialog_title());
            if (usedProxy == null) return false;

            if (proxyAuths != null) {
                for (final String authMethod : proxyAuths) {
                    if ("NTLM".equalsIgnoreCase(authMethod)) {
                        if (usedProxy.isPreferNativeImplementation() == false) {
                            /* enable nativeImplementation for NTLM */
                            usedProxy.setPreferNativeImplementation(true);
                            return true;
                        }
                        break;
                    }
                }
            }

            if (selector instanceof PacProxySelectorImpl) {
                HTTPProxy ret = askForProxyAuth(UIOManager.LOGIC_COUNTDOWN, false, usedProxy, url, _JDT._.ProxyController_updateProxy_proxy_auth_required_msg(url.getHost()), _JDT._.ProxyController_updateProxy_proxy_auth_required_title());
                if (ret != null) {
                    ((PacProxySelectorImpl) selector).setTempAuth(usedProxy, ret.getUser(), ret.getPass());
                    return true;
                }
            } else if (selector instanceof SingleBasicProxySelectorImpl) {
                HTTPProxy ret = askForProxyAuth(UIOManager.LOGIC_COUNTDOWN, false, usedProxy, url, _JDT._.ProxyController_updateProxy_proxy_auth_required_msg(url.getHost()), _JDT._.ProxyController_updateProxy_proxy_auth_required_title());
                if (ret != null) {
                    ((SingleBasicProxySelectorImpl) selector).setTempAuth(ret.getUser(), ret.getPass());
                    return true;
                }
            }

            return false;
        } catch (Throwable e) {
            logger.log(e);
        }
        return false;
    }

    public HTTPProxy updateProxyAuthForUpdater(HTTPProxy usedProxy, List<String> proxyAuths, URL url) {
        // pd = new ProxyDialog(usedProxy, T.T.TranslationProxyDialogAuthRequired(url.getHost()));
        // pd.setTitle(_AWU.T.proxydialog_title());
        if (usedProxy == null) return null;

        if (proxyAuths != null) {
            for (final String authMethod : proxyAuths) {
                if ("NTLM".equalsIgnoreCase(authMethod)) {
                    if (usedProxy.isPreferNativeImplementation() == false) {
                        /* enable nativeImplementation for NTLM */
                        usedProxy.setPreferNativeImplementation(true);
                        return usedProxy;
                    }
                    break;
                }
            }
        }

        try {
            return askForProxyAuth(0, true, usedProxy, url, _JDT._.ProxyController_updateProxy_proxy_auth_required_msg_updater(url.getHost()), _AWU.T.proxydialog_title());

        } catch (Throwable e) {
            logger.log(e);
        }
        return null;

    }

    private ConcurrentHashMap<String, HTTPProxy> replaceMap     = new ConcurrentHashMap<String, HTTPProxy>();
    private AtomicInteger                        requestCounter = new AtomicInteger(0);

    private HTTPProxy                            manualProxy;

    public HTTPProxy askForProxyAuth(final int flags, boolean typeEditable, HTTPProxy usedProxy, URL url, String msg, String title) throws DialogClosedException, DialogCanceledException {

        System.out.println("Wait for");
        String proxyID = toProxyID(usedProxy);
        // HTTPProxy replaced = replaceMap.get(proxyID);
        // if (replaced != null) {
        // //
        // return replaced;
        // }
        // requestCounter.incrementAndGet();
        if (manualProxy != null && manualProxy.equals(usedProxy)) {
            manualProxy = null;
        }
        if (manualProxy != null) {
            //
            return manualProxy;
        }
        synchronized (this) {
            try {
                if (manualProxy != null && manualProxy.equals(usedProxy)) {
                    manualProxy = null;
                }
                if (manualProxy != null) {
                    //
                    return manualProxy;
                }
                ProxyDialog pd = new ProxyDialog(usedProxy, msg) {
                    {
                        flagMask |= flags;
                    }
                };
                pd.setTimeout(5 * 60 * 1000);
                pd.setAuthRequired(true);
                pd.setTypeEditable(typeEditable);
                pd.setHostEditable(typeEditable);
                pd.setPortEditable(typeEditable);
                pd.setTitle(title);

                HTTPProxy ret = Dialog.getInstance().showDialog(pd);
                if (ret == null) {
                    //
                    manualProxy = null;
                    return null;
                }
                if (toProxyID(ret).equals(proxyID)) {
                    //
                    return ret;
                }
                manualProxy = ret;
                // replaceMap.put(proxyID, ret);

                return ret;
            } finally {
                // if (requestCounter.decrementAndGet() == 0) {
                // replaceMap.clear();
                // }
            }
        }
    }

    public String toProxyID(HTTPProxy usedProxy) {
        return usedProxy.getUser() + ":" + usedProxy.getPass() + "@" + usedProxy.getHost() + ":" + usedProxy.getPort() + "(Native: " + usedProxy.isPreferNativeImplementation() + ")";
    }

}
