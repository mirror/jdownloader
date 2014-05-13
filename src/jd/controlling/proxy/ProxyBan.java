package jd.controlling.proxy;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class ProxyBan {

    private String domain;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public long getUntil() {
        return until;
    }

    public void setUntil(long until) {
        this.until = until;
    }

    private long      until;
    private String    description;
    private HTTPProxy proxy;

    public HTTPProxy getProxy() {
        return proxy;
    }

    public void setProxy(HTTPProxy proxy) {
        this.proxy = proxy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProxyBan(HTTPProxy usedProxy, String bannedDomain, long l, String explain) {
        this.proxy = usedProxy;
        this.domain = bannedDomain;
        this.until = l;
        this.description = explain;
    }

}
