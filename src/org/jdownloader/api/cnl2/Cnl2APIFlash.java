package org.jdownloader.api.cnl2;

import org.appwork.remoteapi.ApiMethodName;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiRawMethod;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("flash")
public interface Cnl2APIFlash extends RemoteAPIInterface {
    @ApiMethodName("addcrypted2")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this is the addcrypted2 function of the cnl2 interface
     * @param response
     * @param request
     */
    public void addcrypted2(RemoteAPIResponse response, RemoteAPIRequest request);

}
