package jd.controlling.linkcollector;

import org.appwork.utils.event.SimpleEvent;

public class LinkCollectorEvent extends SimpleEvent<LinkCollector, Object, LinkCollectorEvent.TYPE> {

    public LinkCollectorEvent(LinkCollector caller, TYPE type, Object[] parameters) {
        super(caller, type, parameters);
    }

    public LinkCollectorEvent(LinkCollector caller, TYPE type, Object parameter) {
        super(caller, type, new Object[] { parameter });
    }

    public LinkCollectorEvent(LinkCollector caller, TYPE type) {
        super(caller, type);
    }

    public static enum TYPE {
        COLLECTOR_START,
        COLLECTOR_STOP,
        REFRESH_DATA,
        REFRESH_STRUCTURE,
        REMOVE_CONTENT
    }

}