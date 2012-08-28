package org.jdownloader.extensions.streaming;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import jd.plugins.DownloadLink;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIException;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HeadRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.streaming.dataprovider.PipeStreamingInterface;
import org.jdownloader.extensions.streaming.dataprovider.rar.PartFileDataProvider;
import org.jdownloader.extensions.streaming.dataprovider.rar.RarArchiveDataProvider;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.image.JPEGImage;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaNode;
import org.jdownloader.extensions.streaming.rarstream.RarStreamer;
import org.jdownloader.extensions.streaming.upnp.DLNAOp;
import org.jdownloader.extensions.streaming.upnp.DLNAOrg;
import org.jdownloader.extensions.streaming.upnp.MediaServer;
import org.jdownloader.extensions.streaming.upnp.PlayToUpnpRendererDevice;
import org.jdownloader.logging.LogController;

public class HttpApiImpl implements HttpRequestHandler {

    private HashMap<String, StreamingInterface> interfaceMap = new HashMap<String, StreamingInterface>();
    private StreamingExtension                  extension;
    private LogSource                           logger;
    private MediaServer                         mediaServer;

    public HttpApiImpl(StreamingExtension extension, MediaServer mediaServer) {
        this.extension = extension;
        this.mediaServer = mediaServer;
        logger = LogController.getInstance().getLogger(getClass().getName());
        Profile.init();
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
    public boolean onGetRequest(GetRequest request, HttpResponse response) {

        String[] params = new Regex(request.getRequestedPath(), "^/stream/([^\\/]+)/([^\\/]+)/?(.*)$").getRow(0);
        if (params == null) return false;
        String deviceid = params[0];
        String id = params[1];
        String subpath = params[2];

        try {
            if (".albumart".equals(subpath)) {
                MediaItem item = (MediaItem) extension.getMediaArchiveController().getItemById(id);
                File path = Application.getResource(item.getThumbnailPath());

                String ct = JPEGImage.JPEG_LRG.getMimeType().getLabel();
                String dlnaFeatures = "DLNA.ORG_PN=" + JPEGImage.JPEG_TN.getProfileID();
                if (dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader("ContentFeatures.DLNA.ORG", dlnaFeatures));
                response.getResponseHeaders().add(new HTTPHeader("TransferMode.DLNA.ORG", "Streaming"));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(IconIO.getScaledInstance(ImageProvider.read(path), 160, 160), "jpeg", baos);
                baos.close();
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, baos.size() + ""));
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, ct));
                response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "bytes"));
                response.setResponseCode(ResponseCode.SUCCESS_OK);

                // JPegImage.JPEG_TN
                response.getOutputStream().write(baos.toByteArray());

                response.closeConnection();
                return true;

            }
            // can be null if this has been a playto from downloadlist or linkgrabber
            MediaNode mediaItem = extension.getMediaArchiveController().getItemById(id);
            DownloadLink dlink = extension.getLinkById(id);

            PlayToUpnpRendererDevice callingDevice = null;
            for (PlayToUpnpRendererDevice dev : mediaServer.getPlayToRenderer()) {
                if (request.getRemoteAddress().contains(dev.getAddress())) {
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

            Profile dlnaProfile = findProfile((MediaItem) mediaItem);

            String dlnaFeatures = "DLNA.ORG_PN=" + format + ";DLNA.ORG_OP=" + DLNAOp.create(DLNAOp.RANGE_SEEK_SUPPORTED) + ";DLNA.ORG_FLAGS=" + DLNAOrg.create(DLNAOrg.STREAMING_TRANSFER_MODE);

            String ct = "video/" + format;
            if (format.equals("jpg")) {
                ct = JPEGImage.JPEG_LRG.getMimeType().getLabel();
                dlnaFeatures = "DLNA.ORG_PN=" + JPEGImage.JPEG_LRG.getProfileID();
            }
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
            if (callingDevice != null) {
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
            }
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, ct));

            if (dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader("ContentFeatures.DLNA.ORG", dlnaFeatures));
            response.getResponseHeaders().add(new HTTPHeader("TransferMode.DLNA.ORG", "Streaming"));
            response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "bytes"));
            if (streamingInterface instanceof RarStreamer) {
                while (((RarStreamer) streamingInterface).getExtractionThread() == null && ((RarStreamer) streamingInterface).getException() == null) {
                    Thread.sleep(100);
                }

                if (((RarStreamer) streamingInterface).getException() != null) {
                    response.setResponseCode(ResponseCode.ERROR_BAD_REQUEST);
                    response.getOutputStream();
                    response.closeConnection();
                    return true;

                }
            }

            long length;
            if (request instanceof HeadRequest) {
                System.out.println("HEAD " + request.getRequestHeaders());
                response.setResponseCode(ResponseCode.SUCCESS_OK);
                length = streamingInterface.getFinalFileSize();
                if (length > 0) response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, length + ""));

                response.getOutputStream();
                response.closeConnection();
                return true;
            } else if (request instanceof GetRequest) {
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
        return false;
    }

    private Profile findProfile(MediaItem mediaItem) {
        ArrayList<Profile> ret = new ArrayList<Profile>();
        for (Profile p : Profile.ALL_PROFILES) {
            System.out.println(p);
        }
        return null;
    }

    @Override
    public boolean onPostRequest(PostRequest request, HttpResponse response) {
        return false;
    }

}
