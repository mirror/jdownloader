package org.jdownloader.controlling.domainrules.event;

import java.util.EventListener;

public interface DomainRuleControllerListener extends EventListener {

    void onDomainRulesUpdated();

}