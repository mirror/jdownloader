package org.jdownloader.updatev2;

import org.appwork.utils.event.SimpleEvent;

public class UpdaterEvent extends SimpleEvent<Object, Object, UpdaterEvent.Type> {

    public static enum Type {
        UPDATES_AVAILABLE,
        UPDATE_STATUS
    }

    public UpdaterEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}