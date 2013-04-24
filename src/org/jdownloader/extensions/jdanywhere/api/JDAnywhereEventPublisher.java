package org.jdownloader.extensions.jdanywhere.api;

import java.util.concurrent.CopyOnWriteArraySet;

import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;

public class JDAnywhereEventPublisher implements EventPublisher {

    private CopyOnWriteArraySet<EventsSender> eventSenders = new CopyOnWriteArraySet<EventsSender>();

    @Override
    public String[] getPublisherEventIDs() {
        return new String[0];
    }

    @Override
    public String getPublisherName() {
        return "jdanywhere";
    }

    @Override
    public synchronized void register(EventsSender eventsAPI) {
        boolean wasEmpty = eventSenders.isEmpty();
        eventSenders.add(eventsAPI);
        if (wasEmpty && eventSenders.isEmpty() == false) {
            /* register */
        }
    }

    @Override
    public synchronized void unregister(EventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
        if (eventSenders.isEmpty()) {
            /* unregister */
        }
    }

}
