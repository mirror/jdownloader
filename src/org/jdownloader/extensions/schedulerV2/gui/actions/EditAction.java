package org.jdownloader.extensions.schedulerV2.gui.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.extensions.schedulerV2.gui.AddScheduleEntryDialog;
import org.jdownloader.extensions.schedulerV2.gui.SchedulerTable;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.AbstractAddAction;

public class EditAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long    serialVersionUID = 1L;
    private final SchedulerTable table;

    public EditAction(SchedulerTable table) {
        super();
        this.table = table;
        setName(_GUI._.literally_edit());
        setIconKey(IconKey.ICON_EDIT);
    }

    public void actionPerformed(ActionEvent e) {
        List<ScheduleEntry> selected = table.getModel().getSelectedObjects();
        if (selected.size() != 1) {
            return;
        }
        ScheduleEntry toBeEdited = selected.get(0);
        long old = toBeEdited.getID();
        final AddScheduleEntryDialog dialog = new AddScheduleEntryDialog(toBeEdited);

        try {
            ScheduleEntry entry = Dialog.getInstance().showDialog(dialog);
            if (entry != null) {
                table.getExtension().replaceScheduleEntry(old, entry);
            }
        } catch (DialogNoAnswerException e2) {
            e2.printStackTrace();
        }
    }
}
