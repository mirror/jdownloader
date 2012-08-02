package org.jdownloader.extensions.vlcstreaming.upnp;

import java.io.IOException;

import org.appwork.utils.Application;
import org.jdownloader.images.NewTheme;
import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.ManufacturerDetails;
import org.teleal.cling.model.meta.ModelDetails;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.support.connectionmanager.ConnectionManagerService;
import org.teleal.cling.support.model.Protocol;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.ProtocolInfos;

public class MediaServer implements Runnable {

    private static final int SERVER_VERSION = 1;

    public static void main(String[] args) throws Exception {
        Application.setApplication(".jd_home");
        // Start a user thread that runs the UPnP stack
        Thread serverThread = new Thread(new MediaServer());
        serverThread.setDaemon(false);
        serverThread.start();
    }

    public void run() {
        try {

            final UpnpService upnpService = new UpnpServiceImpl();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    upnpService.shutdown();
                }
            });

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(createDevice());

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private LocalDevice createDevice() throws IOException, ValidationException {
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("org.jdownloader.extensions.vlcstreaming.upnp.MediaServer"));
        DeviceType type = new UDADeviceType("MediaServer", SERVER_VERSION);

        DeviceDetails details = new DeviceDetails("AppWork UPNP Media Server", new ManufacturerDetails("AppWorkGmbH"), new ModelDetails("AppWorkMediaServer", "A Upnp MediaServer to access the Downloadlist and Linkgrabberlist of JDownloader", "v1"));

        org.teleal.cling.model.meta.Icon icon = new org.teleal.cling.model.meta.Icon("image/png", 64, 64, 32, NewTheme.I().getImageUrl("logo/jd_logo_64_64"));

        LocalDevice device = new LocalDevice(identity, type, details, icon, new LocalService[] { createContentDirectory(), createConnectionManager() });

        return device;
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

        mp3ContentService.setManager(new DefaultServiceManager<ContentDirectory>(mp3ContentService, ContentDirectory.class));
        return mp3ContentService;

    }

}