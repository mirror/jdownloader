package jd.controlling.linkchecker;

import org.appwork.utils.event.SimpleEvent;

public class LinkCheckerEvent extends SimpleEvent<LinkChecker<?>, Object, LinkCheckerEvent.Type> {

    public LinkCheckerEvent(LinkChecker<?> caller, Type type) {
        super(caller, type);
    }

    public static enum Type {
        STARTED,
        STOPPED
    }
}
