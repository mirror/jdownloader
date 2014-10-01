package org.jdownloader.extensions.schedulerV2.model;

import org.appwork.storage.Storable;

public class ScheduleEntryStorable implements Storable {
    public ScheduleEntryStorable(/* Storable */) {
    }

    private boolean enabled         = true;
    private String  name;
    // ID for action
    private String  actionStorageID = "";
    private String  actionParameter = null;
    // Time
    private String  timeType        = "ONLYONCE";
    private long    timestamp       = 0;
    private int     intervalMin     = 0;
    private int     intervalHour    = 0;
    private int     intervalDay     = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActionStorageID() {
        return actionStorageID;
    }

    public void setActionStorageID(String actionStorageID) {
        this.actionStorageID = actionStorageID;
    }

    public String getActionParameter() {
        return actionParameter;
    }

    public void setActionParameter(String actionParameter) {
        this.actionParameter = actionParameter;
    }

    public String getTimeType() {
        return timeType;
    }

    public void setTimeType(String timeType) {
        this.timeType = timeType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getIntervalMin() {
        return intervalMin;
    }

    public void setIntervalMin(int intervalMin) {
        this.intervalMin = intervalMin;
    }

    public int getIntervalHour() {
        return intervalHour;
    }

    public void setIntervalHour(int intervalHour) {
        this.intervalHour = intervalHour;
    }

}
