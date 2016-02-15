package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("PAUSE_DOWNLOADS")
public class PauseDownloadAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public PauseDownloadAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T.T.action_pauseDownloads();
    }

    @Override
    public void execute(LogInterface logger) {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(true);
    }

}
