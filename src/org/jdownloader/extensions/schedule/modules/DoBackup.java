package org.jdownloader.extensions.schedule.modules;

import org.jdownloader.extensions.schedule.SchedulerModule;
import org.jdownloader.extensions.schedule.SchedulerModuleInterface;

import jd.controlling.JDController;
import jd.update.JDUpdateUtils;
import jd.utils.locale.JDL;

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
        return JDL.L("jd.plugins.optional.schedule.modules.doBackup", "Do Backup");
    }

    public boolean needParameter() {
        return false;
    }

}
