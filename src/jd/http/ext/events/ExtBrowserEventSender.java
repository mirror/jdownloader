package jd.http.ext.events;


public class ExtBrowserEventSender extends Eventsender<ExtBrowserListener, ExtBrowserEvent> {

    @Override
    protected void fireEvent(ExtBrowserListener listener, ExtBrowserEvent event) {
        listener.onFrameEvent(event);
    }

}
