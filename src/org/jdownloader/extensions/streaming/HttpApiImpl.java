package org.jdownloader.extensions.streaming;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.HashMap;

import jd.parser.Regex;
import jd.plugins.DownloadLink;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.Input2OutputStreamForwarder;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.streaming.dataprovider.PipeStreamingInterface;
import org.jdownloader.extensions.streaming.dataprovider.rar.PartFileDataProvider;
import org.jdownloader.extensions.streaming.dataprovider.rar.RarArchiveDataProvider;
import org.jdownloader.extensions.streaming.rarstream.RarStreamer;
import org.jdownloader.extensions.streaming.upnp.DLNAOp;
import org.jdownloader.extensions.streaming.upnp.DLNAOrg;
import org.jdownloader.extensions.streaming.upnp.MediaServer;
import org.jdownloader.extensions.streaming.upnp.PlayToUpnpRendererDevice;
import org.jdownloader.logging.LogController;

public class HttpApiImpl implements HttpApiDefinition {

    private HashMap<String, StreamingInterface>   interfaceMap   = new HashMap<String, StreamingInterface>();
    private StreamingExtension                    extension;
    private LogSource                             logger;
    private MediaServer                           mediaServer;
    private static final PlayToUpnpRendererDevice UNKNOWN_DEVICE = new UnknownUPNPDevice();

    public HttpApiImpl(StreamingExtension extension, MediaServer mediaServer) {
        this.extension = extension;
        this.mediaServer = mediaServer;
        logger = LogController.getInstance().getLogger(getClass().getName());
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
                new StreamingThread(response, request, streamingInterface).start();
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

    @Override
    public void streamByLinkID(RemoteAPIResponse response, RemoteAPIRequest request, String id, String deviceID) {

        try {

            String subpath = "";
            // request.getRequestHeaders().get(")

            final HTTPHeader rangeRequest = request.getRequestHeaders().get("Range");
            long startPosition = 0;
            long stopPosition = -1;
            if (rangeRequest != null) {
                String start = new Regex(rangeRequest.getValue(), "(\\d+).*?-").getMatch(0);
                String stop = new Regex(rangeRequest.getValue(), "-.*?(\\d+)").getMatch(0);
                if (start != null) startPosition = Long.parseLong(start);
                if (stop != null) stopPosition = Long.parseLong(stop);
            }

            if (startPosition > 0) {
                response.setResponseCode(ResponseCode.ERROR_RANGE_NOT_SUPPORTED);
                return;
            }

            id = URLDecoder.decode(id, "UTF-8");
            int i = id.indexOf("/");
            if (i > 0) {
                subpath = id.substring(i);
                id = id.substring(0, i);
            }
            while (subpath.startsWith("/") || subpath.startsWith("\\")) {
                subpath = subpath.substring(1);
            }

            DownloadLink dlink = extension.getLinkById(id);

            PlayToUpnpRendererDevice callingDevice = UNKNOWN_DEVICE;
            for (PlayToUpnpRendererDevice dev : mediaServer.getPlayToRenderer()) {
                if (request.getRemoteAdress().contains(dev.getAddress())) {
                    callingDevice = dev;
                    break;
                }
            }
            logger.info("Call from " + callingDevice);

            final DownloadLink link = dlink;
            StreamingInterface streamingInterface = null;
            streamingInterface = interfaceMap.get(id);
            boolean archiveIsOpen = true;
            if (streamingInterface == null) {
                ExtractionExtension archiver = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();

                DownloadLinkArchiveFactory fac = new DownloadLinkArchiveFactory(dlink);
                Archive archive = archiver.getArchiveByFactory(fac);
                if (archive != null) {

                    streamingInterface = new PipeStreamingInterface(null, new RarArchiveDataProvider(archive, subpath, new PartFileDataProvider(extension.getDownloadLinkDataProvider())));
                    // Thread.sleep(5000);
                    addHandler(id, streamingInterface);

                } else {

                    streamingInterface = new PipeStreamingInterface(dlink, extension.getDownloadLinkDataProvider());
                    addHandler(id, streamingInterface);

                }
            }
            System.out.println(dlink);

            String format = Files.getExtension(dlink.getName());
            if (!StringUtils.isEmpty(subpath)) {
                format = Files.getExtension(subpath);
            }
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
            if (format.equals("flv")) {
                ct = "video/x-flv";
                // dlnaFeatures = null;

            }
            if (format.equals("mkv")) {
                ct = "video/x-mkv";
                // dlnaFeatures = null;

            }
            if (callingDevice.getProtocolInfos() != null) {
                for (ProtocolInfo pi : callingDevice.getProtocolInfos()) {

                    if (pi.getContentFormatMimeType().getSubtype().contains(format)) {

                        System.out.println(1);
                        ct = pi.getContentFormat();
                        dlnaFeatures = pi.getAdditionalInfo();
                        break;
                    }
                }
            }

            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, ct));

            if (dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader("ContentFeatures.DLNA.ORG", dlnaFeatures));
            response.getResponseHeaders().add(new HTTPHeader("TransferMode.DLNA.ORG", "Streaming"));
            response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "none"));
            if (streamingInterface instanceof RarStreamer) {
                while (((RarStreamer) streamingInterface).getExtractionThread() == null && ((RarStreamer) streamingInterface).getException() == null) {
                    Thread.sleep(100);
                }

                if (((RarStreamer) streamingInterface).getException() != null) {
                    response.setResponseCode(ResponseCode.ERROR_BAD_REQUEST);
                    response.getOutputStream();
                    response.closeConnection();
                    return;

                }
            }
            long length;
            switch (request.getRequestType()) {
            case HEAD:
                System.out.println("HEAD " + request.getRequestHeaders());
                response.setResponseCode(ResponseCode.SUCCESS_OK);
                length = streamingInterface.getFinalFileSize();
                if (length > 0) response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, length + ""));

                response.getOutputStream();
                response.closeConnection();
                return;
            case GET:
                System.out.println("GET " + request.getRequestHeaders());

                try {

                    new StreamingThread(response, request, streamingInterface).run();

                } catch (final Throwable e) {
                    if (e instanceof RemoteAPIException) throw (RemoteAPIException) e;
                    throw new RemoteAPIException(e);
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
            if (e instanceof RemoteAPIException) {

            throw (RemoteAPIException) e; }
            throw new RemoteAPIException(e);
        } finally {
            System.out.println("Resp: " + response.getResponseHeaders().toString());
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
