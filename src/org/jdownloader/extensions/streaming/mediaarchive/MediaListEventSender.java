package org.jdownloader.extensions.streaming.mediaarchive;

import org.appwork.utils.event.Eventsender;

public class MediaListEventSender extends Eventsender<MediaListListener, MediaListEvent> {

    @Override
    protected void fireEvent(MediaListListener listener, MediaListEvent event) {
        switch (event.getType()) {
        case CONTENT_CHANGED:
            listener.onContentChanged(event.getCaller());
            // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}