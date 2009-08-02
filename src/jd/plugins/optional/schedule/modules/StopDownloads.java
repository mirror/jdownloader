package jd.plugins.optional.schedule.modules;

import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class StopDownloads implements SchedulerModuleInterface {
    private static final long serialVersionUID = -8723808261141140944L;

    public void execute(String parameter) {
    	JDUtilities.getController().stopDownloads();
    }

    public String getName() {
        return "plugin.optional.schedular.module.stopDownloads";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Stop Downloads");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}