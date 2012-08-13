package org.jdownloader.extensions.streaming.upnp;

import org.appwork.storage.config.ConfigInterface;

public interface RendererDeviceSettings extends ConfigInterface {

    void setProtocolInfos(String string);

    String getProtocolInfos();

}
