package org.jdownloader.extensions.streaming.mediaarchive;

import org.appwork.utils.event.Eventsender;

public class MediaArchiveEventSender extends Eventsender<MediaArchiveListener, MediaArchiveEvent> {

    @Override
    protected void fireEvent(MediaArchiveListener listener, MediaArchiveEvent event) {
        switch (event.getType()) {
        case PREPARER_QUEUE_UPDATE:
            listener.onPrepareQueueUpdated(event.getCaller());
            return;
            // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}