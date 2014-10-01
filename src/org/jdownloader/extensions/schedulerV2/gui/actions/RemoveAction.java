package org.jdownloader.extensions.schedulerV2.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.schedulerV2.CFG_SCHEDULER;
import org.jdownloader.extensions.schedulerV2.gui.SchedulerTable;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;
import org.jdownloader.extensions.schedulerV2.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class RemoveAction extends AppAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private SchedulerTable      table;
    private boolean             force            = false;
    private List<ScheduleEntry> selection        = null;

    public RemoveAction(SchedulerTable table) {
        this.table = table;
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    public RemoveAction(List<ScheduleEntry> selection2, boolean force) {
        this.force = force;
        this.selection = selection2;
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        final List<ScheduleEntry> finalSelection;
        if (selection != null) {
            finalSelection = selection;
        } else if (table != null) {
            finalSelection = table.getModel().getSelectedObjects();
        } else {
            finalSelection = null;
        }
        // to prevent that objects are selected while removing -> would throw error
        // table.getModel().setSelectedObject(null);

        if (finalSelection != null && finalSelection.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (ScheduleEntry rule : finalSelection) {
                if (sb.length() > 0) {
                    sb.append("\r\n");
                }
                sb.append("\"");
                sb.append(rule.getName());
                sb.append("\"");
            }
            try {
                if (!force) {
                    Dialog.getInstance().showConfirmDialog(Dialog.STYLE_LARGE, T._.entry_remove_action_title(finalSelection.size()), T._.entry_remove_action_msg(finalSelection.size() <= 1 ? sb.toString() : "\r\n" + sb.toString()));
                }

                ArrayList<ScheduleEntryStorable> scheduleRules = CFG_SCHEDULER.CFG.getEntryList();
                for (ScheduleEntry entry : finalSelection) {
                    scheduleRules.remove(entry.getStorable());
                }
                CFG_SCHEDULER.CFG.setEntryList(new ArrayList<ScheduleEntryStorable>(scheduleRules));

            } catch (DialogNoAnswerException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        if (this.table != null) {
            return super.isEnabled();
        }
        return (selection != null && selection.size() > 0);
    }

}
