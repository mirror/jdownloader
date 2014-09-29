package org.jdownloader.controlling.domainrules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DomainRuleSet extends ArrayList<CompiledDomainRule> {
    HashMap<CompiledDomainRule, AtomicInteger> counterMap = new HashMap<CompiledDomainRule, AtomicInteger>();

    @Override
    public boolean add(CompiledDomainRule e) {
        counterMap.put(e, new AtomicInteger());
        return super.add(e);
    }

    public HashMap<CompiledDomainRule, AtomicInteger> getMap() {
        return counterMap;
    }
}
