package jd.controlling.proxy;

import java.io.File;
import java.util.LinkedList;

import jd.controlling.JDLogger;
import jd.plugins.Account;
import jd.plugins.PluginForHost;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.event.DefaultEventSender;
import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ProxyController {

    private static final ProxyController INSTANCE = new ProxyController();

    private LinkedList<ProxyInfo> proxies = new LinkedList<ProxyInfo>();

    private ProxyInfo defaultproxy = null;

    private DefaultEventSender<ProxyEvent<ProxyInfo>> eventSender = null;

    private ProxyInfo none = null;
    private File settingsFile = null;

    public static final ProxyController getInstance() {
        return INSTANCE;
    }

    public DefaultEventSender<ProxyEvent<ProxyInfo>> getEventSender() {
        return eventSender;
    }

    private ProxyController() {
        eventSender = new DefaultEventSender<ProxyEvent<ProxyInfo>>();
        settingsFile = Application.getResource("cfg/proxysettings.json");
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

    private void saveProxySettings() {
        synchronized (proxies) {
            ProxyDataList ret = new ProxyDataList();
            int index = 0;
            for (ProxyInfo proxy : proxies) {
                ret.add(proxy.toProxyData());
                if (defaultproxy == proxy) {
                    ret.setDefaultProxy(index);
                }
                index++;
            }
            JSonStorage.saveTo(settingsFile, true, null, JSonStorage.serializeToJson(ret));
        }
    }

    private void loadProxySettings() {
        ProxyDataList ret = JSonStorage.restoreFrom(settingsFile, true, null, new TypeRef<ProxyDataList>() {
        }, new ProxyDataList());
        if (ret.size() == 0) {
            /*
             * no proxysettings available, create new proxylist with 'none' only
             * and make it default
             */
            none = new ProxyInfo(HTTPProxy.NONE);
            defaultproxy = none;
            proxies.add(none);
        } else {
            none = null;
            for (ProxyData proxyData : ret) {
                ProxyInfo proxy = null;
                try {
                    /* convert proxyData to ProxyInfo */
                    proxies.add(proxy = new ProxyInfo(proxyData));
                    if (proxy.getProxy().getType().equals(HTTPProxy.TYPE.NONE)) {
                        none = proxy;
                    }
                } catch (final Throwable e) {
                    JDLogger.exception(e);
                }
            }
            /* restore defaultProxy */
            try {
                defaultproxy = null;
                defaultproxy = proxies.get(ret.getDefaultProxy());
            } catch (final Throwable e) {
                /* invalid default proxy, set to first one */
                JDLogger.exception(e);
            }
            if (none == null) {
                /* add missing none proxy */
                none = new ProxyInfo(HTTPProxy.NONE);
                proxies.add(0, none);
            }
            if (defaultproxy == null) {
                /* in case we could not restore defaultproxy, we use 'none' */
                defaultproxy = none;
            }

        }
    }

    public void init() {
    }

    /**
     * returns a copy of current proxy list
     * 
     * @return
     */
    public LinkedList<ProxyInfo> getList() {
        return new LinkedList<ProxyInfo>(proxies);
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
    public void setEnabled(ProxyInfo proxy, boolean enabled) {
        if (proxy == null) return;
        if (proxy.isEnabled() == enabled) return;
        synchronized (proxies) {
            if (!proxies.contains(proxy)) return;
            proxy.setEnabled(enabled);
            boolean enableNone = true;
            for (ProxyInfo pro : proxies) {
                if (pro.isEnabled()) {
                    enableNone = false;
                    break;
                }
            }
            /*
             * if no proxy is enabled, at least the none proxy must always be
             * enabled
             */
            if (enableNone) {
                none.setEnabled(true);
            }
        }
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.REFRESH, null));
    }

    /** removes given proxy from proxylist */
    public void remove(ProxyInfo proxy) {
        if (proxy == null) return;
        /* none proxy cant get removed */
        if (proxy == none) return;
        if (proxy.getProxy().isNone()) return;
        synchronized (proxies) {
            if (!proxies.remove(proxy)) return;
            if (proxies.size() == 1) {
                /*
                 * if only one proxy is left, then we will set none as default
                 * and enabled
                 */
                none.setEnabled(true);
                defaultproxy = none;
            }
        }
        eventSender.fireEvent(new ProxyEvent<ProxyInfo>(this, ProxyEvent.Types.REMOVED, proxy));
    }

    public ProxyInfo getProxyForDownload(PluginForHost plugin, Account acc) {
        synchronized (proxies) {
            final String host = plugin.getHost();
            final int maxactive = plugin.getMaxSimultanDownload(acc);
            for (ProxyInfo info : proxies) {
                if (!info.isEnabled()) {
                    /* only use enabled proxies */
                    continue;
                }
                /* an account must be used or waittime must be over */
                /*
                 * only the default proxy may use accounts, to prevent
                 * accountblocks because of simultan ip's using it
                 */
                if ((info == defaultproxy && acc != null) || (info.getRemainingIPBlockWaittime(host) <= 0 && info.getRemainingTempUnavailWaittime(host) <= 0)) {
                    /* active downloads must be less than allowed download */
                    int active = info.activeDownloadsbyHosts(host);
                    if (active < maxactive) return info;
                }
            }
        }
        return null;
    }

    /* optimize for speed */
    public long getRemainingIPBlockWaittime(final String host) {
        Long ret = null;
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (!info.isEnabled()) {
                    /* only use enabled proxies */
                    continue;
                }
                long ret2 = info.getRemainingIPBlockWaittime(host);
                if (ret == null || ret2 < ret) ret = ret2;
            }
        }
        return ret;
    }

    /* optimize for speed */
    public long getRemainingTempUnavailWaittime(final String host) {
        Long ret = null;
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (!info.isEnabled()) {
                    /* only use enabled proxies */
                    continue;
                }
                long ret2 = info.getRemainingTempUnavailWaittime(host);
                if (ret == null || ret2 < ret) ret = ret2;
            }
        }
        return ret;
    }

    public boolean hasRemainingIPBlockWaittime(final String host) {
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (!info.isEnabled()) {
                    /* only use enabled proxies */
                    continue;
                }
                if (info.getRemainingIPBlockWaittime(host) > 0) return true;
            }
        }
        return false;
    }

    public boolean hasTempUnavailWaittime(final String host) {
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (!info.isEnabled()) {
                    /* only use enabled proxies */
                    continue;
                }
                if (info.getRemainingTempUnavailWaittime(host) > 0) return true;
            }
        }
        return false;
    }

    public void resetTempUnavailWaittime(final String host, boolean onlyLocal) {
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (onlyLocal && info.getProxy().isRemote()) continue;
                info.resetTempUnavailWaittime(host);
            }
        }
    }

    public void resetIPBlockWaittime(final String host, boolean onlyLocal) {
        synchronized (proxies) {
            for (ProxyInfo info : proxies) {
                if (onlyLocal && info.getProxy().isRemote()) continue;
                info.resetIPBlockWaittime(host);
            }
        }
    }
}
