package org.jdownloader.statistics;

import org.appwork.storage.Storable;

public class LogDetails implements Storable {
    private String logID;
    private String id;
    private String cls;
    private String host;
    private String type;

    public String getCls() {
        return cls;
    }

    public void setCls(String cls) {
        this.cls = cls;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogID() {
        return logID;
    }

    public void setLogID(String stacktrace) {
        this.logID = stacktrace;
    }

    public LogDetails(/* storable */) {
    }

    public LogDetails(String logID, String errorID, String cls, String host, String type) {
        this.logID = logID;
        this.id = errorID;
        this.cls = cls;
        this.host = host;
        this.type = type;
    }
}
