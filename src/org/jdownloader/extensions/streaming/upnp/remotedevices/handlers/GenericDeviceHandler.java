package org.jdownloader.extensions.streaming.upnp.remotedevices.handlers;

import org.appwork.net.protocol.http.HTTPConstants;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.video.MPEG4Part2;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;

public class GenericDeviceHandler extends AbstractDeviceHandler {

    private String upnpUserAgent;

    /**
     * this handler is never autodetected. that's why all matches methods return false
     * 
     * @param extension
     */

    public boolean matchesUpnpHeader(UpnpHeaders headers) {
        try {
            if (upnpUserAgent == null) upnpUserAgent = headers.get(HTTPConstants.HEADER_REQUEST_USER_AGENT).get(0);
        } catch (Exception e) {

        }
        return false;
    }

    @Override
    public boolean matchesUpnpUserAgent(String upnpUserAgent) {
        if (upnpUserAgent == null) this.upnpUserAgent = upnpUserAgent;
        return false;
    }

    @Override
    public boolean matchesStreamUserAgent(String string) {
        return false;
    }

    @Override
    public boolean matchesRemoteDevice(RemoteDevice d, DeviceCache cache) {
        return false;
    }

    @Override
    public Profile getBestProfileForTranscoding(MediaItem mediaItem) {
        return MPEG4Part2.MPEG4_P2_TS_ASP_AAC;
    }

    protected String createStreamUrl(MediaItem c, String format, String subpath) {
        return getExtension().createStreamUrl(c.getUniqueID(), upnpUserAgent, format, subpath);
    }

    @Override
    public String getID() {
        return "default";
    }

}
