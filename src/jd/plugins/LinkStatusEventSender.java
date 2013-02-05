package jd.plugins;

import org.appwork.utils.event.Eventsender;

public class LinkStatusEventSender extends Eventsender<LinkStatusEventListener, LinkStatusEvent> {

    private static final LinkStatusEventSender INSTANCE = new LinkStatusEventSender();

    public static final LinkStatusEventSender getInstance() {
        return INSTANCE;
    }

    private LinkStatusEventSender() {
    }

    @Override
    protected void fireEvent(LinkStatusEventListener listener, LinkStatusEvent event) {
        listener.LinkStatusChanged(event.getLinkStatus());
    }

}