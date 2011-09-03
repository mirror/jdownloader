package jd.controlling.reconnect.plugins.upnp;

import org.appwork.storage.config.ConfigInterface;
import org.jdownloader.settings.annotations.AboutConfig;

public interface UPUPReconnectSettings extends ConfigInterface {

    @AboutConfig
    String getControlURL();

    void setControlURL(String str);

    @AboutConfig
    String getFriendlyName();

    void setFriendlyName(String name);

    @AboutConfig
    String getServiceType();

    void setServiceType(String text);

    @AboutConfig
    String getWANService();

    void setWANService(String wan);

    @AboutConfig
    boolean isIPCheckEnabled();

    void setIPCheckEnabled(boolean b);
}
