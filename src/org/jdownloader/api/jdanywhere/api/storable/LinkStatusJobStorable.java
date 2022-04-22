package org.jdownloader.api.jdanywhere.api.storable;

import org.appwork.storage.Storable;

public class LinkStatusJobStorable implements Storable {
    public LinkStatusJobStorable() {
    }

    private String  linkID;
    private int     status;
    private String  statusText;
    private boolean finished;   ;
    private boolean progress;

    public String getLinkID() {
        return linkID;
    }

    public void setLinkID(String linkID) {
        this.linkID = linkID;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean isFinished) {
        this.finished = isFinished;
    }

    public boolean isInProgress() {
        return progress;
    }

    public void setInProgress(boolean inProgress) {
        this.progress = inProgress;
    }
}
