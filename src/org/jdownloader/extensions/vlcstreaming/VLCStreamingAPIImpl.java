package org.jdownloader.extensions.vlcstreaming;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import jd.parser.Regex;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.Input2OutputStreamForwarder;
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

    @Override
    public void video(RemoteAPIRequest request, RemoteAPIResponse response, String format) {

        try {
            // seeking
            // String dlnaFeatures = "DLNA.ORG_PN=AVC_MP4_HP_HD_AAC;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01500000000000000000000000000000";
            String dlnaFeatures = "DLNA.ORG_PN=AVC_MP4_HP_HD_AAC;DLNA.ORG_OP=00;DLNA.ORG_FLAGS=00900000000000000000000000000000";
            File fileToServe = new File("g:\\test." + format);

            long cl;
            switch (request.getRequestType()) {
            case HEAD:
                System.out.println("HEAD " + request.getRequestHeaders());
                response.setResponseCode(ResponseCode.SUCCESS_OK);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, fileToServe.length() + ""));
                response.getResponseHeaders().add(new HTTPHeader("ContentFeatures.DLNA.ORG", dlnaFeatures));
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "video/mp4"));
                response.getResponseHeaders().add(new HTTPHeader("TransferMode.DLNA.ORG", "Streaming"));
                response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "bytes"));
                response.getOutputStream();
                response.closeConnection();
                return;
            case GET:
                System.out.println("GET " + request.getRequestHeaders());

                final HTTPHeader rangeRequest = request.getRequestHeaders().get("Range");
                long startPosition = 0;
                long stopPosition = -1;
                if (rangeRequest != null) {
                    String start = new Regex(rangeRequest.getValue(), "(\\d+).*?-").getMatch(0);
                    String stop = new Regex(rangeRequest.getValue(), "-.*?(\\d+)").getMatch(0);
                    if (start != null) startPosition = Long.parseLong(start);
                    if (stop != null) stopPosition = Long.parseLong(stop);
                }
                if (stopPosition <= 0) stopPosition = fileToServe.length() - 1;

                cl = (stopPosition - startPosition) + 1;
                if (startPosition > 5000 && true) {
                    // response.setResponseCode(ResponseCode.ERROR_RANGE_NOT_SUPPORTED);
                    // System.out.println("NOT Supported");
                    // response.closeConnection();
                    // return;

                    // response.setResponseCode(ResponseCode.ERROR_NOT_FOUND);
                    System.out.println("BAD");
                    // response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, 0 + ""));
                    // response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "video/mp4"));
                    //
                    // // 01500000000000000000000000000000
                    // response.getResponseHeaders().add(new HTTPHeader("ContentFeatures.DLNA.ORG", dlnaFeatures));
                    // response.getResponseHeaders().add(new HTTPHeader("TransferMode.DLNA.ORG", "Streaming"));
                    // // response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + startPosition + "-" + (startPosition)
                    // +
                    // // "/" + fileToServe.length()));
                    // response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "bytes"));

                    // response.getOutputStream();
                    // startPosition = 0;
                    // response.closeConnection();
                    // return;
                }

                response.setResponseCode(ResponseCode.SUCCESS_PARTIAL_CONTENT);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, cl + ""));
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "video/mp4"));

                // 01500000000000000000000000000000
                response.getResponseHeaders().add(new HTTPHeader("ContentFeatures.DLNA.ORG", dlnaFeatures));
                response.getResponseHeaders().add(new HTTPHeader("TransferMode.DLNA.ORG", "Streaming"));
                response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + startPosition + "-" + (stopPosition) + "/" + fileToServe.length()));
                response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "bytes"));

                FileInputStream ins = new FileInputStream(fileToServe);
                ins.skip(startPosition);
                Input2OutputStreamForwarder forwarder = new Input2OutputStreamForwarder(ins, response.getOutputStream(), 1024);
                forwarder.forward(null);
                System.out.println("Served");
                // response.getOutputStream();
                response.closeConnection();
            }
        } catch (final Throwable e) {
            if (e instanceof RemoteAPIException) throw (RemoteAPIException) e;
            throw new RemoteAPIException(e);
        }
    }
}
