package org.jdownloader.api;

public class RIDEntry {

    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getRid() {
        return rid;
    }

    public void setRid(long rid) {
        this.rid = rid;
    }

    private long rid;

    public RIDEntry(long rid) {
        this.rid = rid;
        this.timestamp = System.currentTimeMillis();
    }

}
