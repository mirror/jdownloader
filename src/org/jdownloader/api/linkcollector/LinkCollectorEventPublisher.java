package org.jdownloader.api.linkcollector;

import java.util.concurrent.CopyOnWriteArraySet;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;

public class LinkCollectorEventPublisher implements EventPublisher, LinkCollectorListener {

    private CopyOnWriteArraySet<EventsSender> eventSenders = new CopyOnWriteArraySet<EventsSender>();
    private final String[]                    eventIDs;

    private enum EVENTID {
        CONTENT_ADDED,
        LINK_ADDED,
        STRUCTURE_REFRESH,
        LIST_LOADED,
        DATA_REFRESH,
        FILTERED_LINKS_EMPTY,
        FILTERED_LINKS_AVAILABLE,
        ABORT,
        CONTENT_MODIFIED,
        DUPE_ADDED,
        CONTENT_REMOVED
    }

    public LinkCollectorEventPublisher() {
        eventIDs = new String[] { EVENTID.CONTENT_REMOVED.name(), EVENTID.DUPE_ADDED.name(), EVENTID.CONTENT_MODIFIED.name(), EVENTID.ABORT.name(), EVENTID.CONTENT_ADDED.name(), EVENTID.LINK_ADDED.name(), EVENTID.STRUCTURE_REFRESH.name(), EVENTID.LIST_LOADED.name(), EVENTID.DATA_REFRESH.name(), EVENTID.FILTERED_LINKS_EMPTY.name(), EVENTID.FILTERED_LINKS_AVAILABLE.name() };
    }

    @Override
    public String[] getPublisherEventIDs() {
        return eventIDs;
    }

    @Override
    public String getPublisherName() {
        return "linkcollector";
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.ABORT.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.FILTERED_LINKS_AVAILABLE.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.FILTERED_LINKS_EMPTY.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.DATA_REFRESH.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.STRUCTURE_REFRESH.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.CONTENT_REMOVED.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.CONTENT_ADDED.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorContentModified(LinkCollectorEvent event) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.CONTENT_MODIFIED.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.LINK_ADDED.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
        SimpleEventObject eventObject = new SimpleEventObject(this, EVENTID.DUPE_ADDED.name(), null);
        for (EventsSender eventSender : eventSenders) {
            eventSender.publishEvent(eventObject, null);
        }
    }

    @Override
    public synchronized void register(EventsSender eventsAPI) {
        boolean wasEmpty = eventSenders.isEmpty();
        eventSenders.add(eventsAPI);
        if (wasEmpty && eventSenders.isEmpty() == false) {
            LinkCollector.getInstance().getEventsender().addListener(this, true);
        }
    }

    @Override
    public synchronized void unregister(EventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
        if (eventSenders.isEmpty()) {
            LinkCollector.getInstance().getEventsender().removeListener(this);
        }
    }
}
