package org.jdownloader.extensions.schedulerV2.actions;

import jd.controlling.downloadcontroller.DownloadSession.STOPMARK;
import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("SET_STOPMARK")
public class SetStopMarkAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public SetStopMarkAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_setSopMark();
    }

    @Override
    public void execute() {
        DownloadWatchDog.getInstance().getSession().setStopMark(STOPMARK.RANDOM);
    }
}
