package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import jd.plugins.Account;
import jd.plugins.PluginForHost;

import org.jdownloader.logging.LogController;

public class AccountEntry extends Object {

    private Account account;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    private boolean hasAccountDetailsMethod = false;

    public AccountEntry(Account acc) {
        this.account = acc;
        try {
            Class<? extends PluginForHost> cls = account.getPlugin().getClass();
            if (cls.getDeclaredMethod("showAccountDetailsDialog", new Class[] { Account.class }) != null) {
                hasAccountDetailsMethod = true;
            }
        } catch (Throwable e) {
        }
    }

    public boolean isDetailsDialogSupported() {
        return hasAccountDetailsMethod;
    }

    public void showAccountInfoDialog() {
        if (hasAccountDetailsMethod) {
            try {
                account.getPlugin().showAccountDetailsDialog(account);
            } catch (Throwable e) {
                LogController.CL(true).log(e);
            }
        }
    }

}
