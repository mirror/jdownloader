package org.jdownloader.api.cnl2;

import org.appwork.remoteapi.ApiMethodName;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiRawMethod;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("flash")
public interface Cnl2APIFlash extends RemoteAPIInterface {
    @ApiMethodName("addcrypted2")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this is the addcrypted2 function of the cnl2 interface
     * @param response
     * @param request
     */
    public void addcrypted2(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

    @ApiMethodName("add")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this is the normal add function of the cnl2 interface
     * @param response
     * @param request
     */
    public void add(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

    @ApiMethodName("addcrypted")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this is the normal addcrypted(dlc) function of the cnl2 interface
     * @param response
     * @param request
     */
    public void addcrypted(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

    @ApiMethodName("")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this function returns "JDownloader" and has no function name /flash/
     * @param response
     * @param request
     */
    public void alive(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

}
