package org.jdownloader.api.accounts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jd.controlling.AccountController;
import jd.plugins.Account;

public class AccountAPIImpl implements AccountAPI {

    public List<AccountStorable> list() {
        HashMap<String, ArrayList<Account>> list = AccountController.getInstance().list();
        ArrayList<AccountStorable> ret = new ArrayList<AccountStorable>();
        for (ArrayList<Account> accs : list.values()) {
            if (accs != null) {
                for (Account acc : accs) {
                    ret.add(new AccountStorable(acc));
                }
            }
        }
        return ret;
    }

    public boolean remove(Long[] ids) {
        ArrayList<Account> removeACCs = getAccountbyIDs(ids);
        for (Account acc : removeACCs) {
            AccountController.getInstance().removeAccount(acc.getHoster(), acc);
        }
        return true;
    }

    private ArrayList<Account> getAccountbyIDs(Long IDs[]) {
        HashMap<String, ArrayList<Account>> list = AccountController.getInstance().list();
        ArrayList<Long> todoIDs = new ArrayList<Long>(Arrays.asList(IDs));
        ArrayList<Account> accs = new ArrayList<Account>();
        for (ArrayList<Account> laccs : list.values()) {
            if (laccs != null && todoIDs.size() > 0) {
                Iterator<Long> it = todoIDs.iterator();
                while (it.hasNext()) {
                    long id = it.next();
                    for (Account acc : laccs) {
                        if (acc.getID().getID() == id) {
                            accs.add(acc);
                            it.remove();
                        }
                    }
                }
            } else if (todoIDs.size() == 0) {
                break;
            }
        }
        return accs;
    }

    public boolean setEnabledState(boolean enabled, Long[] ids) {
        ArrayList<Account> accs = getAccountbyIDs(ids);
        for (Account acc : accs) {
            acc.setEnabled(enabled);
        }
        return true;
    }

    public AccountStorable getAccountInfo(long id) {
        ArrayList<Account> accs = getAccountbyIDs(new Long[] { id });
        if (accs.size() == 1) { return new AccountStorable(accs.get(0)); }
        return null;
    }
}
