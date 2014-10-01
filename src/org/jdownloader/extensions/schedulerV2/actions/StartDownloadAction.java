package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class StartDownloadAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "START_DOWNLOAD";
    }

    @Override
    public String getReadableName() {
        return "Start Download"; // TODO
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.NONE;
    }

    @Override
    public void execute(String parameter) {
        DownloadWatchDog.getInstance().startDownloads();
    }

}
