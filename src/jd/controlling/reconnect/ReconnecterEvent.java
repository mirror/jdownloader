package jd.controlling.reconnect;

import org.appwork.utils.event.SimpleEvent;

public class ReconnecterEvent extends SimpleEvent<Reconnecter, Object, jd.controlling.reconnect.ReconnecterEvent.Type> {
    public ReconnecterEvent(Type type, Object... parameters) {
        super(Reconnecter.getInstance(), type, parameters);
    }

    public static enum Type {
        BEFORE,
        /**
         * PARAMETER[0]: Boolean | Success
         */
        AFTER,

        SETTINGS_CHANGED
    }

}
