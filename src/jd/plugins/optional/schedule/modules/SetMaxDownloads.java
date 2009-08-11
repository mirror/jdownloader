package jd.plugins.optional.schedule.modules;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.utils.locale.JDL;

public class SetMaxDownloads implements SchedulerModuleInterface {
    private static final long serialVersionUID = 9151617805665511866L;

    public void execute(String parameter) {
        SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, Integer.parseInt(parameter));
        SubConfiguration.getConfig("DOWNLOAD").save();
    }

    public String getName() {
        return "plugin.optional.schedular.module.setMaxDownloads";
    }

    public boolean needParameter() {
        return true;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Set max Downloads");
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