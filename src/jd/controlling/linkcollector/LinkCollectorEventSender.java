package jd.controlling.linkcollector;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.utils.event.Eventsender;

public class LinkCollectorEventSender extends Eventsender<LinkCollectorListener, LinkCollectorEvent> {

    @Override
    protected void fireEvent(LinkCollectorListener listener, LinkCollectorEvent event) {
        switch (event.getType()) {
        case ABORT:
            listener.onLinkCollectorAbort(event);
            break;
        case FILTERED_AVAILABLE:
            listener.onLinkCollectorFilteredLinksAvailable(event);
            break;
        case FILTERED_EMPTY:
            listener.onLinkCollectorFilteredLinksEmpty(event);
            break;
        case REFRESH_DATA:
            listener.onLinkCollectorDataRefresh(event);
            break;
        case REFRESH_STRUCTURE:
            listener.onLinkCollectorStructureRefresh(event);
            break;
        case REMOVE_CONTENT:
            listener.onLinkCollectorContentRemoved(event);
            break;
        case ADD_CONTENT:
            listener.onLinkCollectorContentAdded(event);
            break;
        case ADDED_LINK:
            listener.onLinkCollectorLinkAdded(event, (CrawledLink) event.getParameter());
            break;
        case DUPE_LINK:
            listener.onLinkCollectorDupeAdded(event, (CrawledLink) event.getParameter());
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}