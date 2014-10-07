package org.jdownloader.extensions.schedulerV2.gui;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.jdownloader.extensions.schedulerV2.SchedulerExtension;
import org.jdownloader.extensions.schedulerV2.gui.actions.EditAction;
import org.jdownloader.extensions.schedulerV2.gui.actions.NewAction;
import org.jdownloader.extensions.schedulerV2.gui.actions.RemoveAction;
import org.jdownloader.extensions.schedulerV2.model.ScheduleEntry;

public class SchedulerTable extends BasicJDTable<ScheduleEntry> {

    private final SchedulerExtension extension;

    public SchedulerExtension getExtension() {
        return extension;
    }

    public SchedulerTable(SchedulerExtension extension, ExtTableModel<ScheduleEntry> tableModel) {
        super(tableModel);
        this.extension = extension;
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

    @Override
    protected boolean onDoubleClick(MouseEvent e, ScheduleEntry obj) {
        new EditAction(this).actionPerformed(null);
        return true;
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, ScheduleEntry contextObject, List<ScheduleEntry> selection, ExtColumn<ScheduleEntry> column, MouseEvent mouseEvent) {
        if (popup != null) {
            popup.add(new NewAction(this));
            if (selection != null && selection.size() >= 1) {
                popup.add(new RemoveAction(this));
            }
            if (selection != null && selection.size() == 1) {
                popup.add(new EditAction(this));
            }
        }
        return popup;
    }
}
