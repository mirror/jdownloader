package jd.controlling.linkcollector;

import org.appwork.utils.event.Eventsender;

public class LinkCollectorApiEventSender extends Eventsender<LinkCollectorApiEventListener, LinkCollectorApiEvent> {

    private static final LinkCollectorApiEventSender INSTANCE = new LinkCollectorApiEventSender();

    @Override
    protected void fireEvent(final LinkCollectorApiEventListener listener, final LinkCollectorApiEvent event) {
        listener.onLinkCollectorApiEvent(event);
    };

    public static final LinkCollectorApiEventSender getInstance() {
        return INSTANCE;
    }

    private LinkCollectorApiEventSender() {
    }

}
