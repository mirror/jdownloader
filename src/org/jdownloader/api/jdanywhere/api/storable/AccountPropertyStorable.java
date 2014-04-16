package org.jdownloader.api.jdanywhere.api.storable;

import jd.plugins.AccountProperty;

import org.appwork.storage.Storable;

public class AccountPropertyStorable implements Storable {

    private AccountProperty accountProperty;

    @SuppressWarnings("unused")
    private AccountPropertyStorable() {
        this.accountProperty = null;
    }

    public AccountPropertyStorable(AccountProperty accountProperty) {
        this.accountProperty = accountProperty;
    }

    /**
     * @return the Value
     */
    public Object getValue() {
        if (accountProperty == null) return null;
        return accountProperty.getValue();
    }

    /**
     * @return the Property
     */
    public Object getProperty() {
        if (accountProperty == null) return null;
        return accountProperty.getProperty();
    }

    public Object getAccountID() {
        if (accountProperty == null) return null;
        return accountProperty.getAccount().getId().toString();
    }

}
