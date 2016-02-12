package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("RECONNECT")
public class ReconnectAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public ReconnectAction(String config) {
        super(config);
    }

    @Override
    public String getReadableName() {
        return T.T.action_Reconnect();
    }

    @Override
    public void execute() {
        try {
            DownloadWatchDog.getInstance().requestReconnect(true);
        } catch (InterruptedException e) {
        }
    }

}
