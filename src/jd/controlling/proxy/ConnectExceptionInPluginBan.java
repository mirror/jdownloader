package jd.controlling.proxy;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ConnectExceptionInPluginBan extends AbstractBasicBan {

    private DownloadLinkCandidate     candidate;
    private AbstractProxySelectorImpl selector;
    private HTTPProxy                 proxy;
    private long                      created;

    public ConnectExceptionInPluginBan(DownloadLinkCandidate candidate, AbstractProxySelectorImpl proxySelector, HTTPProxy proxy) {
        super(candidate.getCachedAccount().getPlugin(), proxySelector, proxy);
        created = System.currentTimeMillis();
        this.candidate = candidate;
        this.selector = proxySelector;
        this.proxy = proxy;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() - created > 15 * 60 * 1000l;
    }
    // @Override
    // public boolean isSelectorBannedByPlugin(Plugin candidate) {
    // if (plugin == null) {
    // // if plugin is null, all plugins are affected
    // return true;
    // }
    //
    // return pluginsEquals(candidate, plugin);
    // }
    //
    // @Override
    // public boolean isProxyBannedByUrlOrPlugin(HTTPProxy orgReference, URL url, Plugin pluginFromThread) {
    // if (pluginFromThread != null && plugin == null) {
    // // //all plugins are affected
    //
    // return true;
    // }
    // if (pluginsEquals(plugin, pluginFromThread)) {
    // return true;
    // }
    // if (plugin != null && pluginFromThread == null) {
    // // this is a plugin ban, but the requesting connection is not plugin related
    // return false;
    // }
    // return false;
    // }
}
