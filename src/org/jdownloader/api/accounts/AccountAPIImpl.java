package org.jdownloader.api.accounts;

import java.util.ArrayList;
import java.util.HashMap;
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

}
