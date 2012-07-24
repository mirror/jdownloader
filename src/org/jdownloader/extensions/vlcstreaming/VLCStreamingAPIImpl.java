package org.jdownloader.extensions.vlcstreaming;

import java.util.HashMap;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.net.httpserver.requests.HttpRequest;

public class VLCStreamingAPIImpl implements VLCStreamingAPI {

    private HashMap<String, StreamingInterface> interfaceMap = new HashMap<String, StreamingInterface>();

    public VLCStreamingAPIImpl(VLCStreamingExtension extension) {
    }

    @Override
    public void play(RemoteAPIResponse response, RemoteAPIRequest request) {
        try {
            StreamingInterface streamingInterface = null;
            String id = HttpRequest.getParameterbyKey(request, "id");
            if (id != null) {
                streamingInterface = interfaceMap.get(id);
            }
            if (streamingInterface != null) {
                response.setResponseAsync(true);
                new VLCStreamingThread(response, request, streamingInterface).start();
            } else {
                response.setResponseCode(ResponseCode.ERROR_NOT_FOUND);
                response.getOutputStream();
                response.closeConnection();
            }
        } catch (final Throwable e) {
            if (e instanceof RemoteAPIException) throw (RemoteAPIException) e;
            throw new RemoteAPIException(e);
        }
    }

    public void addHandler(String name, StreamingInterface rarStreamer) {
        interfaceMap.put(name, rarStreamer);
    }

    public StreamingInterface getStreamingInterface(String name) {
        return interfaceMap.get(name);
    }
}
