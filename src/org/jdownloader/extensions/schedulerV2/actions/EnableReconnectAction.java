package org.jdownloader.extensions.schedulerV2.actions;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("ENABLE_RECONNECT")
public class EnableReconnectAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public EnableReconnectAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T.T.action_enableReconnect();
    }

    @Override
    public void execute(LogInterface logger) {
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.AUTO_RECONNECT_ENABLED.setValue(true);
    }

}
