package jd.gui.swing.jdgui;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableIcon;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class LockAllColumnsAction extends AppAction {
    private BasicJDTable<?> table;

    public LockAllColumnsAction(BasicJDTable<?> table) {
        super();
        boolean alllocked = true;
        for (ExtColumn<?> c : table.getModel().getColumns()) {
            if (c.isResizable()) {
                alllocked = false;
                break;
            }
        }
        if (alllocked) {
            setName(_GUI.T.LockAllColumnsAction_LockAllColumnsAction_unlockall_columns_());
        } else {
            setName(_GUI.T.LockAllColumnsAction_LockAllColumnsAction_lockall_columns_());
        }
        this.putValue(Action.SMALL_ICON, ExtTableIcon.TABLE_LOCK_COLUMN.get(table.getContextIconSize()));
        this.table = table;
        ;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        boolean alllocked = true;
        for (ExtColumn<?> c : table.getModel().getColumns()) {
            if (c.isResizable()) {
                alllocked = false;
                break;
            }
        }
        if (alllocked) {
            for (ExtColumn<?> c : table.getModel().getColumns()) {
                c.setResizable(true);
            }
        } else {
            for (ExtColumn<?> c : table.getModel().getColumns()) {
                c.setResizable(false);
            }
        }
    }
}
