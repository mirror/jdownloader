package org.jdownloader.controlling.hosterrule;

import java.util.ArrayList;
import java.util.List;

public class AccountUsageRule {

    private boolean              enabled;
    private String               hoster;
    private List<AccountGroup>   accounts;
    private HosterRuleController owner;

    public AccountUsageRule(String tld) {
        hoster = tld;
        accounts = new ArrayList<AccountGroup>();
    }

    public void setHoster(String hoster) {
        this.hoster = hoster;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (owner != null) owner.fireUpdate();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getHoster() {
        return hoster;
    }

    public void setAccounts(List<AccountGroup> list) {
        accounts.clear();
        accounts.addAll(list);
    }

    public List<AccountGroup> getAccounts() {
        return accounts;
    }

    public void setOwner(HosterRuleController controller) {
        owner = controller;
    }

}
