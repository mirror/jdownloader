package jd.controlling.proxy;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import jd.controlling.JDLogger;
import jd.controlling.proxy.ProxyData.Type;
import jd.plugins.Account;
import jd.plugins.PluginForHost;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.appwork.utils.Regex;
import org.appwork.utils.event.DefaultEventSender;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.settings.InternetConnectionSettings;

public class ProxyController implements ConfigEventListener {

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

    public void onConfigValueModified(ConfigInterface config, String key, Object newValue) {
        System.out.println("Reload");
        config.getStorageHandler().getEventSender().removeListener(this);
        loadProxySettings();
        config.getStorageHandler().getEventSender().addListener(this);
    }

    private ProxyController() {
        eventSender = new DefaultEventSender<ProxyEvent<ProxyInfo>>();
        config = JsonConfig.create(InternetConnectionSettings.class);

        none = new ProxyInfo(HTTPProxy.NONE);

        initDirects();
        loadProxySettings();
        saveProxySettings();
        config.getStorageHandler().getEventSender().addListener(this);
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

    public static ArrayList<ProxyData> autoConfig() {
        ArrayList<ProxyData> ret = new ArrayList<ProxyData>();
        try {
            if (CrossSystem.isWindows()) { return ProxyController.checkReg(); }
            /* we enable systemproxies to query them for a test getPage */
            System.setProperty("java.net.useSystemProxies", "true");

            List<Proxy> l;
            l = ProxySelector.getDefault().select(new URI("http://www.appwork.org"));

            for (final Proxy p : l) {
                final SocketAddress ad = p.address();
                if (ad != null && ad instanceof InetSocketAddress) {
                    final InetSocketAddress isa = (InetSocketAddress) ad;
                    if (isa.getHostName().trim().length() == 0) {
                        continue;
                    }
                    switch (p.type()) {
                    case HTTP:
                        ProxyData pd = new ProxyData();
                        pd.setHost(isa.getHostName());
                        pd.setPort(isa.getPort());
                        pd.setType(Type.HTTP);
                        ret.add(pd);

                        break;
                    case SOCKS:
                        pd = new ProxyData();
                        pd.setHost(isa.getHostName());
                        pd.setPort(isa.getPort());
                        pd.setType(Type.SOCKS5);
                        ret.add(pd);
                        break;
                    }
                }
            }
        } catch (final Throwable e1) {
            Log.exception(Level.WARNING, e1);
        } finally {
            System.setProperty("java.net.useSystemProxies", "false");

        }
        return ret;
    }

    private static byte[] toCstr(final String str) {
        final byte[] result = new byte[str.length() + 1];
        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }

    /**
     * Checks windows registry for proxy settings
     */
    private static ArrayList<ProxyData> checkReg() {
        ArrayList<ProxyData> ret = new ArrayList<ProxyData>();
        try {
            final Preferences userRoot = Preferences.userRoot();
            final Class<?> clz = userRoot.getClass();
            final Method openKey = clz.getDeclaredMethod("openKey", byte[].class, int.class, int.class);
            openKey.setAccessible(true);

            final Method closeKey = clz.getDeclaredMethod("closeKey", int.class);
            closeKey.setAccessible(true);
            final Method winRegQueryValue = clz.getDeclaredMethod("WindowsRegQueryValueEx", int.class, byte[].class);
            winRegQueryValue.setAccessible(true);

            byte[] valb = null;
            String val = null;
            String key = null;
            Integer handle = -1;

            // Query Internet Settings for Proxy
            key = "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings";
            try {
                handle = (Integer) openKey.invoke(userRoot, ProxyController.toCstr(key), 0x20019, 0x20019);
                valb = (byte[]) winRegQueryValue.invoke(userRoot, handle.intValue(), ProxyController.toCstr("ProxyServer"));
                val = valb != null ? new String(valb).trim() : null;
            } finally {
                closeKey.invoke(Preferences.userRoot(), handle);
            }
            if (val != null) {
                for (String vals : val.split(";")) {
                    /* parse ip */
                    String proxyurl = new Regex(vals, "(\\d+\\.\\d+\\.\\d+\\.\\d+)").getMatch(0);
                    if (proxyurl == null) {
                        /* parse domain name */
                        proxyurl = new Regex(vals, "=(.*?)($|:)").getMatch(0);
                    }
                    final String port = new Regex(vals, ":(\\d+)").getMatch(0);
                    if (proxyurl != null) {
                        if (vals.trim().contains("socks")) {
                            final int rPOrt = port != null ? Integer.parseInt(port) : 1080;
                            ProxyData pd = new ProxyData();
                            pd.setHost(proxyurl);
                            pd.setPort(rPOrt);
                            pd.setType(Type.SOCKS5);
                            ret.add(pd);
                        } else {
                            final int rPOrt = port != null ? Integer.parseInt(port) : 8080;
                            ProxyData pd = new ProxyData();
                            pd.setHost(proxyurl);
                            pd.setPort(rPOrt);
                            pd.setType(Type.HTTP);
                            ret.add(pd);
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            Log.exception(Level.WARNING, e);
        }
        return ret;
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

        setDefaultProxy(none);
        ArrayList<ProxyData> ret = config.getCustomProxyList();
        ArrayList<ProxyData> autoProxies = autoConfig();
        System.out.println("Found  defaultProxies" + JSonStorage.toString(autoProxies));
        boolean rotCheck = false;
        synchronized (proxies) {

            proxies.clear();

            HashMap<String, ProxyInfo> dupeMap = new HashMap<String, ProxyInfo>();
            ProxyInfo proxy = null;
            // restore customs
            if (ret != null) {
                for (ProxyData proxyData : ret) {

                    try {
                        // we do not restore direct

                        /* convert proxyData to ProxyInfo */
                        proxies.add(proxy = new ProxyInfo(proxyData));
                        dupeMap.put(proxyData.getHost() + ":" + proxyData.getPort() + "_" + proxyData.getType(), proxy);
                        if (proxyData.isDefaultProxy()) {
                            setDefaultProxy(proxy);
                        }
                        if (proxy.isProxyRotationEnabled()) rotCheck = true;

                    } catch (final Throwable e) {
                        JDLogger.exception(e);
                    }
                }
            }

            for (ProxyData proxyData : autoProxies) {
                ProxyInfo dupe = dupeMap.get(proxyData.getHost() + ":" + proxyData.getPort() + "_" + proxyData.getType());
                if (dupe == null) {
                    proxies.add(proxy = new ProxyInfo(proxyData));
                    dupeMap.put(proxyData.getHost() + ":" + proxyData.getPort() + "_" + proxyData.getType(), proxy);
                }
                if (!config.isNoneDefault() && defaultproxy == none) {
                    setDefaultProxy(proxy);
                    if (rotCheck) {
                        proxy.setProxyRotationEnabled(true);
                    }
                }
            }

        }
        synchronized (directs) {
            directs.clear();

            // restore directs
            ArrayList<DirectGatewayData> ret2 = config.getDirectGatewayList();
            if (ret2 != null) {
                for (DirectGatewayData d : ret2) {
                    ProxyInfo p = getDirectProxyByGatewayIP(d.getIp());
                    if (p != null) {
                        p.setProxyRotationEnabled(d.isProxyRotationEnabled());
                        if (p.isProxyRotationEnabled()) rotCheck = true;
                        if (d.isDefault()) {
                            setDefaultProxy(p);
                        }
                    }
                }
            }
        }

        // if (config.isNoneDefault()) {
        // setDefaultProxy(none);
        //
        // }

        none.setProxyRotationEnabled(config.isNoneRotationEnabled());
        if (none.isProxyRotationEnabled()) rotCheck = true;

        if (!rotCheck) {
            // we need at least one rotation
            none.setProxyRotationEnabled(true);
            config.setNoneRotationEnabled(true);
        }
        if (none == defaultproxy) config.setNoneDefault(true);
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(ProxyController.this, ProxyEvent.Types.REFRESH, null));

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

    public ArrayList<ProxyInfo> getListForRotation() {
        ArrayList<ProxyInfo> ret = new ArrayList<ProxyInfo>();

        if (none.isProxyRotationEnabled()) ret.add(none);

        synchronized (directs) {
            for (ProxyInfo pi : directs) {
                if (pi.isProxyRotationEnabled()) ret.add(pi);

            }

        }

        synchronized (proxies) {
            for (ProxyInfo pi : proxies) {
                if (pi.isProxyRotationEnabled()) ret.add(pi);

            }

        }
        return ret;
    }

    public ProxyInfo getNone() {
        return none;
    }

    public void onConfigValidatorError(ConfigInterface config, Throwable validateException, KeyHandler methodHandler) {
    }
}
