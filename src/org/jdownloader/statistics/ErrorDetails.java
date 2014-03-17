package org.jdownloader.statistics;

import org.appwork.storage.Storable;

public class ErrorDetails implements Storable {

    private String stacktrace;
    private String id;
    private long   buildTime = 0;

    public long getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(long buildTime) {
        this.buildTime = buildTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }

    public ErrorDetails(/* storable */) {

    }

    public ErrorDetails(String errorID) {

        this.id = errorID;
    }

}
