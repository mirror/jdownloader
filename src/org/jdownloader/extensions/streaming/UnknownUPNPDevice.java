package org.jdownloader.extensions.streaming;

import jd.plugins.DownloadLink;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.jdownloader.extensions.streaming.upnp.PlayToUpnpRendererDevice;

public class UnknownUPNPDevice extends PlayToUpnpRendererDevice {

    public UnknownUPNPDevice() {
        super(null, null, null);
    }

    protected void init() {

    }

    @Override
    public Device getRendererDevice() {
        return super.getRendererDevice();
    }

    @Override
    public Service getAvtransportService() {
        return super.getAvtransportService();
    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public ProtocolInfos getProtocolInfos() {
        return super.getProtocolInfos();
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public void play(DownloadLink link, String id) {
        super.play(link, id);
    }
}
