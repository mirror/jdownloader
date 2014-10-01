package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;
import org.jdownloader.extensions.schedulerV2.translate.T;

public class SetConnectionsAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "SET_CONNECTIONS";
    }

    @Override
    public String getReadableName() {
        return T._.action_setConnections();
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
