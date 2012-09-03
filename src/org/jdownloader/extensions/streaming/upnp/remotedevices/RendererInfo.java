package org.jdownloader.extensions.streaming.upnp.remotedevices;

import org.fourthline.cling.model.meta.RemoteDevice;
import org.jdownloader.extensions.streaming.upnp.DeviceCache;
import org.jdownloader.extensions.streaming.upnp.remotedevices.handlers.AbstractDeviceHandler;

public class RendererInfo {

    private AbstractDeviceHandler handler;
    private RemoteDevice          device;
    private DeviceCache           cache;

    public RendererInfo(AbstractDeviceHandler profile, RemoteDevice d, DeviceCache deviceCache) {
        this.handler = profile;
        this.device = d;
        this.cache = deviceCache;
    }

    public AbstractDeviceHandler getHandler() {
        return handler;
    }

    public RemoteDevice getDevice() {
        return device;
    }

    public DeviceCache getCache() {
        return cache;
    }

    public String toString() {
        return handler + " " + device + "\r\n" + cache;
    }

}
