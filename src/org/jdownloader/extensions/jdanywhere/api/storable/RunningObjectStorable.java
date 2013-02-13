package org.jdownloader.extensions.jdanywhere.api.storable;

import org.appwork.storage.Storable;

public class RunningObjectStorable implements Storable {

    private long packageID;
    private long linkID;
    private long speed;
    private long done;

    public long getPackageID() {
        return packageID;
    }

    public void setPackageID(long packageID) {
        this.packageID = packageID;
    }

    /**
     * @return the linkID
     */
    public long getLinkID() {
        return linkID;
    }

    /**
     * @param linkID
     *            the linkID to set
     */
    public void setLinkID(long linkID) {
        this.linkID = linkID;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public long getDone() {
        return done;
    }

    public void setDone(long done) {
        this.done = done;
    }

}
