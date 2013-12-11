package org.jdownloader.api.accounts.v2;

import jd.plugins.Account;

import org.appwork.storage.Storable;

public class AccountAPIStorableV2 extends org.jdownloader.myjdownloader.client.bindings.AccountStorable implements Storable {

    @SuppressWarnings("unused")
    private AccountAPIStorableV2(/* Storable */) {
        super();
    }

    public AccountAPIStorableV2(Account acc) {
        super();

        setUUID(acc.getId().getID());
        setHostname(acc.getHoster());
    }

}
