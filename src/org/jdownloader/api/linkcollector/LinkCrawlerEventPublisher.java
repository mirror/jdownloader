package org.jdownloader.api.linkcollector;

import jd.controlling.linkcrawler.LinkCrawlerEvent;
import jd.controlling.linkcrawler.LinkCrawlerListener;

import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsSender;

public class LinkCrawlerEventPublisher implements LinkCrawlerListener, EventPublisher {
    @Override
    public void onLinkCrawlerEvent(LinkCrawlerEvent event) {
    }

    @Override
    public String[] getPublisherEventIDs() {
        return null;
    }

    @Override
    public String getPublisherName() {
        return null;
    }

    @Override
    public void register(EventsSender eventsAPI) {
    }

    @Override
    public void unregister(EventsSender eventsAPI) {
    }
}
