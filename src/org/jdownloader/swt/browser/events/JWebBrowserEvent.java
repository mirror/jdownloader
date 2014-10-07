package org.jdownloader.swt.browser.events;

import org.appwork.utils.event.SimpleEvent;

public abstract class JWebBrowserEvent extends SimpleEvent<Object, Object, JWebBrowserEvent.Type> {

    public static enum Type {
    }

    public JWebBrowserEvent() {
        super(null, null, null);
    }

    abstract public void sendTo(JWebBrowserListener listener);
}