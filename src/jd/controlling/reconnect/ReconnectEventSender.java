package jd.controlling.reconnect;

import org.appwork.utils.event.Eventsender;

public class ReconnectEventSender extends Eventsender<ReconnecterListener, ReconnecterEvent> {

    @Override
    protected void fireEvent(ReconnecterListener listener, ReconnecterEvent event) {
        switch (event.getType()) {
        case AFTER:
            listener.onAfterReconnect(event);
            break;
        case BEFORE:
            listener.onBeforeReconnect(event);
            break;
        }
    }

}
