package jd.plugins.optional.schedule.modules;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.utils.locale.JDL;

public class SetSpeed implements SchedulerModuleInterface {
    private static final long serialVersionUID = -6026889777421088500L;

    public void execute(String parameter) {
    	SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, Integer.valueOf(parameter));
    	SubConfiguration.getConfig("DOWNLOAD").save();
    }

    public String getName() {
        return "plugin.optional.schedular.module.setDownloadSpeed";
    }

    public boolean needParameter() {
        return true;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Set Downloadspeed");
    }

    public boolean checkParameter(String parameter) {
        try {
            Integer.parseInt(parameter);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
}