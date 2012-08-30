package org.jdownloader.extensions.streaming.upnp;

import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.storage.config.ConfigInterface;

public interface RendererDeviceSettings extends ConfigInterface {

    void setProtocolInfos(String string);

    String getProtocolInfos();

    void setHeaders(HashMap<String, ArrayList<String>> headers);

    HashMap<String, ArrayList<String>> getHeaders();
}
