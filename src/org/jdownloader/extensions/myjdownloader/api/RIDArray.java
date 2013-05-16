package org.jdownloader.extensions.myjdownloader.api;

import java.util.ArrayList;

public class RIDArray extends ArrayList<RIDEntry> {

    private long minAcceptedRID = Long.MIN_VALUE;

    public long getMinAcceptedRID() {
        return minAcceptedRID;
    }

    public void setMinAcceptedRID(long minAcceptedRID) {
        this.minAcceptedRID = minAcceptedRID;
    }

}
