package org.jdownloader.extensions.schedulerV2.actions;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("DISABLE_RECONNECT")
public class DisableReconnectAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public DisableReconnectAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T.T.action_disableReconnect();
    }

    @Override
    public void execute(LogInterface logger) {
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.AUTO_RECONNECT_ENABLED.setValue(false);
    }

}
