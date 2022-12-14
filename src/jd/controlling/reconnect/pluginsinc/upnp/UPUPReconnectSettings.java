package jd.controlling.reconnect.pluginsinc.upnp;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface UPUPReconnectSettings extends ConfigInterface {
    @AboutConfig
    String getControlURL();

    void setControlURL(String str);

    @AboutConfig
    String getModelName();

    void setModelName(String name);

    @AboutConfig
    String getServiceType();

    void setServiceType(String text);

    @AboutConfig
    String getWANService();

    void setWANService(String wan);

    @AboutConfig
    boolean isIPCheckEnabled();

    void setIPCheckEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(0)
    @SpinnerValidator(min = 0, max = 5 * 60 * 1000, step = 50)
    int getFirstReconnectWaitTimeout();

    void setFirstReconnectWaitTimeout(int wait);
}
