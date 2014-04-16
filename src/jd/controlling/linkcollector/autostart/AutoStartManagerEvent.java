package jd.controlling.linkcollector.autostart;

import org.appwork.utils.event.SimpleEvent;

public class AutoStartManagerEvent extends SimpleEvent<Object, Object, AutoStartManagerEvent.Type> {

    public static enum Type {
        RESET,
        RUN,
        DONE
    }

    public AutoStartManagerEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}