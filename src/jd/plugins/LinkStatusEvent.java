package jd.plugins;

import jd.plugins.LinkStatus;

import org.appwork.utils.event.DefaultEvent;

public class LinkStatusEvent extends DefaultEvent {

    public LinkStatusEvent(LinkStatus caller) {
        super(caller);
    }

    public LinkStatus getLinkStatus() {
        return (LinkStatus) super.getCaller();
    }
}
