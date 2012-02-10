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
        HashMap<String, ArrayList<Account>> list = AccountController.getInstance().list();
        ArrayList<Long> removeIDS = new ArrayList<Long>(Arrays.asList(ids));
        ArrayList<Account> removeACCs = new ArrayList<Account>();
        for (ArrayList<Account> accs : list.values()) {
            if (accs != null && removeIDS.size() > 0) {
                Iterator<Long> it = removeIDS.iterator();
                while (it.hasNext()) {
                    long id = it.next();
                    for (Account acc : accs) {
                        if (acc.getID().getID() == id) {
                            removeACCs.add(acc);
                            it.remove();
                        }
                    }
                }
            } else if (removeIDS.size() == 0) {
                break;
            }
        }
        for (Account acc : removeACCs) {
            AccountController.getInstance().removeAccount(acc.getHoster(), acc);
        }
        return true;
    }
}
