package jd.gui.swing.jdgui;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.appwork.resources.AWUTheme;
import org.appwork.swing.exttable.ExtColumn;
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
            setName(_GUI._.LockAllColumnsAction_LockAllColumnsAction_unlockall_columns_());
        } else {
            setName(_GUI._.LockAllColumnsAction_LockAllColumnsAction_lockall_columns_());
        }

        this.putValue(Action.SMALL_ICON, AWUTheme.getInstance().getIcon("exttable/lockColumn", table.getContextIconSize()));
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
