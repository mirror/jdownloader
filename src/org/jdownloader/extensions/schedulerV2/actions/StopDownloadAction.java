package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("STOP_DOWNLOAD")
public class StopDownloadAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public StopDownloadAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T.T.action_stopDownload();
    }

    @Override
    public void execute(LogInterface logger) {
        DownloadWatchDog.getInstance().stopDownloads();
    }

}
