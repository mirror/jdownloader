package jd.plugins.optional.jdpremserv.model;

import org.appwork.utils.formatter.SizeFormater;

public class PremServHoster implements Comparable<PremServHoster> {
    private String domain;
    private long traffic = -1;

    public PremServHoster(String domain, long traffic) {
        this.domain = domain.toLowerCase();
        this.traffic = traffic;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain.toLowerCase();
    }

    public long getTraffic() {
        return traffic;
    }

    public void setTraffic(long traffic) {
        this.traffic = traffic;
    }

    public String toString() {
        return domain + "(" + SizeFormater.formatBytes(traffic) + ")";
    }

    public int compareTo(PremServHoster o) {
        return domain.compareToIgnoreCase(o.domain);
    }
}
