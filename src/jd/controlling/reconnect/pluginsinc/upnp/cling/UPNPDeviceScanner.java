package jd.controlling.reconnect.pluginsinc.upnp.cling;

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.pluginsinc.upnp.translate.T;
import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.jdownloader.logging.LogController;

public class UPNPDeviceScanner {

    private LogSource logger;

    public UPNPDeviceScanner() {
        logger = LogController.getInstance().getLogger(UPNPDeviceScanner.class.getName());
    }

    public List<UpnpRouterDevice> scan() throws InterruptedException {
        logger.info("Starting Cling...");
        final HashSet<UpnpRouterDevice> ret = new HashSet<UpnpRouterDevice>();
        final AtomicLong lastreceive = new AtomicLong(System.currentTimeMillis());

        // final HashSet<RemoteDevice> devices = new HashSet<UpnpRouterDevice>();
        DefaultUpnpServiceConfiguration config = new DefaultUpnpServiceConfiguration() {
            @Override
            public Executor getMulticastReceiverExecutor() {
                return super.getMulticastReceiverExecutor();
            }

            @Override
            public Integer getRemoteDeviceMaxAgeSeconds() {
                return super.getRemoteDeviceMaxAgeSeconds();
            }

        };

        final UpnpServiceImpl upnpService = new UpnpServiceImpl(config, new RegistryListener() {

            public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
                logger.info("Discovery started: " + device.getDisplayString() + " - " + device.getType());

            }

            public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
                logger.info("Discovery failed: " + device.getDisplayString() + " => " + ((org.fourthline.cling.model.ValidationException) ex).getErrors());
            }

            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                logger.info("Remote device available: " + device.getDisplayString() + " - " + device.getType());
                try {
                    Service service = device.findService(new UDAServiceType("WANIPConnection", 1));
                    if (service != null && service instanceof RemoteService) {
                        Action action = service.getAction(ForceTermination.FORCE_TERMINATION);

                        if (action != null) {
                            UpnpRouterDevice d = new UpnpRouterDevice();
                            d.setModelname(device.getDisplayString());
                            d.setWanservice(T._.interaction_UpnpReconnect_wanservice_ip());
                            URL url = ((RemoteService) service).getDevice().normalizeURI(((RemoteService) service).getControlURI());
                            d.setControlURL(url + "");
                            d.setServiceType(((RemoteService) service).getServiceType() + "");
                            d.setFriendlyname(device.getDetails().getFriendlyName());
                            d.setManufactor(device.getDetails().getManufacturerDetails().getManufacturer());
                            logger.info("Found " + JSonStorage.serializeToJson(d));
                            synchronized (ret) {
                                ret.add(d);
                            }
                            lastreceive.set(System.currentTimeMillis());

                        }

                    }

                    try {
                        service = device.findService(new UDAServiceType("WANPPPConnection", 1));
                        if (service != null && service instanceof RemoteService) {
                            Action action = service.getAction(ForceTermination.FORCE_TERMINATION);
                            if (action != null) {
                                UpnpRouterDevice d = new UpnpRouterDevice();
                                d.setModelname(device.getDisplayString());
                                d.setWanservice(T._.interaction_UpnpReconnect_wanservice_ppp());
                                URL url = ((RemoteService) service).getDevice().normalizeURI(((RemoteService) service).getControlURI());
                                d.setServiceType(((RemoteService) service).getServiceType() + "");
                                d.setControlURL(url + "");
                                d.setFriendlyname(device.getDetails().getFriendlyName());
                                d.setManufactor(device.getDetails().getManufacturerDetails().getManufacturer());
                                logger.info("Found " + JSonStorage.serializeToJson(d));
                                synchronized (ret) {
                                    ret.add(d);
                                }
                                lastreceive.set(System.currentTimeMillis());

                            }
                        }
                    } catch (Exception e) {
                        logger.log(e);
                    }

                } catch (Exception e) {
                    logger.log(e);
                }

            }

            public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
                // logger.info("Remote device updated: " + device.getDisplayString() + " - " + device.getType());
            }

            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                // logger.info("Remote device removed: " + device.getDisplayString());
            }

            public void localDeviceAdded(Registry registry, LocalDevice device) {
                // logger.info("Local device added: " + device.getDisplayString());
            }

            public void localDeviceRemoved(Registry registry, LocalDevice device) {
                // logger.info("Local device removed: " + device.getDisplayString());
            }

            public void beforeShutdown(Registry registry) {
                logger.info("Before shutdown, the registry has devices: " + registry.getDevices().size());
            }

            public void afterShutdown() {
                logger.info("Shutdown of registry complete!");
            }

        });
        // Send a search message to all devices and services, they should respond soon
        upnpService.getControlPoint().search();
        try {
            // Let's wait 10 seconds for them to respond
            logger.info("Waiting 15 seconds before shutting down...");
            Thread.sleep(15000);
            // while (ret.size() == 0 && System.currentTimeMillis() - lastreceive.get() < 30000) {
            // logger.info("Wait another 1 sec");
            // Thread.sleep(1000);
            // }
            final ArrayList<UpnpRouterDevice> devices;
            synchronized (ret) {
                devices = new ArrayList<UpnpRouterDevice>(ret);
            }
            if (devices.size() > 1) {
                InetAddress gateWay = null;
                try {
                    gateWay = RouterUtils.getIPFormNetStat();
                } catch (Throwable e) {
                    logger.log(e);
                }
                if (gateWay == null) {
                    try {
                        gateWay = RouterUtils.getIPFromRouteCommand();
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
                if (gateWay != null) {
                    final String gateWayAddress = gateWay.getHostAddress();
                    UpnpRouterDevice tryFirst = null;
                    for (UpnpRouterDevice device : devices) {
                        final String host = Browser.getHost(device.getControlURL(), false);
                        if (StringUtils.equals(host, gateWayAddress)) {
                            tryFirst = device;
                            break;
                        }
                    }
                    if (tryFirst != null) {
                        devices.remove(tryFirst);
                        devices.add(0, tryFirst);
                    }
                }
            }
            return devices;
        } finally {
            for (int i = 0; i < 4; i++) {
                // SEVERE: Router error on shutdown: org.fourthline.cling.transport.RouterException: Router wasn't available exclusively
                // after waiting 6000ms, lock failed: WriteLock
                // org.fourthline.cling.transport.RouterException: Router wasn't available exclusively after waiting 6000ms, lock failed:
                // WriteLock
                try {
                    logger.info("Stopping Cling...");
                    upnpService.shutdown();
                    break;
                } catch (Throwable e) {
                    logger.log(e);
                }
            }

        }
    }

    public static void main(String[] args) throws InterruptedException {
        new UPNPDeviceScanner().scan();
    }
}
