package org.jdownloader.extensions.streaming;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import javax.imageio.ImageIO;

import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
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
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.streaming.dataprovider.PipeStreamingInterface;
import org.jdownloader.extensions.streaming.dataprovider.rar.PartFileDataProvider;
import org.jdownloader.extensions.streaming.dataprovider.rar.RarArchiveDataProvider;
import org.jdownloader.extensions.streaming.dlna.DLNATransferMode;
import org.jdownloader.extensions.streaming.dlna.DLNATransportConstants;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.image.JPEGImage;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.rarstream.RarStreamer;
import org.jdownloader.extensions.streaming.upnp.MediaServer;
import org.jdownloader.extensions.streaming.upnp.remotedevices.RendererInfo;
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
    public synchronized boolean onGetRequest(GetRequest request, HttpResponse response) {

        String[] params = new Regex(request.getRequestedPath(), "^/stream/([^\\/]+)/([^\\/]+)/([^\\/]+)/?(.*)$").getRow(0);
        if (params == null) return false;
        String deviceid = null;
        String formatID = null;
        try {
            deviceid = URLDecoder.decode(params[0], "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        String id = params[1];
        try {
            formatID = URLDecoder.decode(params[2], "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String subpath = params[3];
        RendererInfo callingDevice = mediaServer.getDeviceManager().findDevice(deviceid, request.getRemoteAddress().get(0), request.getRequestHeaders());
        logger.info("Stream:_ " + request);
        try {

            if ("JPEG_TN".equals(formatID)) {
                onAlbumArtRequest(request, response, id, callingDevice, JPEGImage.JPEG_TN);
                return true;

            } else if ("JPEG_SM".equals(formatID)) {
                onAlbumArtRequest(request, response, id, callingDevice, JPEGImage.JPEG_SM);
                return true;

            }
            // can be null if this has been a playto from downloadlist or linkgrabber
            MediaItem mediaItem = (MediaItem) extension.getMediaArchiveController().getItemById(id);
            DownloadLink dlink = extension.getLinkById(id);
            if (dlink == null) { throw new WTFException("Link null"); }

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

            logger.info("Call from " + callingDevice);
            String dlnaFeatures = null;
            // we should redirect the contenttype from the plugins by default.
            String ct = org.jdownloader.extensions.streaming.dlna.Extensions.getType(format) + "/" + format;
            String acceptRanges = "bytes";
            String transferMode = null;
            if (mediaItem != null) {

                boolean isTranscodeRequired = false;
                Profile dlnaProfile = callingDevice.getHandler().getBestProfileWithoutTranscoding(mediaItem);

                if (dlnaProfile == null) {
                    isTranscodeRequired = true;
                    dlnaProfile = callingDevice.getHandler().getBestProfileForTranscoding(mediaItem);
                }
                if (dlnaProfile != null) transferMode = callingDevice.getHandler().getTransferMode(request, mediaItem instanceof ImageMediaItem ? DLNATransferMode.INTERACTIVE : DLNATransferMode.STREAMING);
                if (dlnaProfile != null) dlnaFeatures = callingDevice.getHandler().getDlnaFeaturesString(dlnaProfile, mediaItem, getHeader(request, DLNATransportConstants.HEADER_TRANSFERMODE), getHeader(request, DLNATransportConstants.HEADER_FEATURES));
                acceptRanges = callingDevice.getHandler().createHeaderAcceptRanges(dlnaProfile, mediaItem, getHeader(request, DLNATransportConstants.HEADER_TRANSFERMODE), getHeader(request, DLNATransportConstants.HEADER_FEATURES));
                ct = callingDevice.getHandler().createContentType(dlnaProfile, mediaItem);

            }

            // transferMode = null;
            if (ct != null) response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, ct));

            if (dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_FEATURES, dlnaFeatures));
            if (transferMode != null) response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_TRANSFERMODE, transferMode));
            response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", acceptRanges));
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

    private String getHeader(GetRequest request, String key) {
        try {
            return request.getRequestHeaders().get(key).getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void onAlbumArtRequest(GetRequest request, HttpResponse response, String id, RendererInfo callingDevice, JPEGImage profile) throws IOException {
        MediaItem item = (MediaItem) extension.getMediaArchiveController().getItemById(id);
        File path = Application.getResource(item.getThumbnailPath());
        String ct = profile.getMimeType().getLabel();
        String dlnaFeatures = callingDevice.getHandler().getDlnaFeaturesString(profile, item, getHeader(request, DLNATransportConstants.HEADER_TRANSFERMODE), getHeader(request, DLNATransportConstants.HEADER_FEATURES));
        if (!(request instanceof HeadRequest) && dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_FEATURES, dlnaFeatures));
        response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_TRANSFERMODE, callingDevice.getHandler().getTransferMode(request, DLNATransferMode.INTERACTIVE)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(IconIO.getScaledInstance(ImageProvider.read(path), profile.getWidth().getMax(), profile.getHeight().getMax()), "jpeg", baos);
        baos.close();
        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, baos.size() + ""));
        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, ct));
        response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "bytes"));
        response.setResponseCode(ResponseCode.SUCCESS_OK);

        // JPegImage.JPEG_TN
        if (!(request instanceof HeadRequest)) response.getOutputStream().write(baos.toByteArray());

        response.closeConnection();
    }

    // private List<ProfileMatch> findProfile(MediaItem mediaItem) {
    // if (mediaItem == null) return null;
    // ArrayList<ProfileMatch> ret = new ArrayList<ProfileMatch>();
    // logger.info("find DLNA Profile: " + mediaItem.getDownloadLink());
    // for (Profile p : Profile.ALL_PROFILES) {
    // if (mediaItem instanceof VideoMediaItem) {
    // VideoMediaItem video = (VideoMediaItem) mediaItem;
    // if (p instanceof AbstractAudioVideoProfile) {
    // ProfileMatch match = video.matches((AbstractAudioVideoProfile) p);
    //
    // if (match != null) {
    // logger.info(match.toString());
    // ret.add(match);
    // }
    //
    // }
    // }
    //
    // }
    // return ret;
    // }

    @Override
    public boolean onPostRequest(PostRequest request, HttpResponse response) {
        return false;
    }

}
