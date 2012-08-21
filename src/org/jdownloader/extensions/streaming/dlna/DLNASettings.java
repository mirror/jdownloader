package org.jdownloader.extensions.streaming.dlna;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface DLNASettings extends ConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isRelaxedModeEnabled();

    void setRelaxedModeEnabled(boolean b);

}
