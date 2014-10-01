package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class UnpauseDownloadAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "UNPAUSE_DOWNLOADS";
    }

    @Override
    public String getReadableName() {
        return "Unpause downloads";
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.NONE;
    }

    @Override
    public void execute(String parameter) {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(false);
    }

}
