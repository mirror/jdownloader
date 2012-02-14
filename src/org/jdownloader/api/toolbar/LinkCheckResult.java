package org.jdownloader.api.toolbar;

import java.util.ArrayList;

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
    protected ArrayList<LinkStatus> links  = null;

    public ArrayList<LinkStatus> getLinks() {
        return links;
    }

    public void setLinks(ArrayList<LinkStatus> links) {
        this.links = links;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

}
