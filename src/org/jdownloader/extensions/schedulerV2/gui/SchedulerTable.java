package org.jdownloader.extensions.schedulerV2.gui;

import java.awt.event.KeyEvent;
import java.util.List;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtTableModel;
import org.jdownloader.extensions.schedulerV2.gui.actions.RemoveAction;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;

public class SchedulerTable extends BasicJDTable<ScheduleEntry> {

    public SchedulerTable(ExtTableModel<ScheduleEntry> tableModel) {
        super(tableModel);
    }

    /**
     * 
     */
    private static final long serialVersionUID = -4807977628054210618L;

    @Override
    protected boolean onShortcutDelete(List<ScheduleEntry> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(this).actionPerformed(null);
        return true;
    }

}
