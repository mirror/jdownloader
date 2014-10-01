package org.jdownloader.extensions.schedulerV2;

import java.util.ArrayList;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;

public interface SchedulerConfig extends ExtensionConfigInterface {

    @AboutConfig
    @DefaultJsonObject("[]")
    ArrayList<ScheduleEntryStorable> getEntryList();

    void setEntryList(ArrayList<ScheduleEntryStorable> e);
}
