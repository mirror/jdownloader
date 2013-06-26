package org.jdownloader.api;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;

public class InvalidIdException extends BasicRemoteAPIException {

    public InvalidIdException(long max) {
        super("Invalid ID: " + max, ResponseCode.ERROR_BAD_REQUEST);

    }

}
