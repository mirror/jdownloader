package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("STOP_DOWNLOAD")
public class StopDownloadAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public StopDownloadAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_stopDownload();
    }

    @Override
    public void execute() {
        DownloadWatchDog.getInstance().stopDownloads();
    }

}
