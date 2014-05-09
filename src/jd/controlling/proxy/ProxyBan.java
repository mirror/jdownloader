package jd.controlling.proxy;

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

    private long   until;
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProxyBan(String bannedDomain, long l, String explain) {
        this.domain = bannedDomain;
        this.until = l;
        this.description = explain;
    }

}
