package org.jdownloader.api.jdanywhere.api.storable;

import jd.plugins.AccountProperty;

import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.Storable;
import org.appwork.storage.StorableAllowPrivateAccessModifier;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;

@StorableValidatorIgnoresMissingSetter
public class AccountPropertyStorable implements Storable {
    private AccountProperty accountProperty;

    @SuppressWarnings("unused")
    @StorableAllowPrivateAccessModifier
    private AccountPropertyStorable() {
        this.accountProperty = null;
    }

    public AccountPropertyStorable(AccountProperty accountProperty) {
        this.accountProperty = accountProperty;
    }

    /**
     * @return the Value
     */
    @AllowNonStorableObjects
    public Object getValue() {
        if (accountProperty == null) {
            return null;
        }
        return accountProperty.getValue();
    }

    /**
     * @return the Property
     */
    public jd.plugins.AccountProperty.Property getProperty() {
        if (accountProperty == null) {
            return null;
        }
        return accountProperty.getProperty();
    }

    public String getAccountID() {
        if (accountProperty == null) {
            return null;
        }
        return accountProperty.getAccount().getId().toString();
    }
}
