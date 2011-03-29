package jd.plugins;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface PlugionOptionalConfig extends ConfigInterface {

    void setEnabled(boolean ret);

    boolean isEnabled();

    void setGuiEnabled(boolean b);

    boolean isGuiEnabled();

    @DefaultBooleanValue(true)
    boolean isFreshInstall();

    void setFreshInstall(boolean b);

}
