package jd.controlling.proxy;

import java.net.InetAddress;
import java.util.ArrayList;

import jd.controlling.JDLogger;
import jd.plugins.Account;
import jd.plugins.PluginForHost;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.DefaultEventSender;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.jdownloader.settings.InternetConnectionSettings;

public class ProxyController {

    private static final ProxyController              INSTANCE     = new ProxyController();

    private ArrayList<ProxyInfo>                      proxies      = new ArrayList<ProxyInfo>();
    private ArrayList<ProxyInfo>                      directs      = new ArrayList<ProxyInfo>();
    private ProxyInfo                                 defaultproxy = null;

    private DefaultEventSender<ProxyEvent<ProxyInfo>> eventSender  = null;

    private ProxyInfo                                 none         = null;

    private InternetConnectionSettings                config;

    public static final ProxyController getInstance() {
        return INSTANCE;
    }

    public DefaultEventSender<ProxyEvent<ProxyInfo>> getEventSender() {
        return eventSender;
    }

    private ProxyController() {
        eventSender = new DefaultEventSender<ProxyEvent<ProxyInfo>>();
        config = JsonConfig.create(InternetConnectionSettings.class);
        none = new ProxyInfo(HTTPProxy.NONE);
        setDefaultProxy(none);
        initDirects();
        loadProxySettings();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                saveProxySettings();
            }

            @Override
            public String toString() {
                return "ProxyController" + " Priority: " + this.getHookPriority();
            }

        });
    }

    private void initDirects() {
        synchronized (directs) {
            for (InetAddress ip : HTTPProxyUtils.getLocalIPs()) {
                HTTPProxy p = new HTTPProxy(TYPE.DIRECT);
                p.setLocalIP(ip);
                directs.add(new ProxyInfo(p));
            }
            if (directs.size() <= 1) {
                // we can use non if we have only one WAN ips anyway
                directs.clear();
            }
        }
    }

    private void saveProxySettings() {
        synchronized (proxies) {
            ArrayList<ProxyData> ret = new ArrayList<ProxyData>();

            for (ProxyInfo proxy : proxies) {
                ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == defaultproxy);
                ret.add(pd);

            }
            config.setCustomProxyList(ret);
        }

        synchronized (directs) {
            ArrayList<DirectGatewayData> ret = new ArrayList<DirectGatewayData>();

            for (ProxyInfo proxy : directs) {
                DirectGatewayData d = new DirectGatewayData();
                d.setDefault(proxy == defaultproxy);
                d.setIp(proxy.getProxy().getLocalIP().getHostAddress());
                d.setProxyRotationEnabled(proxy.isProxyRotationEnabled());

                ret.add(d);

            }
            config.setDirectGatewayList(ret);
        }
        // ProxyData n = none.toProxyData();
        // config.setNoneProxy();
        config.setNoneDefault(none == defaultproxy);
        config.setNoneRotationEnabled(none.isProxyRotationEnabled());

    }

    private void loadProxySettings() {
        ArrayList<ProxyData> ret = config.getCustomProxyList();

        // restore customs
        if (ret != null) {
            for (ProxyData proxyData : ret) {
                ProxyInfo proxy = null;
                try {
                    // we do not restore direct

                    /* convert proxyData to ProxyInfo */
                    proxies.add(proxy = new ProxyInfo(proxyData));
                    if (proxyData.isDefaultProxy()) {
                        setDefaultProxy(proxy);
                    }

                } catch (final Throwable e) {
                    JDLogger.exception(e);
                }
            }
        }
        // restore directs
        ArrayList<DirectGatewayData> ret2 = config.getDirectGatewayList();
        if (ret2 != null) {
            for (DirectGatewayData d : ret2) {
                ProxyInfo p = getDirectProxyByGatewayIP(d.getIp());
                if (p != null) {
                    p.setProxyRotationEnabled(d.isProxyRotationEnabled());
                    if (d.isDefault()) {
                        setDefaultProxy(p);
                    }
                }
            }
        }

        if (config.isNoneDefault()) {
            setDefaultProxy(none);
        }

        none.setProxyRotationEnabled(config.isNoneRotationEnabled());
        saveProxySettings();
    }

    private ProxyInfo getDirectProxyByGatewayIP(String gatewayIP) {
        synchronized (directs) {
            for (ProxyInfo pi : directs) {
                if (pi.getProxy().getLocalIP().getHostAddress().equalsIgnoreCase(gatewayIP)) { return pi; }
            }
        }
        return null;
    }

    public void init() {
    }

    /**
     * returns a copy of current proxy list
     * 
     * @return
     */
    public ArrayList<ProxyInfo> getList() {
        ArrayList<ProxyInfo> ret = new ArrayList<ProxyInfo>();

        ret.add(none);

        synchronized (directs) {
            ret.addAll(directs);
        }

        synchronized (proxies) {

            ret.addAll(proxies);
        }
        return ret;
    }

    /**
     * returns the default proxy for all normal browser activity as well as for
     * premium usage
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
        synchronized (proxies) {
            if (defaultproxy == def) return;
            if (def == null) {
                defaultproxy = none;
            } else {
                defaultproxy = def;
            }
        }
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.REFRESH, null));
    }

    /**
     * add proxy to proxylist in case its not in it yet
     * 
     * @param proxy
     */
    public void addProxy(HTTPProxy proxy) {
        if (proxy == null) return;
        ProxyInfo ret = null;
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                HTTPProxy pro = info.getProxy();
                if (pro.sameProxy(proxy)) return;
            }
            proxies.add(ret = new ProxyInfo(proxy));
        }
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.ADDED, ret));
    }

    /**
     * enable/disable given proxy, enables none-proxy in case no proxy would be
     * enabled anymore
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

        synchronized (proxies) {
            if (proxies.remove(proxy)) {
                if (proxy == defaultproxy) {
                    setDefaultProxy(none);
                }
            } else {
                return;
            }
        }

        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.REMOVED, proxy));
    }

    public ProxyInfo getProxyForDownload(PluginForHost plugin, Account acc) {
        final String host = plugin.getHost();
        final int maxactive = plugin.getMaxSimultanDownload(acc);
        if (acc != null) {
            /* an account must be used or waittime must be over */
            /*
             * only the default proxy may use accounts, to prevent accountblocks
             * because of simultan ip's using it
             */
            int active = defaultproxy.activeDownloadsbyHosts(host);
            if (active < maxactive) return defaultproxy;

            return null;
        }

        if (none.isProxyRotationEnabled()) {
            /* only use enabled proxies */

            if (none.getRemainingIPBlockWaittime(host) <= 0 && none.getRemainingTempUnavailWaittime(host) <= 0) {
                /* active downloads must be less than allowed download */
                int active = none.activeDownloadsbyHosts(host);
                if (active < maxactive) return none;
            }
        }
        synchronized (directs) {

            for (ProxyInfo info : directs) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */

                    if (info.getRemainingIPBlockWaittime(host) <= 0 && info.getRemainingTempUnavailWaittime(host) <= 0) {
                        /* active downloads must be less than allowed download */
                        int active = info.activeDownloadsbyHosts(host);
                        if (active < maxactive) return info;
                    }
                }
            }
        }
        synchronized (proxies) {

            for (ProxyInfo info : proxies) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */

                    if (info.getRemainingIPBlockWaittime(host) <= 0 && info.getRemainingTempUnavailWaittime(host) <= 0) {
                        /* active downloads must be less than allowed download */
                        int active = info.activeDownloadsbyHosts(host);
                        if (active < maxactive) return info;
                    }
                }
            }
        }
        return null;
    }

    /* optimize for speed */
    public long getRemainingIPBlockWaittime(final String host) {
        long ret = -1;
        if (none.isProxyRotationEnabled()) {
            ret = Math.max(0, none.getRemainingIPBlockWaittime(host));
        }
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */

                    long ret2 = ret = Math.max(0, info.getRemainingIPBlockWaittime(host));
                    if (ret2 < ret) ret = ret2;
                }
            }
        }

        synchronized (directs) {
            for (ProxyInfo info : directs) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */

                    long ret2 = ret = Math.max(0, info.getRemainingIPBlockWaittime(host));
                    if (ret2 < ret) ret = ret2;
                }
            }
        }
        return ret;
    }

    /* optimize for speed */
    public long getRemainingTempUnavailWaittime(final String host) {
        long ret = -1;
        if (none.isProxyRotationEnabled()) {
            ret = Math.max(0, none.getRemainingTempUnavailWaittime(host));
        }
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */

                    long ret2 = ret = Math.max(0, info.getRemainingTempUnavailWaittime(host));
                    if (ret2 < ret) ret = ret2;
                }
            }
        }

        synchronized (directs) {
            for (ProxyInfo info : directs) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */

                    long ret2 = ret = Math.max(0, info.getRemainingTempUnavailWaittime(host));
                    if (ret2 < ret) ret = ret2;
                }
            }
        }
        return ret;
    }

    public boolean hasRemainingIPBlockWaittime(final String host) {

        if (none.isProxyRotationEnabled()) {

            if (none.getRemainingIPBlockWaittime(host) > 0) return true;
        }
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */

                    if (info.getRemainingIPBlockWaittime(host) > 0) return true;
                }
            }
        }

        synchronized (directs) {
            for (ProxyInfo info : directs) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */
                    if (info.getRemainingIPBlockWaittime(host) > 0) return true;
                }
            }
        }
        return false;
    }

    public boolean hasTempUnavailWaittime(final String host) {

        if (none.isProxyRotationEnabled()) {

            if (none.getRemainingTempUnavailWaittime(host) > 0) return true;
        }
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */

                    if (info.getRemainingTempUnavailWaittime(host) > 0) return true;
                }
            }
        }

        synchronized (directs) {
            for (ProxyInfo info : directs) {
                if (info.isProxyRotationEnabled()) {
                    /* only use enabled proxies */
                    if (info.getRemainingTempUnavailWaittime(host) > 0) return true;
                }
            }
        }
        return false;
    }

    public void resetTempUnavailWaittime(final String host, boolean onlyLocal) {

        none.resetTempUnavailWaittime(host);
        synchronized (directs) {
            for (ProxyInfo info : directs) {
                info.resetIPBlockWaittime(host);
            }
        }
        if (!onlyLocal) {
            synchronized (proxies) {
                for (ProxyInfo info : proxies) {
                    // if (onlyLocal && info.getProxy().isRemote()) continue;
                    info.resetTempUnavailWaittime(host);
                }
            }
        }
    }

    public void resetIPBlockWaittime(final String host, boolean onlyLocal) {
        none.resetIPBlockWaittime(host);
        synchronized (directs) {
            for (ProxyInfo info : directs) {
                info.resetIPBlockWaittime(host);
            }
        }
        if (!onlyLocal) {
            synchronized (proxies) {
                for (ProxyInfo info : proxies) {
                    // if (onlyLocal && info.getProxy().isRemote()) continue;
                    info.resetIPBlockWaittime(host);
                }
            }
        }
    }
}
