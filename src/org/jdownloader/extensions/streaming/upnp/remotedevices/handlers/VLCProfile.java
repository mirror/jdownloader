package org.jdownloader.extensions.streaming.upnp.remotedevices.handlers;

import org.appwork.net.protocol.http.HTTPConstants;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;

public class VLCProfile extends AbstractDeviceHandler {
    private boolean libupnp1616WorkaroundEnabled = false;

    public VLCProfile() {

    }

    @Override
    public boolean matchesUpnpUserAgent(String upnpUserAgent) {
        // this might conflict with other libupnp tools
        if (upnpUserAgent.contains("Portable SDK for UPnP devices/1.6.16")) {
            libupnp1616WorkaroundEnabled = true;
        }
        return upnpUserAgent.matches("^\\d+\\.\\d+\\.\\d+ \\d+/Service Pack \\d+\\, UPnP/1\\.0\\, Portable SDK for UPnP devices/.*");
    }

    @Override
    public boolean matchesStreamUserAgent(String string) {
        // VLC/2.0.2 LibVLC/2.0.2
        return string.matches("^VLC/2\\..* LibVLC/2\\..*");
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

    @Override
    protected long getMaxResultsPerCall(long maxResults) {

        // if (userAgent != null && userAgent.contains("Portable SDK for UPnP devices/1.6.16")) {

        if (!libupnp1616WorkaroundEnabled) return super.getMaxResultsPerCall(maxResults);
        // workaround a libdlna bug
        return Math.max(maxResults, 5);
    }

    @Override
    public Profile getBestProfileForTranscoding(MediaItem mediaItem) {
        // does not matter. vlc plays anything

        return null;
    }

    @Override
    public String getID() {
        return "vlc";
    }

}
