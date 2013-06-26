package org.jdownloader.api;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;

public class TypeNotFoundException extends BasicRemoteAPIException {

    public TypeNotFoundException() {
        super("Requested Type not found!", ResponseCode.ERROR_BAD_REQUEST);
    }

}
