package org.jdownloader.statistics;

import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;

import org.appwork.storage.Storable;

public class ErrorDetails implements Storable {

    private String stacktrace;
    private String id;

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

    public ErrorDetails(String errorID, DownloadLinkCandidateResult result) {
        this.stacktrace = result.getErrorID();
        this.id = errorID;
    }

}
