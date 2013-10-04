package org.jdownloader.controlling.hosterrule;

import jd.controlling.AccountController;
import jd.plugins.Account;

import org.appwork.storage.Storable;

public class AccountReferenceStorable implements Storable {
    private long    id;
    private boolean enabled;

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

    public AccountReference restore(String hoster) {
        if (getId() == FreeAccountReference.FREE_ID) { return new FreeAccountReference(hoster); }
        AccountReference ret = new AccountReference();
        for (Account acc : AccountController.getInstance().list(null)) {
            System.out.println(acc + " - " + acc.getId().getID() + " - " + getId());
            if (acc.getId().getID() == getId()) {
                ret.setAccount(acc);
                break;
            }
        }
        ret.setEnabled(isEnabled());
        return ret;
    }

}
