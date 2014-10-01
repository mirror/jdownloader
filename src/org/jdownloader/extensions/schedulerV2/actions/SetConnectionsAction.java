package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class SetConnectionsAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "SET_CHUNKS";
    }

    @Override
    public String getReadableName() {
        return "Set Chunks";
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.INT;
    }

    @Override
    public void execute(String parameter) {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.setValue(Integer.parseInt(parameter));
    }
}
