package org.jdownloader.controlling.hosterrule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AccountUsageRule {

    private boolean              enabled;
    private String               hoster;
    private List<AccountGroup>   accounts;
    private HosterRuleController owner;

    public AccountUsageRule(String tld) {
        hoster = tld;
        accounts = new CopyOnWriteArrayList<AccountGroup>();
    }

    public void setHoster(String hoster) {
        this.hoster = hoster;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        HosterRuleController lowner = owner;
        if (lowner != null) lowner.fireUpdate(this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getHoster() {
        return hoster;
    }

    public List<AccountGroup> getAccounts() {
        return accounts;
    }

    public void setOwner(HosterRuleController controller) {
        owner = controller;
    }

    public void set(boolean enabledState, List<AccountGroup> list) {
        if (list == null || list.isEmpty()) {
            list = new ArrayList<AccountGroup>(1);
            enabledState = false;
        }
        this.enabled = enabledState;
        this.accounts = new CopyOnWriteArrayList<AccountGroup>(list);
        HosterRuleController lowner = owner;
        if (lowner != null) lowner.fireUpdate(this);
    }
}
