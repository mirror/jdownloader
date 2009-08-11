package jd.plugins.optional.schedule.modules;

import jd.config.Configuration;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class EnablePremium implements SchedulerModuleInterface {
    private static final long serialVersionUID = 8621543260803605634L;

    public void execute(String parameter) {
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
        JDUtilities.getConfiguration().save();
    }

    public String getName() {
        return "plugin.optional.schedular.module.enablePremium";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Enable Premium");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}