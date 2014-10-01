package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public interface IScheduleAction {
    /**
     * NO_ACTION(0), RECONNECT(1), START_DOWNLOAD(2), STOP_DOWNLOAD(3), SET_DOWNLOADSPEED(4), SET_CHUNKS(5), SET_CONNECTIONS(6),
     * PAUSE_DOWNLOAD(7), UNPAUSE_DOWNLOAD(8), ENABLE_PREMIUM(9), ENABLE_RECONNECT(10), DISABLE_RECONNECT(11);
     * 
     * @return
     */
    public String getStorableID();

    public String getReadableName();

    public ActionParameter getParameterType();

    public void execute(String parameter);
}
