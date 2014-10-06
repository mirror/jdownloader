package org.jdownloader.extensions.schedulerV2.actions;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("DISABLE_RECONNECT")
public class DisableReconnectAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public DisableReconnectAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_disableReconnect();
    }

    @Override
    public void execute() {
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.AUTO_RECONNECT_ENABLED.setValue(false);
    }

}
