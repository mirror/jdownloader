package org.jdownloader.api;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;

public class BadOrderException extends BasicRemoteAPIException {

    private long latestId;

    public long getLatestId() {
        return latestId;
    }

    public void setLatestId(long latestId) {
        this.latestId = latestId;
    }

    public BadOrderException(long max) {
        super("Answer Dialog " + max + " first", ResponseCode.ERROR_BAD_REQUEST);
        this.latestId = max;
    }

}
