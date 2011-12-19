package org.jdownloader.extensions.schedule.modules;

import org.jdownloader.extensions.schedule.SchedulerModule;
import org.jdownloader.extensions.schedule.SchedulerModuleInterface;
import org.jdownloader.extensions.schedule.translate.T;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

@SchedulerModule
public class EnableClipboard implements SchedulerModuleInterface {

    private static final long serialVersionUID = 7131982059931117440L;

    public boolean checkParameter(String parameter) {
        return false;
    }

    public void execute(String parameter) {
        GraphicalUserInterfaceSettings.CLIPBOARD_MONITORED.setValue(true);
    }

    public String getTranslation() {
        return T._.jd_plugins_optional_schedule_modules_enableClipboard();
    }

    public boolean needParameter() {
        return false;
    }

}