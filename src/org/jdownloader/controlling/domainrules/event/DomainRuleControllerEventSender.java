package org.jdownloader.controlling.domainrules.event;

import org.appwork.utils.event.Eventsender;

public class DomainRuleControllerEventSender extends Eventsender<DomainRuleControllerListener, DomainRuleControllerEvent> {

    @Override
    protected void fireEvent(DomainRuleControllerListener listener, DomainRuleControllerEvent event) {
        event.sendTo(listener);
    }
}