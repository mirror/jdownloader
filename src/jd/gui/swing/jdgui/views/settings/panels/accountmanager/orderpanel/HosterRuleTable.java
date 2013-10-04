package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtTableModel;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.HosterRuleController;

public class HosterRuleTable extends BasicJDTable<AccountUsageRule> {

    public HosterRuleTable(ExtTableModel<AccountUsageRule> tableModel) {
        super(tableModel);
    }

    @Override
    protected boolean onShortcutDelete(List<AccountUsageRule> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(selectedObjects, direct).actionPerformed(null);
        return true;
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, AccountUsageRule obj) {
        if (obj != null) {
            HosterRuleController.getInstance().showEditPanel(obj);
            return true;
        }
        return false;
    }

}
