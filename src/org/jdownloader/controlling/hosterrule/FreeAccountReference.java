package org.jdownloader.controlling.hosterrule;

import java.util.Date;

import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.exceptions.WTFException;

public class FreeAccountReference extends AccountReference {

    static final int FREE_ID = 0;
    private String   hoster;

    public FreeAccountReference(String hoster) {
        this.hoster = hoster;
    }

    @Override
    public Account getAccount() {
        throw new WTFException("Not implemented");
    }

    public Date getExpireDate() {
        return null;
    }

    public AccountInfo getAccountInfo() {
        return null;
    }

    public boolean isValid() {
        return true;
    }

    public boolean isTempDisabled() {
        return false;
    }

    public long getTmpDisabledTimeout() {
        return -1;
    }

    @Override
    public long getID() {
        return FREE_ID;
    }

    @Override
    public String getHoster() {
        return hoster;
    }

    @Override
    public String getUser() {
        return "";
    }

    public static boolean isFreeAccount(AccountReference ar) {
        if (ar == null) return false;
        return ar.getID() == FREE_ID;
    }

}
