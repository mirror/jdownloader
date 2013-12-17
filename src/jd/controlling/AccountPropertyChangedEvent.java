package jd.controlling;

import jd.plugins.Account;
import jd.plugins.Account.AccountProperty;

public class AccountPropertyChangedEvent extends AccountControllerEvent {

    private AccountProperty property;

    public AccountProperty getProperty() {
        return property;
    }

    public void setProperty(AccountProperty property) {
        this.property = property;
    }

    public AccountPropertyChangedEvent(Account account, AccountProperty property, boolean doRefresh) {
        super(AccountController.getInstance(), AccountControllerEvent.Types.ACCOUNT_PROPERTY_UPDATE, account);
        this.setForceCheck(doRefresh);
        this.property = property;
    }

}
