package jd.http.ext.events;

import org.appwork.utils.event.Eventsender;

public class HtmlFrameControllerEventsender extends Eventsender<HtmlFrameControllerListener, HtmlFrameControllerEvent> {

    @Override
    protected void fireEvent(HtmlFrameControllerListener listener, HtmlFrameControllerEvent event) {
        listener.onFrameEvent(event);

    }

}
