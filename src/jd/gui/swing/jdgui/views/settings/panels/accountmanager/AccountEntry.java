package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import jd.plugins.Account;

public class AccountEntry {
    private final Account account;

    public Account getAccount() {
        return account;
    }

    public AccountEntry(Account acc) {
        this.account = acc;
    }
}
