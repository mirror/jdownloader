package org.jdownloader.extensions.streaming;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;

import javax.imageio.ImageIO;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HeadRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.extensions.streaming.dataprovider.DownloadStreamManager;
import org.jdownloader.extensions.streaming.dataprovider.StreamFactoryInterface;
import org.jdownloader.extensions.streaming.dataprovider.TranscodeStreamManager;
import org.jdownloader.extensions.streaming.dlna.DLNATransferMode;
import org.jdownloader.extensions.streaming.dlna.DLNATransportConstants;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.image.AbstractImageProfile;
import org.jdownloader.extensions.streaming.dlna.profiles.image.JPEGImage;
import org.jdownloader.extensions.streaming.dlna.profiles.image.PNGImage;
import org.jdownloader.extensions.streaming.mediaarchive.ImageMediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaNode;
import org.jdownloader.extensions.streaming.mediaarchive.StreamError;
import org.jdownloader.extensions.streaming.mediaarchive.StreamError.ErrorCode;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.UndefinedMediaItem;
import org.jdownloader.extensions.streaming.upnp.MediaServer;
import org.jdownloader.extensions.streaming.upnp.remotedevices.RendererInfo;
import org.jdownloader.logging.LogController;

public class HttpApiImpl implements HttpRequestHandler {

    private StreamingExtension     extension;
    private LogSource              logger;
    private MediaServer            mediaServer;
    private TranscodeStreamManager transcodeManager;
    private DownloadStreamManager  downloadManager;

    public HttpApiImpl(StreamingExtension extension, MediaServer mediaServer) {
        this.extension = extension;
        this.mediaServer = mediaServer;
        logger = LogController.getInstance().getLogger(getClass().getName());
        Profile.init();
        transcodeManager = new TranscodeStreamManager(extension);
        downloadManager = new DownloadStreamManager(extension);

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
        MediaItem mediaItem = null;
        DownloadLink dlink = null;
        String[] params = new Regex(request.getRequestedPath(), "^/stream/([^\\/]+)/([^\\/]+)/([^\\/]+)/?(.*)$").getRow(0);
        if (params == null) return false;
        String deviceid = null;
        String formatID = null;
        try {
            deviceid = URLDecoder.decode(params[0], "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        String id = null;
        try {
            formatID = URLDecoder.decode(params[2], "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        try {
            id = URLDecoder.decode(params[1], "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String subpath = params[3];
        RendererInfo callingDevice = mediaServer.getDeviceManager().findDevice(deviceid, request.getRemoteAddress().get(0), request.getRequestHeaders());
        logger.info("Stream:_ " + request);
        try {

            if (JPEGImage.JPEG_TN.getProfileID().equals(formatID)) {
                onAlbumArtRequest(request, response, id, callingDevice, JPEGImage.JPEG_TN);
                return true;

            } else if (JPEGImage.JPEG_SM.getProfileID().equals(formatID)) {
                onAlbumArtRequest(request, response, id, callingDevice, JPEGImage.JPEG_SM);
                return true;

            } else if (PNGImage.PNG_SM_ICO.getProfileID().equals(formatID)) {
                onAlbumArtRequest(request, response, id, callingDevice, PNGImage.PNG_SM_ICO);
                return true;

            } else if (PNGImage.PNG_LRG_ICO.getProfileID().equals(formatID)) {
                onAlbumArtRequest(request, response, id, callingDevice, PNGImage.PNG_LRG_ICO);
                return true;

            }
            ByteRange range = new ByteRange(request);
            // can be null if this has been a playto from downloadlist or linkgrabber
            mediaItem = (MediaItem) extension.getItemById(id);
            dlink = extension.getLinkById(id);
            if (dlink == null) { throw new WTFException("Link null"); }

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
            boolean isTranscodeRequired = false;
            Profile dlnaProfile = null;
            if (mediaItem != null && !(mediaItem instanceof UndefinedMediaItem)) {

                dlnaProfile = callingDevice.getHandler().getBestProfileWithoutTranscoding(mediaItem, formatID);

                if (dlnaProfile == null) {
                    isTranscodeRequired = true;
                    dlnaProfile = callingDevice.getHandler().getBestProfileForTranscoding(mediaItem, formatID);
                }
                if (dlnaProfile != null) transferMode = callingDevice.getHandler().getTransferMode(request, mediaItem instanceof ImageMediaItem ? DLNATransferMode.INTERACTIVE : DLNATransferMode.STREAMING);
                if (dlnaProfile != null) dlnaFeatures = callingDevice.getHandler().getDlnaFeaturesString(dlnaProfile, mediaItem, getHeader(request, DLNATransportConstants.HEADER_TRANSFERMODE), getHeader(request, DLNATransportConstants.HEADER_FEATURES));
                acceptRanges = callingDevice.getHandler().createHeaderAcceptRanges(dlnaProfile, mediaItem, getHeader(request, DLNATransportConstants.HEADER_TRANSFERMODE), getHeader(request, DLNATransportConstants.HEADER_FEATURES));
                ct = callingDevice.getHandler().createContentType(dlnaProfile, mediaItem);

            }

            // boolean archiveIsOpen = true;
            // if (streamingInterface == null) {
            // ExtractionExtension archiver = (ExtractionExtension)
            // ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();
            //
            // DownloadLinkArchiveFactory fac = new DownloadLinkArchiveFactory(dlink);
            // Archive archive = archiver.getArchiveByFactory(fac);
            // if (archive != null) {
            //
            // streamingInterface = new PipeStreamingInterface(null, new
            // TranscodeDataProvider(extension.getSettings().getStreamServerPort(), mediaItem, callingDevice, isTranscodeRequired,
            // dlnaProfile, new RarArchiveDataProvider(archive, subpath, new
            // PartFileDataProvider(extension.getDownloadLinkDataProvider()))));
            // // Thread.sleep(5000);
            // addHandler(id + "DeviceHandler:" + callingDevice.getHandler().getID(), streamingInterface);
            //
            // } else {

            StreamFactoryInterface streamfactory = downloadManager.getStreamFactory(dlink);
            if (isTranscodeRequired) {
                streamfactory = transcodeManager.getStreamFactory(streamfactory, mediaItem, callingDevice, dlnaProfile);
            }

            // streamingInterface = new PipeStreamingInterface(dlink, new
            // TranscodeDataProvider(extension.getSettings().getStreamServerPort(), mediaItem, callingDevice, isTranscodeRequired,
            // dlnaProfile, extension.getDownloadLinkDataProvider()));
            // addHandler(id + "DeviceHandler:" + callingDevice.getHandler().getID(), streamingInterface);
            //
            // }
            // }
            // transferMode = null;

            // if (streamingInterface instanceof RarStreamer) {
            // while (((RarStreamer) streamingInterface).getExtractionThread() == null && ((RarStreamer) streamingInterface).getException()
            // == null) {
            // Thread.sleep(100);
            // }
            //
            // if (((RarStreamer) streamingInterface).getException() != null) {
            // response.setResponseCode(ResponseCode.ERROR_BAD_REQUEST);
            // response.getOutputStream();
            // response.closeConnection();
            // return true;
            //
            // }
            // }
            response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", acceptRanges));
            long length;
            if (request instanceof HeadRequest) {
                System.out.println("HEAD " + request.getRequestHeaders());
                response.setResponseCode(ResponseCode.SUCCESS_OK);
                length = streamfactory.getContentLength();
                if (length > 0) response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, length + ""));
                if (ct != null) response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, ct));
                if (dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_FEATURES, dlnaFeatures));
                if (transferMode != null) response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_TRANSFERMODE, transferMode));
                response.getOutputStream(true);
                response.closeConnection();
                return true;
            } else if (request instanceof GetRequest) {
                System.out.println("GET " + request.getRequestHeaders());
                if (ct != null) response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, ct));
                if (dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_FEATURES, dlnaFeatures));
                if (transferMode != null) response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_TRANSFERMODE, transferMode));
                new StreamLinker(response).run(streamfactory, range);

            }
        } catch (final Throwable e) {
            if (e.getMessage().contains("socket write error")) {

                // connection has been closed by the caller;
                return true;
            }
            handleException(e, dlink, mediaItem);
            logger.log(e);
            try {
                response.getOutputStream(true).write(Exceptions.getStackTrace(e).getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e1) {

            } catch (IOException e1) {

            }

            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/plain"));
            response.setResponseCode(ResponseCode.SERVERERROR_INTERNAL);

        } finally {
            System.out.println("Resp: " + response.getResponseHeaders().toString());

        }
        return false;
    }

    private void handleException(Throwable e, DownloadLink dlink, MediaItem mediaItem) {
        if (e instanceof PluginException) {
            if (dlink != null) {
                ((PluginException) e).fillLinkStatus(dlink.getLinkStatus());

                if (dlink.getLinkStatus().hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                    if (mediaItem != null) mediaItem.setDownloadError(new StreamError(ErrorCode.LINK_OFFLINE));

                }
            }

        }
        if (e.getCause() != null && e.getCause() != e) handleException(e.getCause(), dlink, mediaItem);
    }

    private String getHeader(GetRequest request, String key) {
        try {
            return request.getRequestHeaders().get(key).getValue();
        } catch (Exception e) {
            return null;
        }
    }

    public void onAlbumArtRequest(GetRequest request, HttpResponse response, String id, RendererInfo callingDevice, AbstractImageProfile profile) throws IOException {
        MediaNode item = (MediaNode) extension.getMediaArchiveController().getItemById(id);
        URL path = Application.getRessourceURL(item.getThumbnailPath());
        String ct = profile.getMimeType().getLabel();
        String dlnaFeatures = null;

        if (item instanceof MediaItem) {
            dlnaFeatures = callingDevice.getHandler().getDlnaFeaturesString(profile, (MediaItem) item, getHeader(request, DLNATransportConstants.HEADER_TRANSFERMODE), getHeader(request, DLNATransportConstants.HEADER_FEATURES));
        }
        if (!(request instanceof HeadRequest) && dlnaFeatures != null) response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_FEATURES, dlnaFeatures));
        response.getResponseHeaders().add(new HTTPHeader(DLNATransportConstants.HEADER_TRANSFERMODE, callingDevice.getHandler().getTransferMode(request, DLNATransferMode.INTERACTIVE)));
        // ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // ImageIO.write(IconIO.getScaledInstance(ImageProvider.read(path), profile.getWidth().getMax(), profile.getHeight().getMax()),
        // "jpeg", baos);
        // baos.close();
        byte[] data = createIcon(path, profile.getWidth().getMax(), profile.getHeight().getMax(), profile instanceof JPEGImage ? "jpeg" : "png");
        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, data.length + ""));
        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, ct));
        response.getResponseHeaders().add(new HTTPHeader("Accept-Ranges", "bytes"));
        response.setResponseCode(ResponseCode.SUCCESS_OK);

        // JPegImage.JPEG_TN
        if (!(request instanceof HeadRequest)) response.getOutputStream(true).write(data);

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

    private byte[] createIcon(URL path, int width, int height, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            if ("png".equals(format) || path.toString().toLowerCase(Locale.ENGLISH).endsWith(".jpeg") || path.toString().toLowerCase(Locale.ENGLISH).endsWith(".jpg")) {
                BufferedImage ret = (BufferedImage) ImageIO.read(path);
                ImageIO.write(IconIO.getScaledInstance(ret, width, height, Interpolation.BICUBIC, true), format, baos);
            } else {
                BufferedImage ret = (BufferedImage) ImageIO.read(path);
                ret = IconIO.getScaledInstance(ret, width, height, Interpolation.BICUBIC, true);
                Color bg = Color.WHITE;

                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();

                GraphicsConfiguration gc = gd.getDefaultConfiguration();
                final BufferedImage image = gc.createCompatibleImage(width, height);

                // paint
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Float roundedRectangle = new Rectangle2D.Float(0, 0, width, height);
                g.setColor(bg);
                g.fill(roundedRectangle);

                g.drawImage(ret, 0, 0, null);
                g.dispose();
                ImageIO.write(image, format, baos);
            }
            if (baos.size() == 0) throw new WTFException("Image Not found");
            return baos.toByteArray();
        } finally {
            baos.close();
        }
    }

    @Override
    public boolean onPostRequest(PostRequest request, HttpResponse response) {
        return false;
    }

}
