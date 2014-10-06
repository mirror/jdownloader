package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("DISABLE_SPEEDLIMIT")
public class DisableSpeedLimitAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public DisableSpeedLimitAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_disableSpeedLimit();
    }

    @Override
    public void execute() {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(false);
    }
}
