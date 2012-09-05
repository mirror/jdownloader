package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.Date;

import org.appwork.storage.Storable;

public class StreamError implements Storable {
    public static enum ErrorCode {
        LINK_OFFLINE

    }

    private String description;

    public String getDescription() {
        return description;
    }

    public String toString() {
        return "[" + code + "] " + description + " (" + new Date(timestamp) + ")";

    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ErrorCode getCode() {
        return code;
    }

    public void setCode(ErrorCode code) {
        this.code = code;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private ErrorCode code;
    private long      timestamp;

    public StreamError(ErrorCode linkOffline) {
        this.code = linkOffline;
        timestamp = System.currentTimeMillis();

    }

    private StreamError(/* Storable */) {
        timestamp = System.currentTimeMillis();
    }
}
