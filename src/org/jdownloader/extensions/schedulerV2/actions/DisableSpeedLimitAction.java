package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;
import org.jdownloader.extensions.schedulerV2.translate.T;

public class DisableSpeedLimitAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "DISABLE_SPEEDLIMIT";
    }

    @Override
    public String getReadableName() {
        return T._.action_disableSpeedLimit();
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.NONE;
    }

    @Override
    public void execute(String parameter) {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(false);
    }
}
