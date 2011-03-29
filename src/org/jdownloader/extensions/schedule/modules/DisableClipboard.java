package org.jdownloader.extensions.schedule.modules;

import org.jdownloader.extensions.schedule.SchedulerModule;
import org.jdownloader.extensions.schedule.SchedulerModuleInterface;

import jd.controlling.ClipboardHandler;
import jd.utils.locale.JDL;

@SchedulerModule
public class DisableClipboard implements SchedulerModuleInterface {

    private static final long serialVersionUID = -8088061436921363098L;

    public boolean checkParameter(String parameter) {
        return false;
    }

    public void execute(String parameter) {
        ClipboardHandler.getClipboard().setEnabled(false);
    }

    public String getTranslation() {
        return JDL.L("jd.plugins.optional.schedule.modules.disableClipboard", "Disable Clipboard Monitoring");
    }

    public boolean needParameter() {
        return false;
    }

}
