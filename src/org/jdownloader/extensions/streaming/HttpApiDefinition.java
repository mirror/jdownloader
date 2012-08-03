package org.jdownloader.extensions.streaming;

import org.appwork.remoteapi.ApiMethodName;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiRawMethod;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("vlcstreaming")
public interface HttpApiDefinition extends RemoteAPIInterface {

    @ApiMethodName("play")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    public void play(RemoteAPIResponse response, RemoteAPIRequest request);

    @ApiMethodName("video")
    @ApiRawMethod(/* this method does not use json, it uses raw parameters */)
    public void video(RemoteAPIRequest request, RemoteAPIResponse response, String format);
}
