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
import org.appwork.utils.Files;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.Input2OutputStreamForwarder;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.jdownloader.extensions.vlcstreaming.upnp.DLNAOp;
import org.jdownloader.extensions.vlcstreaming.upnp.DLNAOrg;

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

    public static final int DLNA_ORG_FLAG_SENDER_PACED               = (1 << 31);
    public static final int DLNA_ORG_FLAG_TIME_BASED_SEEK            = (1 << 30);
    public static final int DLNA_ORG_FLAG_BYTE_BASED_SEEK            = (1 << 29);
    public static final int DLNA_ORG_FLAG_PLAY_CONTAINER             = (1 << 28);
    public static final int DLNA_ORG_FLAG_S0_INCREASE                = (1 << 27);
    public static final int DLNA_ORG_FLAG_SN_INCREASE                = (1 << 26);
    public static final int DLNA_ORG_FLAG_RTSP_PAUSE                 = (1 << 25);
    public static final int DLNA_ORG_FLAG_STREAMING_TRANSFER_MODE    = (1 << 24);
    public static final int DLNA_ORG_FLAG_INTERACTIVE_TRANSFERT_MODE = (1 << 23);
    public static final int DLNA_ORG_FLAG_BACKGROUND_TRANSFERT_MODE  = (1 << 22);
    public static final int DLNA_ORG_FLAG_CONNECTION_STALL           = (1 << 21);
    public static final int DLNA_ORG_FLAG_DLNA_V15                   = (1 << 20);

    @Override
    public void video(RemoteAPIRequest request, RemoteAPIResponse response, String path) {

        try {
            File fileToServe = new File(path);
            System.out.println(path);
            String format = Files.getExtension(fileToServe.getName());
            // seeking
            String dlnaFeatures = "DLNA.ORG_PN=" + format + ";DLNA.ORG_OP=" + DLNAOp.create(DLNAOp.RANGE_SEEK_SUPPORTED) + ";DLNA.ORG_FLAGS=" + DLNAOrg.create(DLNAOrg.STREAMING_TRANSFER_MODE);

            String ct = "video/" + format;
            if (format.equals("mp3")) {
                ct = "audio/mpeg";
                dlnaFeatures = "DLNA.ORG_PN=MP3";
            }
            if (format.equals("flac")) {
                ct = "audio/flac";
                dlnaFeatures = null;
            }

            if (format.equals("mkv")) {
                ct = "video/x-mkv";
                // dlnaFeatures = null;

            }
            long cl;

            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, ct));

            if (dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader("ContentFeatures.DLNA.ORG", dlnaFeatures));
            response.getResponseHeaders().add(new HTTPHeader("TransferMode.DLNA.ORG", "Streaming"));
            response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "bytes"));
            switch (request.getRequestType()) {
            case HEAD:
                System.out.println("HEAD " + request.getRequestHeaders());
                response.setResponseCode(ResponseCode.SUCCESS_OK);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, fileToServe.length() + ""));
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

                response.setResponseCode(ResponseCode.SUCCESS_PARTIAL_CONTENT);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, cl + ""));

                response.getResponseHeaders().add(new HTTPHeader("Content-Range", "bytes " + startPosition + "-" + (stopPosition) + "/" + fileToServe.length()));

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
        } finally {
            System.out.println("Resp: " + response.getResponseHeaders().toString());
        }
    }

    private String createFlags() {
        return null;
    }
}
