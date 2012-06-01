package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import javax.swing.AbstractAction;

import jd.plugins.Account;

public abstract class TableBarAction extends AbstractAction {
    private static final long serialVersionUID = 5527701560420863832L;
    protected Account         account;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
