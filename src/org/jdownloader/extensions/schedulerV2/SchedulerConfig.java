package org.jdownloader.extensions.schedulerV2;

import java.util.List;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;

public interface SchedulerConfig extends ExtensionConfigInterface {

    @AboutConfig
    @DescriptionForConfigEntry("Contains all schedule plans")
    @DefaultJsonObject("[]")
    List<ScheduleEntryStorable> getEntryList();

    void setEntryList(List<ScheduleEntryStorable> e);

    @AboutConfig
    @DescriptionForConfigEntry("Adds a Debug Action (Message). Requires JD restart.")
    @DefaultJsonObject("false")
    boolean isDebugMode();

    void setDebugMode(boolean debug);
}
