package jd.gui.swing.jdgui.views.settings.components;

import org.appwork.utils.event.SimpleEvent;

public class StateUpdateEvent<Caller> extends SimpleEvent<Caller, Object, StateUpdateEvent.Types> {
    public StateUpdateEvent(Caller caller) {
        super(caller, Types.VALUE_UPDATED);
    }

    public static enum Types {
        VALUE_UPDATED

    }

}
