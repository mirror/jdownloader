package org.jdownloader.api;

import java.io.IOException;

import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.net.httpserver.responses.HttpResponse;

public class MyJDRmoteAPIResponse extends RemoteAPIResponse {

    public MyJDRmoteAPIResponse(HttpResponse response, RemoteAPI remoteAPI) {
        super(response, remoteAPI);
    }

    @Override
    public void sendBytes(boolean gzip, boolean chunked, byte[] bytes) throws IOException {
        super.sendBytes(gzip, chunked, bytes);
    }
}
