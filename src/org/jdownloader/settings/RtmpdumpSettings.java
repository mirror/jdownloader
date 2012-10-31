package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface RtmpdumpSettings extends ConfigInterface {

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Enable debug mode, nearly everything will be logged!")
    boolean isDebugModeEnabled();

    void setDebugModeEnabled(boolean b);

}