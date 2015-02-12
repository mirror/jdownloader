package org.jdownloader.jdserv;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("RedirectInterface")
public interface RedirectInterface extends RemoteAPIInterface {
    //

    // public static CounterInterface INST =
    // JD_SERV_CONSTANTS.create(CounterInterface.class);
    void banner(RemoteAPIRequest request, RemoteAPIResponse response, String source, String md5, String lng, boolean hasUploaded, boolean hasOthers);

    void ul(RemoteAPIRequest request, RemoteAPIResponse response, String source, String sig, String uid, String pid, boolean hasUploaded, boolean hasOthers);

    void redirect(String url, RemoteAPIResponse response);

    void banner(RemoteAPIRequest request, RemoteAPIResponse response, String md5, String sig, String uid, String pid, String source, String lng, boolean hasUploaded, boolean hasOthers);

}
