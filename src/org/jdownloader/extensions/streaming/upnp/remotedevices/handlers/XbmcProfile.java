package org.jdownloader.extensions.streaming.upnp.remotedevices.handlers;

import org.appwork.net.protocol.http.HTTPConstants;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;

public class XbmcProfile extends AbstractDeviceHandler {
    public XbmcProfile() {

    }

    // Content-type: text/xml; charset="utf-8"
    // Host: 192.168.2.122:8896
    // Content-length: 453
    // User-agent: Platinum/0.5.3.0, DLNADOC/1.50
    // Soapaction: "urn:schemas-upnp-org:service:ContentDirectory:1#Browse"

    public boolean matchesUpnpHeader(UpnpHeaders headers) {
        return headers.size() == 5 && "text/xml; charset=\"utf-8\"".equals(headers.get(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE).get(0));
    }

    @Override
    public boolean matchesUpnpUserAgent(String upnpUserAgent) {
        return upnpUserAgent != null && upnpUserAgent.matches("^Platinum/.*\\, DLNADOC/1\\.50");
    }

    @Override
    public boolean matchesStreamUserAgent(String string) {
        return string != null && string.matches("^XBMC/.*");
    }

    @Override
    public boolean matchesRemoteDevice(RemoteDevice d, DeviceCache cache) {
        if (!cache.getServerName().matches("UPnP/.*, DLNADOC/1\\.50\\, Platinum/.*")) return false;
        if (!"XBMC".equals(d.getDetails().getModelDetails().getModelName())) return false;
        return true;
    }

    @Override
    public String getID() {
        return "xbmc";
    }

}
