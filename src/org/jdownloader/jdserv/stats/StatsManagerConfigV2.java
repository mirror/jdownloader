package org.jdownloader.jdserv.stats;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface StatsManagerConfigV2 extends ConfigInterface {

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isEnabled();

    void setEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    void setAlwaysAllowLogUploads(boolean dontShowAgainSelected);

    boolean isAlwaysAllowLogUploads();

}
