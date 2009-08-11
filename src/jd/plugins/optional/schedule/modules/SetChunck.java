package jd.plugins.optional.schedule.modules;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.utils.locale.JDL;


public class SetChunck implements SchedulerModuleInterface {
    private static final long serialVersionUID = -986046937528397324L;

    public void execute(String parameter) {
        SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, Integer.parseInt(parameter));
        SubConfiguration.getConfig("DOWNLOAD").save();
    }

    public String getName() {
        return "plugin.optional.schedular.module.setChuncks";
    }

    public boolean needParameter() {
        return true;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Set Chuncks");
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