package org.jdownloader.extensions.streaming.upnp.remotedevices.handlers;

import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;

public class Ps3Profile extends AbstractDeviceHandler {
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
    public Profile getBestProfileForTranscoding(MediaItem mediaItem) {
        return null;
    }

    @Override
    public Profile getBestProfileWithoutTranscoding(MediaItem mediaItem) {
        return null;
    }

    @Override
    public String getID() {
        return "ps3";
    }

}
