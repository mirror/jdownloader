package org.jdownloader.controlling.hosterrule;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class AccountRuleStorable implements Storable {

    private String hoster;

    public String getHoster() {
        return hoster;
    }

    public void setHoster(String hoster) {
        this.hoster = hoster;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ArrayList<AccountGroupStorable> getAccounts() {
        return accounts;
    }

    public void setAccounts(ArrayList<AccountGroupStorable> accounts) {
        this.accounts = accounts;
    }

    private boolean                         enabled;
    private ArrayList<AccountGroupStorable> accounts;

    private AccountRuleStorable(/* Storable */) {

    }

    public AccountRuleStorable(AccountUsageRule hr) {

        this.hoster = hr.getHoster();
        this.enabled = hr.isEnabled();
        this.accounts = new ArrayList<AccountGroupStorable>();
        for (AccountGroup ag : hr.getAccounts()) {
            accounts.add(new AccountGroupStorable(ag));
        }
    }

    public AccountUsageRule restore() {
        AccountUsageRule ret = new AccountUsageRule(this.getHoster());
        ret.setEnabled(isEnabled());
        ArrayList<AccountGroup> list = new ArrayList<AccountGroup>(accounts.size());
        for (AccountGroupStorable ags : accounts) {
            list.add(ags.restore(getHoster()));
        }
        ret.setAccounts(list);
        return ret;
    }

}
