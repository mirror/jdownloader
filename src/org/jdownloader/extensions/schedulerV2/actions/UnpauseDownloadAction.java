package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("UNPAUSE_DOWNLOAD")
public class UnpauseDownloadAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public UnpauseDownloadAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_unpauseDownload();
    }

    @Override
    public void execute() {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(false);
    }

}
