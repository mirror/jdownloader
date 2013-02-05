package org.jdownloader.api.mobile.downloads;

import org.appwork.storage.Storable;

public class LinkStatusJob implements Storable {

    private String  linkID;
    private int     status;
    private String  statusText;
    private boolean isFinished;
    private boolean isActive;
    private boolean inProgress;

    /**
     * @return the linkID
     */
    public String getLinkID() {
        return linkID;
    }

    /**
     * @param linkID
     *            the linkID to set
     */
    public void setLinkID(String linkID) {
        this.linkID = linkID;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @return the statusText
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * @param statusText
     *            the statusText to set
     */
    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    /**
     * @return the isFinished
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * @param isFinished
     *            the isFinished to set
     */
    public void setFinished(boolean isFinished) {
        this.isFinished = isFinished;
    }

    /**
     * @return the isActive
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * @param isActive
     *            the isActive to set
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * @return the inProgress
     */
    public boolean isInProgress() {
        return inProgress;
    }

    /**
     * @param inProgress
     *            the inProgress to set
     */
    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

}
