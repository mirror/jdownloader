package org.jdownloader.api.accounts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jd.controlling.AccountController;
import jd.plugins.Account;

public class AccountAPIImpl implements AccountAPI {

    public List<AccountStorable> list() {
        java.util.List<AccountStorable> ret = new ArrayList<AccountStorable>();
        for (Account acc : AccountController.getInstance().list()) {
            if (acc != null) {
                ret.add(new AccountStorable(acc));
            }
        }
        return ret;
    }

    public boolean remove(Long[] ids) {
        java.util.List<Account> removeACCs = getAccountbyIDs(ids);
        for (Account acc : removeACCs) {
            AccountController.getInstance().removeAccount(acc);
        }
        return true;
    }

    private java.util.List<Account> getAccountbyIDs(Long IDs[]) {
        java.util.List<Long> todoIDs = new ArrayList<Long>(Arrays.asList(IDs));
        java.util.List<Account> accs = new ArrayList<Account>();
        for (Account lacc : AccountController.getInstance().list()) {
            if (lacc != null && todoIDs.size() > 0) {
                Iterator<Long> it = todoIDs.iterator();
                while (it.hasNext()) {
                    long id = it.next();
                    if (lacc.getID().getID() == id) {
                        accs.add(lacc);
                        it.remove();
                    }
                }
            } else if (todoIDs.size() == 0) {
                break;
            }
        }
        return accs;
    }

    public boolean setEnabledState(boolean enabled, Long[] ids) {
        java.util.List<Account> accs = getAccountbyIDs(ids);
        for (Account acc : accs) {
            acc.setEnabled(enabled);
        }
        return true;
    }

    public AccountStorable getAccountInfo(long id) {
        java.util.List<Account> accs = getAccountbyIDs(new Long[] { id });
        if (accs.size() == 1) { return new AccountStorable(accs.get(0)); }
        return null;
    }
}
