package org.jdownloader.extensions.streaming.upnp;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.support.connectionmanager.callback.GetProtocolInfo;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.jdownloader.logging.LogController;

public class MediaRenderer {

    private RemoteDevice              device;
    private RemoteService             connectionManager;
    private UpnpService               service;
    protected ProtocolInfos           sinkProtocolInfos;
    protected ProtocolInfos           sourceProtocolInfos;
    protected String                  errorMessage;
    private static final UDAServiceId CONNECTIONMANAGER_ID = new UDAServiceId("ConnectionManager");

    public MediaRenderer(UpnpService upnpService, RemoteDevice device) {

        this.device = device;
        this.service = upnpService;
        connectionManager = device.findService(CONNECTIONMANAGER_ID);

        service.getControlPoint().execute(new GetProtocolInfo(connectionManager) {

            @Override
            public void received(ActionInvocation actionInvocation, ProtocolInfos sinkProtocolInfos, ProtocolInfos sourceProtocolInfos) {
                MediaRenderer.this.sinkProtocolInfos = sinkProtocolInfos;
                MediaRenderer.this.sourceProtocolInfos = sourceProtocolInfos;
                LogController.getInstance().getLogger("streaming").info(getDisplayName() + " Supported Protocols: " + sinkProtocolInfos);

            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                LogController.getInstance().getLogger("streaming").info(getDisplayName() + ": " + defaultMsg);
                MediaRenderer.this.errorMessage = defaultMsg;
            }

        });

    }

    protected String getDisplayName() {
        return device.getDisplayString();
    }

    public String getHost() {
        return device.getIdentity().getDescriptorURL().getHost();
    }

    public String getUniqueId() {
        return device.getIdentity().getUdn().toString();
    }

}
