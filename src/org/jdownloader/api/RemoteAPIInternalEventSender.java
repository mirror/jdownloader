package org.jdownloader.api;

import org.appwork.utils.event.Eventsender;

public class RemoteAPIInternalEventSender extends Eventsender<RemoteAPIInternalEventListener, RemoteAPIInternalEvent> {

    @Override
    protected void fireEvent(RemoteAPIInternalEventListener listener, RemoteAPIInternalEvent event) {
        event.fireTo(listener);
    }

}