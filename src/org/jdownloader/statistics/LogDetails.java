package org.jdownloader.statistics;

import org.appwork.storage.Storable;

public class LogDetails implements Storable {

    private String logID;
    private String id;

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

    public LogDetails(String logID, String errorID) {
        this.logID = logID;
        this.id = errorID;
    }

}
