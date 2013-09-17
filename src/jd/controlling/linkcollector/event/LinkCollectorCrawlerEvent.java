package jd.controlling.linkcollector.event;

import org.appwork.utils.event.SimpleEvent;

public class LinkCollectorCrawlerEvent extends SimpleEvent<Object, Object, LinkCollectorCrawlerEvent.Type> {

    public static enum Type {
        CRAWLER_PLUGIN,
        CONTAINER_PLUGIN,
        HOST_PLUGIN
    }

    public LinkCollectorCrawlerEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}