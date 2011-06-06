package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;
import jd.plugins.Account;

public class PremiumAccountTable extends SettingsTable<Account> {

    private static final long serialVersionUID = -2166408567306279016L;

    public PremiumAccountTable(AccountManagerSettings accountManagerSettings) {
        super(new PremiumAccountTableModel(accountManagerSettings));
        this.addMouseListener(new ContextMenuListener(this));
    }

}
