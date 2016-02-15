package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.reconnect.Reconnecter.ReconnectResult;

import org.appwork.utils.logging2.LogInterface;
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
    public void execute(LogInterface logger) {
        try {
            logger.info("Request Reconnect");
            final ReconnectResult result = DownloadWatchDog.getInstance().requestReconnect(true);
            logger.info("Reconnect Result:" + result);
        } catch (InterruptedException e) {
            logger.log(e);
        }
    }

}
