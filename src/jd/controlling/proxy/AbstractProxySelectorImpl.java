package jd.controlling.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.ProxySelectorInterface;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.updatev2.FilterList;
import org.jdownloader.updatev2.ProxyData;

public abstract class AbstractProxySelectorImpl implements ProxySelectorInterface {
    public enum Type {
        NONE,
        DIRECT,
        SOCKS4,
        SOCKS5,
        HTTP,
        PAC
    }

    private FilterList filter;

    public FilterList getFilter() {
        return filter;
    }

    public boolean isAllowedByFilter(String host) {
        if (filter == null)
            return true;

        return filter.validate(host);
    }

    public void setFilter(FilterList filter) {
        this.filter = filter;
    }

    final protected static AtomicLong IDs = new AtomicLong(0);

    protected String                  ID  = null;

    abstract public ProxyData toProxyData();

    abstract public Type getType();

    public void setType(Type value) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    abstract public boolean isPreferNativeImplementation();

    abstract public void setPreferNativeImplementation(boolean preferNativeImplementation);

    abstract public boolean setRotationEnabled(ExtProxy p, boolean enabled);

    abstract public String toExportString();

    abstract public List<HTTPProxy> listProxies();

    private final HashMap<String, HashSet<SingleDownloadController>> activeSingleDownloadControllers = new HashMap<String, HashSet<SingleDownloadController>>();

    /*
     * by default a proxy supports resume
     */
    private boolean                                                  resumeIsAllowed                 = true;
    private boolean                                                  proxyRotationEnabled;

    protected ArrayList<ProxyBan>                                    banList;

    public boolean isProxyRotationEnabled() {
        return proxyRotationEnabled;
    }

    public void setProxyRotationEnabled(boolean noneRotationEnabled) {
        this.proxyRotationEnabled = noneRotationEnabled;
        if (proxyRotationEnabled) {
            // reset banlist on enable/disable
            banList = new ArrayList<ProxyBan>();
            onBanListUpdate();
        }
    }

    public boolean isReconnectSupported() {
        switch (getType()) {
        case NONE:
            return true;
        default:
            return false;
        }
    }

    public AbstractProxySelectorImpl() {
        banList = new ArrayList<ProxyBan>();
    }

    public int activeDownloadsbyHosts(String host) {
        if (host == null)
            return 0;
        host = host.toLowerCase(Locale.ENGLISH);
        synchronized (activeSingleDownloadControllers) {
            HashSet<SingleDownloadController> ret = this.activeSingleDownloadControllers.get(host);
            if (ret != null)
                return ret.size();
        }
        return 0;
    }

    public boolean hasActiveDownloads() {
        synchronized (activeSingleDownloadControllers) {
            return activeSingleDownloadControllers.size() > 0;
        }
    }

    public boolean add(final SingleDownloadController singleDownloadController) {
        if (singleDownloadController == null)
            return false;
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
        if (singleDownloadController == null)
            return false;
        String host = singleDownloadController.getDownloadLink().getHost().toLowerCase(Locale.ENGLISH);
        boolean remove = false;
        synchronized (activeSingleDownloadControllers) {
            HashSet<SingleDownloadController> ret = this.activeSingleDownloadControllers.get(host);
            if (ret != null) {
                remove = ret.remove(singleDownloadController);
                if (ret.size() == 0)
                    activeSingleDownloadControllers.remove(host);
            }
        }
        return remove;
    }

    public void setResumeAllowed(boolean b) {
        resumeIsAllowed = b;
    }

    public boolean isResumeAllowed() {
        if (this.isLocal())
            return true;
        return resumeIsAllowed;
    }

    abstract protected boolean isLocal();

    /**
     * NOT SYNCHRONIZED:USE FROM DOWNLOADWATCHDOG QUEUE ONLY
     * 
     * @param proxyBan
     */
    public void addBan(ProxyBan proxyBan) {

        ArrayList<ProxyBan> newList = new ArrayList<ProxyBan>(banList);
        newList.add(proxyBan);
        banList = newList;
        onBanListUpdate();

    }

    abstract protected void onBanListUpdate();

    public ArrayList<ProxyBan> getBanList() {
        return banList;
    }

    public boolean isBanned(HTTPProxy cached) {
        ArrayList<ProxyBan> cleanUp = null;
        try {
            for (ProxyBan pb : banList) {
                if (pb.getProxy() == null)
                    continue;
                if (pb.getUntil() > 0 && pb.getUntil() < System.currentTimeMillis()) {
                    if (cleanUp == null)
                        cleanUp = new ArrayList<ProxyBan>();
                    cleanUp.add(pb);
                    continue;
                }
                if (pb.getProxy().equals(cached)) {
                    //
                    return true;
                }
            }
        } finally {
            if (cleanUp != null) {
                ArrayList<ProxyBan> newList = new ArrayList<ProxyBan>(banList);
                newList.removeAll(cleanUp);
                banList = newList;
                onBanListUpdate();
            }
        }
        return false;
    }

    public boolean isBanned(String host) {

        ArrayList<ProxyBan> cleanUp = null;
        try {
            for (ProxyBan pb : banList) {
                if (pb.getProxy() != null)
                    continue;
                if (pb.getUntil() > 0 && pb.getUntil() < System.currentTimeMillis()) {
                    if (cleanUp == null)
                        cleanUp = new ArrayList<ProxyBan>();
                    cleanUp.add(pb);
                    continue;
                }
                if (host.equalsIgnoreCase(pb.getDomain())) {
                    //
                    return true;
                }
            }
        } finally {
            if (cleanUp != null) {
                ArrayList<ProxyBan> newList = new ArrayList<ProxyBan>(banList);
                newList.removeAll(cleanUp);
                banList = newList;
                onBanListUpdate();
            }
        }
        return false;

    }

}
