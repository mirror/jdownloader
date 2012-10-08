package org.jdownloader.statistics;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface StatsManagerConfig extends ConfigInterface {

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isEnabled();

    void setEnabled(boolean b);

}
