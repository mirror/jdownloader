package org.jdownloader.extensions.streaming.upnp.remotedevices.handlers;

import org.appwork.net.protocol.http.HTTPConstants;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;
import org.jdownloader.extensions.streaming.upnp.clingext.ExtRemoteDevice;

//tested with wmp 12
public class WindowsMediaPlayerProfile extends AbstractDeviceHandler {
    public WindowsMediaPlayerProfile() {

    }

    @Override
    public boolean matchesUpnpUserAgent(String upnpUserAgent) {

        return upnpUserAgent.matches("^Microsoft-Windows/.* UPnP/.* Windows-Media-Player/.* DLNADOC/.* \\(MS-DeviceCaps/.*\\).*");
    }

    @Override
    public boolean matchesStreamUserAgent(String string) {
        return string.matches("^Windows\\-Media\\-Player.*");
    }

    public boolean matchesUpnpHeader(UpnpHeaders headers) {
        if (!"no-cache".equals(headers.get(HTTPConstants.HEADER_REQUEST_CACHE_CONTROL).get(0))) return false;
        if (!"text/xml; charset=\"utf-8\"".equals(headers.get(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE).get(0))) return false;
        if (!"Close".equals(headers.get(HTTPConstants.HEADER_REQUEST_CONNECTION).get(0))) return false;
        if (!"no-cache".equals(headers.get(HTTPConstants.HEADER_REQUEST_PRAGMA).get(0))) return false;
        return headers.size() == 8;
    }

    // server headers:

    // {Cache-control=[max-age=900], Opt=["http://schemas.upnp.org/upnp/1/0/"; ns=01], Host=[239.255.255.250:1900],
    // Usn=[uuid:0db1bbea-ebf1-4362-93e6-8f7cf16d1988::urn:schemas-upnp-org:service:ContentDirectory:1],
    // Location=[http://192.168.2.122:2869/upnphost/udhisapi.dll?content=uuid:0db1bbea-ebf1-4362-93e6-8f7cf16d1988],
    // 01-nls=[8e8d2bfca30f43de7e80165c490ac44c], Nt=[urn:schemas-upnp-org:service:ContentDirectory:1], Nts=[ssdp:alive],
    // Server=[Microsoft-Windows-NT/5.1 UPnP/1.0 UPnP-Device-Host/1.0]}
    @Override
    public boolean matchesRemoteDevice(RemoteDevice d, DeviceCache cache) {
        if (d instanceof ExtRemoteDevice) {
            try {
                if (((ExtRemoteDevice) d).getHeaders().get(HTTPConstants.HEADER_RESPONSE_SERVER).get(0).matches("Microsoft\\-Windows.* UPnP/.* UPnP\\-Device\\-Host/.*")) { return true;

                }
            } catch (Throwable e) {

            }
        }
        return "Windows Media Player Sharing".equals(d.getDetails().getModelDetails().getModelName());
    }

    @Override
    public Profile getBestProfileForTranscoding(MediaItem mediaItem) {
        return null;
    }

    @Override
    public String getID() {
        return "wmp";
    }

}
