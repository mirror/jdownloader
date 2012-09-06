package org.jdownloader.extensions.streaming.upnp.remotedevices.handlers;

import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;

public class Ps3Profile extends AbstractDeviceHandler {
    // http://manuals.playstation.net/document/en/ps3/current/video/filetypes.html
    // The following types of files can be played under (Video).
    // Memory Stick Video Format
    // - MPEG-4 SP (AAC LC)
    // - H.264/MPEG-4 AVC High Profile (AAC LC)
    // - MPEG-2 TS(H.264/MPEG-4 AVC, AAC LC)
    // MP4 file format
    // - H.264/MPEG-4 AVC High Profile (AAC LC)
    // MPEG-1 (MPEG Audio Layer 2)
    // MPEG-2 PS (MPEG2 Audio Layer 2, AAC LC, AC3(Dolby Digital), LPCM)
    // MPEG-2 TS(MPEG2 Audio Layer 2, AC3(Dolby Digital), AAC LC)
    // MPEG-2 TS(H.264/MPEG-4 AVC, AAC LC)
    // AVI
    // - Motion JPEG (Linear PCM)
    // - Motion JPEG (μ-Law)
    // AVCHD (.m2ts / .mts)
    // DivX
    // WMV
    // - VC-1(WMA Standard V2)
    public Ps3Profile() {

    }

    // Host: 192.168.2.102:8896
    // Content-type: text/xml; charset="utf-8"
    // Content-length: 924
    // X-av-client-info: av=5.0; cn="Sony Computer Entertainment Inc.";
    // mn="PLAYSTATION 3"; mv="1.0";
    // User-agent: UPnP/1.0 DLNADOC/1.50
    // Soapaction: "urn:schemas-upnp-org:service:ContentDirectory:1#Browse"
    public boolean matchesUpnpHeader(UpnpHeaders headers) {

        if (headers.get("X-av-client-info").get(0).matches("av=5\\.0\\; cn=\"Sony Computer Entertainment Inc\\.\"\\; mn=\"PLAYSTATION 3\"\\; mv=\"1\\.0\"\\;")) return true;

        return false;
    }

    @Override
    public boolean matchesUpnpUserAgent(String upnpUserAgent) {

        // UPnP/1.0 DLNADOC/1.50
        return upnpUserAgent != null && upnpUserAgent.matches("^UPnP/1\\.0 DLNADOC/1\\.50");
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
        return "ps3";
    }

}
