package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("START_DOWNLOAD")
public class StartDownloadAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public StartDownloadAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T.T.action_startDownload();
    }

    @Override
    public void execute(LogInterface logger) {
        DownloadWatchDog.getInstance().startDownloads();
    }

}
