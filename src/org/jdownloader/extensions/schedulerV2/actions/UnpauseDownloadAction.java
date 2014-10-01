package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.helpers.ActionParameter;
import org.jdownloader.extensions.schedulerV2.translate.T;

public class UnpauseDownloadAction implements IScheduleAction {

    @Override
    public String getStorableID() {
        return "UNPAUSE_DOWNLOAD";
    }

    @Override
    public String getReadableName() {
        return T._.action_unpauseDownload();
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
