package org.jdownloader.osevents;

import org.appwork.utils.event.SimpleEvent;

public class OperatingSystemEvent extends SimpleEvent<Object, Object, OperatingSystemEvent.Type> {

    public static enum Type {
        SHUTDOWN_VETO,
        SESSION_END,
        SIGNAL,
        SIGNAL_TERM,
        SIGNAL_HUP
    }

    public OperatingSystemEvent(Type type, Object... parameters) {
        super(null, type, parameters);
    }
}