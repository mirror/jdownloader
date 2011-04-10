package org.jdownloader.extensions.schedule.modules;

import jd.controlling.JDController;
import jd.update.JDUpdateUtils;

import org.jdownloader.extensions.schedule.SchedulerModule;
import org.jdownloader.extensions.schedule.SchedulerModuleInterface;
import org.jdownloader.extensions.schedule.translate.T;

@SchedulerModule
public class DoBackup implements SchedulerModuleInterface {

    private static final long serialVersionUID = 1056431689595464918L;

    public boolean checkParameter(String parameter) {
        return false;
    }

    public void execute(String parameter) {
        JDController.getInstance().syncDatabase();
        JDUpdateUtils.backupDataBase();
    }

    public String getTranslation() {
        return T._.jd_plugins_optional_schedule_modules_doBackup();
    }

    public boolean needParameter() {
        return false;
    }

}