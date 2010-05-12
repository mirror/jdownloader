package jd.plugins.optional.schedule.modules;

import jd.OptionalPluginWrapper;
import jd.controlling.JDLogger;
import jd.plugins.optional.schedule.SchedulerModule;
import jd.plugins.optional.schedule.SchedulerModuleInterface;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@SchedulerModule
public class DoSleep implements SchedulerModuleInterface {

    private static final long serialVersionUID = 7232503485324370368L;

    public boolean checkParameter(String parameter) {
        return false;
    }

    public void execute(String parameter) {

        OptionalPluginWrapper addon = JDUtilities.getOptionalPlugin("shutdown");
        if (addon == null) {
            JDLogger.getLogger().info("JDShutdown addon not loaded! Cannot sleep!");
            return;
        }
        addon.getPlugin().interact("Sleep", null);
    }

    public String getTranslation() {
        return JDL.L("jd.plugins.optional.schedule.modules.doSleep", "Do Sleep");
    }

    public boolean needParameter() {
        return false;
    }

}
