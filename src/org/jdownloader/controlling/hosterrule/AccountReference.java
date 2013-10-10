package org.jdownloader.controlling.hosterrule;

import java.util.Date;

import jd.plugins.Account;
import jd.plugins.AccountInfo;

public class AccountReference {

    public AccountReference() {
    }

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public String toString() {
        return "Account: " + getAccount();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AccountReference(Account acc) {
        setAccount(acc);
    }

    public Account getAccount() {
        return account;
    }

    private Account account;

    public long getID() {
        return account.getId().getID();
    }

    public void setAccount(Account acc) {
        this.account = acc;
    }

    public String getHoster() {
        return account.getHoster();
    }

    public String getUser() {
        return account.getUser();
    }

    public Date getExpireDate() {
        AccountInfo ai = account.getAccountInfo();
        if (ai == null) {
            return null;
        } else {
            if (ai.getValidUntil() <= 0) return null;
            return new Date(ai.getValidUntil());
        }

    }

    public boolean isValid() {
        return getAccount().isValid();
    }

    public boolean isTempDisabled() {
        return getAccount().isTempDisabled();
    }

    public long getTmpDisabledTimeout() {
        return getAccount().getTmpDisabledTimeout();
    }

    public AccountInfo getAccountInfo() {
        return getAccount().getAccountInfo();
    }

}
