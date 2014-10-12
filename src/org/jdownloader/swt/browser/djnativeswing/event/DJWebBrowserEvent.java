package org.jdownloader.swt.browser.djnativeswing.event;

import org.appwork.utils.event.SimpleEvent;

public abstract class DJWebBrowserEvent extends SimpleEvent<Object, Object, DJWebBrowserEvent.Type> {

    public static enum Type {
    }

    public DJWebBrowserEvent() {
        super(null, null, null);
    }

    abstract public void sendTo(DJWebBrowserListener listener);
}