package jd.plugins.optional.schedule.modules;

import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class PauseDownloads implements SchedulerModuleInterface {
    private static final long serialVersionUID = -3871092307047328210L;

    public void execute(String parameter) {
        JDUtilities.getController().getWatchdog().pause(true);
    }

    public String getName() {
        return "plugin.optional.schedular.module.pauseDownloads";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Pause Downloads");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}