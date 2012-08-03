package org.jdownloader.extensions.vlcstreaming.upnp;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;

public class Main implements Runnable {
    public static void main(String[] args) throws Exception {

        Thread clientThread = new Thread(new Main());
        clientThread.setDaemon(false);
        clientThread.start();

    }

    void executeAction(UpnpService upnpService, Service switchPowerService) {

        ActionCallback setAVTransportURIAction = new SetAVTransportURI(switchPowerService, "http://192.168.2.122:3128/vlcstreaming/video?mkv", "<DIDL-Lite xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\"></DIDL-Lite>") {
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                // Something was wrong
                System.out.println("WRONG SETURI" + defaultMsg);
            }
        };
        ActionCallback playAction = new Play(switchPowerService) {
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                // Something was wrong
                System.out.println("WRONG Play " + defaultMsg);
            }
        };

        // Executes asynchronous in the background
        // upnpService.getControlPoint().execute(new Stop(switchPowerService) {
        //
        // @Override
        // public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        // System.out.println("WRONG STOP " + defaultMsg);
        // }
        //
        // });
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        upnpService.getControlPoint().execute(setAVTransportURIAction);
        upnpService.getControlPoint().execute(playAction);
        System.out.println("Played");
    }

    RegistryListener createRegistryListener(final UpnpService upnpService) {
        return new DefaultRegistryListener() {

            ServiceId serviceId = new UDAServiceId("AVTransport");

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {

                Service switchPower;
                if ((switchPower = device.findService(serviceId)) != null) {

                    if (device.getDetails().getFriendlyName().equals("[TV]UE55D7000")) {
                        System.out.println("Service discovered: " + switchPower);
                        executeAction(upnpService, switchPower);
                    }

                }

            }

            @Override
            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                Service switchPower;
                if ((switchPower = device.findService(serviceId)) != null && device.getDetails().getFriendlyName().equals("[TV]UE55D7000")) {
                    System.out.println("Service disappeared: " + switchPower);
                }
            }

        };
    }

    @Override
    public void run() {
        try {

            UpnpService upnpService = new UpnpServiceImpl();

            // Add a listener for device registration events
            upnpService.getRegistry().addListener(createRegistryListener(upnpService));

            // Broadcast a search message for all devices
            upnpService.getControlPoint().search(new STAllHeader());

        } catch (Exception ex) {
            System.err.println("Exception occured: " + ex);
            System.exit(1);
        }
    }
}
