package org.jdownloader.controlling;

import org.appwork.utils.event.Eventsender;

public class FileCreationEventSender extends Eventsender<FileCreationListener, FileCreationEvent> {

    @Override
    protected void fireEvent(FileCreationListener listener, FileCreationEvent event) {
        switch (event.getType()) {
        case NEW_FILES:
            listener.onNewFile(event.getCaller(), event.getFiles());
            break;
        // fill
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}