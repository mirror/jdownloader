package org.jdownloader.extensions.streaming.upnp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jd.plugins.DownloadLink;

import org.appwork.utils.Hash;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.jdownloader.logging.LogController;

public class PlayToUpnpRendererDevice implements PlayToDevice {

    private Device rendererDevice;

    public Device getRendererDevice() {
        return rendererDevice;
    }

    public Service getAvtransportService() {
        return avtransportService;
    }

    private Service     avtransportService;
    private MediaServer mediaServer;
    private LogSource   logger;

    public PlayToUpnpRendererDevice(MediaServer mediaServer, Device d, Service avtransport) {
        this.rendererDevice = d;
        this.avtransportService = avtransport;
        this.mediaServer = mediaServer;
        logger = LogController.getInstance().getLogger(PlayToUpnpRendererDevice.class.getName());
    }

    public String getDisplayName() {
        if (rendererDevice.getIdentity() instanceof RemoteDeviceIdentity) {
            try {
                return rendererDevice.getDetails().getFriendlyName() + " @ " + InetAddress.getByName(((RemoteDeviceIdentity) rendererDevice.getIdentity()).getDescriptorURL().getHost()).getHostName();
            } catch (UnknownHostException e) {

            }
        }
        return rendererDevice.getDetails().getFriendlyName();

    }

    @Override
    public void play(final DownloadLink link) {
        new Thread("PlayTpUpnpDevice") {
            public void run() {

                final String id = Hash.getMD5(link.getDownloadURL());
                final String url = "http://" + mediaServer.getHost() + ":3128/vlcstreaming/stream?" + id;

                ActionCallback setAVTransportURIAction = new SetAVTransportURI(avtransportService, url, "<DIDL-Lite xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\"></DIDL-Lite>") {
                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        // Something was wrong
                        System.out.println("WRONG SETURI" + defaultMsg);
                        logger.severe("SetAVTransportURI: " + defaultMsg);
                        Dialog.getInstance().showErrorDialog(defaultMsg);
                    }
                };
                ActionCallback playAction = new Play(avtransportService) {
                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        // Something was wrong
                        System.out.println("WRONG Play " + defaultMsg);
                        logger.severe("Play Action: " + defaultMsg);
                        Dialog.getInstance().showErrorDialog(defaultMsg);
                    }
                };

                mediaServer.getControlPoint().execute(setAVTransportURIAction);
                mediaServer.getControlPoint().execute(playAction);
                System.out.println("Played");
            }
        }.start();

    }

}
