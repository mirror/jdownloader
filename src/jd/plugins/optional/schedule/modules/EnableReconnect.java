package jd.plugins.optional.schedule.modules;

import jd.config.Configuration;
import jd.controlling.reconnect.Reconnecter;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class EnableReconnect implements SchedulerModuleInterface {
    private static final long serialVersionUID = 2009958114124145334L;

    public void execute(String parameter) {
        if(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true))
            Reconnecter.toggleReconnect();
    }

    public String getName() {
        return "plugin.optional.schedular.module.enableReconnect";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Enable Reconnect");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}