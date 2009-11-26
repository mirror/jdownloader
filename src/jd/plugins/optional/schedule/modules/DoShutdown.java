package jd.plugins.optional.schedule.modules;

import jd.OptionalPluginWrapper;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class DoShutdown implements SchedulerModuleInterface {

    private static final long serialVersionUID = 7232503485324370368L;

    public boolean checkParameter(String parameter) {
        return false;
    }

    public void execute(String parameter) {
        
        OptionalPluginWrapper addon = JDUtilities.getOptionalPlugin("shutdown");
        addon.getPlugin().interact("shutdown", null);
    }

    public String getTranslation() {
        return JDL.L("jd.plugins.optional.schedule.modules.doShutdown", "Do Shutdown");
    }

    public boolean needParameter() {
        return false;
    }

}
