package org.jdownloader.plugins.accounts;

import javax.swing.JComponent;

import jd.plugins.Account;

public interface EditAccountPanel {

    JComponent getComponent();

    void setAccount(Account defaultAccount);

    boolean validateInputs();

    void setNotifyCallBack(Notifier notifier);

    Account getAccount();

}
