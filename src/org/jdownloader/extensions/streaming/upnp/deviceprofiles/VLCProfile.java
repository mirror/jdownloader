package org.jdownloader.extensions.streaming.upnp.deviceprofiles;

import org.appwork.net.protocol.http.HTTPConstants;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;

public class VLCProfile extends AbstractDeviceProfile {
    public VLCProfile() {

    }

    @Override
    public boolean matchesUpnpUserAgent(String upnpUserAgent) {
        // this might conflict with other libupnp tools
        return upnpUserAgent.matches("^\\d+\\.\\d+\\.\\d+ \\d+/Service Pack \\d+\\, UPnP/1\\.0\\, Portable SDK for UPnP devices/.*");
    }

    @Override
    public boolean matchesStreamUserAgent(String string) {
        return string != null && string.matches("^XBMC/.*");
    }

    // Host: 192.168.2.122:8896
    // Content-type: text/xml; charset="utf-8"
    // Content-length: 437
    // User-agent: 6.1.7601 2/Service Pack 1, UPnP/1.0, Portable SDK for UPnP devices/1.6.16
    // Soapaction: "urn:schemas-upnp-org:service:ContentDirectory:1#Browse"

    public boolean matchesUpnpHeader(UpnpHeaders headers) {

        return headers.size() == 5 && "text/xml; charset=\"utf-8\"".equals(headers.get(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE).get(0));
    }

    @Override
    public boolean matchesRemoteDevice(RemoteDevice d, DeviceCache cache) {
        // vlc is no remote device
        return false;
    }

}
