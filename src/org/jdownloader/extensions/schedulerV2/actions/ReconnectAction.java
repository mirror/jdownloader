package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.reconnect.Reconnecter;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("RECONNECT")
public class ReconnectAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public ReconnectAction(String config) {
        super(config);
    }

    @Override
    public String getReadableName() {
        return T._.action_Reconnect();
    }

    @Override
    public void execute() {
        Reconnecter.getInstance().doReconnect();
    }

}
