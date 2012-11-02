package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface RtmpdumpSettings extends ConfigInterface {

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Enable rtmpdump debug mode")
    boolean isRtmpDumpDebugModeEnabled();

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Enable flvfixer debug mode. Beware, log file can be rather large!")
    boolean isFlvFixerDebugModeEnabled();

    void setRtmpDumpDebugModeEnabled(boolean b);

    void setFlvFixerDebugModeEnabled(boolean b);

}