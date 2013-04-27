package org.jdownloader.api.cnl2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiMethodName;
import org.appwork.remoteapi.annotations.ApiRawMethod;
import org.appwork.remoteapi.exceptions.InternalApiException;

public interface FlashGotAPI extends RemoteAPIInterface {
    @ApiMethodName("flashgot")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this method handles the flashgot communication
     * @param response
     */
    public void flashgot(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;
}
