package org.jdownloader.controlling.hosterrule;

import org.appwork.utils.event.SimpleEvent;

public class HosterRuleControllerEvent extends SimpleEvent<Object, Object, HosterRuleControllerEvent.Type> {

    public static enum Type {
        ADDED,
        DATA_UPDATE,
        REMOVED
    }

    public HosterRuleControllerEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}