package org.jdownloader.extensions.streaming.upnp;

import java.io.IOException;
import java.util.logging.LogManager;


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
import org.fourthline.cling.model.types.DLNACaps;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.RegistrationException;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.item.VideoItem;
import org.jdownloader.logging.ExtLogManager;
import org.jdownloader.logging.LogController;
import org.seamless.util.MimeType;

public class SimpleServer {
    public static void main(String[] args) {

        System.setProperty("java.net.preferIPv4Stack", "true");
        // }

        // the logmanager should not be initialized here. so setting the property should tell the logmanager to init a ExtLogManager
        // instance.
        System.setProperty("java.util.logging.manager", "jd.ExtLogManager");
        ((ExtLogManager) LogManager.getLogManager()).setLogController(LogController.getInstance());

        new Thread() {
            public void run() {
                UpnpServiceImpl upnpService = new UpnpServiceImpl();
                try {
                    upnpService.getRegistry().addDevice(createDevice());
                } catch (RegistrationException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ValidationException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private static LocalDevice createDevice() throws IOException, ValidationException {

        long sessionid = System.currentTimeMillis();
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("TestMediaServer" + sessionid));
        DeviceType type = new UDADeviceType("MediaServer", 1);

        ManufacturerDetails manufacturer = new ManufacturerDetails("GmbH", "http://server.org");

        // Common Details
        DeviceDetails ownDetails = new DeviceDetails("TTest: " + sessionid, manufacturer, new ModelDetails("Media Server", "Media Server", "1"), "", null, "http://server.org/mediaserver", new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5), new DLNADoc("M-DMS", DLNADoc.Version.V1_5) }, new DLNACaps(new String[] {}));

        // Device Details Provider

        LocalDevice device = new LocalDevice(identity, type, ownDetails, (Icon) null, new LocalService[] { createContentDirectory(), createConnectionManager(), createMediaReceiverRegistrar() });

        return device;
    }

    private static LocalService createMediaReceiverRegistrar() {
        LocalService<MediaReceiverRegistrar> mediaReceiverRegistrar = new AnnotationLocalServiceBinder().read(MediaReceiverRegistrar.class);
        mediaReceiverRegistrar.setManager(new DefaultServiceManager<MediaReceiverRegistrar>(mediaReceiverRegistrar, MediaReceiverRegistrar.class));
        return mediaReceiverRegistrar;
    }

    private static LocalService createConnectionManager() {
        LocalService<ConnectionManagerService> connectionManager = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
        connectionManager.setManager(new DefaultServiceManager<ConnectionManagerService>(connectionManager, ConnectionManagerService.class));
        return connectionManager;
    }

    private static LocalService createContentDirectory() {
        LocalService<MyContentDirectory> contentDirectory = new AnnotationLocalServiceBinder().read(MyContentDirectory.class);
        contentDirectory.setManager(new DefaultServiceManager<MyContentDirectory>(contentDirectory, MyContentDirectory.class));
        return contentDirectory;
    }

    public static class MyContentDirectory extends AbstractContentDirectoryService {

        @Override
        public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult, long maxResults, SortCriterion[] orderby) throws ContentDirectoryException {
            try {
                DIDLContent d = new DIDLContent();
                Res res = new Res(new MimeType("video", "mp4"), 14435l, "0", 192l, "http://192.168.2.1/dfs");
                //
                d.addItem(new VideoItem("1", "-1", "myVideo.mp4", null, res));

                return new BrowseResult(new DIDLParser().generate(d), 1, 1);
            } catch (Exception e) {
                throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, e.toString());
            }
        }

        @Override
        public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {
            try {
                DIDLContent d = new DIDLContent();
                Res res = new Res(new MimeType("video", "mp4"), 14435l, "0", 192l, "http://192.168.2.1/dfs");
                //
                d.addItem(new VideoItem("1", "-1", "myVideo.mp4", null, res));

                return new BrowseResult(new DIDLParser().generate(d), 1, 1);
            } catch (Exception e) {
                throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, e.toString());
            }
        }

    }

}
