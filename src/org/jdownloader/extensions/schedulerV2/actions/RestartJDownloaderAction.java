package org.jdownloader.extensions.schedulerV2.actions;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.schedulerV2.translate.T;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyRestartRequest;

@ScheduleActionIDAnnotation("RESTART")
public class RestartJDownloaderAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {
    public RestartJDownloaderAction(String config) {
        super(config);
    }

    @Override
    public String getReadableName() {
        return T.T.action_Restart_JDownloader();
    }

    @Override
    public void execute(LogInterface logger) {
        RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
    }
}
