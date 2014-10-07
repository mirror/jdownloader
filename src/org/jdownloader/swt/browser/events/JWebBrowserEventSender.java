package org.jdownloader.swt.browser.events;

import org.appwork.utils.event.Eventsender;

public class JWebBrowserEventSender extends Eventsender<JWebBrowserListener, JWebBrowserEvent> {

    @Override
    protected void fireEvent(JWebBrowserListener listener, JWebBrowserEvent event) {
        event.sendTo(listener);
    }
}