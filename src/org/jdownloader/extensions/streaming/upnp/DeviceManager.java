package org.jdownloader.extensions.streaming.upnp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.HeaderCollection;
import org.appwork.utils.net.SimpleHTTP;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.control.IncomingActionResponseMessage;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.protocol.sync.SendingAction;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.connectionmanager.callback.GetProtocolInfo;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.upnp.remotedevices.RendererInfo;
import org.jdownloader.extensions.streaming.upnp.remotedevices.handlers.AbstractDeviceHandler;
import org.jdownloader.extensions.streaming.upnp.remotedevices.handlers.GenericDeviceHandler;
import org.jdownloader.logging.LogController;

public class DeviceManager implements RegistryListener {
    private static final UDAServiceId        CONNECTIONMANAGER_ID = new UDAServiceId("ConnectionManager");
    private RemoteService                    connectionManager;
    private UpnpServiceImpl                  upnpService;
    private ControlPoint                     controlPoint;
    private LogSource                        logger;
    private Queue                            resourceLoader;
    private ArrayList<AbstractDeviceHandler> deviceProfiles;
    private RendererInfo                     defaultDevice;
    private StreamingExtension               extension;

    public DeviceManager(StreamingExtension extension, UpnpServiceImpl upnpService) {
        this.upnpService = upnpService;
        this.controlPoint = upnpService.getControlPoint();
        logger = LogController.getInstance().getLogger(DeviceManager.class.getName());
        resourceLoader = new Queue("DeviceManagerResourceLoader") {
        };

        this.extension = extension;
        initProfiles();
        GenericDeviceHandler generic = new GenericDeviceHandler();
        generic.setExtension(extension);
        defaultDevice = new RendererInfo(generic, null, null);
    }

    private void initProfiles() {
        deviceProfiles = new ArrayList<AbstractDeviceHandler>();

        try {

            List<Class<? extends AbstractDeviceHandler>> classes = ReflectionUtils.getClassesInPackage(Thread.currentThread().getContextClassLoader(), AbstractDeviceHandler.class.getPackage().getName(), null, AbstractDeviceHandler.class);
            for (Class<? extends AbstractDeviceHandler> cl : classes) {
                try {
                    if (cl == GenericDeviceHandler.class) continue;
                    AbstractDeviceHandler i = cl.newInstance();
                    i.setExtension(extension);
                    deviceProfiles.add(i);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
    }

    @Override
    public void remoteDeviceAdded(Registry registry, final RemoteDevice device) {

        System.out.println(device);

        if ("MediaRenderer".equalsIgnoreCase(device.getType().getType())) {

            final String id = device.getIdentity().getUdn().toString().substring(5);
            final RendererDeviceSettings deviceCache = JsonConfig.create(Application.getResource("tmp/streaming/devices/" + id), RendererDeviceSettings.class);

            int size = 0;
            org.fourthline.cling.model.meta.Icon biggest = null;
            for (org.fourthline.cling.model.meta.Icon icon : device.getIcons()) {

                if (biggest == null || icon.getHeight() * icon.getWidth() > size) {
                    biggest = icon;
                    size = icon.getHeight() * icon.getWidth();
                }
            }
            System.out.println("Biggest Icon: " + biggest);
            try {
                if (biggest != null) {
                    if (biggest.getUri() != null) {
                        URL descriptor = device.getIdentity().getDescriptorURL();

                        final URL iconUrl = biggest.getUri().isAbsolute() ? biggest.getUri().toURL() : new URL(descriptor, biggest.getUri().toString());

                        final String format = biggest.getMimeType().getSubtype();
                        resourceLoader.addAsynch(new QueueAction<Void, RuntimeException>() {

                            @Override
                            protected Void run() throws RuntimeException {

                                try {
                                    File file = Application.getResource("tmp/streaming/devices/icon_" + id + "." + format);
                                    FileCreationManager.getInstance().mkdir(file.getParentFile());
                                    new SimpleHTTP().download(iconUrl, null, file);
                                    deviceCache.setIconPath(Files.getRelativePath(Application.getResource("tmp").getParentFile(), file));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        });
                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            deviceCache.setDisplayString(device.getDetails().getFriendlyName());
            deviceCache.setUDN(id);
            deviceCache.setDescriptorURL(device.getIdentity().getDescriptorURL() + "");

            connectionManager = device.findService(CONNECTIONMANAGER_ID);
            if (connectionManager != null) {
                controlPoint.execute(new GetProtocolInfo(connectionManager) {

                    private IncomingActionResponseMessage response;

                    @Override
                    public void run() {
                        Service service = actionInvocation.getAction().getService();
                        if (service instanceof RemoteService) {
                            if (getControlPoint() == null) { throw new IllegalStateException("Callback must be executed through ControlPoint"); }

                            RemoteService remoteService = (RemoteService) service;

                            // Figure out the remote URL where we'd like to send the action request to
                            URL controLURL = remoteService.getDevice().normalizeURI(remoteService.getControlURI());

                            // Do it
                            SendingAction prot = getControlPoint().getProtocolFactory().createSendingAction(actionInvocation, controLURL);
                            prot.run();

                            response = prot.getOutputMessage();
                            try {
                                deviceCache.setServerName(response.getHeaders().get(HTTPConstants.HEADER_RESPONSE_SERVER).get(0));
                            } catch (Exception e) {

                            }
                            if (response == null) {
                                failure(actionInvocation, null);
                            } else if (response.getOperation().isFailed()) {
                                failure(actionInvocation, response.getOperation());
                            } else {
                                success(actionInvocation);
                            }
                        }
                    }

                    @Override
                    public void received(ActionInvocation actionInvocation, ProtocolInfos sinkProtocolInfos, ProtocolInfos sourceProtocolInfos) {

                        ProtocolInfos list = new ProtocolInfos();
                        for (ProtocolInfo pi : sinkProtocolInfos) {
                            if (pi.getProtocol() == Protocol.HTTP_GET) {
                                list.add(pi);
                            } else {
                                // do not use ProtocolInfo.toString() - it contains nullpointer
                                logger.warning("Unsupported Streamprotocol: " + device.getDisplayString() + " " + pi.getProtocol() + ":" + pi.getNetwork() + ":" + pi.getAdditionalInfo() + ":" + pi.getContentFormat());
                            }
                        }

                        if (list != null) {
                            deviceCache.setProtocolInfos(list.toString());
                        }
                        logger.info("Received Supported Protocols: " + deviceCache);

                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        logger.info("Error Receiving Protocol " + defaultMsg + ": " + deviceCache);
                    }

                });
            }
        }
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice device) {
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
    }

    @Override
    public void beforeShutdown(Registry registry) {
    }

    @Override
    public void afterShutdown() {
    }

    public DeviceCache getDeviceCache(Device device) {
        final String id = device.getIdentity().getUdn().toString().substring(5);
        final RendererDeviceSettings deviceCache = JsonConfig.create(Application.getResource("tmp/streaming/devices/" + id), RendererDeviceSettings.class);

        return new DeviceCache(id, device, deviceCache);
    }

    public RendererInfo findDeviceByUpnpHeaders(UpnpHeaders headers) {
        // TODO: some cache to get faster access
        for (AbstractDeviceHandler dp : deviceProfiles) {
            List<String> ua = headers.get(HTTPConstants.HEADER_REQUEST_USER_AGENT);
            try {
                if (dp.matchesUpnpUserAgent(ua.get(0))) {
                    if (dp.matchesUpnpHeader(headers)) {//
                        return new RendererInfo(dp, null, null);

                    }
                }
            } catch (Exception e) {
                logger.log(e);
            }
        }
        return defaultDevice;
    }

    /**
     * Tries to find a matching device.
     * 
     * there can be devices that do not have an own upnp server, and this have no remotedevice. we should have a profile for them. so
     * devices that have a profile, but no remotedevice are ok. On the other hand, there can be devices that have a remotedevice, but no
     * profile. This is a generic device then.
     * 
     * @param deviceID
     * @param address
     * @param headerCollection
     * @return
     */
    public RendererInfo findDevice(String deviceID, String address, HeaderCollection headerCollection) {
        logger.info("Try to find Device:");
        logger.info("UpnpUA: " + deviceID);
        logger.info("Address: " + address);
        logger.info("Header: " + headerCollection);
        AbstractDeviceHandler profile = findDeviceHandler(deviceID, headerCollection);

        // full match.
        if (profile != null) {
            for (RemoteDevice d : upnpService.getRegistry().getRemoteDevices()) {
                try {
                    if (!d.getIdentity().getDescriptorURL().getHost().equals(address)) continue;
                    DeviceCache cache = getDeviceCache(d);
                    if (profile.matchesRemoteDevice(d, cache)) {
                        logger.info("Profile Match: " + profile + " " + d);
                        logger.info(cache + "");
                        return new RendererInfo(profile, d, cache);

                    }
                } catch (Exception e) {

                }
            }

        }

        logger.info("No profile Match");

        if (profile == null) {
            // no profile. let's check to find a single generic remote device in the calleríp.
            ArrayList<RemoteDevice> matchesByIP = new ArrayList<RemoteDevice>();
            for (RemoteDevice d : upnpService.getRegistry().getRemoteDevices()) {
                try {
                    if (address != null && address.equals(d.getIdentity().getDescriptorURL().getHost())) {
                        logger.info("Found Device at " + address + ": " + d);
                        logger.info(getDeviceCache(d) + "");
                        matchesByIP.add(d);
                    }
                } catch (Exception e) {

                }
            }
            // if we have more than one device on this ip, we cannot differ between them. We need to create a Profile in this case
            if (matchesByIP.size() == 1) {

                logger.info("IP Match: " + profile + " " + matchesByIP.get(0));
                DeviceCache cache = getDeviceCache(matchesByIP.get(0));
                logger.info(cache + "");
                try {

                    return new RendererInfo(profile == null ? defaultDevice.getHandler() : profile, matchesByIP.get(0), cache);

                } catch (Exception e) {

                }

            }
        } else {
            // at least we have a profile.
            logger.info("No Remote Device found, or several Remote Devices on " + address + " profile: " + profile);
            return new RendererInfo(profile == null ? defaultDevice.getHandler() : profile, null, null);

        }

        // nothing found.we have to guess
        return defaultDevice;
    }

    private AbstractDeviceHandler findDeviceHandler(String deviceID, HeaderCollection headerCollection) {
        for (AbstractDeviceHandler dp : deviceProfiles) {
            HTTPHeader ua = headerCollection.get(HTTPConstants.HEADER_REQUEST_USER_AGENT);
            try {

                if (dp.matchesUpnpUserAgent(deviceID) || dp.getID().equals(deviceID)) {
                    if (dp.matchesStreamUserAgent(ua == null ? null : ua.getValue())) {
                        if (dp.matchesStreamHeader(headerCollection)) { return dp; }
                    }
                }
            } catch (Exception e) {
                logger.log(e);
            }
        }
        return null;
    }

}
