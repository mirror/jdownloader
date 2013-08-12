package jd.controlling.downloadcontroller;

import jd.controlling.proxy.ProxyInfo;
import jd.plugins.Account;
import jd.plugins.DownloadLink;

public class SingleDownloadControllerActivator {
    protected final boolean      byPassSimultanDownloadNum;
    protected final DownloadLink link;

    public boolean isByPassSimultanDownloadNum() {
        return byPassSimultanDownloadNum;
    }

    public DownloadLink getLink() {
        return link;
    }

    public ProxyInfo getProxy() {
        return proxy;
    }

    public Account getAccount() {
        return account;
    }

    protected final ProxyInfo proxy;
    protected final Account   account;

    public SingleDownloadControllerActivator(DownloadLink link, Account account, ProxyInfo proxy, boolean byPassSimultanDownloadNum) {
        this.link = link;
        this.account = account;
        this.proxy = proxy;
        this.byPassSimultanDownloadNum = byPassSimultanDownloadNum;
    }

    public String toString() {
        return "Proxy: " + proxy + ", Account " + account + " link: " + link + " bypasssim: " + byPassSimultanDownloadNum;
    }
}
