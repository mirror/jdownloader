package jd.plugins.optional.schedule.modules;

import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class UnPauseDownloads implements SchedulerModuleInterface {
    private static final long serialVersionUID = 2359797425294554626L;

    public void execute(String parameter) {
        JDUtilities.getController().getWatchdog().pause(false);
    }

    public String getName() {
        return "plugin.optional.schedular.module.unpauseDownloads";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Unpause Downloads");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}