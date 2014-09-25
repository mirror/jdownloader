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
import org.jdownloader.updatev2.FilterList;
import org.jdownloader.updatev2.ProxyData;

public abstract class AbstractProxySelectorImpl implements ProxySelectorInterface {
    public static enum Type {
        NONE,
        DIRECT,
        SOCKS4,
        SOCKS5,
        HTTP,
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

    public boolean isAllowedByFilter(String host, Account acc) {
        if (filter == null) {
            return true;
        }
        return filter.validate(host, acc == null ? null : acc.getUser());
    }

    public void setFilter(FilterList filter) {
        this.filter = filter;
    }

    public ProxyData toProxyData() {
        ProxyData ret = new ProxyData();
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
    protected final CopyOnWriteArrayList<SelectProxyByUrlHook>    selectProxyByUrlHooks           = new CopyOnWriteArrayList<SelectProxyByUrlHook>();

    public boolean isReconnectSupported() {
        return Type.NONE.equals(getType());
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

    public void addSessionBan(ConnectionBan ban) {
        for (ConnectionBan b : banList) {
            if (b.isExpired()) {
                banList.remove(b);
                continue;
            }
            if (b.canSwallow(ban)) {
                return;
            }
            if (ban.canSwallow(b)) {
                banList.remove(b);
                continue;
            }
        }
        banList.add(ban);
    }

    public boolean isProxyBannedFor(final HTTPProxy orgReference, final URL url, final Plugin pluginFromThread, final boolean ignoreConnectBans) {
        boolean banned = false;
        for (ConnectionBan b : banList) {
            if (b.isExpired()) {
                banList.remove(b);
            } else if (banned == false && b.isProxyBannedByUrlOrPlugin(orgReference, url, pluginFromThread, ignoreConnectBans)) {
                banned = true;
            }
        }
        return banned;
    }

    public boolean isSelectorBannedFor(final Plugin pluginForHost, final boolean ignoreConnectBans) {
        boolean banned = false;
        for (ConnectionBan b : banList) {
            if (b.isExpired()) {
                banList.remove(b);
            } else if (banned == false && b.isSelectorBannedByPlugin(pluginForHost, ignoreConnectBans)) {
                banned = true;
            }
        }
        return banned;
    }

    public void addSelectProxyByUrlHook(SelectProxyByUrlHook selectProxyByUrlHook) {
        if (selectProxyByUrlHook != null) {
            selectProxyByUrlHooks.addIfAbsent(selectProxyByUrlHook);
        }

    }

    public void removeSelectProxyByUrlHook(SelectProxyByUrlHook hook) {
        if (hook != null) {
            selectProxyByUrlHooks.remove(hook);
        }
    }
}
