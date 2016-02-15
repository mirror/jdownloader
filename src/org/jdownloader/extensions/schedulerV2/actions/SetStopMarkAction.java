package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadSession.STOPMARK;
import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("SET_STOPMARK")
public class SetStopMarkAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public SetStopMarkAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T.T.action_setSopMark();
    }

    @Override
    public void execute(LogInterface logger) {
        DownloadWatchDog.getInstance().setStopMark(STOPMARK.RANDOM);
    }
}
