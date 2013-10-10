package jd.controlling.proxy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.downloadcontroller.SingleDownloadController;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.updatev2.ProxyData;

public class ProxyInfo extends HTTPProxy {

    final private static AtomicLong                                  IDs                             = new AtomicLong(0);
    private boolean                                                  proxyRotationEnabled            = true;
    private String                                                   ID                              = null;

    private final HashMap<String, HashSet<SingleDownloadController>> activeSingleDownloadControllers = new HashMap<String, HashSet<SingleDownloadController>>();

    /*
     * by default a proxy supports resume
     */
    private boolean                                                  resumeIsAllowed                 = true;

    /**
     * @return the enabled
     */
    public boolean isProxyRotationEnabled() {
        return proxyRotationEnabled;
    }

    public boolean isReconnectSupported() {
        switch (getType()) {
        case NONE:
            return true;
        default:
            return false;
        }
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
        ret.setRangeRequestsSupported(isResumeAllowed());
        return ret;
    }

    public ProxyInfo(ProxyData proxyData, HTTPProxy proxyTemplate) {
        this.cloneProxy(proxyTemplate);
        this.proxyRotationEnabled = proxyData.isProxyRotationEnabled();
        this.ID = proxyData.getID();
        this.setConnectMethodPrefered(proxyData.getProxy().isConnectMethodPrefered());
        this.setResumeAllowed(proxyData.isRangeRequestsSupported());
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

    public int activeDownloadsbyHosts(String host) {
        if (host == null) return 0;
        host = host.toLowerCase(Locale.ENGLISH);
        synchronized (activeSingleDownloadControllers) {
            HashSet<SingleDownloadController> ret = this.activeSingleDownloadControllers.get(host);
            if (ret != null) return ret.size();
        }
        return 0;
    }

    public boolean hasActiveDownloads() {
        synchronized (activeSingleDownloadControllers) {
            return activeSingleDownloadControllers.size() > 0;
        }
    }

    public boolean add(final SingleDownloadController singleDownloadController) {
        if (singleDownloadController == null) return false;
        String host = singleDownloadController.getDownloadLink().getHost().toLowerCase(Locale.ENGLISH);
        synchronized (activeSingleDownloadControllers) {
            HashSet<SingleDownloadController> ret = this.activeSingleDownloadControllers.get(host);
            if (ret == null) {
                ret = new HashSet<SingleDownloadController>();
                activeSingleDownloadControllers.put(host, ret);
            }
            return ret.add(singleDownloadController);
        }
    }

    public boolean remove(final SingleDownloadController singleDownloadController) {
        if (singleDownloadController == null) return false;
        String host = singleDownloadController.getDownloadLink().getHost().toLowerCase(Locale.ENGLISH);
        boolean remove = false;
        synchronized (activeSingleDownloadControllers) {
            HashSet<SingleDownloadController> ret = this.activeSingleDownloadControllers.get(host);
            if (ret != null) {
                remove = ret.remove(singleDownloadController);
                if (ret.size() == 0) activeSingleDownloadControllers.remove(host);
            }
        }
        return remove;
    }

    public void setResumeAllowed(boolean b) {
        resumeIsAllowed = b;
    }

    public boolean isResumeAllowed() {
        if (this.isLocal()) return true;
        return resumeIsAllowed;
    }
}
