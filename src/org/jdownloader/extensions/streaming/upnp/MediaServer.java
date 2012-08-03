package org.jdownloader.extensions.streaming.upnp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.profile.HeaderDeviceDetailsProvider;
import org.fourthline.cling.model.types.DLNACaps;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.jdownloader.extensions.streaming.upnp.content.ContentDirectory;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class MediaServer implements Runnable {

    private static final int SERVER_VERSION = 1;

    public static void main(String[] args) throws Exception {
        Application.setApplication(".jd_home");
        // Start a user thread that runs the UPnP stack
        Thread serverThread = new Thread(new MediaServer());
        serverThread.setDaemon(false);
        serverThread.start();
    }

    private LogSource       logger;
    private UpnpServiceImpl upnpService;

    public MediaServer() {
        logger = LogController.getInstance().getLogger("UPNP Media Server");
    }

    public void run() {
        try {

            // final UpnpService upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration(8895) {
            // // Override using Apache Http instead of sun http
            // // This could be used to implement our own http stack instead
            // @Override
            // public StreamClient<StreamClientConfigurationImpl> createStreamClient() {
            // return new StreamClientImpl(new StreamClientConfigurationImpl());
            // }
            //
            // @Override
            // public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
            // return new StreamServerImpl(new StreamServerConfigurationImpl(networkAddressFactory.getStreamListenPort()));
            // }
            // }, new RegistryListener[0]);
            upnpService = new UpnpServiceImpl();

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(createDevice());

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
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
        DeviceDetails wmpDetails = new DeviceDetails(host + ": JDMedia", manufacturer, new ModelDetails("Windows Media Player Sharing", "Windows Media Player Sharing", "12.0"), "000da201238c", "100000000001", "http://appwork.org/mediaserver", new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5), }, new DLNACaps(new String[] { "av-upload", "image-upload", "audio-upload" }));

        // Common Details
        DeviceDetails ownDetails = new DeviceDetails(host + ": JDMedia", manufacturer, new ModelDetails("JDownloader Media Server", "JDownloader Media Server", "1"), "000da201238c", "100000000001", "http://appwork.org/mediaserver", new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5), }, new DLNACaps(new String[] { "av-upload", "image-upload", "audio-upload" }));

        // Device Details Provider
        Map<HeaderDeviceDetailsProvider.Key, DeviceDetails> headerDetails = new HashMap<HeaderDeviceDetailsProvider.Key, DeviceDetails>();
        // WDTV?
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", "FDSSDP"), wmpDetails);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", "Xbox.*"), wmpDetails);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("X-AV-Client-Info", ".*PLAYSTATION 3.*"), ownDetails);
        HeaderDeviceDetailsProvider provider = new HeaderDeviceDetailsProvider(ownDetails, headerDetails);

        Icon icon = new Icon("image/png", 64, 64, 32, NewTheme.I().getImageUrl("logo/jd_logo_64_64"));

        LocalDevice device = new LocalDevice(identity, type, provider, icon, new LocalService[] { createContentDirectory(), createConnectionManager(), createMediaReceiverRegistrar() });

        return device;
    }

    private LocalService<MediaReceiverRegistrar> createMediaReceiverRegistrar() {
        LocalService<MediaReceiverRegistrar> mediaReceiverRegistrar = new AnnotationLocalServiceBinder().read(MediaReceiverRegistrar.class);
        mediaReceiverRegistrar.setManager(new DefaultServiceManager<MediaReceiverRegistrar>(mediaReceiverRegistrar, MediaReceiverRegistrar.class));
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
        final ContentDirectory library = new ContentDirectory();
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

}