package org.jdownloader.api.cnl2;

import org.appwork.remoteapi.ApiMethodName;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("")
public interface Cnl2APIBasics extends RemoteAPIInterface {

    @ApiMethodName("crossdomain.xml")
    /**
     * this method returns the needed crossdomain.xml file for the flash stuff
     * @param response
     */
    public void crossdomainxml(RemoteAPIResponse response);

    @ApiMethodName("jdcheck.js")
    /**
     * this version sets a js variable to signal that jdownloader is running
     * @param response
     */
    public void jdcheckjs(RemoteAPIResponse response);
}
