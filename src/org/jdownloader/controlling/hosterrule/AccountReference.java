package org.jdownloader.controlling.hosterrule;

import java.util.Date;

import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountTrafficView;

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
        final AccountInfo ai = account.getAccountInfo();
        if (ai == null) {
            return null;
        } else {
            if (ai.getValidUntil() <= 0) {
                return null;
            } else {
                return new Date(ai.getValidUntil());
            }
        }
    }

    public boolean isValid() {
        final Account acc = getAccount();
        if (acc != null) {
            return acc.isValid();
        } else {
            return true;
        }
    }

    public final AccountTrafficView getAccountTrafficView() {
        final Account acc = getAccount();
        if (acc != null) {
            return acc.getAccountTrafficView();
        } else {
            return null;
        }
    }

    public boolean isAvailable() {
        final Account acc = getAccount();
        if (acc != null) {
            return acc.getAccountController() != null;
        } else {
            return true;
        }
    }

    public boolean isTempDisabled() {
        final Account acc = getAccount();
        if (acc != null) {
            return acc.isTempDisabled();
        } else {
            return false;
        }
    }

    public long getTmpDisabledTimeout() {
        final Account acc = getAccount();
        if (acc != null) {
            return acc.getTmpDisabledTimeout();
        } else {
            return -1;
        }
    }

    public AccountInfo getAccountInfo() {
        final Account acc = getAccount();
        if (acc != null) {
            return acc.getAccountInfo();
        }
        return null;
    }
}
