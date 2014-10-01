package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class EnableReconnectAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "DISABLE_RECONNECT";
    }

    @Override
    public String getReadableName() {
        return "Disable Reconnect";
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.NONE;
    }

    @Override
    public void execute(String parameter) {
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.AUTO_RECONNECT_ENABLED.setValue(true);
    }

}
