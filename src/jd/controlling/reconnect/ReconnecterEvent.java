package jd.controlling.reconnect;

import org.appwork.utils.event.DefaultEvent;

public class ReconnecterEvent extends DefaultEvent {
    /**
     * Is called before a reconnect process
     */
    public static final int BEFORE           = 1;
    /**
     * Called after reconnect. parameter is boolean success true/false
     */
    public static final int AFTER            = 2;
    /**
     * Reconnecter settings changed. parameter is StorageValueChangeEvent
     */
    public static final int SETTINGS_CHANGED = 3;

    public ReconnecterEvent(final int id) {
        this(Reconnecter.getInstance(), id, null);
    }

    public ReconnecterEvent(final int id, final Object parameter) {
        this(Reconnecter.getInstance(), id, parameter);
    }

    public ReconnecterEvent(final Object caller, final int eventID, final Object parameter) {
        super(caller, eventID, parameter);
    }

}
