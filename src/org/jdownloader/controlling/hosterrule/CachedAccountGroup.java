package org.jdownloader.controlling.hosterrule;

import java.util.ArrayList;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;

import org.jdownloader.controlling.hosterrule.AccountGroup.Rules;

public class CachedAccountGroup extends ArrayList<CachedAccount> {
    protected final Rules rule;

    public Rules getRule() {
        return rule;
    }

    public CachedAccountGroup(AccountGroup.Rules rule) {
        this.rule = rule;
    }
}
