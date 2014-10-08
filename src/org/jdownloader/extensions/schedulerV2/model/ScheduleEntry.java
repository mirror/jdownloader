package org.jdownloader.extensions.schedulerV2.model;

import java.util.List;

import org.jdownloader.extensions.schedulerV2.actions.AbstractScheduleAction;
import org.jdownloader.extensions.schedulerV2.actions.IScheduleActionConfig;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.TIME_OPTIONS;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.WEEKDAY;

public class ScheduleEntry {

    private final ScheduleEntryStorable                         storableEntry;
    private final AbstractScheduleAction<IScheduleActionConfig> action;

    public ScheduleEntry(ScheduleEntryStorable storableEntry) throws Exception {
        this.storableEntry = storableEntry;
        this.action = ActionHelper.newActionInstance(storableEntry);
    }

    public String getName() {
        return storableEntry.getName();
    }

    public void setName(String name) {
        storableEntry.setName(name);
    }

    public void setEnabled(boolean enabled) {
        storableEntry.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return storableEntry.isEnabled();
    }

    public AbstractScheduleAction getAction() {
        return this.action;
    }

    public ScheduleEntryStorable getStorable() {
        return this.storableEntry;
    }

    public TIME_OPTIONS getTimeType() {
        return storableEntry._getTimeType();
    }

    public void setTimeType(TIME_OPTIONS timeType) {
        storableEntry._setTimeType(timeType);
    }

    public long getTimestamp() {
        return storableEntry.getTimestamp();
    }

    public void setTimestamp(long timestamp) {
        storableEntry.setTimestamp(timestamp);
    }

    public int getIntervalMinunte() {
        return storableEntry.getIntervalMinute();
    }

    public void setIntervalMin(int intervalMin) {
        storableEntry.setIntervalMin(intervalMin);
    }

    public int getIntervalHour() {
        return storableEntry.getIntervalHour();
    }

    public void setIntervalHour(int intervalHour) {
        storableEntry.setIntervalHour(intervalHour);
    }

    public long getID() {
        return storableEntry.getId();
    }

    public List<WEEKDAY> getSelectedDays() {
        return storableEntry._getSelectedDays();
    }

    public void setSelectedDays(List<WEEKDAY> days) {
        storableEntry._setSelectedDays(days);
    }
}
