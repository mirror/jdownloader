package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;
import org.jdownloader.extensions.schedulerV2.translate.T;

public class SetDownloadspeedAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "SET_DOWNLOADSPEED";
    }

    @Override
    public String getReadableName() {
        return T._.action_setDownloadspeed();
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.SPEED;
    }

    @Override
    public void execute(String parameter) {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(true);
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.setValue(Integer.parseInt(parameter));
    }
}
