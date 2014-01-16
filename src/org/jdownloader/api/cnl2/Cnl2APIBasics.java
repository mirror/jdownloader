package org.jdownloader.api.cnl2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiMethodName;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiRawMethod;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("")
public interface Cnl2APIBasics extends RemoteAPIInterface, FlashGotAPI {

    @ApiMethodName("crossdomain.xml")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this method returns the needed crossdomain.xml file for the flash stuff
     * @param response
     */
    public void crossdomainxml(RemoteAPIResponse response) throws InternalApiException;

    @ApiMethodName("jdcheck.js")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this method sets a js variable to signal that jdownloader is running
     * @param response
     */
    public void jdcheckjs(RemoteAPIResponse response) throws InternalApiException;

    @ApiMethodName("jdcheckjson")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this method returns a json object containing information about the JD {version: #JD_VERSION#, deviceId: #MY_JD_DEVICE_ID}
     * @param response
     */
    public void jdcheckjson(RemoteAPIResponse response) throws InternalApiException;

    @ApiMethodName("flash")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this function returns "JDownloader" and has no function name /flash
     * @param response
     * @param request
     */
    public void alive(RemoteAPIResponse response, RemoteAPIRequest request) throws InternalApiException;

    @ApiMethodName("favicon.ico")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this returns the jdownloader logo as favicon
     * @param response
     */
    public void favicon(RemoteAPIResponse response) throws InternalApiException;
}
