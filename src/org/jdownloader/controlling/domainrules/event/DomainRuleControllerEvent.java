package org.jdownloader.controlling.domainrules.event;

import org.appwork.utils.event.SimpleEvent;

public abstract class DomainRuleControllerEvent extends SimpleEvent<Object, Object, DomainRuleControllerEvent.Type> {

    public static enum Type {
    }

    public DomainRuleControllerEvent() {
        this(null, null);
    }

    public DomainRuleControllerEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }

    public abstract void sendTo(DomainRuleControllerListener listener);

}