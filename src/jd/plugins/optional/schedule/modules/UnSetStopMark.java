package jd.plugins.optional.schedule.modules;

import jd.controlling.DownloadWatchDog;
import jd.utils.locale.JDL;

public class UnSetStopMark implements SchedulerModuleInterface {
    private static final long serialVersionUID = -6187873889631828704L;

    public void execute(String parameter) {
        if (DownloadWatchDog.getInstance().isStopMarkSet()) {
            DownloadWatchDog.getInstance().setStopMark(null);
        }
    }

    public String getName() {
        return "plugin.optional.schedular.module.unsetstopmark";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Unset StopMark");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}