package org.jdownloader.api.accounts;

import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.storage.Storable;

public class AccountStorable implements Storable {

    private Account acc;

    public String getUsername() {
        return acc.getUser();
    }

    public String getStatus() {
        AccountInfo ai = acc.getAccountInfo();
        if (ai != null) return ai.getStatus();
        return null;
    }

    public boolean isEnabled() {
        return acc.isEnabled();
    }

    public long getExpireDate() {
        AccountInfo ai = acc.getAccountInfo();
        if (ai != null) return ai.getValidUntil();
        return -1;
    }

    public long getTrafficLeft() {
        AccountInfo ai = acc.getAccountInfo();
        if (ai != null) return ai.getTrafficLeft();
        return -1;
    }

    public long getTrafficMax() {
        AccountInfo ai = acc.getAccountInfo();
        if (ai != null) return ai.getTrafficMax();
        return -1;
    }

    public long getId() {
        return acc.getID().getID();
    }

    public String getHostname() {
        return acc.getHoster();
    }

    public boolean isValid() {
        return acc.isValid();
    }

    @SuppressWarnings("unused")
    private AccountStorable(/* Storable */) {
    }

    public AccountStorable(Account acc) {
        this.acc = acc;
    }

}
