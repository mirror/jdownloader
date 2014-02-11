package jd.controlling.reconnect.pluginsinc.upnp.cling;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.reconnect.pluginsinc.upnp.translate.T;

import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.Device;
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
        System.out.println("Starting Cling...");
        // final HashSet<RemoteDevice> devices = new HashSet<UpnpRouterDevice>();
        final UpnpServiceImpl upnpService = new UpnpServiceImpl(new RegistryListener() {

            public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
                System.out.println("Discovery started: " + device.getDisplayString() + " - " + device.getType());

            }

            public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
                System.out.println("Discovery failed: " + device.getDisplayString() + " => " + ((org.fourthline.cling.model.ValidationException) ex).getErrors());
            }

            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                System.out.println("Remote device available: " + device.getDisplayString() + " - " + device.getType());
                // UpnpRouterDevice d = new UpnpRouterDevice();
                // DeviceDetails detail = device.getDetails();
                // // d.setControlURL(device.getDetails().g);
                // d.setFriendlyname(device.getDetails().getFriendlyName());
                // d.setHost(device.getIdentity().getDescriptorURL().getHost());
                // // d.setLocation(location);
                // d.setManufactor(detail.getManufacturerDetails().getManufacturer());
                // d.setModelname(detail.getModelDetails().getModelName());
                // // d.setServiceType(servicyType);
                // d.setUrlBase(detail.getBaseURL() + "");
                // // d.setWanservice(wanservice);
                // devices.add(d);

            }

            public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
                System.out.println("Remote device updated: " + device.getDisplayString() + " - " + device.getType());
            }

            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                System.out.println("Remote device removed: " + device.getDisplayString());
            }

            public void localDeviceAdded(Registry registry, LocalDevice device) {
                System.out.println("Local device added: " + device.getDisplayString());
            }

            public void localDeviceRemoved(Registry registry, LocalDevice device) {
                System.out.println("Local device removed: " + device.getDisplayString());
            }

            public void beforeShutdown(Registry registry) {
                System.out.println("Before shutdown, the registry has devices: " + registry.getDevices().size());
            }

            public void afterShutdown() {
                System.out.println("Shutdown of registry complete!");
            }

        });
        // Send a search message to all devices and services, they should respond soon
        upnpService.getControlPoint().search();
        try {
            // Let's wait 10 seconds for them to respond
            System.out.println("Waiting 10 seconds before shutting down...");
            Thread.sleep(15000);
            HashSet<UpnpRouterDevice> ret = new HashSet<UpnpRouterDevice>();
            UDAServiceType serviceType = new UDAServiceType("WANIPConnection", 1);
            for (Device device : upnpService.getRegistry().getDevices(serviceType)) {
                try {
                    final Service service = device.findService(serviceType);
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
                            ret.add(d);

                        }

                    }
                } catch (Exception e) {
                    logger.log(e);
                }

            }
            serviceType = new UDAServiceType("WANPPPConnection", 1);
            for (Device device : upnpService.getRegistry().getDevices(serviceType)) {
                try {
                    final Service service = device.findService(serviceType);
                    if (service != null) {
                        Action action = service.getAction(ForceTermination.FORCE_TERMINATION);
                        if (action != null) {
                            UpnpRouterDevice d = new UpnpRouterDevice();
                            d.setModelname(device.getDisplayString());
                            d.setWanservice(T._.interaction_UpnpReconnect_wanservice_ppp());
                            URL url = ((RemoteService) service).getDevice().normalizeURI(((RemoteService) service).getControlURI());
                            d.setControlURL(url + "");
                            d.setFriendlyname(device.getDetails().getFriendlyName());
                            d.setManufactor(device.getDetails().getManufacturerDetails().getManufacturer());
                            ret.add(d);

                        }
                    }
                } catch (Exception e) {
                    logger.log(e);
                }

            }

            // Release all resources and advertise BYEBYE to other UPnP devices

            return new ArrayList<UpnpRouterDevice>(ret);
        } finally {
            System.out.println("Stopping Cling...");
            upnpService.shutdown();

        }
    }

    public static void main(String[] args) throws InterruptedException {
        new UPNPDeviceScanner().scan();
    }
}
