package jd.controlling.linkcrawler;

import org.appwork.utils.event.SimpleEvent;

public class LinkCrawlerEvent extends SimpleEvent<LinkCrawler, Object, LinkCrawlerEvent.Type> {

    public LinkCrawlerEvent(LinkCrawler caller, Type type) {
        super(caller, type);
    }

    public static enum Type {
        STARTED,
        STOPPED
    }

}
