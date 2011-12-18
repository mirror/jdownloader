package org.jdownloader.api.cnl2;

import org.appwork.remoteapi.ApiMethodName;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiRawMethod;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("")
public interface Cnl2APIBasics extends RemoteAPIInterface {

    @ApiMethodName("crossdomain.xml")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this method returns the needed crossdomain.xml file for the flash stuff
     * @param response
     */
    public void crossdomainxml(RemoteAPIResponse response);

    @ApiMethodName("jdcheck.js")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this method sets a js variable to signal that jdownloader is running
     * @param response
     */
    public void jdcheckjs(RemoteAPIResponse response);

    @ApiMethodName("flash")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this function returns "JDownloader" and has no function name /flash
     * @param response
     * @param request
     */
    public void alive(RemoteAPIResponse response, RemoteAPIRequest request);

    @ApiMethodName("favicon.ico")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    /**
     * this returns the jdownloader logo as favicon
     * @param response
     */
    public void favicon(RemoteAPIResponse response);

}
