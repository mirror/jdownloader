package org.jdownloader.statistics;

import org.jdownloader.myjdownloader.client.json.AbstractJsonData;

public abstract class AbstractLogEntry extends AbstractJsonData {
    private String isp = null;

    public String getIsp() {
        return isp;
    }

    private long buildTime;

    public long getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(long parseLong) {
        this.buildTime = parseLong;
    }

    private String country = null;
    private String os      = "WINDOWS";

    public String getOs() {
        return os;
    }

    private int utcOffset = 0;

    public int getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(int utcOffset) {
        this.utcOffset = utcOffset;
    }

    public void setOs(String os) {
        this.os = os;
    }

    private long timestamp = 0;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private long sessionStart = 0;

    public long getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(long sessionStart) {
        this.sessionStart = sessionStart;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

}
