package org.jdownloader.actions.event;

import org.appwork.utils.event.SimpleEvent;

public class AppActionEvent extends SimpleEvent<Object, Object, AppActionEvent.Type> {

    public static enum Type {
        PROPERTY_CHANGE
    }

    public AppActionEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}