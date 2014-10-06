package org.jdownloader.extensions.schedulerV2.model;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.TIME_OPTIONS;

public class ScheduleEntryStorable implements Storable {

    public ScheduleEntryStorable(/* Storable */) {
    }

    private long id = new UniqueAlltimeID().getID();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private boolean enabled  = true;
    private String  name;
    // ID for action
    private String  actionID = null;

    public String getActionID() {
        return actionID;
    }

    public void setActionID(String actionID) {
        this.actionID = actionID;
    }

    private String actionConfig = null;

    public String getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(String actionConfig) {
        this.actionConfig = actionConfig;
    }

    // Time
    private String timeType       = TIME_OPTIONS.ONLYONCE.name();
    private long   timestamp      = 0;
    private int    intervalMinute = 0;
    private int    intervalHour   = 0;

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

    public TIME_OPTIONS _getTimeType() {
        try {
            return TIME_OPTIONS.valueOf(timeType);
        } catch (final Exception e) {
            return TIME_OPTIONS.ONLYONCE;
        }
    }

    public void _setTimeType(TIME_OPTIONS timeType) {
        if (timeType == null) {
            timeType = TIME_OPTIONS.ONLYONCE;
        }
        this.timeType = timeType.name();
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

    public int getIntervalMinute() {
        return intervalMinute;
    }

    public void setIntervalMin(int intervalMinute) {
        this.intervalMinute = intervalMinute;
    }

    public int getIntervalHour() {
        return intervalHour;
    }

    public void setIntervalHour(int intervalHour) {
        this.intervalHour = intervalHour;
    }

}
