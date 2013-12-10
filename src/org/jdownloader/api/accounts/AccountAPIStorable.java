package org.jdownloader.api.accounts;

import jd.plugins.Account;

import org.appwork.storage.Storable;

public class AccountAPIStorable extends org.jdownloader.myjdownloader.client.bindings.AccountStorable implements Storable {

    private Account acc;

    @Override
    public long getUUID() {
        return acc.getId().getID();
    }

    @Override
    public String getHostname() {
        return acc.getHoster();
    }

    @SuppressWarnings("unused")
    private AccountAPIStorable(/* Storable */) {
        super();
    }

    public AccountAPIStorable(Account acc) {
        super();
        this.acc = acc;
    }

}
