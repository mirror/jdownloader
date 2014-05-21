package jd.controlling.proxy;

import java.net.URL;

import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class PluginRelatedConnectionBan extends AbstractBan {

    protected Plugin    plugin;

    protected HTTPProxy proxy;

    public PluginRelatedConnectionBan(Plugin plugin, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy) {
        super(proxySelector);
        this.plugin = plugin;

        this.proxy = proxy;
    }

    @Override
    public String toString() {
        return _JDT._.AuthExceptionGenericBan_toString_plugin(proxy.toString(), plugin.getHost());
    }

    // _JDT._.plugins_errors_proxy_connection()
    //
    // @Override
    // public boolean validate(AbstractProxySelectorImpl selector, HTTPProxy orgReference, URL url) {
    //
    // if (proxy != null) {
    // if (!proxy.equals(orgReference)) {
    // return false;
    // }
    // }
    // Thread thread = Thread.currentThread();
    // if (thread instanceof AccountCheckerThread) {
    //
    // AccountCheckJob job = ((AccountCheckerThread) thread).getJob();
    // if (job != null) {
    // Account account = job.getAccount();
    // PluginForHost plg = account.getPlugin();
    // if (plg != null) {
    // if (StringUtils.equalsIgnoreCase(plg.getHost(), plugin.getHost())) {
    // return true;
    // }
    //
    // }
    // return false;
    // }
    //
    // } else if (thread instanceof LinkCheckerThread) {
    // PluginForHost plg = ((LinkCheckerThread) thread).getPlugin();
    // if (plg != null) {
    // if (StringUtils.equalsIgnoreCase(plg.getHost(), plugin.getHost())) {
    //
    // }
    // return false;
    // }
    // } else if (thread instanceof SingleDownloadController) {
    // DownloadLinkCandidate candidate = ((SingleDownloadController) thread).getDownloadLinkCandidate();
    // return isSelectorBannedByPlugin(candidate.getCachedAccount().getPlugin());
    //
    // }
    //
    // return false;
    // }

    @Override
    public boolean isSelectorBannedByPlugin(Plugin candidate, boolean ignoreConnectBans) {

        // auth is always a ban reason
        return true;
    }

    @Override
    public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        // auth is always a ban reason
        return true;
    }

    // @Override
    // public boolean isSelectorBannedByUrl(URL url) {
    // return false;
    // }

    private boolean pluginsEquals(Plugin a, Plugin b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }

        return StringUtils.equalsIgnoreCase(a.getHost(), b.getHost());

    }

    // @Override
    // public boolean validate(ConnectionBan ban) {
    // if (ban instanceof PluginRelatedConnectionBan) {
    // PluginRelatedConnectionBan abb = (PluginRelatedConnectionBan) ban;
    // if (StringUtils.equalsIgnoreCase(abb.plugin.getHost(), plugin.getHost())) {
    // if (proxyEquals(abb.proxy, proxy)) {
    // return true;
    // }
    // }
    // }
    // return false;
    // }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public boolean canSwallow(ConnectionBan ban) {
        if (!(ban instanceof PluginRelatedConnectionBan)) {
            return false;
        }
        if (!proxyEquals(((PluginRelatedConnectionBan) ban).proxy, proxy)) {
            return false;
        }
        // actually not really required. of one plugin is banned, all are banned
        if (!pluginsEquals(((PluginRelatedConnectionBan) ban).plugin, plugin)) {
            return false;
        }
        return true;
    }
}
