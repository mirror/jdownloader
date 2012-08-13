package org.jdownloader.extensions.streaming.upnp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jd.plugins.DownloadLink;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.action.ActionArgumentValue;
import org.fourthline.cling.model.action.ActionException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ErrorCode;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.connectionmanager.callback.GetProtocolInfo;
import org.fourthline.cling.support.model.Protocol;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.jdownloader.logging.LogController;

public class PlayToUpnpRendererDevice implements PlayToDevice {

    private Device rendererDevice;

    public Device getRendererDevice() {
        return rendererDevice;
    }

    public Service getAvtransportService() {
        return avtransportService;
    }

    private Service                avtransportService;
    private MediaServer            mediaServer;
    private LogSource              logger;
    private RendererDeviceSettings settings;
    private ProtocolInfos          protocolInfos;
    private String                 address;

    public PlayToUpnpRendererDevice(MediaServer mediaServer, Device d, Service avtransport) {
        this.rendererDevice = d;
        this.avtransportService = avtransport;
        this.mediaServer = mediaServer;
        logger = LogController.getInstance().getLogger(PlayToUpnpRendererDevice.class.getName());
        settings = JsonConfig.create(Application.getResource("tmp/streaming/devices/" + d.getIdentity().getUdn().toString().substring(5)), RendererDeviceSettings.class);
        address = ((RemoteDeviceIdentity) rendererDevice.getIdentity()).getDescriptorURL().getHost();
        init();
    }

    private void init() {
        if (!StringUtils.isEmpty(settings.getProtocolInfos())) {
            protocolInfos = new ProtocolInfos(settings.getProtocolInfos());
            return;
        }

        new Thread("ProtocolInfoLoader") {
            public void run() {

                ServiceId serviceId = new UDAServiceId("ConnectionManager");

                Service connectionManager = rendererDevice.findService(serviceId);

                GetProtocolInfo protocolInfoGetter = new GetProtocolInfo(connectionManager) {

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {

                        logger.severe(getDisplayName() + ": " + defaultMsg);
                    }

                    @Override
                    public void success(ActionInvocation invocation) {
                        ActionArgumentValue sink = invocation.getOutput("Sink");
                        try {
                            if (sink == null) {
                                setProtocolInfos(null);
                            } else {
                                ProtocolInfos pis = new ProtocolInfos("");

                                String[] infos = ModelUtil.fromCommaSeparatedList(sink.toString());
                                if (infos != null) {
                                    for (String info : infos) {
                                        ProtocolInfo pi = new ProtocolInfo(info);
                                        if (pi.getProtocol() != Protocol.HTTP_GET) {

                                            logger.warning("Unsupported Streamprotocol: " + getDisplayName() + " " + info);

                                        } else {
                                            pis.add(pi);
                                        }
                                    }

                                }
                                setProtocolInfos(pis);
                            }

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            invocation.setFailure(new ActionException(ErrorCode.ACTION_FAILED, "Can't parse ProtocolInfo response: " + ex, ex));
                            failure(invocation, null);
                        }
                    }

                    @Override
                    public void received(ActionInvocation actionInvocation, ProtocolInfos sinkProtocolInfos, ProtocolInfos sourceProtocolInfos) {
                        setProtocolInfos(sinkProtocolInfos);

                    }

                };

                mediaServer.getControlPoint().execute(protocolInfoGetter);

            }
        }.start();
    }

    public ProtocolInfos getProtocolInfos() {
        return protocolInfos;
    }

    private void setProtocolInfos(ProtocolInfos sinkProtocolInfos) {
        ProtocolInfos list = new ProtocolInfos();
        for (ProtocolInfo pi : sinkProtocolInfos) {
            if (pi.getProtocol() == Protocol.HTTP_GET) {
                list.add(pi);
            } else {
                // do not use ProtocolInfo.toString() - it contains nullpointer
                logger.warning("Unsupported Streamprotocol: " + getDisplayName() + " " + pi.getProtocol() + ":" + pi.getNetwork() + ":" + pi.getAdditionalInfo() + ":" + pi.getContentFormat());
            }
        }
        protocolInfos = list;
        logger.info("ProtocolInfos  for " + getDisplayName() + ": " + list);
        if (list != null) {
            settings.setProtocolInfos(list.toString());
        }
    }

    public String getDisplayName() {
        if (rendererDevice.getIdentity() instanceof RemoteDeviceIdentity) {
            try {
                return rendererDevice.getDetails().getFriendlyName() + " @ " + InetAddress.getByName(address).getHostName();
            } catch (UnknownHostException e) {

            }
        }
        return rendererDevice.getDetails().getFriendlyName();

    }

    @Override
    public void play(final DownloadLink link, final String id) {
        new Thread("PlayTpUpnpDevice") {
            public void run() {
                logger.info("Play " + link + " on " + getDisplayName() + " Supported formats: " + settings.getProtocolInfos());

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
