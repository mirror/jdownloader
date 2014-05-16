package jd.controlling.proxy;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.ProxySelectorInterface;
import jd.http.Request;
import jd.plugins.Plugin;

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

    // private boolean useForPremiumEnabled = true;
    //
    // public boolean isUseForPremiumEnabled() {
    // return useForPremiumEnabled;
    // }

    // public void setUseForPremiumEnabled(boolean useForPremiumEnabled) {
    // this.useForPremiumEnabled = useForPremiumEnabled;
    // if (useForPremiumEnabled) {
    // // reset banlist on enable/disable
    // banList = new ArrayList<ProxyBan>();
    // onBanListUpdate();
    // }
    // }
    @Override
    public boolean reportConnectException(Request request, int retryCounter, IOException e) {
        return ProxyController.getInstance().reportConnectException(request, retryCounter, e);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean useForFreeEnabled) {
        this.enabled = useForFreeEnabled;

    }

    private boolean    enabled = true;
    private FilterList filter;

    public FilterList getFilter() {
        return filter;
    }

    public boolean isAllowedByFilter(String host) {
        if (filter == null) {
            return true;
        }

        return filter.validate(host);
    }

    public void setFilter(FilterList filter) {
        this.filter = filter;
    }

    // final protected static AtomicLong IDs = new AtomicLong(0);
    //
    // protected String ID = null;

    public ProxyData toProxyData() {

        ProxyData ret = new ProxyData();
        ret.setEnabled(isEnabled());

        ret.setFilter(getFilter());
        // ret.setID(this.ID);
        ret.setRangeRequestsSupported(isResumeAllowed());

        return ret;
    }

    abstract public Type getType();

    public void setType(Type value) {
        throw new IllegalStateException("This operation is not allowed on this Factory Type");
    }

    abstract public boolean isPreferNativeImplementation();

    abstract public void setPreferNativeImplementation(boolean preferNativeImplementation);

    abstract public String toExportString();

    private final HashMap<String, HashSet<SingleDownloadController>> activeSingleDownloadControllers = new HashMap<String, HashSet<SingleDownloadController>>();

    /*
     * by default a proxy supports resume
     */
    private boolean                                                  resumeIsAllowed                 = true;

    public boolean isReconnectSupported() {
        switch (getType()) {
        case NONE:
            return true;
        default:
            return false;
        }
    }

    public AbstractProxySelectorImpl() {

    }

    public int activeDownloadsbyHosts(String host) {
        if (host == null) {
            return 0;
        }
        host = host.toLowerCase(Locale.ENGLISH);
        synchronized (activeSingleDownloadControllers) {
            HashSet<SingleDownloadController> ret = this.activeSingleDownloadControllers.get(host);
            if (ret != null) {
                return ret.size();
            }
        }
        return 0;
    }

    public boolean hasActiveDownloads() {
        synchronized (activeSingleDownloadControllers) {
            return activeSingleDownloadControllers.size() > 0;
        }
    }

    public boolean add(final SingleDownloadController singleDownloadController) {
        if (singleDownloadController == null) {
            return false;
        }
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
        if (singleDownloadController == null) {
            return false;
        }
        String host = singleDownloadController.getDownloadLink().getHost().toLowerCase(Locale.ENGLISH);
        boolean remove = false;
        synchronized (activeSingleDownloadControllers) {
            HashSet<SingleDownloadController> ret = this.activeSingleDownloadControllers.get(host);
            if (ret != null) {
                remove = ret.remove(singleDownloadController);
                if (ret.size() == 0) {
                    activeSingleDownloadControllers.remove(host);
                }
            }
        }
        return remove;
    }

    public void setResumeAllowed(boolean b) {
        resumeIsAllowed = b;
    }

    public boolean isResumeAllowed() {
        if (this.isLocal()) {
            return true;
        }
        return resumeIsAllowed;
    }

    abstract protected boolean isLocal();

    private CopyOnWriteArrayList<ConnectionBan> banList = new CopyOnWriteArrayList<ConnectionBan>();

    public List<ConnectionBan> getBanList() {
        return Collections.unmodifiableList(banList);
    }

    public void addSessionBan(ConnectionBan ban) {
        banList.add(ban);
    }

    public boolean isProxyBannedFor(HTTPProxy orgReference, URL url, Plugin pluginFromThread) {

        for (ConnectionBan b : banList) {

            if (b.isExpired()) {
                banList.remove(b);
                continue;
            }
            if (b.isProxyBannedByUrlOrPlugin(orgReference, url, pluginFromThread)) {
                return true;
            }
        }
        return false;

    }

    public boolean isSelectorBannedFor(Plugin pluginForHost) {
        for (ConnectionBan b : banList) {
            if (b.isExpired()) {
                banList.remove(b);
                continue;
            }
            if (b.isSelectorBannedByPlugin(pluginForHost)) {
                return true;
            }
        }
        return false;
    }

    // public boolean isSelectorBannedFor(URL url) {
    // for (ConnectionBan b : banList) {
    // if (b.isExpired()) {
    // banList.remove(b);
    // continue;
    // }
    // if (b.isSelectorBannedByUrl(url)) {
    // return true;
    // }
    // }
    // return false;
    // }

}
