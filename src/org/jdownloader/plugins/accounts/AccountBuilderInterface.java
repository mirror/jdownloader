package org.jdownloader.plugins.accounts;

import javax.swing.JComponent;

import jd.plugins.Account;

public interface AccountBuilderInterface {

    JComponent getComponent();

    void setAccount(Account defaultAccount);

    boolean validateInputs();

    Account getAccount();

    boolean updateAccount(Account input, Account output);

}
