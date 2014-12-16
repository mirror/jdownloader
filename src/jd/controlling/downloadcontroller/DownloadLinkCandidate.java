package jd.controlling.downloadcontroller;

import java.lang.ref.WeakReference;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.plugins.DownloadLink;

public class DownloadLinkCandidate {
    private final boolean                     forced;
    private final WeakReference<DownloadLink> link;
    private final CachedAccount               cachedAccount;
    private final AbstractProxySelectorImpl   proxySelector;
    private final boolean                     customizedAccount;

    public AbstractProxySelectorImpl getProxySelector() {
        return proxySelector;
    }

    public CachedAccount getCachedAccount() {
        return cachedAccount;
    }

    public DownloadLink getLink() {
        return link == null ? null : link.get();
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public String toString() {
        DownloadLink link = getLink();
        return "DownloadCandidate:" + link + "|Host " + (link == null ? null : link.getHost()) + "|Account:" + cachedAccount + "|Proxy:" + proxySelector;
    }

    public DownloadLinkCandidate(DownloadLink link, boolean forced) {
        this(link, forced, null, null, false);
    }

    public DownloadLinkCandidate(DownloadLinkCandidate candidate, CachedAccount cachedAccount) {
        this(candidate.getLink(), candidate.isForced(), cachedAccount, null, false);
    }

    public DownloadLinkCandidate(DownloadLinkCandidate candidate, CachedAccount cachedAccount, boolean customizedAccount) {
        this(candidate.getLink(), candidate.isForced(), cachedAccount, null, customizedAccount);
    }

    public DownloadLinkCandidate(DownloadLink link, boolean forced, CachedAccount cachedAccount, AbstractProxySelectorImpl proxy, boolean customizedAccount) {
        this.link = new WeakReference<DownloadLink>(link);
        this.forced = forced;
        this.cachedAccount = cachedAccount;
        this.proxySelector = proxy;
        this.customizedAccount = customizedAccount;
    }

    public DownloadLinkCandidate(DownloadLinkCandidate candidate, AbstractProxySelectorImpl proxy) {
        this(candidate.getLink(), candidate.isForced(), candidate.getCachedAccount(), proxy, candidate.isCustomizedAccount());
    }

    public boolean isCustomizedAccount() {
        return customizedAccount;
    }
}