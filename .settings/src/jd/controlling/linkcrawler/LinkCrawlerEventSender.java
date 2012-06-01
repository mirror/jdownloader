package jd.controlling.linkcrawler;

import org.appwork.utils.event.Eventsender;

public class LinkCrawlerEventSender extends Eventsender<LinkCrawlerListener, LinkCrawlerEvent> {

    @Override
    protected void fireEvent(LinkCrawlerListener listener, LinkCrawlerEvent event) {
        listener.onLinkCrawlerEvent(event);
    }

}
