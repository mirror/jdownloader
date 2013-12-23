package jd.controlling;

import jd.plugins.Account;
import jd.plugins.AccountProperty;

public class AccountPropertyChangedEvent extends AccountControllerEvent {

    private final AccountProperty property;

    public AccountProperty getProperty() {
        return property;
    }

    public AccountPropertyChangedEvent(Account account, AccountProperty property) {
        super(AccountController.getInstance(), AccountControllerEvent.Types.ACCOUNT_PROPERTY_UPDATE, account);
        this.property = property;
    }

}
