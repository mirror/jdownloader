package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("START_DOWNLOAD")
public class StartDownloadAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public StartDownloadAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_startDownload();
    }

    @Override
    public void execute() {
        DownloadWatchDog.getInstance().startDownloads();
    }

}
