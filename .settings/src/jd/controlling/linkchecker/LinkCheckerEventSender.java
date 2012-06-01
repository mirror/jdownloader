package jd.controlling.linkchecker;

import org.appwork.utils.event.Eventsender;

public class LinkCheckerEventSender extends Eventsender<LinkCheckerListener, LinkCheckerEvent> {

    @Override
    protected void fireEvent(LinkCheckerListener listener, LinkCheckerEvent event) {
        listener.onLinkCheckerEvent(event);
    }

}
