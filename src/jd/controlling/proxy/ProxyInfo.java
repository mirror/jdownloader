package jd.controlling.proxy;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.DownloadLink;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ProxyInfo extends HTTPProxy {

    final private static AtomicLong           IDs                  = new AtomicLong(0);
    private boolean                           proxyRotationEnabled = true;
    private String                            ID                   = null;

    private final HashMap<String, Integer>    activeHosts          = new HashMap<String, Integer>();

    private final HashMap<String, ProxyBlock> unavailBlocks        = new HashMap<String, ProxyBlock>();
    private final HashMap<String, ProxyBlock> ipBlocks             = new HashMap<String, ProxyBlock>();

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
        ret.setProxy(HTTPProxy.getStorable(this));
        ret.setID(this.ID);
        return ret;
    }

    public ProxyInfo(ProxyData proxyData, HTTPProxy proxyTemplate) {
        this.cloneProxy(proxyTemplate);
        this.proxyRotationEnabled = proxyData.isProxyRotationEnabled();
        this.ID = proxyData.getID();
        if (ID == null) {
            if (isNone()) {
                this.ID = "NONE";
            } else {
                this.ID = getType().name() + IDs.incrementAndGet() + "_" + System.currentTimeMillis();
            }
        }
    }

    public ProxyInfo(HTTPProxy proxy) {
        cloneProxy(proxy);
        if (proxy.isNone()) {
            this.ID = "NONE";
        } else {
            this.ID = proxy.getType().name() + IDs.incrementAndGet() + "_" + System.currentTimeMillis();
        }
    }

    protected ProxyBlock getHostIPBlockTimeout(final String host) {
        synchronized (ipBlocks) {
            ProxyBlock ret = ipBlocks.get(host);
            if (ret != null && (ret.getBlockedTimeout()) <= 0) {
                ipBlocks.remove(host);
                ret = null;
            }
            return ret;
        }
    }

    protected ProxyBlock getHostBlockedTimeout(final String host) {
        synchronized (unavailBlocks) {
            ProxyBlock ret = unavailBlocks.get(host);
            if (ret != null && (ret.getBlockedTimeout()) <= 0) {
                unavailBlocks.remove(host);
                ret = null;
            }
            return ret;
        }
    }

    public ProxyBlock setHostBlockedTimeout(final DownloadLink link, final long waittime) {
        synchronized (unavailBlocks) {
            if (waittime <= 0) {
                unavailBlocks.remove(link.getHost());
                return null;
            } else {
                ProxyBlock old = unavailBlocks.get(link.getHost());
                if (old == null || old.getBlockedUntil() > System.currentTimeMillis() + waittime) {
                    unavailBlocks.put(link.getHost(), old = new ProxyBlock(link, System.currentTimeMillis() + waittime, ProxyBlock.REASON.UNAVAIL));
                }
                return old;
            }
        }
    }

    /* set IPBlock timeout for host of given DownloadLink */
    public ProxyBlock setHostIPBlockTimeout(final DownloadLink link, final long waittime) {
        synchronized (ipBlocks) {
            if (waittime <= 0) {
                ipBlocks.remove(link.getHost());
                return null;
            } else {
                ProxyBlock old = ipBlocks.get(link.getHost());
                if (old == null || old.getBlockedUntil() > System.currentTimeMillis() + waittime) {
                    ipBlocks.put(link.getHost(), old = new ProxyBlock(link, System.currentTimeMillis() + waittime, ProxyBlock.REASON.IP));
                }
                return old;
            }
        }
    }

    protected void removeHostIPBlockTimeout(final String host) {
        synchronized (ipBlocks) {
            if (host == null) {
                ipBlocks.clear();
            } else {
                ipBlocks.remove(host);
            }
        }
    }

    public void removeHostBlockedWaittime(final String host) {
        synchronized (unavailBlocks) {
            if (host == null) {
                unavailBlocks.clear();
            } else {
                unavailBlocks.remove(host);
            }
        }
    }

    public int activeDownloadsbyHosts(final String host) {
        synchronized (this.activeHosts) {
            Integer ret = this.activeHosts.get(host);
            if (ret != null) return ret;
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

}
