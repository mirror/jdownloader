package org.jdownloader.extensions.extraction;

import org.appwork.utils.event.Eventsender;

public class ExtractionEventSender extends Eventsender<ExtractionListener, ExtractionEvent> {
    @Override
    protected void fireEvent(ExtractionListener listener, ExtractionEvent event) {
        listener.onExtractionEvent(event);
    }
}