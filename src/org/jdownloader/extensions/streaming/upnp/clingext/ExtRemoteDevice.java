package org.jdownloader.extensions.streaming.upnp.clingext;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.discovery.IncomingNotificationRequest;
import org.fourthline.cling.model.message.discovery.IncomingSearchResponse;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.UDAVersion;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDN;

public class ExtRemoteDevice extends RemoteDevice {
    private HashMap<String, List<String>> headers;

    public HashMap<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(HashMap<String, List<String>> headers) {
        this.headers = headers;
    }

    public ExtRemoteDevice(IncomingNotificationRequest inputMessage, RemoteDeviceIdentity rdIdentity) throws ValidationException {
        super(rdIdentity);
        this.headers = new HashMap<String, List<String>>();
        UpnpHeaders rHeaders = inputMessage.getHeaders();

        for (Entry<String, List<String>> h : rHeaders.entrySet()) {
            headers.put(h.getKey(), h.getValue());
        }
    }

    public ExtRemoteDevice(IncomingSearchResponse inputMessage, RemoteDeviceIdentity rdIdentity) throws ValidationException {
        super(rdIdentity);
        this.headers = new HashMap<String, List<String>>();
        UpnpHeaders rHeaders = inputMessage.getHeaders();

        for (Entry<String, List<String>> h : rHeaders.entrySet()) {
            headers.put(h.getKey(), h.getValue());
        }
    }

    public ExtRemoteDevice(RemoteDeviceIdentity remoteDeviceIdentity, UDAVersion version, DeviceType type, DeviceDetails details, Icon[] icons, RemoteService[] services, RemoteDevice[] remoteDevices) throws ValidationException {
        super(remoteDeviceIdentity, version, type, details, icons, services, remoteDevices);
    }

    @Override
    public RemoteDevice newInstance(UDN udn, UDAVersion version, DeviceType type, DeviceDetails details, Icon[] icons, RemoteService[] services, List<RemoteDevice> embeddedDevices) throws ValidationException {
        ExtRemoteDevice ret = new ExtRemoteDevice(new RemoteDeviceIdentity(udn, getIdentity()), version, type, details, icons, services, embeddedDevices.size() > 0 ? embeddedDevices.toArray(new RemoteDevice[embeddedDevices.size()]) : null);
        ret.headers = headers;
        return ret;
    }

}
