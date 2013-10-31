package org.jdownloader.gui.event;

import org.appwork.utils.event.SimpleEvent;

public class GUIEvent extends SimpleEvent<Object, Object, GUIEvent.Type> {

    public static enum Type {
        TAB_SWITCH,
        KEY_MODIFIER
    }

    public GUIEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}