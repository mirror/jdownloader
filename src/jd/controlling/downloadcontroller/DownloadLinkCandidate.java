package jd.controlling.downloadcontroller;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.proxy.ProxyInfo;
import jd.plugins.DownloadLink;

public class DownloadLinkCandidate {
    private final boolean       forced;
    private final DownloadLink  link;
    private final CachedAccount cachedAccount;
    private final ProxyInfo     proxy;

    public ProxyInfo getProxy() {
        return proxy;
    }

    public CachedAccount getCachedAccount() {
        return cachedAccount;
    }

    public DownloadLink getLink() {
        return link;
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public String toString() {
        return "DownloadCandidate: " + link + " from Host " + link.getHost() + " with Account " + cachedAccount + " over Proxy " + proxy;
    }

    public DownloadLinkCandidate(DownloadLink link, boolean forced) {
        this.link = link;
        this.forced = forced;
        this.cachedAccount = null;
        this.proxy = null;
    }

    public DownloadLinkCandidate(DownloadLinkCandidate candidate, CachedAccount cachedAccount) {
        this.link = candidate.link;
        this.forced = candidate.forced;
        this.cachedAccount = cachedAccount;
        this.proxy = null;
    }

    public DownloadLinkCandidate(DownloadLinkCandidate candidate, ProxyInfo proxy) {
        this.link = candidate.link;
        this.forced = candidate.forced;
        this.cachedAccount = candidate.cachedAccount;
        this.proxy = proxy;
    }
}