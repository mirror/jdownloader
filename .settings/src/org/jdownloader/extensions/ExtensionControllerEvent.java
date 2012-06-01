package org.jdownloader.extensions;

import org.appwork.utils.event.SimpleEvent;

public class ExtensionControllerEvent extends SimpleEvent<Object, Object, ExtensionControllerEvent.Type> {

    public static enum Type {
        UPDATED
    }

    public ExtensionControllerEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}