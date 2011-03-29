package jd.plugins;

import org.appwork.storage.config.ConfigInterface;

public interface PlugionOptionalConfig extends ConfigInterface {

    void setEnabled(boolean ret);

    boolean isEnabled();

    void setGuiEnabled(boolean b);

    boolean isGuiEnabled();

}
