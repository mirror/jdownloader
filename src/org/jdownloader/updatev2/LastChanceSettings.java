package org.jdownloader.updatev2;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface LastChanceSettings extends ConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("LastChance system is meant to help to rescue a broken JDownloader installation")
    boolean isBackgroundLastChanceEnabled();

    void setBackgroundLastChanceEnabled(boolean enabled);
}
