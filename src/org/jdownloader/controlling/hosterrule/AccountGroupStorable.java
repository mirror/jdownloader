package org.jdownloader.controlling.hosterrule;

import java.util.ArrayList;
import java.util.List;

import jd.plugins.Account;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.hosterrule.AccountGroup.Rules;

public class AccountGroupStorable implements Storable {

    private ArrayList<AccountReferenceStorable> children;
    private Rules                               rule = Rules.RANDOM;
    private String                              name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRule() {
        return rule.name();
    }

    public void setRule(String rule) {
        try {
            this.rule = Rules.valueOf(rule);
        } catch (final Throwable e) {
            this.rule = Rules.ORDER;
        }
    }

    private AccountGroupStorable(/* Storable */) {

    }

    public AccountGroupStorable(AccountGroup ag) {
        rule = ag.getRule();
        children = new ArrayList<AccountReferenceStorable>();
        name = ag.getName();
        for (AccountReference acc : ag.getChildren()) {
            children.add(new AccountReferenceStorable(acc));
        }
    }

    public ArrayList<AccountReferenceStorable> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<AccountReferenceStorable> children) {
        this.children = children;
    }

    public AccountGroup restore(String hoster, List<Account> availableAccounts) {
        if (availableAccounts != null) {
            ArrayList<AccountReference> childsP = new ArrayList<AccountReference>(children.size());
            for (AccountReferenceStorable ars : children) {
                AccountReference child = ars.restore(hoster, availableAccounts);
                if (child != null) childsP.add(child);
            }
            if (childsP.size() > 0) {
                AccountGroup ret = new AccountGroup(childsP);
                ret.setRule(rule);
                ret.setName(getName());
                return ret;
            }
        }
        return null;
    }
}
