package org.jdownloader.extensions.schedulerV2.gui.actions;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.schedulerV2.gui.SchedulerTable;
import org.jdownloader.extensions.schedulerV2.helpers.ActionHelper.WEEKDAY;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntryStorable;
import org.jdownloader.extensions.schedulerV2.translate.T;
import org.jdownloader.gui.IconKey;

public class CopyAction extends AppAction {

    private SchedulerTable      table;
    private List<ScheduleEntry> selection = null;

    public CopyAction(SchedulerTable table) {
        this.table = table;
        setName(T._.lit_copy());
        setIconKey(IconKey.ICON_COPY);
    }

    public CopyAction(List<ScheduleEntry> selection2) {
        this.selection = selection2;
        setName(T._.lit_copy());
        setIconKey(IconKey.ICON_COPY);
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

        if (finalSelection != null && finalSelection.size() > 0) {

            for (ScheduleEntry rule : finalSelection) {
                table.getExtension().addScheduleEntry(copy(rule));
            }

        }
    }

    private ScheduleEntry copy(ScheduleEntry rule) {
        ScheduleEntryStorable copyStorable = new ScheduleEntryStorable();
        copyStorable.setEnabled(rule.isEnabled());
        copyStorable.setIntervalHour(rule.getIntervalHour());
        copyStorable.setIntervalMin(rule.getIntervalMinunte());
        copyStorable.setName(rule.getName());
        copyStorable._setSelectedDays(new LinkedList<WEEKDAY>(rule.getSelectedDays()));
        copyStorable.setTimestamp(rule.getTimestamp());
        copyStorable._setTimeType(rule.getTimeType());
        copyStorable.setActionID(rule.getAction().getActionID());
        copyStorable.setActionConfig(rule.getStorable().getActionConfig() + "");
        try {
            ScheduleEntry copy = new ScheduleEntry(copyStorable);
            return copy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public boolean isEnabled() {
        if (this.table != null) {
            return super.isEnabled();
        }
        return (selection != null && selection.size() > 0);
    }

}
