package org.jdownloader.jdserv;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("RedirectInterface")
public interface RedirectInterface extends RemoteAPIInterface {
    //

    // public static CounterInterface INST =
    // JD_SERV_CONSTANTS.create(CounterInterface.class);

    void redirect(String url, RemoteAPIResponse response);

}
