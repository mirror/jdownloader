package org.jdownloader.extensions.vlcstreaming;

import org.appwork.remoteapi.ApiMethodName;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiRawMethod;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("vlcstreaming")
public interface VLCStreamingAPI extends RemoteAPIInterface {

    @ApiMethodName("play")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    public void play(RemoteAPIResponse response, RemoteAPIRequest request);
}
