package org.jdownloader.extensions.streaming.renderer;

import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.upnp.DefaultRendererDevice;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;
import org.jdownloader.extensions.streaming.upnp.deviceprofiles.AbstractDeviceProfile;

public class RendererDeviceImpl extends DefaultRendererDevice {

    private AbstractDeviceProfile profile;
    private RemoteDevice          device;
    private DeviceCache           cache;

    public RendererDeviceImpl(AbstractDeviceProfile profile, RemoteDevice d, DeviceCache deviceCache) {
        this.profile = profile;
        this.device = d;
        this.cache = deviceCache;
    }

    public String toString() {
        return profile + " " + device + "\r\n" + cache;
    }
    // @Override
    // public Profile getBestProfile(MediaItem mediaItem) {
    // return null;
    // }
    //
    // @Override
    // public String createDlnaOrgPN(Profile dlnaProfile, MediaItem mediaItem) {
    // return null;
    // }
    //
    // @Override
    // public Profile getBestTranscodeProfile(MediaItem mediaItem) {
    // return null;
    // }
    //
    // @Override
    // public String createDlnaOrgOP(Profile dlnaProfile, MediaItem mediaItem) {
    // return null;
    // }
    //
    // @Override
    // public String createDlnaOrgFlags(Profile dlnaProfile, MediaItem mediaItem) {
    // return null;
    // }

}
