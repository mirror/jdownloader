package org.jdownloader.extensions.streaming.upnp;

import java.util.List;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.support.model.ProtocolInfos;

public class DeviceCache {

    private String                 id;
    private Device                 device;
    private RendererDeviceSettings cache;

    public DeviceCache(String id, Device device, RendererDeviceSettings deviceCache) {
        this.id = id;
        this.device = device;
        this.cache = deviceCache;
    }

    public String toString() {
        return cache.toString();
    }

    public String getAddress() {
        try {
            return ((RemoteDeviceIdentity) device.getIdentity()).getDescriptorURL().getHost();
        } catch (Exception e) {
            return null;
        }
    }

    public ProtocolInfos getProtocolInfos() {
        try {
            if (!StringUtils.isEmpty(cache.getProtocolInfos())) {
                //
                return new ProtocolInfos(cache.getProtocolInfos());

            }
        } catch (Exception e) {

        }

        return null;
    }

    public String getUserAgent() {

        return getHeaderValue(HTTPConstants.HEADER_REQUEST_USER_AGENT);

    }

    public String getServerName() {
        return getHeaderValue(HTTPConstants.HEADER_RESPONSE_SERVER);
    }

    private String getHeaderValue(String key) {
        if (cache.getHeaders() == null) return null;
        List<String> l = cache.getHeaders().get(key);
        if (l != null && l.size() > 0) return l.get(0);
        return null;
    }

}
