package jd.plugins.optional.schedule.modules;

import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class StartDownloads implements SchedulerModuleInterface {
    private static final long serialVersionUID = -6031730798379292357L;

    public void execute(String parameter) {
        JDUtilities.getController().startDownloads();
    }

    public String getName() {
        return "plugin.optional.schedular.module.startDownloads";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Start Downloads");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}