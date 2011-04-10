package org.jdownloader.extensions.schedule.modules;

import jd.controlling.ClipboardHandler;

import org.jdownloader.extensions.schedule.SchedulerModule;
import org.jdownloader.extensions.schedule.SchedulerModuleInterface;
import org.jdownloader.extensions.schedule.translate.T;

@SchedulerModule
public class EnableClipboard implements SchedulerModuleInterface {

    private static final long serialVersionUID = 7131982059931117440L;

    public boolean checkParameter(String parameter) {
        return false;
    }

    public void execute(String parameter) {
        ClipboardHandler.getClipboard().setEnabled(true);
    }

    public String getTranslation() {
        return T._.jd_plugins_optional_schedule_modules_enableClipboard();
    }

    public boolean needParameter() {
        return false;
    }

}