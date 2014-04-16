package jd.controlling.linkcollector.autostart;

import org.appwork.utils.event.Eventsender;

public class AutoStartManagerEventSender extends Eventsender<AutoStartManagerListener, AutoStartManagerEvent> {

    @Override
    protected void fireEvent(AutoStartManagerListener listener, AutoStartManagerEvent event) {
        switch (event.getType()) {
        case DONE:
            listener.onAutoStartManagerDone();
            break;
        case RESET:
            listener.onAutoStartManagerReset();
            break;
        case RUN:
            listener.onAutoStartManagerRunning();
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}