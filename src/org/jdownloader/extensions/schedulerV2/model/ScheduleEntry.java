package org.jdownloader.extensions.schedulerV2.model;

import org.jdownloader.extensions.schedulerV2.actions.IScheduleAction;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper;
import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class ScheduleEntry {

    private ScheduleEntryStorable storableEntry;
    private IScheduleAction       action;

    public ScheduleEntry(ScheduleEntryStorable storableEntry) {
        this.storableEntry = storableEntry;
        this.action = ActionHelper.getAction(storableEntry.getActionStorageID());
    }

    public ScheduleEntry() {
        this.storableEntry = new ScheduleEntryStorable();
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

    public IScheduleAction getAction() {
        return this.action;
    }

    public void setAction(IScheduleAction action, String actionParameter) {
        if (!action.getParameterType().equals(ActionParameter.NONE) && actionParameter == null) {
            System.err.println("Parameter missing!");// TODO
            return;
        }
        this.action = action;
        this.storableEntry.setActionParameter(actionParameter);
        storableEntry.setActionStorageID(action.getStorableID());
    }

    public ScheduleEntryStorable getStorable() {
        return this.storableEntry;
    }

    public String getActionParameter() {
        return storableEntry.getActionParameter();
    }

    public String getTimeType() {
        return storableEntry.getTimeType();
    }

    public void setTimeType(String timeType) {
        storableEntry.setTimeType(timeType);
    }

    public long getTimestamp() {
        return storableEntry.getTimestamp();
    }

    public void setTimestamp(long timestamp) {
        storableEntry.setTimestamp(timestamp);
    }

    public int getIntervalMin() {
        return storableEntry.getIntervalMin();
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
}
