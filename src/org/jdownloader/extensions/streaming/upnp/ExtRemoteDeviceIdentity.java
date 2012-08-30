package org.jdownloader.extensions.streaming.upnp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.fourthline.cling.model.message.discovery.IncomingNotificationRequest;
import org.fourthline.cling.model.message.discovery.IncomingSearchResponse;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;

public class ExtRemoteDeviceIdentity extends RemoteDeviceIdentity {

    private HashMap<String, ArrayList<String>> headers;

    public ExtRemoteDeviceIdentity(IncomingNotificationRequest inputMessage) {
        super(inputMessage);
        this.headers = new HashMap<String, ArrayList<String>>();
        for (Entry<String, ArrayList<String>> h : headers.entrySet()) {
            headers.put(h.getKey(), h.getValue());
        }

    }

    public ExtRemoteDeviceIdentity(IncomingSearchResponse inputMessage) {
        super(inputMessage);
        this.headers = new HashMap<String, ArrayList<String>>();
        for (Entry<String, ArrayList<String>> h : headers.entrySet()) {
            headers.put(h.getKey(), h.getValue());
        }
    }

    public HashMap<String, ArrayList<String>> getHeaders() {
        return headers;
    }

}
