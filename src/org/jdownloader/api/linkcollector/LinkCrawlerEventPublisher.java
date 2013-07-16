package org.jdownloader.api.linkcollector;

import java.util.concurrent.CopyOnWriteArraySet;

import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerEvent;
import jd.controlling.linkcrawler.LinkCrawlerListener;

import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;

public class LinkCrawlerEventPublisher implements EventPublisher, LinkCrawlerListener {

    private CopyOnWriteArraySet<EventsSender> eventSenders = new CopyOnWriteArraySet<EventsSender>();
    private final String[]                    eventIDs;

    private enum EVENTID {
        STARTED,
        STOPPED
    }

    public LinkCrawlerEventPublisher() {
        eventIDs = new String[] { EVENTID.STARTED.name(), EVENTID.STOPPED.name() };
    }

    @Override
    public void onLinkCrawlerEvent(LinkCrawlerEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, event.getType().name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public String[] getPublisherEventIDs() {
        return eventIDs;
    }

    @Override
    public String getPublisherName() {
        return "linkcrawler";
    }

    @Override
    public void register(EventsSender eventsAPI) {
        boolean wasEmpty = eventSenders.isEmpty();
        eventSenders.add(eventsAPI);
        if (wasEmpty && eventSenders.isEmpty() == false) {
            LinkCrawler.getEventSender().addListener(this, true);
        }
    }

    @Override
    public void unregister(EventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
        if (eventSenders.isEmpty()) {
            LinkCrawler.getEventSender().removeListener(this);
        }
    }

}
