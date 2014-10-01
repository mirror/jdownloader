package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;

public class PauseDownloadAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "PAUSE_DOWNLOADS";
    }

    @Override
    public String getReadableName() {
        return "Pause downloads";
    }

    @Override
    public ActionParameter getParameterType() {
        return ActionParameter.NONE;
    }

    @Override
    public void execute(String parameter) {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(true);
    }

}
