package org.jdownloader.extensions.streaming.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.Command;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.profile.ControlPointInfo;
import org.fourthline.cling.model.profile.HeaderDeviceDetailsProvider;
import org.fourthline.cling.model.types.DLNACaps;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.StreamClientConfigurationImpl;
import org.fourthline.cling.transport.impl.StreamClientImpl;
import org.fourthline.cling.transport.impl.StreamServerConfigurationImpl;
import org.fourthline.cling.transport.impl.StreamServerImpl;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.spi.StreamClient;
import org.fourthline.cling.transport.spi.StreamServer;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.streaming.upnp.content.ContentDirectory;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.seamless.http.Headers;

import com.sun.net.httpserver.HttpServer;

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
            upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration(8895) {
                // Override using Apache Http instead of sun http
                // This could be used to implement our own http stack instead
                @Override
                public StreamClient<StreamClientConfigurationImpl> createStreamClient() {
                    return new StreamClientImpl(new StreamClientConfigurationImpl()) {

                        @Override
                        public StreamResponseMessage sendRequest(StreamRequestMessage requestMessage) {
                            return super.sendRequest(requestMessage);
                        }

                        @Override
                        protected void applyRequestProperties(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) {
                            super.applyRequestProperties(urlConnection, requestMessage);
                        }

                        @Override
                        protected void applyHeaders(HttpURLConnection urlConnection, Headers headers) {
                            super.applyHeaders(urlConnection, headers);
                        }

                        @Override
                        protected void applyRequestBody(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) throws IOException {
                            super.applyRequestBody(urlConnection, requestMessage);
                        }

                        @Override
                        protected StreamResponseMessage createResponse(HttpURLConnection urlConnection, InputStream inputStream) throws Exception {
                            logger.info(urlConnection.toString());
                            StreamResponseMessage ret = super.createResponse(urlConnection, inputStream);
                            logger.info(ret + "\r\n" + ret.getBodyString());
                            return ret;
                        }

                    };
                }

                @Override
                public StreamServer<StreamServerConfigurationImpl> createStreamServer(NetworkAddressFactory networkAddressFactory) {
                    return new StreamServerImpl(new StreamServerConfigurationImpl(networkAddressFactory.getStreamListenPort())) {
                        synchronized public void init(InetAddress bindAddress, Router router) throws InitializationException {
                            try {
                                InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());

                                server = HttpServer.create(socketAddress, configuration.getTcpConnectionBacklog());
                                server.createContext("/", new ServerRequestHttpHandler(router, logger));

                                logger.info("Created server (for receiving TCP streams) on: " + server.getAddress());

                            } catch (Exception ex) {
                                throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
                            }
                        }
                    };
                }

                protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort) {
                    return new FixedNetworkAddressFactoryImpl(streamListenPort);
                }
            }, new RegistryListener[0]);

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(createDevice());
            DefaultRegistryListener listener = new DefaultRegistryListener() {

                @Override
                public void remoteDeviceAdded(Registry registry, RemoteDevice device) {

                    Service switchPower;

                    if ("MediaRenderer".equals(device.getType().getType())) {
                        logger.info("New MediaRenderer Device:" + device.getDisplayString());
                        logger.info("IP: " + device.getIdentity().getDescriptorURL().getHost());
                        logger.info(device.toString());
                        for (Service s : device.getServices()) {
                            logger.info("Service: " + s);
                        }
                        addRenderer(new MediaRenderer(upnpService, device));
                    }

                }

                @Override
                public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                    Service switchPower;
                    if ("MediaRenderer".equals(device.getType().getType())) {
                        removeRenderer(new MediaRenderer(upnpService, device));

                    }
                }

            };

            // upnpService.getRegistry().addListener(listener);
            //
            // // Broadcast a search message for all devices
            // upnpService.getControlPoint().search(new STAllHeader());
        } catch (Exception ex) {
            try {
                upnpService.shutdown();
            } catch (Throwable e) {

            }
            logger.log(ex);
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
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("org.jdownloader.extensions.vlcstreaming.upnp.MediaServer"));
        DeviceType type = new UDADeviceType("MediaServer", SERVER_VERSION);

        DeviceDetails details = new DeviceDetails("AppWork UPNP Media Server", new ManufacturerDetails("AppWorkGmbH"), new ModelDetails("AppWorkMediaServer", "A Upnp MediaServer to access the Downloadlist and Linkgrabberlist of JDownloader", "v1"));

        ManufacturerDetails manufacturer = new ManufacturerDetails("AppWork GmbH", "http://appwork.org");
        // Windows Media Player Device Details
        // seem like windows mediaplayer needs a special device description
        // http://4thline.org/projects/mailinglists.html#nabble-td3827350
        DeviceDetails wmpDetails = new DeviceDetails("JDMedia@" + host, manufacturer, new ModelDetails("Windows Media Player Sharing", "Windows Media Player Sharing", "12.0"), "000da201238c", "100000000001", "http://appwork.org/mediaserver", new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5), }, new DLNACaps(new String[] { "av-upload", "image-upload", "audio-upload" }));

        // Common Details
        DeviceDetails ownDetails = new DeviceDetails("JDMedia@" + host, manufacturer, new ModelDetails("JDownloader Media Server", "JDownloader Media Server", "1"), "000da201238c", "100000000001", "http://appwork.org/mediaserver", new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5), }, new DLNACaps(new String[] { "av-upload", "image-upload", "audio-upload" }));

        // Device Details Provider
        Map<HeaderDeviceDetailsProvider.Key, DeviceDetails> headerDetails = new HashMap<HeaderDeviceDetailsProvider.Key, DeviceDetails>();
        // WDTV?
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", "FDSSDP"), wmpDetails);
        // headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", ".*Windows\\-Media\\-Player.*"), wmpDetails);
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

}