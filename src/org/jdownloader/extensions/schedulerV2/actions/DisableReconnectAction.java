package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class DisableReconnectAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "ENABLE_RECONNECT";
    }

    @Override
    public String getReadableName() {
        return "Enable Reconnect";
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.NONE;
    }

    @Override
    public void execute(String parameter) {
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.AUTO_RECONNECT_ENABLED.setValue(false);
    }

}
