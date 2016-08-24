package org.jdownloader.api.linkcollector;

import java.util.concurrent.CopyOnWriteArraySet;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.RemoteAPIEventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;

public class LinkCollectorEventPublisher implements EventPublisher, LinkCollectorListener {

    private final CopyOnWriteArraySet<RemoteAPIEventsSender> eventSenders = new CopyOnWriteArraySet<RemoteAPIEventsSender>();
    private final String[]                                   eventIDs;

    private enum EVENTID {
        CONTENT_ADDED,
        LINK_ADDED,
        STRUCTURE_REFRESH,
        LIST_LOADED,
        DATA_REFRESH,
        FILTERED_LINKS_EMPTY,
        FILTERED_LINKS_AVAILABLE,
        ABORT,
        DUPE_ADDED,
        CONTENT_REMOVED
    }

    public LinkCollectorEventPublisher() {
        eventIDs = new String[] { EVENTID.CONTENT_REMOVED.name(), EVENTID.DUPE_ADDED.name(), EVENTID.ABORT.name(), EVENTID.CONTENT_ADDED.name(), EVENTID.LINK_ADDED.name(), EVENTID.STRUCTURE_REFRESH.name(), EVENTID.LIST_LOADED.name(), EVENTID.DATA_REFRESH.name(), EVENTID.FILTERED_LINKS_EMPTY.name(), EVENTID.FILTERED_LINKS_AVAILABLE.name() };
    }

    @Override
    public String[] getPublisherEventIDs() {
        return eventIDs;
    }

    @Override
    public String getPublisherName() {
        return "linkcollector";
    }

    private final boolean hasSubscriptionFor(final String eventID) {
        if (eventSenders.size() > 0) {
            for (final RemoteAPIEventsSender eventSender : eventSenders) {
                if (eventSender.hasSubscriptionFor(this, eventID)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
        sendEventID(EVENTID.ABORT);
    }

    private final void sendEventID(EVENTID eventid) {
        if (eventid != null && hasSubscriptionFor(eventid.name())) {
            final SimpleEventObject eventObject = new SimpleEventObject(this, eventid.name(), eventid.name());
            for (final RemoteAPIEventsSender eventSender : eventSenders) {
                eventSender.publishEvent(eventObject, null);
            }
        }
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
        sendEventID(EVENTID.FILTERED_LINKS_AVAILABLE);
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
        sendEventID(EVENTID.FILTERED_LINKS_EMPTY);
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        sendEventID(EVENTID.DATA_REFRESH);
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        sendEventID(EVENTID.STRUCTURE_REFRESH);
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        sendEventID(EVENTID.CONTENT_REMOVED);
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        sendEventID(EVENTID.CONTENT_ADDED);
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        sendEventID(EVENTID.LINK_ADDED);
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
        sendEventID(EVENTID.DUPE_ADDED);
    }

    @Override
    public synchronized void register(RemoteAPIEventsSender eventsAPI) {
        final boolean wasEmpty = eventSenders.isEmpty();
        eventSenders.add(eventsAPI);
        if (wasEmpty && eventSenders.isEmpty() == false) {
            LinkCollector.getInstance().getEventsender().addListener(this, true);
        }
    }

    @Override
    public synchronized void unregister(RemoteAPIEventsSender eventsAPI) {
        eventSenders.remove(eventsAPI);
        if (eventSenders.isEmpty()) {
            LinkCollector.getInstance().getEventsender().removeListener(this);
        }
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }

    @Override
    public void onLinkCrawlerFinished() {
    }

}
