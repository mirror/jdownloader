package jd.plugins.optional.jdpremserv.model;

public class TrafficLog {

    private String domain;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public long getTraffic() {
        return traffic;
    }

    public void setTraffic(long traffic) {
        this.traffic = traffic;
    }

    private long traffic;
    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public TrafficLog(String domain, long traffic) {
        this.domain = domain;
        this.traffic = traffic;
        this.timestamp = System.currentTimeMillis();
    }

    @SuppressWarnings("unused")
    private TrafficLog() {
    }

}
