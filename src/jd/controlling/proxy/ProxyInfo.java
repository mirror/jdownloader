package jd.controlling.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.updatev2.ProxyData;

public class ProxyInfo extends HTTPProxy {

    final private static AtomicLong           IDs                  = new AtomicLong(0);
    private boolean                           proxyRotationEnabled = true;
    private String                            ID                   = null;
    private HashSet<String>                   permitDenyList       = new HashSet<String>();

    private final HashMap<String, Integer>    activeHosts          = new HashMap<String, Integer>();

    private final HashMap<String, ProxyBlock> unavailBlocks        = new HashMap<String, ProxyBlock>();
    private final HashMap<String, ProxyBlock> ipBlocks             = new HashMap<String, ProxyBlock>();
    /*
     * by default a proxy supports resume
     */
    private boolean                           resumeIsAllowed      = true;

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
        ret.setPermitDenyList(getPermitDenyList());
        ret.setResumeIsAllowed(this.resumeIsAllowed);
        return ret;
    }

    public ProxyInfo(ProxyData proxyData, HTTPProxy proxyTemplate) {
        this.cloneProxy(proxyTemplate);
        this.proxyRotationEnabled = proxyData.isProxyRotationEnabled();
        this.ID = proxyData.getID();
        this.setPermitDenyList(proxyData.getPermitDenyList());
        this.setConnectMethodPrefered(proxyData.getProxy().isConnectMethodPrefered());
        this.setResumeAllowed(proxyData.isResumeAllowed());
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
        this.setPermitDenyList(null);
    }

    public List<String> getPermitDenyList() {
        synchronized (permitDenyList) {
            return new ArrayList<String>(this.permitDenyList);
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

    public void setPermitDenyList(List<String> permitDenyList) {
        HashSet<String> newList = new HashSet<String>();
        if (permitDenyList == null || permitDenyList.size() == 0) {
            /* by default every host is allowed */
            newList.add("+*");
        } else {
            newList.addAll(permitDenyList);
        }
        this.permitDenyList = newList;
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

    public boolean isHostAllowed(String host) {
        HashSet<String> lpermitDenyList = permitDenyList;
        if (lpermitDenyList.contains("+*")) {
            /* list contains allow all */
            if (lpermitDenyList.contains("-" + host)) {
                /* host is not allowed */
                return false;
            }
            return true;
        } else {
            /* list does not allow all, so we have to check for host */
            return lpermitDenyList.contains("+" + host);
        }
    }

    public void setHostPermitDenyState(String host, boolean allowed) {
        HashSet<String> newList = new HashSet<String>(permitDenyList);
        if (host == null) {
            /** host is null so we want to permit/deny everything */
            if (allowed) {
                /* we want to allow everything */
                newList.remove("-*");
                newList.add("+*");
            } else {
                /* we want to forbidd everything */
                newList.remove("+*");
                newList.add("-*");
            }
        } else {
            /* we only want to permit/deny given host */
            if (allowed) {
                /* we want to allow host */
                newList.remove("-" + host);
                newList.add("+" + host);
            } else {
                /* we want to forbidd host */
                newList.remove("+" + host);
                newList.add("-" + host);
            }
        }
        permitDenyList = newList;
    }

    public void removeHostFromPermitDenyList(String host) {
        if (StringUtils.isEmpty(host)) {
            // remove everything
            setPermitDenyList(null);
        } else {
            HashSet<String> newList = new HashSet<String>(permitDenyList);
            /* remove existing permit/deny rule for given host */
            newList.remove("-" + host);
            newList.remove("+" + host);
            permitDenyList = newList;
        }
    }

    public void setResumeAllowed(boolean b) {
        resumeIsAllowed = b;
    }

    public boolean isResumeAllowed() {
        if (this.isLocal()) return true;
        return resumeIsAllowed;
    }
}
