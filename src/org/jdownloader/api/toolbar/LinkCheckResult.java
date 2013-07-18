package org.jdownloader.api.toolbar;

import org.appwork.storage.Storable;

public class LinkCheckResult implements Storable {

    public static enum STATUS {
        NA,
        PENDING,
        FINISHED
    }

    public LinkCheckResult(/* Storable */) {
    }

    protected STATUS                status = STATUS.NA;
    protected java.util.List<LinkStatus> links  = null;

    public java.util.List<LinkStatus> getLinks() {
        return links;
    }

    public void setLinks(java.util.List<LinkStatus> links) {
        this.links = links;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

}
