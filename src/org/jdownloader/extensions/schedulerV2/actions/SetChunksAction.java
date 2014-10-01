package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;
import org.jdownloader.extensions.schedulerV2.translate.T;

public class SetChunksAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "SET_CHUNKS";
    }

    @Override
    public String getReadableName() {
        return T._.action_setChunks();
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
