package jd.plugins.optional.schedule.modules;

import jd.controlling.reconnect.Reconnecter;
import jd.utils.locale.JDL;

public class DoReconnect implements SchedulerModuleInterface {
    private static final long serialVersionUID = 1782904765267434004L;

    public boolean checkParameter(String parameter) {
        return true;
    }

    public void execute(String parameter) {
        Reconnecter.doManualReconnect();
    }

    public String getName() {
        return "plugin.optional.schedular.module.doReconnect";
    }

    public String getTranslation() {
        return JDL.L(getName(), "Do Reconnect");
    }

    public boolean needParameter() {
        return false;
    }
}