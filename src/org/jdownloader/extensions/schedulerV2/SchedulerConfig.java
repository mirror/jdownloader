package org.jdownloader.extensions.schedulerV2;

import java.util.ArrayList;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;

public interface SchedulerConfig extends ExtensionConfigInterface {

    @AboutConfig
    @DescriptionForConfigEntry("Blbalblabla is activated")
    @DefaultBooleanValue(true)
    boolean isBlablablaEnabled();

    void setBlablablaEnabled(boolean ms);

    @AboutConfig
    @DefaultJsonObject("[]")
    ArrayList<ScheduleEntryStorable> getEntryList();

    void setEntryList(ArrayList<ScheduleEntryStorable> e);
}
