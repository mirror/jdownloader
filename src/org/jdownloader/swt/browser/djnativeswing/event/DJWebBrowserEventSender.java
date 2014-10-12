package org.jdownloader.swt.browser.djnativeswing.event;

import org.appwork.utils.event.Eventsender;

public class DJWebBrowserEventSender extends Eventsender<DJWebBrowserListener, DJWebBrowserEvent> {

    @Override
    protected void fireEvent(DJWebBrowserListener listener, DJWebBrowserEvent event) {
        event.sendTo(listener);
    }
}