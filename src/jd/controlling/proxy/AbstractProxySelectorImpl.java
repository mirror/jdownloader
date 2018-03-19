package jd.controlling.proxy;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.ProxySelectorInterface;
import jd.http.Request;
import jd.plugins.Account;
import jd.plugins.Plugin;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.FilterList;
import org.jdownloader.updatev2.ProxyData;

public abstract class AbstractProxySelectorImpl implements ProxySelectorInterface {
    public static enum Type {
        NONE,
        DIRECT,
        SOCKS4,
        SOCKS5,
        HTTP,
        HTTPS,
        PAC
    }

    @Override
    public boolean reportConnectException(Request request, int retryCounter, IOException e) {
        return ProxyController.getInstance().reportConnectException(request, retryCounter, e);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public boolean setEnabled(boolean enabled) {
        final boolean ret = this.enabled.getAndSet(enabled);
        if (enabled) {
            clearBanList();
        }
        return ret;
    }

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private volatile FilterList filter;

    protected void clearBanList() {
        banList.clear();
    }

    public FilterList getFilter() {
        return filter;
    }

    public boolean isAllowedByFilter(final String host, final Account acc) {
        final FilterList lFilter = filter;
        if (lFilter == null) {
            return true;
        }
        return lFilter.validate(host, acc == null ? null : acc.getUser());
    }

    public void setFilter(final FilterList filter) {
        this.filter = filter;
    }

    public ProxyData toProxyData() {
        final ProxyData ret = new ProxyData();
        ret.setEnabled(isEnabled());
        ret.setFilter(getFilter());
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

    /*
     * by default a proxy supports resume
     */
    private boolean                                               resumeIsAllowed                 = true;
    protected final CopyOnWriteArraySet<SingleDownloadController> activeSingleDownloadControllers = new CopyOnWriteArraySet<SingleDownloadController>();
    protected final CopyOnWriteArrayList<SelectProxyByURLHook>    selectProxyByURLHooks           = new CopyOnWriteArrayList<SelectProxyByURLHook>();
    private boolean                                               reconnectSupported;

    public boolean isReconnectSupported() {
        return reconnectSupported;
    }

    public void setReconnectSupported(boolean reconnectSupported) {
        this.reconnectSupported = reconnectSupported;
    }

    public AbstractProxySelectorImpl() {
    }

    public boolean add(final SingleDownloadController singleDownloadController) {
        return singleDownloadController != null && activeSingleDownloadControllers.add(singleDownloadController);
    }

    public boolean remove(final SingleDownloadController singleDownloadController) {
        return singleDownloadController != null && activeSingleDownloadControllers.remove(singleDownloadController);
    }

    public Set<SingleDownloadController> getSingleDownloadControllers() {
        return activeSingleDownloadControllers;
    }

    public void setResumeAllowed(boolean b) {
        resumeIsAllowed = b;
    }

    public boolean isResumeAllowed() {
        return this.isLocal() || resumeIsAllowed;
    }

    abstract protected boolean isLocal();

    protected final CopyOnWriteArrayList<ConnectionBan> banList = new CopyOnWriteArrayList<ConnectionBan>();

    public List<ConnectionBan> getBanList() {
        return Collections.unmodifiableList(banList);
    }

    public void addSessionBan(final ConnectionBan newBan) {
        if (newBan != null) {
            boolean addFlag = true;
            for (final ConnectionBan oldBan : banList) {
                if (oldBan.isExpired()) {
                    banList.remove(oldBan);
                } else if (oldBan.canSwallow(newBan)) {
                    addFlag = false;
                } else if (newBan.canSwallow(oldBan)) {
                    banList.remove(oldBan);
                }
            }
            if (addFlag && banList.addIfAbsent(newBan)) {
                LogController.CL().severe(newBan.toString());
            }
        }
    }

    public boolean isProxyBannedFor(final HTTPProxy orgReference, final URL url, final Plugin pluginFromThread, final boolean ignoreConnectBans) {
        for (final ConnectionBan ban : banList) {
            if (ban.isExpired()) {
                banList.remove(ban);
            } else if (ban.isProxyBannedByUrlOrPlugin(orgReference, url, pluginFromThread, ignoreConnectBans)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSelectorBannedFor(final Plugin pluginForHost, final boolean ignoreConnectBans) {
        for (final ConnectionBan ban : banList) {
            if (ban.isExpired()) {
                banList.remove(ban);
            } else if (ban.isSelectorBannedByPlugin(pluginForHost, ignoreConnectBans)) {
                return true;
            }
        }
        return false;
    }

    public void addSelectProxyByUrlHook(SelectProxyByURLHook selectProxyByUrlHook) {
        if (selectProxyByUrlHook != null) {
            selectProxyByURLHooks.addIfAbsent(selectProxyByUrlHook);
        }
    }

    public void removeSelectProxyByUrlHook(SelectProxyByURLHook selectProxyByUrlHook) {
        if (selectProxyByUrlHook != null) {
            selectProxyByURLHooks.remove(selectProxyByUrlHook);
        }
    }
}
