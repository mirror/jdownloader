package jd.controlling.downloadcontroller;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.proxy.ProxyInfo;
import jd.plugins.DownloadLink;

public class DownloadLinkCandidate {
    private final boolean       forced;
    private final DownloadLink  link;
    private final CachedAccount cachedAccount;
    private final ProxyInfo     proxy;
    private final boolean       customizedAccount;

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
        return "DownloadCandidate:" + link + "|Host " + link.getHost() + "|Account:" + cachedAccount + "|Proxy:" + proxy;
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

    public DownloadLinkCandidate(DownloadLink link, boolean forced, CachedAccount cachedAccount, ProxyInfo proxy, boolean customizedAccount) {
        this.link = link;
        this.forced = forced;
        this.cachedAccount = cachedAccount;
        this.proxy = proxy;
        this.customizedAccount = customizedAccount;
    }

    public DownloadLinkCandidate(DownloadLinkCandidate candidate, ProxyInfo proxy) {
        this(candidate.getLink(), candidate.isForced(), candidate.getCachedAccount(), proxy, candidate.isCustomizedAccount());
    }

    public boolean isCustomizedAccount() {
        return customizedAccount;
    }
}