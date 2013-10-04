package org.jdownloader.controlling.hosterrule;

import java.util.ArrayList;

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

    public Rules getRule() {
        return rule;
    }

    public void setRule(Rules rule) {
        this.rule = rule;
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

    public AccountGroup restore(String hoster) {
        ArrayList<AccountReference> childsP = new ArrayList<AccountReference>();
        for (AccountReferenceStorable ars : children) {
            childsP.add(ars.restore(hoster));
        }
        AccountGroup ret = new AccountGroup(childsP);
        ret.setName(getName());
        return ret;
    }
}
