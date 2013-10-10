package org.jdownloader.controlling.hosterrule;

import java.util.List;

import jd.plugins.Account;

import org.appwork.storage.Storable;

public class AccountReferenceStorable implements Storable {
    private long    id      = -1;
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private AccountReferenceStorable(/* storable */) {
    }

    public AccountReferenceStorable(AccountReference acc) {
        this.id = acc.getID();
        this.enabled = acc.isEnabled();
    }

    public AccountReference restore(String hoster, List<Account> availableAccounts) {
        if (getId() == FreeAccountReference.FREE_ID) { return new FreeAccountReference(hoster); }
        if (availableAccounts != null) {
            for (Account acc : availableAccounts) {
                if (acc.getId().getID() == getId()) {
                    AccountReference ret = new AccountReference(acc);
                    ret.setEnabled(isEnabled());
                    return ret;
                }
            }
        }
        return null;
    }
}
