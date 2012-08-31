package org.jdownloader.extensions.streaming.upnp.deviceprofiles;

import org.appwork.utils.net.HeaderCollection;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;

public abstract class AbstractDeviceProfile {
    /**
     * Optional header check
     * 
     * @param headerCollection
     * @return
     */
    public boolean matchesStreamHeader(HeaderCollection headerCollection) {
        return true;
    }

    public abstract boolean matchesUpnpHeader(UpnpHeaders headers);

    public String getProfileID() {
        return getClass().getSimpleName();
    }

    /**
     * Checks the userAgent we see when the device calls the browse Content Directory Command.
     * 
     * @param upnpUserAgent
     * @return
     */
    public abstract boolean matchesUpnpUserAgent(String upnpUserAgent);

    /**
     * Checks the useragent we see when the device requests a stream url
     * 
     * @param string
     * @return
     */
    public abstract boolean matchesStreamUserAgent(String string);

    public abstract boolean matchesRemoteDevice(RemoteDevice d, DeviceCache cache);

}
