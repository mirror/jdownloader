package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;
import jd.plugins.Account;

import org.appwork.utils.swing.table.ExtColumn;

public class PremiumAccountTable extends BasicJDTable<Account> {

    private static final long serialVersionUID = -2166408567306279016L;

    public PremiumAccountTable(AccountManagerSettings accountManagerSettings) {
        super(new PremiumAccountTableModel(accountManagerSettings));
        this.setSearchEnabled(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.table.ExtTable#onShortcutDelete(java.util.ArrayList
     * , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(ArrayList<Account> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(selectedObjects, direct).actionPerformed(null);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.table.ExtTable#onContextMenu(javax.swing.JPopupMenu
     * , java.lang.Object, java.util.ArrayList,
     * org.appwork.utils.swing.table.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, Account contextObject, ArrayList<Account> selection, ExtColumn<Account> column) {
        if (popup != null) {
            if (selection == null) {
                popup.add(new NewAction());
                popup.add(new RemoveAction(selection, false));
                popup.add(new BuyAction());
                popup.add(new RefreshAction(null));
            } else {
                popup.add(new NewAction());
                popup.add(new RemoveAction(selection, false));
                popup.add(new RefreshAction(selection));
            }
        }
        return popup;
    }

}
