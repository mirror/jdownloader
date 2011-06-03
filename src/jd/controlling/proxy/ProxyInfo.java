package jd.controlling.proxy;

import java.util.HashMap;

import jd.controlling.proxy.ProxyData.StatusID;
import jd.controlling.proxy.ProxyData.Type;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ProxyInfo {

    private final HTTPProxy                proxy;
    private boolean                        proxyRotationEnabled = true;

    private final HashMap<String, Integer> activeHosts          = new HashMap<String, Integer>();
    private final HashMap<String, Long>    ipblockedHosts       = new HashMap<String, Long>();
    private final HashMap<String, Long>    unavailHosts         = new HashMap<String, Long>();

    /**
     * @return the enabled
     */
    public boolean isProxyRotationEnabled() {
        return proxyRotationEnabled;
    }

    /**
     * @param enabled
     *            the enabled to set
     */
    protected void setProxyRotationEnabled(boolean enabled) {
        this.proxyRotationEnabled = enabled;
    }

    public ProxyData toProxyData() {
        ProxyData ret = new ProxyData();
        ret.setProxyRotationEnabled(this.isProxyRotationEnabled());
        ret.setHost(proxy.getHost());
        ret.setUser(proxy.getUser());
        ret.setPort(proxy.getPort());
        ret.setType(Type.valueOf(proxy.getType().name()));
        ret.setStatus(StatusID.valueOf(proxy.getStatus().name()));

        return ret;
    }

    public ProxyInfo(ProxyData proxyData) {
        this.proxy = new HTTPProxy(HTTPProxy.TYPE.valueOf(proxyData.getType().name()), proxyData.getHost(), proxyData.getPort());
        this.proxy.setPass(proxyData.getPass());
        this.proxy.setUser(proxyData.getUser());
        this.proxy.setStatus(HTTPProxy.STATUS.valueOf(proxyData.getStatus().name()));
        this.proxyRotationEnabled = proxyData.isProxyRotationEnabled();
    }

    public ProxyInfo(HTTPProxy proxy) {
        this.proxy = proxy;
    }

    public final HTTPProxy getProxy() {
        return proxy;
    }

    protected long getRemainingIPBlockWaittime(final String host) {
        synchronized (ipblockedHosts) {
            if (!ipblockedHosts.containsKey(host)) { return 0; }
            long ret = Math.max(0, ipblockedHosts.get(host) - System.currentTimeMillis());
            if (ret == 0) {
                ipblockedHosts.remove(host);
            }
            return ret;
        }
    }

    protected long getRemainingTempUnavailWaittime(final String host) {
        synchronized (unavailHosts) {
            if (!unavailHosts.containsKey(host)) { return 0; }
            long ret = Math.max(0, unavailHosts.get(host) - System.currentTimeMillis());
            if (ret == 0) {
                unavailHosts.remove(host);
            }
            return ret;
        }
    }

    public void setRemainingTempUnavail(final String host, final long waittime) {
        synchronized (unavailHosts) {
            if (waittime <= 0) {
                unavailHosts.remove(host);
            } else {
                this.unavailHosts.put(host, System.currentTimeMillis() + waittime);
            }
        }
    }

    public void setRemainingIPBlockWaittime(final String host, final long waittime) {
        synchronized (ipblockedHosts) {
            if (waittime <= 0) {
                ipblockedHosts.remove(host);
            } else {
                ipblockedHosts.put(host, System.currentTimeMillis() + waittime);
            }
        }
    }

    protected void resetIPBlockWaittime(final String host) {
        synchronized (ipblockedHosts) {
            if (host == null) {
                ipblockedHosts.clear();
            } else {
                ipblockedHosts.remove(host);
            }
        }
    }

    public void resetTempUnavailWaittime(final String host) {
        synchronized (unavailHosts) {
            if (host == null) {
                unavailHosts.clear();
            } else {
                unavailHosts.remove(host);
            }
        }
    }

    public int activeDownloadsbyHosts(final String host) {
        synchronized (this.activeHosts) {
            if (this.activeHosts.containsKey(host)) { return this.activeHosts.get(host); }
        }
        return 0;
    }

    public void increaseActiveDownloads(final String host) {
        synchronized (this.activeHosts) {
            Integer ret = this.activeHosts.get(host);
            if (ret == null) {
                ret = 1;
            } else {
                ret++;
            }
            this.activeHosts.put(host, ret);
        }
    }

    public void decreaseActiveDownloads(final String host) {
        synchronized (this.activeHosts) {
            Integer ret = this.activeHosts.get(host);
            if (ret == null || ret == 0) {
                throw new RuntimeException("How could this happen?");
            } else {
                ret--;
            }
            if (ret == 0) {
                this.activeHosts.remove(host);
            } else {
                this.activeHosts.put(host, ret);
            }
        }
    }

    @Override
    public String toString() {
        return proxy.toString();
    }
}
