package jd.controlling.linkcollector.event;

import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.utils.event.Eventsender;

public class LinkCollectorCrawlerEventSender extends Eventsender<LinkCollectorCrawlerListener, LinkCollectorCrawlerEvent> {

    @Override
    protected void fireEvent(LinkCollectorCrawlerListener listener, LinkCollectorCrawlerEvent event) {
        switch (event.getType()) {
        case CRAWLER_PLUGIN:
            listener.onProcessingCrawlerPlugin((LinkCollectorCrawler) event.getCaller(), (CrawledLink) event.getParameter());
            break;
        case CONTAINER_PLUGIN:
            listener.onProcessingContainerPlugin((LinkCollectorCrawler) event.getCaller(), (CrawledLink) event.getParameter());
            break;
        case HOST_PLUGIN:
            listener.onProcessingHosterPlugin((LinkCollectorCrawler) event.getCaller(), (CrawledLink) event.getParameter());
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}