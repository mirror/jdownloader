package org.jdownloader.controlling.hosterrule;

import java.util.Date;

import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountTrafficView;

import org.appwork.utils.Hash;

public class AccountReference {
    public AccountReference() {
    }

    protected boolean enabled = true;

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

    protected Account account;

    public long getID() {
        return getAccount().getId().getID();
    }

    protected String getRef() {
        final Account account = getAccount();
        final String ret = Hash.getStringHash(account.getHoster() + account.getUser() + account.getPass(), Hash.HASH_TYPE_SHA512);
        return ret;
    }

    public void setAccount(Account acc) {
        this.account = acc;
    }

    public String getHoster() {
        return getAccount().getHoster();
    }

    public String getUser() {
        return getAccount().getUser();
    }

    public Date getExpireDate() {
        final AccountInfo ai = getAccount().getAccountInfo();
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

    public AccountTrafficView getAccountTrafficView() {
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
