package org.jdownloader.extensions.streaming.upnp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.Command;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.message.IncomingDatagramMessage;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.message.UpnpResponse;
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
import org.fourthline.cling.protocol.async.ReceivingNotification;
import org.fourthline.cling.protocol.async.ReceivingSearch;
import org.fourthline.cling.protocol.async.ReceivingSearchResponse;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.fourthline.cling.transport.Router;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.streaming.upnp.content.ContentDirectory;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class MediaServer implements Runnable {

    private static final int SERVER_VERSION = 1;

    private LogSource        logger;
    private UpnpServiceImpl  upnpService;

    public MediaServer() {
        logger = LogController.getInstance().getLogger("streaming");

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public void getProtocolInfo(InetAddress adr) {

    }

    public void run() {
        try {
            logger.info("Wait for extraction Module");
            while (ExtensionController.getInstance().getExtension(ExtractionExtension.class) == null || !ExtensionController.getInstance().getExtension(ExtractionExtension.class)._isEnabled()) {
                Thread.sleep(1000);
            }

            logger.info("Wait for extraction Module: Done");
            upnpService = new UpnpServiceImpl(new Configuration(), new RegistryListener[0]) {

                protected ProtocolFactory createProtocolFactory() {
                    return new ProtocolFactoryImpl(this) {
                        public ReceivingAsync createReceivingAsync(IncomingDatagramMessage message) throws ProtocolCreationException {
                            logger.fine("Creating protocol for incoming asynchronous: " + message);

                            if (message.getOperation() instanceof UpnpRequest) {
                                IncomingDatagramMessage<UpnpRequest> incomingRequest = message;

                                switch (incomingRequest.getOperation().getMethod()) {
                                case NOTIFY:
                                    return isByeBye(incomingRequest) || isSupportedServiceAdvertisement(incomingRequest) ? new ReceivingNotification(getUpnpService(), incomingRequest) : null;
                                case MSEARCH:
                                    return new ReceivingSearch(getUpnpService(), incomingRequest) {
                                        @Override
                                        protected boolean waitBeforeExecution() throws InterruptedException {
                                            // this may help to find the server faster
                                            return true;
                                        }
                                    };
                                }

                            } else if (message.getOperation() instanceof UpnpResponse) {
                                IncomingDatagramMessage<UpnpResponse> incomingResponse = message;

                                return isSupportedServiceAdvertisement(incomingResponse) ? new ReceivingSearchResponse(getUpnpService(), incomingResponse) : null;
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
            upnpService.getControlPoint().search(new UDAServiceTypeHeader(new UDAServiceType("AVTransport", 1)));
        } catch (Throwable ex) {
            try {
                upnpService.shutdown();
            } catch (Throwable e) {

            }
            logger.log(ex);
            Dialog.getInstance().showExceptionDialog("Exception", "Could not start upnp server", ex);
        }
    }

    private HashMap<String, MediaRenderer> renderer = new HashMap<String, MediaRenderer>();

    protected void removeRenderer(MediaRenderer mediaRenderer) {
        renderer.remove(mediaRenderer.getUniqueId());
    }

    protected void addRenderer(MediaRenderer mediaRenderer) {
        renderer.put(mediaRenderer.getUniqueId(), mediaRenderer);
    }

    private LocalDevice createDevice() throws IOException, ValidationException {

        String host = getHostName();

        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("org.jdownloader.extensions.vlcstreaming.upnp.MediaServer.new"));
        DeviceType type = new UDADeviceType("MediaServer", SERVER_VERSION);

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
        // headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent",
        // ".*Windows\\-Media\\-Player.*"), wmpDetails);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", "Xbox.*"), wmpDetails);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("X-AV-Client-Info", ".*PLAYSTATION 3.*"), ownDetails);
        HeaderDeviceDetailsProvider provider = new HeaderDeviceDetailsProvider(ownDetails, headerDetails) {
            public DeviceDetails provide(ControlPointInfo info) {
                logger.info("Requesting Device Details:\r\n " + JSonStorage.toString(info));
                DeviceDetails ret = super.provide(info);
                logger.info("Response Device Details:\r\n " + JSonStorage.toString(ret));
                return ret;
            }
        };

        Icon icon = new Icon("image/png", 64, 64, 32, NewTheme.I().getImageUrl("logo/jd_logo_64_64"));

        LocalDevice device = new LocalDevice(identity, type, provider, icon, new LocalService[] { createContentDirectory(), createConnectionManager(), createMediaReceiverRegistrar() });

        return device;
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

    private LocalService<ContentDirectory> createContentDirectory() {
        @SuppressWarnings("unchecked")
        LocalService<ContentDirectory> mp3ContentService = new AnnotationLocalServiceBinder().read(ContentDirectory.class);

        // init here to bypass the lazy init.
        final ContentDirectory library = new ContentDirectory(this);
        mp3ContentService.setManager(new DefaultServiceManager<ContentDirectory>(mp3ContentService, ContentDirectory.class) {

            @Override
            protected ContentDirectory createServiceInstance() throws Exception {
                return library;
            }

        });
        return mp3ContentService;

    }

    public void shutdown() {
        upnpService.shutdown();
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
        ArrayList<PlayToUpnpRendererDevice> ret = new ArrayList<PlayToUpnpRendererDevice>();
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