package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("PAUSE_DOWNLOADS")
public class PauseDownloadAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public PauseDownloadAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_pauseDownloads();
    }

    @Override
    public void execute() {

        DownloadWatchDog.getInstance().pauseDownloadWatchDog(true);
    }

}
