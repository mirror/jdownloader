package org.jdownloader.extensions.streaming.upnp;

import java.util.HashMap;
import java.util.List;

import org.appwork.storage.config.ConfigInterface;

public interface RendererDeviceSettings extends ConfigInterface {

    void setProtocolInfos(String string);

    String getProtocolInfos();

    void setHeaders(HashMap<String, List<String>> headers);

    HashMap<String, List<String>> getHeaders();

    void setDisplayString(String displayString);

    String getDisplayString();

    void setUDN(String UDN);

    String getUDN();

    void setDescriptorURL(String string);

    String getDescriptorURL();

    void setIconPath(String relativePath);

    String getIconPath();

    void setServerName(String list);

    String getServerName();
}
