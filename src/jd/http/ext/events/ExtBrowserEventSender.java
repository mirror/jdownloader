package jd.http.ext.events;

import org.appwork.utils.event.Eventsender;

public class ExtBrowserEventSender extends Eventsender<ExtBrowserListener, ExtBrowserEvent> {

    @Override
    protected void fireEvent(ExtBrowserListener listener, ExtBrowserEvent event) {
        listener.onFrameEvent(event);
    }

}
