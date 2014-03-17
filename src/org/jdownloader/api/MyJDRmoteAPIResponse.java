package org.jdownloader.api;

import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.net.httpserver.responses.HttpResponse;

public class MyJDRmoteAPIResponse extends RemoteAPIResponse {
    
    public MyJDRmoteAPIResponse(HttpResponse response, RemoteAPI remoteAPI) {
        super(response, remoteAPI);
    }
    
}
