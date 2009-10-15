package jd.plugins.optional.schedule.modules;

import jd.controlling.DownloadWatchDog;
import jd.utils.locale.JDL;

public class SetStopMark implements SchedulerModuleInterface {
    private static final long serialVersionUID = -6187873889631828704L;

    public void execute(String parameter) {
        if (DownloadWatchDog.getInstance().getActiveDownloads() > 0) {
            Object obj = DownloadWatchDog.getInstance().getRunningDownloads().get(0);
            DownloadWatchDog.getInstance().setStopMark(obj);
        }
    }

    public String getName() {
        return "plugin.optional.schedular.module.setstopmark";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Set StopMark");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}