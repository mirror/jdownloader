package org.jdownloader.extensions.streaming.upnp;

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
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.Command;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.NetworkAddress;
import org.fourthline.cling.model.ValidationError;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.message.IncomingDatagramMessage;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.control.IncomingActionRequestMessage;
import org.fourthline.cling.model.message.discovery.OutgoingSearchResponseRootDevice;
import org.fourthline.cling.model.message.header.UDAServiceTypeHeader;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.profile.ControlPointInfo;
import org.fourthline.cling.model.profile.HeaderDeviceDetailsProvider;
import org.fourthline.cling.model.types.DLNACaps;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.protocol.ProtocolCreationException;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.protocol.ProtocolFactoryImpl;
import org.fourthline.cling.protocol.ReceivingAsync;
import org.fourthline.cling.protocol.async.ReceivingSearch;
import org.fourthline.cling.protocol.sync.ReceivingAction;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.fourthline.cling.transport.Router;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.mediaarchive.UpnpContentDirectory;
import org.jdownloader.extensions.streaming.upnp.clingext.Configuration;
import org.jdownloader.extensions.streaming.upnp.clingext.ExtReceivingNotification;
import org.jdownloader.extensions.streaming.upnp.clingext.ExtReceivingSearchResponse;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class MediaServer implements Runnable {

    private static final int   SERVER_VERSION = 1;

    private LogSource          logger;
    private UpnpServiceImpl    upnpService;

    private StreamingExtension extension;

    private DeviceManager      deviceManager;

    public StreamingExtension getExtension() {
        return extension;
    }

    public MediaServer(StreamingExtension streamingExtension) {
        logger = LogController.getInstance().getLogger("streaming");
        extension = streamingExtension;
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(final Object shutdownRequest) {
                shutdown();
            }
        });
    }

    public void run() {
        try {
            logger.info("Wait for extraction Module");
            while (ExtensionController.getInstance().getExtension(ExtractionExtension.class) == null || !ExtensionController.getInstance().getExtension(ExtractionExtension.class)._isEnabled()) {
                Thread.sleep(1000);
            }

            logger.info("Wait for extraction Module: Done");
            upnpService = new UpnpServiceImpl(new Configuration(extension), new RegistryListener[0]) {

                protected ProtocolFactory createProtocolFactory() {
                    // return super.createProtocolFactory();
                    return new ProtocolFactoryImpl(this) {
                        public ReceivingAsync createReceivingAsync(IncomingDatagramMessage message) throws ProtocolCreationException {

                            if (message.getOperation() instanceof UpnpRequest) {
                                IncomingDatagramMessage<UpnpRequest> incomingRequest = message;

                                switch (incomingRequest.getOperation().getMethod()) {
                                case NOTIFY:
                                    return isByeBye(incomingRequest) || isSupportedServiceAdvertisement(incomingRequest) ? new ExtReceivingNotification(getUpnpService(), incomingRequest) : null;
                                case MSEARCH:
                                    /**
                                     * IT seems like rpoot device searches must be answered by a OutgoingSearchResponseRootDevice - USNRootDeviceHeader header.
                                     * . xbmc for example has discovery problems if jd has a bad formated answer
                                     */
                                    return new ReceivingSearch(getUpnpService(), incomingRequest) {
                                        @Override
                                        protected void sendSearchResponseRootDevices(NetworkAddress activeStreamServer) {
                                            logger.fine("Responding to root device search with advertisement messages for all local root devices");
                                            for (LocalDevice device : getUpnpService().getRegistry().getLocalDevices()) {

                                                getUpnpService().getRouter().send(new OutgoingSearchResponseRootDevice(getInputMessage(), getDescriptorLocation(activeStreamServer, device), device));
                                            }
                                        }
                                    };

                                }

                            } else if (message.getOperation() instanceof UpnpResponse) {
                                IncomingDatagramMessage<UpnpResponse> incomingResponse = message;

                                return isSupportedServiceAdvertisement(incomingResponse) ? new ExtReceivingSearchResponse(getUpnpService(), incomingResponse) : null;
                            }

                            throw new ProtocolCreationException("Protocol for incoming datagram message not found: " + message);
                        }
                    };
                }
            };

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(createDevice());

            // upnpService.getRegistry().addListener(listener);
            //
            // // Broadcast a search message for all devices
            deviceManager = new DeviceManager(extension, upnpService);
            upnpService.getRegistry().addListener(deviceManager);
            upnpService.getControlPoint().search(new UDAServiceTypeHeader(new UDAServiceType("AVTransport", 1)));

        } catch (Throwable ex) {
            if (ex instanceof ValidationException) {
                List<ValidationError> errors = ((ValidationException) ex).getErrors();
                System.out.println(errors);
            }
            try {
                upnpService.shutdown();
            } catch (Throwable e) {

            }
            logger.log(ex);
            Dialog.getInstance().showExceptionDialog("Exception", "Could not start upnp server", ex);
        }
    }

    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    private HashMap<String, MediaRenderer> renderer = new HashMap<String, MediaRenderer>();

    private LocalDevice                    device;

    protected void removeRenderer(MediaRenderer mediaRenderer) {
        renderer.remove(mediaRenderer.getUniqueId());
    }

    protected void addRenderer(MediaRenderer mediaRenderer) {
        renderer.put(mediaRenderer.getUniqueId(), mediaRenderer);
    }

    private LocalDevice createDevice() throws IOException, ValidationException {

        String host = getHostName();

        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("org.jdownloader.extensions.vlcstreaming.upnp.MediaServer.new"), 180);

        DeviceType type = new UDADeviceType("MediaServer", SERVER_VERSION);
        // OutgoingSearchResponseRootDeviceUDN
        ManufacturerDetails manufacturer = new ManufacturerDetails("AppWork GmbH", "http://appwork.org");
        // Windows Media Player Device Details
        // seem like windows mediaplayer needs a special device description
        // http://4thline.org/projects/mailinglists.html#nabble-td3827350
        DeviceDetails wmpDetails = new DeviceDetails("JDMedia " + host, manufacturer, new ModelDetails("Windows Media Player Sharing", "Windows Media Player Sharing", "12.0"), "000da201238c", "100000000001", "http://appwork.org/mediaserver", new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5), }, new DLNACaps(new String[] { "av-upload", "image-upload", "audio-upload" }));
        ModelDetails model = new ModelDetails("JDownloader Media Server", "JDownloader Media Server", "1");
        // Common Details

        DeviceDetails ownDetails = new DeviceDetails("JDMedia " + host, manufacturer, model, (String) null, (String) null, new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5), new DLNADoc("M-DMS", DLNADoc.Version.V1_5) }, new DLNACaps(new String[] {}));

        // Device Details Provider
        Map<HeaderDeviceDetailsProvider.Key, DeviceDetails> headerDetails = new HashMap<HeaderDeviceDetailsProvider.Key, DeviceDetails>();
        // WDTV?
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", "FDSSDP"), wmpDetails);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", ".*Windows\\-Media\\-Player.*"), wmpDetails);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", "Xbox.*"), wmpDetails);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("X-AV-Client-Info", ".*PLAYSTATION 3.*"), ownDetails);
        HeaderDeviceDetailsProvider provider = new HeaderDeviceDetailsProvider(ownDetails, headerDetails) {
            public DeviceDetails provide(ControlPointInfo info) {
                DeviceDetails ret = super.provide(info);
                return ret;
            }
        };
        final ArrayList<Icon> lst = new ArrayList<Icon>();
        try {
            lst.add(new Icon("image/png", 256, 256, 24, new URI("icon/256.png"), createIcon("png", 256)) {

                @Override
                public Device getDevice() {
                    return device;
                }

            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            lst.add(new Icon("image/jpeg", 256, 256, 24, new URI("icon/256.jpg"), createIcon("jpeg", 256)) {

                @Override
                public Device getDevice() {
                    return device;
                }

            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            lst.add(new Icon("image/png", 120, 120, 24, new URI("icon/120.png"), createIcon("png", 120)) {

                @Override
                public Device getDevice() {
                    return device;
                }

            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            lst.add(new Icon("image/jpeg", 120, 120, 24, new URI("icon/120.jpg"), createIcon("jpeg", 120)) {

                @Override
                public Device getDevice() {
                    return device;
                }

            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            lst.add(new Icon("image/png", 48, 48, 24, new URI("icon/48.png"), createIcon("png", 48)) {

                @Override
                public Device getDevice() {
                    return device;
                }

            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            lst.add(new Icon("image/jpeg", 48, 48, 24, new URI("icon/48.jpg"), createIcon("jpeg", 48)) {

                @Override
                public Device getDevice() {
                    return device;
                }

            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            lst.add(new Icon("image/png", 32, 32, 24, new URI("icon/32.png"), createIcon("png", 32)) {

                @Override
                public Device getDevice() {
                    return device;
                }

            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            lst.add(new Icon("image/jpeg", 32, 32, 24, new URI("icon/32.jpg"), createIcon("jpeg", 32)) {

                @Override
                public Device getDevice() {
                    return device;
                }

            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        final Icon[] icons = lst.toArray(new Icon[] {});
        device = new LocalDevice(identity, type, provider, null, new LocalService[] { createContentDirectory(), createConnectionManager(), createMediaReceiverRegistrar() }) {
            public Icon[] getIcons() {
                IncomingActionRequestMessage rm = ReceivingAction.getRequestMessage();
                return icons;
            }

            public boolean hasIcons() {
                return true;
            }
        };

        return device;
    }

    private byte[] createIcon(String format, int size) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            if ("png".equals(format)) {
                BufferedImage ret = (BufferedImage) NewTheme.I().getImage("logo/jd_logo_256_256", size, false);
                ImageIO.write(ret, format, baos);
            } else {
                BufferedImage ret = (BufferedImage) NewTheme.I().getImage("logo/jd_logo_256_256", size - 4, false);
                int w = size;
                int h = size;

                Color fg = Color.BLACK;
                Color bg = Color.WHITE;

                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();

                GraphicsConfiguration gc = gd.getDefaultConfiguration();
                final BufferedImage image = gc.createCompatibleImage(w, h);

                // paint
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Float roundedRectangle = new Rectangle2D.Float(0, 0, w - 1, h - 1);
                g.setColor(bg);
                g.fill(roundedRectangle);
                g.setColor(bg.darker());
                g.draw(roundedRectangle);

                g.drawImage(ret, 2, 2, null);
                g.dispose();
                ImageIO.write(image, format, baos);
            }
            if (baos.size() == 0) throw new WTFException("Image Not found");
            return baos.toByteArray();
        } finally {
            baos.close();
        }

    }

    private LocalService<MediaReceiverRegistrar> createMediaReceiverRegistrar() {
        LocalService<MediaReceiverRegistrar> mediaReceiverRegistrar = new AnnotationLocalServiceBinder().read(MediaReceiverRegistrar.class);
        mediaReceiverRegistrar.setManager(new DefaultServiceManager<MediaReceiverRegistrar>(mediaReceiverRegistrar, MediaReceiverRegistrar.class) {
            public void execute(Command<MediaReceiverRegistrar> cmd) throws Exception {

                super.execute(cmd);
                logger.info("Exceuted: " + cmd);
            }

        });
        return mediaReceiverRegistrar;
    }

    private String getHostName() {
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            if (!org.appwork.utils.StringUtils.isEmpty(hostName)) { return hostName; }
        } catch (UnknownHostException e) {
            logger.log(e);
        }
        return "Home Server";
    }

    private LocalService<ConnectionManagerService> createConnectionManager() {
        LocalService<ConnectionManagerService> service = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
        final ProtocolInfos sourceProtocols = new ProtocolInfos(new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "audio/mpeg", "DLNA.ORG_PN=MP3;DLNA.ORG_OP=01"), new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/mpeg", "DLNA.ORG_PN=MPEG1;DLNA.ORG_OP=01;DLNA.ORG_CI=0"), new ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/mp4", "DLNA.ORG_PN=mp4;DLNA.ORG_OP=01;DLNA.ORG_CI=0"));
        service.setManager(new DefaultServiceManager<ConnectionManagerService>(service, null) {
            @Override
            protected ConnectionManagerService createServiceInstance() throws Exception {
                return new ConnectionManagerService(sourceProtocols, null);
            }
        });
        return service;
    }

    private LocalService<UpnpContentDirectory> createContentDirectory() {
        @SuppressWarnings("unchecked")
        LocalService<UpnpContentDirectory> mp3ContentService = new AnnotationLocalServiceBinder().read(UpnpContentDirectory.class);

        // init here to bypass the lazy init.
        // final ContentDirectory library = new ContentDirectory(this);
        final UpnpContentDirectory library = new UpnpContentDirectory(extension);
        mp3ContentService.setManager(new DefaultServiceManager<UpnpContentDirectory>(mp3ContentService, UpnpContentDirectory.class) {

            @Override
            protected UpnpContentDirectory createServiceInstance() throws Exception {
                return library;
            }

        });
        return mp3ContentService;

    }

    public void shutdown() {
        try {
            upnpService.shutdown();
        } catch (Exception e) {

        }
    }

    public Router getRouter() {
        return upnpService.getRouter();
    }

    /**
     * Returns a list of devices we can "Push" to
     * 
     * @return
     */
    public List<PlayToUpnpRendererDevice> getPlayToRenderer() {

        Collection<Device> devices = upnpService.getRegistry().getDevices(new UDAServiceType("AVTransport", 1));
        java.util.List<PlayToUpnpRendererDevice> ret = new ArrayList<PlayToUpnpRendererDevice>();
        ServiceId serviceId = new UDAServiceId("AVTransport");
        for (Device d : devices) {
            try {
                Service avtransport = d.findService(serviceId);
                Action play = avtransport.getAction("Play");
                Action setURI = avtransport.getAction("SetAVTransportURI");
                if (play != null && setURI != null) {
                    ret.add(new PlayToUpnpRendererDevice(this, d, avtransport));
                }
            } catch (Throwable e) {
                logger.log(e);
            }
        }

        return ret;
    }

    public ControlPoint getControlPoint() {

        return upnpService.getControlPoint();
    }

    public String getHost() {
        return getRouter().getNetworkAddressFactory().getBindAddresses()[0].getHostAddress();
    }

}