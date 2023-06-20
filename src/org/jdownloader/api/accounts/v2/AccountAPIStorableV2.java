package org.jdownloader.api.accounts.v2;

import jd.plugins.Account;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableAllowPrivateAccessModifier;

public class AccountAPIStorableV2 extends org.jdownloader.myjdownloader.client.bindings.AccountStorable implements Storable {
    @SuppressWarnings("unused")
    @StorableAllowPrivateAccessModifier
    private AccountAPIStorableV2(/* Storable */) {
        super();
    }

    public AccountAPIStorableV2(Account acc) {
        super();
        setUUID(acc.getId().getID());
        setHostname(acc.getHoster());
    }
}
