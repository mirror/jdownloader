package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public interface IScheduleAction {

    public String getStorableID();

    public String getReadableName();

    public ActionParameter getParameterType();

    public void execute(String parameter);
}
