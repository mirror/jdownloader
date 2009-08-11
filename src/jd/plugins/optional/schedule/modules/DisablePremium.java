package jd.plugins.optional.schedule.modules;

import jd.config.Configuration;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class DisablePremium implements SchedulerModuleInterface {
    private static final long serialVersionUID = -6187873889631828704L;

    public void execute(String parameter) {
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, false);
        JDUtilities.getConfiguration().save();
    }

    public String getName() {
        return "plugin.optional.schedular.module.disablePremium";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Disable Premium");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}
