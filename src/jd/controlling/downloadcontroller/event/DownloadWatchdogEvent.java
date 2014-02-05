package jd.controlling.downloadcontroller.event;

import org.appwork.utils.event.SimpleEvent;

public class DownloadWatchdogEvent extends SimpleEvent<Object, Object, DownloadWatchdogEvent.Type> {

    public static enum Type {
        DATA_UPDATE,
        STATE_RUNNING,
        STATE_STOPPED,
        STATE_IDLE,
        STATE_PAUSE,
        STATE_STOPPING,
        LINK_STARTED,
        LINK_STOPPED,
        PROPERTY_CHANGE
    }

    public DownloadWatchdogEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}