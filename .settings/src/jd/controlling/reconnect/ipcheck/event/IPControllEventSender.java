package jd.controlling.reconnect.ipcheck.event;

import org.appwork.utils.event.Eventsender;

public class IPControllEventSender extends Eventsender<IPControllListener, IPControllEvent> {

    @Override
    protected void fireEvent(IPControllListener listener, IPControllEvent event) {
        switch (event.getType()) {
        case FORBIDDEN_IP:
            listener.onIPForbidden(event.getParameter());
            break;
        case INVALIDATED:
            listener.onIPInvalidated(event.getParameter());
            break;
        case IP_CHANGED:
            listener.onIPChanged(event.getParameter(0), event.getParameter(1));
            break;
        case OFFLINE:
            listener.onIPOffline();
            break;
        case VALIDATED:
            listener.onIPValidated(event.getParameter(0), event.getParameter(1));
            break;
        case ONLINE:
            listener.onIPOnline(event.getParameter());
            break;
        case STATECHANGED:
            listener.onIPStateChanged(event.getParameter(0), event.getParameter(1));
            break;

        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}