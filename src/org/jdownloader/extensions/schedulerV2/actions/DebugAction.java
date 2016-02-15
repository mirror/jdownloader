package org.jdownloader.extensions.schedulerV2.actions;

import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.swing.dialog.Dialog;

@ScheduleActionIDAnnotation("DEBUG_ACTION")
public class DebugAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {

    public DebugAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return "Debug Message";
    }

    @Override
    public void execute(LogInterface logger) {
        Dialog.I().showMessageDialog("Debug: Scheduler triggered");
    }
}
