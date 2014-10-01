package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class StopDownloadAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "STOP_DOWNLOAD";
    }

    @Override
    public String getReadableName() {
        return "Stop Download"; // TODO
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.NONE;
    }

    @Override
    public void execute(String parameter) {
        DownloadWatchDog.getInstance().stopDownloads();
    }

}
