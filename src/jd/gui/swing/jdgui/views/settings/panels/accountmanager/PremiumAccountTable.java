package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;
import jd.plugins.Account;

public class PremiumAccountTable extends SettingsTable<Account> {

    private static final long serialVersionUID = -2166408567306279016L;

    public PremiumAccountTable(AccountManagerSettings accountManagerSettings) {
        super(new PremiumAccountTableModel(accountManagerSettings));
        this.addMouseListener(new ContextMenuListener(this));
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
        new RemoveAction(this).actionPerformed(null);
        return true;
    }

}
