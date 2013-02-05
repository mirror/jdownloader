package jd.controlling.linkcollector;

import org.appwork.utils.event.SimpleEvent;

public class LinkCollectorApiEvent extends SimpleEvent<LinkCollector, Object, LinkCollectorApiEvent.TYPE> {

    // public LinkCollectorApiEvent(Object caller) {
    // super(caller);
    // }
    //
    // public Object getItem() {
    // return super.getCaller();
    // }

    public LinkCollectorApiEvent(LinkCollector caller, TYPE type, Object[] parameters) {
        super(caller, type, parameters);
    }

    public LinkCollectorApiEvent(LinkCollector caller, TYPE type, Object parameter) {
        super(caller, type, new Object[] { parameter });
    }

    public LinkCollectorApiEvent(LinkCollector caller, TYPE type) {
        super(caller, type);
    }

    public static enum TYPE {
        REMOVE_CONTENT,
        ADD_CONTENT,
    }

}
