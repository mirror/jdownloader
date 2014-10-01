package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class SetChunksAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "SET_CONNECTIONS";
    }

    @Override
    public String getReadableName() {
        return "Set Connections";
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.INT;
    }

    @Override
    public void execute(String parameter) {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_CHUNKS_PER_FILE.setValue(Integer.parseInt(parameter));
    }
}
